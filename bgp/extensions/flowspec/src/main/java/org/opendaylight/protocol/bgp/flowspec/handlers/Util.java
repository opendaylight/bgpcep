/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class Util {
    private Util() {
        // Hidden on purpose
    }

    /**
     * Given an uint16, this method instead of writing the value in 2B field, compresses the value to lowest required
     * byte field depending on the value.
     *
     * @param value uint16 to be written
     * @param buffer ByteBuf where the value will be written
     */
    public static void writeShortest(final Uint16 value, final ByteBuf buffer) {
        final int unsigned = value.toJava();
        if (unsigned <= Values.UNSIGNED_BYTE_MAX_VALUE) {
            buffer.writeByte(unsigned);
        } else {
            buffer.writeShort(unsigned);
        }
    }

    /**
     * Given an uint32, this method instead of writing the value in 4B field, compresses the value to lowest required
     * byte field depending on the value.
     *
     * @param value uint32 to be written
     * @param buffer ByteBuf where the value will be written
     */
    public static void writeShortest(final Uint32 value, final ByteBuf buffer) {
        final long unsigned = value.toJava();
        if (unsigned <= Values.UNSIGNED_BYTE_MAX_VALUE) {
            buffer.writeByte((int) unsigned);
        } else if (unsigned <= Values.UNSIGNED_SHORT_MAX_VALUE) {
            buffer.writeShort((int) unsigned);
        } else {
            buffer.writeInt(value.intValue());
        }
    }
}
