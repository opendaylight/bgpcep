/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static com.google.common.base.Verify.verify;

import java.util.Map;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.SAFI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.bgp.rib.rib.loc.rib.tables.routes.FlowspecRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.ipv4.DestinationFlowspecIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.ipv4.route.FlowspecRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;

public final class FlowspecIpv4RIBSupport
        extends AbstractFlowspecRIBSupport<SimpleFlowspecIpv4NlriParser,
        FlowspecRoutesCase,
        FlowspecRoutes,
        FlowspecRoute,
        FlowspecRouteKey> {
    public FlowspecIpv4RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(
                mappingService,
                FlowspecRoutesCase.class,
                FlowspecRoutes.class,
                FlowspecRoute.class,
                Ipv4AddressFamily.class,
                FlowspecSubsequentAddressFamily.class,
                DestinationFlowspecIpv4.QNAME,
                new SimpleFlowspecIpv4NlriParser(SAFI.FLOWSPEC),
                key -> key.getPathId().getValue(), FlowspecRouteKey::getRouteKey
        );
    }

    @Override
    public FlowspecRouteKey createRouteListKey(final PathId pathId, final String routeKey) {
        return new FlowspecRouteKey(pathId, routeKey);
    }

    @Override
    public PathId extractPathId(final FlowspecRouteKey routeListKey) {
        return routeListKey.getPathId();
    }

    @Override
    public String extractRouteKey(final FlowspecRouteKey routeListKey) {
        return routeListKey.getRouteKey();
    }

    @Override
    public Map<FlowspecRouteKey, FlowspecRoute> extractAdjRibInRoutes(final Routes routes) {
        verify(routes instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
            .bgp.rib.rib.peer.adj.rib.in.tables.routes.FlowspecRoutesCase, "Unrecognized routes %s", routes);
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
                .bgp.rib.rib.peer.adj.rib.in.tables.routes.FlowspecRoutesCase) routes).getFlowspecRoutes()
                .nonnullFlowspecRoute();
    }
}
