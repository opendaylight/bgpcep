/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled_unicast;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.bgp.rib.rib.loc.rib.tables.routes.LabeledUnicastRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.labeled.unicast.routes.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicast;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
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
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



final class LabeledUnicastRIBSupport extends AbstractRIBSupport {

    private static final Logger LOG = LoggerFactory.getLogger(LabeledUnicastRIBSupport.class);
    private static final LabeledUnicastRIBSupport SINGLETON = new LabeledUnicastRIBSupport();
    private static final QName ROUTE_KEY = QName.cachedReference(QName.create(LabeledUnicastRoute.QNAME, "route-key"));
    private static final ApplyRoute DELETE_ROUTE = new DeleteRoute();

    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
        .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
        .addChild(Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(LabeledUnicastRoutes.QNAME))
            .addChild(ImmutableNodes.mapNodeBuilder(LabeledUnicastRoute.QNAME).build()).build()).build();
    private final NodeIdentifier destination = new NodeIdentifier(DestinationLabeledUnicast.QNAME);
    private final NodeIdentifier route = new NodeIdentifier(LabeledUnicastRoute.QNAME);
    private final NodeIdentifier nlriRoutesList = new NodeIdentifier(CLabeledUnicastDestination.QNAME);
    private final ApplyRoute putRoute = new PutRoute();


    static LabeledUnicastRIBSupport getInstance() {
        return SINGLETON;
    }
    private abstract static class ApplyRoute {
        abstract void apply(DOMDataWriteTransaction tx, YangInstanceIdentifier base, NodeIdentifierWithPredicates routeKey, DataContainerNode<?> route, final ContainerNode attributes);
    }

    private static final class DeleteRoute extends ApplyRoute {

        @Override
        void apply(DOMDataWriteTransaction tx, YangInstanceIdentifier base,
                NodeIdentifierWithPredicates routeKey,
                DataContainerNode<?> route, ContainerNode attributes) {
            tx.delete(LogicalDatastoreType.OPERATIONAL, base.node(routeKey));

        }

    }
    private final class PutRoute extends ApplyRoute {

        @Override
        void apply(DOMDataWriteTransaction tx, YangInstanceIdentifier base,
                NodeIdentifierWithPredicates routeKey,
                DataContainerNode<?> route, ContainerNode attributes) {
            //build the DataContainer data
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

    private LabeledUnicastRIBSupport() {
        super(LabeledUnicastRoutesCase.class, LabeledUnicastRoutes.class, LabeledUnicastRoute.class);
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
    private void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
            final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(this.nlriRoutesList);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(this.route);
                    for (final UnkeyedListEntryNode e : ((UnkeyedListNode)routes).getValue()) {
                        final NodeIdentifierWithPredicates routeKey = new NodeIdentifierWithPredicates(LabeledUnicastRoute.QNAME, ROUTE_KEY, LUNlriParser.stringNlri(e));
                        function.apply(tx, base, routeKey,  e, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }
    @Override
    protected void deleteDestinationRoutes(DOMDataWriteTransaction tx,
            YangInstanceIdentifier tablePath, ContainerNode destination,NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, null, DELETE_ROUTE);
    }

    @Override
    protected void putDestinationRoutes(DOMDataWriteTransaction tx,
            YangInstanceIdentifier tablePath, ContainerNode destination,
            ContainerNode attributes,NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, attributes, this.putRoute);

    }


    @Override
    protected MpReachNlri buildReach(Collection<MapEntryNode> routes,
            CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        mb.setCNextHop(hop);

        final List<CLabeledUnicastDestination> dests = new ArrayList<>(routes.size());
        for (final MapEntryNode route : routes) {
            dests.add(LUNlriParser.extractCLabeledUnicastDestination(route));
        }
        //get the AFI
        if (dests.size() >= 1) {
            final CLabeledUnicastDestination sampleDest = dests.get(0);
            if(sampleDest.getPrefixType().getIpv4Prefix() != null){
                mb.setAfi(Ipv4AddressFamily.class);
            } else {
                mb.setAfi(Ipv6AddressFamily.class);
            }
        }

        mb.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(dests).build()).build()).build());
        return mb.build();
    }

    @Override
    protected MpUnreachNlri buildUnreach(Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setSafi(LabeledUnicastSubsequentAddressFamily.class);

        final List<CLabeledUnicastDestination> dests = new ArrayList<>(routes.size());
        for (final MapEntryNode route : routes) {
            dests.add(LUNlriParser.extractCLabeledUnicastDestination(route));
        }
        //get the AFI
        if (dests.size() >= 1) {
            final CLabeledUnicastDestination sampleDest = dests.get(0);
            if (sampleDest.getPrefixType().getIpv4Prefix() != null) {
                mb.setAfi(Ipv4AddressFamily.class);
            } else {
                mb.setAfi(Ipv6AddressFamily.class);
            }
        }

        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(
                    dests).build()).build()).build());
        return mb.build();
    }

}
