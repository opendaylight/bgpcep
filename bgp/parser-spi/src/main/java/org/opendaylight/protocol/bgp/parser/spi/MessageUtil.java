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
import java.util.Arrays;
import org.opendaylight.protocol.util.ByteArray;

public final class MessageUtil {

    public static final int LENGTH_FIELD_LENGTH = 2;
    public static final int MARKER_LENGTH = 16;
    public static final int TYPE_FIELD_LENGTH = 1;
    public static final int COMMON_HEADER_LENGTH = LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + MARKER_LENGTH;

    private MessageUtil() {

    }

    /**
     * Serializes this BGP Message header to byte array.
     *
     * @param type message type to be formatted
     * @param body message body
     *
     * @return byte array representation of this header
     */
    public static ByteBuf formatMessage(final int type, final ByteBuf body) {
        final byte[] retBytes = new byte[MARKER_LENGTH];
        Arrays.fill(retBytes, 0, MARKER_LENGTH, UnsignedBytes.MAX_VALUE);

        ByteBuf retByteBuf = Unpooled.buffer(COMMON_HEADER_LENGTH + body.writerIndex());
        retByteBuf.writeBytes(retBytes);
        retByteBuf.writeBytes(ByteArray.intToBytes(body.writerIndex() + COMMON_HEADER_LENGTH, LENGTH_FIELD_LENGTH));
        retByteBuf.writeByte(UnsignedBytes.checkedCast(type));
        retByteBuf.writeBytes(body);
        return retByteBuf;
    }
}
