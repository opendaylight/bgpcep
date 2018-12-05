/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4;

import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecExtensionProviderContext;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.AbstractFlowspecL3vpnRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;

public final class FlowspecL3vpnIpv4RIBSupport
        extends AbstractFlowspecL3vpnRIBSupport<FlowspecL3vpnIpv4NlriParser,
        FlowspecL3vpnIpv4RoutesCase,
        FlowspecL3vpnIpv4Routes,
        FlowspecL3vpnRoute,
        FlowspecL3vpnRouteKey> {
    private static final FlowspecL3vpnIpv4Routes EMPTY_CONTAINER
            = new FlowspecL3vpnIpv4RoutesBuilder().setFlowspecL3vpnRoute(Collections.emptyList()).build();
    private static final FlowspecL3vpnIpv4RoutesCase EMPTY_CASE = new FlowspecL3vpnIpv4RoutesCaseBuilder()
            .setFlowspecL3vpnIpv4Routes(EMPTY_CONTAINER).build();
    private static FlowspecL3vpnIpv4RIBSupport SINGLETON;

    private FlowspecL3vpnIpv4RIBSupport(
            final SimpleFlowspecExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        super(
                mappingService,
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

    public static synchronized FlowspecL3vpnIpv4RIBSupport getInstance(
            final SimpleFlowspecExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        if(SINGLETON == null){
            SINGLETON = new FlowspecL3vpnIpv4RIBSupport(context, mappingService);
        }
        return SINGLETON;
    }

    @Override
    public FlowspecL3vpnRouteKey createRouteListKey(final long pathId, final String routeKey) {
        return new FlowspecL3vpnRouteKey(new PathId(pathId), routeKey);
    }

    @Override
    public FlowspecL3vpnRoute createRoute(final FlowspecL3vpnRoute route, final String routeKey,
            final long pathId, final Attributes attributes) {
        final FlowspecL3vpnRouteBuilder builder;
        if (route != null) {
            builder = new FlowspecL3vpnRouteBuilder(route);
        } else {
            builder = new FlowspecL3vpnRouteBuilder();
        }
        return builder.withKey(new FlowspecL3vpnRouteKey(new PathId(pathId), routeKey))
                .setAttributes(attributes).build();
    }

    @Override
    public FlowspecL3vpnIpv4RoutesCase emptyRoutesCase() {
        return EMPTY_CASE;
    }

    @Override
    public FlowspecL3vpnIpv4Routes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    public List<FlowspecL3vpnRoute> routesFromContainer(final FlowspecL3vpnIpv4Routes container) {
        return container.getFlowspecL3vpnRoute();
    }
}
