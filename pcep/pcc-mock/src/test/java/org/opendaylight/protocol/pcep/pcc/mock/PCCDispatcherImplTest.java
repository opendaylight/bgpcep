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
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactoryDependencies;
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.MockPcepSessionErrorPolicy;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.pojo.DefaultPCEPExtensionConsumerContext;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yangtools.yang.common.Uint8;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PCCDispatcherImplTest {
    private final DefaultPCEPSessionNegotiatorFactory nf = new DefaultPCEPSessionNegotiatorFactory(
        new PCEPTimerProposal(Uint8.TEN, Uint8.valueOf(40)), List.of(), MockPcepSessionErrorPolicy.ZERO, null);

    private PCCDispatcherImpl dispatcher;
    private PCEPDispatcherImpl pcepDispatcher;
    private InetSocketAddress serverAddress;
    private InetSocketAddress clientAddress;
    private MessageRegistry registry;

    @Mock
    PCEPSessionNegotiatorFactoryDependencies negotiatorDependencies;

    @Before
    public void setUp() {
        registry = new DefaultPCEPExtensionConsumerContext().getMessageHandlerRegistry();

        dispatcher = new PCCDispatcherImpl(registry);
        pcepDispatcher = new PCEPDispatcherImpl();
        serverAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        clientAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(0);
        doReturn(null).when(negotiatorDependencies).getPeerProposal();
    }

    @After
    public void tearDown() {
        dispatcher.close();
        pcepDispatcher.close();
    }

    @Test(timeout = 20000)
    public void testClientReconnect() throws Exception {
        final Future<PCEPSession> futureSession = dispatcher.createClient(serverAddress, 1,
            new TestingSessionListenerFactory(), nf, KeyMapping.of(), clientAddress);
        final TestingSessionListenerFactory slf = new TestingSessionListenerFactory();
        doReturn(slf).when(negotiatorDependencies).getListenerFactory();

        final ChannelFuture futureServer = pcepDispatcher.createServer(serverAddress, KeyMapping.of(), registry, nf,
            negotiatorDependencies);
        futureServer.sync();
        final Channel channel = futureServer.channel();
        assertNotNull(futureSession.get());
        checkSessionListenerNotNull(slf, clientAddress.getHostString());
        final TestingSessionListener sl = checkSessionListenerNotNull(slf, clientAddress.getAddress().getHostAddress());
        assertNotNull(sl.getSession());
        assertTrue(sl.isUp());
        channel.close().get();
        pcepDispatcher.close();

        pcepDispatcher = new PCEPDispatcherImpl();

        final TestingSessionListenerFactory slf2 = new TestingSessionListenerFactory();
        doReturn(slf2).when(negotiatorDependencies).getListenerFactory();
        final ChannelFuture future2 = pcepDispatcher.createServer(serverAddress, KeyMapping.of(), registry, nf,
            negotiatorDependencies);
        future2.sync();
        final Channel channel2 = future2.channel();
        final TestingSessionListener sl2 = checkSessionListenerNotNull(slf2,
            clientAddress.getAddress().getHostAddress());
        assertNotNull(sl2.getSession());
        assertTrue(sl2.isUp());
        channel2.close();
    }
}
