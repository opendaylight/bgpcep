/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;

public class NextHopUtilTest {

    private static final byte[] IPV4B = {42, 42, 42, 42};
    private static final byte[] IPV6B = {0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
    private static final byte[] IPV6LB = {0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, (byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0xc0, 1, 0x0b, (byte) 0xff, (byte) 0xfe, 0x7e, 0, 0};

    private static final Ipv4Address IPV4 = new Ipv4Address("42.42.42.42");
    private static final Ipv6Address IPV6 = new Ipv6Address("2001:db8::1");
    private static final Ipv6Address IPV6L = new Ipv6Address("fe80::c001:bff:fe7e:0");

    @Test
    public void testSerializeNextHop() {
        CNextHop hop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(IPV4).build()).build();
        final ByteBuf buffer = Unpooled.buffer();
        NextHopUtil.serializeNextHop(hop, buffer);
        assertArrayEquals(IPV4B, ByteArray.readAllBytes(buffer));

        hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(IPV6).build()).build();
        buffer.clear();
        NextHopUtil.serializeNextHop(hop, buffer);
        assertArrayEquals(IPV6B, ByteArray.readAllBytes(buffer));

        hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(IPV6)
                .setLinkLocal(IPV6L).build()).build();
        buffer.clear();
        NextHopUtil.serializeNextHop(hop, buffer);
        assertArrayEquals(IPV6LB, ByteArray.readAllBytes(buffer));

        buffer.clear();

        hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setLinkLocal(IPV6L).build()).build();
        buffer.clear();
        try {
            NextHopUtil.serializeNextHop(hop, buffer);
            fail("Exception should happen");
        } catch (final IllegalArgumentException e) {
            assertEquals("Ipv6 Next Hop is missing Global address.", e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSerializeNextHopException() {
        NextHopUtil.serializeNextHop(() -> null, null);
    }

    @Test
    public void testParseNextHop() {
        CNextHop hop = null;
        try {
            hop = NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(IPV4B));
        } catch (final IllegalArgumentException e) {
            fail("This exception should not happen");
        }
        assertEquals(IPV4, ((Ipv4NextHopCase) hop).getIpv4NextHop().getGlobal());

        try {
            hop = NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(IPV6B));
        } catch (final IllegalArgumentException e) {
            fail("This exception should not happen");
        }
        assertEquals(IPV6, ((Ipv6NextHopCase) hop).getIpv6NextHop().getGlobal());
        assertNull(((Ipv6NextHopCase) hop).getIpv6NextHop().getLinkLocal());

        try {
            hop = NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(IPV6LB));
        } catch (final IllegalArgumentException e) {
            fail("This exception should not happen");
        }
        assertEquals(IPV6, ((Ipv6NextHopCase) hop).getIpv6NextHop().getGlobal());
        assertEquals(IPV6L, ((Ipv6NextHopCase) hop).getIpv6NextHop().getLinkLocal());

        final byte[] wrong = new byte[]{(byte) 0x20, (byte) 0x01, (byte) 0x0d};
        try {
            NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(wrong));
            fail("Exception should happen");
        } catch (final IllegalArgumentException e) {
            assertEquals("Cannot parse NEXT_HOP attribute. Wrong bytes length: 3", e.getMessage());
        }
    }
}
