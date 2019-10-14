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
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Utility methods from interacting ByteBufs.
 */
public final class ByteBufUintUtil {
    private ByteBufUintUtil() {

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
}
