/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Values;

/**
 * Utility class which is intended for formatting parameter.
 */
public final class ParameterUtil {
    private ParameterUtil() {
    }

    /**
     * Adds header to parameter value in RFC4271 format.
     *
     * @param type of the parameter
     * @param value parameter value
     * @param buffer ByteBuf where the parameter will be copied with its header
     * @throws IllegalArgumentException if value length exceeds 255 bytes
     */
    public static void formatParameter(final int type, final ByteBuf value, final ByteBuf buffer)
            throws ParameterLengthOverflowException {
        final int valueLength = value.writerIndex();
        ParameterLengthOverflowException.throwIf(valueLength > Values.UNSIGNED_BYTE_MAX_VALUE,
            "Cannot encode %s-byte value", valueLength);

        buffer.writeByte(type);
        buffer.writeByte(valueLength);
        buffer.writeBytes(value);
    }

    /**
     * Adds header to parameter value in draft-ietf-idr-ext-opt-param-05 format.
     *
     * @param type of the parameter
     * @param value parameter value
     * @param buffer ByteBuf where the parameter will be copied with its header
     * @throws IllegalArgumentException if value length exceeds 65535 bytes
     */
    public static void formatExtendedParameter(final int type, final ByteBuf value, final ByteBuf buffer) {
        final int valueLength = value.writerIndex();
        checkArgument(valueLength < Values.UNSIGNED_SHORT_MAX_VALUE, "Cannot encode %s-byte value", valueLength);

        buffer.writeByte(type);
        buffer.writeShort(valueLength);
        buffer.writeBytes(value);
    }
}
