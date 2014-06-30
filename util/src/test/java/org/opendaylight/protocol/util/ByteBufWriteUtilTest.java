/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.util;

import static org.junit.Assert.assertArrayEquals;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeBitSetOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeBooleanOrZero;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeByteOrZero;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32OrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIntOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4AddressOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4PrefixOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6AddressOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6PrefixOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeLongOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeMediumOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeShortOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByteOrZero;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedIntOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedLongOrZeros;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShortOrZeros;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.math.BigInteger;
import java.util.BitSet;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;

public class ByteBufWriteUtilTest {

    private static final byte[] ONE_BYTE_ZERO = {0};

    private static final byte[] TWO_BYTE_ZEROS = {0, 0};

    private static final byte[] FOUR_BYTE_ZEROS = {0, 0, 0, 0};

    private static final byte[] EIGHT_BYTE_ZEROS = { 0, 0, 0, 0, 0, 0, 0, 0 };

    @Test
    public void testWriteIntegerValue() {
        final byte[] result = { 0, 0, 0, 5 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.INT_BYTES_LENGTH);
        writeIntOrZeros(5, output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeIntOrZeros(null, output);
        assertArrayEquals(FOUR_BYTE_ZEROS, output.array());
    }

    @Test
    public void testWriteShortValue() {
        final byte[] result = { 0, 5 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.SHORT_BYTES_LENGTH);
        writeShortOrZeros((short) 5, output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeShortOrZeros(null, output);
        assertArrayEquals(TWO_BYTE_ZEROS, output.array());
    }

    @Test
    public void testWriteMediumValue() {
        final byte[] result = { 0, 0, 5 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.MEDIUM_BYTES_LENGTH);
        writeMediumOrZeros(5, output);
        assertArrayEquals(result, output.array());

        output.clear();
        final byte[] resultZero = { 0, 0, 0 };
        writeMediumOrZeros(null, output);
        assertArrayEquals(resultZero, output.array());
    }

    @Test
    public void testWriteLongValue() {
        final byte[] result = { 0, 0, 0, 0, 0, 0, 0, 5 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.LONG_BYTES_LENGTH);
        writeLongOrZeros((long) 5, output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeLongOrZeros(null, output);
        assertArrayEquals(EIGHT_BYTE_ZEROS, output.array());
    }

    @Test
    public void testWriteBooleanValue() {
        final byte[] result = { 1 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.ONE_BYTE_LENGTH);
        writeBooleanOrZero(true, output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeBooleanOrZero(null, output);
        assertArrayEquals(ONE_BYTE_ZERO, output.array());
    }

    @Test
    public void testWriteByteValue() {
        final byte[] result = { 0x0C };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.ONE_BYTE_LENGTH);
        writeByteOrZero((byte) 0x0C, output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeByteOrZero(null, output);
        assertArrayEquals(ONE_BYTE_ZERO, output.array());
    }

    @Test
    public void testWriteUnsignedByteValue() {
        final byte[] result = { 5 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.ONE_BYTE_LENGTH);
        writeUnsignedByteOrZero((short) 5, output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeUnsignedByteOrZero(null, output);
        assertArrayEquals(ONE_BYTE_ZERO, output.array());
    }

    @Test
    public void testWriteUnsignedShortValue() {
        final byte[] result = { 0, 5 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.SHORT_BYTES_LENGTH);
        writeUnsignedShortOrZeros(5, output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeUnsignedShortOrZeros(null, output);
        assertArrayEquals(TWO_BYTE_ZEROS, output.array());
    }

    @Test
    public void testWriteUnsignedIntValue() {
        final byte[] result = { 0, 0, 0, 5 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.INT_BYTES_LENGTH);
        ByteBufWriteUtil.writeUnsignedIntOrZeros((long) 5, output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeUnsignedIntOrZeros(null, output);
        assertArrayEquals(FOUR_BYTE_ZEROS, output.array());
    }

    @Test
    public void testWriteUnsignedLongValue() {
        final byte[] result = { 0, 0, 0, 0, 0, 0, 0, 5 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.LONG_BYTES_LENGTH);
        writeUnsignedLongOrZeros(new BigInteger("5"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeUnsignedLongOrZeros(null, output);
        assertArrayEquals(EIGHT_BYTE_ZEROS, output.array());
    }

    @Test
    public void testWriteIpv4Address() {
        final byte[] result = { 127, 0, 0, 1 };
        final ByteBuf output = Unpooled.buffer(Ipv4Util.IP4_LENGTH);
        writeIpv4AddressOrZeros(new Ipv4Address("127.0.0.1"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeIpv4AddressOrZeros(null, output);
        assertArrayEquals(FOUR_BYTE_ZEROS, output.array());
    }

    @Test
    public void testWriteIpv4Prefix() {
        final byte[] result = { 123, 122, 4, 5, 8 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.IPV4_PREFIX_BYTE_LENGTH);
        writeIpv4PrefixOrZeros(new Ipv4Prefix("123.122.4.5/8"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        final byte[] zeroResult = { 0, 0, 0, 0, 0 };
        writeIpv4PrefixOrZeros(null, output);
        assertArrayEquals(zeroResult, output.array());
    }

    @Test
    public void testWriteIpv6Address() {
        final byte[] result = { 0x20, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x01 };
        final ByteBuf output = Unpooled.buffer(Ipv6Util.IPV6_LENGTH);
        writeIpv6AddressOrZeros(new Ipv6Address("2001::1"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        final byte[] zeroResult = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,};
        writeIpv6AddressOrZeros(null, output);
        assertArrayEquals(zeroResult, output.array());
    }

    @Test
    public void testWriteIpv6Prefix() {
        final byte[] result = { 0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x40 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.IPV6_PREFIX_BYTE_LENGTH);
        writeIpv6PrefixOrZeros(new Ipv6Prefix("2001:db8:1:2::/64"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        final byte[] zeroResult = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        writeIpv6PrefixOrZeros(null, output);
        assertArrayEquals(zeroResult, output.array());
    }

    @Test
    public void testWriteFloat32() {
        final byte[] result = { 0, 0, 0, 5 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.FLOAT32_BYTES_LENGTH);
        writeFloat32OrZeros(new Float32(result), output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeFloat32OrZeros(null, output);
        assertArrayEquals(FOUR_BYTE_ZEROS, output.array());
    }

    @Test
    public void testWriteBitSet() {
        final byte[] result = { 1 };
        final ByteBuf output = Unpooled.buffer(1);
        final BitSet bitSet = new BitSet(8);
        bitSet.set(7);
        writeBitSetOrZeros(bitSet, 1, output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeBitSetOrZeros(null, 1, output);
        assertArrayEquals(ONE_BYTE_ZERO, output.array());
    }
}
