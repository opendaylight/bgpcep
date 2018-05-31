/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import java.util.Collections;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.destination.ipv6.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.route.FlowspecRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.route.FlowspecRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.routes.FlowspecIpv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;

public final class FlowspecIpv6RIBSupport
        extends AbstractFlowspecRIBSupport<SimpleFlowspecIpv6NlriParser,
        FlowspecIpv6RoutesCase,
        FlowspecIpv6Routes,
        FlowspecRoute,
        FlowspecRouteKey> {
    private static final FlowspecIpv6Routes EMPTY_CONTAINER
            = new FlowspecIpv6RoutesBuilder().setFlowspecRoute(Collections.emptyList()).build();
    private static final FlowspecIpv6RoutesCase EMPTY_CASE = new FlowspecIpv6RoutesCaseBuilder()
            .setFlowspecIpv6Routes(EMPTY_CONTAINER).build();
    private static FlowspecIpv6RIBSupport SINGLETON;

    private FlowspecIpv6RIBSupport(
            SimpleFlowspecExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        super(
                mappingService,
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

    static synchronized FlowspecIpv6RIBSupport getInstance(
            SimpleFlowspecExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new FlowspecIpv6RIBSupport(context, mappingService);
        }
        return SINGLETON;
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
        return builder.withKey(new FlowspecRouteKey(new PathId(pathId), routeKey)).setAttributes(attributes).build();
    }

    @Override
    public FlowspecIpv6RoutesCase emptyRoutesCase() {
        return EMPTY_CASE;
    }

    @Override
    public FlowspecIpv6Routes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    public FlowspecRouteKey createRouteListKey(final long pathId, final String routeKey) {
        return new FlowspecRouteKey(new PathId(pathId), routeKey);
    }
}
