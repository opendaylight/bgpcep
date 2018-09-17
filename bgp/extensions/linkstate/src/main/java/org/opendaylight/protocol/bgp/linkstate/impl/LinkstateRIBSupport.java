/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.bgp.rib.rib.loc.rib.tables.routes.LinkstateRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.bgp.rib.rib.loc.rib.tables.routes.LinkstateRoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.routes.LinkstateRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.routes.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.routes.linkstate.routes.LinkstateRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LinkstateRIBSupport
        extends AbstractRIBSupport<LinkstateRoutesCase, LinkstateRoutes, LinkstateRoute, LinkstateRouteKey> {
    private static final Logger LOG = LoggerFactory.getLogger(LinkstateRIBSupport.class);

    private static final LinkstateRoutes EMPTY_CONTAINER
            = new LinkstateRoutesBuilder().setLinkstateRoute(Collections.emptyList()).build();
    private static final LinkstateRoutesCase EMPTY_CASE
            = new LinkstateRoutesCaseBuilder().setLinkstateRoutes(EMPTY_CONTAINER).build();
    private static LinkstateRIBSupport SINGLETON;
    private final YangInstanceIdentifier.NodeIdentifier nlriRoutesList
            = new YangInstanceIdentifier.NodeIdentifier(CLinkstateDestination.QNAME);

    private LinkstateRIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(
                mappingService,
                LinkstateRoutesCase.class,
                LinkstateRoutes.class,
                LinkstateRoute.class,
                LinkstateAddressFamily.class,
                LinkstateSubsequentAddressFamily.class,
                DestinationLinkstate.QNAME);
    }

    public synchronized static LinkstateRIBSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if(SINGLETON == null){
            SINGLETON = new LinkstateRIBSupport(mappingService);
        }
        return SINGLETON;
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode linkstate) {
        final ByteBuf buffer = Unpooled.buffer();
        final CLinkstateDestination cLinkstateDestination = LinkstateNlriParser.extractLinkstateDestination(linkstate);
        SimpleNlriTypeRegistry.getInstance().serializeNlriType(cLinkstateDestination, buffer);
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf =
                linkstate.getChild(routePathIdNid());
        return PathIdUtil.createNidKey(routeQName(), routeKeyQName(),
                pathIdQName(), Arrays.toString(ByteArray.readAllBytes(buffer)), maybePathIdLeaf);
    }

    private static List<CLinkstateDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(LinkstateNlriParser::extractLinkstateDestination).collect(Collectors.toList());
    }

    @Override
    protected List<Pair<String, PathId>> processDestination(final DOMDataWriteTransaction tx,
                                                            final YangInstanceIdentifier routesPath,
                                                            final ContainerNode destination,
                                                            final ContainerNode attributes,
                                                            final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes
                    = destination.getChild(this.nlriRoutesList);
            return processRoute(maybeRoutes, routesPath, attributes, function, tx);
        }
        return Collections.EMPTY_LIST;
    }

    private List<Pair<String, PathId>> processRoute(
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes,
            final YangInstanceIdentifier routesPath,
            final ContainerNode attributes, final ApplyRoute function, final DOMDataWriteTransaction tx) {
        if (maybeRoutes.isPresent()) {
            final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
            if (routes instanceof UnkeyedListNode) {
                final YangInstanceIdentifier base = routesYangInstanceIdentifier(routesPath);
                List<Pair<String, PathId>> keyList = new ArrayList<>();
                for (final UnkeyedListEntryNode e : ((UnkeyedListNode) routes).getValue()) {
                    final NodeIdentifierWithPredicates routeKey = createRouteKey(e);
                    function.apply(tx, base, routeKey, e, attributes);
                    keyList.add(new ImmutablePair<>((String) routeKey.getKeyValues().get(routeKeyQName()),
                            new PathId((Long) routeKey.getKeyValues().get(pathIdQName()))));
                }
                return keyList;
            } else {
                LOG.warn("Routes {} are not a map", routes);
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                new DestinationLinkstateBuilder().setCLinkstateDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder()
                .setDestinationLinkstate(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                        .linkstate.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                        .destination.linkstate._case.DestinationLinkstateBuilder()
                        .setCLinkstateDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    public LinkstateRoute createRoute(final LinkstateRoute route, final String routeKey,
            final long pathId, final Attributes attributes) {
        final LinkstateRouteBuilder builder;
        if (route != null) {
            builder = new LinkstateRouteBuilder(route);
        } else {
            builder = new LinkstateRouteBuilder();
        }
        return builder.withKey(createRouteListKey(pathId, routeKey)).setAttributes(attributes).build();
    }

    @Override
    public LinkstateRoutesCase emptyRoutesCase() {
        return EMPTY_CASE;
    }

    @Override
    public LinkstateRoutes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    public LinkstateRouteKey createRouteListKey(final long pathId, final String routeKey) {
        return new LinkstateRouteKey(new PathId(pathId), routeKey);
    }
}
