/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import com.google.common.base.Optional;
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
import org.opendaylight.tcpmd5.api.KeyMapping;

public class BmpMockDispatcherTest {

    private final BmpMessageRegistry registry = Mockito.mock(BmpMessageRegistry.class);
    private final BmpSessionFactory sessionFactory = Mockito.mock(BmpSessionFactory.class);
    private final BmpSessionListenerFactory slf = Mockito.mock(BmpSessionListenerFactory.class);

    @Test
    public void testCreateClient() throws InterruptedException {
        final BmpMockDispatcher dispatcher = new BmpMockDispatcher(this.registry, this.sessionFactory);
        final int port = getRandomPort();
        final BmpDispatcherImpl serverDispatcher = new BmpDispatcherImpl(new NioEventLoopGroup(), new NioEventLoopGroup(),
                this.registry, this.sessionFactory);
        serverDispatcher.createServer(new InetSocketAddress(InetAddresses.forString("0.0.0.0"), port), this.slf, Optional.<KeyMapping>absent());

        final ChannelFuture channelFuture = dispatcher.createClient(new InetSocketAddress(InetAddresses.forString("127.0.0.2"), 0),
                new InetSocketAddress(InetAddresses.forString("127.0.0.3"), port));
        final Channel channel = channelFuture.sync().channel();

        Assert.assertTrue(channel.isActive());
        serverDispatcher.close();
    }

    protected static int getRandomPort() {
        return (int) (Math.random() * 64000 + 1024);
    }

}
