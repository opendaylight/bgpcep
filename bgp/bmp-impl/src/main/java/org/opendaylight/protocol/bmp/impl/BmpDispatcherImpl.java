/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BmpDispatcherImpl implements BmpDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(BmpDispatcherImpl.class);
    private static final int MAX_CONNECTIONS_COUNT = 128;

    private final BmpHandlerFactory hf;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final BmpSessionFactory sessionFactory;
    private final MD5ServerChannelFactory scf;

    public BmpDispatcherImpl(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final BmpMessageRegistry registry,
            final BmpSessionFactory sessionFactory, final MD5ServerChannelFactory scf) {
        this.bossGroup = Preconditions.checkNotNull(bossGroup);
        this.workerGroup = Preconditions.checkNotNull(workerGroup);
        this.hf = new BmpHandlerFactory(Preconditions.checkNotNull(registry));
        this.sessionFactory = Preconditions.checkNotNull(sessionFactory);
        this.scf = scf;
    }

    @Override
    public ChannelFuture createServer(final InetSocketAddress address, final BmpSessionListenerFactory slf, final KeyMapping keys) {
        Preconditions.checkNotNull(address);
        Preconditions.checkNotNull(slf);
        Preconditions.checkState(!keys.isPresent() || this.scf.isPresent(), "No key access instance available, cannot use key mapping.");

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
            b.channelFactory(this.scf.get());
            b.option(MD5ChannelOption.TCP_MD5SIG, keys.get());
            LOG.debug("Adding MD5 keys {} to boostrap {}", keys.get(), b);
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
        try {
            this.workerGroup.shutdownGracefully();
        } finally {
            this.bossGroup.shutdownGracefully();
        }
    }
}
