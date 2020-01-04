/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.spi.parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;

public final class TlvUtil {
    private TlvUtil() {
        // Hidden on purpose
    }

    public static void formatTlv(final int type, final ByteBuf value, final ByteBuf out) {
        formatTlvHeader(type, value.readableBytes(), out);
        out.writeBytes(value);
    }

    public static void formatTlvShort16(final int type, final int value, final ByteBuf out) {
        formatTlvHeader(type, Short.BYTES, out);
        ByteBufWriteUtil.writeUnsignedShort(value, out);
    }

    public static void formatTlvCounter32(final int type, final Counter32 value, final ByteBuf out) {
        if (value != null && value.getValue() != null) {
            formatTlvHeader(type, Integer.BYTES, out);
            ByteBufWriteUtil.writeUnsignedInt(value.getValue(), out);
        }
    }

    public static void formatTlvGauge64(final int type, final Gauge64 value, final ByteBuf out) {
        if (value != null && value.getValue() != null) {
            formatTlvHeader(type, Long.BYTES, out);
            ByteBufWriteUtil.writeUnsignedLong(value.getValue(), out);
        }
    }

    public static void formatTlvUtf8(final int type, final String value, final ByteBuf out) {
        if (value != null) {
            TlvUtil.formatTlv(type, Unpooled.copiedBuffer(value, StandardCharsets.UTF_8), out);
        }
    }

    public static void formatTlvAscii(final int type, final String value, final ByteBuf out) {
        if (value != null) {
            TlvUtil.formatTlv(type, Unpooled.copiedBuffer(value, StandardCharsets.US_ASCII), out);
        }

    }

    private static void formatTlvHeader(final int type, final int length, final ByteBuf output) {
        ByteBufWriteUtil.writeUnsignedShort(type, output);
        ByteBufWriteUtil.writeUnsignedShort(length, output);
    }
}
