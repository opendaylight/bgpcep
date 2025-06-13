/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Singleton
@Component(service = BGPNettyGroups.class)
public final class BGPNettyGroups implements AutoCloseable {
    @NonNullByDefault
    private abstract static class AbstractImpl {
        private static final ThreadFactory BOSS_TF = Thread.ofPlatform().name("bgp-boss-", 0).daemon(true).factory();
        private static final ThreadFactory WORKER_TF =
            Thread.ofPlatform().name("bgp-worker-", 0).daemon(true).factory();

        private final EventLoopGroup bossGroup;
        private final EventLoopGroup workerGroup;

        AbstractImpl(final IoHandlerFactory ioHandlerFactory) {
            bossGroup = new MultiThreadIoEventLoopGroup(BOSS_TF, ioHandlerFactory);
            workerGroup = new MultiThreadIoEventLoopGroup(WORKER_TF, ioHandlerFactory);
        }

        abstract Class<? extends SocketChannel> channelClass();

        abstract Class<? extends ServerSocketChannel> serverChannelClass();

        abstract void setupKeys(Bootstrap bootstrap, KeyMapping keys);
    }

    @NonNullByDefault
    private static final class EpollImpl extends AbstractImpl {
        EpollImpl() {
            super(EpollIoHandler.newFactory());
        }

        @Override
        Class<EpollSocketChannel> channelClass() {
            return EpollSocketChannel.class;
        }

        @Override
        Class<EpollServerSocketChannel> serverChannelClass() {
            return EpollServerSocketChannel.class;
        }

        @Override
        void setupKeys(final Bootstrap bootstrap, final KeyMapping keys) {
            bootstrap.option(EpollChannelOption.TCP_MD5SIG, keys.asMap());
        }
    }

    @NonNullByDefault
    private static final class NioImpl extends AbstractImpl {
        NioImpl() {
            super(NioIoHandler.newFactory());
        }

        @Override
        Class<NioSocketChannel> channelClass() {
            return NioSocketChannel.class;
        }

        @Override
        Class<NioServerSocketChannel> serverChannelClass() {
            return NioServerSocketChannel.class;
        }

        @Override
        void setupKeys(final Bootstrap bootstrap, final KeyMapping keys) {
            throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
        }
    }

    private static final long TIMEOUT = 10;

    private AbstractImpl impl;

    @Inject
    @Activate
    public BGPNettyGroups() {
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

    Bootstrap createBootstrap(final @Nullable KeyMapping keys) {
        final var bootstrap = new Bootstrap();
        bootstrap.channel(impl.channelClass());
        if (keys != null && !keys.isEmpty()) {
            impl.setupKeys(bootstrap, keys);
        }
        return bootstrap.group(impl.workerGroup);
    }

    ServerBootstrap createServerBootstrap() {
        final var bootstrap = new ServerBootstrap();
        bootstrap.channel(impl.serverChannelClass());
        return bootstrap.group(impl.bossGroup, impl.workerGroup);
    }
}
