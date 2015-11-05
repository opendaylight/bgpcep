/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Optional;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCCDispatcher extends PCEPDispatcherImpl {

    private InetSocketAddress localAddress;
    private final PCEPHandlerFactory factory;
    private final MD5ChannelFactory<?> cf;
    private Optional<KeyMapping> keys;

    public PCCDispatcher(final MessageRegistry registry,
            final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> negotiatorFactory) {
        super(registry, negotiatorFactory, new NioEventLoopGroup(), new NioEventLoopGroup(), null, null);
        this.factory = new PCEPHandlerFactory(registry);
        this.cf = new MD5NioSocketChannelFactory(DeafultKeyAccessFactory.getKeyAccessFactory());
    }

    @Override
    protected void customizeBootstrap(final Bootstrap b) {
        if (this.keys.isPresent()) {
            if (this.cf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }

            b.channelFactory(this.cf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys.get());
        }
        if (this.localAddress != null) {
            b.localAddress(this.localAddress);
        }
    }

    public synchronized void createClient(final InetSocketAddress localAddress, final InetSocketAddress remoteAddress,
            final ReconnectStrategyFactory strategyFactory, final SessionListenerFactory<PCEPSessionListener> listenerFactory,
            final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> negotiatorFactory, final Optional<KeyMapping> keys) {
        this.localAddress = localAddress;
        this.keys = keys;
        super.createReconnectingClient(remoteAddress, strategyFactory, new PipelineInitializer<PCEPSessionImpl>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
                ch.pipeline().addLast(PCCDispatcher.this.factory.getDecoders());
                ch.pipeline().addLast("negotiator",
                        negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
                ch.pipeline().addLast(PCCDispatcher.this.factory.getEncoders());
            }
        });
        this.localAddress = null;
        this.keys = Optional.absent();
    }

    private static final class DeafultKeyAccessFactory {
        private static final Logger LOG = LoggerFactory.getLogger(DeafultKeyAccessFactory.class);
        private static final KeyAccessFactory FACTORY;

        static {
            KeyAccessFactory factory;

            try {
                factory = NativeKeyAccessFactory.getInstance();
            } catch (NativeSupportUnavailableException e) {
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
