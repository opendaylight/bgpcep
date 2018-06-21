/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import org.junit.Test;

public class AbstractFlowspecNlriParserTest {

    @Test(expected = IllegalStateException.class)
    public void zeroNlriLengthTest() throws Exception {
        // invalid zero NRLI length
        AbstractFlowspecNlriParser.readNlriLength(Unpooled.wrappedBuffer(new byte[]{0x00, 0x0}));
    }

    @Test
    public void readNlriLength() throws Exception {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(5003);
        byteBuffer.put(new byte[]{(byte) 0x01});   // length = 1
        byteBuffer.put(new byte[]{(byte) 0xf0, (byte) 0xf0});   // length = 240
        byteBuffer.put(new byte[]{(byte) 0xf0, (byte) 0xf1});   // length = 241
        byteBuffer.put(new byte[]{(byte) 0xff, (byte) 0xff});   // length = 4095
        byteBuffer.rewind();
        final ByteBuf byteBuf = Unpooled.wrappedBuffer(byteBuffer);
        assertEquals(1, AbstractFlowspecNlriParser.readNlriLength(byteBuf));
        assertEquals(240, AbstractFlowspecNlriParser.readNlriLength(byteBuf));
        assertEquals(241, AbstractFlowspecNlriParser.readNlriLength(byteBuf));
        assertEquals(4095, AbstractFlowspecNlriParser.readNlriLength(byteBuf));
    }

}
