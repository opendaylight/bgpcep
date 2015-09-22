/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.bgp.rib.rib.loc.rib.tables.routes.FlowspecRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.routes.flowspec.routes.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
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
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FlowspecRIBSupport extends AbstractRIBSupport {

    private abstract static class ApplyRoute {
        abstract void apply(DOMDataWriteTransaction tx, YangInstanceIdentifier base, NodeIdentifierWithPredicates routeKey, DataContainerNode<?> route, final ContainerNode attributes);
    }

    private static final class DeleteRoute extends ApplyRoute {
        @Override
        void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base, final NodeIdentifierWithPredicates routeKey, final DataContainerNode<?> route, final ContainerNode attributes) {
            tx.delete(LogicalDatastoreType.OPERATIONAL, base.node(routeKey));
        }
    }

    private final class PutRoute extends ApplyRoute {
        @Override
        void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base, final NodeIdentifierWithPredicates routeKey,
            final DataContainerNode<?> route, final ContainerNode attributes) {
            final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = ImmutableNodes.mapEntryBuilder();
            b.withNodeIdentifier(routeKey);

            // FIXME: All route children, there should be a utility somewhere to do this
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

    private static final Logger LOG = LoggerFactory.getLogger(FlowspecRIBSupport.class);

    private static final QName ROUTE_KEY = QName.cachedReference(QName.create(FlowspecRoute.QNAME, "route-key"));
    private static final FlowspecRIBSupport SINGLETON = new FlowspecRIBSupport();
    private static final ApplyRoute DELETE_ROUTE = new DeleteRoute();

    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
        .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
        .addChild(Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(FlowspecRoutes.QNAME))
            .addChild(ImmutableNodes.mapNodeBuilder(FlowspecRoute.QNAME).build()).build()).build();
    private final NodeIdentifier destination = new NodeIdentifier(DestinationFlowspec.QNAME);
    private final NodeIdentifier route = new NodeIdentifier(FlowspecRoute.QNAME);
    private final ApplyRoute putRoute = new PutRoute();

    private final NodeIdentifier NLRI = new NodeIdentifier(Flowspec.QNAME);

    private FlowspecRIBSupport() {
        super(FlowspecRoutesCase.class, FlowspecRoutes.class, FlowspecRoute.class);
    }

    static FlowspecRIBSupport getInstance() {
        return SINGLETON;
    }

    @Override
    public ChoiceNode emptyRoutes() {
        return this.emptyRoutes;
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
        return this.destination;
    }

    private void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final YangInstanceIdentifier base = tablePath.node(ROUTES).node(routesContainerIdentifier()).node(this.route);
            final NodeIdentifierWithPredicates routeKey = new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, ROUTE_KEY,
                    FSNlriParser.stringNlri(destination));
            function.apply(tx, base, routeKey, destination, attributes);
        }
    }

    @Override
    protected void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination) {
        processDestination(tx, tablePath, destination, null, DELETE_ROUTE);
    }

    @Override
    protected void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination, final ContainerNode attributes) {
        processDestination(tx, tablePath, destination, attributes, this.putRoute);
    }

    @Override
    protected MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(Ipv4AddressFamily.class);
        mb.setSafi(FlowspecSubsequentAddressFamily.class);
        mb.setCNextHop(hop);

        mb.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationFlowspecCaseBuilder().setDestinationFlowspec(
                new DestinationFlowspecBuilder().setFlowspec(FSNlriParser.extractFlowspec(Iterables.getOnlyElement(routes))).build()).build()).build());
        return mb.build();
    }

    @Override
    protected MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(Ipv4AddressFamily.class);
        mb.setSafi(FlowspecSubsequentAddressFamily.class);

        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCaseBuilder().setDestinationFlowspec(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder().setFlowspec(FSNlriParser.extractFlowspec(Iterables.getOnlyElement(routes))).build()).build()).build());
        return mb.build();
    }
}
