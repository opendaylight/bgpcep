/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;

/**
 * This class was created to minimize usage of Java BitSet class, as this one
 * is hard to use within specifics of network protocols. Uses network byte
 * order.
 */
public final class BitArray {

    private final int size;

    private final byte[] backingArray;

    private final int offset;

    /**
     * Creates a BitArray with fixed size of bits. For sizes smaller than
     * 8 the whole byte is allocated.
     *
     * @param size Number of bits relevant for this BitArray. Needs to be
     * greater than 0.
     */
    public BitArray(final int size) {
        Preconditions.checkArgument(size >= 1, "Minimum size is 1 bit.");
        this.size = size;
        this.backingArray = new byte[calculateBytes(size)];
        this.offset = (calculateBytes(this.size) * Byte.SIZE) - this.size;
    }

    private BitArray(final byte[] backingArray, final int size) {
        requireNonNull(backingArray, "Byte Array cannot be null");
        this.size = size;
        this.backingArray = backingArray.clone();
        this.offset = (calculateBytes(this.size) * Byte.SIZE) - this.size;
    }

    /**
     * Returns a new BitArray created from bytes from given ByteBuf.
     *
     * @param buffer ByteBuf, whose readerIndex will be moved by
     * minimum number of bytes required for the bit size.
     * @param size Number of bits to be allocated in BitArray
     * @return new BitArray
     */
    public static BitArray valueOf(final ByteBuf buffer, final int size) {
        Preconditions.checkArgument(size >= 1, "Minimum size is 1 bit.");
        requireNonNull(buffer, "Byte Array cannot be null");
        final byte[] b = new byte[calculateBytes(size)];
        buffer.readBytes(b, 0, b.length);
        return new BitArray(b, size);
    }

    /**
     * Returns a new BitArray with given byte array as backing
     * array.
     *
     * @param bytes byte array
     * @return new BitArray
     */
    public static BitArray valueOf(final byte[] bytes) {
        return new BitArray(bytes, bytes.length);
    }

    /**
     * Returns new BitArray with given byte as backing
     * array.
     *
     * @param b unsigned byte
     * @return new BitArray
     */
    public static BitArray valueOf(final byte b) {
        return new BitArray(new byte[] {b}, Byte.SIZE);
    }

    /**
     * If the value given is TRUE, sets bit on given position.
     * Checks for null value. Index is counted from the rightmost
     * bit as 0 to size -1 being the leftmost bit.
     *
     * @param index position of bit that will be set
     * @param value Boolean
     */
    public void set(final int index, final Boolean value) {
        Preconditions.checkArgument(index < this.size, "Index out of bounds.");
        if (value == null || value.equals(Boolean.FALSE)) {
            return;
        }
        final int pos = calculatePosition(index);
        final byte b = this.backingArray[pos];
        this.backingArray[pos] = (byte) (UnsignedBytes.toInt(b) | mask(index));
    }

    /**
     * Returns boolean value for a bit on specific position.
     * Index is counted from the rightmost bit as 0 to
     * size -1 being the leftmost bit.
     *
     * @param index position of bit
     * @return boolean value
     */
    public boolean get(final int index) {
        Preconditions.checkArgument(index < this.size, "Index out of bounds.");
        final byte b = this.backingArray[calculatePosition(index)];
        return ((byte) (UnsignedBytes.toInt(b) & mask(index))) != 0;
    }

    /**
     * Returns the backing byte array of this bitset
     *
     * @return byte[]
     */
    public byte[] array() {
        return this.backingArray.clone();
    }

    /**
     * If possible, returns one byte as backing array.
     *
     * @return byte
     */
    public byte toByte() {
        Preconditions.checkArgument(Byte.SIZE >= this.size, "Cannot put backing array to a single byte.");
        return this.backingArray[0];
    }

    /**
     * Writes backing array to given ByteBuf, even if the backing array is
     * empty, to preserve the number of allocated bits.
     *
     * @param buffer ByteBuf
     */
    public void toByteBuf(final ByteBuf buffer) {
        buffer.writeBytes(this.backingArray);
    }

    /**
     * Calculates the size in bytes necessary for given number of bits.
     *
     * @param size size
     * @return minimum byte size to contain the position of the bit
     */
    private static int calculateBytes(final int size) {
        return (size + Byte.SIZE - 1) / Byte.SIZE;
    }

    /**
     * Calculates which byte in byte array is going to be affected.
     *
     * @param index index of the bit to be changed
     * @return position in byte array
     */
    private int calculatePosition(final int index) {
        return (index + this.offset) / Byte.SIZE;
    }

    /**
     * Returns a byte where only one bit is set
     *
     * @param index bit index within full byte array
     * @return byte with one bit set
     */
    private byte mask(final int index) {
        return (byte) (1 << ((this.size -1 - index) % Byte.SIZE));
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder("BitArray [");
        for (int i = 0; i < this.backingArray.length; i++) {
            b.append(Integer.toBinaryString(UnsignedBytes.toInt(this.backingArray[i])));
            if (i != this.backingArray.length - 1) {
                b.append(' ');
            }
        }
        b.append(']');
        return b.toString();
    }
}
