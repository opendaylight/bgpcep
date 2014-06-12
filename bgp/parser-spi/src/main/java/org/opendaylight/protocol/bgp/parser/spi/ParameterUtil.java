/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class ParameterUtil {

    private static final int HEADER_SIZE = 2;

    private ParameterUtil() {

    }

    public static ByteBuf formatParameter(final int type, final ByteBuf value) {
        final ByteBuf bytes = Unpooled.buffer(HEADER_SIZE + value.writerIndex());
        bytes.writeByte(UnsignedBytes.checkedCast(type));
        bytes.writeByte(UnsignedBytes.checkedCast(value.writerIndex()));
        bytes.writeBytes(value);
        return bytes;
    }
}
