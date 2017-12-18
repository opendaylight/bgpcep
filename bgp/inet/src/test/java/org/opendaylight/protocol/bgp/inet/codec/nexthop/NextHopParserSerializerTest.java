/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.inet.codec.nexthop;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
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

public class NextHopParserSerializerTest {

    private static final byte[] IPV6LB = {0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, (byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0xc0, 1, 0x0b, (byte) 0xff, (byte) 0xfe, 0x7e, 0, 0};

    private static final Ipv6Address IPV6 = new Ipv6Address("2001:db8::1");
    private static final Ipv6Address IPV6L = new Ipv6Address("fe80::c001:bff:fe7e:0");

    private Ipv4NextHopParserSerializer ipv4NextHopParserSerializer;
    private Ipv6NextHopParserSerializer ipv6NextHopParserSerializer;
    private CNextHop hop;
    private ByteBuf buffer;

    @Before
    public final void setUp() {
        this.ipv4NextHopParserSerializer = new Ipv4NextHopParserSerializer();
        this.ipv6NextHopParserSerializer = new Ipv6NextHopParserSerializer();
        this.buffer = Unpooled.buffer();
    }

    @Test
    public void testSerializeIpv4NextHopCase() throws BGPParsingException {
        final byte[] ipv4B = {42, 42, 42, 42};
        this.hop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
            .setGlobal(new Ipv4Address("42.42.42.42")).build()).build();

        this.ipv4NextHopParserSerializer.serializeNextHop(this.hop, this.buffer);
        assertArrayEquals(ipv4B, ByteArray.readAllBytes(this.buffer));

        final CNextHop parsedHop = this.ipv4NextHopParserSerializer.parseNextHop(Unpooled.wrappedBuffer(ipv4B));
        assertTrue(this.hop instanceof Ipv4NextHopCase);
        assertEquals(this.hop, parsedHop);
    }

    @Test
    public void testSerializeIpv6LinkNextHopCase() throws BGPParsingException {
        this.hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder()
                .setGlobal(IPV6).setLinkLocal(IPV6L).build()).build();
        this.buffer.clear();
        this.ipv6NextHopParserSerializer.serializeNextHop(this.hop, this.buffer);
        assertArrayEquals(IPV6LB, ByteArray.readAllBytes(this.buffer));

        final CNextHop parsedHop = this.ipv6NextHopParserSerializer.parseNextHop(Unpooled.wrappedBuffer(IPV6LB));
        assertTrue(parsedHop instanceof Ipv6NextHopCase);
        assertEquals(this.hop, parsedHop);
    }

    @Test
    public void testSerializeIpv4NextHopEmpty() {
        this.buffer.clear();
        try {
            this.ipv4NextHopParserSerializer.serializeNextHop(() -> null, this.buffer);
        } catch (final IllegalArgumentException e) {
            assertEquals("cNextHop is not a Ipv4 NextHop object.", e.getMessage());
        }
    }

    @Test
    public void testSerializeIpv6NextHopEmpty() {
        this.buffer.clear();
        try {
            this.ipv6NextHopParserSerializer.serializeNextHop(() -> null, this.buffer);
        } catch (final IllegalArgumentException e) {
            assertEquals("cNextHop is not a Ipv6 NextHop object.", e.getMessage());
        }
    }
}