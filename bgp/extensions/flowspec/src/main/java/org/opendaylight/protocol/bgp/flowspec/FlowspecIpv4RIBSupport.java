/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.SAFI;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.bgp.rib.rib.loc.rib.tables.routes.FlowspecRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.ipv4.DestinationFlowspecIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;

public final class FlowspecIpv4RIBSupport
        extends AbstractFlowspecIpRIBSupport<SimpleFlowspecIpv4NlriParser,
        FlowspecRoutesCase,
        FlowspecRoutes,
        FlowspecRoute> {
    public FlowspecIpv4RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
            FlowspecRoutesCase.class, FlowspecRoutesCase.QNAME,
            FlowspecRoutes.class, FlowspecRoutes.QNAME,
            FlowspecRoute.class, FlowspecRoute.QNAME,
            Ipv4AddressFamily.VALUE, Ipv4AddressFamily.QNAME,
            FlowspecSubsequentAddressFamily.VALUE, FlowspecSubsequentAddressFamily.QNAME,
            DestinationFlowspecIpv4.QNAME,
            new SimpleFlowspecIpv4NlriParser(SAFI.FLOWSPEC));
    }
}
