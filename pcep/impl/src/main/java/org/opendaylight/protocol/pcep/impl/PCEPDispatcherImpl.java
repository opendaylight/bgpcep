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
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPDispatcherDependencies;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PCEPDispatcher.
 */
public class PCEPDispatcherImpl implements PCEPDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPDispatcherImpl.class);

    private final PCEPSessionNegotiatorFactory<PCEPSessionImpl> snf;
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
    public PCEPDispatcherImpl(final @NonNull MessageRegistry registry,
            final @NonNull PCEPSessionNegotiatorFactory<PCEPSessionImpl> negotiatorFactory,
            final @NonNull EventLoopGroup bossGroup, final @NonNull EventLoopGroup workerGroup) {
        snf = requireNonNull(negotiatorFactory);
        this.bossGroup = requireNonNull(bossGroup);
        this.workerGroup = requireNonNull(workerGroup);
        hf = new PCEPHandlerFactory(registry);
        executor = requireNonNull(GlobalEventExecutor.INSTANCE);
    }

    @Override
    public final synchronized ChannelFuture createServer(final PCEPDispatcherDependencies dispatcherDependencies) {
        keys = dispatcherDependencies.getKeys();

        final ChannelPipelineInitializer initializer = (ch, promise) -> {
            ch.pipeline().addLast(hf.getDecoders());
            ch.pipeline().addLast("negotiator", snf
                    .getSessionNegotiator(dispatcherDependencies, ch, promise));
            ch.pipeline().addLast(hf.getEncoders());
        };

        final ServerBootstrap b = createServerBootstrap(initializer);
        final InetSocketAddress address = dispatcherDependencies.getAddress();
        final ChannelFuture f = b.bind(address);
        LOG.debug("Initiated server {} at {}.", f, address);

        // FIXME: err, why are we resetting this?
        keys = KeyMapping.of();
        return f;
    }

    synchronized ServerBootstrap createServerBootstrap(final ChannelPipelineInitializer initializer) {
        final ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                initializer.initializeChannel(ch, new DefaultPromise<>(executor));
            }
        });
        b.option(ChannelOption.SO_BACKLOG, 128);

        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        if (Epoll.isAvailable()) {
            b.channel(EpollServerSocketChannel.class);
            b.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        } else {
            b.channel(NioServerSocketChannel.class);
        }
        if (!keys.isEmpty()) {
            if (Epoll.isAvailable()) {
                b.option(EpollChannelOption.TCP_MD5SIG, keys.asMap());
            } else {
                throw new UnsupportedOperationException("Setting TCP-MD5 signatures is not supported",
                        Epoll.unavailabilityCause().getCause());
            }
        }

        // Make sure we are doing round-robin processing
        b.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1));

        if (b.config().group() == null) {
            b.group(bossGroup, workerGroup);
        }

        return b;
    }

    @Override
    public final PCEPSessionNegotiatorFactory<PCEPSessionImpl> getPCEPSessionNegotiatorFactory() {
        return snf;
    }

    protected interface ChannelPipelineInitializer {
        void initializeChannel(SocketChannel socketChannel, Promise<PCEPSessionImpl> promise);
    }
}
