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
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.ip.destination.type.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.ip.destination.type.VpnDestinationBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Wang
 */
public abstract class AbstractVpnRIBSupport extends AbstractRIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVpnRIBSupport.class);
    private final NodeIdentifier nlriRoutesListNid;
    private final NodeIdentifier prefixTypeNid;
    private final NodeIdentifier labelStackNid;
    private final NodeIdentifier lvNid;
    private final NodeIdentifier rdNid;
    private final NodeIdentifier destinationNid;
    private final QName routeKey;
    private final Class<? extends AddressFamily> addressFamilyClazz;

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
    protected AbstractVpnRIBSupport(Class<? extends Routes> cazeClass, Class<? extends DataObject> containerClass, Class<? extends Route> listClass,
        Class<? extends AddressFamily> addressFamilyClass, final QName vpnDstContainerClassQname) {
        super(cazeClass, containerClass, listClass);
        final QName classQname = BindingReflections.findQName(containerClass).intern();
        routeKey = QName.create(routeQName(), "route-key").intern();
        final QName vpnDstClassQname = QName.create(classQname, VpnDestination.QNAME.getLocalName());
        nlriRoutesListNid = NodeIdentifier.create(vpnDstClassQname);
        prefixTypeNid = NodeIdentifier.create(QName.create(vpnDstClassQname, "prefix").intern());
        labelStackNid = NodeIdentifier.create(QName.create(vpnDstClassQname, "label-stack").intern());
        lvNid = NodeIdentifier.create(QName.create(vpnDstClassQname, "label-value").intern());
        rdNid = NodeIdentifier.create(QName.create(vpnDstClassQname, "route-distinguisher").intern());
        destinationNid = NodeIdentifier.create(vpnDstContainerClassQname);
        addressFamilyClazz = addressFamilyClass;
    }

    private VpnDestination extractVpnDestination(DataContainerNode<? extends PathArgument> route) {
        final VpnDestination dst = new VpnDestinationBuilder()
            .setPrefix(LabeledUnicastRIBSupport.extractPrefix(route, prefixTypeNid))
            .setLabelStack(LabeledUnicastRIBSupport.extractLabel(route, labelStackNid, lvNid))
            .setRouteDistinguisher(extractRouteDistinguisher(route))
            .build();
        return dst;
    }

    private RouteDistinguisher extractRouteDistinguisher(final DataContainerNode<? extends YangInstanceIdentifier.PathArgument> route) {
        if (route.getChild(rdNid).isPresent()) {
            return RouteDistinguisherBuilder.getDefaultInstance((String) route.getChild(rdNid).get().getValue());
        }
        return null;
    }

    @Nonnull
    @Override
    protected NodeIdentifier destinationContainerIdentifier() {
        return destinationNid;
    }

    @Override
    protected void deleteDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tablePath, ContainerNode destination, NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, null, DELETE_ROUTE);
    }

    @Override
    protected void putDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tablePath, ContainerNode destination, ContainerNode attributes, NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, attributes, putRoute);
    }

    protected abstract DestinationType getAdvertisedDestinationType(List<VpnDestination> dests);

    protected abstract DestinationType getWithdrawnDestinationType(List<VpnDestination> dests);

    @Nonnull
    @Override
    protected MpReachNlri buildReach(Collection<MapEntryNode> routes, CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder().setAfi(this.addressFamilyClazz).setSafi(MplsLabeledVpnSubsequentAddressFamily.class)
            .setCNextHop(hop);

        final List<VpnDestination> dests = new ArrayList<>(routes.size());
        dests.addAll(routes.stream().map(this::extractVpnDestination).collect(Collectors.toList()));

        mb.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(getAdvertisedDestinationType(dests)).build()).build();
        return mb.build();
    }

    @Nonnull
    @Override
    protected MpUnreachNlri buildUnreach(Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder().setAfi(this.addressFamilyClazz).setSafi(MplsLabeledVpnSubsequentAddressFamily.class);

        final List<VpnDestination> dests = new ArrayList<>(routes.size());
        dests.addAll(routes.stream().map(this::extractVpnDestination).collect(Collectors.toList()));

        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(getWithdrawnDestinationType(dests)).build()).build();
        return mb.build();
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
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(this.nlriRoutesListNid);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    UnkeyedListNode routeListNode = (UnkeyedListNode) routes;
                    LOG.debug("{} routes are found", routeListNode.getSize());
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(routeNid());
                    for (final UnkeyedListEntryNode e : routeListNode.getValue()) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(e);
                        LOG.debug("Route {} is processed.", routeKey);
                        function.apply(tx, base, routeKey, e, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        } else {
            LOG.debug("Destination is null.");
        }
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode l3vpn) {
        final ByteBuf buffer = Unpooled.buffer();

        final VpnDestination dest = extractVpnDestination(l3vpn);
        AbstractVpnNlriParser.serializeNlri(Collections.singletonList(dest), buffer);
        return new NodeIdentifierWithPredicates(routeQName(), this.routeKey, ByteArray.readAllBytes(buffer));
    }
}
