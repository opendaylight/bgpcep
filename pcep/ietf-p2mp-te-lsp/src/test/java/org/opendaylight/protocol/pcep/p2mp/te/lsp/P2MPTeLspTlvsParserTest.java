/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.p2mp.te.lsp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.pcep.p2mp.te.lsp.P2MPTeLspCapabilityParser.P2MP_CAPABILITY;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;

public class P2MPTeLspTlvsParserTest {
    private static final byte[] SPC_TLV_BYTES = {0x0, 0x06, 0x0, 0x2, 0x0, 0x0, 0x0, 0x0};

    @Test
    public void testSrPceCapabilityParser() throws PCEPDeserializerException {
        final P2MPTeLspCapabilityParser parser = new P2MPTeLspCapabilityParser();
        assertEquals(P2MP_CAPABILITY, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(SPC_TLV_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(P2MP_CAPABILITY, buff);
        assertArrayEquals(SPC_TLV_BYTES, ByteArray.getAllBytes(buff));
    }
}