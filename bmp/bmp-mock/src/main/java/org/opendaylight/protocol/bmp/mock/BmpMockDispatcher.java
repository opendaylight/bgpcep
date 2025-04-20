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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.impl.BmpHandlerFactory;
import org.opendaylight.protocol.bmp.impl.BmpNettyGroups;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BmpMockDispatcher implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BmpMockDispatcher.class);
    private static final int CONNECT_TIMEOUT = 2000;
    private static final int INITIAL_BACKOFF = 15_000;

    private final BmpHandlerFactory hf;
    private final BmpSessionFactory sessionFactory;

    private final BmpNettyGroups nettyGroups;
    private final BmpMockSessionListenerFactory slf;

    private boolean close;

    BmpMockDispatcher(final BmpMessageRegistry registry, final BmpSessionFactory sessionFactory) {
        this.sessionFactory = requireNonNull(sessionFactory);
        slf = new BmpMockSessionListenerFactory();
        requireNonNull(registry);
        hf = new BmpHandlerFactory(registry);
        nettyGroups = new BmpNettyGroups();
    }

    ChannelFuture createClient(final @NonNull SocketAddress localAddress,
            final @NonNull InetSocketAddress remoteAddress) {
        final Bootstrap bootstrap = nettyGroups.createClientBootstrap(sessionFactory, hf,
            BmpNettyGroups::createChannelWithEncoder, slf, remoteAddress, localAddress, CONNECT_TIMEOUT,
            KeyMapping.of(), true);
        final ChannelFuture channelFuture = bootstrap.connect(remoteAddress);
        LOG.info("BMP client {} <--> {} deployed", localAddress, remoteAddress);
        channelFuture.addListener(new BootstrapListener(bootstrap, localAddress, remoteAddress));
        return channelFuture;
    }

    ChannelFuture createServer(final InetSocketAddress localAddress) {
        requireNonNull(localAddress);
        final ServerBootstrap serverBootstrap = nettyGroups.createServerBootstrap(sessionFactory, hf, slf,
            BmpNettyGroups::createChannelWithEncoder, KeyMapping.of());
        final ChannelFuture channelFuture = serverBootstrap.bind(localAddress);
        LOG.info("Initiated BMP server at {}.", localAddress);
        return channelFuture;
    }

    @Override
    public synchronized void close() {
        if (!close) {
            close = true;
            nettyGroups.close();
        }
    }

    private class BootstrapListener implements ChannelFutureListener {
        private final Bootstrap bootstrap;
        private final InetSocketAddress remoteAddress;
        private final SocketAddress localAddress;
        private final long delay;
        private final Timer timer = new Timer();

        BootstrapListener(final Bootstrap bootstrap, final SocketAddress localAddress,
                final InetSocketAddress remoteAddress) {
            this.bootstrap = bootstrap;
            this.remoteAddress = remoteAddress;
            this.localAddress = localAddress;
            delay = INITIAL_BACKOFF;
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
                loop.schedule(() -> bootstrap.connect().addListener(this), delay, TimeUnit.MILLISECONDS);
                LOG.info("The connection try to BMP router {} failed. Next reconnection attempt in {} milliseconds.",
                        remoteAddress, delay);
            }
        }

        private void scheduleConnect() {
            if (!close) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        createClient(localAddress, remoteAddress);
                    }
                }, 5);
            }
        }
    }
}
