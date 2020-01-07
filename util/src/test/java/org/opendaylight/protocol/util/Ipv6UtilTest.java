/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.Assert.assertArrayEquals;
import static org.opendaylight.protocol.util.Ipv6Util.writeIpv6Address;
import static org.opendaylight.protocol.util.Ipv6Util.writeIpv6Prefix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;

public class Ipv6UtilTest {
    @Test
    public void testWriteIpv6Address() {
        final byte[] result = { 0x20, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x01 };
        final ByteBuf output = Unpooled.buffer(Ipv6Util.IPV6_LENGTH);
        writeIpv6Address(new Ipv6AddressNoZone("2001::1"), output);
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
        final ByteBuf output = Unpooled.buffer(Ipv6Util.PREFIX_BYTE_LENGTH);
        writeIpv6Prefix(new Ipv6Prefix("2001:db8:1:2::/64"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        final byte[] zeroResult = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        writeIpv6Prefix(null, output);
        assertArrayEquals(zeroResult, output.array());
    }
}
