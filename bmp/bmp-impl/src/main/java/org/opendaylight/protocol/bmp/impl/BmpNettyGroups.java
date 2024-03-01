/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Singleton
@Component(service = BmpNettyGroups.class)
public final class BmpNettyGroups implements AutoCloseable {
    @FunctionalInterface
    public interface CreateChannel {
        ChannelInitializer<AbstractChannel> create(@NonNull BmpSessionFactory sessionFactory,
                @NonNull BmpHandlerFactory hf, @NonNull BmpSessionListenerFactory slf);
    }

    private abstract static class AbstractImpl {
        private final EventLoopGroup bossGroup;
        private final EventLoopGroup workerGroup;

        AbstractImpl(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
            this.bossGroup = requireNonNull(bossGroup);
            this.workerGroup = requireNonNull(workerGroup);
        }

        abstract void setupBootstrap(Bootstrap bootstrap);

        abstract void setupBootstrap(ServerBootstrap serverBootstrap);

        abstract void setupKeys(AbstractBootstrap<?, ?> bootstrap, KeyMapping keys);
    }

    private static final class EpollImpl extends AbstractImpl {
        EpollImpl() {
            super(new EpollEventLoopGroup(BOSS_TF), new EpollEventLoopGroup(WORKER_TF));
        }

        @Override
        void setupBootstrap(final Bootstrap bootstrap) {
            bootstrap.channel(EpollSocketChannel.class);
        }

        @Override
        void setupBootstrap(final ServerBootstrap serverBootstrap) {
            serverBootstrap.channel(EpollServerSocketChannel.class);
        }

        @Override
        void setupKeys(final AbstractBootstrap<?, ?> bootstrap, final KeyMapping keys) {
            bootstrap.option(EpollChannelOption.TCP_MD5SIG, keys.asMap());
        }
    }

    private static final class NioImpl extends AbstractImpl {
        NioImpl() {
            super(new NioEventLoopGroup(BOSS_TF), new NioEventLoopGroup(WORKER_TF));
        }

        @Override
        void setupBootstrap(final Bootstrap bootstrap) {
            bootstrap.channel(NioSocketChannel.class);
        }

        @Override
        void setupBootstrap(final ServerBootstrap serverBootstrap) {
            serverBootstrap.channel(NioServerSocketChannel.class);
        }

        @Override
        void setupKeys(final AbstractBootstrap<?, ?> bootstrap, final KeyMapping keys) {
            throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
        }
    }

    private static final ThreadFactory BOSS_TF = new ThreadFactoryBuilder()
        .setNameFormat("bmp-boss-%d")
        .setDaemon(true)
        .build();
    private static final ThreadFactory WORKER_TF = new ThreadFactoryBuilder()
        .setNameFormat("bmp-worker-%d")
        .setDaemon(true)
        .build();
    private static final int MAX_CONNECTIONS_COUNT = 128;
    private static final long TIMEOUT = 10;

    private AbstractImpl impl;

    @Inject
    @Activate
    public BmpNettyGroups() {
        impl = Epoll.isAvailable() ? new EpollImpl() : new NioImpl();
    }

    @PreDestroy
    @Deactivate
    @Override
    public void close() {
        if (impl != null) {
            impl.workerGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
            impl.bossGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
            impl = null;
        }
    }

    public static ChannelInitializer<AbstractChannel> createChannelWithDecoder(
            final @NonNull BmpSessionFactory sessionFactory, final @NonNull BmpHandlerFactory hf,
            final @NonNull BmpSessionListenerFactory slf) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(final AbstractChannel ch) throws Exception {
                ch.pipeline().addLast(hf.getDecoders()).addLast(sessionFactory.getSession(ch, slf));
            }
        };
    }

    public static ChannelInitializer<AbstractChannel> createChannelWithEncoder(
            final @NonNull BmpSessionFactory sessionFactory, final @NonNull BmpHandlerFactory hf,
            final @NonNull BmpSessionListenerFactory slf) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(final AbstractChannel ch) throws Exception {
                ch.pipeline().addLast(hf.getEncoders()).addLast(sessionFactory.getSession(ch, slf));
            }
        };
    }

    /**
     * To be used by BMP Dispatcher mainly.
     */
    public ServerBootstrap createServerBootstrap(final @NonNull BmpSessionFactory sessionFactory,
            final @NonNull BmpHandlerFactory hf, final @NonNull BmpSessionListenerFactory slf,
            final @NonNull CreateChannel createChannel, final @NonNull KeyMapping keys) {
        final var serverBootstrap = new ServerBootstrap();
        impl.setupBootstrap(serverBootstrap);
        if (!keys.isEmpty()) {
            impl.setupKeys(serverBootstrap, keys);
        }

        return serverBootstrap.childHandler(createChannel.create(sessionFactory, hf, slf))
            .option(ChannelOption.SO_BACKLOG, MAX_CONNECTIONS_COUNT)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .group(impl.bossGroup, impl.workerGroup);
    }

    /**
     * To be used by BMP Dispatcher mainly.
     */
    public Bootstrap createClientBootstrap(final @NonNull BmpSessionFactory sessionFactory,
            final @NonNull BmpHandlerFactory hf, final @NonNull CreateChannel createChannel,
            final @NonNull BmpSessionListenerFactory slf, final @NonNull InetSocketAddress remoteAddress,
            final int connectTimeout, final @NonNull KeyMapping keys) {
        return createClientBootstrap(sessionFactory, hf, createChannel, slf, remoteAddress, null, connectTimeout, keys,
            false);
    }

    public Bootstrap createClientBootstrap(final @NonNull BmpSessionFactory sessionFactory,
            final @NonNull BmpHandlerFactory hf, final @NonNull CreateChannel createChannel,
            final @NonNull BmpSessionListenerFactory slf, final @NonNull InetSocketAddress remoteAddress,
            final @Nullable SocketAddress localAddress, final int connectTimeout, final @NonNull KeyMapping keys,
            final boolean reuseAddress) {
        final var bootstrap = new Bootstrap();
        impl.setupBootstrap(bootstrap);
        if (!keys.isEmpty()) {
            impl.setupKeys(bootstrap, keys);
        }
        if (localAddress != null) {
            bootstrap.localAddress(localAddress);
        }
        return bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddress)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
            .group(impl.workerGroup)
            .handler(createChannel.create(sessionFactory, hf, slf))
            .remoteAddress(remoteAddress);
    }
}
