/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.PcepSessionTimers;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A curated {@link #keepAlive()} and {@link #deadTimer()} timer proposal.
 */
public record PCEPTimerProposal(@NonNull Uint8 keepAlive, @NonNull Uint8 deadTimer) {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTimerProposal.class);
    private static final int KA_TO_DEADTIMER_RATIO = 4;

    public PCEPTimerProposal {
        requireNonNull(keepAlive);

        if (!Uint8.ZERO.equals(keepAlive)) {
            requireNonNull(deadTimer);
            if (!Uint8.ZERO.equals(deadTimer) && deadTimer.toJava() / keepAlive.toJava() != KA_TO_DEADTIMER_RATIO) {
                LOG.warn("dead-timer-value ({}) should be {} times greater than keep-alive-timer-value ({}}",
                    deadTimer, KA_TO_DEADTIMER_RATIO, keepAlive);
            }
        } else {
            deadTimer = Uint8.ZERO;
        }
    }

    public PCEPTimerProposal(final PcepSessionTimers timers) {
        this(timers.requireKeepAliveTimerValue(), timers.requireDeadTimerValue());
    }
}
