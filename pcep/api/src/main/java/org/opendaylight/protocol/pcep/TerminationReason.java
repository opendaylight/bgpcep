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
import org.opendaylight.yangtools.yang.common.Uint8;

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

        final short highest = Arrays.stream(reasons).map(TerminationReason::getUintValue).max(Uint8::compareTo).get()
                .toJava();
        final TerminationReason[] init = new TerminationReason[highest + 1];
        for (TerminationReason reason : reasons) {
            init[reason.getUintValue().toJava()] = reason;
        }

        REASONS = init;
    }

    private final Uint8 value;

    TerminationReason(final int value) {
        this.value = Uint8.valueOf(value);
    }

    /**
     * Gets value of termination reason.
     *
     * @return short value
     */
    public Uint8 getUintValue() {
        return value;
    }

    /**
     * Gets termination reason for specific short value.
     *
     * @param valueArg corresponding to Termination reason
     * @return corresponding TerminationReason item
     */
    public static TerminationReason forValue(final Uint8 valueArg) {
        final short value = valueArg.toJava();
        return value >= REASONS.length ? null : REASONS[value];
    }
}
