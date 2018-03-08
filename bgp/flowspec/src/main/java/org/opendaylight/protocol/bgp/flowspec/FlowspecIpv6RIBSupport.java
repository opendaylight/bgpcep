/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import java.util.Collection;
import java.util.Collections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.ipv6.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv6.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv6.route.FlowspecRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv6.route.FlowspecRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public final class FlowspecIpv6RIBSupport extends AbstractFlowspecRIBSupport<SimpleFlowspecIpv6NlriParser,
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.rib.peer.adj
                .rib.in.tables.routes.FlowspecIpv6RoutesCase, FlowspecRoute, FlowspecRouteKey> {

    public FlowspecIpv6RIBSupport(SimpleFlowspecExtensionProviderContext context) {
        super(
                FlowspecIpv6RoutesCase.class,
                FlowspecIpv6Routes.class,
                FlowspecRoute.class,
                Ipv6AddressFamily.class,
                FlowspecSubsequentAddressFamily.class,
                DestinationFlowspec.QNAME,
                new SimpleFlowspecIpv6NlriParser(context
                        .getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV6,
                                SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC))
        );
    }

    static FlowspecIpv6RIBSupport getInstance(SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecIpv6RIBSupport(context);
    }

    @Override
    public FlowspecRouteKey extractRouteKey(final FlowspecRoute route) {
        return route.getKey();
    }

    @Override
    public FlowspecRoute createRoute(final FlowspecRoute route, final FlowspecRouteKey routeKey,
            final PathId pathId, final Attributes attributes) {
        final FlowspecRouteBuilder builder;
        if (route != null) {
            builder = new FlowspecRouteBuilder(route);
        } else {
            builder = new FlowspecRouteBuilder();
        }
        return builder.setRouteKey(routeKey.getRouteKey()).setPathId(pathId).setAttributes(attributes).build();
    }

    @Override
    public Collection<FlowspecRoute> changedRoutes(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
            .yang.bgp.flowspec.rev171207.bgp.rib.rib.peer.adj.rib.in.tables.routes.FlowspecIpv6RoutesCase routes) {
        final FlowspecIpv6Routes routesCont = routes.getFlowspecIpv6Routes();
        if (routesCont == null) {
            return Collections.emptyList();
        }
        return routesCont.getFlowspecRoute();
    }

    @Override
    public FlowspecRouteKey createNewRouteKey(final PathId pathId, final FlowspecRouteKey routeKey) {
        return new FlowspecRouteKey(pathId, routeKey.getRouteKey());
    }
}
