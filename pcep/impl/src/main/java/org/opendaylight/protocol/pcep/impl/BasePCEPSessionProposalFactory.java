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
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.PcepSessionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class BasePCEPSessionProposalFactory implements PCEPSessionProposalFactory {
    private final @NonNull List<PCEPCapability> capabilities;
    private final @NonNull PCEPTimerProposal timers;

    public BasePCEPSessionProposalFactory(final PCEPTimerProposal timers, final List<PCEPCapability> capabilities) {
        this.timers = requireNonNull(timers);
        this.capabilities = requireNonNull(capabilities);
    }

    public BasePCEPSessionProposalFactory(final PcepSessionTimers timers, final List<PCEPCapability> capabilities) {
        this(new PCEPTimerProposal(timers), capabilities);
    }

    public BasePCEPSessionProposalFactory(final Uint8 deadTimer, final Uint8 keepAlive,
            final List<PCEPCapability> capabilities) {
        this(new PCEPTimerProposal(keepAlive, deadTimer), capabilities);
    }

    @Override
    public Open getSessionProposal(final InetSocketAddress address, final Uint8 sessionId,
            final PCEPPeerProposal peerProposal) {
        return Util.createOpenObject(address, sessionId, timers, capabilities, peerProposal);
    }

    @Override
    public List<PCEPCapability> getCapabilities() {
        return capabilities;
    }
}
