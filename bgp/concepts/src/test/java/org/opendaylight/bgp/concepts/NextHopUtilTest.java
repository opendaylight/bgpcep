/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import io.netty.buffer.Unpooled;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;

public class NextHopUtilTest {

    @Test
    public void testNlriUtil() {
        CNextHop hop = null;
        final byte[] ipv4 = new byte[] { 42, 42, 42, 42 };
        try {
            hop = NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(ipv4));
        } catch (final IllegalArgumentException e) {
            fail("This exception should not happen");
        }
        assertEquals("42.42.42.42", ((Ipv4NextHopCase) hop).getIpv4NextHop().getGlobal().getValue());

        final byte[] ipv6 = new byte[] { (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 01 };
        try {
            hop = NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(ipv6));
        } catch (final IllegalArgumentException e) {
            fail("This exception should not happen");
        }
        assertEquals("2001:db8::1", ((Ipv6NextHopCase) hop).getIpv6NextHop().getGlobal().getValue());
        assertNull(((Ipv6NextHopCase) hop).getIpv6NextHop().getLinkLocal());

        final byte[] ipv6l = new byte[] { (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,
            01, (byte) 0xfe, (byte) 0x80, 00, 00, 00, 00, 00, 00, (byte) 0xc0, 01, 0x0b, (byte) 0xff, (byte) 0xfe, 0x7e, 00, 00 };
        try {
            hop = NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(ipv6l));
        } catch (final IllegalArgumentException e) {
            fail("This exception should not happen");
        }
        assertEquals("2001:db8::1", ((Ipv6NextHopCase) hop).getIpv6NextHop().getGlobal().getValue());
        assertEquals("fe80::c001:bff:fe7e:0", ((Ipv6NextHopCase) hop).getIpv6NextHop().getLinkLocal().getValue());

        final byte[] wrong = new byte[] { (byte) 0x20, (byte) 0x01, (byte) 0x0d };
        try {
            NextHopUtil.parseNextHop(Unpooled.wrappedBuffer(wrong));
            fail("Exception should happen");
        } catch (final IllegalArgumentException e) {
            assertEquals("Cannot parse NEXT_HOP attribute. Wrong bytes length: 3", e.getMessage());
        }
    }
}
