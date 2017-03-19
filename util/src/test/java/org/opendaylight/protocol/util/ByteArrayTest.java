/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.BitSet;
import org.junit.Before;
import org.junit.Test;

public class ByteArrayTest {

    final byte[] before = new byte[] { 15, 28, 4, 6, 9, 10 };

    @Test
    public void testReadBytes() {
        final ByteBuf buffer = Unpooled.copiedBuffer(this.before);
        buffer.readerIndex(1);
        assertArrayEquals(new byte[] { 28, 4, 6 }, ByteArray.readBytes(buffer, 3));
        assertEquals(4, buffer.readerIndex());

        assertArrayEquals(new byte[] { 9, 10 }, ByteArray.readAllBytes(buffer));
        assertEquals(buffer.readerIndex(), buffer.writerIndex());
    }

    @Test
    public void testGetBytes() {
        final ByteBuf buffer = Unpooled.copiedBuffer(this.before);
        buffer.readerIndex(1);
        assertArrayEquals(new byte[] { 28, 4, 6 }, ByteArray.getBytes(buffer, 3));
        assertEquals(1, buffer.readerIndex());

        assertArrayEquals(new byte[] { 28, 4, 6, 9, 10 }, ByteArray.getAllBytes(buffer));
        assertNotSame(buffer.readerIndex(), buffer.writerIndex());
    }

    @Test
    public void testSubByte() {
        byte[] after = ByteArray.subByte(this.before, 0, 3);
        byte[] expected = new byte[] { 15, 28, 4 };
        assertArrayEquals(expected, after);
        after = ByteArray.subByte(this.before, 5, 1);
        expected = new byte[] { 10 };
        assertArrayEquals(expected, after);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubByte2() {
        ByteArray.subByte(new byte[0], 2, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubByte3() {
        ByteArray.subByte(this.before, 2, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubByte4() {
        ByteArray.subByte(this.before, -1, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubByte5() {
        ByteArray.subByte(this.before, 9, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubByte6() {
        ByteArray.subByte(this.before, 2, 19);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubByte7() {
        ByteArray.subByte(this.before, 2, 7);
    }

    @Test
    public void testCutBytes() {
        byte[] after = ByteArray.cutBytes(this.before, 2);
        byte[] expected = new byte[] { 4, 6, 9, 10 };
        assertArrayEquals(expected, after);
        after = ByteArray.cutBytes(this.before, 6);
        expected = new byte[] {};
        assertArrayEquals(expected, after);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCutBytes2() {
        ByteArray.cutBytes(new byte[0], 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCutBytes3() {
        ByteArray.cutBytes(this.before, 9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCutBytes4() {
        ByteArray.cutBytes(this.before, 0);
    }

    private final BitSet inBitSet = new BitSet();

    @Before
    public void generateBitSet() {
        // 0x03
        this.inBitSet.set(6, 8);

        // 0xFF
        this.inBitSet.set(8, 16);

        // 0x01
        this.inBitSet.set(23);

        // 0x80
        this.inBitSet.set(24);
    }

    @Test
    public void testFileToBytes() throws IOException {
        final String FILE_TO_TEST = "src/test/resources/PCEStatefulCapabilityTlv1.bin";

        final File fileToCompareWith = new File(FILE_TO_TEST);
        final InputStream bytesIStream = new FileInputStream(fileToCompareWith);

        try {
            final byte[] actualBytes = ByteArray.fileToBytes(FILE_TO_TEST);

            if (fileToCompareWith.length() > Integer.MAX_VALUE) {
                throw new IOException("Too large file to load in byte array.");
            }

            final byte[] expectedBytes = new byte[(int) fileToCompareWith.length()];

            int offset = 0;
            int numRead = 0;
            while (offset < expectedBytes.length && (numRead = bytesIStream.read(expectedBytes, offset, actualBytes.length - offset)) >= 0) {
                offset += numRead;
            }

            assertArrayEquals(expectedBytes, actualBytes);
        } finally {
            bytesIStream.close();
        }
    }

    /**
     * if less than 4 bytes are converted, zero bytes should be appended at the buffer's start
     */
    @Test
    public void testBytesToLong_prependingZeros() {
        assertEquals(1, ByteArray.bytesToLong(new byte[] { 0, 0, 1 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBytesToInt() {
        final byte[] b = new byte[Integer.SIZE + 1];
        ByteArray.bytesToInt(b);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBytesToShort2() {
        final byte[] b = new byte[Short.SIZE + 1];
        ByteArray.bytesToInt(b);
    }

    @Test
    public void testCopyBitRange() {
        assertEquals((byte) 10, ByteArray.copyBitsRange((byte) 0x28, 2, 4));
        assertEquals((byte) 3, ByteArray.copyBitsRange((byte) 0xFF, 2, 2));
        assertEquals((byte) 7, ByteArray.copyBitsRange((byte) 0xFF, 5, 3));
        assertEquals((byte) 15, ByteArray.copyBitsRange((byte) 0xFF, 0, 4));
        assertEquals((byte) 31, ByteArray.copyBitsRange((byte) 0xF9, 0, 5));
        assertEquals((byte) 0xA2, ByteArray.copyBitsRange((byte) 0xA2, 0, 8));
        assertEquals((byte) 1, ByteArray.copyBitsRange((byte) 0xFF, 5, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyBitsRange2() {
        ByteArray.copyBitsRange((byte) 0x28, -1, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyBitsRange3() {
        ByteArray.copyBitsRange((byte) 0x28, 1, 187);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyBitsRange4() {
        ByteArray.copyBitsRange((byte) 0x28, 1, 40);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyBitsRange5() {
        ByteArray.copyBitsRange((byte) 0x28, 28, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyBitsRange6() {
        ByteArray.copyBitsRange((byte) 0x28, 2, -2);
    }

    @Test
    public void testBytesToHRString() {
        byte[] b;

        // test valid US-ASCII string
        b = new byte[] { (byte) 79, (byte) 102, (byte) 45, (byte) 57, (byte) 107, (byte) 45, (byte) 48, (byte) 50 };
        final String expected = "Of-9k-02";
        assertEquals(expected, ByteArray.bytesToHRString(b));

        // test Utf-8 restricted bytes
        b = new byte[] { (byte) 246, (byte) 248, (byte) 254 };
        assertEquals(Arrays.toString(b), ByteArray.bytesToHRString(b));

        // test unexpected continuation bytes
        b = new byte[] { (byte) 128, (byte) 113, (byte) 98 };
        assertEquals(Arrays.toString(b), ByteArray.bytesToHRString(b));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<ByteArray> c = ByteArray.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testEncodeBase64() {
        final String result = ByteArray.encodeBase64(Unpooled.wrappedBuffer("abc123".getBytes()));
        assertEquals("YWJjMTIz", result);
    }

}
