/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bmp.impl.BmpDispatcherUtil.createClientBootstrap;
import static org.opendaylight.protocol.bmp.impl.BmpDispatcherUtil.createServerBootstrap;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionConsumerContext;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = BmpDispatcher.class)
public class BmpDispatcherImpl implements BmpDispatcher, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BmpDispatcherImpl.class);

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int INITIAL_BACKOFF = 30_000;
    private static final int MAXIMUM_BACKOFF = 720_000;
    private static final long TIMEOUT = 10;

    private final BmpHandlerFactory hf;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final BmpSessionFactory sessionFactory;
    @GuardedBy("this")
    private boolean close;

    @Activate
    public BmpDispatcherImpl(
            @Reference(target = "(type=global-boss-group)") final EventLoopGroup bossGroup,
            @Reference(target = "(type=global-worker-group)") final EventLoopGroup workerGroup,
            @Reference final BmpExtensionConsumerContext ctx, @Reference final BmpSessionFactory sessionFactory) {
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup();
            this.workerGroup = new EpollEventLoopGroup();
        } else {
            this.bossGroup = requireNonNull(bossGroup);
            this.workerGroup = requireNonNull(workerGroup);
        }
        this.hf = new BmpHandlerFactory(ctx.getBmpMessageRegistry());
        this.sessionFactory = requireNonNull(sessionFactory);
    }

    @Override
    public ChannelFuture createClient(final InetSocketAddress remoteAddress, final BmpSessionListenerFactory slf,
            final KeyMapping keys) {
        final Bootstrap bootstrap = createClientBootstrap(this.sessionFactory, this.hf,
                BmpDispatcherUtil::createChannelWithDecoder, slf, remoteAddress, this.workerGroup,
                CONNECT_TIMEOUT, keys);
        final ChannelFuture channelPromise = bootstrap.connect();
        channelPromise.addListener(new BootstrapListener(bootstrap, remoteAddress, slf, keys));
        LOG.debug("Initiated BMP Client {} at {}.", channelPromise, remoteAddress);
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

    @Deactivate
    @PreDestroy
    @Override
    public synchronized void close() {
        this.close = true;
        if (Epoll.isAvailable()) {
            this.workerGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
            this.bossGroup.shutdownGracefully(0, TIMEOUT, TimeUnit.SECONDS);
        }
    }

    private class BootstrapListener implements ChannelFutureListener {
        private final Bootstrap bootstrap;
        private final InetSocketAddress remoteAddress;
        private final BmpSessionListenerFactory slf;
        private final KeyMapping keys;
        private long delay;
        private final Timer timer = new Timer();

        BootstrapListener(final Bootstrap bootstrap,
                final InetSocketAddress remoteAddress, final BmpSessionListenerFactory slf, final KeyMapping keys) {
            this.bootstrap = bootstrap;
            this.remoteAddress = remoteAddress;
            this.delay = INITIAL_BACKOFF;
            this.slf = slf;
            this.keys = keys;
        }

        @Override
        public void operationComplete(final ChannelFuture future) throws Exception {
            if (future.isCancelled()) {
                LOG.debug("Connection {} cancelled!", future);
            } else if (future.isSuccess()) {
                LOG.debug("Connection {} succeeded!", future);
                future.channel().closeFuture().addListener((ChannelFutureListener) channelFuture -> scheduleConnect());
            } else {
                if (this.delay > MAXIMUM_BACKOFF) {
                    LOG.warn("The time of maximum backoff has been exceeded. No further connection attempts with BMP "
                            + "router {}.", this.remoteAddress);
                    future.cancel(false);
                    return;
                }
                final EventLoop loop = future.channel().eventLoop();
                loop.schedule(() -> this.bootstrap.connect().addListener(this), this.delay, TimeUnit.MILLISECONDS);
                LOG.info("The connection try to BMP router {} failed. Next reconnection attempt in {} milliseconds.",
                        this.remoteAddress, this.delay);
                this.delay *= 2;
            }
        }

        private void scheduleConnect() {
            if (!BmpDispatcherImpl.this.close) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        createClient(BootstrapListener.this.remoteAddress, BootstrapListener.this.slf,
                                BootstrapListener.this.keys);
                    }
                }, 5);
            }
        }
    }
}
