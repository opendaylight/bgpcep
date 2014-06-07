/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import com.google.common.primitives.UnsignedBytes;

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
    public static byte[] formatMessage(final int type, final byte[] body) {
        final byte[] retBytes = new byte[COMMON_HEADER_LENGTH + body.length];

        Arrays.fill(retBytes, 0, MARKER_LENGTH, UnsignedBytes.MAX_VALUE);
        System.arraycopy(ByteArray.intToBytes(body.length + COMMON_HEADER_LENGTH, LENGTH_FIELD_LENGTH), 0, retBytes, MARKER_LENGTH,
                LENGTH_FIELD_LENGTH);

        retBytes[MARKER_LENGTH + LENGTH_FIELD_LENGTH] = UnsignedBytes.checkedCast(type);
        ByteArray.copyWhole(body, retBytes, COMMON_HEADER_LENGTH);

        return retBytes;
    }
}
