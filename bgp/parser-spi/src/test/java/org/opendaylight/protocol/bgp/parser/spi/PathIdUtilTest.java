/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

class PathIdUtilTest {
    private final ByteBuf buffer = Unpooled.buffer();

    @Test
    void testWritePathIdNull() {
        PathIdUtil.writePathId(null, buffer);
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void testWritePathIdZero() {
        PathIdUtil.writePathId(PathIdUtil.NON_PATH_ID, buffer);
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void testWritePathId() {
        PathIdUtil.writePathId(new PathId(Uint32.TEN), buffer);
        assertEquals(Integer.BYTES, buffer.readableBytes());
    }

    @Test
    void testReadPathId() {
        buffer.writeInt(10);
        final PathId pathId = PathIdUtil.readPathId(buffer);
        assertEquals(Uint32.TEN, pathId.getValue());
    }

    @Test
    void testExtractPathId() {
        final NodeIdentifier NII = new NodeIdentifier(QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2015-03-05", "path-id").intern());
        final ContainerNode cont = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(NII)
            .addChild(ImmutableNodes.leafNode(NII, Uint32.ZERO))
            .build();
        assertEquals(0L, PathIdUtil.extractPathId(cont, NII).longValue());
    }

    @Test
    void testReadPathIdBufferNull() {
        assertThrows(IllegalArgumentException.class, () -> PathIdUtil.readPathId(null));
    }

    @Test
    void testReadPathIdBufferEmpty() {
        assertThrows(IllegalArgumentException.class, () -> PathIdUtil.readPathId(buffer));
    }
}
