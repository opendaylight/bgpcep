/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

/**
 * Utility class which is intended for formatting parameter.
 */
public final class ParameterUtil {
    private ParameterUtil() {
    }

    /**
     * Adds header to parameter value.
     *
     * @param type of the parameter
     * @param value parameter value
     * @param buffer ByteBuf where the parameter will be copied with its header
     * @throws IllegalArgumentException if value length exceeds 255 bytes
     */
    public static void formatParameter(final int type, final ByteBuf value, final ByteBuf buffer) {
        final int valueLength = value.writerIndex();
        if (valueLength > 255) {
            throw new IllegalArgumentException(String.format(
                "Cannot encode parameter %s because value length %s exceeds parameter length field size (value %s)",
                type, valueLength, ByteBufUtil.hexDump(value)));
        }

        buffer.writeByte(type);
        buffer.writeByte(valueLength);
        buffer.writeBytes(value);
    }
}
