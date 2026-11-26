/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.binding.EnumTypeObject;
import org.opendaylight.yangtools.binding.ScalarTypeObject;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Core contract of outputting various YANG-modeled constructs into a binary stream.
 */
public interface BinaryOutput {
    /**
     * Appends a single byte. The 24 high-order bits of the specified value are ignored.
     *
     * @param value the byte to append
     * @return this object
     * @throws IndexOutOfBoundsException if the TLV body length or underlying {@link ByteBuf#maxCapacity()} would be
     *                                   exceeded
     */
    @NonNullByDefault
    BinaryOutput write1(int value);

    /**
     * Appends a single byte.
     *
     * @param value the byte to append
     * @return this object
     * @throws IndexOutOfBoundsException if the TLV body length or underlying {@link ByteBuf#maxCapacity()} would be
     *                                   exceeded
     */
    @NonNullByDefault
    BinaryOutput write1(Uint8 value);

    @NonNull BinaryOutput write1(@NonNull ScalarTypeObject<Uint8> value);

    @NonNullByDefault
    BinaryOutput write2(int value);

    @NonNullByDefault
    BinaryOutput write2(Uint16 value);

    @NonNull BinaryOutput write2(@NonNull ScalarTypeObject<Uint16> value);

    @NonNullByDefault
    BinaryOutput write2(EnumTypeObject value);

    @NonNullByDefault
    BinaryOutput write4(int value);

    @NonNullByDefault
    BinaryOutput write4(Uint32 value);

    @NonNull BinaryOutput write4(@NonNull ScalarTypeObject<Uint32> value);

    @NonNullByDefault
    BinaryOutput write4(EnumTypeObject value);

    @NonNullByDefault
    BinaryOutput write8(long value);

    @NonNullByDefault
    BinaryOutput write8(Uint64 value);

    @NonNull BinaryOutput write8(@NonNull ScalarTypeObject<Uint64> value);
}
