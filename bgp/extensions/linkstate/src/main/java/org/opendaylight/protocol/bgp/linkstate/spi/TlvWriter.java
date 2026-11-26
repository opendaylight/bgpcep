/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.VerifyException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yangtools.binding.ScalarTypeObject;
import org.opendaylight.yangtools.concepts.Mutable;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility wrapper for writing TLVs into a {@link ByteBuf}. It utilizes the buffer to receive the data without
 * affecting its {@link ByteBuf#writerIndex()}. A complete TLV is made visible via {@link #commit(Uint16)}, at which
 * point the TLV header is written reflecting
 *
 * @since 3.0.0
 */
public final class TlvWriter implements Mutable {
    private static final Logger LOG = LoggerFactory.getLogger(TlvWriter.class);
    private static final int HEADER_SIZE = 4;

    private final ByteBuf slice;
    private final ByteBuf buf;
    private final int start;

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

    public @NonNull TlvWriter write4(final @NonNull ScalarTypeObject<Uint32> value) {
        return write4(value.getValue());
    }

    @NonNullByDefault
    public TlvWriter write4(final Float32 value) {
        slice.writeBytes(value.getValue());
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

    public void commit(final int type) {
        commit(Uint16.valueOf(type));
    }

    @NonNullByDefault
    public void commit(final Uint16 type) {
        final var index = buf.writerIndex();
        if (start != index) {
            throw new VerifyException("Writer index moved from " + start + " to " + index);
        }

        final var length = slice.writerIndex();
        buf.writeShort(type.shortValue()).writeShort(length).writerIndex(index + length + HEADER_SIZE);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Serialized tlv type {} to: {}", type, ByteBufUtil.hexDump(slice, 0, length));
        }
    }
}
