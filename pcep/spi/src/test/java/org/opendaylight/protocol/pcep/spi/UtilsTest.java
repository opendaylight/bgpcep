/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.pcep.spi.VendorInformationUtil.VENDOR_INFORMATION_OBJECT_CLASS;
import static org.opendaylight.protocol.pcep.spi.VendorInformationUtil.VENDOR_INFORMATION_OBJECT_TYPE;
import static org.opendaylight.protocol.pcep.spi.VendorInformationUtil.VENDOR_INFORMATION_TLV_TYPE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;

public class UtilsTest {

    @Test
    public void testLabelUtil() {
        final byte[] expected = { (byte) 0x81, 0x04, 0x01, 0x02, 0x03, 0x04 };
        final ByteBuf out = Unpooled.buffer();
        final ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4 });
        LabelUtil.formatLabel(4, true, true, body, out);
        assertArrayEquals(expected, ByteArray.readAllBytes(out));

        final byte[] ex = { 0, 0x05, 0x01, 0x02, 0x03, 0x04 };
        body.resetReaderIndex();
        LabelUtil.formatLabel(5, null, null, body, out);
        assertArrayEquals(ex, ByteArray.getAllBytes(out));
    }

    @Test
    public void testMessageUtil() {
        final byte[] expected = { (byte) 0x20, 0x08, 0, 0x0a, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
        final ByteBuf out = Unpooled.buffer();
        final ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4, 5, 6 });
        MessageUtil.formatMessage(8, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testObjectUtil() {
        final byte[] expected = { 0x08, 0x13, 0, 0x06, 0x01, 0x02 };
        final ByteBuf out = Unpooled.buffer();
        final ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2 });
        ObjectUtil.formatSubobject(1, 8, true, true, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testObjectUtilFalse() {
        final byte[] expected = { 0x08, 0x10, 0, 0x06, 0x01, 0x02 };
        final ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2 });
        ObjectUtil.formatSubobject(1, 8, false, false, body, out);
        assertArrayEquals(expected, ByteArray.readAllBytes(out));

        body = Unpooled.copiedBuffer(new byte[] { 1, 2 });
        ObjectUtil.formatSubobject(1, 8, null, null, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testTlvUtil() {
        byte[] expected = { 0, 4, 0, 4, 1, 2, 3, 4 };
        final ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4 });
        TlvUtil.formatTlv(4, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));

        expected = new byte[]{ 0, 4, 0, 5, 1, 2, 3, 4, 5, 0, 0, 0 };
        out.clear();
        body = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4, 5 });
        TlvUtil.formatTlv(4, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testRROSubobjectUtil() {
        final byte[] expected = { 4, 6, 1, 2, 3, 4 };
        final ByteBuf out = Unpooled.buffer();
        final ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4 });
        RROSubobjectUtil.formatSubobject(4, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testEROSubobjectUtil() {
        byte[] expected = { (byte) 0x82, 6, 0, 1, 2, 3 };
        final ByteBuf out = Unpooled.buffer();
        final ByteBuf body = Unpooled.copiedBuffer(new byte[] { 0, 1, 2, 3 });
        body.markReaderIndex();
        EROSubobjectUtil.formatSubobject(2, true, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));

        expected = new byte[]{ 2, 6, 0, 1, 2, 3 };
        out.clear();
        body.resetReaderIndex();
        EROSubobjectUtil.formatSubobject(2, false, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testVendorInformationUtil() {
        assertTrue(VendorInformationUtil.isVendorInformationTlv(VENDOR_INFORMATION_TLV_TYPE));
        assertFalse(VendorInformationUtil.isVendorInformationTlv(VENDOR_INFORMATION_OBJECT_CLASS));

        assertTrue(VendorInformationUtil.isVendorInformationObject(VENDOR_INFORMATION_OBJECT_CLASS,
            VENDOR_INFORMATION_OBJECT_TYPE));
        assertFalse(VendorInformationUtil.isVendorInformationObject(VENDOR_INFORMATION_OBJECT_CLASS,
            VENDOR_INFORMATION_TLV_TYPE));
        assertFalse(VendorInformationUtil.isVendorInformationObject(VENDOR_INFORMATION_TLV_TYPE,
            VENDOR_INFORMATION_OBJECT_TYPE));
        assertFalse(VendorInformationUtil.isVendorInformationObject(VENDOR_INFORMATION_OBJECT_TYPE,
            VENDOR_INFORMATION_OBJECT_CLASS));
    }
}
