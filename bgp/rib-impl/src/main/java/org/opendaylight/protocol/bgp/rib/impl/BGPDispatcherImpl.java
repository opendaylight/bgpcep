/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.protocol.BGPProtocolSessionPromise;
import org.opendaylight.protocol.bgp.rib.impl.protocol.BGPReconnectPromise;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.ChannelPipelineInitializer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionNegotiatorFactory;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of BGPDispatcher.
 */
public class BGPDispatcherImpl implements BGPDispatcher, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPDispatcherImpl.class);
    private static final int SOCKET_BACKLOG_SIZE = 128;
    private static final int FIX_BUFFER_SIZE = 1;
    private static final long TIMEOUT = 10;

    private static final WriteBufferWaterMark WATER_MARK = new WriteBufferWaterMark(128 * 1024, 256 * 1024);

    private final BGPHandlerFactory handlerFactory;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final BGPPeerRegistry bgpPeerRegistry;

    public BGPDispatcherImpl(final MessageRegistry messageRegistry, final EventLoopGroup bossGroup,
            final EventLoopGroup workerGroup, final BGPPeerRegistry bgpPeerRegistry) {
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup();
            this.workerGroup = new EpollEventLoopGroup();
        } else {
            this.bossGroup = requireNonNull(bossGroup);
            this.workerGroup = requireNonNull(workerGroup);
        }
        this.bgpPeerRegistry = requireNonNull(bgpPeerRegistry);
        this.handlerFactory = new BGPHandlerFactory(messageRegistry);
    }

    @Override
    public synchronized Future<BGPSessionImpl> createClient(
            final InetSocketAddress remoteAddress,
            final int retryTimer) {
        return createClient(remoteAddress, retryTimer,
                createClientBootStrap(KeyMapping.getKeyMapping(), false));
    }

    private synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress remoteAddress,
            final int retryTimer, final Bootstrap clientBootStrap) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(this.bgpPeerRegistry);
        final ChannelPipelineInitializer<BGPSessionImpl> initializer = BGPChannel.createChannelPipelineInitializer(
                this.handlerFactory, snf);

        final BGPProtocolSessionPromise<BGPSessionImpl> sessionPromise = new BGPProtocolSessionPromise<>(remoteAddress,
                retryTimer, clientBootStrap, this.bgpPeerRegistry);
        clientBootStrap.handler(BGPChannel.createClientChannelHandler(initializer, sessionPromise));
        sessionPromise.connect();
        LOG.debug("Client created.");
        return sessionPromise;
    }

    @VisibleForTesting
    public synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress localAddress,
            final InetSocketAddress remoteAddress, final int retryTimer, final boolean reuseAddress) {
        final Bootstrap clientBootStrap = createClientBootStrap(KeyMapping.getKeyMapping(), reuseAddress);
        clientBootStrap.localAddress(localAddress);
        return createClient(remoteAddress, retryTimer, clientBootStrap);
    }

    private synchronized Bootstrap createClientBootStrap(final KeyMapping keys, final boolean reuseAddress) {
        final Bootstrap bootstrap = new Bootstrap();
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        if (keys != null && !keys.isEmpty()) {
            if (Epoll.isAvailable()) {
                bootstrap.option(EpollChannelOption.TCP_MD5SIG, keys);
            } else {
                throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
            }
        }

        // Make sure we are doing round-robin processing
        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(FIX_BUFFER_SIZE));
        bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK);
        bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddress);

        if (bootstrap.config().group() == null) {
            bootstrap.group(this.workerGroup);
        }

        return bootstrap;
    }

    @Override
    public synchronized void close() throws InterruptedException {
        if (Epoll.isAvailable()) {
            LOG.debug("Closing Dispatcher");
            this.workerGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
            this.bossGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
        }
    }

    @Override
    public synchronized Future<Void> createReconnectingClient(final InetSocketAddress remoteAddress,
            final int retryTimer, final KeyMapping keys) {
        return createReconnectingClient(remoteAddress, retryTimer, keys, null, false);
    }

    @VisibleForTesting
    synchronized Future<Void> createReconnectingClient(final InetSocketAddress remoteAddress,
            final int retryTimer, final KeyMapping keys, final InetSocketAddress localAddress,
            final boolean reuseAddress) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(this.bgpPeerRegistry);
        final Bootstrap bootstrap = createClientBootStrap(keys, reuseAddress);
        bootstrap.localAddress(localAddress);
        final BGPReconnectPromise<?> reconnectPromise = new BGPReconnectPromise<>(GlobalEventExecutor.INSTANCE,
                remoteAddress, retryTimer, bootstrap, this.bgpPeerRegistry,
                BGPChannel.createChannelPipelineInitializer(this.handlerFactory, snf));
        reconnectPromise.connect();
        return reconnectPromise;
    }

    @Override
    public synchronized ChannelFuture createServer(final InetSocketAddress serverAddress) {
        final BGPServerSessionNegotiatorFactory snf = new BGPServerSessionNegotiatorFactory(this.bgpPeerRegistry);
        final ChannelPipelineInitializer<?> initializer = BGPChannel.
                createChannelPipelineInitializer(this.handlerFactory, snf);
        final ServerBootstrap serverBootstrap = createServerBootstrap(initializer);
        final ChannelFuture channelFuture = serverBootstrap.bind(serverAddress);
        LOG.debug("Initiated server {} at {}.", channelFuture, serverAddress);
        return channelFuture;
    }

    @Override
    public BGPPeerRegistry getBGPPeerRegistry() {
        return this.bgpPeerRegistry;
    }

    private synchronized ServerBootstrap createServerBootstrap(final ChannelPipelineInitializer initializer) {
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        if (Epoll.isAvailable()) {
            serverBootstrap.channel(EpollServerSocketChannel.class);
            serverBootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        } else {
            serverBootstrap.channel(NioServerSocketChannel.class);
        }
        final ChannelHandler serverChannelHandler = BGPChannel.createServerChannelHandler(initializer);
        serverBootstrap.childHandler(serverChannelHandler);

        serverBootstrap.option(ChannelOption.SO_BACKLOG, SOCKET_BACKLOG_SIZE);
        serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK);

        // Make sure we are doing round-robin processing
        serverBootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(FIX_BUFFER_SIZE));

        if (serverBootstrap.config().group() == null) {
            serverBootstrap.group(this.bossGroup, this.workerGroup);
        }
        return serverBootstrap;
    }

    private static final class BGPChannel {
        private static final String NEGOTIATOR = "negotiator";

        private BGPChannel() {

        }

        static <S extends BGPSession, T extends BGPSessionNegotiatorFactory<S>> ChannelPipelineInitializer<S>
        createChannelPipelineInitializer(final BGPHandlerFactory hf, final T snf) {
            return (channel, promise) -> {
                channel.pipeline().addLast(hf.getDecoders());
                channel.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(channel, promise));
                channel.pipeline().addLast(hf.getEncoders());
            };
        }

        static <S extends BGPSession> ChannelHandler createClientChannelHandler(
                final ChannelPipelineInitializer<S> initializer, final Promise<S> promise) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel channel) {
                    initializer.initializeChannel(channel, promise);
                }
            };
        }

        static ChannelHandler createServerChannelHandler(final ChannelPipelineInitializer initializer) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                @SuppressWarnings("unchecked")
                protected void initChannel(final SocketChannel channel) {
                    initializer.initializeChannel(channel,
                            new DefaultPromise<BGPSessionImpl>(GlobalEventExecutor.INSTANCE));
                }
            };
        }
    }
}