/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalServerChannel;
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
import org.opendaylight.protocol.bgp.rib.protocol.ReconnectStrategy;
import org.opendaylight.protocol.bgp.rib.protocol.ReconnectStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 22.6.2015.
 */
public abstract class PCEPAbstractDispatcher implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPAbstractDispatcher.class);
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventExecutor executor;

    protected PCEPAbstractDispatcher(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this(GlobalEventExecutor.INSTANCE, bossGroup, workerGroup);
    }

    protected PCEPAbstractDispatcher(EventExecutor executor, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.bossGroup = (EventLoopGroup) Preconditions.checkNotNull(bossGroup);
        this.workerGroup = (EventLoopGroup) Preconditions.checkNotNull(workerGroup);
        this.executor = (EventExecutor) Preconditions.checkNotNull(executor);
    }

    protected ChannelFuture createServer(InetSocketAddress address, PCEPAbstractDispatcher.PipelineInitializer initializer) {
        return this.createServer(address, NioServerSocketChannel.class, initializer);
    }

    protected <CH extends Channel> ChannelFuture createServer(SocketAddress address, Class<? extends ServerChannel> channelClass, final PCEPAbstractDispatcher.ChannelPipelineInitializer<CH> initializer) {
        ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<CH>() {
            protected void initChannel(CH ch) {
                initializer.initializeChannel(ch, new DefaultPromise(PCEPAbstractDispatcher.this.executor));
            }
        });
        b.option(ChannelOption.SO_BACKLOG, Integer.valueOf(128));
        if (!LocalServerChannel.class.equals(channelClass)) {
            b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.valueOf(true));
            b.childOption(ChannelOption.TCP_NODELAY, Boolean.valueOf(true));
        }

        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        this.customizeBootstrap(b);
        if (b.group() == null) {
            b.group(this.bossGroup, this.workerGroup);
        }

        try {
            b.channel(channelClass);
        } catch (IllegalStateException var6) {
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, var6);
        }

        ChannelFuture f = b.bind(address);
        LOG.debug("Initiated server {} at {}.", f, address);
        return f;
    }

    protected void customizeBootstrap(ServerBootstrap b) {
    }

    protected Future<PCEPSessionImpl> createClient(InetSocketAddress address, ReconnectStrategy strategy, final PCEPAbstractDispatcher.PipelineInitializer initializer) {
        Bootstrap b = new Bootstrap();
        final PCEPProtocolSessionPromise p = new PCEPProtocolSessionPromise(this.executor, address, strategy, b);
        ((Bootstrap) b.option(ChannelOption.SO_KEEPALIVE, Boolean.valueOf(true))).handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) {
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

    private void setWorkerGroup(Bootstrap b) {
        if (b.group() == null) {
            b.group(this.workerGroup);
        }

    }

    protected Future<PCEPSessionImpl> createClient(InetSocketAddress address, ReconnectStrategy strategy, Bootstrap bootstrap, final PCEPAbstractDispatcher.PipelineInitializer initializer) {
        final PCEPProtocolSessionPromise p = new PCEPProtocolSessionPromise(this.executor, address, strategy, bootstrap);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) {
                initializer.initializeChannel(ch, p);
            }
        });
        p.connect();
        LOG.debug("Client created.");
        return p;
    }

    protected void customizeBootstrap(Bootstrap b) {
    }

    /**
     * @deprecated
     */
    @Deprecated
    protected Future<Void> createReconnectingClient(InetSocketAddress address, ReconnectStrategyFactory connectStrategyFactory, ReconnectStrategy reestablishStrategy, PCEPAbstractDispatcher.PipelineInitializer initializer) {
        return this.createReconnectingClient(address, connectStrategyFactory, initializer);
    }

    protected Future<Void> createReconnectingClient(InetSocketAddress address, ReconnectStrategyFactory connectStrategyFactory, PCEPAbstractDispatcher.PipelineInitializer initializer) {
        Bootstrap b = new Bootstrap();
        PCEPReconnectPromise p = new PCEPReconnectPromise(GlobalEventExecutor.INSTANCE, this, address, connectStrategyFactory, b, initializer);
        b.option(ChannelOption.SO_KEEPALIVE, Boolean.valueOf(true));
        this.customizeBootstrap(b);
        this.setWorkerGroup(b);
        this.setChannelFactory(b);
        p.connect();
        return p;
    }

    private void setChannelFactory(Bootstrap b) {
        try {
            b.channel(NioSocketChannel.class);
        } catch (IllegalStateException var3) {
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, var3);
        }

    }

    /**
     * @deprecated
     */
    @Deprecated
    public void close() {
        try {
            this.workerGroup.shutdownGracefully();
        } finally {
            this.bossGroup.shutdownGracefully();
        }

    }

    protected interface PipelineInitializer extends PCEPAbstractDispatcher.ChannelPipelineInitializer<SocketChannel> {
    }

    protected interface ChannelPipelineInitializer<CH extends Channel> {
        void initializeChannel(CH var1, Promise<PCEPSessionImpl> var2);
    }
}
