/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.BitArray;

public final class ObjectUtil {

    private static final int HEADER_SIZE = 4;

    private static final int FLAGS_SIZE = 4;
    /*
     * flags offsets inside multi-field
     */
    private static final int PROCESSED = 2;
    private static final int IGNORED = 3;

    private ObjectUtil() {
    }

    public static void formatSubobject(final int objectType, final int objectClass, final Boolean processingRule, final Boolean ignore,
        final ByteBuf body, final ByteBuf out) {
        out.writeByte(objectClass);
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(IGNORED, ignore);
        flags.set(PROCESSED, processingRule);
        final byte flagB = flags.toByte();
        final int typeByte = objectType << FLAGS_SIZE | flagB & 0xff;
        out.writeByte(typeByte);
        out.writeShort(body.writerIndex() + HEADER_SIZE);
        out.writeBytes(body);
    }
}
