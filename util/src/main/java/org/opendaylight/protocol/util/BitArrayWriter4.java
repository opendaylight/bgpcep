/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import io.netty.buffer.ByteBuf;

/**
 * A {@link BitArrayWriter} implementation for 1..32 bits. Operates on a single {@code int} field.
 */
final class BitArrayWriter4 extends BitArrayWriter {
    private int bits;

    BitArrayWriter4(final int size) {
        super(size);
    }

    @Override
    void setBit(final int index) {
        bits |= 1 << index;
    }

    @Override
    public ByteBuf writeTo(final ByteBuf buf) {
        if (length <= 16) {
            return length <= 8 ? buf.writeByte(bits) : buf.writeShort(bits);
        }
        return length <= 24 ? buf.writeMedium(bits) : buf.writeInt(bits);
    }
}
