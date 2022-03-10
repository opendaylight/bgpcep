/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.protocol;

import static java.util.Objects.requireNonNull;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.ChannelPipelineInitializer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BGPReconnectPromise<S extends BGPSession> extends DefaultPromise<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPReconnectPromise.class);

    private final InetSocketAddress address;
    private final int retryTimer;
    private final Bootstrap bootstrap;
    private final BGPPeerRegistry peerRegistry;
    private final ChannelPipelineInitializer<S> initializer;
    private BGPProtocolSessionPromise<S> pending;

    public BGPReconnectPromise(final @NonNull EventExecutor executor, final @NonNull InetSocketAddress address,
            final int retryTimer, final @NonNull Bootstrap bootstrap, final @NonNull BGPPeerRegistry peerRegistry,
            final @NonNull ChannelPipelineInitializer<S> initializer) {
        super(executor);
        this.bootstrap = bootstrap;
        this.initializer = requireNonNull(initializer);
        this.address = requireNonNull(address);
        this.retryTimer = retryTimer;
        this.peerRegistry = requireNonNull(peerRegistry);
    }

    public synchronized void connect() {
        if (this.pending != null) {
            this.pending.cancel(true);
        }

        // Set up a client with pre-configured bootstrap, but add a closed channel handler
        // into the pipeline to support reconnect attempts
        this.pending = connectSessionPromise(this.address, this.retryTimer, this.bootstrap, this.peerRegistry,
            (channel, promise) -> {
                this.initializer.initializeChannel(channel, promise);
                // add closed channel handler
                // This handler has to be added as last channel handler and the channel inactive event has to be
                // caught by it
                // Handlers in front of it can react to channelInactive event, but have to forward the event or
                // the reconnect will not work. This handler is last so all handlers in front of it can handle
                // channel inactive (to e.g. resource cleanup) before a new connection is started
                channel.pipeline().addLast(new ClosedChannelHandler(this));
            });

        this.pending.addListener(future -> {
            if (!future.isSuccess() && !this.isDone()) {
                this.setFailure(future.cause());
            }
        });
    }

    private static <S extends BGPSession> BGPProtocolSessionPromise<S> connectSessionPromise(
            final InetSocketAddress address, final int retryTimer, final Bootstrap bootstrap,
            final BGPPeerRegistry peerRegistry, final ChannelPipelineInitializer<S> initializer) {
        final BGPProtocolSessionPromise<S> sessionPromise = new BGPProtocolSessionPromise<>(address, retryTimer,
                bootstrap, peerRegistry);
        final ChannelHandler chInit = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel channel) {
                LOG.info("Initializing channel with {}", channel.remoteAddress());
                initializer.initializeChannel(channel, sessionPromise);
            }
        };

        bootstrap.handler(chInit);
        sessionPromise.connect();
        LOG.debug("Client created.");
        return sessionPromise;
    }

    /**
     * Indicate whether the initial connection was established successfully.
     *
     * @return true if initial connection was established successfully, false if initial connection failed due
     *         to e.g. Connection refused, Negotiation failed
     */
    private synchronized boolean isInitialConnectFinished() {
        requireNonNull(this.pending);
        return this.pending.isDone() && this.pending.isSuccess();
    }

    private synchronized void reconnect() {
        requireNonNull(this.pending);
        this.pending.reconnect();
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            requireNonNull(this.pending);
            this.pending.cancel(mayInterruptIfRunning);
            return true;
        }
        return false;
    }

    /**
     * Channel handler that responds to channelInactive event and reconnects the session.
     * Only if the promise was not canceled.
     */
    private static final class ClosedChannelHandler extends ChannelInboundHandlerAdapter {
        private final BGPReconnectPromise<?> promise;

        ClosedChannelHandler(final BGPReconnectPromise<?> promise) {
            this.promise = promise;
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            // This is the ultimate channel inactive handler, not forwarding
            if (this.promise.isCancelled()) {
                return;
            }

            if (!this.promise.isInitialConnectFinished()) {
                LOG.debug("Connection to {} was dropped during negotiation, reattempting", this.promise.address);
                this.promise.reconnect();
                return;
            }

            LOG.debug("Reconnecting after connection to {} was dropped", this.promise.address);
            this.promise.connect();
        }
    }
}
