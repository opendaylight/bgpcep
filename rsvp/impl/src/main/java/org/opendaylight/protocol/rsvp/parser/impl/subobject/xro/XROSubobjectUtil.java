/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.xro;

import io.netty.buffer.ByteBuf;

public final class XROSubobjectUtil {

    private static final int HEADER_SIZE = 2;

    private static final int MANDATORY_BIT = 7;

    private XROSubobjectUtil() {
        throw new UnsupportedOperationException();
    }

    public static void formatSubobject(final int type, final Boolean mandatory, final ByteBuf body, final ByteBuf buffer) {
        if (mandatory == null) {
            buffer.writeByte(type);
        } else {
            buffer.writeByte(type | (mandatory ? 1 << MANDATORY_BIT : 0));
        }
        buffer.writeByte(body.writerIndex() + HEADER_SIZE);
        buffer.writeBytes(body);
    }
}
