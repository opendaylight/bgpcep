/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import org.junit.jupiter.api.Test;

class ByteArrayTest {
    private final byte[] before = new byte[] { 15, 28, 4, 6, 9, 10 };

    @Test
    void testReadBytes() {
        final var buf = Unpooled.copiedBuffer(before);
        buf.readerIndex(1);
        assertArrayEquals(new byte[] { 28, 4, 6 }, ByteArray.readBytes(buf, 3));
        assertEquals(4, buf.readerIndex());

        assertArrayEquals(new byte[] { 9, 10 }, ByteArray.readAllBytes(buf));
        assertEquals(buf.readerIndex(), buf.writerIndex());
    }

    @Test
    void testGetBytes() {
        final var buf = Unpooled.copiedBuffer(before);
        buf.readerIndex(1);
        assertArrayEquals(new byte[] { 28, 4, 6 }, ByteArray.getBytes(buf, 3));
        assertEquals(1, buf.readerIndex());

        assertArrayEquals(new byte[] { 28, 4, 6, 9, 10 }, ByteArray.getAllBytes(buf));
        assertNotSame(buf.readerIndex(), buf.writerIndex());
    }

    @Test
    void testSubByte() {
        assertArrayEquals(new byte[] { 15, 28, 4 }, ByteArray.subByte(before, 0, 3));
        assertArrayEquals(new byte[] { 10 }, ByteArray.subByte(before, 5, 1));
    }

    @Test
    void testSubByte2() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.subByte(new byte[0], 2, 2));
    }

    @Test
    void testSubByte3() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.subByte(before, 2, -1));
    }

    @Test
    void testSubByte4() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.subByte(before, -1, 2));
    }

    @Test
    void testSubByte5() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.subByte(before, 9, 2));
    }

    @Test
    void testSubByte6() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.subByte(before, 2, 19));
    }

    @Test
    void testSubByte7() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.subByte(before, 2, 7));
    }

    @Test
    void testCutBytes() {
        assertArrayEquals(new byte[] { 4, 6, 9, 10 }, ByteArray.cutBytes(before, 2));
        assertArrayEquals(new byte[] {}, ByteArray.cutBytes(before, 6));
    }

    @Test
    void testCutBytes2() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.cutBytes(new byte[0], 5));
    }

    @Test
    void testCutBytes3() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.cutBytes(before, 9));
    }

    @Test
    void testCutBytes4() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.cutBytes(before, 0));
    }

    @Test
    void generateBitSet() {
        final var inBitSet = new BitSet();

        // 0x03
        inBitSet.set(6, 8);

        // 0xFF
        inBitSet.set(8, 16);

        // 0x01
        inBitSet.set(23);

        // 0x80
        inBitSet.set(24);
    }

    @Test
    void testFileToBytes() throws Exception {
        final var fileToTest = Path.of("src", "test", "resources", "PCEStatefulCapabilityTlv1.bin");
        final var fileToCompareWith = fileToTest.toFile();

        try (var bytesIStream = Files.newInputStream(fileToTest)) {
            final var actualBytes = ByteArray.fileToBytes(fileToTest.toString());

            if (fileToCompareWith.length() > Integer.MAX_VALUE) {
                throw new IOException("Too large file to load in byte array.");
            }

            final var expectedBytes = new byte[(int) fileToCompareWith.length()];

            int offset = 0;
            int numRead = 0;
            while (offset < expectedBytes.length
                    && (numRead = bytesIStream.read(expectedBytes, offset, actualBytes.length - offset)) >= 0) {
                offset += numRead;
            }

            assertArrayEquals(expectedBytes, actualBytes);
        }
    }

    /**
     * if less than 4 bytes are converted, zero bytes should be appended at the buffer's start.
     */
    @Test
    void testBytesToLong_prependingZeros() {
        assertEquals(1, ByteArray.bytesToLong(new byte[] { 0, 0, 1 }));
    }

    @Test
    void testBytesToInt() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.bytesToInt(new byte[Integer.SIZE + 1]));
    }

    @Test
    void testBytesToShort2() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.bytesToInt(new byte[Short.SIZE + 1]));
    }

    @Test
    void testCopyBitRange() {
        assertEquals((byte) 10, ByteArray.copyBitsRange((byte) 0x28, 2, 4));
        assertEquals((byte) 3, ByteArray.copyBitsRange((byte) 0xFF, 2, 2));
        assertEquals((byte) 7, ByteArray.copyBitsRange((byte) 0xFF, 5, 3));
        assertEquals((byte) 15, ByteArray.copyBitsRange((byte) 0xFF, 0, 4));
        assertEquals((byte) 31, ByteArray.copyBitsRange((byte) 0xF9, 0, 5));
        assertEquals((byte) 0xA2, ByteArray.copyBitsRange((byte) 0xA2, 0, 8));
        assertEquals((byte) 1, ByteArray.copyBitsRange((byte) 0xFF, 5, 1));
    }

    @Test
    void testCopyBitsRange2() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.copyBitsRange((byte) 0x28, -1, 4));
    }

    @Test
    void testCopyBitsRange3() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.copyBitsRange((byte) 0x28, 1, 187));
    }

    @Test
    void testCopyBitsRange4() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.copyBitsRange((byte) 0x28, 1, 40));
    }

    @Test
    void testCopyBitsRange5() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.copyBitsRange((byte) 0x28, 28, 2));
    }

    @Test
    void testCopyBitsRange6() {
        assertThrows(IllegalArgumentException.class, () -> ByteArray.copyBitsRange((byte) 0x28, 2, -2));
    }

    @Test
    void testBytesToHRString() {
        // test valid US-ASCII string
        assertEquals("Of-9k-02", ByteArray.bytesToHRString(
            new byte[] { (byte) 79, (byte) 102, (byte) 45, (byte) 57, (byte) 107, (byte) 45, (byte) 48, (byte) 50 }));

        // test Utf-8 restricted bytes
        var bytes = new byte[] { (byte) 246, (byte) 248, (byte) 254 };
        assertEquals(Arrays.toString(bytes), ByteArray.bytesToHRString(bytes));

        // test unexpected continuation bytes
        bytes = new byte[] { (byte) 128, (byte) 113, (byte) 98 };
        assertEquals(Arrays.toString(bytes), ByteArray.bytesToHRString(bytes));
    }

    @Test
    void testEncodeBase64() {
        assertEquals("YWJjMTIz", ByteArray.encodeBase64(Unpooled.wrappedBuffer("abc123".getBytes())));
    }
}
