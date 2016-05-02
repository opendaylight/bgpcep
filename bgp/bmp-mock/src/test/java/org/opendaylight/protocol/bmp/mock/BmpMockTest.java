/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.impl.BmpActivator;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.impl.session.DefaultBmpSessionFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BmpMockTest {

    private final BmpSessionListener sessionListener = Mockito.mock(BmpSessionListener.class);
    private int serverPort;
    private BmpExtensionProviderActivator bmpActivator;
    private BmpDispatcher bmpDispatcher;

    @Before
    public void setUp() {
        final BmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        this.bmpActivator = new BmpActivator(
                ServiceLoaderBGPExtensionProviderContext.getSingletonInstance());
        this.bmpActivator.start(ctx);
        this.bmpDispatcher = new BmpDispatcherImpl(new NioEventLoopGroup(), new NioEventLoopGroup(), ctx.getBmpMessageRegistry(),
                new DefaultBmpSessionFactory());
        this.serverPort = BmpMockDispatcherTest.getRandomPort();
    }

    @After
    public void tearDown() throws Exception {
        this.bmpActivator.stop();
        this.bmpDispatcher.close();
    }

    @Test
    public void testMain() throws Exception {
        final BmpSessionListenerFactory bmpSessionListenerFactory = new BmpSessionListenerFactory() {
            @Override
            public BmpSessionListener getSessionListener() {
                return BmpMockTest.this.sessionListener;
            }
        };
        Channel serverChannel = bmpDispatcher.createServer(new InetSocketAddress("127.0.0.1", serverPort),
                bmpSessionListenerFactory, Optional.<KeyMapping>absent()).channel();

        BmpMock.main(new String[] {"--remote_address", "127.0.0.1:" + serverPort, "--peers_count", "3", "--pre_policy_routes", "3"});
        Thread.sleep(1000);
        Mockito.verify(this.sessionListener).onSessionUp(Mockito.any(BmpSession.class));
        //1 * Initiate message + 3 * PeerUp Notification + 9 * Route Monitoring message
        Mockito.verify(this.sessionListener, Mockito.times(13)).onMessage(Mockito.any(Notification.class));

        serverChannel.close().sync();
    }

    @Test
    public void testMainInPassiveMode() throws Exception {
        final BmpSessionListenerFactory bmpSessionListenerFactory = new BmpSessionListenerFactory() {
            @Override
            public BmpSessionListener getSessionListener() {
                return BmpMockTest.this.sessionListener;
            }
        };

        // create a local server in passive mode instead
        BmpMock.main(new String[]{"--local_address", "127.0.0.1:" + serverPort, "--peers_count", "3", "--pre_policy_routes", "3", "--passive"});
        Channel serverChannel = bmpDispatcher.createClient(new InetSocketAddress("127.0.0.1", serverPort),
                bmpSessionListenerFactory, Optional.<KeyMapping>absent()).channel();

        Thread.sleep(1000);
        Mockito.verify(this.sessionListener).onSessionUp(Mockito.any(BmpSession.class));
        //1 * Initiate message + 3 * PeerUp Notification + 9 * Route Monitoring message
        Mockito.verify(this.sessionListener, Mockito.times(13)).onMessage(Mockito.any(Notification.class));

        serverChannel.close().sync();
    }
}
