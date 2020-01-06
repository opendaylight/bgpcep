/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.rsvp.util;

import static org.junit.Assert.assertArrayEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;

public class XROSubobjectUtilTest {
    @Test
    public void testXROSubobjectUtil() {
        byte[] expected = { (byte) 0x82, 6, 0, 1, 2, 3 };
        final ByteBuf out = Unpooled.buffer();
        final ByteBuf body = Unpooled.copiedBuffer(new byte[] { 0, 1, 2, 3 });
        body.markReaderIndex();
        XROSubobjectUtil.formatSubobject(2, true, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));

        expected = new byte[]{ 2, 6, 0, 1, 2, 3 };
        out.clear();
        body.resetReaderIndex();
        XROSubobjectUtil.formatSubobject(2, false, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));

        out.clear();
        body.resetReaderIndex();
        XROSubobjectUtil.formatSubobject(2, null, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }
}
