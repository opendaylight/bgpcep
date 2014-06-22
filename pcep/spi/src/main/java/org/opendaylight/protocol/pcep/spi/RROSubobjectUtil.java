/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;

public final class RROSubobjectUtil {

    private static final int HEADER_SIZE = 2;

    private RROSubobjectUtil() {
    }

    public static void formatSubobject(final int type, final ByteBuf body, final ByteBuf buffer) {
        buffer.writeByte(type);
        buffer.writeByte(body.writerIndex() + HEADER_SIZE);
        buffer.writeBytes(body);
    }
}
