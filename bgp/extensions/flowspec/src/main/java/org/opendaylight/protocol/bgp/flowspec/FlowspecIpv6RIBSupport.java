/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.ipv6.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv6.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public final class FlowspecIpv6RIBSupport extends AbstractFlowspecRIBSupport<SimpleFlowspecIpv6NlriParser> {

    public FlowspecIpv6RIBSupport(SimpleFlowspecExtensionProviderContext context) {
        super(
            FlowspecIpv6RoutesCase.class,
            FlowspecIpv6Routes.class,
            FlowspecRoute.class,
            Ipv6AddressFamily.class,
            FlowspecSubsequentAddressFamily.class,
            DestinationFlowspec.QNAME,
            new SimpleFlowspecIpv6NlriParser(context.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV6, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC))
        );
    }

    static FlowspecIpv6RIBSupport getInstance(SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecIpv6RIBSupport(context);
    }
}
