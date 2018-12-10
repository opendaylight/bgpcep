/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecRoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.destination.ipv4.DestinationFlowspecIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv4.route.FlowspecRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv4.route.FlowspecRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.routes.FlowspecRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;

public final class FlowspecIpv4RIBSupport
        extends AbstractFlowspecRIBSupport<SimpleFlowspecIpv4NlriParser,
        FlowspecRoutesCase,
        FlowspecRoutes,
        FlowspecRoute,
        FlowspecRouteKey> {
    private static final FlowspecRoutes EMPTY_CONTAINER
            = new FlowspecRoutesBuilder().setFlowspecRoute(Collections.emptyList()).build();
    private static final FlowspecRoutesCase EMPTY_CASE
            = new FlowspecRoutesCaseBuilder().setFlowspecRoutes(EMPTY_CONTAINER).build();
    private static FlowspecIpv4RIBSupport SINGLETON;

    private FlowspecIpv4RIBSupport(final SimpleFlowspecExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        super(
                mappingService,
                FlowspecRoutesCase.class,
                FlowspecRoutes.class,
                FlowspecRoute.class,
                Ipv4AddressFamily.class,
                FlowspecSubsequentAddressFamily.class,
                DestinationFlowspecIpv4.QNAME,
                new SimpleFlowspecIpv4NlriParser(context
                        .getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4,
                                SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC))
        );
    }

    static synchronized FlowspecIpv4RIBSupport getInstance(
            final SimpleFlowspecExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null){
            SINGLETON = new FlowspecIpv4RIBSupport(context, mappingService);
        }
        return SINGLETON;
    }

    @Override
    public FlowspecRoute createRoute(final FlowspecRoute route, final FlowspecRouteKey key,
            final Attributes attributes) {
        final FlowspecRouteBuilder builder;
        if (route != null) {
            builder = new FlowspecRouteBuilder(route);
        } else {
            builder = new FlowspecRouteBuilder();
        }
        return builder.withKey(key).setAttributes(attributes).build();
    }

    @Override
    public FlowspecRoutesCase emptyRoutesCase() {
        return EMPTY_CASE;
    }

    @Override
    public FlowspecRoutes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    public FlowspecRouteKey createRouteListKey(final PathId pathId, final String routeKey) {
        return new FlowspecRouteKey(pathId, routeKey);
    }

    @Override
    public List<FlowspecRoute> routesFromContainer(final FlowspecRoutes container) {
        return container.getFlowspecRoute();
    }
}
