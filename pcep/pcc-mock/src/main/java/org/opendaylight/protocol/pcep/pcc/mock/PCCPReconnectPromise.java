/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCCPReconnectPromise extends DefaultPromise<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(PCCPReconnectPromise.class);
    private final AbstractPCCDispatcher dispatcher;
    private final InetSocketAddress address;
    private final ReconnectStrategyFactory strategyFactory;
    private final Bootstrap b;
    private final AbstractPCCDispatcher.ChannelPipelineInitializer initializer;
    private Future<?> pending;

    public PCCPReconnectPromise(final EventExecutor executor, final AbstractPCCDispatcher dispatcher, final InetSocketAddress address, final ReconnectStrategyFactory connectStrategyFactory,
                                final Bootstrap b, final AbstractPCCDispatcher.ChannelPipelineInitializer initializer) {
        super(executor);
        this.b = b;
        this.initializer = Preconditions.checkNotNull(initializer);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.address = Preconditions.checkNotNull(address);
        this.strategyFactory = Preconditions.checkNotNull(connectStrategyFactory);
    }

    synchronized void connect() {
        ReconnectStrategy cs = this.strategyFactory.createReconnectStrategy();
        this.pending = this.dispatcher.createClient(this.address, cs, this.b, new AbstractPCCDispatcher.ChannelPipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel channel, final Promise<PCEPSessionImpl> promise) {
                PCCPReconnectPromise.this.initializer.initializeChannel(channel, promise);
                channel.pipeline().addLast(new ClosedChannelHandler(PCCPReconnectPromise.this));
            }
        });
        this.pending.addListener(new GenericFutureListener<Future<Object>>() {
            @Override
            public void operationComplete(final Future<Object> future) throws Exception {
                if (!future.isSuccess()) {
                    PCCPReconnectPromise.this.setFailure(future.cause());
                }

            }
        });
    }

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
        } else {
            return false;
        }
    }

    private static final class ClosedChannelHandler extends ChannelInboundHandlerAdapter {
        private final PCCPReconnectPromise promise;

        public ClosedChannelHandler(PCCPReconnectPromise promise) {
            this.promise = promise;
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            if (!this.promise.isCancelled()) {
                if (!this.promise.isInitialConnectFinished()) {
                    PCCPReconnectPromise.LOG.debug("Connection to {} was dropped during negotiation, reattempting", this.promise.address);
                }

                PCCPReconnectPromise.LOG.debug("Reconnecting after connection to {} was dropped", this.promise.address);
                this.promise.connect();
            }
        }
    }
}

