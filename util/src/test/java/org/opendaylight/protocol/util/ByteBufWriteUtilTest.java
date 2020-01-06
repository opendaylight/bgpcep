/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.Assert.assertArrayEquals;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;

public class ByteBufWriteUtilTest {
    private static final byte[] FOUR_BYTE_ZEROS = { 0, 0, 0, 0 };

    @Test
    public void testWriteFloat32() {
        final byte[] result = { 0, 0, 0, 5 };
        final ByteBuf output = Unpooled.buffer(Float.BYTES);
        writeFloat32(new Float32(result), output);
        assertArrayEquals(result, output.array());

        output.clear();
        writeFloat32(null, output);
        assertArrayEquals(FOUR_BYTE_ZEROS, output.array());
    }
}
