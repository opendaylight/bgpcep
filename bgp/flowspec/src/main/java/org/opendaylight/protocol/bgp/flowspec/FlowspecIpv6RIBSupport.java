/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.DestinationFlowspecIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public final class FlowspecIpv6RIBSupport extends AbstractFlowspecRIBSupport<FlowspecIpv6NlriParser> {

    public FlowspecIpv6RIBSupport(FlowspecExtensionProviderContext context) {
        super(
            FlowspecIpv6RoutesCase.class,
            FlowspecIpv6Routes.class,
            FlowspecRoute.class,
            DestinationFlowspecIpv6.QNAME,
            Ipv6AddressFamily.class,
            FlowspecSubsequentAddressFamily.class,
            new FlowspecIpv6NlriParser(context.getFlowspecTypeRegistry(FlowspecExtensionProviderContext.AFI.IPV6, FlowspecExtensionProviderContext.SAFI.FLOWSPEC))
        );
    }

    static FlowspecIpv6RIBSupport getInstance(FlowspecExtensionProviderContext context) {
        return new FlowspecIpv6RIBSupport(context);
    }

}
