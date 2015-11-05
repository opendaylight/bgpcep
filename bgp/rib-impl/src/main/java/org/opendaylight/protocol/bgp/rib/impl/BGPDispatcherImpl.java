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
    private final MD5ServerChannelFactory<?> serverChannelFactory;
    private final MD5ChannelFactory<?> channelFactory;
    private final BGPHandlerFactory handlerFactory;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventExecutor executor;
    private Optional<KeyMapping> keys;

    public BGPDispatcherImpl(final MessageRegistry messageRegistry, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this(messageRegistry, bossGroup, workerGroup, null, null);
    }

    public BGPDispatcherImpl(final MessageRegistry messageRegistry, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final MD5ChannelFactory<?> cf, final MD5ServerChannelFactory<?> scf) {
        this.bossGroup = Preconditions.checkNotNull(bossGroup);
        this.workerGroup = Preconditions.checkNotNull(workerGroup);
        this.executor = Preconditions.checkNotNull(GlobalEventExecutor.INSTANCE);
        this.handlerFactory = new BGPHandlerFactory(messageRegistry);
        this.channelFactory = cf;
        this.serverChannelFactory = scf;
        this.keys = Optional.absent();
    }

    @Override
    public synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress address, final BGPPeerRegistry listener, final ReconnectStrategy strategy) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(listener);
        final ChannelPipelineInitializer initializer = BGPChannel.createChannelPipelineInitializer(BGPDispatcherImpl.this.handlerFactory, snf);

        final Bootstrap bootstrap = createClientBootStrap();
        final BGPProtocolSessionPromise sessionPromise = new BGPProtocolSessionPromise(this.executor, address, strategy, bootstrap);
        bootstrap.handler(BGPChannel.createChannelInitializer(initializer, sessionPromise));
        sessionPromise.connect();
        LOG.debug("Client created.");
        return sessionPromise;
    }

    protected Bootstrap createClientBootStrap() {
        final Bootstrap bootstrap = new Bootstrap();
        if (this.keys.isPresent()) {
            if (this.channelFactory == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }
            bootstrap.channelFactory(this.channelFactory);
            bootstrap.option(MD5ChannelOption.TCP_MD5SIG, this.keys.get());
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }

        // Make sure we are doing round-robin processing
        bootstrap.option(ChannelOption.MAX_MESSAGES_PER_READ, 1);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);

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
        final ReconnectStrategyFactory connectStrategyFactory, final Optional<KeyMapping> keys) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(peerRegistry);
        this.keys = keys;
        final Bootstrap bootstrap = createClientBootStrap();
        final BGPReconnectPromise reconnectPromise = new BGPReconnectPromise<BGPSessionImpl>(GlobalEventExecutor.INSTANCE, address,
            connectStrategyFactory, bootstrap, BGPChannel.createChannelPipelineInitializer(BGPDispatcherImpl.this.handlerFactory, snf));
        reconnectPromise.connect();
        this.keys = null;
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
        serverBootstrap.childHandler(BGPChannel.createChannelInitializer(initializer, new DefaultPromise(BGPDispatcherImpl.this.executor)));
        serverBootstrap.option(ChannelOption.SO_BACKLOG, Integer.valueOf(SOCKET_BACKLOG_SIZE));
        serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        if (this.keys.isPresent()) {
            if (this.serverChannelFactory == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }
            serverBootstrap.channelFactory(this.serverChannelFactory);
            serverBootstrap.option(MD5ChannelOption.TCP_MD5SIG, this.keys.get());
        } else {
            serverBootstrap.channel(NioServerSocketChannel.class);
        }

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

        public static <S extends BGPSession> ChannelHandler createChannelInitializer(final ChannelPipelineInitializer initializer, final Promise<S> promise) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel channel) {
                    initializer.initializeChannel(channel, promise);
                }
            };
        }
    }
}
