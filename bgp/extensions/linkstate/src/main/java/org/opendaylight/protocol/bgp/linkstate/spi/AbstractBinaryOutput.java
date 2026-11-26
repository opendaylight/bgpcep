/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.binding.EnumTypeObject;
import org.opendaylight.yangtools.binding.ScalarTypeObject;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Abstract base class for {@link BinaryOutput} implementations.
 */
public abstract class AbstractBinaryOutput<T extends AbstractBinaryOutput<T>> implements BinaryOutput {
    /**
     * {@return {@code this}}
     */
    protected abstract @NonNull T self();

    protected abstract void append1(int value);

    protected abstract void append2(int value);

    protected abstract void append4(int value);

    protected abstract void append8(long value);

    @Override
    public final T write1(final int value) {
        append1(value);
        return self();
    }

    @Override
    public final T write1(final Uint8 value) {
        return write1(value.byteValue());
    }

    @Override
    public final T write1(final ScalarTypeObject<Uint8> value) {
        return write1(value.getValue());
    }

    @Override
    public final T write2(final int value) {
        append2(value);
        return self();
    }

    @Override
    public final T write2(final Uint16 value) {
        return write2(value.shortValue());
    }

    @Override
    public final T write2(final ScalarTypeObject<Uint16> value) {
        return write2(value.getValue());
    }

    @Override
    public final T write2(final EnumTypeObject value) {
        return write2(value.getIntValue());
    }

    @Override
    public final T write4(final int value) {
        append4(value);
        return self();
    }

    @Override
    public final T write4(final Uint32 value) {
        return write4(value.intValue());
    }

    @Override
    public final T write4(final ScalarTypeObject<Uint32> value) {
        return write4(value.getValue());
    }

    @Override
    public final T write4(final EnumTypeObject value) {
        return write4(value.getIntValue());
    }

    @Override
    public final T write8(final long value) {
        append8(value);
        return self();
    }

    @Override
    public final T write8(final Uint64 value) {
        return write8(value.longValue());
    }

    @Override
    public final T write8(final ScalarTypeObject<Uint64> value) {
        return write8(value.getValue());
    }
}
