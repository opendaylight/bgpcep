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

public final class CapabilityUtil {

    private static final int HEADER_SIZE = 2;

    private CapabilityUtil() {

    }

    public static ByteBuf formatCapability(final int code, final ByteBuf value) {
        final ByteBuf ret = Unpooled.buffer(HEADER_SIZE + value.writerIndex());
        ret.writeByte(UnsignedBytes.checkedCast(code));
        ret.writeByte(UnsignedBytes.checkedCast(value.writerIndex()));
        ret.writeBytes(value);
        return ret;
    }
}
