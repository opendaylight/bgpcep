/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock.protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCCDispatcherImpl implements PCCDispatcher, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCCDispatcherImpl.class);

    private static final int CONNECT_TIMEOUT = 2000;

    private final PCEPHandlerFactory factory;
    private final EventLoopGroup workerGroup;

    public PCCDispatcherImpl(final @NonNull MessageRegistry registry) {
        workerGroup = new MultiThreadIoEventLoopGroup(
            Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory());
        factory = new PCEPHandlerFactory(registry);
    }

    @Override
    public Future<PCEPSession> createClient(final InetSocketAddress remoteAddress, final long reconnectTime,
            final PCEPSessionNegotiatorFactory negotiatorFactory, final KeyMapping keys,
            final InetSocketAddress localAddress) {
        final Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.localAddress(localAddress);
        setChannelFactory(b, keys);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.SO_REUSEADDR, true);
        b.option(ChannelOption.RECVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1));
        final long retryTimer = reconnectTime == -1 ? 0 : reconnectTime;
        final PCCReconnectPromise promise =
                new PCCReconnectPromise(remoteAddress, (int) retryTimer, CONNECT_TIMEOUT, b);
        final ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                ch.pipeline().addLast(factory.getDecoders());
                ch.pipeline().addLast("negotiator", negotiatorFactory.getSessionNegotiator(ch, promise));
                ch.pipeline().addLast(factory.getEncoders());
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelInactive(final ChannelHandlerContext ctx) {
                        if (promise.isCancelled()) {
                            return;
                        }

                        if (!promise.isInitialConnectFinished()) {
                            LOG.debug("Connection to {} was dropped during negotiation, reattempting", remoteAddress);
                            return;
                        }
                        LOG.debug("Reconnecting after connection to {} was dropped", remoteAddress);
                        createClient(remoteAddress, reconnectTime, negotiatorFactory, keys, localAddress);
                    }
                });
            }
        };
        b.handler(channelInitializer);
        promise.connect();
        return promise;
    }

    private static void setChannelFactory(final Bootstrap bootstrap, final KeyMapping keys) {
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
        if (!keys.isEmpty()) {
            if (Epoll.isAvailable()) {
                bootstrap.option(EpollChannelOption.TCP_MD5SIG, keys.asMap());
            } else {
                throw new UnsupportedOperationException(Epoll.unavailabilityCause().getCause());
            }
        }
    }

    @Override
    public void close() {
        try {
            workerGroup.shutdownGracefully().get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.warn("Failed to properly close dispatcher.", e);
        }
    }
}
