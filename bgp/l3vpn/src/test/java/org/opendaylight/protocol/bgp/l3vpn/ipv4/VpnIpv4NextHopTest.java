/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv4;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;

public class VpnIpv4NextHopTest {

    private static final VpnIpv4NextHopParserSerializer HANDLER = new VpnIpv4NextHopParserSerializer();

    @Test
    public void testSerializeIpv4NextHopCase() throws BGPParsingException {
        final ByteBuf buffer = Unpooled.buffer();
        final byte[] nextHop = {0, 0, 0, 0, 0, 0, 0, 0, 42, 42, 42, 42};
        final CNextHop hop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4Address("42.42.42.42")).build()).build();

        HANDLER.serializeNextHop(hop, buffer);
        assertArrayEquals(nextHop, ByteArray.readAllBytes(buffer));

        final CNextHop parsedHop = HANDLER.parseNextHop(Unpooled.wrappedBuffer(nextHop));
        assertTrue(hop instanceof Ipv4NextHopCase);
        assertEquals(hop, parsedHop);
    }
}
