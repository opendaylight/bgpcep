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
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.PcepSessionTimers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.PcepSessionTls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

public class DefaultPCEPSessionNegotiatorFactory extends AbstractPCEPSessionNegotiatorFactory {
    private final @NonNull PCEPSessionListenerFactory listenerFactory;
    private final @NonNull PCEPTimerProposal timers;
    private final @NonNull List<PCEPCapability> capabilities;
    private final @NonNull Uint16 maxUnknownMessages;
    private final PcepSessionTls tlsConfiguration;

    public DefaultPCEPSessionNegotiatorFactory(final PCEPSessionListenerFactory listenerFactory,
            final PcepSessionTimers timers, final List<PCEPCapability> capabilities, final Uint16 maxUnknownMessages) {
        this(listenerFactory, timers, capabilities, maxUnknownMessages, null);
    }

    public DefaultPCEPSessionNegotiatorFactory(final PCEPSessionListenerFactory listenerFactory,
            final PcepSessionTimers timers, final List<PCEPCapability> capabilities, final Uint16 maxUnknownMessages,
            final PcepSessionTls tlsConfiguration) {
        this(listenerFactory, new PCEPTimerProposal(timers), capabilities, maxUnknownMessages, tlsConfiguration);
    }

    public DefaultPCEPSessionNegotiatorFactory(final PCEPSessionListenerFactory listenerFactory,
            final PCEPTimerProposal timers, final List<PCEPCapability> capabilities, final Uint16 maxUnknownMessages,
            final PcepSessionTls tlsConfiguration) {
        this.listenerFactory = requireNonNull(listenerFactory);
        this.timers = requireNonNull(timers);
        this.capabilities = requireNonNull(capabilities);
        this.maxUnknownMessages = requireNonNull(maxUnknownMessages);
        this.tlsConfiguration = tlsConfiguration;
    }

    @Override
    protected final AbstractPCEPSessionNegotiator createNegotiator(final Promise<PCEPSession> promise,
            final Channel channel, final Uint8 sessionId) {
        final var address = (InetSocketAddress) channel.remoteAddress();

        final var builder = new TlvsBuilder();
        for (final var capability : capabilities) {
            capability.setCapabilityProposal(address, builder);
        }

        appendPeerSpecificTls(address, builder);

        return new DefaultPCEPSessionNegotiator(promise, channel, listenerFactory.getSessionListener(), sessionId,
            new OpenBuilder()
                .setSessionId(sessionId)
                .setKeepalive(timers.keepAlive())
                .setDeadTimer(timers.deadTimer())
                .setTlvs(builder.build())
                .build(), maxUnknownMessages, tlsConfiguration);
    }

    protected void appendPeerSpecificTls(final @NonNull InetSocketAddress address, final @NonNull TlvsBuilder builder) {
        // No-op by default
    }
}
