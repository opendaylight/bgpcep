/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedBytes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPPeerRegistry.SessionReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPSessionNegotiator extends AbstractSessionNegotiator {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPSessionNegotiator.class);

    private static final Comparator<byte[]> COMPARATOR = UnsignedBytes.lexicographicalComparator();

    private final PCEPSessionListenerFactory factory;

    private final AbstractPCEPSessionNegotiatorFactory negFactory;
    private final PCEPPeerProposal peerProposal;

    public PCEPSessionNegotiator(final Channel channel, final Promise<PCEPSessionImpl> promise, final PCEPSessionListenerFactory factory,
        final AbstractPCEPSessionNegotiatorFactory negFactory, final PCEPPeerProposal peerProposal) {
        super(promise, channel);
        this.factory = factory;
        this.negFactory = negFactory;
        this.peerProposal = peerProposal;
    }

    @Override
    protected void startNegotiation() throws ExecutionException {
        final Object lock = this;

        LOG.debug("Bootstrap negotiation for channel {} started", this.channel);

        /*
         * We have a chance to see if there's a client session already
         * registered for this client.
         */
        final byte[] clientAddress = ((InetSocketAddress) this.channel.remoteAddress()).getAddress().getAddress();
        final PCEPPeerRegistry sessionReg = this.negFactory.getSessionRegistry();

        synchronized (lock) {
            if (sessionReg.getSessionReference(clientAddress).isPresent()) {
                final byte[] serverAddress = ((InetSocketAddress) this.channel.localAddress()).getAddress().getAddress();
                if (COMPARATOR.compare(serverAddress, clientAddress) > 0) {
                    final Optional<SessionReference> sessionRefMaybe = sessionReg.removeSessionReference(clientAddress);
                    try {
                        if (sessionRefMaybe.isPresent()) {
                            sessionRefMaybe.get().close();
                        }
                    } catch (final Exception e) {
                        LOG.error("Unexpected failure to close old session", e);
                    }
                } else {
                    negotiationFailed(new IllegalStateException("A conflicting session for address "
                            + ((InetSocketAddress) this.channel.remoteAddress()).getAddress() + " found."));
                    return;
                }
            }

            final Short sessionId = sessionReg.nextSession(clientAddress);
            final AbstractPCEPSessionNegotiator n = this.negFactory.createNegotiator(this.promise, this.factory.getSessionListener(),
                    this.channel, sessionId, this.peerProposal);

            sessionReg.putSessionReference(clientAddress, new SessionReference() {
                @Override
                public void close() throws ExecutionException {
                    try {
                        sessionReg.releaseSession(clientAddress, sessionId);
                    } finally {
                        PCEPSessionNegotiator.this.channel.close();
                    }
                }

                @Override
                public Short getSessionId() {
                    return sessionId;
                }
            });

            this.channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture future) {
                    synchronized (lock) {
                        sessionReg.removeSessionReference(clientAddress);
                    }
                }
            });

            LOG.info("Replacing bootstrap negotiator for channel {}", this.channel);
            this.channel.pipeline().replace(this, "negotiator", n);
            n.startNegotiation();
        }
    }

    @Override
    protected void handleMessage(final Message msg) {
        throw new IllegalStateException("Bootstrap negotiator should have been replaced");
    }
}
