/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
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

public final class PCCDispatcher extends AbstractPCCDispatcher implements PCEPDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(PCCDispatcher.class);
    private final PCEPHandlerFactory factory;
    private final MD5ChannelFactory<?> cf;
    private final PCEPSessionNegotiatorFactory snf;
    private final PCEPHandlerFactory hf;
    private final ChannelFactory<? extends ServerChannel> scf;
    private InetSocketAddress localAddress;
    private KeyMapping keys;

    public PCCDispatcher(final MessageRegistry registry, final PCEPSessionNegotiatorFactory negotiatorFactory) {
        super(new NioEventLoopGroup(), new NioEventLoopGroup());
        this.snf = Preconditions.checkNotNull(negotiatorFactory);
        this.factory = new PCEPHandlerFactory(registry);
        this.cf = new MD5NioSocketChannelFactory(DeafultKeyAccessFactory.getKeyAccessFactory());
        this.hf = new PCEPHandlerFactory(registry);
        this.scf = null;
    }


    @Override
    protected void customizeBootstrap(final Bootstrap b) {
        if (this.keys != null && !this.keys.isEmpty()) {
            if (this.cf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }

            b.channelFactory(this.cf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys);
        }
        if (this.localAddress != null) {
            b.localAddress(this.localAddress);
        }
    }

    public synchronized void createClient(final InetSocketAddress localAddress, final InetSocketAddress remoteAddress,
            final ReconnectStrategyFactory strategyFactory, final PCEPSessionListenerFactory listenerFactory,
            final PCEPSessionNegotiatorFactory negotiatorFactory, final KeyMapping keys) {
        this.localAddress = localAddress;
        this.keys = keys;
        super.createReconnectingClient(remoteAddress, strategyFactory, new AbstractPCCDispatcher.ChannelPipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
                ch.pipeline().addLast(PCCDispatcher.this.factory.getDecoders());
                ch.pipeline().addLast("negotiator",
                        negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise, null));
                ch.pipeline().addLast(PCCDispatcher.this.factory.getEncoders());
            }
        });
        this.localAddress = null;
        this.keys = null;
    }

    @Override
    public synchronized ChannelFuture createServer(final InetSocketAddress address, final PCEPSessionListenerFactory listenerFactory, final PCEPPeerProposal peerProposal) {
        return createServer(address, null, listenerFactory, peerProposal);
    }

    @Override
    public void customizeBootstrap(final ServerBootstrap b) {
        if (this.keys != null && !this.keys.isEmpty()) {
            if (this.scf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }

            LOG.debug("Adding MD5 keys {} to boostrap {}", this.keys, b);
            b.channelFactory(this.scf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys);
        }

        // Make sure we are doing round-robin processing
        b.childOption(ChannelOption.MAX_MESSAGES_PER_READ, 1);
    }

    @Override
    public synchronized ChannelFuture createServer(final InetSocketAddress address, final KeyMapping keys,
                                                   final PCEPSessionListenerFactory listenerFactory, final PCEPPeerProposal peerProposal) {
        this.keys = keys;
        final ChannelFuture ret = super.createServer(address, new ChannelPipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
                ch.pipeline().addLast(PCCDispatcher.this.hf.getDecoders());
                ch.pipeline().addLast("negotiator", PCCDispatcher.this.snf.getSessionNegotiator(listenerFactory, ch, promise, peerProposal));
                ch.pipeline().addLast(PCCDispatcher.this.hf.getEncoders());
            }
        });

        this.keys = null;
        return ret;
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
}
