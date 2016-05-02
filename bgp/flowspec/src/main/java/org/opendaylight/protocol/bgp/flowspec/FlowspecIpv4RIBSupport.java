/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.DestinationFlowspecIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;

public final class FlowspecIpv4RIBSupport extends AbstractFlowspecRIBSupport<FlowspecIpv4NlriParser> {

    public FlowspecIpv4RIBSupport(FlowspecExtensionProviderContext context) {
        super(
            FlowspecRoutesCase.class,
            FlowspecRoutes.class,
            FlowspecRoute.class,
            DestinationFlowspecIpv4.QNAME,
            Ipv4AddressFamily.class,
            FlowspecSubsequentAddressFamily.class,
            new FlowspecIpv4NlriParser(context.getFlowspecTypeRegistry(FlowspecExtensionProviderContext.AFI.IPV4, FlowspecExtensionProviderContext.SAFI.FLOWSPEC))
        );
    }

    static FlowspecIpv4RIBSupport getInstance(FlowspecExtensionProviderContext context) {
        return new FlowspecIpv4RIBSupport(context);
    }

}
