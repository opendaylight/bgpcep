package org.opendaylight.protocol.bmp.spi.parser;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class TlvUtil {

    private TlvUtil() {
        throw new UnsupportedOperationException();
    }

    public static void formatTlv(final int type, final ByteBuf value, ByteBuf out) {
        out.writeByte(type);
        out.writeByte(value.writerIndex());
        out.writeBytes(value);
    }

    public static String parseUTF8(ByteBuf bytes, final int length) {
        return bytes.readSlice(length).toString(Charset.forName("UTF-8"));
    }

    public static void formatTlvUtf8(final int type, final String value, ByteBuf byteBuffer) {
        final ByteBuf buf = Unpooled.copiedBuffer(value, Charset.forName("UTF-8"));
        TlvUtil.formatTlv(type, buf, byteBuffer);
    }

    public static void formatTlvASCII(final int type, final String value, ByteBuf buffer) {
        final ByteBuf buf = Unpooled.copiedBuffer(value, Charset.forName("US-ASCII"));
        TlvUtil.formatTlv(type, buf, buffer);
    }

    public static String parseASCII(ByteBuf bytes, int length) {
        return bytes.readSlice(length).toString(Charset.forName("US-ASCII"));
    }
}
