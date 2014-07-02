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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.AttributeFlags;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;

public class UtilsTest {

    @Test
    public void testCapabilityUtil() {
        final byte[] result = new byte[] { 1, 2, 4, 8 };
        ByteBuf aggregator = Unpooled.buffer();
        CapabilityUtil.formatCapability(1, Unpooled.wrappedBuffer(new byte[] { 4, 8 }),aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testMessageUtil() {
        final byte[] result = new byte[] { UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, 0, 23, 3, 32, 5, 14, 21 };
        ByteBuf formattedMessage = Unpooled.buffer();
        MessageUtil.formatMessage(3, Unpooled.wrappedBuffer(new byte[] { 32, 5, 14, 21 }), formattedMessage);
        assertArrayEquals(result, ByteArray.getAllBytes(formattedMessage));
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
        ByteBuf aggregator = Unpooled.buffer();
        ParameterUtil.formatParameter(1, Unpooled.wrappedBuffer(new byte[] { 4, 8 }), aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testAttributeUtil() {
        final byte[] result = new byte[] { 0x40, 03, 04, 10, 00, 00, 02 };
        ByteBuf aggregator = Unpooled.buffer();
        AttributeUtil.formatAttribute(64 , 3 , Unpooled.wrappedBuffer(new byte[] { 10, 0, 0, 2 }), aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testAttributeUtilExtended() {
        final byte[] value = new byte[258];
        Arrays.fill(value, 0, 258, UnsignedBytes.MAX_VALUE);
        final byte[] header = new byte[] { (byte) 0x50, 03, 01, 02 };
        final byte[] result = new byte[262];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(value, 0, result, 4, value.length);
        ByteBuf aggregator = Unpooled.buffer();
        AttributeUtil.formatAttribute(AttributeFlags.TRANSITIVE , 3 , Unpooled.wrappedBuffer(value), aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }
}
