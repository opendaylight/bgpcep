/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public final class MessageUtil {

    @VisibleForTesting
    public static final int MARKER_LENGTH = 16;
    @VisibleForTesting
    public static final int COMMON_HEADER_LENGTH = 19;
    private static final byte[] MARKER = new byte[MARKER_LENGTH];

    static {
        Arrays.fill(MARKER, 0, MARKER_LENGTH, UnsignedBytes.MAX_VALUE);
    }

    private MessageUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds header to message value.
     *
     * @param type of the message
     * @param body message body
     * @param buffer ByteBuf where the message will be copied with its header
     */
    public static void formatMessage(final int type, final ByteBuf body, final ByteBuf buffer) {
        buffer.writeBytes(MARKER);
        buffer.writeShort(body.writerIndex() + COMMON_HEADER_LENGTH);
        buffer.writeByte(type);
        buffer.writeBytes(body);
    }
}
