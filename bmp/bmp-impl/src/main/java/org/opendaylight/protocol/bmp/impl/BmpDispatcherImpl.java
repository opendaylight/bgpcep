/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import static java.util.Objects.requireNonNull;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BmpDispatcherImpl implements BmpDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(BmpDispatcherImpl.class);

    private static final int MAX_CONNECTIONS_COUNT = 128;

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int INITIAL_BACKOFF = 30_000;
    private static final int MAXIMUM_BACKOFF = 720_000;
    private static final long TIMEOUT = 10;

    private final BmpHandlerFactory hf;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final BmpSessionFactory sessionFactory;

    public BmpDispatcherImpl(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup,
            final BmpMessageRegistry registry, final BmpSessionFactory sessionFactory) {
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup();
            this.workerGroup = new EpollEventLoopGroup();
        } else {
            this.bossGroup = requireNonNull(bossGroup);
            this.workerGroup = requireNonNull(workerGroup);
        }
        this.hf = new BmpHandlerFactory(requireNonNull(registry));
        this.sessionFactory = requireNonNull(sessionFactory);
    }

    @Override
    public ChannelFuture createClient(final InetSocketAddress address, final BmpSessionListenerFactory slf,
        final KeyMapping keys) {

        final Bootstrap b = new Bootstrap();

        requireNonNull(address);

        if (Epoll.isAvailable()) {
            b.channel(EpollSocketChannel.class);
        } else {
            b.channel(NioSocketChannel.class);
        }
        if (!keys.isEmpty()) {
            if (Epoll.isAvailable()) {
                b.option(EpollChannelOption.TCP_MD5SIG, keys);
            } else {
                throw new UnsupportedOperationException (Epoll.unavailabilityCause().getCause());
            }
        }
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
        b.group(this.workerGroup);

        b.handler(new ChannelInitializer<AbstractChannel>() {
            @Override
            protected void initChannel(final AbstractChannel ch) throws Exception {
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
    public ChannelFuture createServer(final InetSocketAddress address, final BmpSessionListenerFactory slf,
        final KeyMapping keys) {
        requireNonNull(address);
        requireNonNull(slf);

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

        if (Epoll.isAvailable()) {
            b.channel(EpollServerSocketChannel.class);
        } else {
            b.channel(NioServerSocketChannel.class);
        }

        if (!keys.isEmpty()) {
            if (Epoll.isAvailable()) {
                b.option(EpollChannelOption.TCP_MD5SIG, keys);
            } else {
                throw new UnsupportedOperationException (Epoll.unavailabilityCause().getCause());
            }
        }
        b.group(this.bossGroup, this.workerGroup);
        final ChannelFuture f = b.bind(address);

        LOG.debug("Initiated BMP server {} at {}.", f, address);
        return f;
    }

    @Override
    public void close() {
        if (Epoll.isAvailable()) {
            this.workerGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
            this.bossGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
        }
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
                if (this.delay > MAXIMUM_BACKOFF) {
                    LOG.warn("The time of maximum backoff has been exceeded. No further connection attempts with BMP " +
                        "router {}.", this.address);
                    cf.cancel(false);
                    return;
                }
                final EventLoop loop = cf.channel().eventLoop();
                loop.schedule(() -> this.bootstrap.connect().addListener(this), this.delay, TimeUnit.MILLISECONDS);
                LOG.info("The connection try to BMP router {} failed. Next reconnection attempt in {} milliseconds.",
                    this.address, this.delay);
                this.delay *= 2;
            }
        }
    }
}
