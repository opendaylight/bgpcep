/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;

/**
 * Utility class for ByteBuf's write methods.
 */
public final class ByteBufWriteUtil {
    private ByteBufWriteUtil() {
        // Hidden on purpose
    }

    /**
     * Writes Float32 <code>value</code> if not null, otherwise writes zeros to
     * the <code>output</code> ByteBuf. ByteBuf's writerIndex is increased by 4.
     *
     * @param value
     *            Float32 value to be written to the output.
     * @param output
     *            ByteBuf, where value or zeros are written.
     */
    public static void writeFloat32(final Float32 value, final ByteBuf output) {
        if (value != null) {
            output.writeBytes(value.getValue());
        } else {
            output.writeInt(0);
        }
    }
}
