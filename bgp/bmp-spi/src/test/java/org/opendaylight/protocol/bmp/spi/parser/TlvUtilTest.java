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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;

/**
 * Created by cgasparini on 19.5.2015.
 */
public class TlvUtilTest {

    private static final byte[] TLV_IN = {(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05};
    private static final byte[] TLV_OUT = {(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x05, (byte) 0x01,
        (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05};
    private static final byte[] TLV_COUNTER32_OUT = {(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x05};
    private static final byte[] TLV_GAUGE64_OUT = {(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05};
    private static final byte[] TLV_UTF8_OUT = {(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x05, (byte) 0x69,
        (byte) 0x6e, (byte) 0x66, (byte) 0x6f, (byte) 0x31};
    private static final byte[] TLV_ASCII_OUT = {(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x4E,
        (byte) 0x61, (byte) 0x6D, (byte) 0x65};

    @Test
    public void testFormatTlv() throws Exception {
        final ByteBuf out = Unpooled.buffer(TLV_OUT.length);
        final ByteBuf in = Unpooled.copiedBuffer(TLV_IN);
        TlvUtil.formatTlv(1, in, out);
        Assert.assertArrayEquals(TLV_OUT, ByteArray.getAllBytes(out));
    }

    @Test
    public void testFormatTlvCounter32() throws Exception {
        ByteBuf out = Unpooled.buffer(TLV_COUNTER32_OUT.length);
        TlvUtil.formatTlvCounter32(1, new Counter32(5L), out);
        Assert.assertArrayEquals(TLV_COUNTER32_OUT, ByteArray.getAllBytes(out));
        out = Unpooled.EMPTY_BUFFER;
        TlvUtil.formatTlvCounter32(1, null, out);
        Assert.assertFalse(out.isReadable());
    }

    @Test
    public void testFormatTlvGauge64() throws Exception {
        ByteBuf out = Unpooled.buffer(TLV_GAUGE64_OUT.length);
        TlvUtil.formatTlvGauge64(1, new Gauge64(BigInteger.valueOf(5)), out);
        Assert.assertArrayEquals(TLV_GAUGE64_OUT, ByteArray.getAllBytes(out));
        out = Unpooled.EMPTY_BUFFER;
        TlvUtil.formatTlvGauge64(1, null, out);
        Assert.assertFalse(out.isReadable());
    }

    @Test
    public void testFormatTlvUtf8() throws Exception {
        ByteBuf out = Unpooled.buffer(TLV_UTF8_OUT.length);
        TlvUtil.formatTlvUtf8(1, "info1", out);
        Assert.assertArrayEquals(TLV_UTF8_OUT, ByteArray.getAllBytes(out));
        out = Unpooled.EMPTY_BUFFER;
        TlvUtil.formatTlvUtf8(1, null, out);
        Assert.assertFalse(out.isReadable());
    }

    @Test
    public void testFormatTlvASCII() throws Exception {
        ByteBuf out = Unpooled.buffer(TLV_ASCII_OUT.length);
        TlvUtil.formatTlvAscii(1, "Name", out);
        Assert.assertArrayEquals(TLV_ASCII_OUT, ByteArray.getAllBytes(out));
        out = Unpooled.EMPTY_BUFFER;
        TlvUtil.formatTlvAscii(1, null, out);
        Assert.assertFalse(out.isReadable());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testBmpMessageConstantsPrivateConstructor() throws Throwable {
        final Constructor<BmpMessageConstants> c = BmpMessageConstants.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testTlvUtilPrivateConstructor() throws Throwable {
        final Constructor<TlvUtil> c = TlvUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}