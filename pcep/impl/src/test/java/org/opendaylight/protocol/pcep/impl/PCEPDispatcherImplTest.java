/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPDispatcherDependencies;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.DefaultPCEPExtensionConsumerContext;
import org.opendaylight.protocol.util.InetSocketAddressUtil;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
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
        final PCEPSessionProposalFactory sessionProposal = new BasePCEPSessionProposalFactory(DEAD_TIMER, KEEP_ALIVE,
                List.of());
        final EventLoopGroup eventLoopGroup;
        if (Epoll.isAvailable()) {
            eventLoopGroup = new EpollEventLoopGroup();
        } else {
            eventLoopGroup = new NioEventLoopGroup();
        }
        final MessageRegistry msgReg = new DefaultPCEPExtensionConsumerContext().getMessageHandlerRegistry();
        dispatcher = new PCEPDispatcherImpl(msgReg,
                new DefaultPCEPSessionNegotiatorFactory(sessionProposal, 0),
                eventLoopGroup, eventLoopGroup);

        doReturn(KeyMapping.of()).when(dispatcherDependencies).getKeys();
        doReturn(null).when(dispatcherDependencies).getPeerProposal();

        final PCEPDispatcherImpl dispatcher2 = new PCEPDispatcherImpl(msgReg,
                new DefaultPCEPSessionNegotiatorFactory(sessionProposal, 0),
                eventLoopGroup, eventLoopGroup);
        disp2Spy = spy(dispatcher2);

        pccMock = new PCCMock(new DefaultPCEPSessionNegotiatorFactory(sessionProposal, 0),
                new PCEPHandlerFactory(msgReg));
    }

    @Test(timeout = 20000)
    public void testCreateClientServer() throws InterruptedException, ExecutionException {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress serverAddr = new InetSocketAddress("0.0.0.0", port);
        final InetSocketAddress clientAddr1 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);
        final InetSocketAddress clientAddr2 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);

        doReturn(serverAddr).when(dispatcherDependencies).getAddress();
        doReturn(listenerFactory).when(dispatcherDependencies).getListenerFactory();
        doReturn(new SimpleSessionListener()).when(listenerFactory).getSessionListener();
        final ChannelFuture futureChannel = dispatcher.createServer(dispatcherDependencies);
        futureChannel.sync();
        final PCEPSessionImpl session1 = pccMock.createClient(clientAddr1,
                RETRY_TIMER, CONNECT_TIMEOUT, SimpleSessionListener::new).get();

        final PCEPSessionImpl session2 = pccMock.createClient(clientAddr2,
                RETRY_TIMER, CONNECT_TIMEOUT, SimpleSessionListener::new).get();

        assertTrue(futureChannel.channel().isActive());
        assertEquals(clientAddr1.getAddress().getHostAddress(), session1.getPeerPref().getIpAddress());
        assertEquals(DEAD_TIMER, session1.getDeadTimerValue().shortValue());
        assertEquals(KEEP_ALIVE, session1.getKeepAliveTimerValue().shortValue());

        assertEquals(clientAddr2.getAddress().getHostAddress(), session2.getPeerPref().getIpAddress());
        assertEquals(DEAD_TIMER, session2.getDeadTimerValue().shortValue());
        assertEquals(KEEP_ALIVE, session2.getKeepAliveTimerValue().shortValue());

        session1.close();
        session2.close();
        assertTrue(futureChannel.channel().isActive());
    }

    @Test(timeout = 20000)
    public void testCreateDuplicateClient() throws InterruptedException, ExecutionException {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress serverAddr = new InetSocketAddress("0.0.0.0", port);
        final InetSocketAddress clientAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);

        doReturn(serverAddr).when(dispatcherDependencies).getAddress();
        doReturn(listenerFactory).when(dispatcherDependencies).getListenerFactory();
        doReturn(new SimpleSessionListener()).when(listenerFactory).getSessionListener();

        dispatcher.createServer(dispatcherDependencies).sync();
        final Future<PCEPSessionImpl> futureClient = pccMock.createClient(clientAddr, RETRY_TIMER, CONNECT_TIMEOUT,
                SimpleSessionListener::new);
        futureClient.sync();

        try (PCEPSessionImpl ignored = futureClient.get()) {
            final var cause = assertThrows(ExecutionException.class,
                () -> pccMock.createClient(clientAddr, RETRY_TIMER, CONNECT_TIMEOUT, SimpleSessionListener::new).get())
                .getCause();
            assertThat(cause, instanceOf(IllegalStateException.class));
            assertThat(cause.getMessage(), allOf(
                startsWith("A conflicting session for address "),
                endsWith(" found.")));
        }
    }

    @Test(timeout = 20000)
    public void testReconnectClient() throws InterruptedException, ExecutionException {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress clientAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);

        doReturn(new InetSocketAddress("0.0.0.0", port)).when(dispatcherDependencies).getAddress();
        doReturn(listenerFactory).when(dispatcherDependencies).getListenerFactory();
        doReturn(new SimpleSessionListener()).when(listenerFactory).getSessionListener();
        dispatcher.createServer(dispatcherDependencies).sync();
        final PCEPSessionImpl session1 = pccMock.createClient(clientAddr,
                RETRY_TIMER, CONNECT_TIMEOUT, SimpleSessionListener::new).get();

        assertEquals(clientAddr.getAddress(), session1.getRemoteAddress());
        assertEquals(DEAD_TIMER, session1.getDeadTimerValue().shortValue());
        assertEquals(KEEP_ALIVE, session1.getKeepAliveTimerValue().shortValue());
        session1.closeChannel().sync();

        final PCEPSessionImpl session2 = pccMock.createClient(clientAddr,
                RETRY_TIMER, CONNECT_TIMEOUT, SimpleSessionListener::new).get();

        assertEquals(clientAddr.getAddress(), session1.getRemoteAddress());
        assertEquals(DEAD_TIMER, session2.getDeadTimerValue().shortValue());
        assertEquals(KEEP_ALIVE, session2.getKeepAliveTimerValue().shortValue());

        session2.close();
    }

    @Test(timeout = 20000)
    public void testCustomizeBootstrap() throws InterruptedException {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress clientAddr1 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);
        final InetSocketAddress clientAddr2 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);
        final KeyMapping keys = KeyMapping.of(Map.of(
            clientAddr1.getAddress(), "CLIENT1_ADDRESS",
            clientAddr2.getAddress(), "CLIENT2_ADDRESS"));

        doReturn(new InetSocketAddress("0.0.0.0", port)).when(dispatcherDependencies).getAddress();

        final ChannelFuture futureChannel = disp2Spy.createServer(dispatcherDependencies);
        futureChannel.sync();
        verify(disp2Spy).createServerBootstrap(any(PCEPDispatcherImpl.ChannelPipelineInitializer.class));
    }

    @After
    public void tearDown() {
        dispatcher.close();
        disp2Spy.close();
    }

    private static class PCCMock {
        private final PCEPSessionNegotiatorFactory<PCEPSessionImpl> negotiatorFactory;
        private final PCEPHandlerFactory factory;
        private final EventExecutor executor;
        private final EventLoopGroup workerGroup;

        PCCMock(final PCEPSessionNegotiatorFactory<PCEPSessionImpl> negotiatorFactory,
                final PCEPHandlerFactory factory) {
            workerGroup = new NioEventLoopGroup();
            this.negotiatorFactory = requireNonNull(negotiatorFactory);
            this.factory = requireNonNull(factory);
            executor = requireNonNull(GlobalEventExecutor.INSTANCE);
        }

        Future<PCEPSessionImpl> createClient(final InetSocketAddress address, final int retryTimer,
                final int connectTimeout, final PCEPSessionListenerFactory listenerFactory) {
            return createClient(address, retryTimer, connectTimeout, (ch, promise) -> {
                ch.pipeline().addLast(factory.getDecoders());
                ch.pipeline().addLast("negotiator", negotiatorFactory.getSessionNegotiator(
                    () -> listenerFactory, ch, promise));
                ch.pipeline().addLast(factory.getEncoders());
            });
        }

        Future<PCEPSessionImpl> createClient(final InetSocketAddress address, final int retryTimer,
                final int connectTimeout, final PCEPDispatcherImpl.ChannelPipelineInitializer initializer) {
            final Bootstrap b = new Bootstrap();
            final PCEPProtocolSessionPromise<PCEPSessionImpl> p = new PCEPProtocolSessionPromise<>(executor,
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

        private static void setChannelFactory(final Bootstrap bootstrap) {
            try {
                bootstrap.channel(NioSocketChannel.class);
            } catch (final IllegalStateException ignored) {
                // Ignored
            }
        }

        private void setWorkerGroup(final Bootstrap bootstrap) {
            if (bootstrap.config().group() == null) {
                bootstrap.group(workerGroup);
            }
        }
    }

}
