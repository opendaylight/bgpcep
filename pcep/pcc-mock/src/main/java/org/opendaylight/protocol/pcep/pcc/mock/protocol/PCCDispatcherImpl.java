/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.netconf.transport.api.UnsupportedConfigurationException;
import org.opendaylight.netconf.transport.spi.NettyTransportSupport;
import org.opendaylight.netconf.transport.spi.TcpMd5Secrets;
import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCCDispatcherImpl implements PCCDispatcher, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCCDispatcherImpl.class);
    private static final ThreadFactory TF = Thread.ofPlatform().name("odl-pcep-pcc-mock-", 0).factory();

    private static final int CONNECT_TIMEOUT = 2000;

    private final PCEPHandlerFactory factory;
    private final EventLoopGroup workerGroup;

    public PCCDispatcherImpl(final @NonNull MessageRegistry registry) {
        workerGroup = NettyTransportSupport.newEventLoopGroup(0, TF);
        factory = new PCEPHandlerFactory(registry);
    }

    @Override
    public Future<PCEPSession> createClient(final InetSocketAddress remoteAddress, final long reconnectTime,
            final PCEPSessionNegotiatorFactory negotiatorFactory, final TcpMd5Secrets secrets,
            final InetSocketAddress localAddress) {
        final var b = NettyTransportSupport.newBootstrap()
            .group(workerGroup)
            .localAddress(localAddress)
            .option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
            .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
            .option(ChannelOption.RECVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1));

        if (!secrets.isEmpty()) {
            try {
                NettyTransportSupport.setTcpMd5(b, secrets);
            } catch (UnsupportedConfigurationException e) {
                return GlobalEventExecutor.INSTANCE.newFailedFuture(e);
            }
        }

        final long retryTimer = reconnectTime == -1 ? 0 : reconnectTime;
        final var promise = new PCCReconnectPromise(remoteAddress, (int) retryTimer, CONNECT_TIMEOUT, b);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                ch.pipeline()
                    .addLast(factory.getDecoders())
                    .addLast("negotiator", negotiatorFactory.getSessionNegotiator(ch, promise))
                    .addLast(factory.getEncoders())
                    .addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelInactive(final ChannelHandlerContext ctx) {
                            if (promise.isCancelled()) {
                                return;
                            }

                            if (promise.isInitialConnectFinished()) {
                                LOG.debug("Reconnecting after connection to {} was dropped", remoteAddress);
                                createClient(remoteAddress, reconnectTime, negotiatorFactory, secrets, localAddress);
                            } else {
                                LOG.debug("Connection to {} was dropped during negotiation, reattempting",
                                    remoteAddress);
                            }
                        }
                    });
            }
        });
        promise.connect();
        return promise;
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
