/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class for methods working with byte array.
 */
public final class ByteArray {

    private static final Logger LOG = LoggerFactory.getLogger(ByteArray.class);

    private ByteArray() {
        throw new UnsupportedOperationException();
    }

    /**
     * Helper method missing from netty ByteBuf methods. Directly returns byte array part of the given buffer, starting
     * at reader index, with given length. Increases reader index of the buffer by 'length'.
     *
     * @param buffer ByteBuf from which the bytes are going to be taken
     * @param length length of the returned byte array
     * @return byte array
     */
    public static byte[] readBytes(final ByteBuf buffer, final int length) {
        Preconditions.checkArgument(buffer != null && buffer.readableBytes() >= length,
            "Buffer cannot be read for %s bytes.", length);
        final byte[] result = new byte[length];
        buffer.readBytes(result);
        return result;
    }

    /**
     * Helper method missing from netty ByteBuf methods. Directly returns all readable bytes from buffer as byte array.
     * Adjusts reader index of the buffer by length of readable bytes in the buffer.
     *
     * @param buffer byteBuf from which the bytes are going to be taken
     * @return byte array
     */
    public static byte[] readAllBytes(final ByteBuf buffer) {
        return readBytes(buffer, buffer.readableBytes());
    }

    /**
     * Helper method missing from netty ByteBuf methods. Directly returns byte array part of the given buffer, starting
     * at reader index, with given length. Does not modify reader or writer index of the buffer.
     *
     * @param buffer ByteBuf from which the bytes are going to be taken
     * @param length length of the returned byte array
     * @return byte array
     */
    public static byte[] getBytes(final ByteBuf buffer, final int length) {
        Preconditions.checkArgument(buffer != null && buffer.readableBytes() >= length,
            "Buffer cannot be read for %s bytes.", length);
        final byte[] result = new byte[length];
        buffer.getBytes(buffer.readerIndex(), result);
        return result;
    }

    /**
     * Helper method missing from netty ByteBuf methods. Directly returns all readable bytes from buffer as byte array.
     * Does not modify writer or reader index of the buffer.
     *
     * @param buffer byteBuf from which the bytes are going to be taken
     * @return byte array
     */
    public static byte[] getAllBytes(final ByteBuf buffer) {
        return getBytes(buffer, buffer.readableBytes());
    }

    /**
     * Returns a new byte array from given byte array, starting at start index with the size of the length parameter.
     * Byte array given as parameter stays untouched.
     *
     * @param bytes original byte array
     * @param startIndex beginning index, inclusive
     * @param length how many bytes should be in the sub-array
     * @return a new byte array that is a sub-array of the original
     */
    public static byte[] subByte(final byte[] bytes, final int startIndex, final int length) {
        Preconditions.checkArgument(checkLength(bytes, length) && checkStartIndex(bytes, startIndex, length), "Cannot create subByte, invalid arguments: Length: %s startIndex: %s", length, startIndex);
        final byte[] res = new byte[length];
        System.arraycopy(bytes, startIndex, res, 0, length);
        return res;
    }

    private static boolean checkLength(final byte[] bytes, final int length) {
        return length > 0 && bytes.length > 0 && length <= bytes.length;
    }

    private static boolean checkStartIndex(final byte[] bytes, final int startIndex, final int length) {
        return startIndex >= 0 && startIndex < bytes.length && (startIndex + length <= bytes.length);
    }

    /**
     * Converts byte array to Integer. If there are less bytes in the array as required (4), the method will push
     * adequate number of zero bytes prepending given byte array.
     *
     * @param bytes array to be converted to int
     * @return int
     */
    public static int bytesToInt(final byte[] bytes) {
        Preconditions.checkArgument(bytes.length <= Integer.SIZE / Byte.SIZE, "Cannot convert bytes to integer. Byte array too big.");
        byte[] res = new byte[Integer.SIZE / Byte.SIZE];
        if (bytes.length != Integer.SIZE / Byte.SIZE) {
            System.arraycopy(bytes, 0, res, Integer.SIZE / Byte.SIZE - bytes.length, bytes.length);
        } else {
            res = bytes;
        }
        final ByteBuffer buff = ByteBuffer.wrap(res);
        return buff.getInt();
    }

    /**
     * Converts byte array to long. If there are less bytes in the array as required (Long.Size), the method will push
     * adequate number of zero bytes prepending given byte array.
     *
     * @param bytes array to be converted to long
     * @return long
     */
    public static long bytesToLong(final byte[] bytes) {
        Preconditions.checkArgument(bytes.length <= Long.SIZE / Byte.SIZE, "Cannot convert bytes to long.Byte array too big.");
        byte[] res = new byte[Long.SIZE / Byte.SIZE];
        if (bytes.length != Long.SIZE / Byte.SIZE) {
            System.arraycopy(bytes, 0, res, Long.SIZE / Byte.SIZE - bytes.length, bytes.length);
        } else {
            res = bytes;
        }
        final ByteBuffer buff = ByteBuffer.wrap(res);
        return buff.getLong();
    }

    /**
     * Cuts 'count' number of bytes from the beginning of given byte array.
     *
     * @param bytes array to be cut, cannot be null
     * @param count how many bytes needed to be cut, needs to be greater than 0
     * @return bytes array without first 'count' bytes
     */
    public static byte[] cutBytes(final byte[] bytes, final int count) {
        Preconditions.checkArgument(bytes.length != 0 && count <= bytes.length && count > 0, "Cannot cut bytes, invalid arguments: Count: %s bytes.length: %s", count, bytes.length);
        return Arrays.copyOfRange(bytes, count, bytes.length);
    }

    /**
     * Parse byte to bits, from the leftmost bit.
     *
     * @param b byte to be parsed
     * @return array of booleans with size of 8
     */
    @Deprecated
    public static boolean[] parseBits(final byte b) {
        final boolean[] bits = new boolean[Byte.SIZE];
        int j = 0;
        for (int i = Byte.SIZE - 1; i >= 0; i--) {
            bits[j] = ((b & (1 << i)) != 0);
            j++;
        }
        return bits;
    }

    /**
     * Parses array of bytes to BitSet, from left most bit.
     *
     * @param bytes array of bytes to be parsed
     * @return BitSet with length = bytes.length * Byte.SIZE
     */
    @Deprecated
    public static BitSet bytesToBitSet(final byte[] bytes) {
        final BitSet bitSet = new BitSet(bytes.length * Byte.SIZE);
        for (int bytesIter = 0; bytesIter < bytes.length; bytesIter++) {
            final int offset = bytesIter * Byte.SIZE;
            for (int byteIter = Byte.SIZE - 1; byteIter >= 0; byteIter--) {
                bitSet.set(offset + (Byte.SIZE - byteIter - 1), (bytes[bytesIter] & 1 << (byteIter)) != 0);
            }
        }
        return bitSet;
    }

    /**
     * Parses BitSet to bytes, from most left bit.
     *
     * @param bitSet BitSet to be parsed
     * @param returnedLength Length of returned array. Overlapping flags are truncated.
     * @return parsed array of bytes with length of bitSet.length / Byte.SIZE
     */
    @Deprecated
    public static byte[] bitSetToBytes(final BitSet bitSet, final int returnedLength) {
        final byte[] bytes = new byte[returnedLength];

        for (int bytesIter = 0; bytesIter < bytes.length; bytesIter++) {
            final int offset = bytesIter * Byte.SIZE;

            for (int byteIter = Byte.SIZE - 1; byteIter >= 0; byteIter--) {
                bytes[bytesIter] |= (bitSet.get(offset + (Byte.SIZE - byteIter - 1)) ? 1 << byteIter : 0);
            }
        }
        return bytes;
    }

    /**
     * Parses file to array of bytes
     *
     * @param name path to file to by parsed
     * @return parsed array of bytes
     */
    public static byte[] fileToBytes(final String name) throws IOException {
        final File file = new File(name);
        int offset = 0;
        int numRead = 0;

        if (file.length() > Integer.MAX_VALUE) {
            throw new IOException("Too large file to load in byte array.");
        }
        final byte[] byteArray = new byte[(int) file.length()];
        try (final FileInputStream fin = new FileInputStream(file)) {
            while (offset < byteArray.length) {
                numRead = fin.read(byteArray, offset, byteArray.length - offset);
                if (numRead >= 0) {
                    offset += numRead;
                }
            }
            fin.close();
        }
        return byteArray;
    }

    /**
     * Copies range of bits from passed byte and align to right.<br>
     *
     * @param src source byte to copy from
     * @param fromBit bit from which will copy (inclusive) - numbered from 0
     * @param length of bits to by copied, valid values are 1 through 8
     * @return copied value aligned to right
     */
    public static byte copyBitsRange(final byte src, final int fromBit, final int length) {
        Preconditions.checkArgument(fromBit >= 0 && fromBit <= Byte.SIZE - 1 && length >= 1 && length <= Byte.SIZE, "fromBit or toBit is out of range.");
        Preconditions.checkArgument(fromBit + length <= Byte.SIZE, "Out of range.");

        byte retByte = 0;
        int retI = 0;

        for (int i = fromBit + length - 1; i >= fromBit; i--) {

            if ((src & 1 << (Byte.SIZE - i - 1)) != 0) {
                retByte |= 1 << retI;
            }

            retI++;
        }

        return retByte;
    }

    /**
     * Decodes bytes to human readable UTF-8 string. If bytes are not valid UTF-8, they are represented as raw binary.
     *
     * @param bytes bytes to be decoded to string
     * @return String representation of passed bytes
     */
    public static String bytesToHRString(final byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (final CharacterCodingException e) {
            LOG.debug("Could not apply UTF-8 encoding.", e);
            return Arrays.toString(bytes);
        }
    }
}
