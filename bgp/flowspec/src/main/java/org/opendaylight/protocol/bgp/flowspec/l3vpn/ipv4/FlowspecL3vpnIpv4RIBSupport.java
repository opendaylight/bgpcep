/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4;

import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecExtensionProviderContext;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.AbstractFlowspecL3vpnRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.peer.effective.rib.in.tables.routes.FlowspecL3vpnIpv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;

public final class FlowspecL3vpnIpv4RIBSupport extends AbstractFlowspecL3vpnRIBSupport<FlowspecL3vpnIpv4NlriParser,
        FlowspecL3vpnRoute, FlowspecL3vpnRouteKey> {

    public FlowspecL3vpnIpv4RIBSupport(final SimpleFlowspecExtensionProviderContext context) {
        super(
                FlowspecL3vpnIpv4RoutesCase.class,
                FlowspecL3vpnIpv4Routes.class,
                FlowspecL3vpnRoute.class,
                DestinationFlowspecL3vpnIpv4.QNAME,
                Ipv4AddressFamily.class,
                new FlowspecL3vpnIpv4NlriParser(context
                        .getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4,
                                SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN))
        );
    }

    public static FlowspecL3vpnIpv4RIBSupport getInstance(final SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecL3vpnIpv4RIBSupport(context);
    }

    @Override
    public final FlowspecL3vpnRouteKey createNewRouteKey(final long pathId, final String routeKey) {
        return new FlowspecL3vpnRouteKey(new PathId(pathId), routeKey);
    }

    @Override
    public FlowspecL3vpnRoute createRoute(final FlowspecL3vpnRoute route, final FlowspecL3vpnRouteKey routeKey,
            final long pathId, final Attributes attributes) {
        final FlowspecL3vpnRouteBuilder builder;
        if (route != null) {
            builder = new FlowspecL3vpnRouteBuilder(route);
        } else {
            builder = new FlowspecL3vpnRouteBuilder();
        }
        return builder.setRouteKey(routeKey.getRouteKey()).setPathId(new PathId(pathId))
                .setAttributes(attributes).build();
    }

    @Override
    public Routes emptyRoutesContainer() {
        return new FlowspecL3vpnIpv4RoutesCaseBuilder()
                .setFlowspecL3vpnIpv4Routes(new FlowspecL3vpnIpv4RoutesBuilder().build()).build();
    }
}
