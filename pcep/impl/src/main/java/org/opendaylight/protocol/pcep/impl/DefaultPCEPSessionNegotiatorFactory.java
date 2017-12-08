/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.app.config.rev160707.PcepDispatcherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.app.config.rev160707.pcep.dispatcher.config.Tls;

public final class DefaultPCEPSessionNegotiatorFactory extends AbstractPCEPSessionNegotiatorFactory {
    private final PCEPSessionProposalFactory spf;
    private final int maxUnknownMessages;
    private final Tls tlsConfiguration;

    public DefaultPCEPSessionNegotiatorFactory(final PCEPSessionProposalFactory spf, final int maxUnknownMessages) {
        this(spf, maxUnknownMessages, null);
    }

    private DefaultPCEPSessionNegotiatorFactory(final PCEPSessionProposalFactory spf, final int maxUnknownMessages,
            final Tls tlsConfiguration) {
        this.spf = requireNonNull(spf);
        this.maxUnknownMessages = maxUnknownMessages;
        this.tlsConfiguration = tlsConfiguration;
    }

    public DefaultPCEPSessionNegotiatorFactory(final PCEPSessionProposalFactory spf, final PcepDispatcherConfig config) {
        this(spf, config.getMaxUnknownMessages(), config.getTls());
    }

    @Override
    protected AbstractPCEPSessionNegotiator createNegotiator(final Promise<PCEPSessionImpl> promise,
            final PCEPSessionListener listener, final Channel channel, final short sessionId,
            final PCEPPeerProposal peerProposal) {
        return new DefaultPCEPSessionNegotiator(promise, channel, listener, sessionId, this.maxUnknownMessages,
                this.spf.getSessionProposal((InetSocketAddress) channel.remoteAddress(), sessionId, peerProposal),
                this.tlsConfiguration);
    }

    @Override
    public PCEPSessionProposalFactory getPCEPSessionProposalFactory() {
        return this.spf;
    }
}
