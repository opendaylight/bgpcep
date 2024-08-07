/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.SAFI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.ipv6.DestinationFlowspecIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.ipv6.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;

public final class FlowspecIpv6RIBSupport
        extends AbstractFlowspecIpRIBSupport<SimpleFlowspecIpv6NlriParser,
        FlowspecIpv6RoutesCase,
        FlowspecIpv6Routes,
        FlowspecRoute> {
    public FlowspecIpv6RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
            FlowspecIpv6RoutesCase.class, FlowspecIpv6RoutesCase.QNAME,
            FlowspecIpv6Routes.class, FlowspecIpv6Routes.QNAME,
            FlowspecRoute.class, FlowspecRoute.QNAME,
            Ipv6AddressFamily.VALUE, Ipv6AddressFamily.QNAME,
            FlowspecSubsequentAddressFamily.VALUE, FlowspecSubsequentAddressFamily.QNAME,
            DestinationFlowspecIpv6.QNAME,
            new SimpleFlowspecIpv6NlriParser(SAFI.FLOWSPEC));
    }
}
