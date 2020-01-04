/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

/**
 * Util class for storing various util values as constants.
 */
public final class Values {
    /**
     * Maximum unsigned Short value (65535).
     */
    public static final int UNSIGNED_SHORT_MAX_VALUE = 65535;

    /**
     * Maximum unsigned Byte value (255).
     */
    public static final int UNSIGNED_BYTE_MAX_VALUE = 255;

    /**
     * Maximum unsigned Integer value (2147483648).
     */
    public static final long UNSIGNED_INT_MAX_VALUE = (long) Integer.MAX_VALUE + 1;

    /**
     * Maximum unsigned Byte value in hex (0xFF).
     */
    public static final int BYTE_MAX_VALUE_BYTES = 0xFF;

    /**
     * In order to get the value in first bit, we need to shift the byte by 7.
     */
    public static final int FIRST_BIT_OFFSET = 7;

    private Values() {
        // Hidden on purpose
    }
}
