/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Preconditions;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;

public class PCCMock<M, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> extends
        AbstractDispatcher<S, L> {

    private final SessionNegotiatorFactory<M, S, L> negotiatorFactory;
    private final PCEPHandlerFactory factory;

    public PCCMock(final SessionNegotiatorFactory<M, S, L> negotiatorFactory, final PCEPHandlerFactory factory,
            final DefaultPromise<PCEPSessionImpl> defaultPromise) {
        super(GlobalEventExecutor.INSTANCE, new NioEventLoopGroup(), new NioEventLoopGroup());
        this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
        this.factory = Preconditions.checkNotNull(factory);
    }

    public Future<S> createClient(final InetSocketAddress address, final ReconnectStrategy strategy,
            final SessionListenerFactory<L> listenerFactory) {
        return super.createClient(address, strategy, new PipelineInitializer<S>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<S> promise) {
                ch.pipeline().addLast(PCCMock.this.factory.getDecoders());
                ch.pipeline().addLast("negotiator",
                        PCCMock.this.negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
                ch.pipeline().addLast(PCCMock.this.factory.getEncoders());
            }
        });
    }
}
