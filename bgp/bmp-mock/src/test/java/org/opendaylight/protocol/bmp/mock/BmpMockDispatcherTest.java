/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import static org.opendaylight.protocol.bmp.mock.BmpMockTest.waitFutureComplete;

import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.InetSocketAddressUtil;

public class BmpMockDispatcherTest {

    private final BmpMessageRegistry registry = Mockito.mock(BmpMessageRegistry.class);
    private final BmpSessionFactory sessionFactory = Mockito.mock(BmpSessionFactory.class);
    private final BmpSessionListenerFactory slf = Mockito.mock(BmpSessionListenerFactory.class);

    @Test
    public void testCreateClient() throws InterruptedException {
        final BmpMockDispatcher dispatcher = new BmpMockDispatcher(this.registry, this.sessionFactory);
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress serverAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);
        final BmpDispatcherImpl serverDispatcher = new BmpDispatcherImpl(
                new NioEventLoopGroup(), new NioEventLoopGroup(), this.registry, this.sessionFactory);
        final ChannelFuture futureServer = serverDispatcher
                .createServer(serverAddr, this.slf, KeyMapping.getKeyMapping());
        waitFutureComplete(futureServer);
        final ChannelFuture channelFuture = dispatcher.createClient(InetSocketAddressUtil
                .getRandomLoopbackInetSocketAddress(0), serverAddr);
        waitFutureComplete(channelFuture);
        final Channel channel = channelFuture.sync().channel();

        Assert.assertTrue(channel.isActive());
        channel.close();
        serverDispatcher.close();
    }

    @Test
    public void testCreateServer() throws InterruptedException {
        final BmpMockDispatcher dispatcher = new BmpMockDispatcher(this.registry, this.sessionFactory);
        final int port = InetSocketAddressUtil.getRandomPort();
        final BmpDispatcherImpl serverDispatcher = new BmpDispatcherImpl(
                new NioEventLoopGroup(), new NioEventLoopGroup(), this.registry, this.sessionFactory);
        final ChannelFuture futureServer = dispatcher.createServer(
                new InetSocketAddress(InetAddresses.forString("0.0.0.0"), port));
        waitFutureComplete(futureServer);
        final ChannelFuture channelFuture = serverDispatcher.createClient(
                InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port), this.slf, KeyMapping.getKeyMapping());
        waitFutureComplete(channelFuture);
        final Channel channel = channelFuture.sync().channel();

        Assert.assertTrue(channel.isActive());
        channel.close();
        serverDispatcher.close();
    }

}
