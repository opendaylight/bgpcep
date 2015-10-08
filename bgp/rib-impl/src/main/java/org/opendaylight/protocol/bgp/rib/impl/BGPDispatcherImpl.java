/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.protocol.BGPProtocolSessionPromise;
import org.opendaylight.protocol.bgp.rib.impl.protocol.BGPReconnectPromise;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.ChannelPipelineInitializer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionNegotiatorFactory;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of BGPDispatcher.
 */
public class BGPDispatcherImpl implements BGPDispatcher, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPDispatcherImpl.class);
    private static final int SOCKET_BACKLOG_SIZE = 128;
    private final MD5ServerChannelFactory<?> scf;
    private final MD5ChannelFactory<?> cf;
    private final BGPHandlerFactory hf;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventExecutor executor;
    private KeyMapping keys;

    public BGPDispatcherImpl(final MessageRegistry messageRegistry, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this(messageRegistry, bossGroup, workerGroup, null, null);
    }

    public BGPDispatcherImpl(final MessageRegistry messageRegistry, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final MD5ChannelFactory<?> cf, final MD5ServerChannelFactory<?> scf) {
        this.bossGroup = Preconditions.checkNotNull(bossGroup);
        this.workerGroup = Preconditions.checkNotNull(workerGroup);
        this.executor = Preconditions.checkNotNull(GlobalEventExecutor.INSTANCE);
        this.hf = new BGPHandlerFactory(messageRegistry);
        this.cf = cf;
        this.scf = scf;
        this.keys = null;
    }

    @Override
    public synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress address, final BGPPeerRegistry listener, final ReconnectStrategy strategy) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(listener);
        final ChannelPipelineInitializer initializer = BGPChannel.createChannelPipelineInitializer(BGPDispatcherImpl.this.hf, snf);

        final Bootstrap b = new Bootstrap();
        final BGPProtocolSessionPromise p = new BGPProtocolSessionPromise(this.executor, address, strategy, b);
        b.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        b.handler(BGPChannel.createChannelInitializer(initializer, p));
        this.customizeBootstrap(b);
        setWorkerGroup(b);
        p.connect();
        LOG.debug("Client created.");
        return p;
    }

    @Override
    public void close() {
        try {
            this.workerGroup.shutdownGracefully();
        } finally {
            this.bossGroup.shutdownGracefully();
        }
    }

    @Override
    public synchronized Future<Void> createReconnectingClient(final InetSocketAddress address, final BGPPeerRegistry peerRegistry,
        final ReconnectStrategyFactory connectStrategyFactory, final KeyMapping keys) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(peerRegistry);
        this.keys = keys;

        final Bootstrap b = new Bootstrap();
        final BGPReconnectPromise p = new BGPReconnectPromise<BGPSessionImpl>(GlobalEventExecutor.INSTANCE, address,
            connectStrategyFactory, b, BGPChannel.createChannelPipelineInitializer(BGPDispatcherImpl.this.hf, snf));
        b.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        this.customizeBootstrap(b);
        setWorkerGroup(b);
        p.connect();

        this.keys = null;

        return p;
    }

    @Override
    public ChannelFuture createServer(final BGPPeerRegistry registry, final InetSocketAddress address) {
        final BGPServerSessionNegotiatorFactory snf = new BGPServerSessionNegotiatorFactory(registry);
        final ChannelPipelineInitializer initializer = BGPChannel.createChannelPipelineInitializer(BGPDispatcherImpl.this.hf, snf);
        final ServerBootstrap b = new ServerBootstrap();
        b.childHandler(BGPChannel.createChannelInitializer(initializer, new DefaultPromise(BGPDispatcherImpl.this.executor)));
        b.option(ChannelOption.SO_BACKLOG, Integer.valueOf(SOCKET_BACKLOG_SIZE));
        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        this.customizeBootstrap(b);

        final ChannelFuture f = b.bind(address);
        LOG.debug("Initiated server {} at {}.", f, address);
        return f;
    }

    protected void customizeBootstrap(final Bootstrap b) {
        if (this.keys != null && !this.keys.isEmpty()) {
            if (this.cf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }
            b.channelFactory(this.cf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys);
        }

        // Make sure we are doing round-robin processing
        b.option(ChannelOption.MAX_MESSAGES_PER_READ, 1);
    }

    private void customizeBootstrap(final ServerBootstrap b) {
        if (this.keys != null && !this.keys.isEmpty()) {
            if (this.scf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }
            b.channelFactory(this.scf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys);
        }

        // Make sure we are doing round-robin processing
        b.option(ChannelOption.MAX_MESSAGES_PER_READ, 1);

        if (b.group() == null) {
            b.group(this.bossGroup, this.workerGroup);
        }

        try {
            b.channel(NioServerSocketChannel.class);
        } catch (final IllegalStateException e) {
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, e);
        }
    }

    private void setWorkerGroup(final Bootstrap b) {
        if (b.group() == null) {
            b.group(this.workerGroup);
        }
        try {
            b.channel(NioSocketChannel.class);
        } catch (final IllegalStateException e) {
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, e);
        }
    }

    private static final class BGPChannel {
        private static final String NEGOTIATOR = "negotiator";

        private BGPChannel() {

        }

        public static <S extends BGPSession, T extends BGPSessionNegotiatorFactory> ChannelPipelineInitializer
            createChannelPipelineInitializer(final BGPHandlerFactory hf, final T snf) {
            return new ChannelPipelineInitializer<S>() {
                @Override
                public void initializeChannel(final SocketChannel ch, final Promise<S> promise) {
                    ch.pipeline().addLast(hf.getDecoders());
                    ch.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(ch, promise));
                    ch.pipeline().addLast(hf.getEncoders());
                }
            };
        }

        public static <S extends BGPSession> ChannelHandler createChannelInitializer(final ChannelPipelineInitializer initializer, final Promise<S> promise) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel ch) {
                    initializer.initializeChannel(ch, promise);
                }
            };
        }
    }
}
