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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.bgp.rib.rib.loc.rib.tables.routes.LinkstateRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LinkstateRIBSupport
        extends AbstractRIBSupport<LinkstateRoutesCase, LinkstateRoutes, LinkstateRoute> {
    private static final Logger LOG = LoggerFactory.getLogger(LinkstateRIBSupport.class);
    private static final NodeIdentifier NLRI_ROUTES_LIST = NodeIdentifier.create(CLinkstateDestination.QNAME);

    public LinkstateRIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(
                mappingService,
                LinkstateRoutesCase.class,
                LinkstateRoutes.class,
                LinkstateRoute.class,
                LinkstateAddressFamily.VALUE,
                LinkstateSubsequentAddressFamily.VALUE,
                DestinationLinkstate.QNAME);
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode linkstate) {
        final ByteBuf buffer = Unpooled.buffer();
        final CLinkstateDestination cLinkstateDestination = LinkstateNlriParser.extractLinkstateDestination(linkstate);
        SimpleNlriTypeRegistry.getInstance().serializeNlriType(cLinkstateDestination, buffer);
        return PathIdUtil.createNidKey(routeQName(), routeKeyTemplate(),
                ByteArray.encodeBase64(buffer), linkstate.findChildByArg(routePathIdNid()));
    }

    private static List<CLinkstateDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(LinkstateNlriParser::extractLinkstateDestination).collect(Collectors.toList());
    }

    @Override
    protected Collection<NodeIdentifierWithPredicates> processDestination(final DOMDataTreeWriteTransaction tx,
                                                                          final YangInstanceIdentifier routesPath,
                                                                          final ContainerNode destination,
                                                                          final ContainerNode attributes,
                                                                          final ApplyRoute function) {
        if (destination != null) {
            return processRoute(destination.findChildByArg(LinkstateRIBSupport.NLRI_ROUTES_LIST), routesPath,
                attributes, function, tx);
        }
        return Collections.emptyList();
    }

    private List<NodeIdentifierWithPredicates> processRoute(final Optional<DataContainerChild> maybeRoutes,
            final YangInstanceIdentifier routesPath, final ContainerNode attributes, final ApplyRoute function,
            final DOMDataTreeWriteTransaction tx) {
        if (maybeRoutes.isPresent()) {
            final DataContainerChild routes = maybeRoutes.get();
            if (routes instanceof UnkeyedListNode) {
                final YangInstanceIdentifier base = routesYangInstanceIdentifier(routesPath);
                final Collection<UnkeyedListEntryNode> routesList = ((UnkeyedListNode) routes).body();
                final List<NodeIdentifierWithPredicates> keys = new ArrayList<>(routesList.size());
                for (final UnkeyedListEntryNode linkstateDest : routesList) {
                    final NodeIdentifierWithPredicates routeKey = createRouteKey(linkstateDest);
                    function.apply(tx, base, routeKey, linkstateDest, attributes);
                    keys.add(routeKey);
                }
                return keys;
            }
            LOG.warn("Routes {} are not a map", routes);
        }
        return Collections.emptyList();
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                new DestinationLinkstateBuilder().setCLinkstateDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder()
                .setDestinationLinkstate(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120
                        .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                        .destination.linkstate._case.DestinationLinkstateBuilder()
                        .setCLinkstateDestination(extractRoutes(routes)).build())
                .build();
    }
}
