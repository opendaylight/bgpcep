/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bgp.inet.codec.Ipv6BgpPrefixSidParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.Ipv6SidTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;

public final class Ipv6BgpPrefixSidParserTest {

    private final Ipv6BgpPrefixSidParser handler = new Ipv6BgpPrefixSidParser();

    private final byte[] expected = new byte[] {0, (byte)0x80, 0};

    @Test(expected=IllegalArgumentException.class)
    public void testWrongTlvType() {
        this.handler.serializeBgpPrefixSidTlv(() -> BgpPrefixSidTlv.class, Unpooled.EMPTY_BUFFER);
    }

    @Test
    public void testHandling() {
        final Ipv6SidTlvBuilder tlv = new Ipv6SidTlvBuilder();
        tlv.setProcessIpv6HeadAbility(Boolean.TRUE);
        final ByteBuf serialized = Unpooled.buffer(3);
        this.handler.serializeBgpPrefixSidTlv(tlv.build(), serialized);
        assertArrayEquals(this.expected, serialized.array());
        assertTrue(this.handler.parseBgpPrefixSidTlv(serialized).isProcessIpv6HeadAbility());
    }

    @Test
    public void testType() {
        assertEquals(2, this.handler.getType());
    }
}
