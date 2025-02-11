/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import org.junit.Test;

class BitArrayTest {
    @Test
    void testCreateBitArray() {
        assertArrayEquals(new byte[1], new BitArray(5).array());
        assertArrayEquals(new byte[3], new BitArray(23).array());
        assertArrayEquals(new byte[3], new BitArray(24).array());
        assertArrayEquals(new byte[4], new BitArray(25).array());

        final var a = new byte[] {1, 2, 3, 4};
        assertArrayEquals(a, BitArray.valueOf(a).array());

        final byte b = 44;
        assertEquals(b, BitArray.valueOf(b).toByte());

        final var buf = Unpooled.wrappedBuffer(a);
        assertArrayEquals(new byte[] {1, 2}, BitArray.valueOf(buf, 12).array());

        final var res = Unpooled.buffer();
        final var i = BitArray.valueOf(a);
        i.toByteBuf(res);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, ByteArray.readAllBytes(res));
    }

    @Test
    void testSetAndGet() {
        final var ba = new BitArray(10);
        ba.set(0, null);
        ba.set(1, Boolean.TRUE);
        ba.set(2, Boolean.FALSE);
        ba.set(3, Boolean.TRUE);
        ba.set(7, Boolean.TRUE);
        ba.set(8, Boolean.TRUE);
        ba.set(9, Boolean.TRUE);

        assertEquals("BitArray [1 1000111]", ba.toString());

        assertFalse(ba.get(0));
        assertTrue(ba.get(1));
        assertFalse(ba.get(2));
        assertTrue(ba.get(3));
        assertTrue(ba.get(7));
        assertTrue(ba.get(8));
        assertTrue(ba.get(9));
    }
}
