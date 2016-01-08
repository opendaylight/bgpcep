/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.pcc.mock.api.PccDispatcher;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.tcpmd5.api.DummyKeyAccessFactory;
import org.opendaylight.tcpmd5.api.KeyAccessFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.jni.NativeKeyAccessFactory;
import org.opendaylight.tcpmd5.jni.NativeSupportUnavailableException;
import org.opendaylight.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5NioSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCCDispatcherImpl implements PccDispatcher, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PCCDispatcherImpl.class);

    private static final int RECONNECT_STRATEGY_TIMEOUT = 2000;

    private final PCEPHandlerFactory factory; 
    private final MD5ChannelFactory<?> cf;
    private final NioEventLoopGroup workerGroup;

    public PCCDispatcherImpl(final MessageRegistry registry) {
        this.workerGroup = new NioEventLoopGroup();
        this.factory = new PCEPHandlerFactory(registry);
        this.cf = new MD5NioSocketChannelFactory(DeafultKeyAccessFactory.getKeyAccessFactory());
    }

    private static final class DeafultKeyAccessFactory {
        private static final Logger LOG = LoggerFactory.getLogger(DeafultKeyAccessFactory.class);
        private static final KeyAccessFactory FACTORY;

        static {
            KeyAccessFactory factory;

            try {
                factory = NativeKeyAccessFactory.getInstance();
            } catch (final NativeSupportUnavailableException e) {
                LOG.debug("Native key access not available, using no-op fallback", e);
                factory = DummyKeyAccessFactory.getInstance();
            }

            FACTORY = factory;
        }

        private DeafultKeyAccessFactory() {
            throw new UnsupportedOperationException("Utility class should never be instantiated");
        }

        public static KeyAccessFactory getKeyAccessFactory() {
            return FACTORY;
        }
    }

    @Override
    public Future<PCEPSession> createClient(
            final InetSocketAddress remoteAddress, final long reconnectTime, final PCEPSessionListenerFactory listenerFactory,
            final PCEPSessionNegotiatorFactory negotiatorFactory, final KeyMapping keys, final InetSocketAddress localAddress) {
        final Bootstrap b = new Bootstrap();
        b.group(this.workerGroup);
        b.localAddress(localAddress);
        setChannelFactory(b, keys);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.MAX_MESSAGES_PER_READ, 1);
        final ReconnectStrategyFactory reconnectStrategy = reconnectTime == -1 ? getNeverReconnectStrategyFactory() : getTimedReconnectStrategyFactory(reconnectTime);
        final PCCReconnectPromise promise = new PCCReconnectPromise(remoteAddress, reconnectStrategy, b);
        final ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                ch.pipeline().addLast(PCCDispatcherImpl.this.factory.getDecoders());
                ch.pipeline().addLast("negotiator", negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise, null));
                ch.pipeline().addLast(PCCDispatcherImpl.this.factory.getEncoders());
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
                        if (promise.isCancelled()) {
                            return;
                        }

                        if (!promise.isInitialConnectFinished()) {
                            LOG.debug("Connection to {} was dropped during negotiation, reattempting", remoteAddress);
                            return;
                        }
                        LOG.debug("Reconnecting after connection to {} was dropped", remoteAddress);
                        PCCDispatcherImpl.this.createClient(remoteAddress, reconnectTime, listenerFactory, negotiatorFactory,
                                keys, localAddress);
                    }
                });
            }
        };
        b.handler(channelInitializer);
        promise.connect();
        return promise;
    }

    private void setChannelFactory(final Bootstrap bootstrap, final KeyMapping keys) {
        if (keys != null && !keys.isEmpty()) {
            if (this.cf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }

            LOG.debug("Adding MD5 keys {} to boostrap {}", keys, bootstrap);
            bootstrap.channelFactory(this.cf);
            bootstrap.option(MD5ChannelOption.TCP_MD5SIG, keys);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }
    }

    @Override
    public void close() {
        try {
            this.workerGroup.shutdownGracefully().get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.warn("Failed to properly close dispatcher.", e);
        }
    }

    @SuppressWarnings("deprecation")
    private static ReconnectStrategyFactory getNeverReconnectStrategyFactory() {
        return new ReconnectStrategyFactory() {

            @Override
            public ReconnectStrategy createReconnectStrategy() {
                return new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, RECONNECT_STRATEGY_TIMEOUT);
            }
        };
    }

    @SuppressWarnings("deprecation")
    private static ReconnectStrategyFactory getTimedReconnectStrategyFactory(final long reconnectTime) {
        return new ReconnectStrategyFactory() {

            @Override
            public ReconnectStrategy createReconnectStrategy() {
                return new TimedReconnectStrategy(GlobalEventExecutor.INSTANCE, RECONNECT_STRATEGY_TIMEOUT, reconnectTime, 1.0, null, null, null);
            }
        };
    }
}
