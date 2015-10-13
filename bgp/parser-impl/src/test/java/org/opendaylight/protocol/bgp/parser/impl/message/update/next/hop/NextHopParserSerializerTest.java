/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.next.hop;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class NextHopParserSerializerTest {

    public static final byte[] ipv6lB = {0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, (byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0xc0, 1, 0x0b, (byte) 0xff, (byte) 0xfe, 0x7e, 0, 0};

    public static final Ipv6Address ipv6 = new Ipv6Address("2001:db8::1");
    public static final Ipv6Address ipv6l = new Ipv6Address("fe80::c001:bff:fe7e:0");

    Ipv4NextHopParserSerializer ipv4NextHopParserSerializer;
    Ipv6NextHopParserSerializer ipv6NextHopParserSerializer;
    CNextHop hop;
    ByteBuf buffer;

    @Before
    public final void setUp() {
        ipv4NextHopParserSerializer = new Ipv4NextHopParserSerializer();
        ipv6NextHopParserSerializer = new Ipv6NextHopParserSerializer();
        buffer = Unpooled.buffer();
    }

    @Test
    public void testSerializeIpv4NextHopCase() throws BGPParsingException {
        final byte[] ipv4B = {42, 42, 42, 42};
        hop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
            .setGlobal(new Ipv4Address("42.42.42.42")).build()).build();

        ipv4NextHopParserSerializer.serializeNextHop(hop, buffer);
        assertArrayEquals(ipv4B, ByteArray.readAllBytes(buffer));

        final CNextHop parsedHop = ipv4NextHopParserSerializer.parseNextHop(Unpooled.wrappedBuffer(ipv4B));
        assertTrue(hop instanceof Ipv4NextHopCase);
        assertEquals(hop, parsedHop);
    }

    @Test
    public void testSerializeIpv6LinkNextHopCase() throws BGPParsingException {
        hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(ipv6).setLinkLocal(ipv6l).build()).build();
        buffer.clear();
        ipv6NextHopParserSerializer.serializeNextHop(hop, buffer);
        assertArrayEquals(ipv6lB, ByteArray.readAllBytes(buffer));

        final CNextHop parsedHop = ipv6NextHopParserSerializer.parseNextHop(Unpooled.wrappedBuffer(ipv6lB));
        assertTrue(parsedHop instanceof Ipv6NextHopCase);
        assertEquals(hop, parsedHop);
    }

    @Test
    public void testSerializeIpv4NextHopEmpty() {
        buffer.clear();
        try {
            ipv4NextHopParserSerializer.serializeNextHop(new CNextHop() {
                @Override
                public Class<? extends DataContainer> getImplementedInterface() {
                    return null;
                }
            }, buffer);
        } catch (final IllegalArgumentException e) {
            assertEquals("cNextHop is not a Ipv4 NextHop object.", e.getMessage());
        }
    }

    @Test
    public void testSerializeIpv6NextHopEmpty() {
        buffer.clear();
        try {
            ipv6NextHopParserSerializer.serializeNextHop(new CNextHop() {
                @Override
                public Class<? extends DataContainer> getImplementedInterface() {
                    return null;
                }
            }, buffer);
        } catch (final IllegalArgumentException e) {
            assertEquals("cNextHop is not a Ipv6 NextHop object.", e.getMessage());
        }
    }
}