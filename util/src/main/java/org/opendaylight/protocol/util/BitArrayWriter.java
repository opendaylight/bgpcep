/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static java.util.Objects.checkIndex;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Mutable;

/**
 * A utility that can construct an array of bits and write it out into a {@link ByteBuf}.
 */
@NonNullByDefault
public abstract sealed class BitArrayWriter implements Mutable permits BitArrayWriter4, BitArrayWriter8 {
    final int length;

    BitArrayWriter(final int length) {
        this.length = length;
    }

    public static final BitArrayWriter of(final int length) {
        if (length < 1) {
            throw new IllegalArgumentException("non-positive length " + length);
        }
        if (length <= 32) {
            return new BitArrayWriter4(length);
        }
        if (length <= 64) {
            return new BitArrayWriter8(length);
        }
        // FIXME: array-based 65+ bits?
        throw new IllegalArgumentException("unsupported length " + length);
    }

    public final BitArrayWriter setBit(final int index, final @Nullable Boolean value) {
        checkIndex(index, length);
        if (Boolean.TRUE.equals(value)) {
            setBit(index);
        }
        return this;
    }

    public final BitArrayWriter setBit(final int index, final boolean value) {
        checkIndex(index, length);
        if (value) {
            setBit(index);
        }
        return this;
    }

    abstract void setBit(int index);

    public abstract ByteBuf writeTo(ByteBuf buf);
}
