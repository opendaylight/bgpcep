/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.io.Closeable;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PCEPDispatcher.
 */
public class PCEPDispatcherImpl implements PCEPDispatcher, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPDispatcherImpl.class);
    private static final Integer SOCKET_BACKLOG_SIZE = 128;
    private final PCEPSessionNegotiatorFactory snf;
    private final PCEPHandlerFactory hf;


    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventExecutor executor;
    private final MD5ServerChannelFactory<?> scf;
    private Optional<KeyMapping> keys;

    /**
     * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
     *
     * @param registry a message registry
     * @param negotiatorFactory a negotiation factory
     * @param bossGroup accepts an incoming connection
     * @param workerGroup handles the traffic of accepted connection
     */
    public PCEPDispatcherImpl(final MessageRegistry registry,
                              final PCEPSessionNegotiatorFactory negotiatorFactory,
                              final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this(registry, negotiatorFactory, bossGroup, workerGroup, null);
    }

    /**
     * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
     *
     * @param registry a message registry
     * @param negotiatorFactory a negotiation factory
     * @param bossGroup accepts an incoming connection
     * @param workerGroup handles the traffic of accepted connection
     * @param scf MD5ServerChannelFactory
     */
    public PCEPDispatcherImpl(final MessageRegistry registry,
                              final PCEPSessionNegotiatorFactory negotiatorFactory,
                              final EventLoopGroup bossGroup, final EventLoopGroup workerGroup,
                              final MD5ServerChannelFactory<?> scf) {
        this.snf = Preconditions.checkNotNull(negotiatorFactory);
        this.hf = new PCEPHandlerFactory(registry);
        this.bossGroup = Preconditions.checkNotNull(bossGroup);
        this.workerGroup = Preconditions.checkNotNull(workerGroup);
        this.executor = Preconditions.checkNotNull(GlobalEventExecutor.INSTANCE);
        this.scf = scf;
    }

    @Override
    public synchronized ChannelFuture createServer(final InetSocketAddress address,
                                                   final PCEPSessionListenerFactory listenerFactory, final PCEPPeerProposal peerProposal) {
        return createServer(address, Optional.<KeyMapping>absent(), listenerFactory, peerProposal);
    }

    @Override
    public synchronized ChannelFuture createServer(final InetSocketAddress address, final Optional<KeyMapping> keys,
                                                   final PCEPSessionListenerFactory listenerFactory, final PCEPPeerProposal peerProposal) {
        this.keys = keys;

        final ChannelPipelineInitializer initializer = new ChannelPipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
                ch.pipeline().addLast(PCEPDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast("negotiator", PCEPDispatcherImpl.this.snf.getSessionNegotiator(listenerFactory, ch, promise, peerProposal));
                ch.pipeline().addLast(PCEPDispatcherImpl.this.hf.getEncoders());
            }
        };
        final ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                initializer.initializeChannel(ch, new DefaultPromise(PCEPDispatcherImpl.this.executor));
            }
        });
        b.option(ChannelOption.SO_BACKLOG, SOCKET_BACKLOG_SIZE);

        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        this.customizeBootstrap(b);
        if (b.group() == null) {
            b.group(this.bossGroup, this.workerGroup);
        }

        final ChannelFuture f = b.bind(address);
        LOG.debug("Initiated server {} at {}.", f, address);

        this.keys = null;
        return f;
    }

    protected void customizeBootstrap(final ServerBootstrap b) {
        if (this.keys.isPresent()) {
            if (this.scf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }

            LOG.debug("Adding MD5 keys {} to bootstrap {}", this.keys.get(), b);
            b.channelFactory(this.scf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys.get());
        } else {
            b.channel(NioServerSocketChannel.class);
        }

        // Make sure we are doing round-robin processing
        b.childOption(ChannelOption.MAX_MESSAGES_PER_READ, 1);
    }

    @Override
    public void close() {
    }

    protected interface ChannelPipelineInitializer {
        void initializeChannel(SocketChannel socketChannel, Promise<PCEPSessionImpl> promise);
    }
}
