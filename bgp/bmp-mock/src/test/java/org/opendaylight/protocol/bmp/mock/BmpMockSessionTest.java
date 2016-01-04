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
import com.google.common.net.InetAddresses;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessage;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BmpMockSessionTest {

    private static final InetSocketAddress REMOTE_ADDRESS = new InetSocketAddress(InetAddresses.forString("127.0.0.1"), 0);
    private static final InetSocketAddress LOCAL_ADDRESS = new InetSocketAddress(InetAddresses.forString("127.0.0.2"), 0);

    private ChannelHandlerContext context;
    private EmbeddedChannel channel;
    private BmpMockSession session;

    @Before
    public void setUp() {
        this.session = new BmpMockSession(1, 1, 1);
        this.channel = Mockito.spy(new EmbeddedChannel());
        Mockito.doReturn(REMOTE_ADDRESS).when(this.channel).remoteAddress();
        Mockito.doReturn(LOCAL_ADDRESS).when(this.channel).localAddress();
        this.channel.pipeline().addLast(this.session);
        this.context = Mockito.mock(ChannelHandlerContext.class);
        Mockito.doReturn(this.channel).when(this.context).channel();
    }

    @Test
    public void testBmpMockSession() throws Exception {
        final List<Notification> messages = Lists.newArrayList();
        this.channel.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
                messages.add((Notification) msg);
            }
        });
        this.session.channelActive(this.context);

        assertEquals(REMOTE_ADDRESS.getAddress(), this.session.getRemoteAddress());
        assertTrue(messages.get(0) instanceof InitiationMessage);
        assertTrue(messages.get(1) instanceof PeerUp);
        assertTrue(messages.get(2) instanceof RouteMonitoringMessage);
        assertTrue(messages.get(3) instanceof RouteMonitoringMessage);

        this.session.close();
        assertFalse(this.channel.isWritable());
    }

}
