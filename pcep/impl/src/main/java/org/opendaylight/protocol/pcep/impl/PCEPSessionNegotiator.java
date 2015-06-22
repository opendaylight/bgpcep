/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPPeerRegistry.SessionReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPSessionNegotiator extends ChannelInboundHandlerAdapter implements ChannelInboundHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPSessionNegotiator.class);

    private static final Comparator<byte[]> COMPARATOR = UnsignedBytes.lexicographicalComparator();
    protected final Channel channel;
    private final PCEPSessionListenerFactory factory;
    private final AbstractPCEPSessionNegotiatorFactory negFactory;
    private final Promise<PCEPSessionImpl> promise;

    public PCEPSessionNegotiator(final Channel channel, final Promise<PCEPSessionImpl> promise, final PCEPSessionListenerFactory factory,
                                 final AbstractPCEPSessionNegotiatorFactory negFactory) {
        this.promise = (Promise) Preconditions.checkNotNull(promise);
        this.channel = (Channel) Preconditions.checkNotNull(channel);
        this.factory = factory;
        this.negFactory = negFactory;
    }

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
                final byte[] serverAddress = ((InetSocketAddress) channel.localAddress()).getAddress().getAddress();
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

            final Short sessionId = sessionReg.nextSession(clientAddress);
            final AbstractPCEPSessionNegotiator n = negFactory.createNegotiator(promise, factory.getSessionListener(), channel, sessionId);

            sessionReg.putSessionReference(clientAddress, new SessionReference() {
                @Override
                public void close() throws ExecutionException {
                    try {
                        sessionReg.releaseSession(clientAddress, sessionId);
                    } finally {
                        channel.close();
                    }
                }

                @Override
                public Short getSessionId() {
                    return sessionId;
                }
            });

            channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture future) {
                    synchronized (lock) {
                        sessionReg.removeSessionReference(clientAddress);
                    }
                }
            });

            LOG.info("Replacing bootstrap negotiator for channel {}", channel);
            channel.pipeline().replace(this, "negotiator", n);
            n.startNegotiation();
        }
    }

    protected void handleMessage(final Message msg) {
        throw new IllegalStateException("Bootstrap negotiator should have been replaced");
    }

    protected final void negotiationSuccessful(PCEPSessionImpl session) {
        LOG.debug("Negotiation on channel {} successful with session {}", channel, session);
        channel.pipeline().replace(this, "session", session);
        promise.setSuccess(session);
    }

    protected void negotiationFailed(Throwable cause) {
        LOG.debug("Negotiation on channel {} failed", channel, cause);
        channel.close();
        promise.setFailure(cause);
    }

    protected final void sendMessage(final Message msg) {
        channel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture f) {
                if (!f.isSuccess()) {
                    LOG.info("Failed to send message {}", msg, f.cause());
                    negotiationFailed(f.cause());
                } else {
                    LOG.trace("Message {} sent to socket", msg);
                }

            }
        });
    }

    public final void channelActive(ChannelHandlerContext ctx) {
        LOG.debug("Starting session negotiation on channel {}", channel);

        try {
            startNegotiation();
        } catch (Exception var3) {
            LOG.warn("Unexpected negotiation failure", var3);
            negotiationFailed(var3);
        }

    }

    public final void channelRead(ChannelHandlerContext ctx, Object msg) {
        LOG.debug("Negotiation read invoked on channel {}", channel);

        try {
            handleMessage((Message) msg);
        } catch (Exception var4) {
            LOG.debug("Unexpected error while handling negotiation message {}", msg, var4);
            negotiationFailed(var4);
        }

    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.info("Unexpected error during negotiation", cause);
        negotiationFailed(cause);
    }
}
