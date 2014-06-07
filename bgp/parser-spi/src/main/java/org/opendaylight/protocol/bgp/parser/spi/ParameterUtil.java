/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import com.google.common.primitives.UnsignedBytes;

public final class ParameterUtil {

    private static final int HEADER_SIZE = 2;

    private ParameterUtil() {

    }

    public static byte[] formatParameter(final int type, final byte[] value) {
        final byte[] bytes = new byte[HEADER_SIZE + value.length];
        bytes[0] = UnsignedBytes.checkedCast(type);
        bytes[1] = UnsignedBytes.checkedCast(value.length);
        System.arraycopy(value, 0, bytes, HEADER_SIZE, value.length);
        return bytes;
    }
}
