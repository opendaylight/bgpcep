/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

public enum TerminationReason {
    UNKNOWN((short) 1), EXP_DEADTIMER((short) 2), MALFORMED_MSG((short) 3), TOO_MANY_UNKNWN_REQS((short) 4), TOO_MANY_UNKNOWN_MSGS((short) 5);

    private final short value;

    TerminationReason(final short value) {
        this.value = value;
    }

    /**
     * Gets value of termination reason.
     *
     * @return short value
     */
    public short getShortValue() {
        return this.value;
    }

    /**
     * Gets termination reason for specific short value.
     *
     * @param valueArg
     * @return corresponding TerminationReason item
     */
    public static TerminationReason forValue(final short valueArg) {
        for (TerminationReason reason : values()) {
            if (reason.value == valueArg) {
                return reason;
            }
        }
        return null;
    }

    public static TerminationReason forValue(final Short valueArg) {
        if (valueArg == null) {
            return null;
        } else {
            return forValue(valueArg.shortValue());
        }
    }
}
