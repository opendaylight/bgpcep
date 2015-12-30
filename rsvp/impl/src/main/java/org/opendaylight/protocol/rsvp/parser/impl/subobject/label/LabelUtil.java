/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.label;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.BitArray;

public final class LabelUtil {

    private static final int FLAGS_SIZE = 8;

    private static final int UNIDIRECTIONAL = 0;
    private static final int GLOBAL = 7;

    private LabelUtil() {
        throw new UnsupportedOperationException();
    }

    public static void formatLabel(final int type, final Boolean unidirectional, final Boolean global, final ByteBuf body, final ByteBuf buffer) {
        final BitArray reserved = new BitArray(FLAGS_SIZE);
        reserved.set(UNIDIRECTIONAL, unidirectional);
        reserved.set(GLOBAL, global);
        reserved.toByteBuf(buffer);
        buffer.writeByte(type);
        buffer.writeBytes(body);
    }
}