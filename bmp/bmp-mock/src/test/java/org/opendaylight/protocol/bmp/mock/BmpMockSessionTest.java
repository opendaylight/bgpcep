/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerUp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.RouteMonitoringMessage;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BmpMockSessionTest {

    private static final InetSocketAddress REMOTE_ADDRESS = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(0);
    private static final InetSocketAddress LOCAL_ADDRESS = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(0);

    private ChannelHandlerContext context;
    private Channel channel;
    private BmpMockSession session;
    private List<Notification> messages;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.messages = Lists.newArrayList();
        this.session = new BmpMockSession(1, 1, 1);
        this.channel = Mockito.mock(Channel.class);
        Mockito.doReturn(REMOTE_ADDRESS).when(this.channel).remoteAddress();
        Mockito.doReturn(LOCAL_ADDRESS).when(this.channel).localAddress();
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.doReturn(pipeline).when(this.channel).pipeline();
        Mockito.doAnswer(invocation -> {
            messages.add((Notification) invocation.getArguments()[0]);
            return null;
        }).when(this.channel).writeAndFlush(Mockito.any());
        final ChannelFuture channelFuture = Mockito.mock(ChannelFuture.class);
        Mockito.doReturn(null).when(channelFuture).addListener(Mockito.any());
        Mockito.doReturn(channelFuture).when(this.channel).closeFuture();
        Mockito.doReturn(channelFuture).when(this.channel).close();
        this.context = Mockito.mock(ChannelHandlerContext.class);
        Mockito.doReturn(this.channel).when(this.context).channel();
    }

    @Test
    public void testBmpMockSession() throws Exception {
        this.session.channelActive(this.context);

        assertEquals(REMOTE_ADDRESS.getAddress(), this.session.getRemoteAddress());
        assertTrue(this.messages.get(0) instanceof InitiationMessage);
        assertTrue(this.messages.get(1) instanceof PeerUp);
        assertTrue(this.messages.get(2) instanceof RouteMonitoringMessage);
        assertTrue(this.messages.get(3) instanceof RouteMonitoringMessage);

        this.session.close();
        assertFalse(this.channel.isWritable());
    }

}
