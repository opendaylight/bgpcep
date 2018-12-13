/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;

public final class CapabilityUtil {
    private CapabilityUtil() {
    }

    /**
     * Adds header to capability value.
     *
     * @param code type of the capability
     * @param value capability value
     * @param buffer ByteBuf where the capability will be copied with its header
     */
    public static void formatCapability(final int code, final ByteBuf value, final ByteBuf buffer) {
        buffer.writeByte(code);
        buffer.writeByte(value.writerIndex());
        buffer.writeBytes(value);
    }
}
