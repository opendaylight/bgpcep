/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

public final class PCCDispatcher extends PCEPDispatcherImpl {

    private InetSocketAddress localAddress;
    private final PCEPHandlerFactory factory;

    public PCCDispatcher(final MessageRegistry registry,
            final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> negotiatorFactory) {
        super(registry, negotiatorFactory, new NioEventLoopGroup(), new NioEventLoopGroup(), null, null);
        this.factory = new PCEPHandlerFactory(registry);
    }

    @Override
    protected void customizeBootstrap(final Bootstrap b) {
        super.customizeBootstrap(b);
        if (this.localAddress != null) {
            b.localAddress(this.localAddress);
        }
    }

    public synchronized Future<PCEPSessionImpl> createClient(final InetSocketAddress localAddress, final InetSocketAddress remoteAddress,
            final ReconnectStrategy strategy, final SessionListenerFactory<PCEPSessionListener> listenerFactory,
            final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> negotiatorFactory) {
        this.localAddress = localAddress;
        final Future<PCEPSessionImpl> futureClient = super.createClient(remoteAddress, strategy, new PipelineInitializer<PCEPSessionImpl>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
                ch.pipeline().addLast(PCCDispatcher.this.factory.getDecoders());
                ch.pipeline().addLast("negotiator",
                        negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
                ch.pipeline().addLast(PCCDispatcher.this.factory.getEncoders());
            }
        });
        this.localAddress = null;
        return futureClient;
    }
}
