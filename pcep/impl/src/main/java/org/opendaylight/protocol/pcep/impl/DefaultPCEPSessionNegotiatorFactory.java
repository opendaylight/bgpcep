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
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactoryDependencies;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.app.config.rev160707.PcepDispatcherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.PcepSessionErrorPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.PcepSessionTls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class DefaultPCEPSessionNegotiatorFactory extends AbstractPCEPSessionNegotiatorFactory {
    private final PcepSessionErrorPolicy errorPolicy;
    private final PCEPSessionProposalFactory spf;
    private final PcepSessionTls tlsConfiguration;

    public DefaultPCEPSessionNegotiatorFactory(final PCEPSessionProposalFactory spf,
            final PcepSessionErrorPolicy errorPolicy) {
        this(spf, errorPolicy, null);
    }

    private DefaultPCEPSessionNegotiatorFactory(final PCEPSessionProposalFactory spf,
            final PcepSessionErrorPolicy errorPolicy, final PcepSessionTls tlsConfiguration) {
        this.spf = requireNonNull(spf);
        this.errorPolicy = requireNonNull(errorPolicy);
        this.tlsConfiguration = tlsConfiguration;
    }

    public DefaultPCEPSessionNegotiatorFactory(final PCEPSessionProposalFactory spf,
            final PcepDispatcherConfig config) {
        this(spf, config, config.getTls());
    }

    @Override
    protected AbstractPCEPSessionNegotiator createNegotiator(
            final PCEPSessionNegotiatorFactoryDependencies sessionNegotiatorDependencies,
            final Promise<PCEPSessionImpl> promise,
            final Channel channel,
            final Uint8 sessionId) {

        final Open proposal = spf.getSessionProposal((InetSocketAddress) channel.remoteAddress(), sessionId,
                sessionNegotiatorDependencies.getPeerProposal());
        return new DefaultPCEPSessionNegotiator(
                promise,
                channel,
                sessionNegotiatorDependencies.getListenerFactory().getSessionListener(),
                sessionId,
                proposal,
                errorPolicy,
                tlsConfiguration);
    }

    @Override
    public PCEPSessionProposalFactory getPCEPSessionProposalFactory() {
        return spf;
    }
}
