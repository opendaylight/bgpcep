/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.pcep.pcc.mock.PCCMockCommon.checkSessionListenerNotNull;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPDispatcherDependencies;
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
    private static final PCEPSessionProposalFactory PROPOSAL
            = new BasePCEPSessionProposalFactory(10, 40, CAPS);
    private final DefaultPCEPSessionNegotiatorFactory nf
            = new DefaultPCEPSessionNegotiatorFactory(PROPOSAL, 0);
    private PCCDispatcherImpl dispatcher;
    private PCEPDispatcher pcepDispatcher;
    private InetSocketAddress serverAddress;
    private InetSocketAddress clientAddress;
    private EventLoopGroup workerGroup;
    private EventLoopGroup bossGroup;

    @Mock
    PCEPDispatcherDependencies dispatcherDependencies;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.workerGroup = new NioEventLoopGroup();
        this.bossGroup = new NioEventLoopGroup();
        this.dispatcher = new PCCDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance()
                .getMessageHandlerRegistry());
        this.pcepDispatcher = new PCEPDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance()
                .getMessageHandlerRegistry(),
            this.nf, this.bossGroup, this.workerGroup);
        this.serverAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        this.clientAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(0);
        doReturn(KeyMapping.getKeyMapping()).when(this.dispatcherDependencies).getKeys();
        doReturn(this.serverAddress).when(this.dispatcherDependencies).getAddress();
        doReturn(null).when(this.dispatcherDependencies).getPeerProposal();
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        this.dispatcher.close();
        closeEventLoopGroups();
    }

    private void closeEventLoopGroups() {
        this.workerGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
        this.bossGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    }

    @Test
    public void testClientReconnect() throws Exception {
        final Future<PCEPSession> futureSession = this.dispatcher
                .createClient(this.serverAddress, 1, new TestingSessionListenerFactory(), this.nf,
                        KeyMapping.getKeyMapping(), this.clientAddress);
        final TestingSessionListenerFactory slf = new TestingSessionListenerFactory();
        doReturn(slf).when(this.dispatcherDependencies).getListenerFactory();

        final ChannelFuture futureServer = this.pcepDispatcher.createServer(this.dispatcherDependencies);
        waitFutureSuccess(futureServer);
        final Channel channel = futureServer.channel();
        Assert.assertNotNull(futureSession.get());
        checkSessionListenerNotNull(slf, this.clientAddress.getHostString());
        final TestingSessionListener sl
                = checkSessionListenerNotNull(slf, this.clientAddress.getAddress().getHostAddress());
        Assert.assertNotNull(sl.getSession());
        Assert.assertTrue(sl.isUp());
        channel.close().get();
        closeEventLoopGroups();

        this.workerGroup = new NioEventLoopGroup();
        this.bossGroup = new NioEventLoopGroup();
        this.pcepDispatcher = new PCEPDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance()
                .getMessageHandlerRegistry(),
            this.nf, this.bossGroup, this.workerGroup);

        final TestingSessionListenerFactory slf2 = new TestingSessionListenerFactory();
        doReturn(slf2).when(this.dispatcherDependencies).getListenerFactory();
        final ChannelFuture future2 = this.pcepDispatcher.createServer(this.dispatcherDependencies);
        waitFutureSuccess(future2);
        final Channel channel2 = future2.channel();
        final TestingSessionListener sl2
                = checkSessionListenerNotNull(slf2, this.clientAddress.getAddress().getHostAddress());
        Assert.assertNotNull(sl2.getSession());
        Assert.assertTrue(sl2.isUp());
        channel2.close();
    }
}
