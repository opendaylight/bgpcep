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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.SocketAddress;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.impl.BmpHandlerFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpMockDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMockDispatcher.class);

    final BmpHandlerFactory hf;
    private final BmpSessionFactory sessionFactory;

    public BmpMockDispatcher(final BmpMessageRegistry registry, final BmpSessionFactory sessionFactory) {
        this.sessionFactory = Preconditions.checkNotNull(sessionFactory);
        Preconditions.checkNotNull(registry);
        this.hf = new BmpHandlerFactory(registry);
    }

    public ChannelFuture createClient(final SocketAddress localAddress, final SocketAddress remoteAddress) {
        final NioEventLoopGroup workergroup = new NioEventLoopGroup();
        final Bootstrap b = new Bootstrap();

        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
        b.group(workergroup);

        b.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(final NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(BmpMockDispatcher.this.sessionFactory.getSession(ch, null));
                ch.pipeline().addLast(BmpMockDispatcher.this.hf.getEncoders());
            }
        });
        b.localAddress(localAddress);
        b.remoteAddress(remoteAddress);
        LOG.debug("BMP client {} <--> {} deployed", localAddress, remoteAddress);
        return b.connect();
    }
}
