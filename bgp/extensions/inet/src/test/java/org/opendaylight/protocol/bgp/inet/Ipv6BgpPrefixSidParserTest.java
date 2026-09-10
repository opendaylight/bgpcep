/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.bgp.inet.codec.Ipv6BgpPrefixSidParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.Ipv6SidTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yangtools.binding.CaseObject;
import org.opendaylight.yangtools.binding.lib.AbstractAugmentable;

final class Ipv6BgpPrefixSidParserTest {
    private static final class UnhandledBgpPrefixSidTlv extends AbstractAugmentable<UnhandledBgpPrefixSidTlv>
            implements CaseObject<BgpPrefixSidTlvs, BgpPrefixSidTlv, UnhandledBgpPrefixSidTlv>, BgpPrefixSidTlv {
        @Override
        public Class<UnhandledBgpPrefixSidTlv> implementedInterface() {
            return UnhandledBgpPrefixSidTlv.class;
        }

        @Override
        public int javaHC() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean javaEQ(final UnhandledBgpPrefixSidTlv obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String javaTS() {
            throw new UnsupportedOperationException();
        }
    }

    private final Ipv6BgpPrefixSidParser handler = new Ipv6BgpPrefixSidParser();

    @Test
    void testWrongTlvType() {
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> handler.serializeBgpPrefixSidTlv(new UnhandledBgpPrefixSidTlv(), Unpooled.EMPTY_BUFFER));
        assertEquals("Incoming TLV is not Ipv6SidTlv", ex.getMessage());
    }

    @Test
    void testHandling() {
        final var serialized = Unpooled.buffer(3);
        handler.serializeBgpPrefixSidTlv(new Ipv6SidTlvBuilder().setProcessIpv6HeadAbility(Boolean.TRUE).build(),
            serialized);
        assertArrayEquals(new byte[] { 0, (byte) 0x80, 0 }, serialized.array());
        assertTrue(handler.parseBgpPrefixSidTlv(serialized).getProcessIpv6HeadAbility());
    }

    @Test
    void testType() {
        assertEquals(2, handler.getType());
    }
}
