/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFlowspecRIBSupport<T extends AbstractFlowspecNlriParser> extends AbstractRIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowspecRIBSupport.class);
    private static final ApplyRoute DELETE_ROUTE = new DeleteRoute();

    protected final QName ROUTE_KEY;
    protected final NodeIdentifier ROUTE;
    protected final QName LIST_CLASS_QNAME;
    protected final ChoiceNode EMPTY_ROUTES;
    protected final NodeIdentifier DESTINATION;
    protected final QName PATHID_QNAME;
    protected final NodeIdentifier PATH_ID_NID;
    protected final Class<? extends AddressFamily> AFI_CLASS;
    protected final Class<? extends SubsequentAddressFamily> SAFI_CLASS;

    private final ApplyRoute putRoute = new PutRoute();
    protected final T flowspecNlriParser;

    protected AbstractFlowspecRIBSupport(
        final Class<? extends Routes> cazeClass,
        final Class<? extends DataObject> containerClass,
        final Class<? extends Route> listClass,
        final QName dstContainerClassQName,
        final Class<? extends AddressFamily> afiClass,
        final Class<? extends SubsequentAddressFamily> safiClass,
        final T flowspecNlriParser
    ) {
        super(cazeClass, containerClass, listClass);

        final QName CONTAINER_CLASS_QNAME = BindingReflections.findQName(containerClass).intern();
        LIST_CLASS_QNAME =
            QName.create(
                CONTAINER_CLASS_QNAME.getNamespace(), CONTAINER_CLASS_QNAME.getRevision(), BindingReflections.findQName(listClass).intern().getLocalName()
            );
        ROUTE = NodeIdentifier.create(LIST_CLASS_QNAME);
        ROUTE_KEY = QName.create(LIST_CLASS_QNAME, "route-key").intern();
        EMPTY_ROUTES = Builders.choiceBuilder()
            .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(Routes.QNAME))
            .addChild(
                Builders.containerBuilder()
                    .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(CONTAINER_CLASS_QNAME))
                    .addChild(
                        ImmutableNodes.mapNodeBuilder(
                            LIST_CLASS_QNAME
                        ).build()
                    ).build()
            ).build();
        DESTINATION = NodeIdentifier.create(dstContainerClassQName);
        PATHID_QNAME = QName.create(LIST_CLASS_QNAME, "path-id").intern();
        PATH_ID_NID = new NodeIdentifier(PATHID_QNAME);

        AFI_CLASS = Preconditions.checkNotNull(afiClass);
        SAFI_CLASS = Preconditions.checkNotNull(safiClass);
        this.flowspecNlriParser = Preconditions.checkNotNull(flowspecNlriParser);
    }

    protected abstract static class ApplyRoute {
        abstract void apply(DOMDataWriteTransaction tx, YangInstanceIdentifier base, NodeIdentifierWithPredicates routeKey, DataContainerNode<?> route, final ContainerNode attributes);
    }

    protected final class PutRoute extends ApplyRoute {
        @Override
        void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base, final NodeIdentifierWithPredicates routeKey,
                   final DataContainerNode<?> route, final ContainerNode attributes) {
            final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = ImmutableNodes.mapEntryBuilder();
            b.withNodeIdentifier(routeKey);

            route.getValue().forEach(b::withChild);
            // Add attributes
            final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cb = Builders.containerBuilder(attributes);
            cb.withNodeIdentifier(routeAttributesIdentifier());
            b.withChild(cb.build());
            tx.put(LogicalDatastoreType.OPERATIONAL, base.node(routeKey), b.build());
        }
    }

    protected static final class DeleteRoute extends ApplyRoute {
        @Override
        void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base, final NodeIdentifierWithPredicates routeKey, final DataContainerNode<?> route, final ContainerNode attributes) {
            tx.delete(LogicalDatastoreType.OPERATIONAL, base.node(routeKey));
        }
    }

    @Override
    @Nonnull
    public final ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Override
    @Nonnull
    public final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return ImmutableSet.of();
    }

    @Override
    public final boolean isComplexRoute() {
        return true;
    }

    @Override
    protected final void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
                                              final ContainerNode destination, final ContainerNode attributes, final NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, attributes, this.putRoute);
    }

    @Override
    protected final void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
                                                 final ContainerNode destination, final NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, null, DELETE_ROUTE);
    }

    private void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
                                    final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(ROUTE);
            final NodeIdentifierWithPredicates routeKey = new NodeIdentifierWithPredicates(LIST_CLASS_QNAME, ROUTE_KEY, flowspecNlriParser.stringNlri(destination));
            function.apply(tx, base, routeKey, destination, attributes);
        }
    }

    @Override
    @Nonnull
    protected NodeIdentifier destinationContainerIdentifier() {
        return DESTINATION;
    }

    @Override
    @Nonnull
    public ChoiceNode emptyRoutes() {
        return EMPTY_ROUTES;
    }

    @Override
    @Nonnull
    protected MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(AFI_CLASS);
        mb.setSafi(SAFI_CLASS);
        mb.setCNextHop(hop);

        PathId pathId = null;
        List<Flowspec> flowspecList = new ArrayList<>();

        if (!routes.isEmpty()) {
            final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
            pathId = PathIdUtil.buildPathId(routesCont, PATH_ID_NID);
            flowspecList = flowspecNlriParser.extractFlowspec(routesCont);
        } else {
            LOG.debug("Building Unreach routes with empty list!");
        }

        mb.setAdvertizedRoutes(
            new AdvertizedRoutesBuilder()
                .setDestinationType(
                    flowspecNlriParser.createAdvertizedRoutesDestinationType(
                        flowspecList, pathId
                    )
                ).build()
        );
        return mb.build();
    }

    @Override
    @Nonnull
    protected MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(AFI_CLASS);
        mb.setSafi(SAFI_CLASS);

        PathId pathId = null;
        List<Flowspec> flowspecList = new ArrayList<>();

        if (!routes.isEmpty()) {
            final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
            pathId = PathIdUtil.buildPathId(routesCont, PATH_ID_NID);
            flowspecList = flowspecNlriParser.extractFlowspec(routesCont);
        } else {
            LOG.debug("Building Unreach routes with empty list!");
        }

        mb.setWithdrawnRoutes(
            new WithdrawnRoutesBuilder()
                .setDestinationType(
                    flowspecNlriParser.createWithdrawnDestinationType(
                        flowspecList, pathId
                    )
                ).build()
        );
        return mb.build();
    }

    @Nullable
    @Override
    public PathArgument getRouteIdAddPath(final long pathId, final PathArgument routeId) {
        return PathIdUtil.createNidKey(pathId, routeId, LIST_CLASS_QNAME, PATHID_QNAME, ROUTE_KEY);
    }

    @Override
    public Long extractPathId(final NormalizedNode<?, ?> data) {
        return PathIdUtil.extractPathId(data, PATH_ID_NID);
    }
}
