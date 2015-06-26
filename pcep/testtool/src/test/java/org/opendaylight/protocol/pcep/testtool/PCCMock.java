/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import com.google.common.base.Preconditions;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.pcc.mock.AbstractPCCDispatcher;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;

public class PCCMock extends AbstractPCCDispatcher {

    private final PCEPSessionNegotiatorFactory negotiatorFactory;
    private final PCEPHandlerFactory factory;

    public PCCMock(final PCEPSessionNegotiatorFactory negotiatorFactory, final PCEPHandlerFactory factory,
            final DefaultPromise<PCEPSessionImpl> defaultPromise) {
        super(GlobalEventExecutor.INSTANCE, new NioEventLoopGroup(), new NioEventLoopGroup());
        this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
        this.factory = Preconditions.checkNotNull(factory);
    }

    public Future<PCEPSession> createClient(final InetSocketAddress address, final ReconnectStrategy strategy,
            final PCEPSessionListenerFactory listenerFactory) {
        return super.createClient(address, strategy, new ChannelPipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<PCEPSession> promise) {
                ch.pipeline().addLast(PCCMock.this.factory.getDecoders());
                ch.pipeline().addLast("negotiator", PCCMock.this.negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
                ch.pipeline().addLast(PCCMock.this.factory.getEncoders());
            }
        });
    }

    public static void main(final String[] args) throws Exception {
        final PCEPSessionNegotiatorFactory snf = new DefaultPCEPSessionNegotiatorFactory(new OpenBuilder().setKeepalive(
                (short) 30).setDeadTimer((short) 120).setSessionId((short) 0).build(), 0);

        final PCCMock pcc = new PCCMock(snf, new PCEPHandlerFactory(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry()), new DefaultPromise<PCEPSessionImpl>(GlobalEventExecutor.INSTANCE));

        pcc.createClient(new InetSocketAddress("127.0.0.3", 12345), new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 2000),
                new PCEPSessionListenerFactory() {
                    @Override
                    public PCEPSessionListener getSessionListener() {
                        return new SimpleSessionListener();
                    }
                }).get();
    }
}
