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
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.RecvByteBufAllocator;
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
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.impl.protocol.BGPProtocolSessionPromise;
import org.opendaylight.protocol.bgp.rib.impl.protocol.BGPReconnectPromise;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.ChannelPipelineInitializer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionNegotiatorFactory;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of BGPDispatcher.
 */
@Singleton
@Component(immediate = true, service = BGPDispatcher.class)
public final class BGPDispatcherImpl implements BGPDispatcher, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPDispatcherImpl.class);
    private static final int SOCKET_BACKLOG_SIZE = 128;
    private static final long TIMEOUT = 10;

    private static final WriteBufferWaterMark WATER_MARK = new WriteBufferWaterMark(128 * 1024, 256 * 1024);

    // An adaptive allocator, so we size our message buffers based on what we receive, but make sure we process one
    // message at a time. This should be good enough for most cases, although we could optimize it a bit based on
    // whether we actually negotiate use of large messages -- based on that the range of allocations can be constrained
    // from the default 64-65536 range to 64-4096.
    private static final RecvByteBufAllocator RECV_ALLOCATOR = new AdaptiveRecvByteBufAllocator().maxMessagesPerRead(1);

    private final BGPHandlerFactory handlerFactory;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final BGPPeerRegistry bgpPeerRegistry;

    @Inject
    @Activate
    public BGPDispatcherImpl(@Reference final BGPExtensionConsumerContext extensions,
            @Reference(target = "(type=global-boss-group)") final EventLoopGroup bossGroup,
            @Reference(target = "(type=global-worker-group)") final EventLoopGroup workerGroup,
            @Reference final BGPPeerRegistry bgpPeerRegistry) {
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup();
            this.workerGroup = new EpollEventLoopGroup();
        } else {
            this.bossGroup = requireNonNull(bossGroup);
            this.workerGroup = requireNonNull(workerGroup);
        }
        this.bgpPeerRegistry = requireNonNull(bgpPeerRegistry);
        handlerFactory = new BGPHandlerFactory(extensions.getMessageRegistry());
    }

    @VisibleForTesting
    public synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress localAddress,
            final InetSocketAddress remoteAddress, final int retryTimer, final boolean reuseAddress) {
        final Bootstrap clientBootStrap = createClientBootStrap(KeyMapping.of(), reuseAddress, localAddress);
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(bgpPeerRegistry);
        final ChannelPipelineInitializer<BGPSessionImpl> initializer = BGPChannel.createChannelPipelineInitializer(
                handlerFactory, snf);

        final BGPProtocolSessionPromise<BGPSessionImpl> sessionPromise = new BGPProtocolSessionPromise<>(remoteAddress,
                retryTimer, clientBootStrap, bgpPeerRegistry);
        clientBootStrap.handler(BGPChannel.createClientChannelHandler(initializer, sessionPromise));
        sessionPromise.connect();
        LOG.debug("Client created.");
        return sessionPromise;
    }

    private synchronized Bootstrap createClientBootStrap(final KeyMapping keys, final boolean reuseAddress,
            final InetSocketAddress localAddress) {
        final Bootstrap bootstrap = new Bootstrap();
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        if (keys != null && !keys.isEmpty()) {
            if (Epoll.isAvailable()) {
                bootstrap.option(EpollChannelOption.TCP_MD5SIG, keys.asMap());
            } else {
                throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
            }
        }

        // Make sure we are doing round-robin processing
        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, RECV_ALLOCATOR);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK);
        bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddress);

        if (bootstrap.config().group() == null) {
            bootstrap.group(workerGroup);
        }
        bootstrap.localAddress(localAddress);

        return bootstrap;
    }

    @Deactivate
    @PreDestroy
    @Override
    public synchronized void close() {
        if (Epoll.isAvailable()) {
            LOG.debug("Closing Dispatcher");
            workerGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
            bossGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
        }
    }

    @Override
    public synchronized Future<Void> createReconnectingClient(final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress, final int retryTimer, final KeyMapping keys) {
        return createReconnectingClient(remoteAddress, retryTimer, keys, localAddress, false);
    }

    @VisibleForTesting
    synchronized Future<Void> createReconnectingClient(final InetSocketAddress remoteAddress,
            final int retryTimer, final KeyMapping keys, final InetSocketAddress localAddress,
            final boolean reuseAddress) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(bgpPeerRegistry);
        final Bootstrap bootstrap = createClientBootStrap(keys, reuseAddress, localAddress);
        final BGPReconnectPromise<?> reconnectPromise = new BGPReconnectPromise<>(GlobalEventExecutor.INSTANCE,
                remoteAddress, retryTimer, bootstrap, bgpPeerRegistry,
                BGPChannel.createChannelPipelineInitializer(handlerFactory, snf));
        reconnectPromise.connect();
        return reconnectPromise;
    }

    @Override
    public synchronized ChannelFuture createServer(final InetSocketAddress serverAddress) {
        final BGPServerSessionNegotiatorFactory snf = new BGPServerSessionNegotiatorFactory(bgpPeerRegistry);
        final ChannelPipelineInitializer<?> initializer = BGPChannel.createChannelPipelineInitializer(
            handlerFactory, snf);
        final ServerBootstrap serverBootstrap = createServerBootstrap(initializer);
        final ChannelFuture channelFuture = serverBootstrap.bind(serverAddress);
        LOG.debug("Initiated server {} at {}.", channelFuture, serverAddress);
        return channelFuture;
    }

    @Override
    public BGPPeerRegistry getBGPPeerRegistry() {
        return bgpPeerRegistry;
    }

    private synchronized ServerBootstrap createServerBootstrap(final ChannelPipelineInitializer<?> initializer) {
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
        serverBootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, RECV_ALLOCATOR);

        if (serverBootstrap.config().group() == null) {
            serverBootstrap.group(bossGroup, workerGroup);
        }
        return serverBootstrap;
    }

    private static final class BGPChannel {
        private BGPChannel() {

        }

        static <S extends BGPSession, T extends BGPSessionNegotiatorFactory<S>> ChannelPipelineInitializer<S>
            createChannelPipelineInitializer(final BGPHandlerFactory hf, final T snf) {
            return (channel, promise) -> {
                channel.pipeline().addLast(hf.getDecoders());
                channel.pipeline().addLast(AbstractBGPSessionNegotiator.NEGOTIATOR,
                    snf.getSessionNegotiator(channel, promise));
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

        static <S extends BGPSession> ChannelHandler createServerChannelHandler(
                final ChannelPipelineInitializer<S> initializer) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel channel) {
                    initializer.initializeChannel(channel, new DefaultPromise<>(GlobalEventExecutor.INSTANCE));
                }
            };
        }
    }
}