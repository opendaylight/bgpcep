/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;

class Ipv4UtilTest {
    private static final byte[] FOUR_BYTE_ZEROS = { 0, 0, 0, 0 };

    @Test
    void testWriteIpv4Address() {
        final var output = Unpooled.buffer(Ipv4Util.IP4_LENGTH);
        Ipv4Util.writeIpv4Address(new Ipv4AddressNoZone("127.0.0.1"), output);
        assertArrayEquals(new byte[] { 127, 0, 0, 1 }, output.array());

        output.clear();
        Ipv4Util.writeIpv4Address(null, output);
        assertArrayEquals(FOUR_BYTE_ZEROS, output.array());
    }

    @Test
    void testWriteIpv4Prefix() {
        final var output = Unpooled.buffer(Ipv4Util.PREFIX_BYTE_LENGTH);
        Ipv4Util.writeIpv4Prefix(new Ipv4Prefix("123.122.4.5/8"), output);
        assertArrayEquals(new byte[] { 123, 122, 4, 5, 8 }, output.array());

        output.clear();
        Ipv4Util.writeIpv4Prefix(null, output);
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0 }, output.array());
    }
}
