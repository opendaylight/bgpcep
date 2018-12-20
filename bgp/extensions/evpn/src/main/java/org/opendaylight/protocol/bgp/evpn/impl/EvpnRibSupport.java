/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

import static com.google.common.base.Verify.verify;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.evpn.impl.nlri.EvpnNlriParser;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.bgp.rib.rib.loc.rib.tables.routes.EvpnRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.bgp.rib.rib.loc.rib.tables.routes.EvpnRoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.destination.EvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.routes.EvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.routes.EvpnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.routes.evpn.routes.EvpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.routes.evpn.routes.EvpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.routes.evpn.routes.EvpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.evpn._case.DestinationEvpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EvpnRibSupport extends AbstractRIBSupport<EvpnRoutesCase, EvpnRoutes, EvpnRoute, EvpnRouteKey> {
    private static final Logger LOG = LoggerFactory.getLogger(EvpnRibSupport.class);

    private static final NodeIdentifier NLRI_ROUTES_LIST = NodeIdentifier.create(EvpnDestination.QNAME);
    private static final EvpnRoutes EMPTY_CONTAINER
            =  new EvpnRoutesBuilder().setEvpnRoute(Collections.emptyList()).build();
    private static final EvpnRoutesCase EMPTY_CASE =
            new EvpnRoutesCaseBuilder().setEvpnRoutes(EMPTY_CONTAINER).build();
    private static EvpnRibSupport SINGLETON;

    private EvpnRibSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                EvpnRoutesCase.class,
                EvpnRoutes.class,
                EvpnRoute.class,
                L2vpnAddressFamily.class,
                EvpnSubsequentAddressFamily.class,
                DestinationEvpn.QNAME);
    }

    static synchronized EvpnRibSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new EvpnRibSupport(mappingService);
        }
        return SINGLETON;
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.update
                .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCaseBuilder()
                .setDestinationEvpn(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn
                        .rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination
                        .evpn._case.DestinationEvpnBuilder().setEvpnDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new DestinationEvpnCaseBuilder().setDestinationEvpn(new DestinationEvpnBuilder()
                .setEvpnDestination(extractRoutes(routes)).build()).build();
    }

    private static List<EvpnDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(EvpnNlriParser::extractEvpnDestination).collect(Collectors.toList());
    }

    @Override
    protected Collection<NodeIdentifierWithPredicates> processDestination(final DOMDataWriteTransaction tx,
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
                    final Collection<UnkeyedListEntryNode> routesList = ((UnkeyedListNode) routes).getValue();
                    final List<NodeIdentifierWithPredicates> keys = new ArrayList<>(routesList.size());
                    for (final UnkeyedListEntryNode evpnDest : routesList) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(evpnDest);
                        function.apply(tx, base, routeKey, evpnDest, attributes);
                        keys.add(routeKey);
                    }
                    return keys;
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
        return Collections.emptyList();
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode evpn) {
        final ByteBuf buffer = Unpooled.buffer();
        final EvpnDestination dest = EvpnNlriParser.extractRouteKeyDestination(evpn);
        EvpnNlriParser.serializeNlri(Collections.singletonList(dest), buffer);
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf =
                evpn.getChild(routePathIdNid());
        return PathIdUtil.createNidKey(routeQName(), routeKeyQName(),
                pathIdQName(), ByteArray.encodeBase64(buffer), maybePathIdLeaf);
    }

    @Override
    public EvpnRoute createRoute(final EvpnRoute route, final EvpnRouteKey key, final Attributes attributes) {
        final EvpnRouteBuilder builder;
        if (route != null) {
            builder = new EvpnRouteBuilder(route);
        } else {
            builder = new EvpnRouteBuilder();
        }
        return builder.withKey(key).setAttributes(attributes).build();
    }

    @Override
    public EvpnRoutesCase emptyRoutesCase() {
        return EMPTY_CASE;
    }

    @Override
    public EvpnRoutes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    public EvpnRouteKey createRouteListKey(final PathId pathId, final String routeKey) {
        return new EvpnRouteKey(pathId, routeKey);
    }

    @Override
    public PathId extractPathId(final EvpnRouteKey routeListKey) {
        return routeListKey.getPathId();
    }

    @Override
    public String extractRouteKey(final EvpnRouteKey routeListKey) {
        return routeListKey.getRouteKey();
    }

    @Override
    public List<EvpnRoute> extractAdjRibInRoutes(Routes routes) {
        verify(routes instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329
            .bgp.rib.rib.peer.adj.rib.in.tables.routes.EvpnRoutesCase, "Unrecognized routes %s", routes);
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329
                .bgp.rib.rib.peer.adj.rib.in.tables.routes.EvpnRoutesCase) routes).getEvpnRoutes().nonnullEvpnRoute();
    }
}
