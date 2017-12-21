/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bmp.impl.BmpDispatcherUtil.createBootstrapListener;
import static org.opendaylight.protocol.bmp.impl.BmpDispatcherUtil.createClientBootstrap;
import static org.opendaylight.protocol.bmp.impl.BmpDispatcherUtil.createServerBootstrap;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
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
    public ChannelFuture createClient(final InetSocketAddress localAddress, final BmpSessionListenerFactory slf,
            final KeyMapping keys) {
        final Bootstrap bootstrap = createClientBootstrap(localAddress, this.sessionFactory, this.hf,
                BmpDispatcherUtil::createChannelWithDecoder, slf, this.workerGroup,
                CONNECT_TIMEOUT, keys);
        final ChannelFuture channelPromise = bootstrap.connect();
        channelPromise.addListener(createBootstrapListener(bootstrap, localAddress, INITIAL_BACKOFF, MAXIMUM_BACKOFF));
        LOG.debug("Initiated BMP Client {} at {}.", channelPromise, localAddress);
        return channelPromise;
    }

    @Override
    public ChannelFuture createServer(final InetSocketAddress address, final BmpSessionListenerFactory slf,
            final KeyMapping keys) {
        final ServerBootstrap serverBootstrap = createServerBootstrap(this.sessionFactory, this.hf, slf,
                BmpDispatcherUtil::createChannelWithDecoder, this.bossGroup, this.workerGroup, keys);
        final ChannelFuture channelFuture = serverBootstrap.bind(address);
        LOG.debug("Initiated BMP server {} at {}.", channelFuture, address);
        return channelFuture;
    }

    @Override
    public void close() {
        if (Epoll.isAvailable()) {
            this.workerGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
            this.bossGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
        }
    }
}
