/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

public class PathKeyUtilsTest {
    private final byte[] bytes = new byte[] { 0, 1, 2, 3, 4, 5 };
    private PathKey key1;
    private PathKey key2;
    private PathKey key3;

    @Before
    public void setUp() {
        this.key1 = new PathKeyBuilder().build();
        this.key2 = new PathKeyBuilder().setPathKey(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
            .yang.rsvp.rev150820.PathKey(Uint16.ONE))
            .build();
        this.key3 = new PathKeyBuilder().setPathKey(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
            .yang.rsvp.rev150820.PathKey(Uint16.ONE))
            .setPceId(new PceId(new byte[]{2, 3, 4, 5}))
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSerializationExcption1() {
        PathKeyUtils.serializePathKey(this.key1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSerializationExcption2() {
        PathKeyUtils.serializePathKey(this.key2);
    }

    @Test
    public void testSerialization() {
        final ByteBuf output = PathKeyUtils.serializePathKey(this.key3);
        assertArrayEquals(this.bytes, ByteArray.readAllBytes(output));
    }

    @Test
    public void testParsing() {
        assertEquals(this.key3, PathKeyUtils.parsePathKey(4, Unpooled.copiedBuffer(this.bytes)));
    }
}
