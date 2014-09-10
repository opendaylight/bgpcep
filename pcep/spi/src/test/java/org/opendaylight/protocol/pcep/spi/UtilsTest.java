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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;

public class UtilsTest {

    @Test
    public void testLabelUtil() {
        byte[] expected = { (byte) 0x81, 0x04, 0x01, 0x02, 0x03, 0x04 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4 });
        LabelUtil.formatLabel(4, true, true, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testMessageUtil() {
        byte[] expected = { (byte) 0x20, 0x08, 0, 0x0a, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4, 5, 6 });
        MessageUtil.formatMessage(8, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testObjectUtil() {
        byte[] expected = { 0x08, 0x13, 0, 0x06, 0x01, 0x02 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2 });
        ObjectUtil.formatSubobject(1, 8, true, true, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testObjectUtilFalse() {
        byte[] expected = { 0x08, 0x10, 0, 0x06, 0x01, 0x02 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2 });
        ObjectUtil.formatSubobject(1, 8, false, false, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testXROSubobjectUtil() {
        byte[] expected = { (byte) 0x82, 6, 0, 1, 2, 3 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 0, 1, 2, 3 });
        body.markReaderIndex();
        XROSubobjectUtil.formatSubobject(2, true, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));

        expected = new byte[]{ 2, 6, 0, 1, 2, 3 };
        out.clear();
        body.resetReaderIndex();
        XROSubobjectUtil.formatSubobject(2, false, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testTlvUtil() {
        byte[] expected = { 0, 4, 0, 4, 1, 2, 3, 4 };
        ByteBuf out = Unpooled.buffer();
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
        byte[] expected = { 4, 6, 1, 2, 3, 4 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4 });
        RROSubobjectUtil.formatSubobject(4, body, out);
        assertArrayEquals(expected, ByteArray.getAllBytes(out));
    }

    @Test
    public void testEROSubobjectUtil() {
        byte[] expected = { (byte) 0x82, 6, 0, 1, 2, 3 };
        ByteBuf out = Unpooled.buffer();
        ByteBuf body = Unpooled.copiedBuffer(new byte[] { 0, 1, 2, 3 });
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
        assertTrue(VendorInformationUtil.isVendorInformationTlv(VendorInformationUtil.VENDOR_INFORMATION_TLV_TYPE));
        assertFalse(VendorInformationUtil.isVendorInformationTlv(VendorInformationUtil.VENDOR_INFORMATION_OBJECT_CLASS));

        assertTrue(VendorInformationUtil.isVendorInformationObject(VendorInformationUtil.VENDOR_INFORMATION_OBJECT_CLASS, VendorInformationUtil.VENDOR_INFORMATION_OBJECT_TYPE));
        assertFalse(VendorInformationUtil.isVendorInformationObject(VendorInformationUtil.VENDOR_INFORMATION_OBJECT_CLASS, VendorInformationUtil.VENDOR_INFORMATION_TLV_TYPE));
        assertFalse(VendorInformationUtil.isVendorInformationObject(VendorInformationUtil.VENDOR_INFORMATION_TLV_TYPE, VendorInformationUtil.VENDOR_INFORMATION_OBJECT_TYPE));
        assertFalse(VendorInformationUtil.isVendorInformationObject(VendorInformationUtil.VENDOR_INFORMATION_OBJECT_TYPE, VendorInformationUtil.VENDOR_INFORMATION_OBJECT_CLASS));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testVendorInformationUtilPrivateConstructor() throws Throwable {
        final Constructor<VendorInformationUtil> c = VendorInformationUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testEROSubobjectUtilPrivateConstructor() throws Throwable {
        final Constructor<EROSubobjectUtil> c = EROSubobjectUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testRROSubobjectUtilPrivateConstructor() throws Throwable {
        final Constructor<RROSubobjectUtil> c = RROSubobjectUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testTlvUtilPrivateConstructor() throws Throwable {
        final Constructor<TlvUtil> c = TlvUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testXROSubobjectUtilPrivateConstructor() throws Throwable {
        final Constructor<XROSubobjectUtil> c = XROSubobjectUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testObjectUtilPrivateConstructor() throws Throwable {
        final Constructor<ObjectUtil> c = ObjectUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testMessageUtilPrivateConstructor() throws Throwable {
        final Constructor<MessageUtil> c = MessageUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testLabelUtilPrivateConstructor() throws Throwable {
        final Constructor<LabelUtil> c = LabelUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPCEPMessageConstantsPrivateConstructor() throws Throwable {
        final Constructor<PCEPMessageConstants> c = PCEPMessageConstants.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
