/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.common.primitives.UnsignedBytes;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;

public class UtilsTest {

    @Test
    public void testCapabilityUtil() {
        final byte[] result = new byte[] { 1, 2, 4, 8 };
        assertArrayEquals(result, CapabilityUtil.formatCapability(1, new byte[] { 4, 8 }));
    }

    @Test
    public void testMessageUtil() {
        final byte[] result = new byte[] { UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, 0, 23, 3, 32, 5, 14, 21 };
        assertArrayEquals(result, MessageUtil.formatMessage(3, new byte[] { 32, 5, 14, 21 }));
    }

    @Test
    public void testNlriUtil() {
        final MpReachNlriBuilder builder = new MpReachNlriBuilder();
        final byte[] ipv4 = new byte[] { 42, 42, 42, 42 };
        try {
            NlriUtil.parseNextHop(ipv4, builder);
        } catch (final BGPParsingException e) {
            fail("This exception should not happen");
        }
        CNextHop hop = builder.getCNextHop();
        assertEquals("42.42.42.42", ((Ipv4NextHopCase) hop).getIpv4NextHop().getGlobal().getValue());

        final byte[] ipv6 = new byte[] { (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 01 };
        try {
            NlriUtil.parseNextHop(ipv6, builder);
        } catch (final BGPParsingException e) {
            fail("This exception should not happen");
        }
        hop = builder.getCNextHop();
        assertEquals("2001:db8::1", ((Ipv6NextHopCase) hop).getIpv6NextHop().getGlobal().getValue());
        assertNull(((Ipv6NextHopCase) hop).getIpv6NextHop().getLinkLocal());

        final byte[] ipv6l = new byte[] { (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,
            01, (byte) 0xfe, (byte) 0x80, 00, 00, 00, 00, 00, 00, (byte) 0xc0, 01, 0x0b, (byte) 0xff, (byte) 0xfe, 0x7e, 00, 00 };
        try {
            NlriUtil.parseNextHop(ipv6l, builder);
        } catch (final BGPParsingException e) {
            fail("This exception should not happen");
        }
        hop = builder.getCNextHop();
        assertEquals("2001:db8::1", ((Ipv6NextHopCase) hop).getIpv6NextHop().getGlobal().getValue());
        assertEquals("fe80::c001:bff:fe7e:0", ((Ipv6NextHopCase) hop).getIpv6NextHop().getLinkLocal().getValue());

        final byte[] wrong = new byte[] { (byte) 0x20, (byte) 0x01, (byte) 0x0d };
        try {
            NlriUtil.parseNextHop(wrong, builder);
            fail("Exception should happen");
        } catch (final BGPParsingException e) {
            assertEquals("Cannot parse NEXT_HOP attribute. Wrong bytes length: 3", e.getMessage());
        }
    }

    @Test
    public void testParameterUtil() {
        final byte[] result = new byte[] { 1, 2, 4, 8 };
        assertArrayEquals(result, ParameterUtil.formatParameter(1, new byte[] { 4, 8 }));
    }
}
