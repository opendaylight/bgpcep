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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;

/**
 * @author Kevin Wang
 */
public final class FlowspecL3vpnIpv4RIBSupport extends AbstractFlowspecL3vpnRIBSupport<FlowspecL3vpnIpv4NlriParser> {

    public FlowspecL3vpnIpv4RIBSupport(SimpleFlowspecExtensionProviderContext context) {
        super(
            FlowspecL3vpnIpv4RoutesCase.class,
            FlowspecL3vpnIpv4Routes.class,
            FlowspecRoute.class,
            DestinationFlowspecL3vpnIpv4.QNAME,
            Ipv4AddressFamily.class,
            new FlowspecL3vpnIpv4NlriParser(context.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN))
        );
    }

    public static final FlowspecL3vpnIpv4RIBSupport getInstance(SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecL3vpnIpv4RIBSupport(context);
    }
}
