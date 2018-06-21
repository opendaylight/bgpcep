/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv6;

import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecExtensionProviderContext;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.AbstractFlowspecL3vpnRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.l3vpn.destination.ipv6.DestinationFlowspecL3vpnIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.l3vpn.ipv6.route.FlowspecL3vpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.l3vpn.ipv6.routes.FlowspecL3vpnIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public final class FlowspecL3vpnIpv6RIBSupport extends AbstractFlowspecL3vpnRIBSupport<FlowspecL3vpnIpv6NlriParser> {
    private FlowspecL3vpnIpv6RIBSupport(SimpleFlowspecExtensionProviderContext context) {
        super(
            FlowspecL3vpnIpv6RoutesCase.class,
            FlowspecL3vpnIpv6Routes.class,
            FlowspecL3vpnRoute.class,
            DestinationFlowspecL3vpnIpv6.QNAME,
            Ipv6AddressFamily.class,
            new FlowspecL3vpnIpv6NlriParser(context.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV6, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN))
        );
    }

    public static FlowspecL3vpnIpv6RIBSupport getInstance(final SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecL3vpnIpv6RIBSupport(context);
    }
}
