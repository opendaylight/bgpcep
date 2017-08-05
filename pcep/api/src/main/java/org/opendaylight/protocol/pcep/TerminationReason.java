/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import static com.google.common.base.Verify.verify;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum TerminationReason {
    UNKNOWN(1),
    EXP_DEADTIMER(2),
    MALFORMED_MSG(3),
    TOO_MANY_UNKNWN_REQS(4),
    TOO_MANY_UNKNOWN_MSGS(5);

    private static final TerminationReason[] REASONS;

    static {
        // We are not making many assumptions here
        final TerminationReason[] reasons = TerminationReason.values();
        verify(reasons.length > 0);

        final short highest = Arrays.stream(reasons).map(TerminationReason::getShortValue)
            .collect(Collectors.maxBy(Short::compareTo)).get().shortValue();
        final TerminationReason[] init = new TerminationReason[highest + 1];
        for (TerminationReason reason : reasons) {
            init[reason.getShortValue()] = reason;
        }

        REASONS = init;
    }

    private short value;

    private TerminationReason(final int value) {
        this.value = (short) value;
    }

    /**
     * Gets value of termination reason.
     *
     * @return short value
     */
    public short getShortValue() {
        return value;
    }

    /**
     * Gets termination reason for specific short value.
     *
     * @param valueArg
     * @return corresponding TerminationReason item
     */
    public static TerminationReason forValue(final short valueArg) {
        return valueArg < 0 || valueArg >= REASONS.length ? null : REASONS[valueArg];
    }
}
