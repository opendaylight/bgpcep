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
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactoryDependencies;
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.PcepSessionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.PcepSessionTls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class DefaultPCEPSessionNegotiatorFactory extends AbstractPCEPSessionNegotiatorFactory {
    private final @NonNull PCEPTimerProposal timers;
    private final @NonNull List<PCEPCapability> capabilities;
    private final @NonNull Uint16 maxUnknownMessages;
    private final PcepSessionTls tlsConfiguration;

    public DefaultPCEPSessionNegotiatorFactory(final PcepSessionTimers timers, final List<PCEPCapability> capabilities,
            final Uint16 maxUnknownMessages) {
        this(timers, capabilities, maxUnknownMessages, null);
    }

    public DefaultPCEPSessionNegotiatorFactory(final PcepSessionTimers timers, final List<PCEPCapability> capabilities,
            final Uint16 maxUnknownMessages, final PcepSessionTls tlsConfiguration) {
        this(new PCEPTimerProposal(timers), capabilities, maxUnknownMessages, tlsConfiguration);
    }

    public DefaultPCEPSessionNegotiatorFactory(final PCEPTimerProposal timers, final List<PCEPCapability> capabilities,
            final Uint16 maxUnknownMessages, final PcepSessionTls tlsConfiguration) {
        this.timers = requireNonNull(timers);
        this.capabilities = requireNonNull(capabilities);
        this.maxUnknownMessages = requireNonNull(maxUnknownMessages);
        this.tlsConfiguration = tlsConfiguration;
    }

    @Override
    protected AbstractPCEPSessionNegotiator createNegotiator(
            final PCEPSessionNegotiatorFactoryDependencies sessionNegotiatorDependencies,
            final Promise<PCEPSession> promise,
            final Channel channel,
            final Uint8 sessionId) {

        final Open proposal = Util.createOpenObject((InetSocketAddress) channel.remoteAddress(), sessionId, timers,
            capabilities, sessionNegotiatorDependencies.getPeerProposal());

        return new DefaultPCEPSessionNegotiator(
                promise,
                channel,
                sessionNegotiatorDependencies.getListenerFactory().getSessionListener(),
                sessionId,
                proposal,
                maxUnknownMessages,
                tlsConfiguration);
    }
}
