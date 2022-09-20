/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.pcep.pcc.mock.PCCMockCommon.checkSessionListenerNotNull;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPDispatcherDependencies;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.DefaultPCEPExtensionConsumerContext;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yangtools.yang.common.Uint8;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PCCDispatcherImplTest {

    private static final PCEPSessionProposalFactory PROPOSAL = new BasePCEPSessionProposalFactory(Uint8.TEN,
        Uint8.valueOf(40), List.of());
    private final DefaultPCEPSessionNegotiatorFactory nf = new DefaultPCEPSessionNegotiatorFactory(PROPOSAL, 0);

    private PCCDispatcherImpl dispatcher;
    private PCEPDispatcher pcepDispatcher;
    private InetSocketAddress serverAddress;
    private InetSocketAddress clientAddress;
    private EventLoopGroup workerGroup;
    private EventLoopGroup bossGroup;
    private MessageRegistry registry;

    @Mock
    PCEPDispatcherDependencies dispatcherDependencies;

    @Before
    public void setUp() {
        workerGroup = new NioEventLoopGroup();
        bossGroup = new NioEventLoopGroup();

        registry = new DefaultPCEPExtensionConsumerContext().getMessageHandlerRegistry();

        dispatcher = new PCCDispatcherImpl(registry);
        pcepDispatcher = new PCEPDispatcherImpl(registry, nf, bossGroup, workerGroup);
        serverAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        clientAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(0);
        doReturn(KeyMapping.of()).when(dispatcherDependencies).getKeys();
        doReturn(serverAddress).when(dispatcherDependencies).getAddress();
        doReturn(null).when(dispatcherDependencies).getPeerProposal();
    }

    @After
    public void tearDown() {
        dispatcher.close();
        closeEventLoopGroups();
    }

    private void closeEventLoopGroups() {
        workerGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
        bossGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    }

    @Test(timeout = 20000)
    public void testClientReconnect() throws Exception {
        final Future<PCEPSession> futureSession = dispatcher.createClient(serverAddress, 1,
            new TestingSessionListenerFactory(), nf, KeyMapping.of(), clientAddress);
        final TestingSessionListenerFactory slf = new TestingSessionListenerFactory();
        doReturn(slf).when(dispatcherDependencies).getListenerFactory();

        final ChannelFuture futureServer = pcepDispatcher.createServer(dispatcherDependencies);
        futureServer.sync();
        final Channel channel = futureServer.channel();
        assertNotNull(futureSession.get());
        checkSessionListenerNotNull(slf, clientAddress.getHostString());
        final TestingSessionListener sl = checkSessionListenerNotNull(slf, clientAddress.getAddress().getHostAddress());
        assertNotNull(sl.getSession());
        assertTrue(sl.isUp());
        channel.close().get();
        closeEventLoopGroups();

        workerGroup = new NioEventLoopGroup();
        bossGroup = new NioEventLoopGroup();
        pcepDispatcher = new PCEPDispatcherImpl(registry, nf, bossGroup, workerGroup);

        final TestingSessionListenerFactory slf2 = new TestingSessionListenerFactory();
        doReturn(slf2).when(dispatcherDependencies).getListenerFactory();
        final ChannelFuture future2 = pcepDispatcher.createServer(dispatcherDependencies);
        future2.sync();
        final Channel channel2 = future2.channel();
        final TestingSessionListener sl2 = checkSessionListenerNotNull(slf2,
            clientAddress.getAddress().getHostAddress());
        assertNotNull(sl2.getSession());
        assertTrue(sl2.isUp());
        channel2.close();
    }
}
