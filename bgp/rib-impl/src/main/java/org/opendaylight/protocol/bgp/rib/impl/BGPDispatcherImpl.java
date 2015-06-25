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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.protocol.BGPProtocolSessionPromise;
import org.opendaylight.protocol.bgp.rib.impl.protocol.BGPReconnectPromise;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of BGPDispatcher.
 */
public class BGPDispatcherImpl implements BGPDispatcher, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPDispatcherImpl.class);
    private static final String NEGOTIATOR = "negotiator";
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
        this.bossGroup = (EventLoopGroup) Preconditions.checkNotNull(bossGroup);
        this.workerGroup = (EventLoopGroup) Preconditions.checkNotNull(workerGroup);
        this.executor = (EventExecutor) Preconditions.checkNotNull(GlobalEventExecutor.INSTANCE);
        this.hf = new BGPHandlerFactory(messageRegistry);
        this.cf = cf;
        this.scf = scf;
    }

    @Override
    public synchronized Future<BGPSession> createClient(final InetSocketAddress address,
                                                        final AsNumber remoteAs, final BGPPeerRegistry listener, final ReconnectStrategy strategy) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(remoteAs, listener);
        return createClient(address, strategy, new ChannelPipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<BGPSession> promise) {
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(null, ch, promise));
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getEncoders());
            }
        });
    }

    protected Future<BGPSession> createClient(InetSocketAddress address, ReconnectStrategy strategy, final ChannelPipelineInitializer initializer) {
        Bootstrap b = new Bootstrap();
        final BGPProtocolSessionPromise p = new BGPProtocolSessionPromise(this.executor, address, strategy, b);
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

    public Future<BGPSession> createClient(InetSocketAddress address, ReconnectStrategy strategy, Bootstrap bootstrap,
                                           final ChannelPipelineInitializer initializer) {
        final BGPProtocolSessionPromise p = new BGPProtocolSessionPromise(this.executor, address, strategy, bootstrap);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) {
                initializer.initializeChannel(ch, p);
            }
        });
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
    public synchronized Future<Void> createReconnectingClient(final InetSocketAddress address,
                                                              final AsNumber remoteAs, final BGPPeerRegistry peerRegistry, final ReconnectStrategyFactory connectStrategyFactory,
                                                              final ReconnectStrategyFactory reestablishStrategyFactory, final KeyMapping keys) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(remoteAs, peerRegistry);

        this.keys = keys;
        final Future<Void> ret = createReconnectingClient(address, connectStrategyFactory, reestablishStrategyFactory.createReconnectStrategy(), new ChannelPipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<BGPSession> promise) {
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(null, ch, promise));
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getEncoders());
            }
        });
        this.keys = null;

        return ret;
    }

    protected Future<Void> createReconnectingClient(InetSocketAddress address, ReconnectStrategyFactory connectStrategyFactory, ReconnectStrategy reestablishStrategy, ChannelPipelineInitializer initializer) {
        return this.createReconnectingClient(address, connectStrategyFactory, initializer);
    }

    protected Future<Void> createReconnectingClient(InetSocketAddress address, ReconnectStrategyFactory connectStrategyFactory, ChannelPipelineInitializer initializer) {
        Bootstrap b = new Bootstrap();
        BGPReconnectPromise p = new BGPReconnectPromise(GlobalEventExecutor.INSTANCE, this, address, connectStrategyFactory, b, initializer);
        b.option(ChannelOption.SO_KEEPALIVE, Boolean.valueOf(true));
        this.customizeBootstrap(b);
        this.setWorkerGroup(b);
        this.setChannelFactory(b);
        p.connect();
        return p;
    }

    @Override
    public ChannelFuture createServer(final BGPPeerRegistry registry, final InetSocketAddress address, final BGPSessionValidator sessionValidator) {
        return this.createServer(registry, address, sessionValidator, null);
    }

    @Override
    public ChannelFuture createServer(final BGPPeerRegistry registry, final InetSocketAddress address, final BGPSessionValidator sessionValidator, final KeyMapping keys) {
        final BGPServerSessionNegotiatorFactory snf = new BGPServerSessionNegotiatorFactory(sessionValidator, registry);

        this.keys = keys;
        final ChannelFuture ret = createServer(address, new ChannelPipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<BGPSession> promise) {
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(null, ch, promise));
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getEncoders());
            }
        });
        this.keys = null;

        return ret;
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

    protected void customizeBootstrap(final ServerBootstrap b) {
        if (this.keys != null && !this.keys.isEmpty()) {
            if (this.scf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }
            b.channelFactory(this.scf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys);
        }

        // Make sure we are doing round-robin processing
        b.childOption(ChannelOption.MAX_MESSAGES_PER_READ, 1);
    }

    private void setWorkerGroup(Bootstrap b) {
        if (b.group() == null) {
            b.group(this.workerGroup);
        }

    }

    private void setChannelFactory(Bootstrap b) {
        try {
            b.channel(NioSocketChannel.class);
        } catch (IllegalStateException var3) {
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, var3);
        }
    }

    public ChannelFuture createServer(InetSocketAddress address, ChannelPipelineInitializer initializer) {
        return this.createServer(address, NioServerSocketChannel.class, initializer);
    }

    protected ChannelFuture createServer(SocketAddress address, Class<? extends ServerChannel> channelClass, final ChannelPipelineInitializer initializer) {
        ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
            protected void initChannel(io.netty.channel.socket.SocketChannel ch) {
                initializer.initializeChannel(ch, new DefaultPromise(BGPDispatcherImpl.this.executor));
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

    public interface ChannelPipelineInitializer {
        void initializeChannel(SocketChannel var1, Promise<BGPSession> var2);
    }
}
