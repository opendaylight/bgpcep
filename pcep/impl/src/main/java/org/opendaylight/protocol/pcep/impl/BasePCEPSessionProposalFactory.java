/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;

import java.net.InetSocketAddress;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev220328.PcepSessionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BasePCEPSessionProposalFactory implements PCEPSessionProposalFactory {
    private static final Logger LOG = LoggerFactory.getLogger(BasePCEPSessionProposalFactory.class);
    private static final int KA_TO_DEADTIMER_RATIO = 4;

    private final @NonNull List<PCEPCapability> capabilities;
    private final @NonNull Uint8 keepAlive;
    private final @NonNull Uint8 deadTimer;

    public BasePCEPSessionProposalFactory(final PcepSessionTimers timers, final List<PCEPCapability> capabilities) {
        this(timers.getDeadTimerValue(), timers.getKeepAliveTimerValue(), capabilities);
    }

    public BasePCEPSessionProposalFactory(final Uint8 deadTimer, final Uint8 keepAlive,
            final List<PCEPCapability> capabilities) {
        this.keepAlive = requireNonNull(keepAlive);
        this.capabilities = requireNonNull(capabilities);

        if (!Uint8.ZERO.equals(keepAlive)) {
            this.deadTimer = requireNonNull(deadTimer);
            if (!Uint8.ZERO.equals(deadTimer) && deadTimer.toJava() / keepAlive.toJava() != KA_TO_DEADTIMER_RATIO) {
                LOG.warn("dead-timer-value ({}) should be {} times greater than keep-alive-timer-value ({}}",
                    deadTimer, KA_TO_DEADTIMER_RATIO, keepAlive);
            }
        } else {
            this.deadTimer = Uint8.ZERO;
        }
    }

    @Override
    public Open getSessionProposal(final InetSocketAddress address, final int sessionId,
            final PCEPPeerProposal peerProposal) {
        final var builder = new TlvsBuilder();
        for (final var capability : capabilities) {
            capability.setCapabilityProposal(address, builder);
        }

        if (peerProposal != null) {
            peerProposal.setPeerSpecificProposal(address, builder);
        }

        return new OpenBuilder()
            .setSessionId(Uint8.valueOf(sessionId))
            .setKeepalive(keepAlive)
            .setDeadTimer(deadTimer)
            .setTlvs(builder.build())
            .build();
    }

    @Override
    public List<PCEPCapability> getCapabilities() {
        return capabilities;
    }
}
