/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link BitArrayWriter} implementation for 33..64 bits. Operates on a single {@code long} field.
 */
@NonNullByDefault
final class BitArrayWriter8 extends BitArrayWriter {
    private long bits;

    BitArrayWriter8(final int size) {
        super(size);
    }

    @Override
    void setBit(final int index) {
        bits |= 1L << index;
    }

    @Override
    public ByteBuf writeTo(final ByteBuf buf) {
        return length > 56 ? buf.writeLong(bits) : writeToSlow(buf);
    }

    private ByteBuf writeToSlow(final ByteBuf buf) {
        throw new UnsupportedOperationException();
        // word-swapping needed


//
//        final var head = (int) bits;
//        final var tail = (int) bits >>> 32;
//        if (length > 48) {
//            return buf.writeMedium(tail);
//        }
        // 49..56 -> 3
        // 41..48 -> 2
        // 33..40 -> 1

//
//        return length > 32
//
//        // 33..40
//        // 41..48
//        // 49..56
//        //
//
//        return buf;
    }
}
