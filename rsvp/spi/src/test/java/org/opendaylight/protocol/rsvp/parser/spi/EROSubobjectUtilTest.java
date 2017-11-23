/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi;

import static org.junit.Assert.assertArrayEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

public class EROSubobjectUtilTest {

    @Test(expected = UnsupportedOperationException.class)
    @SuppressWarnings({ "checkstyle:IllegalThrows", "checkstyle:avoidHidingCauseException" })
    public void testPrivateConstructor() throws Throwable {
        final Constructor<EROSubobjectUtil> c = EROSubobjectUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testFormatSubobject1() {
        final byte[] array = new byte[]{2, 3};
        final byte[] expected = new byte[]{(byte) 0x81, 4, 2, 3};
        final ByteBuf body = Unpooled.copiedBuffer(array);
        final ByteBuf aggregator = Unpooled.buffer(4);
        EROSubobjectUtil.formatSubobject(1, Boolean.TRUE, body, aggregator);
        assertArrayEquals(expected, aggregator.array());
    }

    @Test
    public void testFormatSubobject2() {
        final byte[] array = new byte[]{2, 3};
        final byte[] expected = new byte[]{1, 4, 2, 3};
        final ByteBuf body = Unpooled.copiedBuffer(array);
        final ByteBuf aggregator = Unpooled.buffer(4);
        EROSubobjectUtil.formatSubobject(1, Boolean.FALSE, body, aggregator);
        assertArrayEquals(expected, aggregator.array());
    }

    @Test
    public void testFormatSubobject3() {
        final byte[] array = new byte[]{2, 3};
        final byte[] expected = new byte[]{1, 4, 2, 3};
        final ByteBuf body = Unpooled.copiedBuffer(array);
        final ByteBuf aggregator = Unpooled.buffer(4);
        EROSubobjectUtil.formatSubobject(1, null, body, aggregator);
        assertArrayEquals(expected, aggregator.array());
    }
}
