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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.concepts.KeyMapping;

public final class BmpDispatcherUtil {
    private static final int MAX_CONNECTIONS_COUNT = 128;

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

    /**
     * To be used by BMP Dispatcher mainly.
     */
    public static ServerBootstrap createServerBootstrap(
            @Nonnull final BmpSessionFactory sessionFactory,
            @Nonnull final BmpHandlerFactory hf,
            @Nullable final BmpSessionListenerFactory slf,
            @Nonnull CreateChannel createChannel,
            @Nonnull final EventLoopGroup bossGroup,
            @Nonnull final EventLoopGroup workerGroup,
            @Nonnull final KeyMapping keys) {
        return createServerBootstrap(sessionFactory, hf, slf, createChannel, bossGroup, workerGroup, keys,
                true);
    }

    public static ServerBootstrap createServerBootstrap(
            @Nonnull final BmpSessionFactory sessionFactory,
            @Nonnull final BmpHandlerFactory hf,
            @Nullable final BmpSessionListenerFactory slf,
            @Nonnull CreateChannel createChannel,
            @Nonnull final EventLoopGroup bossGroup,
            @Nonnull final EventLoopGroup workerGroup,
            @Nonnull final KeyMapping keys,
            boolean tryEpollSocket) {

        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.childHandler(createChannel.create(sessionFactory, hf, slf));
        serverBootstrap.option(ChannelOption.SO_BACKLOG, MAX_CONNECTIONS_COUNT);
        serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.group(bossGroup, workerGroup);

        if (!tryEpollSocket) {
            serverBootstrap.channel(NioServerSocketChannel.class);
        } else {
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
        }

        return serverBootstrap;
    }

    /**
     * To be used by BMP Dispatcher mainly.
     */
    public static Bootstrap createClientBootstrap(
            @Nonnull final BmpSessionFactory sessionFactory,
            @Nonnull final BmpHandlerFactory hf,
            @Nonnull CreateChannel createChannel,
            @Nonnull final BmpSessionListenerFactory slf,
            @Nonnull final InetSocketAddress remoteAddress,
            @Nonnull final EventLoopGroup workerGroup,
            final int connectTimeout,
            @Nonnull final KeyMapping keys) {
        return createClientBootstrap(sessionFactory, hf, createChannel, slf, remoteAddress, null,
                workerGroup, connectTimeout, keys, false, true);
    }

    public static Bootstrap createClientBootstrap(
            @Nonnull final BmpSessionFactory sessionFactory,
            @Nonnull final BmpHandlerFactory hf,
            @Nonnull CreateChannel createChannel,
            @Nonnull final BmpSessionListenerFactory slf,
            @Nonnull final InetSocketAddress remoteAddress,
            @Nullable final SocketAddress localAddress,
            @Nonnull final EventLoopGroup workerGroup,
            final int connectTimeout,
            @Nonnull final KeyMapping keys,
            boolean reuseAddress,
            boolean tryEpollSocket) {
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddress);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        bootstrap.group(workerGroup);
        bootstrap.handler(createChannel.create(sessionFactory, hf, slf));
        if (localAddress != null) {
            bootstrap.localAddress(localAddress);
        }
        bootstrap.remoteAddress(remoteAddress);

        if (!tryEpollSocket) {
            bootstrap.channel(NioSocketChannel.class);

        } else {
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
        }
        return bootstrap;
    }

    @FunctionalInterface
    public interface CreateChannel {
        ChannelInitializer<AbstractChannel> create(
                @Nonnull final BmpSessionFactory sessionFactory,
                @Nonnull final BmpHandlerFactory hf,
                @Nullable final BmpSessionListenerFactory slf);

    }
}
