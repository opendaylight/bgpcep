/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.INT_BYTES_LENGTH;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.LONG_BYTES_LENGTH;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.SHORT_BYTES_LENGTH;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Gauge64;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class TlvUtil {

    private static final int TLV_HEADER_SIZE = 4;

    public static class Tlv {
        private final int type;
        private final int length;
        private final ByteBuf value;

        private Tlv(final int type, final int length, final ByteBuf value) {
            this.type = type;
            this.length = length;
            this.value = value;
        }

        public static Tlv fromByteBuf(final ByteBuf buffer) {
            Preconditions.checkArgument(buffer.readableBytes() > TLV_HEADER_SIZE);
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            Preconditions.checkArgument(buffer.readableBytes() >= length);
            return new Tlv(type, length, buffer.readSlice(length));
        }

        public int getType() {
            return type;
        }

        public int getLength() {
            return length;
        }

        public ByteBuf getValue() {
            return value;
        }

        public String valueToUtf8String() {
            return getValue().toString(0, getLength(), Charsets.UTF_8);
        }

        public String valueToAsciiString() {
            return getValue().toString(0, getLength(), Charsets.US_ASCII);
        }

    }

    private TlvUtil() {
        throw new UnsupportedOperationException();
    }

    public static void formatTlv(final int type, final ByteBuf value, final ByteBuf out) {
        formatTlvHeader(type, value.readableBytes(), out);
        out.writeBytes(value);
    }

    public static void formatTlvUnsignedInt(final int type, final int value, final ByteBuf out) {
        formatTlvHeader(type, SHORT_BYTES_LENGTH, out);
        ByteBufWriteUtil.writeUnsignedShort(value, out);
    }

    public static int formatTlvCounter32(final int type, final Counter32 value, final ByteBuf out) {
        if (value == null || value.getValue() == null) {
            return 0;
        }
        formatTlvHeader(type, INT_BYTES_LENGTH, out);
        ByteBufWriteUtil.writeUnsignedInt(value.getValue(), out);
        return 1;
    }

    public static int formatTlvGauge64(final int type, final Gauge64 value, final ByteBuf out) {
        if (value == null || value.getValue() == null) {
            return 0;
        }
        formatTlvHeader(type, LONG_BYTES_LENGTH, out);
        ByteBufWriteUtil.writeUnsignedLong(value.getValue(), out);
        return 1;
    }

    public static void formatTlvUtf8(final int type, final String value, final ByteBuf out) {
        TlvUtil.formatTlv(type, Unpooled.copiedBuffer(value, Charsets.UTF_8), out);
    }

    public static void formatTlvASCII(final int type, final String value, final ByteBuf out) {
        TlvUtil.formatTlv(type, Unpooled.copiedBuffer(value, Charsets.US_ASCII), out);
    }

    private static void formatTlvHeader(final int type, final int length, final ByteBuf output) {
        ByteBufWriteUtil.writeUnsignedShort(type, output);
        ByteBufWriteUtil.writeUnsignedShort(length, output);
    }
}
