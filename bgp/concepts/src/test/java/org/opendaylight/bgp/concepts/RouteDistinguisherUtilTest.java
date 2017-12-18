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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;

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
    private static final char SEPARATOR = ':';

    /**
     * Create 4-octet AS RD or IPv4 RD, 2-octet AS RD cannot be created with this function.
     */
    private static RouteDistinguisher createRouteDistinguisher(final int type, final String administratorSubfield,
            final String assignedNumberSubfield) {
        final StringBuilder routeDistiguisher = new StringBuilder();
        if (type == 0) {
            routeDistiguisher.append(type).append(SEPARATOR);
        }
        routeDistiguisher.append(administratorSubfield);
        routeDistiguisher.append(SEPARATOR);
        routeDistiguisher.append(assignedNumberSubfield);
        return RouteDistinguisherBuilder.getDefaultInstance(routeDistiguisher.toString());
    }

    @Test
    public void testAs2BRouteDistinguisher() {
        final RouteDistinguisher expected = createRouteDistinguisher(0, ADMIN, ASSIGNED_NUMBER);
        final RouteDistinguisher parsed = RouteDistinguisherUtil
            .parseRouteDistinguisher(Unpooled.copiedBuffer(AS_2B_BYTES));
        assertEquals(expected.getRdTwoOctetAs(), parsed.getRdTwoOctetAs());
        final ByteBuf byteAggregator = Unpooled.buffer(AS_2B_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(expected, byteAggregator);
        assertArrayEquals(AS_2B_BYTES, byteAggregator.array());
        assertEquals("0" + SEPARATOR + ADMIN + SEPARATOR + ASSIGNED_NUMBER,
                parsed.getRdTwoOctetAs().getValue());
    }

    @Test
    public void testAs2BLongRouteDistinguisher() {
        final RouteDistinguisher expected = createRouteDistinguisher(0, ADMIN, ASSIGNED_NUMBER_BIG);
        final RouteDistinguisher parsed = RouteDistinguisherUtil
            .parseRouteDistinguisher(Unpooled.copiedBuffer(AS_2B_BYTES_BIG));
        assertEquals(expected.getRdTwoOctetAs(), parsed.getRdTwoOctetAs());
        final ByteBuf byteAggregator = Unpooled.buffer(AS_2B_BYTES_BIG.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(expected, byteAggregator);
        assertArrayEquals(AS_2B_BYTES_BIG, byteAggregator.array());
        assertEquals("0" + SEPARATOR + ADMIN + SEPARATOR + ASSIGNED_NUMBER_BIG,
                parsed.getRdTwoOctetAs().getValue());
    }

    @Test
    public void testIpv4RouteDistinguisher() {
        final RouteDistinguisher expected = createRouteDistinguisher(1, IP_ADDRESS, IP_PORT);
        final RouteDistinguisher parsed = RouteDistinguisherUtil
            .parseRouteDistinguisher(Unpooled.copiedBuffer(IP_BYTES));
        assertEquals(expected.getRdIpv4(), parsed.getRdIpv4());
        final ByteBuf byteAggregator = Unpooled.buffer(IP_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(expected, byteAggregator);
        assertArrayEquals(IP_BYTES, byteAggregator.array());
        assertEquals(IP_ADDRESS + SEPARATOR + IP_PORT, parsed.getRdIpv4().getValue());
    }

    @Test
    public void testAs4BRouteDistinguisher() {
        final RouteDistinguisher expected = createRouteDistinguisher(2, ADMIN, ASSIGNED_NUMBER);
        final RouteDistinguisher parsed = RouteDistinguisherUtil
            .parseRouteDistinguisher(Unpooled.copiedBuffer(AS_4B_BYTES));
        assertEquals(expected.getRdAs(), parsed.getRdAs());
        final ByteBuf byteAggregator = Unpooled.buffer(AS_4B_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(expected, byteAggregator);
        assertArrayEquals(AS_4B_BYTES, byteAggregator.array());
        assertEquals(ADMIN + SEPARATOR + ASSIGNED_NUMBER, parsed.getRdAs().getValue());
    }

    @Test
    public void testParseRouteDistinguisher() {
        final RouteDistinguisher expected = RouteDistinguisherUtil
            .parseRouteDistinguisher(ADMIN + SEPARATOR + ASSIGNED_NUMBER);
        final RouteDistinguisher parsed = RouteDistinguisherUtil
            .parseRouteDistinguisher(Unpooled.copiedBuffer(AS_4B_BYTES));
        assertEquals(expected.getRdAs(), parsed.getRdAs());

        final RouteDistinguisher expectedRD = RouteDistinguisherUtil.parseRouteDistinguisher(expected);
        assertEquals(expectedRD.getRdAs(), parsed.getRdAs());

        final RouteDistinguisher expectedObj = RouteDistinguisherUtil
            .parseRouteDistinguisher((Object) (ADMIN + SEPARATOR + ASSIGNED_NUMBER));
        assertEquals(expectedObj.getRdAs(), parsed.getRdAs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRDType() {
        RouteDistinguisherUtil.parseRouteDistinguisher(Unpooled.copiedBuffer(INVALID_RD_TYPE_BYTES));
    }
}
