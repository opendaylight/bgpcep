/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class BitArrayWriterTest {
    @Test
    void testEightBits() {
        final var buf = Unpooled.buffer();
        BitArrayWriter.of(8).setBit(3, true).writeTo(buf);
        assertEquals(1, buf.readableBytes());
        assertEquals((byte) 0b00001000, buf.readByte());
    }
}
