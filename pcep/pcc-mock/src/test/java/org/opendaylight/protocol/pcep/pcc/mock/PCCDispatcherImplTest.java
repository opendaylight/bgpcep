/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.opendaylight.protocol.pcep.pcc.mock.PCCMockCommon.checkSessionListenerNotNull;
import static org.opendaylight.protocol.pcep.pcc.mock.WaitForFutureSucces.waitFutureSuccess;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.util.InetSocketAddressUtil;

public class PCCDispatcherImplTest {

    private static final List<PCEPCapability> CAPS = new ArrayList<>();
    private static final PCEPSessionProposalFactory PROPOSAL = new BasePCEPSessionProposalFactory(30, 120, CAPS);
    private final DefaultPCEPSessionNegotiatorFactory nf = new DefaultPCEPSessionNegotiatorFactory(PROPOSAL, 0);
    private PCCDispatcherImpl dispatcher;
    private PCEPDispatcherImpl pcepDispatcher;

    @Before
    public void setUp() {
        this.dispatcher = new PCCDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry());
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        this.dispatcher.close();
    }

    private void initPcepDispatcher() {
        this.pcepDispatcher = new PCEPDispatcherImpl(
            ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry(),
            this.nf);
    }

    private void destroyPcepDispatcher() {
        this.pcepDispatcher.close();
    }

    @Test
    public void testClientReconnect() throws Exception {
        final InetSocketAddress serverAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final InetSocketAddress clientAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(0);

        initPcepDispatcher();
        // create the client first
        final Future<PCEPSession> futureSession = this.dispatcher
            .createClient(serverAddress, 1, new TestingSessionListenerFactory(),
                this.nf, null, clientAddress);
        waitFutureSuccess(futureSession);
        final TestingSessionListenerFactory slf = new TestingSessionListenerFactory();
        final ChannelFuture futureServer = this.pcepDispatcher.createServer(serverAddress, slf, null);
        waitFutureSuccess(futureServer);
        final Channel channel = futureServer.channel();
        Assert.assertNotNull(futureSession.get());
        final TestingSessionListener sl = checkSessionListenerNotNull(slf, clientAddress.getHostString());
        Assert.assertNotNull(sl.getSession());
        Assert.assertTrue(sl.isUp());
        // close server here
        channel.close().get();
        destroyPcepDispatcher();

        initPcepDispatcher();
        final TestingSessionListenerFactory slf2 = new TestingSessionListenerFactory();
        final ChannelFuture future2 = this.pcepDispatcher.createServer(serverAddress, slf2, null);
        waitFutureSuccess(future2);
        final Channel channel2 = future2.channel();
        final TestingSessionListener sl2 = checkSessionListenerNotNull(slf2, clientAddress.getHostString());
        Assert.assertNotNull(sl2.getSession());
        Assert.assertTrue(sl2.isUp());
        channel2.close();
        destroyPcepDispatcher();
    }
}
