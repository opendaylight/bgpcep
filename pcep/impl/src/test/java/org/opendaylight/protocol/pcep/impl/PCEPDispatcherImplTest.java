/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcherDependencies;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.util.InetSocketAddressUtil;

public class PCEPDispatcherImplTest {
    private static final short DEAD_TIMER = 120;
    private static final short KEEP_ALIVE = 30;
    private static final int RETRY_TIMER = 0;
    private static final int CONNECT_TIMEOUT = 500;

    private PCEPDispatcherImpl dispatcher;
    private PCEPDispatcherImpl disp2Spy;

    @Mock
    private Channel mockChannel;
    @Mock
    private PCEPDispatcherDependencies dispatcherDependencies;
    @Mock
    private PCEPSessionListenerFactory listenerFactory;

    private PCCMock pccMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final List<PCEPCapability> capList = new ArrayList<>();
        final PCEPSessionProposalFactory sessionProposal = new BasePCEPSessionProposalFactory(DEAD_TIMER, KEEP_ALIVE,
                capList);
        final EventLoopGroup eventLoopGroup;
        if (Epoll.isAvailable()) {
            eventLoopGroup = new EpollEventLoopGroup();
        } else {
            eventLoopGroup = new NioEventLoopGroup();
        }
        final MessageRegistry msgReg = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance()
                .getMessageHandlerRegistry();
        this.dispatcher = new PCEPDispatcherImpl(msgReg,
                new DefaultPCEPSessionNegotiatorFactory(sessionProposal, 0),
                eventLoopGroup, eventLoopGroup);

        doReturn(KeyMapping.getKeyMapping()).when(this.dispatcherDependencies).getKeys();
        doReturn(null).when(this.dispatcherDependencies).getPeerProposal();

        doReturn("mockChannel").when(this.mockChannel).toString();
        final PCEPDispatcherImpl dispatcher2 = new PCEPDispatcherImpl(msgReg,
                new DefaultPCEPSessionNegotiatorFactory(sessionProposal, 0),
                eventLoopGroup, eventLoopGroup);
        this.disp2Spy = Mockito.spy(dispatcher2);

        this.pccMock = new PCCMock(new DefaultPCEPSessionNegotiatorFactory(sessionProposal, 0),
                new PCEPHandlerFactory(msgReg));
    }

    @Test
    public void testCreateClientServer() throws InterruptedException, ExecutionException {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress serverAddr = new InetSocketAddress("0.0.0.0", port);
        final InetSocketAddress clientAddr1 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);
        final InetSocketAddress clientAddr2 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);

        doReturn(serverAddr).when(this.dispatcherDependencies).getAddress();
        doReturn(this.listenerFactory).when(this.dispatcherDependencies).getListenerFactory();
        doReturn(new SimpleSessionListener()).when(this.listenerFactory).getSessionListener();
        final ChannelFuture futureChannel = this.dispatcher.createServer(this.dispatcherDependencies);
        waitFutureSuccess(futureChannel);
        final PCEPSessionImpl session1 = this.pccMock.createClient(clientAddr1,
                RETRY_TIMER, CONNECT_TIMEOUT, SimpleSessionListener::new).get();

        final PCEPSessionImpl session2 = this.pccMock.createClient(clientAddr2,
                RETRY_TIMER, CONNECT_TIMEOUT, SimpleSessionListener::new).get();

        Assert.assertTrue(futureChannel.channel().isActive());
        assertEquals(clientAddr1.getAddress().getHostAddress(), session1.getPeerPref().getIpAddress());
        assertEquals(DEAD_TIMER, session1.getDeadTimerValue().shortValue());
        assertEquals(KEEP_ALIVE, session1.getKeepAliveTimerValue().shortValue());

        assertEquals(clientAddr2.getAddress().getHostAddress(), session2.getPeerPref().getIpAddress());
        assertEquals(DEAD_TIMER, session2.getDeadTimerValue().shortValue());
        assertEquals(KEEP_ALIVE, session2.getKeepAliveTimerValue().shortValue());

        session1.close();
        session2.close();
        Assert.assertTrue(futureChannel.channel().isActive());
    }

    @Test
    public void testCreateDuplicateClient() throws InterruptedException, ExecutionException {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress serverAddr = new InetSocketAddress("0.0.0.0", port);
        final InetSocketAddress clientAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);

        doReturn(serverAddr).when(this.dispatcherDependencies).getAddress();
        doReturn(this.listenerFactory).when(this.dispatcherDependencies).getListenerFactory();
        doReturn(new SimpleSessionListener()).when(this.listenerFactory).getSessionListener();

        waitFutureSuccess(this.dispatcher.createServer(this.dispatcherDependencies));
        final Future<PCEPSessionImpl> futureClient = this.pccMock.createClient(clientAddr, RETRY_TIMER, CONNECT_TIMEOUT,
                SimpleSessionListener::new);
        waitFutureSuccess(futureClient);
        final PCEPSessionImpl session1 = futureClient.get();

        try {
            this.pccMock.createClient(clientAddr, RETRY_TIMER, CONNECT_TIMEOUT,
                    SimpleSessionListener::new).get();
            Assert.fail();
        } catch (final ExecutionException e) {
            Assert.assertTrue(e.getMessage().contains("A conflicting session for address"));
        } finally {
            session1.close();
        }
    }

    @Test
    public void testReconectClient() throws InterruptedException, ExecutionException {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress clientAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);

        doReturn(new InetSocketAddress("0.0.0.0", port)).when(this.dispatcherDependencies).getAddress();
        doReturn(this.listenerFactory).when(this.dispatcherDependencies).getListenerFactory();
        doReturn(new SimpleSessionListener()).when(this.listenerFactory).getSessionListener();
        waitFutureSuccess(this.dispatcher.createServer(this.dispatcherDependencies));
        final PCEPSessionImpl session1 = this.pccMock.createClient(clientAddr,
                RETRY_TIMER, CONNECT_TIMEOUT, SimpleSessionListener::new).get();

        assertEquals(clientAddr.getAddress(), session1.getRemoteAddress());
        assertEquals(DEAD_TIMER, session1.getDeadTimerValue().shortValue());
        assertEquals(KEEP_ALIVE, session1.getKeepAliveTimerValue().shortValue());
        waitFutureSuccess(session1.closeChannel());

        final PCEPSessionImpl session2 = this.pccMock.createClient(clientAddr,
                RETRY_TIMER, CONNECT_TIMEOUT, SimpleSessionListener::new).get();

        assertEquals(clientAddr.getAddress(), session1.getRemoteAddress());
        assertEquals(DEAD_TIMER, session2.getDeadTimerValue().shortValue());
        assertEquals(KEEP_ALIVE, session2.getKeepAliveTimerValue().shortValue());

        session2.close();
    }

    @Test
    public void testCustomizeBootstrap() {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress clientAddr1 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);
        final InetSocketAddress clientAddr2 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);
        final KeyMapping keys = KeyMapping.getKeyMapping(clientAddr1.getAddress(), "CLIENT1_ADDRESS");
        keys.put(clientAddr2.getAddress(), "CLIENT2_ADDRESS".getBytes());

        doReturn(new InetSocketAddress("0.0.0.0", port)).when(this.dispatcherDependencies).getAddress();
        doReturn(this.listenerFactory).when(this.dispatcherDependencies).getListenerFactory();
        doReturn(new SimpleSessionListener()).when(this.listenerFactory).getSessionListener();

        final ChannelFuture futureChannel = this.disp2Spy.createServer(this.dispatcherDependencies);
        waitFutureSuccess(futureChannel);
        Mockito.verify(this.disp2Spy).createServerBootstrap(any(PCEPDispatcherImpl.ChannelPipelineInitializer.class));
    }

    @After
    public void tearDown() {
        this.dispatcher.close();
        this.disp2Spy.close();
    }

    private static class PCCMock {
        private final PCEPSessionNegotiatorFactory<PCEPSessionImpl> negotiatorFactory;
        private final PCEPHandlerFactory factory;
        private final EventExecutor executor;
        private final EventLoopGroup workerGroup;

        PCCMock(final PCEPSessionNegotiatorFactory<PCEPSessionImpl> negotiatorFactory,
                final PCEPHandlerFactory factory) {
            this.workerGroup = new NioEventLoopGroup();
            this.negotiatorFactory = requireNonNull(negotiatorFactory);
            this.factory = requireNonNull(factory);
            this.executor = requireNonNull(GlobalEventExecutor.INSTANCE);
        }

        Future<PCEPSessionImpl> createClient(final InetSocketAddress address, final int retryTimer,
                final int connectTimeout, final PCEPSessionListenerFactory listenerFactory) {
            return createClient(address, retryTimer, connectTimeout, (ch, promise) -> {
                ch.pipeline().addLast(this.factory.getDecoders());
                ch.pipeline().addLast("negotiator", this.negotiatorFactory.getSessionNegotiator(
                        () -> listenerFactory,
                        ch,
                        promise));
                ch.pipeline().addLast(this.factory.getEncoders());
            });
        }

        Future<PCEPSessionImpl> createClient(final InetSocketAddress address, final int retryTimer,
                final int connectTimeout, final PCEPDispatcherImpl.ChannelPipelineInitializer initializer) {
            final Bootstrap b = new Bootstrap();
            final PCEPProtocolSessionPromise<PCEPSessionImpl> p = new PCEPProtocolSessionPromise<>(this.executor,
                    address, retryTimer, connectTimeout, b);
            b.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel ch) {
                    initializer.initializeChannel(ch, p);
                }
            });

            setWorkerGroup(b);
            setChannelFactory(b);
            p.connect();
            return p;
        }

        private static void setChannelFactory(final Bootstrap b) {
            try {
                b.channel(NioSocketChannel.class);
            } catch (final IllegalStateException ignored) {
            }
        }

        private void setWorkerGroup(final Bootstrap b) {
            if (b.config().group() == null) {
                b.group(this.workerGroup);
            }
        }
    }

}