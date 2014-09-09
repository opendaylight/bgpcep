/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;
import java.util.BitSet;
import org.opendaylight.protocol.util.ByteArray;

public final class LabelUtil {

    private static final int RES_F_LENGTH = 1;

    private static final int U_FLAG_OFFSET = 0;

    private static final int G_FLAG_OFFSET = 7;

    private LabelUtil() {
        throw new UnsupportedOperationException();
    }

    public static void formatLabel(final int type, final Boolean unidirectional, final Boolean global, final ByteBuf body, final ByteBuf buffer) {
        final BitSet reserved = new BitSet(RES_F_LENGTH * Byte.SIZE);
        if (unidirectional != null) {
            reserved.set(U_FLAG_OFFSET, unidirectional);
        }
        if (global != null) {
            reserved.set(G_FLAG_OFFSET, global);
        }
        buffer.writeBytes(ByteArray.bitSetToBytes(reserved, RES_F_LENGTH));
        buffer.writeByte(type);
        buffer.writeBytes(body);
    }
}
