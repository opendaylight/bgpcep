/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

public class NextHopUtilTest {

 /*   private static final byte[] ipv4B = { 42, 42, 42, 42 };
    private static final byte[] ipv6B = { 0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };
    private static final byte[] ipv6lB = { 0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, (byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0xc0, 1, 0x0b, (byte) 0xff, (byte) 0xfe, 0x7e, 0, 0 };

    private static final Ipv4Address ipv4 = new Ipv4Address("42.42.42.42");
    private static final Ipv6Address ipv6 = new Ipv6Address("2001:db8::1");
    private static final Ipv6Address ipv6l = new Ipv6Address("fe80::c001:bff:fe7e:0");

    @Test
    public void testSerializeNextHop() {
        CNextHop hop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(ipv4).build()).build();
        final ByteBuf buffer = Unpooled.buffer();
        NextHopUtil.serializeNextHop(hop, buffer);
        assertArrayEquals(ipv4B, ByteArray.readAllBytes(buffer));

        hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(ipv6).build()).build();
        buffer.clear();
        NextHopUtil.serializeNextHop(hop, buffer);
        assertArrayEquals(ipv6B, ByteArray.readAllBytes(buffer));

        hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(ipv6).setLinkLocal(ipv6l).build()).build();
        buffer.clear();
        NextHopUtil.serializeNextHop(hop, buffer);
        assertArrayEquals(ipv6lB, ByteArray.readAllBytes(buffer));

        buffer.clear();
        NextHopUtil.serializeNextHop(new CNextHop() {

            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return null;
            }
        }, buffer);
        assertEquals(0, buffer.readableBytes());

        hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setLinkLocal(ipv6l).build()).build();
        buffer.clear();
        try {
            NextHopUtil.serializeNextHop(hop, buffer);
            fail("Exception should happen");
        } catch (final IllegalArgumentException e) {
            assertEquals("Ipv6 Next Hop is missing Global address.", e.getMessage());
        }
    }

    @Test
    public void testParseNextHop() {
        CNextHop hop = null;
        try {
            hop = NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(ipv4B));
        } catch (final IllegalArgumentException e) {
            fail("This exception should not happen");
        }
        assertEquals(ipv4, ((Ipv4NextHopCase) hop).getIpv4NextHop().getGlobal());

        try {
            hop = NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(ipv6B));
        } catch (final IllegalArgumentException e) {
            fail("This exception should not happen");
        }
        assertEquals(ipv6, ((Ipv6NextHopCase) hop).getIpv6NextHop().getGlobal());
        assertNull(((Ipv6NextHopCase) hop).getIpv6NextHop().getLinkLocal());

        try {
            hop = NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(ipv6lB));
        } catch (final IllegalArgumentException e) {
            fail("This exception should not happen");
        }
        assertEquals(ipv6, ((Ipv6NextHopCase) hop).getIpv6NextHop().getGlobal());
        assertEquals(ipv6l, ((Ipv6NextHopCase) hop).getIpv6NextHop().getLinkLocal());

        final byte[] wrong = new byte[] { (byte) 0x20, (byte) 0x01, (byte) 0x0d };
        try {
            NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(wrong));
            fail("Exception should happen");
        } catch (final IllegalArgumentException e) {
            assertEquals("Cannot parse NEXT_HOP attribute. Wrong bytes length: 3", e.getMessage());
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<NextHopUtil> c = NextHopUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }*/
}
