/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4;

import static com.google.common.base.Verify.verify;

import java.util.Map;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.SAFI;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.AbstractFlowspecL3vpnRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;

public final class FlowspecL3vpnIpv4RIBSupport
        extends AbstractFlowspecL3vpnRIBSupport<FlowspecL3vpnIpv4NlriParser,
        FlowspecL3vpnIpv4RoutesCase,
        FlowspecL3vpnIpv4Routes,
        FlowspecL3vpnRoute,
        FlowspecL3vpnRouteKey> {
    public FlowspecL3vpnIpv4RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(
                mappingService,
                FlowspecL3vpnIpv4RoutesCase.class,
                FlowspecL3vpnIpv4Routes.class,
                FlowspecL3vpnRoute.class,
                DestinationFlowspecL3vpnIpv4.QNAME,
                Ipv4AddressFamily.class,
                new FlowspecL3vpnIpv4NlriParser(SAFI.FLOWSPEC_VPN),
                key -> key.getPathId().getValue(), FlowspecL3vpnRouteKey::getRouteKey
        );
    }

    @Override
    public Map<FlowspecL3vpnRouteKey, FlowspecL3vpnRoute> extractAdjRibInRoutes(final Routes routes) {
        verify(routes instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
            .bgp.rib.rib.peer.adj.rib.in.tables.routes.FlowspecL3vpnIpv4RoutesCase, "Unrecognized routes %s", routes);
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
                .bgp.rib.rib.peer.adj.rib.in.tables.routes.FlowspecL3vpnIpv4RoutesCase) routes)
                .getFlowspecL3vpnIpv4Routes().nonnullFlowspecL3vpnRoute();
    }
}
