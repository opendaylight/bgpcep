/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.prefixes.DestinationIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Ipv4PrefixAndPathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 * Class supporting IPv4 unicast RIBs.
 */
final class IPv4RIBSupport extends AbstractIPRibSupport<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
        .yang.bgp.inet.rev171207.bgp.rib.rib.peer.adj.rib.in.tables.routes.Ipv4RoutesCase,
        Ipv4Route, Ipv4RouteKey> {

    private static final IPv4RIBSupport SINGLETON = new IPv4RIBSupport();

    private IPv4RIBSupport() {
        super(Ipv4PrefixAndPathId.class, Ipv4AddressFamily.class,
                Ipv4RoutesCase.class, Ipv4Routes.class, Ipv4Route.class, DestinationIpv4.QNAME, Ipv4Prefixes.QNAME);
    }

    static IPv4RIBSupport getInstance() {
        return SINGLETON;
    }

    private List<Ipv4Prefixes> extractPrefixes(final Collection<MapEntryNode> routes) {
        final List<Ipv4Prefixes> prefs = new ArrayList<>(routes.size());
        for (final MapEntryNode route : routes) {
            final String prefix = (String) NormalizedNodes.findNode(route, routePrefixIdentifier()).get().getValue();
            final Ipv4PrefixesBuilder prefixBuilder = new Ipv4PrefixesBuilder().setPrefix(new Ipv4Prefix(prefix));
            prefixBuilder.setPathId(PathIdUtil.buildPathId(route, routePathIdNid()));
            prefs.add(prefixBuilder.build());
        }
        return prefs;
    }

    @Nonnull
    @Override
    protected DestinationType buildDestination(@Nonnull final Collection<MapEntryNode> routes) {
        return new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder()
                .setIpv4Prefixes(extractPrefixes(routes)).build()).build();
    }

    @Nonnull
    @Override
    protected DestinationType buildWithdrawnDestination(@Nonnull final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder()
                .setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(extractPrefixes(routes))
                        .build()).build();
    }

    @Override
    public Ipv4RouteKey extractRouteKey(final Ipv4Route route) {
        return route.getKey();
    }

    @Override
    public Ipv4Route createRoute(final Ipv4Route route, final Ipv4RouteKey routeKey, final PathId pathId,
            final Attributes attributes) {
        final Ipv4RouteBuilder builder;
        if (route != null) {
            builder = new Ipv4RouteBuilder(route);
        } else {
            builder = new Ipv4RouteBuilder();
        }
        return builder.setPrefix(routeKey.getPrefix()).setPathId(pathId).setAttributes(attributes).build();
    }

    @Override
    public Collection<Ipv4Route> changedRoutes(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .bgp.inet.rev171207.bgp.rib.rib.peer.adj.rib.in.tables.routes.Ipv4RoutesCase routes) {
        final Ipv4Routes routeCont = routes.getIpv4Routes();
        if (routeCont == null) {
            return Collections.emptyList();
        }
        return routeCont.getIpv4Route();
    }

    @Override
    public Ipv4RouteKey createNewRouteKey(final PathId pathId, final Ipv4RouteKey routeKey) {
        return new Ipv4RouteKey(pathId, routeKey.getPrefix());
    }
}
