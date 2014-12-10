/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Preconditions;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;

public class PCEPDispatcherImplTest {

    private static final int PORT = 4189;
    private static final InetSocketAddress CLIENT1_ADDRESS = new InetSocketAddress("127.0.0.10", PORT);
    private static final InetSocketAddress CLIENT2_ADDRESS = new InetSocketAddress("127.0.0.11", PORT);
    private static final short DEAD_TIMER = 120;
    private static final short KEEP_ALIVE = 30;

    private PCEPDispatcherImpl dispatcher;

    private PCCMock<Message, PCEPSessionImpl, PCEPSessionListener> pccMock;

    @Before
    public void setUp() {
        final Open open = new OpenBuilder().setSessionId((short) 0).setDeadTimer(DEAD_TIMER).setKeepalive(KEEP_ALIVE)
                .build();
        final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        final MessageRegistry msgReg = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance()
                .getMessageHandlerRegistry();
        this.dispatcher = new PCEPDispatcherImpl(msgReg, new DefaultPCEPSessionNegotiatorFactory(open, 0),
                eventLoopGroup, eventLoopGroup);
        this.pccMock = new PCCMock<>(new DefaultPCEPSessionNegotiatorFactory(open, 0),
                new PCEPHandlerFactory(msgReg), new DefaultPromise<PCEPSessionImpl>(
                        GlobalEventExecutor.INSTANCE));
    }

    @Test
    public void testCreateClientServer() throws InterruptedException, ExecutionException {
        final ChannelFuture futureChannel = this.dispatcher.createServer(new InetSocketAddress("0.0.0.0", PORT),
                new SessionListenerFactory<PCEPSessionListener>() {
                    @Override
                    public PCEPSessionListener getSessionListener() {
                        return new SimpleSessionListener();
                    }
                });
        final PCEPSessionImpl session1 = pccMock.createClient(CLIENT1_ADDRESS,
                new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 500),
                new SessionListenerFactory<PCEPSessionListener>() {
                    @Override
                    public PCEPSessionListener getSessionListener() {
                        return new SimpleSessionListener();
                    }
                }).get();

        final PCEPSessionImpl session2 = pccMock.createClient(CLIENT2_ADDRESS,
                new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 500),
                new SessionListenerFactory<PCEPSessionListener>() {
                    @Override
                    public PCEPSessionListener getSessionListener() {
                        return new SimpleSessionListener();
                    }
                }).get();

        Assert.assertTrue(futureChannel.channel().isActive());
        Assert.assertEquals(CLIENT1_ADDRESS.getAddress().getHostAddress(), session1.getPeerAddress());
        Assert.assertEquals(DEAD_TIMER, session1.getDeadTimerValue().shortValue());
        Assert.assertEquals(KEEP_ALIVE, session1.getKeepAliveTimerValue().shortValue());

        Assert.assertEquals(CLIENT2_ADDRESS.getAddress().getHostAddress(), session2.getPeerAddress());
        Assert.assertEquals(DEAD_TIMER, session2.getDeadTimerValue().shortValue());
        Assert.assertEquals(KEEP_ALIVE, session2.getKeepAliveTimerValue().shortValue());

        session1.close();
        session2.close();
        Assert.assertTrue(futureChannel.channel().isActive());

        futureChannel.channel().close();
    }

    @After
    public void tearDown() {
        this.dispatcher.close();
    }

    private static class PCCMock<M, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> extends
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

}
