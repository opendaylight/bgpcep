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
    private Channel serverChannel;
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
        final BmpSessionListenerFactory bmpSessionListenerFactory = new BmpSessionListenerFactory() {
            @Override
            public BmpSessionListener getSessionListener() {
                return BmpMockTest.this.sessionListener;
            }
        };
        this.serverPort = BmpMockDispatcherTest.getRandomPort();
        this.serverChannel = this.bmpDispatcher.createServer(new InetSocketAddress("127.0.0.1", this.serverPort),
                bmpSessionListenerFactory, Optional.<KeyMapping>absent()).channel();
    }

    @After
    public void tearDown() throws Exception {
        this.serverChannel.close().sync();
        this.bmpActivator.stop();
        this.bmpDispatcher.close();
    }

    @Test
    public void testMain() throws Exception {
        BmpMock.main(new String[] {"--remote_address", "127.0.0.1:" + serverPort});
        Thread.sleep(1000);
        Mockito.verify(this.sessionListener).onSessionUp(Mockito.any(BmpSession.class));
        Mockito.verify(this.sessionListener).onMessage(Mockito.any(BmpSession.class), Mockito.any(Notification.class));
    }

}
