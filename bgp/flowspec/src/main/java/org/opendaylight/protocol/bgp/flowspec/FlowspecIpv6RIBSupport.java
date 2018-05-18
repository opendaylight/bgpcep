/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.peer.effective.rib.in.tables.routes.FlowspecIpv6RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.destination.ipv6.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.route.FlowspecRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.route.FlowspecRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.routes.FlowspecIpv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;

public final class FlowspecIpv6RIBSupport extends AbstractFlowspecRIBSupport<SimpleFlowspecIpv6NlriParser,
        FlowspecRoute, FlowspecRouteKey> {

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
    public FlowspecRoute createRoute(final FlowspecRoute route, final String routeKey,
            final long pathId, final Attributes attributes) {
        final FlowspecRouteBuilder builder;
        if (route != null) {
            builder = new FlowspecRouteBuilder(route);
        } else {
            builder = new FlowspecRouteBuilder();
        }
        return builder.setKey(new FlowspecRouteKey(new PathId(pathId), routeKey)).setAttributes(attributes).build();
    }

    @Override
    public Routes emptyRoutesContainer() {
        return new FlowspecIpv6RoutesCaseBuilder()
                .setFlowspecIpv6Routes(new FlowspecIpv6RoutesBuilder().build()).build();
    }

    @Override
    public FlowspecRouteKey createRouteListKey(final long pathId, final String routeKey) {
        return new FlowspecRouteKey(new PathId(pathId), routeKey);
    }
}
