/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static org.junit.Assert.assertArrayEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;

public class UtilsTest {

    @Test
    public void testLabelUtil() {
        byte[] expected = { (byte) 0x81, 0x04, 0x01, 0x02, 0x03, 0x04 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4 });
        LabelUtil.formatLabel(4, true, true, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testMessageUtil() {
        byte[] expected = { (byte) 0x20, 0x08, 0, 0x0a, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4, 5, 6 });
        MessageUtil.formatMessage(8, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testObjectUtil() {
        byte[] expected = { 0x08, 0x13, 0, 0x06, 0x01, 0x02 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2 });
        ObjectUtil.formatSubobject(1, 8, true, true, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testObjectUtilFalse() {
        byte[] expected = { 0x08, 0x10, 0, 0x06, 0x01, 0x02 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2 });
        ObjectUtil.formatSubobject(1, 8, false, false, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }
}
