/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.impl.BmpHandlerFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BmpMockDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMockDispatcher.class);
    private static final int CONNECT_TIMEOUT = 2000;
    private static final int MAX_CONNECTIONS_COUNT = 128;

    private final BmpHandlerFactory hf;
    private final BmpSessionFactory sessionFactory;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    BmpMockDispatcher(final BmpMessageRegistry registry, final BmpSessionFactory sessionFactory) {
        this.sessionFactory = Preconditions.checkNotNull(sessionFactory);
        Preconditions.checkNotNull(registry);
        this.hf = new BmpHandlerFactory(registry);
    }

    private Bootstrap createClientInstance(final SocketAddress localAddress) {
        final NioEventLoopGroup workergroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap();

        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.group(workergroup);

        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(final NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(BmpMockDispatcher.this.sessionFactory.getSession(ch, null));
                ch.pipeline().addLast(BmpMockDispatcher.this.hf.getEncoders());
            }
        });
        bootstrap.localAddress(localAddress);
        return bootstrap;
    }

    ChannelFuture createClient(final SocketAddress localAddress, final SocketAddress remoteAddress) {
        Preconditions.checkNotNull(localAddress);
        Preconditions.checkNotNull(remoteAddress);

        // ideally we should use Bootstrap clones here
        final Bootstrap bootstrap = createClientInstance(localAddress);
        final ChannelFuture channelFuture = bootstrap.connect(remoteAddress);
        LOG.info("BMP client {} <--> {} deployed", localAddress, remoteAddress);
        return channelFuture;
    }

    private ServerBootstrap createServerInstance() {
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(final Channel ch) throws Exception {
                ch.pipeline().addLast(BmpMockDispatcher.this.sessionFactory.getSession(ch, null));
                ch.pipeline().addLast(BmpMockDispatcher.this.hf.getEncoders());
            }
        });

        serverBootstrap.option(ChannelOption.SO_BACKLOG, MAX_CONNECTIONS_COUNT);
        serverBootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.group(this.bossGroup, this.workerGroup);
        return serverBootstrap;
    }

    ChannelFuture createServer(final InetSocketAddress localAddress) {
        Preconditions.checkNotNull(localAddress);
        final ServerBootstrap serverBootstrap = createServerInstance();
        final ChannelFuture channelFuture = serverBootstrap.bind(localAddress);
        LOG.info("Initiated BMP server at {}.", localAddress);
        return channelFuture;
    }
}
