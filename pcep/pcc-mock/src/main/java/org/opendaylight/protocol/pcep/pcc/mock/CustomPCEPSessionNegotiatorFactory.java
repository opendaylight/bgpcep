/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import java.net.InetSocketAddress;
import java.util.List;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.PcepSessionTls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

final class CustomPCEPSessionNegotiatorFactory extends DefaultPCEPSessionNegotiatorFactory {
    private final PCEPPeerProposal peerProposal;

    CustomPCEPSessionNegotiatorFactory(final PCEPSessionListenerFactory listenerFactory,
            final PCEPTimerProposal timers, final List<PCEPCapability> capabilities,
            final Uint16 maxUnknownMessages, final PcepSessionTls tlsConfiguration,
            final PCEPPeerProposal peerProposal) {
        super(listenerFactory, timers, capabilities, maxUnknownMessages, tlsConfiguration);
        this.peerProposal = peerProposal;
    }

    @Override
    protected void appendPeerSpecificTls(final InetSocketAddress address, final TlvsBuilder builder) {
        if (peerProposal != null) {
            peerProposal.setPeerSpecificProposal(address, builder);
        }
    }
}