/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A best-effort output limiter. It does not provide any fairness, and acts as a blocking gate-keeper
 * for a sessions' channel.
 */
@ThreadSafe
final class ChannelOutputLimiter extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelOutputLimiter.class);
    private final BGPSessionImpl session;
    private volatile boolean blocked;

    ChannelOutputLimiter(final BGPSessionImpl session) {
        this.session = Preconditions.checkNotNull(session);
    }

    private void ensureWritable() {
        if (this.blocked) {
            LOG.trace("Blocked slow path tripped on session {}", this.session);
            synchronized (this) {
                while (this.blocked) {
                    try {
                        LOG.debug("Waiting for session {} to become writable", this.session);
                        this.wait();
                    } catch (final InterruptedException e) {
                        throw new IllegalStateException("Interrupted while waiting for channel to come back", e);
                    }
                }

                LOG.debug("Resuming write on session {}", this.session);
            }
        }
    }

    void write(final Notification msg) {
        ensureWritable();
        this.session.write(msg);
    }

    ChannelFuture writeAndFlush(final Notification msg) {
        ensureWritable();
        return this.session.writeAndFlush(msg);
    }

    void flush() {
        this.session.flush();
    }

    @Override
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
        final boolean w = ctx.channel().isWritable();

        synchronized (this) {
            this.blocked = !w;
            LOG.debug("Writes on session {} {}", this.session, w ? "unblocked" : "blocked");

            if (w) {
                this.notifyAll();
            } else {
                flush();
            }
        }

        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        synchronized (this) {
            this.blocked = false;
            this.notifyAll();
        }

        super.channelInactive(ctx);
    }
}
