/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeSchemaAwareBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

public class PathIdUtilTest {

    private ByteBuf buffer;

    @Before
    public void setUp() {
        this.buffer = Unpooled.buffer();
    }

    @Test
    public void testWritePathIdNull() {
        PathIdUtil.writePathId(null, this.buffer);
        Assert.assertEquals(0, this.buffer.readableBytes());
    }

    @Test
    public void testWritePathIdZero() {
        PathIdUtil.writePathId(NON_PATH_ID, this.buffer);
        Assert.assertEquals(0, this.buffer.readableBytes());
    }

    @Test
    public void testWritePathId() {
        PathIdUtil.writePathId(new PathId(Uint32.TEN), this.buffer);
        Assert.assertEquals(Integer.BYTES, this.buffer.readableBytes());
    }

    @Test
    public void testReadPathId() {
        this.buffer.writeInt(10);
        final PathId pathId = PathIdUtil.readPathId(this.buffer);
        Assert.assertEquals(Uint32.TEN, pathId.getValue());
    }

    @Test
    public void testExtractPathId() {
        final NodeIdentifier NII = new NodeIdentifier(QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2015-03-05", "path-id").intern());
        final ContainerNode cont = ImmutableContainerNodeSchemaAwareBuilder.create().withNodeIdentifier(NII).addChild(
            new ImmutableLeafNodeBuilder<>().withNodeIdentifier(NII).withValue(Uint32.ZERO).build()).build();
        Assert.assertEquals(0L, PathIdUtil.extractPathId(cont, NII).longValue());
    }


    @Test(expected = IllegalArgumentException.class)
    public void testReadPathIdBufferNull() {
        PathIdUtil.readPathId(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadPathIdBufferEmpty() {
        PathIdUtil.readPathId(this.buffer);
    }
}
