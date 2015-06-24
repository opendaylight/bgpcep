/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.rib.protocol.ReconnectStrategy;
import org.opendaylight.protocol.bgp.rib.protocol.ReconnectStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 22.6.2015.
 */
public class BGPReconnectPromise  extends DefaultPromise<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPReconnectPromise.class);
    private final BGPAbstractDispatcher dispatcher;
    private final InetSocketAddress address;
    private final ReconnectStrategyFactory strategyFactory;
    private final Bootstrap b;
    private final BGPAbstractDispatcher.PipelineInitializer initializer;
    private Future<?> pending;

    public BGPReconnectPromise(EventExecutor executor, BGPAbstractDispatcher dispatcher, InetSocketAddress address, ReconnectStrategyFactory connectStrategyFactory, Bootstrap b, BGPAbstractDispatcher.PipelineInitializer initializer) {
        super(executor);
        this.b = b;
        this.initializer = (BGPAbstractDispatcher.PipelineInitializer) Preconditions.checkNotNull(initializer);
        this.dispatcher = (BGPAbstractDispatcher) Preconditions.checkNotNull(dispatcher);
        this.address = (InetSocketAddress) Preconditions.checkNotNull(address);
        this.strategyFactory = (ReconnectStrategyFactory) Preconditions.checkNotNull(connectStrategyFactory);
    }

    synchronized void connect() {
        ReconnectStrategy cs = this.strategyFactory.createReconnectStrategy();
        this.pending = this.dispatcher.createClient(this.address, cs, this.b, new BGPAbstractDispatcher.PipelineInitializer() {
            public void initializeChannel(SocketChannel channel, Promise<BGPSessionImpl> promise) {
                BGPReconnectPromise.this.initializer.initializeChannel(channel, promise);
                channel.pipeline().addLast(new ChannelHandler[]{new BGPReconnectPromise.ClosedChannelHandler(BGPReconnectPromise.this)});
            }
        });
        this.pending.addListener(new GenericFutureListener<Future<Object>>() {
            public void operationComplete(Future<Object> future) throws Exception {
                if (!future.isSuccess()) {
                    BGPReconnectPromise.this.setFailure(future.cause());
                }

            }
        });
    }

    private boolean isInitialConnectFinished() {
        Preconditions.checkNotNull(this.pending);
        return this.pending.isDone() && this.pending.isSuccess();
    }

    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            Preconditions.checkNotNull(this.pending);
            this.pending.cancel(mayInterruptIfRunning);
            return true;
        } else {
            return false;
        }
    }

    private static final class ClosedChannelHandler extends ChannelInboundHandlerAdapter {
        private final BGPReconnectPromise promise;

        public ClosedChannelHandler(BGPReconnectPromise promise) {
            this.promise = promise;
        }

        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (!this.promise.isCancelled()) {
                if (!this.promise.isInitialConnectFinished()) {
                    BGPReconnectPromise.LOG.debug("Connection to {} was dropped during negotiation, reattempting", this.promise.address);
                }

                BGPReconnectPromise.LOG.debug("Reconnecting after connection to {} was dropped", this.promise.address);
                this.promise.connect();
            }
        }
    }
}