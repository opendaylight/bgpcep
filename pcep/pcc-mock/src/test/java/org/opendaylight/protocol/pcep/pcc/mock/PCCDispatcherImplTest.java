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
import static org.opendaylight.protocol.pcep.pcc.mock.PCCMockCommon.checkSessionListenerNotNull;

import java.net.InetSocketAddress;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.netconf.transport.spi.TcpMd5Secrets;
import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.pojo.DefaultPCEPExtensionConsumerContext;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PCCDispatcherImplTest {
    private PCCDispatcherImpl dispatcher;
    private PCEPDispatcherImpl pcepDispatcher;
    private InetSocketAddress serverAddress;
    private InetSocketAddress clientAddress;
    private MessageRegistry registry;

    @Before
    public void setUp() {
        registry = new DefaultPCEPExtensionConsumerContext().getMessageHandlerRegistry();

        dispatcher = new PCCDispatcherImpl(registry);
        pcepDispatcher = new PCEPDispatcherImpl();
        serverAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        clientAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(0);
    }

    @After
    public void tearDown() {
        dispatcher.close();
        pcepDispatcher.close();
    }

    @Test(timeout = 20000)
    public void testClientReconnect() throws Exception {
        final var slf = new TestingSessionListenerFactory();
        final var nf = new DefaultPCEPSessionNegotiatorFactory(slf,
            new PCEPTimerProposal(Uint8.TEN, Uint8.valueOf(40)), List.of(), Uint16.ZERO, null);

        final var futureSession = dispatcher.createClient(serverAddress, 1, nf, TcpMd5Secrets.of(), clientAddress);

        final var futureServer = pcepDispatcher.createServer(serverAddress, TcpMd5Secrets.of(), registry, nf);
        futureServer.sync();
        final var channel = futureServer.channel();
        assertNotNull(futureSession.get());
        checkSessionListenerNotNull(slf, clientAddress.getHostString());
        final var sl = checkSessionListenerNotNull(slf, clientAddress.getAddress().getHostAddress());
        assertNotNull(sl.getSession());
        assertTrue(sl.isUp());
        channel.close().get();
        pcepDispatcher.close();

        pcepDispatcher = new PCEPDispatcherImpl();

        final var slf2 = new TestingSessionListenerFactory();
        final var future2 = pcepDispatcher.createServer(serverAddress, TcpMd5Secrets.of(), registry,
            new DefaultPCEPSessionNegotiatorFactory(slf2, new PCEPTimerProposal(Uint8.TEN, Uint8.valueOf(40)),
                List.of(), Uint16.ZERO, null));
        future2.sync();
        final var channel2 = future2.channel();
        final var sl2 = checkSessionListenerNotNull(slf2, clientAddress.getAddress().getHostAddress());
        assertNotNull(sl2.getSession());
        assertTrue(sl2.isUp());
        channel2.close();
    }
}
