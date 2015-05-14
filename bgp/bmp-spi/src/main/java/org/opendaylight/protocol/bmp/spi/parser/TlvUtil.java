package org.opendaylight.protocol.bmp.spi.parser;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.math.BigInteger;
import java.nio.charset.Charset;

import org.opendaylight.protocol.util.ByteBufWriteUtil;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class TlvUtil {
    public final static int Counter32 = 4;
    public final static int Gauge64 = 8;

    private TlvUtil() {
        throw new UnsupportedOperationException();
    }

    public static void formatTlv(final int type, final ByteBuf value, final ByteBuf out) {
        ByteBufWriteUtil.writeUnsignedShort(type, out);
        ByteBufWriteUtil.writeUnsignedShort(value.writerIndex(), out);
        out.writeBytes(value);
    }

    public static int formatTlvCounter32(final int type, final Long value, final ByteBuf out) {
        if (value == null) {
            return 0;
        }
        ByteBufWriteUtil.writeUnsignedShort(type, out);
        ByteBufWriteUtil.writeUnsignedShort(Counter32, out);
        ByteBufWriteUtil.writeUnsignedInt(value, out);
        return 1;
    }

    public static int formatTlvGauge64(final int type, final BigInteger value, final ByteBuf out) {
        if (value == null) {
            return 0;
        }
        ByteBufWriteUtil.writeUnsignedShort(type, out);
        ByteBufWriteUtil.writeUnsignedShort(Gauge64, out);
        ByteBufWriteUtil.writeUnsignedLong(value,out);
        return 1;
    }

    public static void formatTlvUtf8(final int type, final String value, final ByteBuf out) {
        TlvUtil.formatTlv(type, Unpooled.copiedBuffer(value, Charset.forName("UTF-8")), out);
    }

    public static void formatTlvASCII(final int type, final String value, final ByteBuf out) {
        TlvUtil.formatTlv(type, Unpooled.copiedBuffer(value, Charset.forName("US-ASCII")), out);
    }

    public static String parseASCII(final ByteBuf bytes, final int length) {
        return bytes.readSlice(length).toString(Charset.forName("US-ASCII"));
    }

    public static String parseUTF8(final ByteBuf bytes, final int length) {
        return bytes.readSlice(length).toString(Charset.forName("UTF-8"));
    }
}
