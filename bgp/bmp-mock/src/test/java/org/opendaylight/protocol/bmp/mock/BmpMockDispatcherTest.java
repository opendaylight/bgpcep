/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.util.CheckUtil.checkEquals;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.impl.session.DefaultBmpSessionFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.util.InetSocketAddressUtil;

public class BmpMockDispatcherTest {

    private final BmpSessionFactory sessionFactory = new DefaultBmpSessionFactory();
    private final BmpMockSessionListener sl = new BmpMockSessionListener();
    @Mock
    private BmpMessageRegistry registry;
    @Mock
    private BmpSessionListenerFactory slf;
    private BmpMockDispatcher bmpMockDispatcher;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(this.sl).when(this.slf).getSessionListener();
        this.bmpMockDispatcher = new BmpMockDispatcher(this.registry, this.sessionFactory);
    }

    @Test
    public void testCreateClient() throws Exception {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress serverAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);

        final BmpDispatcherImpl bmpDispatcher = new BmpDispatcherImpl(
                new NioEventLoopGroup(), new NioEventLoopGroup(), this.registry, this.sessionFactory);
        final ChannelFuture futureServer = bmpDispatcher
                .createServer(serverAddr, this.slf, Optional.absent());
        waitFutureSuccess(futureServer);

        final ChannelFuture channelFuture = this.bmpMockDispatcher.createClient(InetSocketAddressUtil
                .getRandomLoopbackInetSocketAddress(0), serverAddr);
        waitFutureSuccess(channelFuture);
        final Channel channel = channelFuture.sync().channel();

        assertTrue(channel.isActive());
        checkEquals(() -> assertTrue(this.sl.getStatus()));
        channel.close();
        bmpDispatcher.close();
        checkEquals(() -> assertFalse(this.sl.getStatus()));

        final BmpDispatcherImpl bmpDispatcher2 = new BmpDispatcherImpl(
                new NioEventLoopGroup(), new NioEventLoopGroup(), this.registry, this.sessionFactory);
        final ChannelFuture futureServer2 = bmpDispatcher2
                .createServer(serverAddr, this.slf, Optional.absent());
        waitFutureSuccess(futureServer2);
        checkEquals(() -> assertTrue(this.sl.getStatus()));

        bmpDispatcher2.close();
        this.bmpMockDispatcher.close();
        checkEquals(() -> assertFalse(this.sl.getStatus()));
    }

    @Test
    public void testCreateServer() throws Exception {
        final int port = InetSocketAddressUtil.getRandomPort();
        final BmpDispatcherImpl bmpDispatcher = new BmpDispatcherImpl(
                new NioEventLoopGroup(), new NioEventLoopGroup(), this.registry, this.sessionFactory);
        final ChannelFuture futureServer = this.bmpMockDispatcher.createServer(
                new InetSocketAddress(InetAddresses.forString("0.0.0.0"), port));
        waitFutureSuccess(futureServer);
        final ChannelFuture channelFuture = bmpDispatcher.createClient(
                InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port), this.slf, Optional.absent());
        waitFutureSuccess(channelFuture);
        final Channel channel = channelFuture.sync().channel();

        assertTrue(channel.isActive());
        checkEquals(() -> assertTrue(this.sl.getStatus()));
        assertTrue(futureServer.channel().isActive());
        channel.close();

        bmpDispatcher.close();
        this.bmpMockDispatcher.close();
        checkEquals(() -> assertFalse(this.sl.getStatus()));
    }

}
