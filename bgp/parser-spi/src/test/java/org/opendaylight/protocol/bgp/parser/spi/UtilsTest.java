/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static org.junit.Assert.assertArrayEquals;

import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;

public class UtilsTest {

    @Test
    public void testCapabilityUtil() {
        final byte[] result = new byte[] { 1, 2, 4, 8 };
        ByteBuf aggregator = Unpooled.buffer();
        CapabilityUtil.formatCapability(1, Unpooled.wrappedBuffer(new byte[] { 4, 8 }),aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testMessageUtil() {
        final byte[] result = new byte[] { UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, 0, 23, 3, 32, 5, 14, 21 };
        ByteBuf formattedMessage = Unpooled.buffer();
        MessageUtil.formatMessage(3, Unpooled.wrappedBuffer(new byte[] { 32, 5, 14, 21 }), formattedMessage);
        assertArrayEquals(result, ByteArray.getAllBytes(formattedMessage));
    }

    @Test
    public void testParameterUtil() {
        final byte[] result = new byte[] { 1, 2, 4, 8 };
        ByteBuf aggregator = Unpooled.buffer();
        ParameterUtil.formatParameter(1, Unpooled.wrappedBuffer(new byte[] { 4, 8 }), aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testAttributeUtil() {
        final byte[] result = new byte[] { 0x40, 03, 04, 10, 00, 00, 02 };
        ByteBuf aggregator = Unpooled.buffer();
        AttributeUtil.formatAttribute(64 , 3 , Unpooled.wrappedBuffer(new byte[] { 10, 0, 0, 2 }), aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testAttributeUtilExtended() {
        final byte[] value = new byte[258];
        Arrays.fill(value, 0, 258, UnsignedBytes.MAX_VALUE);
        final byte[] header = new byte[] { (byte) 0x50, 03, 01, 02 };
        final byte[] result = new byte[262];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(value, 0, result, 4, value.length);
        ByteBuf aggregator = Unpooled.buffer();
        AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE , 3 , Unpooled.wrappedBuffer(value), aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }
}
