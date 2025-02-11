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

import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;

class Ipv6UtilTest {
    @Test
    void testWriteIpv6Address() {
        final var output = Unpooled.buffer(Ipv6Util.IPV6_LENGTH);
        writeIpv6Address(new Ipv6AddressNoZone("2001::1"), output);
        assertArrayEquals(new byte[] {
            0x20, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
        }, output.array());

        output.clear();
        writeIpv6Address(null, output);
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, output.array());
    }

    @Test
    void testWriteIpv6Prefix() {
        final var output = Unpooled.buffer(Ipv6Util.PREFIX_BYTE_LENGTH);
        writeIpv6Prefix(new Ipv6Prefix("2001:db8:1:2::/64"), output);
        assertArrayEquals(new byte[] {
            0x20, 0x01, 0x0d, (byte) 0xb8, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x40
        }, output.array());

        output.clear();
        writeIpv6Prefix(null, output);
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, output.array());
    }
}
