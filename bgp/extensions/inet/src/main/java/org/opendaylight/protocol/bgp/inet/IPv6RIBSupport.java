/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import static com.google.common.base.Verify.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.DestinationIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.Ipv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.ipv6.routes.Ipv6RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.ipv6.routes.Ipv6RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 * Class supporting IPv6 unicast RIBs.
 */
final class IPv6RIBSupport extends AbstractIPRibSupport<Ipv6RoutesCase, Ipv6Routes, Ipv6Route, Ipv6RouteKey> {

    private static final Ipv6Routes EMPTY_CONTAINER
            = new Ipv6RoutesBuilder().setIpv6Route(Collections.emptyList()).build();
    private static IPv6RIBSupport SINGLETON;

    private IPv6RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.Ipv6Prefix.class,
                Ipv6AddressFamily.class,
                Ipv6RoutesCase.class,
                Ipv6Routes.class,
                Ipv6Route.class,
                DestinationIpv6.QNAME,
                Ipv6Prefixes.QNAME);
    }

    static synchronized IPv6RIBSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new IPv6RIBSupport(mappingService);
        }
        return SINGLETON;
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationIpv6CaseBuilder().setDestinationIpv6(new DestinationIpv6Builder()
                .setIpv6Prefixes(extractPrefixes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update
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
    public Ipv6Route createRoute(final Ipv6Route route, final Ipv6RouteKey key, final Attributes attributes) {
        final Ipv6RouteBuilder builder;
        if (route != null) {
            builder = new Ipv6RouteBuilder(route);
        } else {
            builder = new Ipv6RouteBuilder();
        }
        return builder.withKey(key).setAttributes(attributes).build();
    }

    @Override
    public Ipv6Routes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    public Ipv6RouteKey createRouteListKey(final PathId pathId, final String routeKey) {
        return new Ipv6RouteKey(pathId, routeKey);
    }

    @Override
    public PathId extractPathId(final Ipv6RouteKey routeListKey) {
        return routeListKey.getPathId();
    }

    @Override
    public String extractRouteKey(final Ipv6RouteKey routeListKey) {
        return routeListKey.getRouteKey();
    }

    @Override
    public List<Ipv6Route> extractAdjRibInRoutes(final Routes routes) {
        verify(routes instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329
            .bgp.rib.rib.peer.adj.rib.in.tables.routes.Ipv6RoutesCase, "Unrecognized routes %s", routes);
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.peer
                .adj.rib.in.tables.routes.Ipv6RoutesCase) routes).getIpv6Routes().nonnullIpv6Route();
    }
}
