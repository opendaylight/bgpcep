/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.primitives.UnsignedBytes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.SessionNegotiator;
import org.opendaylight.protocol.pcep.impl.PCEPPeerRegistry.SessionReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SessionNegotiator which takes care of making sure sessions between PCEP peers are kept unique. This needs to be
 * further subclassed to provide either a client or server factory.
 */
public abstract class AbstractPCEPSessionNegotiatorFactory implements PCEPSessionNegotiatorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPCEPSessionNegotiatorFactory.class);

    private final PCEPPeerRegistry sessionRegistry = new PCEPPeerRegistry();

    /**
     * Create a new negotiator. This method needs to be implemented by subclasses to actually provide a negotiator.
     *
     * @param promise         Session promise to be completed by the negotiator
     * @param channel         Associated channel
     * @param sessionId       Session ID assigned to the resulting session
     * @return a PCEP session negotiator
     */
    protected abstract AbstractPCEPSessionNegotiator createNegotiator(Promise<PCEPSession> promise, Channel channel,
        Uint8 sessionId);

    @Override
    public final SessionNegotiator getSessionNegotiator(final Channel channel, final Promise<PCEPSession> promise) {
        LOG.debug("Instantiating bootstrap negotiator for channel {}", channel);
        return new BootstrapSessionNegotiator(channel, promise);
    }

    private final class BootstrapSessionNegotiator extends AbstractSessionNegotiator {
        private static final Comparator<byte[]> COMPARATOR = UnsignedBytes.lexicographicalComparator();

        BootstrapSessionNegotiator(final Channel channel, final Promise<PCEPSession> promise) {
            super(promise, channel);
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        //similar to bgp/rib-impl/src/main/java/org/opendaylight/protocol/bgp/rib/impl/AbstractBGPSessionNegotiator.java
        protected void startNegotiation() throws ExecutionException {
            final Object lock = this;

            LOG.debug("Bootstrap negotiation for channel {} started", channel);

            /*
             * We have a chance to see if there's a client session already
             * registered for this client.
             */
            final byte[] clientAddress = ((InetSocketAddress) channel.remoteAddress()).getAddress().getAddress();

            synchronized (lock) {
                if (sessionRegistry.getSessionReference(clientAddress).isPresent()) {
                    final byte[] serverAddress =
                        ((InetSocketAddress) channel.localAddress()).getAddress().getAddress();
                    if (COMPARATOR.compare(serverAddress, clientAddress) > 0) {
                        sessionRegistry.removeSessionReference(clientAddress).ifPresent(sessionRef -> {
                            try {
                                sessionRef.close();
                            } catch (final Exception e) {
                                LOG.error("Unexpected failure to close old session", e);
                            }
                        });
                    } else {
                        negotiationFailed(new IllegalStateException("A conflicting session for address "
                                + ((InetSocketAddress) channel.remoteAddress()).getAddress() + " found."));
                        return;
                    }
                }

                final Uint8 sessionId = sessionRegistry.nextSession(clientAddress);
                final var negotiator = createNegotiator(promise, channel, sessionId);

                sessionRegistry.putSessionReference(clientAddress, new SessionReference() {
                    @Override
                    public void close() throws ExecutionException {
                        try {
                            sessionRegistry.releaseSession(clientAddress, sessionId);
                        } finally {
                            channel.close();
                        }
                    }

                    @Override
                    public Uint8 getSessionId() {
                        return sessionId;
                    }
                });

                channel.closeFuture().addListener((ChannelFutureListener) future -> {
                    synchronized (lock) {
                        sessionRegistry.removeSessionReference(clientAddress);
                    }
                });

                LOG.info("Replacing bootstrap negotiator for channel {}", channel);
                channel.pipeline().replace(this, "negotiator", negotiator);
                negotiator.startNegotiation();
            }
        }

        @Override
        protected void handleMessage(final Message msg) {
            throw new IllegalStateException("Bootstrap negotiator should have been replaced");
        }
    }
}
