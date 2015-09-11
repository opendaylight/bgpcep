/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.session;

import com.google.common.base.Optional;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.impl.BmpActivator;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;

public class BmpDispatcherImplTest {

    private static final int PORT = 45678;
    private static final InetSocketAddress CLIENT_REMOTE = new InetSocketAddress("127.0.0.10", PORT);
    private static final InetSocketAddress SERVER = new InetSocketAddress("0.0.0.0", PORT);

    private BmpDispatcher dispatcher;
    private BGPActivator bgpActivator;
    private BmpActivator bmpActivator;

    @Mock
    private BmpSession mockedSession;
    @Mock
    private BmpSessionListenerFactory mockedListenerFactory;
    @Mock
    private ChannelHandler mockedChannelHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.doNothing().when(this.mockedSession).handlerRemoved(Mockito.any(ChannelHandlerContext.class));
        Mockito.doNothing().when(this.mockedSession).handlerAdded(Mockito.any(ChannelHandlerContext.class));
        Mockito.doNothing().when(this.mockedSession).channelRegistered(Mockito.any(ChannelHandlerContext.class));
        Mockito.doNothing().when(this.mockedSession).channelActive(Mockito.any(ChannelHandlerContext.class));
        Mockito.doNothing().when(this.mockedSession).channelInactive(Mockito.any(ChannelHandlerContext.class));
        Mockito.doNothing().when(this.mockedSession).channelUnregistered(Mockito.any(ChannelHandlerContext.class));
        Mockito.doNothing().when(this.mockedSession).channelReadComplete(Mockito.any(ChannelHandlerContext.class));

        this.bgpActivator = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        this.bgpActivator.start(context);
        final SimpleBmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        this.bmpActivator = new BmpActivator(context);
        this.bmpActivator.start(ctx);
        final BmpMessageRegistry messageRegistry = ctx.getBmpMessageRegistry();
        this.dispatcher = new BmpDispatcherImpl(new NioEventLoopGroup(), new NioEventLoopGroup(), messageRegistry, new BmpSessionFactory() {
            @Override
            public BmpSession getSession(final Channel channel,
                    final BmpSessionListenerFactory sessionListenerFactory) {
                return BmpDispatcherImplTest.this.mockedSession;
            }
        }, Optional.<MD5ServerChannelFactory<?>>absent());
    }

    @After
    public void tearDown() throws Exception {
        this.bgpActivator.close();
        this.bmpActivator.close();
        this.dispatcher.close();
    }

    @Test
    public void testCreateServer() throws Exception {
        final Channel serverChannel = this.dispatcher.createServer(SERVER, this.mockedListenerFactory, Optional.<KeyMapping>absent()).await().channel();
        Assert.assertTrue(serverChannel.isActive());
        final Channel clientChannel = connectTestClient();
        Assert.assertTrue(clientChannel.isActive());
        Thread.sleep(500);
        Mockito.verify(this.mockedSession, Mockito.times(1)).handlerAdded(Mockito.any(ChannelHandlerContext.class));
        Mockito.verify(this.mockedSession, Mockito.times(1)).channelRegistered(Mockito.any(ChannelHandlerContext.class));
        Mockito.verify(this.mockedSession, Mockito.times(1)).channelActive(Mockito.any(ChannelHandlerContext.class));
        clientChannel.close().get();
        serverChannel.close().get();
    }

    private Channel connectTestClient() throws InterruptedException {
        final Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup());
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                //no initialization
            }
        });
        return b.connect(CLIENT_REMOTE).sync().channel();
    }
}
