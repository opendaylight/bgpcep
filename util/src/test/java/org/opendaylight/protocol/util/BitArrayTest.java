/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

public class BitArrayTest {

    @Test
    public void testCreateBitArray() {
        Assert.assertArrayEquals(new byte[1], new BitArray(5).array());
        Assert.assertArrayEquals(new byte[3], new BitArray(23).array());
        Assert.assertArrayEquals(new byte[3], new BitArray(24).array());
        Assert.assertArrayEquals(new byte[4], new BitArray(25).array());

        final byte[] a = new byte[] {1, 2, 3, 4};
        Assert.assertArrayEquals(a, BitArray.valueOf(a).array());

        final byte b = 44;
        Assert.assertEquals(b, BitArray.valueOf(b).toByte());

        final ByteBuf buf = Unpooled.wrappedBuffer(a);
        Assert.assertArrayEquals(new byte[] {1, 2}, BitArray.valueOf(buf, 12).array());

        final ByteBuf res = Unpooled.buffer();
        final BitArray i = BitArray.valueOf(a);
        i.toByteBuf(res);
        Assert.assertArrayEquals(new byte[] {1, 2, 3, 4}, ByteArray.readAllBytes(res));
    }

    @Test
    public void testSetAndGet() {
        final BitArray ba = new BitArray(10);
        ba.set(0, null);
        ba.set(1, Boolean.TRUE);
        ba.set(2, Boolean.FALSE);
        ba.set(3, Boolean.TRUE);
        ba.set(7, Boolean.TRUE);
        ba.set(8, Boolean.TRUE);
        ba.set(9, Boolean.TRUE);

        Assert.assertFalse(ba.get(0));
        Assert.assertTrue(ba.get(1));
        Assert.assertFalse(ba.get(2));
        Assert.assertTrue(ba.get(3));
        Assert.assertTrue(ba.get(7));
        Assert.assertTrue(ba.get(8));
        Assert.assertTrue(ba.get(9));
    }
}
