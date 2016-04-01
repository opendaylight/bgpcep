/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.labeled.unicast.LabeledUnicastRIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.L3vpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.destination.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.destination.VpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.routes.VpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.routes.vpn.routes.VpnRoute;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Wang
 */
public abstract class AbstractVpnRIBSupport extends AbstractRIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVpnRIBSupport.class);
    private static final ApplyRoute DELETE_ROUTE = new DeleteRoute();
    private static final ChoiceNode EMPTY_ROUTES = Builders.choiceBuilder()
        .withNodeIdentifier(NodeIdentifier.create(Routes.QNAME))
        .addChild(Builders.containerBuilder()
            .withNodeIdentifier(NodeIdentifier.create(VpnRoutes.QNAME))
            .addChild(ImmutableNodes.mapNodeBuilder(VpnRoute.QNAME).build()).build()).build();
    private static final NodeIdentifier DESTINATION = NodeIdentifier.create(L3vpnDestination.QNAME);
    private static final NodeIdentifier NLRI_ROUTES_LIST = NodeIdentifier.create(VpnDestination.QNAME);
    private static final NodeIdentifier ROUTE = NodeIdentifier.create(VpnRoute.QNAME);
    private static final NodeIdentifier PREFIX_TYPE_NID = NodeIdentifier.create(QName.create(VpnDestination.QNAME, "prefix").intern());
    private static final NodeIdentifier LABEL_STACK_NID = NodeIdentifier.create(QName.create(VpnDestination.QNAME, "label-stack").intern());
    private static final NodeIdentifier LV_NID = NodeIdentifier.create(QName.create(VpnDestination.QNAME, "label-value").intern());
    private static final QName ROUTE_KEY = QName.create(VpnRoute.QNAME, "route-key").intern();
    private static final NodeIdentifier RD_NID = NodeIdentifier.create(QName.create(VpnDestination.QNAME, "route-distinguisher").intern());
    private final ApplyRoute putRoute = new PutRoute();
    private final Class<? extends AddressFamily> ADDRESS_FAMILY_CLAZZ;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param cazeClass      Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param containerClass Binding class of the container in routes choice, must not be null.
     * @param listClass      Binding class of the route list, nust not be null;
     */
    protected AbstractVpnRIBSupport(Class<? extends Routes> cazeClass, Class<? extends DataObject> containerClass, Class<? extends Route> listClass, Class<? extends AddressFamily> addressFamilyClass) {
        super(cazeClass, containerClass, listClass);
        ADDRESS_FAMILY_CLAZZ = addressFamilyClass;
    }

    private static VpnDestination extractVpnDestination(final DataContainerNode<? extends YangInstanceIdentifier.PathArgument> route) {
        final VpnDestinationBuilder builder = new VpnDestinationBuilder();
        builder.setPrefix(LabeledUnicastRIBSupport.extractPrefix(route, PREFIX_TYPE_NID));
        builder.setLabelStack(LabeledUnicastRIBSupport.extractLabel(route, LABEL_STACK_NID, LV_NID));
        builder.setRouteDistinguisher(extractRouteDistinguisher(route));
        return builder.build();
    }

    private static RouteDistinguisher extractRouteDistinguisher(final DataContainerNode<? extends YangInstanceIdentifier.PathArgument> route) {
        if (route.getChild(RD_NID).isPresent()) {
            return RouteDistinguisherBuilder.getDefaultInstance((String) route.getChild(RD_NID).get().getValue());
        }
        return null;
    }

    @Nonnull
    @Override
    protected NodeIdentifier destinationContainerIdentifier() {
        return DESTINATION;
    }

    @Override
    protected void deleteDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tablePath, ContainerNode destination, YangInstanceIdentifier.NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, null, DELETE_ROUTE);
    }

    @Override
    protected void putDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tablePath, ContainerNode destination, ContainerNode attributes, YangInstanceIdentifier.NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, attributes, this.putRoute);
    }

    protected abstract DestinationType getAdvertizedDestinationType(List<VpnDestination> dests);

    protected abstract DestinationType getWithdrawnDestinationType(List<VpnDestination> dests);

    @Nonnull
    @Override
    protected MpReachNlri buildReach(Collection<MapEntryNode> routes, CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(ADDRESS_FAMILY_CLAZZ);
        mb.setSafi(MplsLabeledVpnSubsequentAddressFamily.class);
        mb.setCNextHop(hop);

        final List<VpnDestination> dests = new ArrayList<>(routes.size());
        for (final MapEntryNode route : routes) {
            dests.add(extractVpnDestination(route));
        }
        mb.setAdvertizedRoutes(
            new AdvertizedRoutesBuilder().setDestinationType(
                getAdvertizedDestinationType(dests)
            ).build()
        ).build();
        return mb.build();
    }

    @Nonnull
    @Override
    protected MpUnreachNlri buildUnreach(Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(ADDRESS_FAMILY_CLAZZ);
        mb.setSafi(MplsLabeledVpnSubsequentAddressFamily.class);

        final List<VpnDestination> dests = new ArrayList<>(routes.size());
        for (final MapEntryNode route : routes) {
            dests.add(extractVpnDestination(route));
        }
        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            getWithdrawnDestinationType(dests)
            ).build()
        ).build();
        return mb.build();
    }

    @Nonnull
    @Override
    public ChoiceNode emptyRoutes() {
        return EMPTY_ROUTES;
    }

    @Nonnull
    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Nonnull
    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return ImmutableSet.of();
    }

    @Override
    public boolean isComplexRoute() {
        return true;
    }

    private void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
                                    final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(NLRI_ROUTES_LIST);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(ROUTE);
                    for (final UnkeyedListEntryNode e : ((UnkeyedListNode) routes).getValue()) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(e);
                        function.apply(tx, base, routeKey, e, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode l3vpn) {
        final ByteBuf buffer = Unpooled.buffer();

        final VpnDestination dest = extractVpnDestination(l3vpn);
        AbstractVpnNlriParser.serializeNlri(Collections.singletonList(dest), buffer);
        return new NodeIdentifierWithPredicates(VpnRoute.QNAME, ROUTE_KEY, ByteArray.readAllBytes(buffer));
    }

    private abstract static class ApplyRoute {
        abstract void apply(DOMDataWriteTransaction tx, YangInstanceIdentifier base, YangInstanceIdentifier.NodeIdentifierWithPredicates routeKey, DataContainerNode<?> route, final ContainerNode attributes);
    }

    private static final class DeleteRoute extends ApplyRoute {
        @Override
        void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base,
                   final NodeIdentifierWithPredicates routeKey,
                   final DataContainerNode<?> route, final ContainerNode attributes) {
            tx.delete(LogicalDatastoreType.OPERATIONAL, base.node(routeKey));
        }
    }

    private final class PutRoute extends ApplyRoute {
        @Override
        void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base,
                   final NodeIdentifierWithPredicates routeKey,
                   final DataContainerNode<?> route, final ContainerNode attributes) {
            // Build the DataContainer data
            final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> b = ImmutableNodes.mapEntryBuilder();
            b.withNodeIdentifier(routeKey);

            for (final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> child : route.getValue()) {
                b.withChild(child);
            }
            // Add attributes
            final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cb = Builders.containerBuilder(attributes);
            cb.withNodeIdentifier(routeAttributesIdentifier());
            b.withChild(cb.build());
            tx.put(LogicalDatastoreType.OPERATIONAL, base.node(routeKey), b.build());
        }
    }
}
