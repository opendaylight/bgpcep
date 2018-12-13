/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;

public final class AttributeUtil {

    private static final int MAX_ATTR_LENGTH_FOR_SINGLE_BYTE = 255;

    public static final int OPTIONAL = 128;
    public static final int TRANSITIVE = 64;
    public static final int PARTIAL = 32;
    private static final int EXTENDED = 16;

    private AttributeUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds header to attribute value. If the length of the attribute value exceeds one-byte length field,
     * set EXTENDED bit and write length as 2B field.
     *
     * @param flags attribute flags
     * @param type of the attribute
     * @param value attribute value
     * @param buffer ByteBuf where the attribute will be copied with its header
     */
    public static void formatAttribute(final int flags, final int type, final ByteBuf value, final ByteBuf buffer) {
        final int length = value.writerIndex();
        final boolean extended = length > MAX_ATTR_LENGTH_FOR_SINGLE_BYTE ? true : false;
        buffer.writeByte(extended ? flags | EXTENDED : flags);
        buffer.writeByte(type);
        if (extended) {
            buffer.writeShort(length);
        } else {
            buffer.writeByte(length);
        }
        buffer.writeBytes(value);
    }
}
