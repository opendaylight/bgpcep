/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4MULTICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV6MULTICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.McastMplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;

public class TableTypeActivatorTest {
    private static final BgpTableType IPV4 = new BgpTableTypeImpl(Ipv4AddressFamily.VALUE,
            MplsLabeledVpnSubsequentAddressFamily.VALUE);
    private static final BgpTableType IPV6 = new BgpTableTypeImpl(Ipv6AddressFamily.VALUE,
            MplsLabeledVpnSubsequentAddressFamily.VALUE);

    private static final BgpTableType MCAST_L3VPN_IPV4 = new BgpTableTypeImpl(
            Ipv4AddressFamily.VALUE, McastMplsLabeledVpnSubsequentAddressFamily.VALUE);
    private static final BgpTableType MCAST_L3VPN_IPV6 = new BgpTableTypeImpl(
            Ipv6AddressFamily.VALUE, McastMplsLabeledVpnSubsequentAddressFamily.VALUE);

    @Test
    public void testActivator() {
        var registry = BGPTableTypeRegistryConsumer.of(new TableTypeActivator());
        assertEquals(L3VPNIPV4UNICAST.VALUE, registry.getAfiSafiType(IPV4));
        assertEquals(L3VPNIPV6UNICAST.VALUE, registry.getAfiSafiType(IPV6));
        assertEquals(IPV4, registry.getTableType(L3VPNIPV4UNICAST.VALUE));
        assertEquals(IPV6, registry.getTableType(L3VPNIPV6UNICAST.VALUE));

        assertEquals(L3VPNIPV4MULTICAST.VALUE, registry.getAfiSafiType(MCAST_L3VPN_IPV4));
        assertEquals(L3VPNIPV6MULTICAST.VALUE, registry.getAfiSafiType(MCAST_L3VPN_IPV6));
        assertEquals(MCAST_L3VPN_IPV4, registry.getTableType(L3VPNIPV4MULTICAST.VALUE));
        assertEquals(MCAST_L3VPN_IPV6, registry.getTableType(L3VPNIPV6MULTICAST.VALUE));
    }
}
