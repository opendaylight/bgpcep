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
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;

public class PCCDispatcherImplTest {

    private static final List<PCEPCapability> CAPS = new ArrayList<>();
    private static final PCEPSessionProposalFactory PROPOSAL = new BasePCEPSessionProposalFactory(30, 120, CAPS);
    private final DefaultPCEPSessionNegotiatorFactory nf = new DefaultPCEPSessionNegotiatorFactory(PROPOSAL, 0);
    private PCCDispatcherImpl dispatcher;
    private PCEPDispatcher pcepDispatcher;
    private InetSocketAddress serverAddress;
    private InetSocketAddress clientAddress;
    private EventLoopGroup workerGroup;
    private EventLoopGroup bossGroup;

    @Before
    public void setUp() {
        this.workerGroup = new NioEventLoopGroup();
        this.bossGroup = new NioEventLoopGroup();
        this.dispatcher = new PCCDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry());
        this.pcepDispatcher = new PCEPDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry(),
            this.nf, this.bossGroup, this.workerGroup);
        this.serverAddress = new InetSocketAddress("127.0.5.0", 4561);
        this.clientAddress = new InetSocketAddress("127.0.4.0", 4562);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        this.dispatcher.close();
        this.workerGroup.shutdownGracefully().get();
        this.bossGroup.shutdownGracefully().get();
    }

    @Test
    public void testClientReconnect() throws Exception {
        final Future<PCEPSession> futureSession = this.dispatcher.createClient(this.serverAddress, 1, new TestingSessionListenerFactory(),
            this.nf, null, this.clientAddress);
        waitFutureSuccess(futureSession);
        final TestingSessionListenerFactory slf = new TestingSessionListenerFactory();
        final ChannelFuture future = this.pcepDispatcher.createServer(this.serverAddress, slf, null);
        waitFutureSuccess(future);
        final Channel channel = future.channel();
        Assert.assertNotNull(futureSession.get());
        checkSessionListenerNotNull(slf, "127.0.4.0");
        final TestingSessionListener sl = slf.getSessionListenerByRemoteAddress(this.clientAddress.getAddress());
        Assert.assertNotNull(sl);
        Assert.assertNotNull(sl.getSession());
        Assert.assertTrue(sl.isUp());

        channel.close().get();
        this.workerGroup.shutdownGracefully().get();
        this.bossGroup.shutdownGracefully().get();

        this.workerGroup = new NioEventLoopGroup();
        this.bossGroup = new NioEventLoopGroup();
        this.pcepDispatcher = new PCEPDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry(),
            this.nf, this.bossGroup, this.workerGroup);

        final TestingSessionListenerFactory slf2 = new TestingSessionListenerFactory();
        final ChannelFuture future2 = this.pcepDispatcher.createServer(this.serverAddress, slf2, null);
        waitFutureSuccess(future2);
        final Channel channel2 = future2.channel();
        Thread.sleep(1500);
        final TestingSessionListener sl2 = slf2.getSessionListenerByRemoteAddress(this.clientAddress.getAddress());
        Assert.assertNotNull(sl2);
        Assert.assertNotNull(sl2.getSession());
        Assert.assertTrue(sl2.isUp());
    }
}
