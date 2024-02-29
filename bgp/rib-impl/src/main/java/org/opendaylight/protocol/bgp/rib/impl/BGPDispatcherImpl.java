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
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
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
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of BGPDispatcher.
 */
@Singleton
@Component(immediate = true)
public final class BGPDispatcherImpl implements BGPDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(BGPDispatcherImpl.class);
    private static final int SOCKET_BACKLOG_SIZE = 128;

    private static final WriteBufferWaterMark WATER_MARK = new WriteBufferWaterMark(128 * 1024, 256 * 1024);

    // An adaptive allocator, so we size our message buffers based on what we receive, but make sure we process one
    // message at a time. This should be good enough for most cases, although we could optimize it a bit based on
    // whether we actually negotiate use of large messages -- based on that the range of allocations can be constrained
    // from the default 64-65536 range to 64-4096.
    private static final RecvByteBufAllocator RECV_ALLOCATOR = new AdaptiveRecvByteBufAllocator().maxMessagesPerRead(1);

    private final BGPHandlerFactory handlerFactory;
    private final BGPPeerRegistry bgpPeerRegistry;
    private final BGPNettyGroups nettyGroups;

    @Inject
    @Activate
    public BGPDispatcherImpl(@Reference final BGPExtensionConsumerContext extensions,
            @Reference final BGPNettyGroups nettyGroups, @Reference final BGPPeerRegistry bgpPeerRegistry) {
        this.nettyGroups = requireNonNull(nettyGroups);
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
        return nettyGroups.createBootstrap(keys)
            // Make sure we are doing round-robin processing
            .option(ChannelOption.RCVBUF_ALLOCATOR, RECV_ALLOCATOR)
            .option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
            .option(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK)
            .option(ChannelOption.SO_REUSEADDR, reuseAddress)
            .localAddress(localAddress);
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
        return nettyGroups.createServerBootstrap()
            .childHandler(BGPChannel.createServerChannelHandler(initializer))
            .option(ChannelOption.SO_BACKLOG, SOCKET_BACKLOG_SIZE)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK)
            // Make sure we are doing round-robin processing
            .option(ChannelOption.RCVBUF_ALLOCATOR, RECV_ALLOCATOR);
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