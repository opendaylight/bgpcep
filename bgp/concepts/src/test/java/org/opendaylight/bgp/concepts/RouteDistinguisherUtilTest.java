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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;

public class RouteDistinguisherUtilTest {

    private static final String IP_ADDRESS = "1.2.3.4";
    private static final String ADMIN = "55";
    private static final byte[] IP_BYTES = { 0, 1, 1, 2, 3, 4, 0, 10 };
    private static final byte[] AS_4B_BYTES = { 0, 2, 0,0, 0, 55, 0, 8 };
    private static final char SEPARATOR = ':';

    @Test
    public void testIpv4RouteDistinguisher() {
        final RouteDistinguisher expected = createRouteDistinguisher(1, 10L, IP_ADDRESS);
        final RouteDistinguisher parsed = RouteDistinguisherUtil.parseRouteDistinguisher(Unpooled.copiedBuffer(IP_BYTES));
        assertEquals(expected.getString(), parsed.getString());
        final ByteBuf byteAggregator = Unpooled.buffer(IP_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(expected, byteAggregator);
        assertArrayEquals(IP_BYTES, byteAggregator.array());
    }

    @Test
    public void testAs4BRouteDistinguisher() {
        final RouteDistinguisher expected = createRouteDistinguisher(2, 8L, ADMIN);
        final RouteDistinguisher parsed = RouteDistinguisherUtil.parseRouteDistinguisher(Unpooled.copiedBuffer(AS_4B_BYTES));
        assertEquals(expected.getString(), parsed.getString());
        final ByteBuf byteAggregator = Unpooled.buffer(AS_4B_BYTES.length);
        RouteDistinguisherUtil.serializeRouteDistinquisher(expected, byteAggregator);
        assertArrayEquals(AS_4B_BYTES, byteAggregator.array());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<RouteDistinguisherUtil> c = RouteDistinguisherUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private RouteDistinguisher createRouteDistinguisher(final int type, final long assignedNumberSubfield, final String administratorSubfield) {
        final StringBuffer routeDistiguisher = new StringBuffer();
        routeDistiguisher.append(administratorSubfield);
        routeDistiguisher.append(SEPARATOR);
        routeDistiguisher.append(assignedNumberSubfield);
        return new RouteDistinguisher(routeDistiguisher.toString());
    }

}
