/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.labeled.unicast.LabeledUnicastRIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.bgp.rib.rib.loc.rib.tables.routes.VpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.vpn.ipv4._case.DestinationVpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.vpn.ipv4._case.DestinationVpnIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.vpn.ipv4.destination.VpnIpv4Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.vpn.ipv4.destination.VpnIpv4DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.vpn.ipv4.routes.VpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.vpn.ipv4.routes.vpn.ipv4.routes.VpnIpv4Route;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VpnIpv4RIBSupport extends AbstractRIBSupport {

    private static final Logger LOG = LoggerFactory.getLogger(VpnIpv4RIBSupport.class);

    private static final ApplyRoute DELETE_ROUTE = new DeleteRoute();

    private static final ChoiceNode EMPTY_ROUTES = Builders.choiceBuilder()
        .withNodeIdentifier(NodeIdentifier.create(Routes.QNAME))
        .addChild(Builders.containerBuilder()
            .withNodeIdentifier(NodeIdentifier.create(VpnIpv4Routes.QNAME))
            .addChild(ImmutableNodes.mapNodeBuilder(VpnIpv4Route.QNAME).build()).build()).build();
    private static final NodeIdentifier DESTINATION = NodeIdentifier.create(DestinationVpnIpv4.QNAME);
    private static final NodeIdentifier NLRI_ROUTES_LIST = NodeIdentifier.create(VpnIpv4Destination.QNAME);
    private static final NodeIdentifier ROUTE = NodeIdentifier.create(VpnIpv4Route.QNAME);
    private static final NodeIdentifier PREFIX_TYPE_NID = NodeIdentifier.create(QName.create(VpnIpv4Destination.QNAME, "prefix").intern());
    private static final NodeIdentifier LABEL_STACK_NID = NodeIdentifier.create(QName.create(VpnIpv4Destination.QNAME, "label-stack").intern());
    private static final NodeIdentifier LV_NID = NodeIdentifier.create(QName.create(VpnIpv4Destination.QNAME, "label-value").intern());
    private static final QName ROUTE_KEY = QName.create(VpnIpv4Route.QNAME, "route-key").intern();

    private static final NodeIdentifier RD_NID = NodeIdentifier.create(QName.create(VpnIpv4Destination.QNAME, "route-distinguisher").intern());;

    protected VpnIpv4RIBSupport() {
        super(VpnIpv4RoutesCase.class, VpnIpv4Routes.class, VpnIpv4Route.class);
    }

    private final ApplyRoute putRoute = new PutRoute();

    private abstract static class ApplyRoute {
        abstract void apply(DOMDataWriteTransaction tx, YangInstanceIdentifier base, NodeIdentifierWithPredicates routeKey, DataContainerNode<?> route, final ContainerNode attributes);
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
            final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = ImmutableNodes.mapEntryBuilder();
            b.withNodeIdentifier(routeKey);

            for (final DataContainerChild<? extends PathArgument, ?> child : route.getValue()) {
                b.withChild(child);
            }
            // Add attributes
            final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cb = Builders.containerBuilder(attributes);
            cb.withNodeIdentifier(routeAttributesIdentifier());
            b.withChild(cb.build());
            tx.put(LogicalDatastoreType.OPERATIONAL, base.node(routeKey), b.build());
        }
    }

    @Override
    public ChoiceNode emptyRoutes() {
        return EMPTY_ROUTES;
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return ImmutableSet.of();
    }

    @Override
    public boolean isComplexRoute() {
        return true;
    }

    @Override
    protected NodeIdentifier destinationContainerIdentifier() {
        return DESTINATION;
    }

    private void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
        final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(NLRI_ROUTES_LIST);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(ROUTE);
                    for (final UnkeyedListEntryNode e : ((UnkeyedListNode)routes).getValue()) {
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

        final VpnIpv4Destination dest = extractVpnIpv4Destination(l3vpn);
        VpnIpv4NlriParser.serializeNlri(Collections.singletonList(dest), buffer);
        return new NodeIdentifierWithPredicates(VpnIpv4Route.QNAME, ROUTE_KEY, ByteArray.readAllBytes(buffer));
    }

    @Override
    protected void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination, final NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, null, DELETE_ROUTE);
    }

    @Override
    protected void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination, final ContainerNode attributes, final NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, attributes, this.putRoute);
    }

    @Override
    protected MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(Ipv4AddressFamily.class);
        mb.setSafi(MplsLabeledVpnSubsequentAddressFamily.class);
        mb.setCNextHop(hop);

        final List<VpnIpv4Destination> dests = new ArrayList<>(routes.size());
        for (final MapEntryNode route : routes) {
            dests.add(extractVpnIpv4Destination(route));
        }
        mb.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationVpnIpv4CaseBuilder().setDestinationVpnIpv4(
                new DestinationVpnIpv4Builder().setVpnIpv4Destination(dests).build()).build()).build());
        return mb.build();
    }

    @Override
    protected MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(Ipv4AddressFamily.class);
        mb.setSafi(MplsLabeledVpnSubsequentAddressFamily.class);

        final List<VpnIpv4Destination> dests = new ArrayList<>(routes.size());
        for (final MapEntryNode route : routes) {
            dests.add(extractVpnIpv4Destination(route));
        }
        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv4CaseBuilder().setDestinationVpnIpv4(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.vpn.ipv4._case.DestinationVpnIpv4Builder().setVpnIpv4Destination(dests).build()).build()).build());
        return mb.build();
    }

    private static VpnIpv4Destination extractVpnIpv4Destination(final DataContainerNode<? extends PathArgument> route) {
        final VpnIpv4DestinationBuilder builder = new VpnIpv4DestinationBuilder();
        builder.setPrefix(LabeledUnicastRIBSupport.extractPrefix(route, PREFIX_TYPE_NID));
        builder.setLabelStack(LabeledUnicastRIBSupport.extractLabel(route, LABEL_STACK_NID, LV_NID));
        builder.setRouteDistinguisher(extractRouteDistinguisher(route));
        return builder.build();
    }

    private static RouteDistinguisher extractRouteDistinguisher(final DataContainerNode<? extends PathArgument> route) {
        if (route.getChild(RD_NID).isPresent()) {
            return RouteDistinguisherBuilder.getDefaultInstance((String) route.getChild(RD_NID).get().getValue());
        }
        return null;
    }

    @Override
    public long extractPathId(final NormalizedNode<?, ?> normalizedNode) {
        return 0;
    }

    @Nullable
    @Override
    public PathArgument getRouteIdAddPath(final long pathId, final PathArgument routeId) {
        return null;
    }
}
