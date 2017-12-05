/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PCEPDispatcher.
 */
public class PCEPDispatcherImpl implements PCEPDispatcher, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPDispatcherImpl.class);
    private static final Integer SOCKET_BACKLOG_SIZE = 128;
    private static final long TIMEOUT = 10;
    private final PCEPSessionNegotiatorFactory snf;
    private final PCEPHandlerFactory hf;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventExecutor executor;
    @GuardedBy("this")
    private KeyMapping keys;

    /**
     * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
     *
     * @param registry          a message registry
     * @param negotiatorFactory a negotiation factory
     * @param bossGroup         accepts an incoming connection
     * @param workerGroup       handles the traffic of accepted connection
     */
    public PCEPDispatcherImpl(@Nonnull final MessageRegistry registry,
            @Nonnull final PCEPSessionNegotiatorFactory negotiatorFactory,
            @Nonnull final EventLoopGroup bossGroup, @Nonnull final EventLoopGroup workerGroup) {
        this.snf = requireNonNull(negotiatorFactory);
        this.hf = new PCEPHandlerFactory(registry);
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup();
            this.workerGroup = new EpollEventLoopGroup();
        } else {
            this.bossGroup = requireNonNull(bossGroup);
            this.workerGroup = requireNonNull(workerGroup);
        }
        this.executor = requireNonNull(GlobalEventExecutor.INSTANCE);
    }

    @Override
    public final synchronized ChannelFuture createServer(final InetSocketAddress address,
            final PCEPSessionListenerFactory listenerFactory, final PCEPPeerProposal peerProposal) {
        return createServer(address, KeyMapping.getKeyMapping(), listenerFactory, peerProposal);
    }

    @Override
    public final synchronized ChannelFuture createServer(final InetSocketAddress address, final KeyMapping keys,
            final PCEPSessionListenerFactory listenerFactory, final PCEPPeerProposal peerProposal) {
        this.keys = keys;

        final ChannelPipelineInitializer initializer = (ch, promise) -> {
            ch.pipeline().addLast(this.hf.getDecoders());
            ch.pipeline().addLast("negotiator", this.snf.getSessionNegotiator(listenerFactory, ch, promise, peerProposal));
            ch.pipeline().addLast(this.hf.getEncoders());
        };

        final ServerBootstrap b = createServerBootstrap(initializer);
        final ChannelFuture f = b.bind(address);
        LOG.debug("Initiated server {} at {}.", f, address);

        this.keys = KeyMapping.getKeyMapping();
        return f;
    }

    synchronized ServerBootstrap createServerBootstrap(final ChannelPipelineInitializer initializer) {
        final ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                initializer.initializeChannel(ch, new DefaultPromise<>(PCEPDispatcherImpl.this.executor));
            }
        });
        b.option(ChannelOption.SO_BACKLOG, SOCKET_BACKLOG_SIZE);

        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        if (Epoll.isAvailable()) {
            b.channel(EpollServerSocketChannel.class);
            b.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        } else {
            b.channel(NioServerSocketChannel.class);
        }
        if (!this.keys.isEmpty()) {
            if (Epoll.isAvailable()) {
                b.option(EpollChannelOption.TCP_MD5SIG, this.keys);
            } else {
                throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
            }
        }

        // Make sure we are doing round-robin processing
        b.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1));

        if (b.config().group() == null) {
            b.group(this.bossGroup, this.workerGroup);
        }

        return b;
    }

    @Override
    public final void close() {
        if (Epoll.isAvailable()) {
            this.workerGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
            this.bossGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
        }
    }

    @Override
    public final PCEPSessionNegotiatorFactory<?> getPCEPSessionNegotiatorFactory() {
        return this.snf;
    }

    protected interface ChannelPipelineInitializer {
        void initializeChannel(SocketChannel socketChannel, Promise<PCEPSessionImpl> promise);
    }
}
