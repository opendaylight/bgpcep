/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv6;

import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.SAFI;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.AbstractFlowspecL3vpnRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.destination.ipv6.DestinationFlowspecL3vpnIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.ipv6.route.FlowspecL3vpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.ipv6.routes.FlowspecL3vpnIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;

public final class FlowspecL3vpnIpv6RIBSupport
        extends AbstractFlowspecL3vpnRIBSupport<FlowspecL3vpnIpv6NlriParser,
        FlowspecL3vpnIpv6RoutesCase,
        FlowspecL3vpnIpv6Routes,
        FlowspecL3vpnRoute> {
    public FlowspecL3vpnIpv6RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
            FlowspecL3vpnIpv6RoutesCase.class, FlowspecL3vpnIpv6RoutesCase.QNAME,
            FlowspecL3vpnIpv6Routes.class, FlowspecL3vpnIpv6Routes.QNAME,
            FlowspecL3vpnRoute.class, FlowspecL3vpnRoute.QNAME,
            DestinationFlowspecL3vpnIpv6.QNAME,
            Ipv6AddressFamily.VALUE, Ipv6AddressFamily.QNAME,
            new FlowspecL3vpnIpv6NlriParser(SAFI.FLOWSPEC_VPN));
    }
}
