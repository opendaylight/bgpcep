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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPCCDispatcher implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPCCDispatcher.class);
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventExecutor executor;

    protected AbstractPCCDispatcher(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this.bossGroup = Preconditions.checkNotNull(bossGroup);
        this.workerGroup = Preconditions.checkNotNull(workerGroup);
        this.executor = Preconditions.checkNotNull(GlobalEventExecutor.INSTANCE);
    }

    protected ChannelFuture createServer(final InetSocketAddress address, final ChannelPipelineInitializer initializer) {
        return this.createServer(address, NioServerSocketChannel.class, initializer);
    }

    private ChannelFuture createServer(final SocketAddress address, Class<? extends ServerChannel> channelClass,
                                       final AbstractPCCDispatcher.ChannelPipelineInitializer initializer) {
        final ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                initializer.initializeChannel(ch, new DefaultPromise(AbstractPCCDispatcher.this.executor));
            }
        });
        b.option(ChannelOption.SO_BACKLOG, 128);

        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        this.customizeBootstrap(b);
        if (b.group() == null) {
            b.group(this.bossGroup, this.workerGroup);
        }

        try {
            b.channel(channelClass);
        } catch (IllegalStateException e) {
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, e);
        }

        final ChannelFuture f = b.bind(address);
        LOG.debug("Initiated server {} at {}.", f, address);
        return f;
    }

    public void customizeBootstrap(final ServerBootstrap b) {
    }

    protected Future<PCEPSessionImpl> createClient(final InetSocketAddress address, final ReconnectStrategy strategy, final ChannelPipelineInitializer initializer) {
        final Bootstrap b = new Bootstrap();
        final PCCProtocolSessionPromise p = new PCCProtocolSessionPromise(this.executor, address, strategy, b);
        (b.option(ChannelOption.SO_KEEPALIVE, true)).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                initializer.initializeChannel(ch, p);
            }
        });
        this.customizeBootstrap(b);
        this.setWorkerGroup(b);
        this.setChannelFactory(b);
        p.connect();
        LOG.debug("Client created.");
        return p;
    }

    private void setWorkerGroup(final Bootstrap b) {
        if (b.group() == null) {
            b.group(this.workerGroup);
        }
    }

    protected Future<PCEPSessionImpl> createClient(final InetSocketAddress address, final ReconnectStrategy strategy,
                                                   final Bootstrap bootstrap, final ChannelPipelineInitializer initializer) {
        final PCCProtocolSessionPromise p = new PCCProtocolSessionPromise(this.executor, address, strategy, bootstrap);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                initializer.initializeChannel(ch, p);
            }
        });
        p.connect();
        LOG.debug("Client created.");
        return p;
    }

    protected void customizeBootstrap(final Bootstrap b) {
    }

    protected Future<Void> createReconnectingClient(final InetSocketAddress address,final  ReconnectStrategyFactory connectStrategyFactory, final ChannelPipelineInitializer initializer) {
        final Bootstrap b = new Bootstrap();
        final PCCPReconnectPromise p = new PCCPReconnectPromise(GlobalEventExecutor.INSTANCE, this, address, connectStrategyFactory, b, initializer);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        this.customizeBootstrap(b);
        this.setWorkerGroup(b);
        this.setChannelFactory(b);
        p.connect();
        return p;
    }

    private void setChannelFactory(final Bootstrap b) {
        try {
            b.channel(NioSocketChannel.class);
        } catch (IllegalStateException e) {
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, e);
        }

    }

    @Override
    public void close() {
    }

    protected interface ChannelPipelineInitializer {
        void initializeChannel(SocketChannel socketChannel, Promise<PCEPSessionImpl> promise);
    }
}

