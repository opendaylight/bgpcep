/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.impl.BmpNettyGroups;
import org.opendaylight.protocol.bmp.impl.session.DefaultBmpSessionFactory;
import org.opendaylight.protocol.bmp.parser.BmpActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BmpMockTest {
    private final BmpSessionListener sessionListener = mock(BmpSessionListener.class);
    private BmpExtensionProviderActivator bmpActivator;
    private BmpDispatcherImpl bmpDispatcher;

    @Before
    public void setUp() {
        final BmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        bmpActivator = new BmpActivator(
            ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst().orElseThrow());
        bmpActivator.start(ctx);
        bmpDispatcher = new BmpDispatcherImpl(new BmpNettyGroups(), ctx, new DefaultBmpSessionFactory());
    }

    @After
    public void tearDown() throws Exception {
        bmpDispatcher.close();
    }

    @Test(timeout = 20000)
    public void testMain() throws Exception {
        final InetSocketAddress serverAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final BmpSessionListenerFactory bmpSessionListenerFactory = () -> BmpMockTest.this.sessionListener;
        final ChannelFuture futureServer = bmpDispatcher.createServer(serverAddr,
                bmpSessionListenerFactory, KeyMapping.of());
        final Channel serverChannel;
        final int sessionUpWait;
        if (futureServer.isSuccess()) {
            serverChannel = futureServer.channel();
            sessionUpWait = 10;
        } else {
            serverChannel = null;
            // wait longer for the reconnection attempt
            sessionUpWait = 40;
        }

        BmpMock.main(new String[]{
            "--remote_address",
            InetSocketAddressUtil.toHostAndPort(serverAddr).toString(),
            "--peers_count", "3",
            "--pre_policy_routes",
            "3"});

        verify(sessionListener, timeout(TimeUnit.SECONDS.toMillis(sessionUpWait)))
                .onSessionUp(any(BmpSession.class));
        //1 * Initiate message + 3 * PeerUp Notification + 9 * Route Monitoring message
        verify(sessionListener, timeout(TimeUnit.SECONDS.toMillis(10))
            .times(13))
            .onMessage(any(Notification.class));

        if (serverChannel != null) {
            serverChannel.close().sync();
        }
    }

    @Test(timeout = 20000)
    public void testMainInPassiveMode() throws Exception {
        final InetSocketAddress serverAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final BmpSessionListenerFactory bmpSessionListenerFactory = () -> BmpMockTest.this.sessionListener;

        // create a local server in passive mode instead
        final List<ChannelFuture> futureServers = BmpMock.deploy(new String[]
            {"--local_address", InetSocketAddressUtil.toHostAndPort(serverAddr).toString(),
            "--peers_count", "3", "--pre_policy_routes", "3", "--passive"});
        Assert.assertEquals(1, futureServers.size());
        futureServers.get(0).sync();
        final ChannelFuture futureClient = bmpDispatcher.createClient(serverAddr,
                bmpSessionListenerFactory, KeyMapping.of());
        futureClient.sync();
        final Channel serverChannel;
        final int sessionUpWait;
        if (futureClient.isSuccess()) {
            serverChannel = futureClient.channel();
            sessionUpWait = 10;
        } else {
            serverChannel = null;
            // wait longer for the reconnection attempt
            sessionUpWait = 40;
        }

        verify(sessionListener, timeout(TimeUnit.SECONDS.toMillis(sessionUpWait)))
            .onSessionUp(any(BmpSession.class));
        //1 * Initiate message + 3 * PeerUp Notification + 9 * Route Monitoring message
        verify(sessionListener, timeout(TimeUnit.SECONDS.toMillis(10))
            .times(13))
            .onMessage(any(Notification.class));

        if (serverChannel != null) {
            serverChannel.close().sync();
        }
    }
}
