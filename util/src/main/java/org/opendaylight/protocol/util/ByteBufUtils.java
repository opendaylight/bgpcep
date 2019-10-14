/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Utility methods for interacting with {@link ByteBuf}s. These add a number of methods for reading and writing various
 * data types from/to ByteBufs.
 */
public final class ByteBufUtils {
    private ByteBufUtils() {

    }

    public static @NonNull Uint8 readUint8(final ByteBuf buf) {
        return Uint8.fromByteBits(buf.readByte());
    }

    public static @NonNull Uint16 readUint16(final ByteBuf buf) {
        return Uint16.fromShortBits(buf.readShort());
    }

    // TODO: this probably wants a dedicated concept
    public static @NonNull Uint32 readUint24(final ByteBuf buf) {
        return Uint32.fromIntBits(buf.readMedium());
    }

    public static @NonNull Uint32 readUint32(final ByteBuf buf) {
        return Uint32.fromIntBits(buf.readInt());
    }

    public static @NonNull Uint64 readUint64(final ByteBuf buf) {
        return Uint64.fromLongBits(buf.readLong());
    }

    public static void writeMandatory(final ByteBuf buf, final Byte value, final String name) {
        buf.writeByte(nonNullArgument(value, name).byteValue());
    }

    public static void writeMandatory(final ByteBuf buf, final Short value, final String name) {
        buf.writeShort(nonNullArgument(value, name).shortValue());
    }

    public static void writeMandatory(final ByteBuf buf, final Integer value, final String name) {
        buf.writeInt(nonNullArgument(value, name).intValue());
    }

    public static void writeMandatory(final ByteBuf buf, final Long value, final String name) {
        buf.writeLong(nonNullArgument(value, name).longValue());
    }

    public static void writeMandatory(final ByteBuf buf, final Uint8 value, final String name) {
        buf.writeByte(nonNullArgument(value, name).byteValue());
    }

    public static void writeMandatory(final ByteBuf buf, final Uint16 value, final String name) {
        buf.writeShort(nonNullArgument(value, name).shortValue());
    }

    public static void writeMandatory(final ByteBuf buf, final Uint32 value, final String name) {
        buf.writeInt(nonNullArgument(value, name).intValue());
    }

    public static void writeMandatory(final ByteBuf buf, final Uint64 value, final String name) {
        buf.writeLong(nonNullArgument(value, name).longValue());
    }

    public static void writeOptional(final ByteBuf buf, final @Nullable Byte value) {
        if (value != null) {
            buf.writeByte(value.byteValue());
        }
    }

    public static void writeOptional(final ByteBuf buf, final @Nullable Short value) {
        if (value != null) {
            buf.writeShort(value.shortValue());
        }
    }

    public static void writeOptional(final ByteBuf buf, final @Nullable Integer value) {
        if (value != null) {
            buf.writeInt(value.intValue());
        }
    }

    public static void writeOptional(final ByteBuf buf, final @Nullable Long value) {
        if (value != null) {
            buf.writeLong(value.longValue());
        }
    }

    public static void writeOptional(final ByteBuf buf, final @Nullable Uint8 value) {
        if (value != null) {
            buf.writeByte(value.byteValue());
        }
    }

    public static void writeOptional(final ByteBuf buf, final @Nullable Uint16 value) {
        if (value != null) {
            buf.writeShort(value.shortValue());
        }
    }

    public static void writeOptional(final ByteBuf buf, final @Nullable Uint32 value) {
        if (value != null) {
            buf.writeInt(value.intValue());
        }
    }

    public static void writeOptional(final ByteBuf buf, final @Nullable Uint64 value) {
        if (value != null) {
            buf.writeLong(value.longValue());
        }
    }

    public static void writeOrZero(final ByteBuf buf, final @Nullable Byte value) {
        buf.writeByte(value != null ? value.byteValue() : 0);
    }

    public static void writeOrZero(final ByteBuf buf, final @Nullable Short value) {
        buf.writeShort(value != null ? value.shortValue() : (short) 0);
    }

    public static void writeOrZero(final ByteBuf buf, final @Nullable Integer value) {
        buf.writeInt(value != null ? value.intValue() : 0);
    }

    public static void writeOrZero(final ByteBuf buf, final @Nullable Long value) {
        buf.writeLong(value != null ? value.longValue() : 0L);
    }

    public static void writeOrZero(final ByteBuf buf, final @Nullable Uint8 value) {
        buf.writeByte(value != null ? value.byteValue() : 0);
    }

    public static void writeOrZero(final ByteBuf buf, final @Nullable Uint16 value) {
        buf.writeShort(value != null ? value.shortValue() : (short) 0);
    }

    public static void writeOrZero(final ByteBuf buf, final @Nullable Uint32 value) {
        buf.writeInt(value != null ? value.intValue() : 0);
    }

    public static void writeOrZero(final ByteBuf buf, final @Nullable Uint64 value) {
        buf.writeLong(value != null ? value.longValue() : 0L);
    }

    private static <T> @NonNull T nonNullArgument(final @Nullable T obj, final String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " is mandatory");
        }
        return obj;
    }
}
