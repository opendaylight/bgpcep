/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;

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
     */
    public static void formatParameter(final int type, final ByteBuf value, final ByteBuf buffer) {
        buffer.writeByte(type);
        buffer.writeByte(value.writerIndex());
        buffer.writeBytes(value);
    }
}
