/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.net.InetSocketAddress;
import java.util.List;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public final class BasePCEPSessionProposalFactory implements PCEPSessionProposalFactory {

    private final int keepAlive, deadTimer;
    private final List<PCEPCapability> capabilities;

    public BasePCEPSessionProposalFactory(final int deadTimer, final int keepAlive, final List<PCEPCapability> capabilities) {
        this.deadTimer = deadTimer;
        this.keepAlive = keepAlive;
        this.capabilities = capabilities;
    }

    private void addTlvs(final InetSocketAddress address, final TlvsBuilder builder) {
        for (final PCEPCapability capability : this.capabilities) {
            capability.setCapabilityProposal(address, builder);
        }
    }

    @Override
    public Open getSessionProposal(final InetSocketAddress address, final int sessionId) {
        return getSessionProposal(address, sessionId, null);
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

    public int getKeepAlive() {
        return this.keepAlive;
    }

    public int getDeadTimer() {
        return this.deadTimer;
    }

}
