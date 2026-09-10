/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet.codec.nexthop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.binding.CaseObject;
import org.opendaylight.yangtools.binding.lib.AbstractAugmentable;

class NextHopParserSerializerTest {
    private static final class UnhandledNextHopCase extends AbstractAugmentable<UnhandledNextHopCase>
            implements CaseObject<NextHop, CNextHop, UnhandledNextHopCase>, CNextHop {
        @Override
        public Class<UnhandledNextHopCase> implementedInterface() {
            return UnhandledNextHopCase.class;
        }

        @Override
        public int javaHC() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean javaEQ(final UnhandledNextHopCase obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String javaTS() {
            throw new UnsupportedOperationException();
        }
    }

    private static final byte[] IPV6LB = {0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, (byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0xc0, 1, 0x0b, (byte) 0xff, (byte) 0xfe, 0x7e, 0, 0};

    private static final Ipv6AddressNoZone IPV6 = new Ipv6AddressNoZone("2001:db8::1");
    private static final Ipv6AddressNoZone IPV6L = new Ipv6AddressNoZone("fe80::c001:bff:fe7e:0");

    private final Ipv4NextHopParserSerializer ipv4NextHopParserSerializer = new Ipv4NextHopParserSerializer();
    private final Ipv6NextHopParserSerializer ipv6NextHopParserSerializer = new Ipv6NextHopParserSerializer();
    private final ByteBuf buffer = Unpooled.buffer();

    @Test
    void testSerializeIpv4NextHopCase() throws BGPParsingException {
        final byte[] ipv4B = {42, 42, 42, 42};
        final var hop = new Ipv4NextHopCaseBuilder()
            .setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4AddressNoZone("42.42.42.42")).build())
            .build();

        ipv4NextHopParserSerializer.serializeNextHop(hop, buffer);
        assertArrayEquals(ipv4B, ByteArray.readAllBytes(buffer));

        final var parsedHop = ipv4NextHopParserSerializer.parseNextHop(Unpooled.wrappedBuffer(ipv4B));
        assertInstanceOf(Ipv4NextHopCase.class, parsedHop);
        assertEquals(hop, parsedHop);
    }

    @Test
    void testSerializeIpv6LinkNextHopCase() throws BGPParsingException {
        final var hop = new Ipv6NextHopCaseBuilder()
            .setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(IPV6).setLinkLocal(IPV6L).build())
            .build();
        ipv6NextHopParserSerializer.serializeNextHop(hop, buffer);
        assertArrayEquals(IPV6LB, ByteArray.readAllBytes(buffer));

        final var parsedHop = assertInstanceOf(Ipv6NextHopCase.class,
            ipv6NextHopParserSerializer.parseNextHop(Unpooled.wrappedBuffer(IPV6LB)));
        assertEquals(hop, parsedHop);
    }

    @Test
    void testSerializeIpv4NextHopEmpty() {
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> ipv4NextHopParserSerializer.serializeNextHop(new UnhandledNextHopCase(), buffer));
        assertEquals("cNextHop is not a Ipv4 NextHop object.", ex.getMessage());
    }

    @Test
    void testSerializeIpv6NextHopEmpty() {
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> ipv6NextHopParserSerializer.serializeNextHop(new UnhandledNextHopCase(), buffer));
        assertEquals("cNextHop is not a Ipv6 NextHop object.", ex.getMessage());
    }
}
