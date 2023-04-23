/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.opendaylight.bgp.concepts.RouteDistinguisherUtil.SEPARATOR;

import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdTwoOctetAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;

public class RouteDistinguisherUtilTest {
    private static final String IP_ADDRESS = "1.2.3.4";
    private static final String IP_PORT = "10";
    private static final String ADMIN = "55";
    private static final String ASSIGNED_NUMBER = "65535";
    private static final String ASSIGNED_NUMBER_BIG = "4294967295";
    private static final byte[] AS_2B_BYTES = {0, 0, 0, 55, 0, 0, (byte) 0xff, (byte) 0xff};
    private static final byte[] AS_2B_BYTES_BIG = {0, 0, 0, 55, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    private static final byte[] IP_BYTES = {0, 1, 1, 2, 3, 4, 0, 10};
    private static final byte[] AS_4B_BYTES = {0, 2, 0, 0, 0, 55, (byte) 0xff, (byte) 0xff};
    private static final byte[] INVALID_RD_TYPE_BYTES = {0, 3, 0, 0, 0, 55, (byte) 0xff, (byte) 0xff};

    @Test
    public void testAs2BRouteDistinguisher() {
        final var expected = new RouteDistinguisher(
            new RdTwoOctetAs("0" + SEPARATOR + ADMIN + SEPARATOR + ASSIGNED_NUMBER));
        assertEquals(expected,
            RouteDistinguisherUtil.parseRouteDistinguisher("0" + SEPARATOR + ADMIN + SEPARATOR + ASSIGNED_NUMBER));

        final var parsed = RouteDistinguisherUtil.parseRouteDistinguisher(Unpooled.copiedBuffer(AS_2B_BYTES));
        assertEquals(expected, parsed);
        final var byteAggregator = Unpooled.buffer(AS_2B_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(parsed, byteAggregator);
        assertArrayEquals(AS_2B_BYTES, byteAggregator.array());
    }

    @Test
    public void testAs2BLongRouteDistinguisher() {
        final var expected = new RouteDistinguisher(new RdTwoOctetAs(
            "0" + SEPARATOR + ADMIN + SEPARATOR + ASSIGNED_NUMBER_BIG));
        assertEquals(expected,
            RouteDistinguisherUtil.parseRouteDistinguisher("0" + SEPARATOR + ADMIN + SEPARATOR + ASSIGNED_NUMBER_BIG));

        final var parsed = RouteDistinguisherUtil.parseRouteDistinguisher(Unpooled.copiedBuffer(AS_2B_BYTES_BIG));
        assertEquals(expected, parsed);
        final var byteAggregator = Unpooled.buffer(AS_2B_BYTES_BIG.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(parsed, byteAggregator);
        assertArrayEquals(AS_2B_BYTES_BIG, byteAggregator.array());
    }

    @Test
    public void testIpv4RouteDistinguisher() {
        final var expected = new RouteDistinguisher(new RdIpv4(IP_ADDRESS + SEPARATOR + IP_PORT));
        assertEquals(expected, RouteDistinguisherUtil.parseRouteDistinguisher(IP_ADDRESS + SEPARATOR + IP_PORT));

        final var parsed = RouteDistinguisherUtil.parseRouteDistinguisher(Unpooled.copiedBuffer(IP_BYTES));
        assertEquals(expected, parsed);
        final var byteAggregator = Unpooled.buffer(IP_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(parsed, byteAggregator);
        assertArrayEquals(IP_BYTES, byteAggregator.array());
    }

    @Test
    public void testAs4BRouteDistinguisher() {
        final var expected = new RouteDistinguisher(new RdAs(ADMIN + SEPARATOR + ASSIGNED_NUMBER));
        assertEquals(expected, RouteDistinguisherUtil.parseRouteDistinguisher(ADMIN + SEPARATOR + ASSIGNED_NUMBER));

        final var parsed = RouteDistinguisherUtil.parseRouteDistinguisher(Unpooled.copiedBuffer(AS_4B_BYTES));
        assertEquals(expected, parsed);
        final var byteAggregator = Unpooled.buffer(AS_4B_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(parsed, byteAggregator);
        assertArrayEquals(AS_4B_BYTES, byteAggregator.array());
    }

    @Test
    public void testInvalidRDType() {
        final var buf = Unpooled.copiedBuffer(INVALID_RD_TYPE_BYTES);
        assertThrows(IllegalArgumentException.class, () -> RouteDistinguisherUtil.parseRouteDistinguisher(buf));
    }
}
