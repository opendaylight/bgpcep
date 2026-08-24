/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerUp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.RouteMonitoringMessage;

@ExtendWith(MockitoExtension.class)
class BmpMockSessionTest {
    private static final InetSocketAddress REMOTE_ADDRESS = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(0);
    private static final InetSocketAddress LOCAL_ADDRESS = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(0);

    private final BmpMockSession session = new BmpMockSession(1, 1, 1);

    @Mock
    private ChannelHandlerContext context;
    @Mock
    private Channel channel;

    @Test
    void testBmpMockSession() throws InterruptedException {
        doReturn(REMOTE_ADDRESS).when(channel).remoteAddress();
        doReturn(LOCAL_ADDRESS).when(channel).localAddress();

        final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        doReturn(null).when(channel).writeAndFlush(captor.capture());

        final ChannelFuture channelFuture = mock(ChannelFuture.class);
        doReturn(null).when(channelFuture).addListener(any());
        doReturn(channelFuture).when(channel).closeFuture();
        doReturn(channelFuture).when(channel).close();
        doReturn(channel).when(context).channel();

        session.channelActive(context);

        assertEquals(REMOTE_ADDRESS.getAddress(), session.getRemoteAddress());

        final List<Object> messages = captor.getAllValues();
        assertEquals(4, messages.size());
        assertInstanceOf(InitiationMessage.class, messages.get(0));
        assertInstanceOf(PeerUp.class, messages.get(1));
        assertInstanceOf(RouteMonitoringMessage.class, messages.get(2));
        assertInstanceOf(RouteMonitoringMessage.class, messages.get(3));

        session.close();
        assertFalse(channel.isWritable());
    }
}
