/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.attribute.next.hop;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class LinkstateNextHopParserTest {

    public static final byte[] ipv6B = {0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
    public static final byte[] ipv6lB = {0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, (byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, (byte) 0xc0, 1, 0x0b, (byte) 0xff, (byte) 0xfe, 0x7e, 0, 0};

    public static final Ipv6Address ipv6 = new Ipv6Address("2001:db8::1");
    public static final Ipv6Address ipv6l = new Ipv6Address("fe80::c001:bff:fe7e:0");

    LinkstateNextHopParser linkstateNextHopParser;
    CNextHop hop;
    ByteBuf buffer;

    @Before
    public final void setUp() {
        linkstateNextHopParser = new LinkstateNextHopParser();
        buffer = Unpooled.buffer();
    }

    @Test
    public void testSerializeIpv4NextHopCase() throws BGPParsingException {
        final byte[] ipv4B = {42, 42, 42, 42};
        hop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
            .setGlobal(new Ipv4Address("42.42.42.42")).build()).build();

        linkstateNextHopParser.serializeNextHop(hop, buffer);
        assertArrayEquals(ipv4B, ByteArray.readAllBytes(buffer));

        final CNextHop parsedHop = linkstateNextHopParser.parseNextHop(Unpooled.wrappedBuffer(ipv4B));
        assertTrue(parsedHop instanceof Ipv4NextHopCase);
        assertEquals(hop, parsedHop);
    }

    @Test
    public void testSerializeIpv6NextHopCase() throws BGPParsingException {
        hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(ipv6).setLinkLocal(ipv6l).build()).build();
        buffer.clear();
        linkstateNextHopParser.serializeNextHop(hop, buffer);
        assertArrayEquals(ipv6lB, ByteArray.readAllBytes(buffer));

        final CNextHop parsedHop = linkstateNextHopParser.parseNextHop(Unpooled.wrappedBuffer(ipv6B));
        assertTrue(parsedHop instanceof Ipv6NextHopCase);
        final Ipv6NextHop nextHopIpv6 = ((Ipv6NextHopCase) parsedHop).getIpv6NextHop();
        assertEquals(ipv6, nextHopIpv6.getGlobal());
        assertNull(nextHopIpv6.getLinkLocal());
    }

    @Test
    public void testSerializeNextHopEmpty() {
        buffer.clear();
        try {
            linkstateNextHopParser.serializeNextHop(new CNextHop() {
                @Override
                public Class<? extends DataContainer> getImplementedInterface() {
                    return null;
                }
            }, buffer);
        } catch (final IllegalArgumentException e) {
            assertEquals("cNextHop is not a Linkstate Ipv6 NextHop object.", e.getMessage());
        }
    }

    @Test
    public void testSerializeNextHopGlobalAddressEmpty() {
        hop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setLinkLocal(ipv6l).build()).build();
        buffer.clear();
        try {
            linkstateNextHopParser.serializeNextHop(hop, buffer);
            fail("Exception should happen");
        } catch (final IllegalArgumentException e) {
            assertEquals("Ipv6 Next Hop is missing Global address.", e.getMessage());
        }
    }
}