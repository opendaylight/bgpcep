/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactoryDependencies;
import org.opendaylight.protocol.pcep.impl.PCEPPeerRegistry.SessionReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPSessionNegotiator extends AbstractSessionNegotiator {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPSessionNegotiator.class);

    private static final Comparator<byte[]> COMPARATOR = UnsignedBytes.lexicographicalComparator();
    private final AbstractPCEPSessionNegotiatorFactory negFactory;
    private final PCEPSessionNegotiatorFactoryDependencies nfd;

    public PCEPSessionNegotiator(final Channel channel, final Promise<PCEPSessionImpl> promise,
            final PCEPSessionNegotiatorFactoryDependencies dependencies,
            final AbstractPCEPSessionNegotiatorFactory negFactory) {
        super(promise, channel);
        nfd = dependencies;
        this.negFactory = negFactory;
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
        final PCEPPeerRegistry sessionReg = negFactory.getSessionRegistry();

        synchronized (lock) {
            if (sessionReg.getSessionReference(clientAddress).isPresent()) {
                final byte[] serverAddress =
                    ((InetSocketAddress) channel.localAddress()).getAddress().getAddress();
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
                            + ((InetSocketAddress) channel.remoteAddress()).getAddress() + " found."));
                    return;
                }
            }

            final Uint8 sessionId = sessionReg.nextSession(clientAddress);
            final AbstractPCEPSessionNegotiator n = negFactory.createNegotiator(nfd, promise, channel, sessionId);

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
                public Uint8 getSessionId() {
                    return sessionId;
                }
            });

            channel.closeFuture().addListener((ChannelFutureListener) future -> {
                synchronized (lock) {
                    sessionReg.removeSessionReference(clientAddress);
                }
            });

            LOG.info("Replacing bootstrap negotiator for channel {}", channel);
            channel.pipeline().replace(this, "negotiator", n);
            n.startNegotiation();
        }
    }

    @Override
    protected void handleMessage(final Message msg) {
        throw new IllegalStateException("Bootstrap negotiator should have been replaced");
    }
}
