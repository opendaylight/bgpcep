/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.protocol;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.rib.impl.spi.ChannelPipelineInitializer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BGPReconnectPromise<S extends BGPSession> extends DefaultPromise<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPReconnectPromise.class);

    private final InetSocketAddress address;
    private final ReconnectStrategyFactory strategyFactory;
    private final Bootstrap bootstrap;
    private final ChannelPipelineInitializer initializer;
    private final EventExecutor executor;
    private Future<S> pending;

    public BGPReconnectPromise(final EventExecutor executor, final InetSocketAddress address,
                               final ReconnectStrategyFactory connectStrategyFactory, final Bootstrap bootstrap,
                               final ChannelPipelineInitializer initializer) {
        super(executor);
        this.executor = executor;
        this.bootstrap = bootstrap;
        this.initializer = Preconditions.checkNotNull(initializer);
        this.address = Preconditions.checkNotNull(address);
        this.strategyFactory = Preconditions.checkNotNull(connectStrategyFactory);
    }

    public synchronized void connect() {
        final ReconnectStrategy reconnectStrategy = this.strategyFactory.createReconnectStrategy();

        // Set up a client with pre-configured bootstrap, but add a closed channel handler into the pipeline to support reconnect attempts
        this.pending = connectSessionPromise(this.address, reconnectStrategy, this.bootstrap, new ChannelPipelineInitializer<S>() {
            @Override
            public void initializeChannel(final SocketChannel channel, final Promise<S> promise) {
                BGPReconnectPromise.this.initializer.initializeChannel(channel, promise);
                // add closed channel handler
                // This handler has to be added as last channel handler and the channel inactive event has to be caught by it
                // Handlers in front of it can react to channelInactive event, but have to forward the event or the reconnect will not work
                // This handler is last so all handlers in front of it can handle channel inactive (to e.g. resource cleanup) before a new connection is started
                channel.pipeline().addLast(new ClosedChannelHandler(BGPReconnectPromise.this));
            }
        });

        this.pending.addListener(new GenericFutureListener<Future<Object>>() {
            @Override
            public void operationComplete(final Future<Object> future) throws Exception {
                if (!future.isSuccess()) {
                    BGPReconnectPromise.this.setFailure(future.cause());
                }
            }
        });
    }

    public Future<S> connectSessionPromise(final InetSocketAddress address, final ReconnectStrategy strategy, final Bootstrap bootstrap,
                                  final ChannelPipelineInitializer initializer) {
        final BGPProtocolSessionPromise sessionPromise = new BGPProtocolSessionPromise(this.executor, address, strategy, bootstrap);
        final ChannelHandler chInit = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel channel) {
                initializer.initializeChannel(channel, sessionPromise);
            }
        };

        bootstrap.handler(chInit);
        sessionPromise.connect();
        LOG.debug("Client created.");
        return sessionPromise;
    }

    /**
     * @return true if initial connection was established successfully, false if initial connection failed due to e.g. Connection refused, Negotiation failed
     */
    private boolean isInitialConnectFinished() {
        Preconditions.checkNotNull(this.pending);
        return this.pending.isDone() && this.pending.isSuccess();
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            Preconditions.checkNotNull(this.pending);
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
        private final BGPReconnectPromise promise;

        public ClosedChannelHandler(final BGPReconnectPromise promise) {
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
            }

            LOG.debug("Reconnecting after connection to {} was dropped", this.promise.address);
            this.promise.connect();
        }
    }
}