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
import java.util.function.ToIntBiFunction;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float128;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float64;
import org.opendaylight.yangtools.concepts.Mutable;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility wrapper for writing TLVs into a {@link ByteBuf}. It utilizes the buffer to receive the data without
 * affecting its {@link ByteBuf#writerIndex()}. A complete TLV is made visible via {@link #endTlv(Uint16)}, at which
 * point the TLV header is written reflecting
 *
 * @since 3.0.0
 */
public final class TlvWriter extends AbstractBinaryOutput<TlvWriter> implements Mutable, TlvBody {
    private static final Logger LOG = LoggerFactory.getLogger(TlvWriter.class);
    private static final int HEADER_SIZE = 4;

    private final @NonNull ByteBuf buf;

    private ByteBuf slice;
    private int start;

    /**
     * Default constructor.
     *
     * @param buf output buffer
     * @throws IndexOutOfBoundsException if {@code buf} does not have the capacity to hold an empty TLV
     */
    @NonNullByDefault
    public TlvWriter(final ByteBuf buf) {
        this.buf = requireNonNull(buf);
    }

    @NonNullByDefault
    public static <T> void append(final ByteBuf buf, final @Nullable T tlv,
            final ToIntBiFunction<TlvBody, @NonNull T> serializer) {
        if (tlv != null) {
            final var writer = TlvWriter.startTlv(buf);
            writer.endTlv(serializer.applyAsInt(writer, tlv));
        }
    }

    @NonNullByDefault
    public <T> TlvWriter append(final @Nullable T tlv,
            final ToIntBiFunction<TlvBody, @NonNull T> serializer) {
        if (tlv != null) {
            startTlv().endTlv(serializer.applyAsInt(this, tlv));
        }
        return this;
    }

    @NonNullByDefault
    public static TlvWriter startTlv(final ByteBuf buf) {
        return new TlvWriter(buf).startTlv();
    }

    @NonNullByDefault
    public TlvWriter startTlv() {
        buf.ensureWritable(HEADER_SIZE);
        start = buf.writerIndex();
        slice = buf.slice(start + HEADER_SIZE, Uint16.MAX_VALUE.toJava()).writerIndex(0);
        return this;
    }

    @NonNullByDefault
    public TlvWriter endTlv(final int type) {
        return endTlv(Uint16.valueOf(type));
    }

    @NonNullByDefault
    public TlvWriter endTlv(final Uint16 type) {
        final var index = buf.writerIndex();
        if (start != index) {
            throw new VerifyException("Writer index moved from " + start + " to " + index);
        }

        final var length = slice.writerIndex();
        buf.writeShort(type.shortValue()).writeShort(length).writerIndex(index + length + HEADER_SIZE);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Serialized tlv type {} to: {}", type, ByteBufUtil.hexDump(slice, 0, length));
        }
        slice = null;
        start = -1;
        return this;
    }

    @Override
    public TlvWriter write4(final Ipv4AddressNoZone value) {
        slice.writeBytes(IetfInetUtil.ipv4AddressNoZoneBytes(value));
        return this;
    }

    @Override
    public TlvWriter write4(final Float32 value) {
        slice.writeBytes(value.getValue());
        return this;
    }

    @Override
    public TlvWriter write8(final Float64 value) {
        slice.writeBytes(value.getValue());
        return this;
    }

    @Override
    public TlvWriter write16(final Ipv6AddressNoZone value) {
        slice.writeBytes(IetfInetUtil.ipv6AddressNoZoneBytes(value));
        return this;
    }

    @Override
    public TlvWriter write16(final Float128 value) {
        slice.writeBytes(value.getValue());
        return this;
    }

    @Override
    protected TlvWriter self() {
        return this;
    }

    @Override
    protected void append1(final int value) {
        slice.writeByte(value);
    }

    @Override
    protected void append2(final int value) {
        slice.writeShort(value);
    }

    @Override
    protected void append4(final int value) {
        slice.writeInt(value);
    }

    @Override
    protected void append8(final long value) {
        slice.writeLong(value);
    }
}
