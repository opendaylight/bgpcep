/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.session;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.util.CheckUtil.checkEquals;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.parser.BmpActivator;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.opendaylight.protocol.concepts.KeyMapping;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class BmpDispatcherImplTest {
    private static final int PORT = 45678;
    private static final InetSocketAddress CLIENT_REMOTE = new InetSocketAddress("127.0.0.10", PORT);
    private static final InetSocketAddress SERVER = new InetSocketAddress("0.0.0.0", PORT);

    private BmpDispatcherImpl dispatcher;
    private BGPActivator bgpActivator;
    private BmpActivator bmpActivator;

    @Mock
    private BmpSession mockedSession;
    @Mock
    private BmpSessionListenerFactory mockedListenerFactory;

    @Before
    public void setUp() throws Exception {
        doNothing().when(mockedSession).handlerAdded(any(ChannelHandlerContext.class));
        doNothing().when(mockedSession).channelRegistered(any(ChannelHandlerContext.class));
        doNothing().when(mockedSession).channelActive(any(ChannelHandlerContext.class));

        bgpActivator = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        bgpActivator.start(context);
        final SimpleBmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        bmpActivator = new BmpActivator(context);
        bmpActivator.start(ctx);

        dispatcher = new BmpDispatcherImpl(new NioEventLoopGroup(), new NioEventLoopGroup(), ctx,
            (channel, sessionListenerFactory) -> BmpDispatcherImplTest.this.mockedSession);
    }

    @After
    public void tearDown() throws Exception {
        dispatcher.close();
    }

    @Test
    public void testCreateServer() throws Exception {
        final ChannelFuture futureServer = dispatcher.createServer(SERVER, mockedListenerFactory, KeyMapping.of());
        waitFutureSuccess(futureServer);
        final Channel serverChannel = futureServer.channel();
        checkEquals(() -> assertTrue(serverChannel.isActive()));

        final ChannelFuture futureClient = dispatcher.createClient(CLIENT_REMOTE, mockedListenerFactory,
            KeyMapping.of());
        waitFutureSuccess(futureClient);

        final Channel clientChannel = futureClient.channel();
        checkEquals(() -> assertTrue(clientChannel.isActive()));
        verify(mockedSession, timeout(500).times(2)).handlerAdded(any(ChannelHandlerContext.class));
        verify(mockedSession, timeout(500).times(2)).channelRegistered(any(ChannelHandlerContext.class));
        verify(mockedSession, timeout(500).times(2)).channelActive(any(ChannelHandlerContext.class));
        waitFutureSuccess(clientChannel.close());
        waitFutureSuccess(serverChannel.close());
    }
}
