/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

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
import org.opendaylight.protocol.bgp.evpn.impl.nlri.EvpnNlriParser;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.bgp.rib.rib.loc.rib.tables.routes.EvpnRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.destination.EvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.EvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.EvpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.EvpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.EvpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.evpn._case.DestinationEvpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.destination.DestinationType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
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

final class EvpnRibSupport extends AbstractRIBSupport<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
        .bgp.evpn.rev171213.bgp.rib.rib.peer.adj.rib.in.tables.routes.EvpnRoutesCase, EvpnRoute, EvpnRouteKey> {
    private static final EvpnRibSupport SINGLETON = new EvpnRibSupport();
    private static final Logger LOG = LoggerFactory.getLogger(EvpnRibSupport.class);
    private static final QName ROUTE_KEY_QNAME = QName.create(EvpnRoute.QNAME, ROUTE_KEY).intern();
    private static final NodeIdentifier NLRI_ROUTES_LIST = NodeIdentifier.create(EvpnDestination.QNAME);

    private EvpnRibSupport() {
        super(EvpnRoutesCase.class, EvpnRoutes.class, EvpnRoute.class,
                L2vpnAddressFamily.class, EvpnSubsequentAddressFamily.class, DestinationEvpn.QNAME);
    }

    static EvpnRibSupport getInstance() {
        return SINGLETON;
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
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.update
                .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCaseBuilder()
                .setDestinationEvpn(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn
                        .rev171213.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination
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
    protected void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
        final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination
                    .getChild(NLRI_ROUTES_LIST);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(routeNid());
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

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode evpn) {
        final ByteBuf buffer = Unpooled.buffer();
        final EvpnDestination dest = EvpnNlriParser.extractRouteKeyDestination(evpn);
        EvpnNlriParser.serializeNlri(Collections.singletonList(dest), buffer);
        return new NodeIdentifierWithPredicates(routeQName(), ROUTE_KEY_QNAME, ByteArray.encodeBase64(buffer));
    }

    @Override
    public EvpnRouteKey extractRouteKey(final EvpnRoute route) {
        return route.getKey();
    }

    @Override
    public EvpnRoute createRoute(final EvpnRoute route, final EvpnRouteKey routeKey, final PathId pathId,
            final Attributes attributes) {
        final EvpnRouteBuilder builder;
        if (route != null) {
            builder = new EvpnRouteBuilder(route);
        } else {
            builder = new EvpnRouteBuilder();
        }
        return builder.setRouteKey(routeKey.getRouteKey()).setAttributes(attributes).build();
    }

    @Override
    public Collection<EvpnRoute> changedRoutes(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
            .yang.bgp.evpn.rev171213.bgp.rib.rib.peer.adj.rib.in.tables.routes.EvpnRoutesCase routes) {
        final EvpnRoutes routeCont = routes.getEvpnRoutes();
        if (routeCont == null) {
            return Collections.emptyList();
        }
        return routeCont.getEvpnRoute();
    }
}
