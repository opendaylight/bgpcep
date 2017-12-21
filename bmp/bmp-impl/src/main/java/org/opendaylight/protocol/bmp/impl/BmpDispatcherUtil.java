/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpDispatcherUtil {
    public static final int MAX_CONNECTIONS_COUNT = 128;

    private BmpDispatcherUtil() {
        throw new UnsupportedOperationException();
    }

    public static ChannelInitializer<AbstractChannel> createChannelWithDecoder(
            @Nonnull final BmpSessionFactory sessionFactory,
            @Nonnull final BmpHandlerFactory hf,
            @Nullable final BmpSessionListenerFactory slf) {
        return new ChannelInitializer<AbstractChannel>() {
            @Override
            protected void initChannel(final AbstractChannel ch) throws Exception {
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast(sessionFactory.getSession(ch, slf));
            }
        };
    }

    public static ChannelInitializer<AbstractChannel> createChannelWithEncoder(
            @Nonnull final BmpSessionFactory sessionFactory,
            @Nonnull final BmpHandlerFactory hf,
            @Nullable final BmpSessionListenerFactory slf) {
        return new ChannelInitializer<AbstractChannel>() {
            @Override
            protected void initChannel(final AbstractChannel ch) throws Exception {
                ch.pipeline().addLast(hf.getEncoders());
                ch.pipeline().addLast(sessionFactory.getSession(ch, slf));
            }
        };
    }

    public static ServerBootstrap createServerBootstrap(
            @Nonnull final BmpSessionFactory sessionFactory,
            @Nonnull final BmpHandlerFactory hf,
            @Nullable final BmpSessionListenerFactory slf,
            @Nonnull CreateChannel createChannel,
            @Nonnull final EventLoopGroup bossGroup,
            @Nonnull final EventLoopGroup workerGroup,
            @Nonnull final KeyMapping keys) {
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.childHandler(createChannel.create(sessionFactory, hf, slf));
        serverBootstrap.option(ChannelOption.SO_BACKLOG, MAX_CONNECTIONS_COUNT);
        serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.group(bossGroup, workerGroup);

        if (Epoll.isAvailable()) {
            serverBootstrap.channel(EpollServerSocketChannel.class);
        } else {
            serverBootstrap.channel(NioServerSocketChannel.class);
        }

        if (!keys.isEmpty()) {
            if (Epoll.isAvailable()) {
                serverBootstrap.option(EpollChannelOption.TCP_MD5SIG, keys);
            } else {
                throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
            }
        }
        return serverBootstrap;
    }

    public static Bootstrap createClientBootstrap(
            @Nonnull final SocketAddress localAddress,
            @Nonnull final BmpSessionFactory sessionFactory,
            @Nonnull final BmpHandlerFactory hf,
            @Nonnull CreateChannel createChannel,
            @Nullable final BmpSessionListenerFactory slf,
            @Nonnull final EventLoopGroup workerGroup,
            final int connectTimeout,
            @Nonnull final KeyMapping keys) {
        return createClientBootstrap(localAddress, sessionFactory, hf, createChannel, slf, workerGroup, connectTimeout,
                keys, false);
    }

    public static Bootstrap createClientBootstrap(
            @Nonnull final SocketAddress localAddress,
            @Nonnull final BmpSessionFactory sessionFactory,
            @Nonnull final BmpHandlerFactory hf,
            @Nonnull CreateChannel createChannel,
            @Nullable final BmpSessionListenerFactory slf,
            @Nonnull final EventLoopGroup workerGroup,
            final int connectTimeout,
            @Nonnull final KeyMapping keys,
            boolean reuseAddress) {
        final Bootstrap bootstrap = new Bootstrap();
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        if (!keys.isEmpty()) {
            if (Epoll.isAvailable()) {
                bootstrap.option(EpollChannelOption.TCP_MD5SIG, keys);
            } else {
                throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
            }
        }
        bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddress);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        bootstrap.group(workerGroup);
        bootstrap.handler(createChannel.create(sessionFactory, hf, slf));
        bootstrap.remoteAddress(localAddress);
        return bootstrap;
    }

    public static BootstrapListener createBootstrapListener(
            @Nonnull final Bootstrap bootstrap,
            @Nonnull final InetSocketAddress address, final int initialBackoff, final int maximumBackoff) {
        return new BootstrapListener(bootstrap, address, initialBackoff, maximumBackoff);
    }

    @FunctionalInterface
    public interface CreateChannel {
        ChannelInitializer<AbstractChannel> create(
                @Nonnull final BmpSessionFactory sessionFactory,
                @Nonnull final BmpHandlerFactory hf,
                @Nullable final BmpSessionListenerFactory slf);

    }

    private static class BootstrapListener implements ChannelFutureListener {

        private static final Logger LOG = LoggerFactory.getLogger(BootstrapListener.class);

        private final Bootstrap bootstrap;
        private final InetSocketAddress address;
        private final int maximumBackoff;
        private int initialBackoff;

        BootstrapListener(final Bootstrap bootstrap, final InetSocketAddress address, final int initialBackoff,
                final int maximumBackoff) {
            this.bootstrap = bootstrap;
            this.address = address;
            this.initialBackoff = initialBackoff;
            this.maximumBackoff = maximumBackoff;
        }

        @Override
        public void operationComplete(final ChannelFuture cf) throws Exception {
            if (cf.isCancelled()) {
                LOG.debug("Connection {} cancelled!", cf);
            } else if (cf.isSuccess()) {
                LOG.debug("Connection {} succeeded!", cf);
            } else {
                if (this.maximumBackoff != -1 && this.initialBackoff > this.maximumBackoff) {
                    LOG.warn("The time of maximum backoff has been exceeded. No further connection attempts with BMP "
                            + "router {}.", this.address);
                    cf.cancel(false);
                    return;
                }
                final EventLoop loop = cf.channel().eventLoop();
                loop.schedule(() -> this.bootstrap.connect()
                        .addListener(this), this.initialBackoff, TimeUnit.MILLISECONDS);
                LOG.info("The connection try to BMP router {} failed. Next reconnection attempt in {} milliseconds.",
                        this.address, this.initialBackoff);
                this.initialBackoff *= 2;
            }
        }
    }
}
