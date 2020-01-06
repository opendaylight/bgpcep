/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.Assert.assertArrayEquals;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Prefix;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6Prefix;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeMedium;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;

public class ByteBufWriteUtilTest {
    private static final byte[] FOUR_BYTE_ZEROS = { 0, 0, 0, 0 };

    @Test
    public void testWriteMediumValue() {
        final byte[] result = { 0, 0, 5 };
        final ByteBuf output = Unpooled.buffer(3);
        writeMedium(5, output);
        assertArrayEquals(result, output.array());

        output.clear();
        final byte[] resultZero = { 0, 0, 0 };
        writeMedium(null, output);
        assertArrayEquals(resultZero, output.array());
    }

    @Test
    public void testWriteIpv4Address() {
        final byte[] result = { 127, 0, 0, 1 };
        final ByteBuf output = Unpooled.buffer(Ipv4Util.IP4_LENGTH);
        writeIpv4Address(new Ipv4Address("127.0.0.1"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeIpv4Address(null, output);
        assertArrayEquals(FOUR_BYTE_ZEROS, output.array());
    }

    @Test
    public void testWriteIpv4Prefix() {
        final byte[] result = { 123, 122, 4, 5, 8 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.IPV4_PREFIX_BYTE_LENGTH);
        writeIpv4Prefix(new Ipv4Prefix("123.122.4.5/8"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        final byte[] zeroResult = { 0, 0, 0, 0, 0 };
        writeIpv4Prefix(null, output);
        assertArrayEquals(zeroResult, output.array());
    }

    @Test
    public void testWriteIpv6Address() {
        final byte[] result = { 0x20, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x01 };
        final ByteBuf output = Unpooled.buffer(Ipv6Util.IPV6_LENGTH);
        writeIpv6Address(new Ipv6Address("2001::1"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        final byte[] zeroResult = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,};
        writeIpv6Address(null, output);
        assertArrayEquals(zeroResult, output.array());
    }

    @Test
    public void testWriteIpv6Prefix() {
        final byte[] result = { 0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x40 };
        final ByteBuf output = Unpooled.buffer(ByteBufWriteUtil.IPV6_PREFIX_BYTE_LENGTH);
        writeIpv6Prefix(new Ipv6Prefix("2001:db8:1:2::/64"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        final byte[] zeroResult = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        writeIpv6Prefix(null, output);
        assertArrayEquals(zeroResult, output.array());
    }

    @Test
    public void testWriteFloat32() {
        final byte[] result = { 0, 0, 0, 5 };
        final ByteBuf output = Unpooled.buffer(Float.BYTES);
        writeFloat32(new Float32(result), output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeFloat32(null, output);
        assertArrayEquals(FOUR_BYTE_ZEROS, output.array());
    }
}
