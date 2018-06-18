/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.target.impl;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.bgp.route.target.impl.nlri.RouteTargetNlriHandler;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.RouteTargetConstrainsSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.bgp.rib.rib.loc.rib.tables.routes.RouteTargetRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.bgp.rib.rib.loc.rib.tables.routes.RouteTargetRoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.RouteTargetChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.destination.RouteTargetDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.destination.RouteTargetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.routes.RouteTargetRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.routes.RouteTargetRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.routes.route.target.routes.RouteTargetRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.routes.route.target.routes.RouteTargetRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.routes.route.target.routes.RouteTargetRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetAdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.route.target.advertized._case.DestinationRouteTarget;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.route.target.advertized._case.DestinationRouteTargetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetWithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
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
 * Route Target Constrains RIBSupport.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetRIBSupport
        extends AbstractRIBSupport<RouteTargetRoutesCase, RouteTargetRoutes, RouteTargetRoute, RouteTargetRouteKey> {
    private static final Logger LOG = LoggerFactory.getLogger(RouteTargetRIBSupport.class);

    private static final NodeIdentifier NLRI_ROUTES_LIST = NodeIdentifier.create(RouteTargetDestination.QNAME);
    private static final RouteTargetRoutes EMPTY_CONTAINER
            = new RouteTargetRoutesBuilder().setRouteTargetRoute(Collections.emptyList()).build();
    private static final RouteTargetRoutesCase EMPTY_CASE =
            new RouteTargetRoutesCaseBuilder().setRouteTargetRoutes(EMPTY_CONTAINER).build();
    private static RouteTargetRIBSupport SINGLETON;
    private final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects
            = ImmutableSet.of(RouteTargetRoutesCase.class);

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param mappingService Serialization service
     */
    private RouteTargetRIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                RouteTargetRoutesCase.class,
                RouteTargetRoutes.class,
                RouteTargetRoute.class,
                Ipv4AddressFamily.class,
                RouteTargetConstrainsSubsequentAddressFamily.class,
                DestinationRouteTarget.QNAME);
    }

    public static synchronized RouteTargetRIBSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new RouteTargetRIBSupport(mappingService);
        }
        return SINGLETON;
    }

    @Override
    public final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return this.cacheableNlriObjects;
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationRouteTargetAdvertizedCaseBuilder().setDestinationRouteTarget(
                new DestinationRouteTargetBuilder().setRouteTargetDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new DestinationRouteTargetWithdrawnCaseBuilder()
                .setDestinationRouteTarget(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                        .route.target.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination
                        .type.destination.route.target.withdrawn._case.DestinationRouteTargetBuilder()
                        .setRouteTargetDestination(extractRoutes(routes)).build()).build();
    }

    private List<RouteTargetDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(this::extractDestination).collect(Collectors.toList());
    }

    private RouteTargetDestination extractDestination(final DataContainerNode<? extends PathArgument> rtDest) {
        return new RouteTargetDestinationBuilder()
                .setPathId(PathIdUtil.buildPathId(rtDest, routePathIdNid()))
                .setRouteTargetChoice(extractRouteTargetChoice(rtDest))
                .build();
    }

    private RouteTargetChoice extractRouteTargetChoice(final DataContainerNode<? extends PathArgument> route) {
        final DataObject nn = this.mappingService.fromNormalizedNode(this.routeDefaultYii, route).getValue();
        return ((RouteTargetRoute) nn).getRouteTargetChoice();
    }


    @Override
    protected final void processDestination(
            final DOMDataWriteTransaction tx,
            final YangInstanceIdentifier routesPath,
            final ContainerNode destination,
            final ContainerNode attributes,
            final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination
                    .getChild(NLRI_ROUTES_LIST);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesYangInstanceIdentifier(routesPath);
                    for (final UnkeyedListEntryNode rtDest : ((UnkeyedListNode) routes).getValue()) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(rtDest);
                        function.apply(tx, base, routeKey, rtDest, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode routeTarget) {
        final ByteBuf buffer = Unpooled.buffer();
        final RouteTargetDestination dest = extractDestination(routeTarget);
        RouteTargetNlriHandler.serializeNlri(Collections.singletonList(dest), buffer);
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf =
                routeTarget.getChild(routePathIdNid());
        return PathIdUtil.createNidKey(routeQName(), routeKeyQName(),
                pathIdQName(), ByteArray.encodeBase64(buffer), maybePathIdLeaf);
    }

    @Override
    public RouteTargetRoute createRoute(
            final RouteTargetRoute route,
            final String routeKey,
            final long pathId,
            final Attributes attributes) {
        final RouteTargetRouteBuilder builder;
        if (route != null) {
            builder = new RouteTargetRouteBuilder(route);
        } else {
            builder = new RouteTargetRouteBuilder();
        }
        return builder.withKey(createRouteListKey(pathId, routeKey)).setAttributes(attributes).build();
    }


    @Override
    public RouteTargetRoutesCase emptyRoutesCase() {
        return EMPTY_CASE;
    }


    @Override
    public RouteTargetRoutes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }


    @Override
    public RouteTargetRouteKey createRouteListKey(final long pathId, final String routeKey) {
        return new RouteTargetRouteKey(new PathId(pathId), routeKey);
    }
}
