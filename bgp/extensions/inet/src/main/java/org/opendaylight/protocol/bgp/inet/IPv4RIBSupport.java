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
import java.util.Map;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Ipv4PrefixAndPathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 * Class supporting IPv4 unicast RIBs.
 */
final class IPv4RIBSupport extends AbstractIPRibSupport<Ipv4RoutesCase, Ipv4Routes, Ipv4Route, Ipv4RouteKey> {

    private static final Ipv4Routes EMPTY_CONTAINER
            = new Ipv4RoutesBuilder().setIpv4Route(Collections.emptyList()).build();
    private static IPv4RIBSupport SINGLETON = null;

    private IPv4RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(
                mappingService,
                Ipv4PrefixAndPathId.class,
                Ipv4AddressFamily.class,
                Ipv4RoutesCase.class,
                Ipv4Routes.class,
                Ipv4Route.class,
                DestinationIpv4.QNAME,
                Ipv4Prefixes.QNAME);
    }

    static synchronized IPv4RIBSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new IPv4RIBSupport(mappingService);
        }
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

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder()
                .setIpv4Prefixes(extractPrefixes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder()
                .setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(extractPrefixes(routes))
                        .build()).build();
    }

    @Override
    public Ipv4Route createRoute(final Ipv4Route route, final Ipv4RouteKey key, final Attributes attributes) {
        final Ipv4RouteBuilder builder;
        if (route != null) {
            builder = new Ipv4RouteBuilder(route);
        } else {
            builder = new Ipv4RouteBuilder();
        }
        builder.withKey(key).setAttributes(attributes);
        return builder.build();
    }

    @Override
    public Ipv4Routes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    public Ipv4RouteKey createRouteListKey(final PathId pathId, final String routeKey) {
        return new Ipv4RouteKey(pathId, routeKey);
    }

    @Override
    public PathId extractPathId(final Ipv4RouteKey routeListKey) {
        return routeListKey.getPathId();
    }

    @Override
    public String extractRouteKey(final Ipv4RouteKey routeListKey) {
        return routeListKey.getRouteKey();
    }

    @Override
    public Map<Ipv4RouteKey, Ipv4Route> extractAdjRibInRoutes(final Routes routes) {
        verify(routes instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329
            .bgp.rib.rib.peer.adj.rib.in.tables.routes.Ipv4RoutesCase, "Unrecognized routes %s", routes);
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.peer
                .adj.rib.in.tables.routes.Ipv4RoutesCase) routes).getIpv4Routes().nonnullIpv4Route();
    }
}
