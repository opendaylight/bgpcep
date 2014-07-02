/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.bgp.parser.AttributeFlags;

public class AttributeUtil {

    private static final int MAX_ATTR_LENGTH_FOR_SINGLE_BYTE = 255;

    private AttributeUtil() {

    }

    /**
     * Adds header to attribute value. If the length of the attribute value exceeds one-byte length field,
     * set EXTENDED bit and write length as 2B field.
     *
     * @param type of the attribute
     * @param value attribute value
     * @param buffer ByteBuf where the attribute will be copied with its header
     */
    public static void formatAttribute(final int flags, final int type, final ByteBuf value, final ByteBuf buffer) {
        final int length = value.writerIndex();
        final boolean extended = (length > MAX_ATTR_LENGTH_FOR_SINGLE_BYTE) ? true : false;
        buffer.writeByte((extended) ? (flags | AttributeFlags.EXTENDED) : flags);
        buffer.writeByte(type);
        if (extended) {
            buffer.writeShort(length);
        } else {
            buffer.writeByte(length);
        }
        buffer.writeBytes(value);
    }
}
