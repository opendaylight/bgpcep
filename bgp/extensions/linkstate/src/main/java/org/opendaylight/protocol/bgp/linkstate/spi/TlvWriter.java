/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.VerifyException;
import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.concepts.Mutable;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * A utility wrapper for writing TLVs into a {@link ByteBuf}. It utilizes the buffer to receive the data without
 * affecting its {@link ByteBuf#writerIndex()}. A complete TLV is made visible via {@link #commit(Uint16)}, at which
 * point the TLV header is written reflecting
 *
 * @since 3.0.0
 */
public final class TlvWriter implements Mutable {
    private static final int HEADER_SIZE = 4;

    @NonNullByDefault
    private final ByteBuf buf;
    private final int start;

    private ByteBuf slice;

    /**
     * Default constructor.
     *
     * @param buf output buffer
     * @throws IndexOutOfBoundsException if {@code buf} does not have the capacity to hold an empty TLV
     */
    @NonNullByDefault
    public TlvWriter(final ByteBuf buf) {
        this.buf = requireNonNull(buf);
        buf.ensureWritable(HEADER_SIZE);
        start = buf.writerIndex();
        slice = buf.slice(start + HEADER_SIZE, Uint16.MAX_VALUE.toJava()).writerIndex(0);
    }

    /**
     * Appends a single byte. The 24 high-order bits of the specified value are ignored.
     *
     * @param value the byte to append
     * @return this writer
     * @throws IndexOutOfBoundsException if the TLV body length or underlying {@link ByteBuf#maxCapacity()} would be
     *                                   exceeded
     */
    @NonNullByDefault
    public TlvWriter write1(final int value) {
        slice.writeByte(value);
        return this;
    }

    /**
     * Appends a single byte.
     *
     * @param value the byte to append
     * @return this writer
     * @throws IndexOutOfBoundsException if the TLV body length or underlying {@link ByteBuf#maxCapacity()} would be
     *                                   exceeded
     */
    @NonNullByDefault
    public TlvWriter write1(final Uint8 value) {
        ByteBufUtils.writeUint8(slice, value);
        return this;
    }

    @NonNullByDefault
    public TlvWriter write2(final int value) {
        slice.writeShort(value);
        return this;
    }

    @NonNullByDefault
    public TlvWriter write2(final Uint16 value) {
        ByteBufUtils.writeUint16(slice, value);
        return this;
    }

    @NonNullByDefault
    public TlvWriter write4(final int value) {
        slice.writeInt(value);
        return this;
    }

    @NonNullByDefault
    public TlvWriter write4(final Uint32 value) {
        ByteBufUtils.writeUint32(slice, value);
        return this;
    }

    @NonNullByDefault
    public TlvWriter write8(final long value) {
        slice.writeLong(value);
        return this;
    }

    @NonNullByDefault
    public TlvWriter write8(final Uint64 value) {
        ByteBufUtils.writeUint64(slice, value);
        return this;
    }

    public int commit(final int type) {
        return commit(Uint16.valueOf(type));
    }

    public int commit(final Uint16 type) {
        final var index = buf.writerIndex();
        if (start != index) {
            throw new VerifyException("Writer index moved from " + start + " to " + index);
        }

        final var length = slice.writerIndex();
        final var ret = length + HEADER_SIZE;
        buf.writeShort(type.shortValue()).writeShort(length).writerIndex(index + ret);
        slice = null;
        return ret;
    }

    @Override
    public String toString() {
        // FIXME: improve to dump written bytes
        return MoreObjects.toStringHelper(this).add("buf", slice).toString();
    }
}
