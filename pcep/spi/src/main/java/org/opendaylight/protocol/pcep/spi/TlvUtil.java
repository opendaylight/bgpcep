/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;

public final class TlvUtil {

    public static final int HEADER_SIZE = 4;

    public static final int PADDED_TO = 4;

    private TlvUtil() {
    }

    public static void formatTlv(final int type,final ByteBuf body, final ByteBuf out) {
        out.writeShort(type);
        out.writeShort(body.writerIndex());
        out.writeBytes(body);
        out.writeZero(getPadding(HEADER_SIZE + body.writerIndex(), PADDED_TO));
    }

    public static int getPadding(final int length, final int padding) {
        return (padding - length % padding) % padding;
    }
}
