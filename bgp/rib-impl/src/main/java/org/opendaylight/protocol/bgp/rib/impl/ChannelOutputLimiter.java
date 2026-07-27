/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.opendaylight.yangtools.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A best-effort output limiter. It does not provide any fairness, and acts as a blocking gate-keeper for a session's
 * channel.
 *
 * <p>This class is thread-safe.
 */
public final class ChannelOutputLimiter extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelOutputLimiter.class);
    private final BGPSessionImpl session;
    private volatile boolean active = true;

    ChannelOutputLimiter(final BGPSessionImpl session) {
        this.session = requireNonNull(session);
    }

    /**
     * Returns whether the channel write should be blocked. An inactive channel never becomes writable again,
     * so waiting for it would never end.
     *
     * @return {@code true} if the channel is active and cannot take more data
     */
    private boolean isWriteBlocked() {
        return active && !session.isWritable();
    }

    private void ensureWritable() {
        if (isWriteBlocked()) {
            LOG.trace("Blocked slow path tripped on session {}", session);
            synchronized (this) {
                while (isWriteBlocked()) {
                    try {
                        LOG.debug("Waiting for session {} to become writable", session);
                        flush();
                        this.wait();
                    } catch (final InterruptedException e) {
                        throw new IllegalStateException("Interrupted while waiting for channel to come back", e);
                    }
                }

                LOG.debug("Resuming write on session {}", session);
            }
        }
    }

    public void write(final Notification<?> msg) {
        ensureWritable();
        session.write(msg);
    }

    ChannelFuture writeAndFlush(final Notification<?> msg) {
        ensureWritable();
        return session.writeAndFlush(msg);
    }

    public void flush() {
        session.flush();
    }

    @Override
    @SuppressFBWarnings(value = "NN_NAKED_NOTIFY",
        justification = "Locking here stops this notification from landing in ensureWritable() between its "
            + "check and its wait().")
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
        final boolean w = ctx.channel().isWritable();
        LOG.debug("Writes on session {} {}", session, w ? "unblocked" : "blocked");

        if (w) {
            synchronized (this) {
                notifyAll();
            }
        }

        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        synchronized (this) {
            active = false;
            notifyAll();
        }

        super.channelInactive(ctx);
    }
}
