/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import static java.util.Objects.requireNonNull;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherUtil;
import org.opendaylight.protocol.bmp.impl.BmpHandlerFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BmpMockDispatcher implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMockDispatcher.class);
    private static final int CONNECT_TIMEOUT = 2000;
    private static final int INITIAL_BACKOFF = 15_000;
    private static final KeyMapping KEY_MAPPING = KeyMapping.getKeyMapping();
    private final BmpHandlerFactory hf;
    private final BmpSessionFactory sessionFactory;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final BmpMockSessionListenerFactory slf;
    @GuardedBy("this")
    private boolean close;

    BmpMockDispatcher(final BmpMessageRegistry registry, final BmpSessionFactory sessionFactory) {
        this.sessionFactory = requireNonNull(sessionFactory);
        this.slf = new BmpMockSessionListenerFactory();
        requireNonNull(registry);
        this.hf = new BmpHandlerFactory(registry);
    }

    ChannelFuture createClient(@Nonnull final SocketAddress localAddress,
            @Nonnull final InetSocketAddress remoteAddress) {
        final Bootstrap bootstrap = BmpDispatcherUtil.createClientBootstrap(this.sessionFactory, this.hf,
                BmpDispatcherUtil::createChannelWithEncoder, this.slf, remoteAddress, localAddress, this.workerGroup,
                CONNECT_TIMEOUT, KEY_MAPPING, true, false);
        final ChannelFuture channelFuture = bootstrap.connect(remoteAddress);
        LOG.info("BMP client {} <--> {} deployed", localAddress, remoteAddress);
        channelFuture.addListener(new BootstrapListener(bootstrap, localAddress, remoteAddress));
        return channelFuture;
    }

    ChannelFuture createServer(final InetSocketAddress localAddress) {
        requireNonNull(localAddress);
        final ServerBootstrap serverBootstrap = BmpDispatcherUtil.createServerBootstrap(this.sessionFactory,
                this.hf, this.slf, BmpDispatcherUtil::createChannelWithEncoder,
                this.bossGroup, this.workerGroup, KEY_MAPPING, false);
        final ChannelFuture channelFuture = serverBootstrap.bind(localAddress);
        LOG.info("Initiated BMP server at {}.", localAddress);
        return channelFuture;
    }

    @Override
    public synchronized void close() {
        this.close = true;
    }

    private class BootstrapListener implements ChannelFutureListener {
        private final Bootstrap bootstrap;
        private final InetSocketAddress remoteAddress;
        private final SocketAddress localAddress;
        private long delay;
        private Timer timer = new Timer();

        BootstrapListener(final Bootstrap bootstrap, final SocketAddress localAddress,
                final InetSocketAddress remoteAddress) {
            this.bootstrap = bootstrap;
            this.remoteAddress = remoteAddress;
            this.localAddress = localAddress;
            this.delay = INITIAL_BACKOFF;
        }

        @Override
        public void operationComplete(final ChannelFuture future) throws Exception {
            if (future.isCancelled()) {
                LOG.debug("Connection {} cancelled!", future);
            } else if (future.isSuccess()) {
                LOG.debug("Connection {} succeeded!", future);
                future.channel().closeFuture().addListener((ChannelFutureListener) channelFuture -> scheduleConnect());
            } else {
                final EventLoop loop = future.channel().eventLoop();
                loop.schedule(() -> this.bootstrap.connect().addListener(this), this.delay, TimeUnit.MILLISECONDS);
                LOG.info("The connection try to BMP router {} failed. Next reconnection attempt in {} milliseconds.",
                        this.remoteAddress, this.delay);
            }
        }

        private void scheduleConnect() {
            if (!BmpMockDispatcher.this.close) {

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        createClient(BootstrapListener.this.localAddress,
                                BmpMockDispatcher.BootstrapListener.this.remoteAddress);
                    }
                }, (long) 5);
            }
        }
    }
}
