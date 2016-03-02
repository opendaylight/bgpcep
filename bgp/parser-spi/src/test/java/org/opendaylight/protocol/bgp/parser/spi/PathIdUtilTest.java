/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;

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
        PathIdUtil.writePathId(new PathId(0l), this.buffer);
        Assert.assertEquals(0, this.buffer.readableBytes());
    }

    @Test
    public void testWritePathId() {
        PathIdUtil.writePathId(new PathId(10l), this.buffer);
        Assert.assertEquals(Integer.BYTES, this.buffer.readableBytes());
    }

    @Test
    public void testReadPathId() {
        final long expected = 10L;
        ByteBufWriteUtil.writeUnsignedInt(expected, this.buffer);
        final PathId pathId = PathIdUtil.readPathId(this.buffer);
        Assert.assertEquals(expected, pathId.getValue().longValue());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReadPathIdBufferNull() {
        PathIdUtil.readPathId(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReadPathIdBufferEmpty() {
        PathIdUtil.readPathId(this.buffer);
    }

}
