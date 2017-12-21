/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bmp.impl.BmpDispatcherUtil.createBootstrapListener;
import static org.opendaylight.protocol.bmp.impl.BmpDispatcherUtil.createChannelWithEncoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.impl.BmpHandlerFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BmpMockDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMockDispatcher.class);
    private static final int CONNECT_TIMEOUT = 2000;
    private static final int MAX_CONNECTIONS_COUNT = 128;
    private static final int INITIAL_BACKOFF = 15_000;
    private final BmpHandlerFactory hf;
    private final BmpSessionFactory sessionFactory;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    BmpMockDispatcher(final BmpMessageRegistry registry, final BmpSessionFactory sessionFactory) {
        this.sessionFactory = requireNonNull(sessionFactory);
        requireNonNull(registry);
        this.hf = new BmpHandlerFactory(registry);
    }

    private Bootstrap createClientInstance(final SocketAddress localAddress) {
        final NioEventLoopGroup workergroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap();

        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
        bootstrap.group(workergroup);
        bootstrap.handler(createChannelWithEncoder(sessionFactory, hf, null));
        bootstrap.localAddress(localAddress);
        return bootstrap;
    }

    ChannelFuture createClient(@Nonnull final SocketAddress localAddress,
            @Nonnull final InetSocketAddress remoteAddress) {

        // ideally we should use Bootstrap clones here
        final Bootstrap bootstrap = createClientInstance(localAddress);
        bootstrap.remoteAddress(remoteAddress);
        final ChannelFuture channelFuture = bootstrap.connect(remoteAddress);
        LOG.info("BMP client {} <--> {} deployed", localAddress, remoteAddress);
        channelFuture.addListener(createBootstrapListener(bootstrap, remoteAddress, INITIAL_BACKOFF, -1));
        return channelFuture;
    }

    private ServerBootstrap createServerInstance() {
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.childHandler(createChannelWithEncoder(sessionFactory, hf, null));
        serverBootstrap.option(ChannelOption.SO_BACKLOG, MAX_CONNECTIONS_COUNT);
        serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.group(this.bossGroup, this.workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);
        return serverBootstrap;
    }

    ChannelFuture createServer(final InetSocketAddress localAddress) {
        requireNonNull(localAddress);
        final ServerBootstrap serverBootstrap = createServerInstance();
        final ChannelFuture channelFuture = serverBootstrap.bind(localAddress);
        LOG.info("Initiated BMP server at {}.", localAddress);
        return channelFuture;
    }

}
