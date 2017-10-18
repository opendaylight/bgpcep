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
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv6.prefixes.DestinationIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 * Class supporting IPv6 unicast RIBs.
 */
final class IPv6RIBSupport extends AbstractIPRIBSupport {

    private static final IPv6RIBSupport SINGLETON = new IPv6RIBSupport();

    private IPv6RIBSupport() {
        super(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.Ipv6Prefix.class,
                Ipv6AddressFamily.class, Ipv6RoutesCase.class, Ipv6Routes.class, Ipv6Route.class, DestinationIpv6.QNAME, Ipv6Prefixes.QNAME);
    }

    static IPv6RIBSupport getInstance() {
        return SINGLETON;
    }

    @Nonnull
    @Override
    protected DestinationType buildDestination(@Nonnull final Collection<MapEntryNode> routes) {
        return new DestinationIpv6CaseBuilder().setDestinationIpv6(new DestinationIpv6Builder().setIpv6Prefixes(extractPrefixes(routes)).build()).build();
    }

    @Nonnull
    @Override
    protected DestinationType buildWithdrawnDestination(@Nonnull final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.update.attributes.mp.unreach.nlri.withdrawn.routes
                .destination.type.DestinationIpv6CaseBuilder().setDestinationIpv6(
                        new DestinationIpv6Builder().setIpv6Prefixes(extractPrefixes(routes)).build()).build();
    }

    private List<Ipv6Prefixes> extractPrefixes(final Collection<MapEntryNode> routes) {
        final List<Ipv6Prefixes> prefs = new ArrayList<>(routes.size());
        for (final MapEntryNode route : routes) {
            final String prefix = (String) NormalizedNodes.findNode(route, routePrefixIdentifier()).get().getValue();
            prefs.add(new Ipv6PrefixesBuilder().setPathId(PathIdUtil.buildPathId(route, routePathIdNid())).setPrefix(new Ipv6Prefix(prefix)).build());
        }
        return prefs;
    }
}
