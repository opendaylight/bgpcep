/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInts;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Values;

public final class Util {

    private Util() {
        throw new UnsupportedOperationException();
    }

    /**
     * Given the integer values, this method instead of writing the value
     * in 4B field, compresses the value to lowest required byte field
     * depending on the value.
     *
     * @param value integer to be written
     * @param buffer ByteBuf where the value will be written
     */
    protected static void writeShortest(final int value, final ByteBuf buffer) {
        if (value <= Values.UNSIGNED_BYTE_MAX_VALUE) {
            buffer.writeByte(UnsignedBytes.checkedCast(value));
        } else if (value <= Values.UNSIGNED_SHORT_MAX_VALUE) {
            ByteBufWriteUtil.writeUnsignedShort(value, buffer);
        } else if (value <= Values.UNSIGNED_INT_MAX_VALUE) {
            ByteBufWriteUtil.writeUnsignedInt(UnsignedInts.toLong(value), buffer);
        } else {
            buffer.writeLong(value);
        }
    }
}
