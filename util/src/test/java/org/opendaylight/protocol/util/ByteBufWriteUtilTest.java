/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;

class ByteBufWriteUtilTest {
    private static final byte[] FOUR_BYTE_ZEROS = { 0, 0, 0, 0 };

    @Test
    void testWriteFloat32() {
        final byte[] result = { 0, 0, 0, 5 };
        final var output = Unpooled.buffer(Float.BYTES);
        ByteBufWriteUtil.writeFloat32(new Float32(result), output);
        assertArrayEquals(result, output.array());

        output.clear();
        ByteBufWriteUtil.writeFloat32(null, output);
        assertArrayEquals(FOUR_BYTE_ZEROS, output.array());
    }
}
