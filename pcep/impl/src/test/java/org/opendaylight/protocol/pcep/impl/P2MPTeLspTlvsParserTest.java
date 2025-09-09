/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.parser.tlv.P2MPTeLspCapabilityParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.p2mp.pce.capability.tlv.P2mpPceCapabilityBuilder;

class P2MPTeLspTlvsParserTest {
    private static final byte[] SPC_TLV_BYTES = {0x0, 0x06, 0x0, 0x2, 0x0, 0x0, 0x0, 0x0};

    @Test
    void testSrPceCapabilityParser() throws PCEPDeserializerException {
        final var parser = new P2MPTeLspCapabilityParser();
        final var capability = new P2mpPceCapabilityBuilder().build();

        assertEquals(capability, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(SPC_TLV_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(capability, buff);
        assertArrayEquals(SPC_TLV_BYTES, ByteArray.getAllBytes(buff));
    }
}