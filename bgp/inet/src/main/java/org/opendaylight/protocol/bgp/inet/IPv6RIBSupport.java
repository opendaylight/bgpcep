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
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.bgp.rib.rib.peer.effective.rib.in.tables.routes.Ipv6RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.prefixes.DestinationIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.Ipv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 * Class supporting IPv6 unicast RIBs.
 */
final class IPv6RIBSupport extends AbstractIPRibSupport<Ipv6Prefix, Ipv6Route, Ipv6RouteKey> {

    private static final IPv6RIBSupport SINGLETON = new IPv6RIBSupport();

    private IPv6RIBSupport() {
        super(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.Ipv6Prefix.class,
                Ipv6AddressFamily.class, Ipv6RoutesCase.class, Ipv6Routes.class, Ipv6Route.class,
                DestinationIpv6.QNAME, Ipv6Prefixes.QNAME);
    }

    static IPv6RIBSupport getInstance() {
        return SINGLETON;
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationIpv6CaseBuilder().setDestinationIpv6(new DestinationIpv6Builder()
                .setIpv6Prefixes(extractPrefixes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6CaseBuilder()
                .setDestinationIpv6(new DestinationIpv6Builder().setIpv6Prefixes(extractPrefixes(routes))
                        .build()).build();
    }

    private List<Ipv6Prefixes> extractPrefixes(final Collection<MapEntryNode> routes) {
        final List<Ipv6Prefixes> prefs = new ArrayList<>(routes.size());
        for (final MapEntryNode route : routes) {
            final String prefix = (String) NormalizedNodes.findNode(route, routePrefixIdentifier()).get().getValue();
            prefs.add(new Ipv6PrefixesBuilder().setPathId(PathIdUtil.buildPathId(route, routePathIdNid()))
                    .setPrefix(new Ipv6Prefix(prefix)).build());
        }
        return prefs;
    }

    @Override
    public Ipv6Route createRoute(final Ipv6Route route, final Ipv6RouteKey routeKey, final long pathId,
            final Attributes attributes) {
        final Ipv6RouteBuilder builder;
        if (route != null) {
            builder = new Ipv6RouteBuilder(route);
        } else {
            builder = new Ipv6RouteBuilder();
        }
        return builder.setPrefix(routeKey.getPrefix()).setPathId(new PathId(pathId)).setAttributes(attributes).build();
    }

    @Override
    public Ipv6Prefix extractRouteKey(final Ipv6RouteKey routeKey) {
        return routeKey.getPrefix();
    }

    @Override
    public Routes emptyRoutesContainer() {
        return new Ipv6RoutesCaseBuilder().setIpv6Routes(new Ipv6RoutesBuilder().build()).build();
    }

    @Override
    public Ipv6RouteKey createNewRouteKey(final long pathId, final Ipv6Prefix prefix) {
        return new Ipv6RouteKey(new PathId(pathId), prefix);
    }
}
