/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Singleton
@Component
public final class BGPNettyGroups implements AutoCloseable {
    private abstract static class AbstractImpl {
        private final EventLoopGroup bossGroup;
        private final EventLoopGroup workerGroup;

        AbstractImpl(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
            this.bossGroup = requireNonNull(bossGroup);
            this.workerGroup = requireNonNull(workerGroup);
        }

        abstract void setupBootstrap(Bootstrap bootstrap);

        abstract void setupBootstrap(ServerBootstrap serverBootstrap);

        abstract void setupKeys(Bootstrap bootstrap, KeyMapping keys);
    }

    private static final class EpollImpl extends AbstractImpl {
        EpollImpl() {
            super(new EpollEventLoopGroup(), new EpollEventLoopGroup());
        }

        @Override
        void setupBootstrap(final Bootstrap bootstrap) {
            bootstrap.channel(EpollSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        }

        @Override
        void setupBootstrap(final ServerBootstrap serverBootstrap) {
            serverBootstrap.channel(EpollServerSocketChannel.class);
            serverBootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        }

        @Override
        void setupKeys(final Bootstrap bootstrap, final KeyMapping keys) {
            bootstrap.option(EpollChannelOption.TCP_MD5SIG, keys.asMap());
        }
    }

    private static final class NioImpl extends AbstractImpl {
        NioImpl() {
            super(new NioEventLoopGroup(), new NioEventLoopGroup());
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
        impl.setupBootstrap(bootstrap);
        if (keys != null && !keys.isEmpty()) {
            impl.setupKeys(bootstrap, keys);
        }
        return bootstrap.group(impl.workerGroup);
    }

    ServerBootstrap createServerBootstrap() {
        final var bootstrap = new ServerBootstrap();
        impl.setupBootstrap(bootstrap);
        return bootstrap.group(impl.bossGroup, impl.workerGroup);
    }
}
