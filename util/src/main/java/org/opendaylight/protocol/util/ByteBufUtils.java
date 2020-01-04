/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.uint24.rev200104.Uint24;
import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Utility methods for interacting with {@link ByteBuf}s. These add a number of methods for reading and writing various
 * data types from/to ByteBufs.
 */
public final class ByteBufUtils {
    private ByteBufUtils() {

    }

    public static @NonNull Uint24 readUint24(final ByteBuf buf) {
        return new Uint24(Uint32.fromIntBits(buf.readMedium()));
    }
}
