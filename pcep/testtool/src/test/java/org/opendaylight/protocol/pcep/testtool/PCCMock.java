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
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.pcc.mock.AbstractPCCDispatcher;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;

public class PCCMock extends AbstractPCCDispatcher {

    private final PCEPSessionNegotiatorFactory negotiatorFactory;
    private final PCEPHandlerFactory factory;

    public PCCMock(final PCEPSessionNegotiatorFactory negotiatorFactory, final PCEPHandlerFactory factory,
            final DefaultPromise<PCEPSessionImpl> defaultPromise) {
        super(new NioEventLoopGroup(), new NioEventLoopGroup());
        this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
        this.factory = Preconditions.checkNotNull(factory);
    }

    public Future<PCEPSessionImpl> createClient(final InetSocketAddress address, final ReconnectStrategy strategy,
            final PCEPSessionListenerFactory listenerFactory) {
        return super.createClient(address, strategy, new ChannelPipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
                ch.pipeline().addLast(PCCMock.this.factory.getDecoders());
                ch.pipeline().addLast("negotiator", PCCMock.this.negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise, null));
                ch.pipeline().addLast(PCCMock.this.factory.getEncoders());
            }
        });
    }

    public static void main(final String[] args) throws Exception {
        final List<PCEPCapability> caps = new ArrayList<>();
        final PCEPSessionProposalFactory proposal = new BasePCEPSessionProposalFactory((short) 120, (short) 30, caps);
        final PCEPSessionNegotiatorFactory snf = new DefaultPCEPSessionNegotiatorFactory(proposal, 0);

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
