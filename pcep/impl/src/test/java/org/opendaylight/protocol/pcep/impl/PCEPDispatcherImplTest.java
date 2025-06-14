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
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
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
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.netconf.transport.spi.TcpMd5Secrets;
import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.protocol.pcep.spi.pojo.DefaultPCEPExtensionConsumerContext;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PCEPDispatcherImplTest {
    private static final @NonNull Uint8 DEAD_TIMER = Uint8.valueOf(120);
    private static final @NonNull Uint8 KEEP_ALIVE = Uint8.valueOf(30);
    private static final int RETRY_TIMER = 0;
    private static final int CONNECT_TIMEOUT = 500;

    private PCEPDispatcherImpl dispatcher;
    private PCEPDispatcherImpl disp2Spy;

    @Mock
    private Channel mockChannel;

    private MessageRegistry msgReg;
    private PCEPSessionNegotiatorFactory negotiatorFactory;
    private PCCMock pccMock;

    @Before
    public void setUp() {

        msgReg = new DefaultPCEPExtensionConsumerContext().getMessageHandlerRegistry();
        negotiatorFactory = new DefaultPCEPSessionNegotiatorFactory(SimpleSessionListener::new,
            new PCEPTimerProposal(KEEP_ALIVE, DEAD_TIMER), List.of(), Uint16.ZERO, null);

        dispatcher = new PCEPDispatcherImpl();

        final PCEPDispatcherImpl dispatcher2 = new PCEPDispatcherImpl();
        disp2Spy = spy(dispatcher2);

        pccMock = new PCCMock(negotiatorFactory, new PCEPHandlerFactory(msgReg));
    }

    @After
    public void tearDown() {
        dispatcher.close();
        disp2Spy.close();
    }

    @Test(timeout = 20000)
    public void testCreateClientServer() throws InterruptedException, ExecutionException {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress serverAddr = new InetSocketAddress("0.0.0.0", port);
        final InetSocketAddress clientAddr1 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);
        final InetSocketAddress clientAddr2 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);

        final var futureChannel = dispatcher.createServer(serverAddr, TcpMd5Secrets.of(), msgReg,
            negotiatorFactory);
        futureChannel.sync();

        try (var session1 = (PCEPSessionImpl) pccMock.createClient(clientAddr1, RETRY_TIMER, CONNECT_TIMEOUT).get()) {
            try (var session2 = (PCEPSessionImpl) pccMock.createClient(clientAddr2, RETRY_TIMER, CONNECT_TIMEOUT)
                    .get()) {

                assertTrue(futureChannel.channel().isActive());
                assertEquals(clientAddr1.getAddress().getHostAddress(), session1.getPeerPref().getIpAddress());
                assertEquals(DEAD_TIMER.toJava(), session1.getDeadTimerValue());
                assertEquals(KEEP_ALIVE.toJava(), session1.getKeepAliveTimerValue());

                assertEquals(clientAddr2.getAddress().getHostAddress(), session2.getPeerPref().getIpAddress());
                assertEquals(DEAD_TIMER.toJava(), session2.getDeadTimerValue());
                assertEquals(KEEP_ALIVE.toJava(), session2.getKeepAliveTimerValue());
            }
        }
        assertTrue(futureChannel.channel().isActive());
    }

    @Test(timeout = 20000)
    public void testCreateDuplicateClient() throws InterruptedException, ExecutionException {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress serverAddr = new InetSocketAddress("0.0.0.0", port);
        final InetSocketAddress clientAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);

        dispatcher.createServer(serverAddr, TcpMd5Secrets.of(), msgReg, negotiatorFactory).sync();
        final var futureClient = pccMock.createClient(clientAddr, RETRY_TIMER, CONNECT_TIMEOUT);
        futureClient.sync();

        try (var ignored = futureClient.get()) {
            final var cause = assertThrows(ExecutionException.class,
                () -> pccMock.createClient(clientAddr, RETRY_TIMER, CONNECT_TIMEOUT).get())
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

        dispatcher.createServer(new InetSocketAddress("0.0.0.0", port), TcpMd5Secrets.of(), msgReg, negotiatorFactory)
            .sync();
        final var session1 = assertInstanceOf(PCEPSessionImpl.class,
            pccMock.createClient(clientAddr, RETRY_TIMER, CONNECT_TIMEOUT).get());

        assertEquals(clientAddr.getAddress(), session1.getRemoteAddress());
        assertEquals(DEAD_TIMER.toJava(), session1.getDeadTimerValue());
        assertEquals(KEEP_ALIVE.toJava(), session1.getKeepAliveTimerValue());
        session1.closeChannel().sync();

        try (var session2 = (PCEPSessionImpl) pccMock.createClient(clientAddr, RETRY_TIMER, CONNECT_TIMEOUT).get()) {

            assertEquals(clientAddr.getAddress(), session1.getRemoteAddress());
            assertEquals(DEAD_TIMER.toJava(), session2.getDeadTimerValue());
            assertEquals(KEEP_ALIVE.toJava(), session2.getKeepAliveTimerValue());
        }
    }

    @Test(timeout = 20000)
    public void testCustomizeBootstrap() throws Exception {
        final int port = InetSocketAddressUtil.getRandomPort();
        final var clientAddr1 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);
        final var clientAddr2 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);
        final var secrets = TcpMd5Secrets.of(Map.of(
            clientAddr1.getAddress(), "CLIENT1_ADDRESS",
            clientAddr2.getAddress(), "CLIENT2_ADDRESS"));

        final var futureChannel = disp2Spy.createServer(new InetSocketAddress("0.0.0.0", port), secrets, msgReg,
            negotiatorFactory).sync();
        verify(disp2Spy).createServerBootstrap(any(PCEPDispatcherImpl.ChannelPipelineInitializer.class), same(secrets));
    }

    private static class PCCMock {
        private final PCEPSessionNegotiatorFactory negotiatorFactory;
        private final PCEPHandlerFactory factory;
        private final EventExecutor executor;
        private final EventLoopGroup workerGroup;

        PCCMock(final PCEPSessionNegotiatorFactory negotiatorFactory,
                final PCEPHandlerFactory factory) {
            workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            this.negotiatorFactory = requireNonNull(negotiatorFactory);
            this.factory = requireNonNull(factory);
            executor = requireNonNull(GlobalEventExecutor.INSTANCE);
        }

        Future<PCEPSession> createClient(final InetSocketAddress address, final int retryTimer,
                final int connectTimeout) {
            return createClient(address, retryTimer, connectTimeout, (ch, promise) -> {
                ch.pipeline()
                    .addLast(factory.getDecoders())
                    .addLast("negotiator", negotiatorFactory.getSessionNegotiator(ch, promise))
                    .addLast(factory.getEncoders());
            });
        }

        Future<PCEPSession> createClient(final InetSocketAddress address, final int retryTimer,
                final int connectTimeout, final PCEPDispatcherImpl.ChannelPipelineInitializer initializer) {
            final Bootstrap b = new Bootstrap();
            final PCEPProtocolSessionPromise<PCEPSession> p = new PCEPProtocolSessionPromise<>(executor,
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
