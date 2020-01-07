/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.Assert.assertArrayEquals;
import static org.opendaylight.protocol.util.Ipv4Util.writeIpv4Address;
import static org.opendaylight.protocol.util.Ipv4Util.writeIpv4Prefix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;

public class Ipv4UtilTest {
    private static final byte[] FOUR_BYTE_ZEROS = { 0, 0, 0, 0 };

    @Test
    public void testWriteIpv4Address() {
        final byte[] result = { 127, 0, 0, 1 };
        final ByteBuf output = Unpooled.buffer(Ipv4Util.IP4_LENGTH);
        writeIpv4Address(new Ipv4AddressNoZone("127.0.0.1"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeIpv4Address(null, output);
        assertArrayEquals(FOUR_BYTE_ZEROS, output.array());
    }

    @Test
    public void testWriteIpv4Prefix() {
        final byte[] result = { 123, 122, 4, 5, 8 };
        final ByteBuf output = Unpooled.buffer(Ipv4Util.PREFIX_BYTE_LENGTH);
        writeIpv4Prefix(new Ipv4Prefix("123.122.4.5/8"), output);
        assertArrayEquals(result, output.array());

        output.clear();
        final byte[] zeroResult = { 0, 0, 0, 0, 0 };
        writeIpv4Prefix(null, output);
        assertArrayEquals(zeroResult, output.array());
    }
}
