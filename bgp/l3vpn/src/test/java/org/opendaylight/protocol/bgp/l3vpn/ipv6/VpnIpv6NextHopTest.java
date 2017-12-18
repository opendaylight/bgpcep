/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv6;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.Inet6Address;
import org.junit.Test;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;

public class VpnIpv6NextHopTest {

    private static final VpnIpv6NextHopParserSerializer HANDLER = new VpnIpv6NextHopParserSerializer();

    @Test
    public void testSerializeIpv6NextHopCase() throws Exception {
        final String TEST_IPV6 = "2001::1234:5678:90ab:cdef";   // put some random valid IPv6 address here

        final ByteBuf buffer = Unpooled.buffer();
        final byte[] nextHop = new byte[Ipv6Util.IPV6_LENGTH + RouteDistinguisherUtil.RD_LENGTH];
        // now copy the IPv6 address to the byte array
        System.arraycopy(Inet6Address.getByName(TEST_IPV6).getAddress(), 0, nextHop,
                RouteDistinguisherUtil.RD_LENGTH, Ipv6Util.IPV6_LENGTH);
        final CNextHop hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder()
                .setGlobal(new Ipv6Address(TEST_IPV6)).build()).build();

        HANDLER.serializeNextHop(hop, buffer);
        assertArrayEquals(nextHop, ByteArray.readAllBytes(buffer));

        final CNextHop parsedHop = HANDLER.parseNextHop(Unpooled.wrappedBuffer(nextHop));
        assertTrue(hop instanceof Ipv6NextHopCase);
        assertEquals(hop, parsedHop);
    }
}
