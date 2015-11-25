/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5NioSocketChannel;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BmpDispatcherImpl implements BmpDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(BmpDispatcherImpl.class);

    private static final int MAX_CONNECTIONS_COUNT = 128;

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int INITIAL_BACKOFF = 30_000;
    private static final int MAXIMUM_BACKOFF = 720_000;

    private final BmpHandlerFactory hf;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final BmpSessionFactory sessionFactory;
    private final Optional<MD5ServerChannelFactory<?>> md5ServerChFactory;
    private final Optional<MD5ChannelFactory<?>> md5ChannelFactory;

    public BmpDispatcherImpl(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup,
            final BmpMessageRegistry registry, final BmpSessionFactory sessionFactory) {
        this(bossGroup, workerGroup, registry, sessionFactory, Optional.<MD5ChannelFactory<?>>absent(), Optional.<MD5ServerChannelFactory<?>>absent());
    }

    public BmpDispatcherImpl(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup,
            final BmpMessageRegistry registry, final BmpSessionFactory sessionFactory,
            final Optional<MD5ChannelFactory<?>> cf, final Optional<MD5ServerChannelFactory<?>> scf) {
        this.bossGroup = Preconditions.checkNotNull(bossGroup);
        this.workerGroup = Preconditions.checkNotNull(workerGroup);
        this.hf = new BmpHandlerFactory(Preconditions.checkNotNull(registry));
        this.sessionFactory = Preconditions.checkNotNull(sessionFactory);
        this.md5ServerChFactory = Preconditions.checkNotNull(scf);
        this.md5ChannelFactory  = Preconditions.checkNotNull(cf);
    }

    @Override
    public ChannelFuture createClient(final InetSocketAddress address, final BmpSessionListenerFactory slf, final Optional<KeyMapping> keys) {

        final NioEventLoopGroup workergroup = new NioEventLoopGroup();
        final Bootstrap b = new Bootstrap();

        Preconditions.checkNotNull(address);

        if ( keys.isPresent() ) {
            b.channel(MD5NioSocketChannel.class);
            b.option(MD5ChannelOption.TCP_MD5SIG, keys.get());
        } else {
            LOG.info("no md5 key is not found. continue with bootstrap setup.");
            b.channel(NioSocketChannel.class);
        }
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
        b.group(workergroup);

        b.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(final NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(BmpDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast(BmpDispatcherImpl.this.sessionFactory.getSession(ch, slf));
            }
        });

        b.remoteAddress(address);
        final ChannelFuture channelPromise = b.connect();
        channelPromise.addListener(new BmpDispatcherImpl.BootstrapListener(b, address));
        return channelPromise;
    }

    @Override
    public ChannelFuture createServer(final InetSocketAddress address, final BmpSessionListenerFactory slf, final Optional<KeyMapping> keys) {
        Preconditions.checkNotNull(address);
        Preconditions.checkNotNull(slf);

        final ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(final Channel ch) throws Exception {
                ch.pipeline().addLast(BmpDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast(BmpDispatcherImpl.this.sessionFactory.getSession(ch, slf));
            }
        });

        b.option(ChannelOption.SO_BACKLOG, MAX_CONNECTIONS_COUNT);
        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        if (keys.isPresent()) {
            Preconditions.checkState(this.md5ServerChFactory.isPresent(), "No server channel factory instance available,  cannot use key mapping.");
            b.channelFactory(this.md5ServerChFactory.get());
            final KeyMapping key = keys.get();
            b.option(MD5ChannelOption.TCP_MD5SIG, key);
            LOG.debug("Adding MD5 keys {} to boostrap {}", key, b);
        } else {
            b.channel(NioServerSocketChannel.class);
        }
        b.group(this.bossGroup, this.workerGroup);
        final ChannelFuture f = b.bind(address);

        LOG.debug("Initiated BMP server {} at {}.", f, address);
        return f;
    }

    @Override
    public void close() {
    }

    private class BootstrapListener implements ChannelFutureListener {

        private final Bootstrap bootstrap;

        private long delay;

        private final InetSocketAddress address;

        public BootstrapListener(final Bootstrap bootstrap, final InetSocketAddress address) {
            this.bootstrap = bootstrap;
            this.address = address;
            this.delay = INITIAL_BACKOFF;
        }

        @Override
        public void operationComplete(final ChannelFuture cf) throws Exception {
            if (cf.isCancelled()) {
                LOG.debug("Connection {} cancelled!", cf);
            } else if (cf.isSuccess()) {
                LOG.debug("Connection {} succeeded!", cf);
            } else {
                if (delay > MAXIMUM_BACKOFF) {
                    LOG.warn("The time of maximum backoff has been exceeded. No further connection attempts with BMP router {}.", this.address);
                    cf.cancel(false);
                    return;
                }
                final EventLoop loop = cf.channel().eventLoop();
                loop.schedule(new Runnable() {
                    @Override
                    public void run() {
                        BootstrapListener.this.bootstrap.connect().addListener(BootstrapListener.this);
                    }
                }, this.delay, TimeUnit.MILLISECONDS);
                LOG.info("The connection try to BMP router {} failed. Next reconnection attempt in {} milliseconds.", this.address, this.delay);
                this.delay *= 2;
            }
        }
    }
}
