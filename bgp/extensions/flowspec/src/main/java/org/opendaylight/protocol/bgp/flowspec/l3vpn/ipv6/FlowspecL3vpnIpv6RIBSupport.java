/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv6;

import java.util.Collections;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecExtensionProviderContext;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.AbstractFlowspecL3vpnRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv6RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.destination.ipv6.DestinationFlowspecL3vpnIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv6.route.FlowspecL3vpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv6.route.FlowspecL3vpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv6.route.FlowspecL3vpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv6.routes.FlowspecL3vpnIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv6.routes.FlowspecL3vpnIpv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;

public final class FlowspecL3vpnIpv6RIBSupport
        extends AbstractFlowspecL3vpnRIBSupport<FlowspecL3vpnIpv6NlriParser,
        FlowspecL3vpnIpv6RoutesCase,
        FlowspecL3vpnIpv6Routes,
        FlowspecL3vpnRoute,
        FlowspecL3vpnRouteKey> {
    private static final FlowspecL3vpnIpv6Routes EMPTY_CONTAINER
            = new FlowspecL3vpnIpv6RoutesBuilder().setFlowspecL3vpnRoute(Collections.emptyList()).build();
    private static final FlowspecL3vpnIpv6RoutesCase EMPTY_CASE = new FlowspecL3vpnIpv6RoutesCaseBuilder()
            .setFlowspecL3vpnIpv6Routes(EMPTY_CONTAINER).build();
    private static FlowspecL3vpnIpv6RIBSupport SINGLETON;

    private FlowspecL3vpnIpv6RIBSupport(
            final SimpleFlowspecExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        super(
                mappingService,
                FlowspecL3vpnIpv6RoutesCase.class,
                FlowspecL3vpnIpv6Routes.class,
                FlowspecL3vpnRoute.class,
                DestinationFlowspecL3vpnIpv6.QNAME,
                Ipv6AddressFamily.class,
                new FlowspecL3vpnIpv6NlriParser(context
                        .getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV6,
                                SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN))
        );
    }

    public static synchronized FlowspecL3vpnIpv6RIBSupport getInstance(
            final SimpleFlowspecExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new FlowspecL3vpnIpv6RIBSupport(context, mappingService);
        }
        return SINGLETON;
    }

    @Override
    public FlowspecL3vpnRouteKey createRouteListKey(final PathId pathId, final String routeKey) {
        return new FlowspecL3vpnRouteKey(pathId, routeKey);
    }

    @Override
    public FlowspecL3vpnRoute createRoute(final FlowspecL3vpnRoute route, final FlowspecL3vpnRouteKey key,
            final Attributes attributes) {
        final FlowspecL3vpnRouteBuilder builder;
        if (route != null) {
            builder = new FlowspecL3vpnRouteBuilder(route);
        } else {
            builder = new FlowspecL3vpnRouteBuilder();
        }
        return builder.withKey(key).setAttributes(attributes).build();
    }

    @Override
    public FlowspecL3vpnIpv6RoutesCase emptyRoutesCase() {
        return EMPTY_CASE;
    }

    @Override
    public FlowspecL3vpnIpv6Routes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }
}
