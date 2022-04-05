/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A best-effort output limiter. It does not provide any fairness, and acts as a blocking gate-keeper
 * for a sessions' channel.
 *
 * <p>
 * This class is thread-safe.
 */
public final class ChannelOutputLimiter extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelOutputLimiter.class);
    private final BGPSessionImpl session;
    private volatile boolean blocked;

    ChannelOutputLimiter(final BGPSessionImpl session) {
        this.session = requireNonNull(session);
    }

    private void ensureWritable() {
        if (blocked) {
            LOG.trace("Blocked slow path tripped on session {}", session);
            synchronized (this) {
                while (blocked) {
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
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
        final boolean w = ctx.channel().isWritable();

        synchronized (this) {
            blocked = !w;
            LOG.debug("Writes on session {} {}", session, w ? "unblocked" : "blocked");

            if (w) {
                notifyAll();
            }
        }

        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        synchronized (this) {
            blocked = false;
            notifyAll();
        }

        super.channelInactive(ctx);
    }
}
