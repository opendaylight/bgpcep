/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi;

import io.netty.buffer.ByteBuf;

public final class EROSubobjectUtil {

    private static final int HEADER_SIZE = 2;

    private static final int LOOSE_BIT = 7;

    private EROSubobjectUtil() {
        throw new UnsupportedOperationException();
    }

    public static void formatSubobject(int type, final Boolean loose, final ByteBuf body, final ByteBuf buffer) {
        if (loose == null) {
            buffer.writeByte(type);
        } else {
            buffer.writeByte(type | (loose ? 1 << LOOSE_BIT : 0));
        }
        buffer.writeByte(body.writerIndex() + HEADER_SIZE);
        buffer.writeBytes(body);
    }
}
