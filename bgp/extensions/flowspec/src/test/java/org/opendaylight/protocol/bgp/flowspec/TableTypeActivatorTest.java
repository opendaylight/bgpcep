/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV4FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV4L3VPNFLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV6FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV6L3VPNFLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;

public class TableTypeActivatorTest {
    private static final BgpTableType IPV4_FLOW = new BgpTableTypeImpl(Ipv4AddressFamily.VALUE,
        FlowspecSubsequentAddressFamily.VALUE);
    private static final BgpTableType IPV6_FLOW = new BgpTableTypeImpl(Ipv6AddressFamily.VALUE,
        FlowspecSubsequentAddressFamily.VALUE);
    private static final BgpTableType IPV4_VPN_FLOW = new BgpTableTypeImpl(Ipv4AddressFamily.VALUE,
        FlowspecL3vpnSubsequentAddressFamily.VALUE);
    private static final BgpTableType IPV6_VPN_FLOW = new BgpTableTypeImpl(Ipv6AddressFamily.VALUE,
        FlowspecL3vpnSubsequentAddressFamily.VALUE);

    @Test
    public void testActivator() {
        var registry = BGPTableTypeRegistryConsumer.of(new TableTypeActivator());
        assertEquals(IPV4FLOW.VALUE, registry.getAfiSafiType(IPV4_FLOW));
        assertEquals(IPV6FLOW.VALUE, registry.getAfiSafiType(IPV6_FLOW));
        assertEquals(IPV4L3VPNFLOW.VALUE, registry.getAfiSafiType(IPV4_VPN_FLOW));
        assertEquals(IPV6L3VPNFLOW.VALUE, registry.getAfiSafiType(IPV6_VPN_FLOW));

        assertEquals(IPV4_FLOW, registry.getTableType(IPV4FLOW.VALUE));
        assertEquals(IPV6_FLOW, registry.getTableType(IPV6FLOW.VALUE));
        assertEquals(IPV4_VPN_FLOW, registry.getTableType(IPV4L3VPNFLOW.VALUE));
        assertEquals(IPV6_VPN_FLOW, registry.getTableType(IPV6L3VPNFLOW.VALUE));
    }
}
