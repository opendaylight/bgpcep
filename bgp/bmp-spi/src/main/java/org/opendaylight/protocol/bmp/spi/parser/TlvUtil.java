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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class TlvUtil {
    private static final Logger LOG = LoggerFactory.getLogger(TlvUtil.class);
    private final static int Counter32 = 4;
    private final static int Gauge64 = 8;

    private TlvUtil() {
        throw new UnsupportedOperationException();
    }

    public static void formatTlv(final int type, final ByteBuf value, ByteBuf out) {
        out.writeByte(type);
        out.writeByte(value.writerIndex());
        out.writeBytes(value);
    }

    public static void formatTlvCounter32(final short type, final Long value, ByteBuf out) {
        out.writeShort(type);
        out.writeShort(Counter32);
        out.writeLong(value);
    }

    public static void formatTlvGauge64(final short type, final BigInteger value, ByteBuf out) {
        out.writeShort(type);
        out.writeShort(Gauge64);
        out.writeBytes(value.toByteArray());
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

    public static void formatTv(final int type, final ByteBuf value, ByteBuf out) {
        out.writeByte(type);
        out.writeBytes(value);
    }

    public static void formatV(final ByteBuf value, ByteBuf out) {
        out.writeBytes(value);
    }

    public static void formatTvIPV(final String ipVAdd, ByteBuf out) {
        byte[] responseArray = null;
        try {
            responseArray = InetAddress.getByName(ipVAdd).getAddress();
        } catch (final Exception e) {
            responseArray = null;
            LOG.error("Incorrect format ipv4 address ");
        }

        out.writeBytes(responseArray);
    }

    public static String parseIPV(byte[] ipvAddress) {
        try {
            return InetAddress.getByAddress(ipvAddress).toString();
        } catch (UnknownHostException e) {
            LOG.error("Incorrect format ipV address ");
        }
        return null;
    }
}
