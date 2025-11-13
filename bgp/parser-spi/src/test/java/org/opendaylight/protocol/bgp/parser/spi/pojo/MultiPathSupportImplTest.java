/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

class MultiPathSupportImplTest {
    @Test
    void testcreateParserMultiPathSupportNull() {
        assertThrows(NullPointerException.class, () -> MultiPathSupportImpl.createParserMultiPathSupport(null));
    }

    @Test
    void testIsTableTypeSupported() {
        final var ipv4Unicast = new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
        final var ipv4L3vpn = new BgpTableTypeImpl(Ipv4AddressFamily.VALUE,
            MplsLabeledVpnSubsequentAddressFamily.VALUE);
        final var ipv6Unicast = new BgpTableTypeImpl(Ipv6AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
        final var ipv6L3vpn = new BgpTableTypeImpl(Ipv6AddressFamily.VALUE,
            MplsLabeledVpnSubsequentAddressFamily.VALUE);
        final var multiPathSupport = MultiPathSupportImpl.createParserMultiPathSupport(List.of(
            createAddPathCapability(ipv4Unicast, SendReceive.Send),
            createAddPathCapability(ipv4L3vpn, SendReceive.Receive),
            createAddPathCapability(ipv6Unicast, SendReceive.Both)));

        assertTrue(multiPathSupport.isTableTypeSupported(ipv4Unicast));
        assertTrue(multiPathSupport.isTableTypeSupported(ipv6Unicast));
        assertFalse(multiPathSupport.isTableTypeSupported(ipv4L3vpn));
        assertFalse(multiPathSupport.isTableTypeSupported(ipv6L3vpn));
    }

    private static AddressFamilies createAddPathCapability(final BgpTableType afisafi, final SendReceive mode) {
        return new AddressFamiliesBuilder()
            .setAfi(afisafi.getAfi())
            .setSafi(afisafi.getSafi())
            .setSendReceive(mode)
            .build();
    }
}
