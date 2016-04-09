/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
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
    private static final int HIGH_WATER_MARK = 256 * 1024;
    private static final int LOW_WATER_MARK = 128 * 1024;

    private final BGPHandlerFactory handlerFactory;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    public BGPDispatcherImpl(final MessageRegistry messageRegistry, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this.bossGroup = Preconditions.checkNotNull(bossGroup);
        this.workerGroup = Preconditions.checkNotNull(workerGroup);
        this.handlerFactory = new BGPHandlerFactory(messageRegistry);
    }

    @Override
    public synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress address, final BGPPeerRegistry listener, final int retryTimer) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(listener);
        final ChannelPipelineInitializer initializer = BGPChannel.createChannelPipelineInitializer(BGPDispatcherImpl.this.handlerFactory, snf);

        final Bootstrap bootstrap = createClientBootStrap(Optional.<KeyMapping>absent());
        final BGPProtocolSessionPromise sessionPromise = new BGPProtocolSessionPromise(address, retryTimer, bootstrap);
        bootstrap.handler(BGPChannel.createClientChannelHandler(initializer, sessionPromise));
        sessionPromise.connect();
        LOG.debug("Client created.");
        return sessionPromise;
    }

    protected Bootstrap createClientBootStrap(final Optional<KeyMapping> keys) {
        final Bootstrap bootstrap = new Bootstrap();
        if(Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        if (keys.isPresent()) {
            if(Epoll.isAvailable()){
                bootstrap.option(EpollChannelOption.TCP_MD5SIG, keys.get());
            } else {
                throw new UnsupportedOperationException ("BGP Peer is configured with password but native transport is not available");
            }
        }

        // Make sure we are doing round-robin processing
        bootstrap.option(ChannelOption.MAX_MESSAGES_PER_READ, 1);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        bootstrap.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, HIGH_WATER_MARK);
        bootstrap.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, LOW_WATER_MARK);

        if (bootstrap.group() == null) {
            bootstrap.group(this.workerGroup);
        }

        return bootstrap;
    }

    @Override
    public void close() {
    }

    @Override
    public synchronized Future<Void> createReconnectingClient(final InetSocketAddress address, final BGPPeerRegistry peerRegistry,
            final int retryTimer, final Optional<KeyMapping> keys) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(peerRegistry);
        final Bootstrap bootstrap = createClientBootStrap(keys);
        final BGPReconnectPromise reconnectPromise = new BGPReconnectPromise(GlobalEventExecutor.INSTANCE, address,
            retryTimer, bootstrap, BGPChannel.createChannelPipelineInitializer(BGPDispatcherImpl.this.handlerFactory, snf));
        reconnectPromise.connect();
        return reconnectPromise;
    }

    @Override
    public ChannelFuture createServer(final BGPPeerRegistry registry, final InetSocketAddress address) {
        final BGPServerSessionNegotiatorFactory snf = new BGPServerSessionNegotiatorFactory(registry);
        final ChannelPipelineInitializer initializer = BGPChannel.createChannelPipelineInitializer(BGPDispatcherImpl.this.handlerFactory, snf);
        final ServerBootstrap serverBootstrap = createServerBootstrap(initializer);
        final ChannelFuture channelFuture = serverBootstrap.bind(address);
        LOG.debug("Initiated server {} at {}.", channelFuture, address);
        return channelFuture;
    }

    private ServerBootstrap createServerBootstrap(final ChannelPipelineInitializer initializer) {
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        if(Epoll.isAvailable()) {
            //FIXME Bug-5566
            serverBootstrap.channel(EpollServerSocketChannel.class);
        } else {
            serverBootstrap.channel(NioServerSocketChannel.class);
        }
        final ChannelHandler serverChannelHandler = BGPChannel.createServerChannelHandler(initializer);
        serverBootstrap.childHandler(serverChannelHandler);

        serverBootstrap.option(ChannelOption.SO_BACKLOG, Integer.valueOf(SOCKET_BACKLOG_SIZE));
        serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, HIGH_WATER_MARK);
        serverBootstrap.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, LOW_WATER_MARK);

        // Make sure we are doing round-robin processing
        serverBootstrap.option(ChannelOption.MAX_MESSAGES_PER_READ, 1);

        if (serverBootstrap.group() == null) {
            serverBootstrap.group(this.bossGroup, this.workerGroup);
        }
        return serverBootstrap;
    }

    private static final class BGPChannel {
        private static final String NEGOTIATOR = "negotiator";

        private BGPChannel() {

        }

        public static <S extends BGPSession, T extends BGPSessionNegotiatorFactory> ChannelPipelineInitializer
            createChannelPipelineInitializer(final BGPHandlerFactory hf, final T snf) {
            return new ChannelPipelineInitializer<S>() {
                @Override
                public void initializeChannel(final SocketChannel channel, final Promise<S> promise) {
                    channel.pipeline().addLast(hf.getDecoders());
                    channel.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(channel, promise));
                    channel.pipeline().addLast(hf.getEncoders());
                }
            };
        }

        public static <S extends BGPSession> ChannelHandler createClientChannelHandler(final ChannelPipelineInitializer initializer, final Promise<S> promise) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel channel) {
                    initializer.initializeChannel(channel, promise);
                }
            };
        }

        public static ChannelHandler createServerChannelHandler(final ChannelPipelineInitializer initializer) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel channel) {
                    initializer.initializeChannel(channel, new DefaultPromise<BGPSessionImpl>(GlobalEventExecutor.INSTANCE));
                }
            };
        }
    }
}
