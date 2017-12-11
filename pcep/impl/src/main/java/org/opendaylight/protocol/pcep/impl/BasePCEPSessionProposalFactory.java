/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.util.List;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BasePCEPSessionProposalFactory implements PCEPSessionProposalFactory {
    private static final Logger LOG = LoggerFactory.getLogger(BasePCEPSessionProposalFactory.class);
    private static final int KA_TO_DEADTIMER_RATIO = 4;

    private final int keepAlive, deadTimer;
    private final List<PCEPCapability> capabilities;

    public BasePCEPSessionProposalFactory(final int deadTimer, final int keepAlive, final List<PCEPCapability> capabilities) {
        if(keepAlive != 0) {
            Preconditions.checkArgument(keepAlive >= 1, "Minimum value for keep-alive-timer-value is 1");
            if(deadTimer != 0 && (deadTimer / keepAlive != KA_TO_DEADTIMER_RATIO)) {
                LOG.warn("dead-timer-value should be {} times greater than keep-alive-timer-value", KA_TO_DEADTIMER_RATIO);
            }
        }

        this.deadTimer = deadTimer;
        this.keepAlive = keepAlive;
        this.capabilities = requireNonNull(capabilities);
    }

    private void addTlvs(final InetSocketAddress address, final TlvsBuilder builder) {
        for (final PCEPCapability capability : this.capabilities) {
            capability.setCapabilityProposal(address, builder);
        }
    }

    @Override
    public Open getSessionProposal(final InetSocketAddress address, final int sessionId,
            final PCEPPeerProposal peerProposal) {
        final OpenBuilder oBuilder = new OpenBuilder();
        oBuilder.setSessionId((short) sessionId);
        oBuilder.setKeepalive((short) BasePCEPSessionProposalFactory.this.keepAlive);
        if(BasePCEPSessionProposalFactory.this.keepAlive == 0) {
            oBuilder.setDeadTimer((short) 0);
        } else {
            oBuilder.setDeadTimer((short) BasePCEPSessionProposalFactory.this.deadTimer);
        }

        final TlvsBuilder builder = new TlvsBuilder();
        addTlvs(address, builder);

        if (peerProposal != null) {
            peerProposal.setPeerSpecificProposal(address, builder);
        }
        return oBuilder.setTlvs(builder.build()).build();
    }

    @Override
    public List<PCEPCapability> getCapabilities() {
        return this.capabilities;
    }
}
