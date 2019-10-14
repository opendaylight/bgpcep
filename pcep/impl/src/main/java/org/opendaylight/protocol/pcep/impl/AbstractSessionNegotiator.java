/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.pcep.SessionNegotiator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSessionNegotiator extends ChannelInboundHandlerAdapter implements SessionNegotiator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSessionNegotiator.class);
    protected final Channel channel;
    protected final Promise<PCEPSessionImpl> promise;

    protected AbstractSessionNegotiator(final Promise<PCEPSessionImpl> promise, final Channel channel) {
        this.promise = requireNonNull(promise);
        this.channel = requireNonNull(channel);
    }

    protected final void negotiationSuccessful(final PCEPSessionImpl session) {
        LOG.debug("Negotiation on channel {} successful with session {}", this.channel, session);
        this.channel.pipeline().replace(this, "session", session);
        this.promise.setSuccess(session);
    }

    protected void negotiationFailed(final Throwable cause) {
        LOG.debug("Negotiation on channel {} failed", this.channel, cause);
        this.channel.close();
        this.promise.setFailure(cause);
    }

    protected final void sendMessage(final Message msg) {
        this.channel.writeAndFlush(msg).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                LOG.info("Failed to send message {}", msg, f.cause());
                negotiationFailed(f.cause());
            } else {
                LOG.trace("Message {} sent to socket", msg);
            }

        });
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public final void channelActive(final ChannelHandlerContext ctx) {
        LOG.debug("Starting session negotiation on channel {}", this.channel);

        try {
            this.startNegotiation();
        } catch (final Exception e) {
            LOG.warn("Unexpected negotiation failure", e);
            this.negotiationFailed(e);
        }

    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public final void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        LOG.debug("Negotiation read invoked on channel {}", this.channel);

        try {
            this.handleMessage((Message) msg);
        } catch (Exception e) {
            LOG.debug("Unexpected error while handling negotiation message {}", msg, e);
            this.negotiationFailed(e);
        }

    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LOG.info("Unexpected error during negotiation", cause);
        this.negotiationFailed(cause);
    }

    protected abstract void startNegotiation() throws ExecutionException;

    protected abstract void handleMessage(Message msg);
}
