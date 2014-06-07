/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import com.google.common.primitives.UnsignedBytes;

import org.opendaylight.protocol.util.ByteArray;

public final class CapabilityUtil {

    private static final int HEADER_SIZE = 2;

    private CapabilityUtil() {

    }

    public static byte[] formatCapability(final int code, final byte[] value) {
        final byte[] ret = new byte[HEADER_SIZE + value.length];
        ret[0] = UnsignedBytes.checkedCast(code);
        ret[1] = UnsignedBytes.checkedCast(value.length);
        ByteArray.copyWhole(value, ret, HEADER_SIZE);
        return ret;
    }
}
