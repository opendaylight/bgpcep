/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;
import java.util.BitSet;
import org.opendaylight.protocol.util.ByteArray;

public final class ObjectUtil {

    private static final int HEADER_SIZE = 4;

    private static final int OT_SF_LENGTH = 4;
    /*
     * flags offsets inside multi-field
     */
    private static final int P_FLAG_OFFSET = 6;
    private static final int I_FLAG_OFFSET = 7;

    private ObjectUtil() {
        throw new UnsupportedOperationException();
    }

    public static void formatSubobject(final int objectType, final int objectClass, final Boolean processingRule, final Boolean ignore,
        final ByteBuf body, final ByteBuf out) {
        out.writeByte(objectClass);
        BitSet flags = new BitSet(Byte.SIZE);
        if (ignore != null) {
            flags.set(I_FLAG_OFFSET, ignore);
        }
        if (processingRule != null) {
            flags.set(P_FLAG_OFFSET, processingRule);
        }
        byte[] flagB = ByteArray.bitSetToBytes(flags, 1);
        int typeByte = objectType << OT_SF_LENGTH | flagB[0];
        out.writeByte(typeByte);
        out.writeShort(body.writerIndex() + HEADER_SIZE);
        out.writeBytes(body);
    }
}
