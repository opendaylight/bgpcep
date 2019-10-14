/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Close;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Starttls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.StarttlsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.message.CCloseMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.keepalive.message.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.start.tls.message.StartTlsMessageBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class AbstractPCEPSessionTest {

    protected static final short KEEP_ALIVE = 15;
    protected static final short DEADTIMER = 40;

    @Mock
    protected Channel channel;

    @Mock
    private ChannelFuture channelFuture;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private ScheduledFuture<?> future;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private SocketAddress address;

    protected final String ipAddress = InetSocketAddressUtil.getRandomLoopbackIpAddress();
    protected final int port = InetSocketAddressUtil.getRandomPort();
    protected final List<Notification> msgsSend = new ArrayList<>();

    protected Open openMsg;

    protected Close closeMsg;

    protected Starttls startTlsMsg;

    protected Keepalive kaMsg;

    protected SimpleSessionListener listener;

    @Before
    public final void setUp() {
        MockitoAnnotations.initMocks(this);
        final ChannelFuture cfuture = new DefaultChannelPromise(this.channel);
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            AbstractPCEPSessionTest.this.msgsSend.add((Notification) args[0]);
            return cfuture;
        }).when(this.channel).writeAndFlush(any(Notification.class));
        doReturn(this.channelFuture).when(this.channel).closeFuture();
        doReturn(this.channelFuture).when(this.channelFuture).addListener(any(GenericFutureListener.class));
        doReturn("TestingChannel").when(this.channel).toString();
        doReturn(this.pipeline).when(this.channel).pipeline();
        doReturn(this.address).when(this.channel).localAddress();
        doReturn(this.address).when(this.channel).remoteAddress();
        doReturn(this.eventLoop).when(this.channel).eventLoop();
        doReturn(true).when(this.future).cancel(false);
        doReturn(this.future).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class),
            any(ChannelHandler.class));
        doReturn(this.pipeline).when(this.pipeline).addFirst(any(ChannelHandler.class));
        doReturn(true).when(this.channel).isActive();
        doReturn(mock(ChannelFuture.class)).when(this.channel).close();
        doReturn(new InetSocketAddress(this.ipAddress, this.port)).when(this.channel).remoteAddress();
        doReturn(new InetSocketAddress(this.ipAddress, this.port)).when(this.channel).localAddress();
        this.openMsg = new OpenBuilder()
                .setOpenMessage(new OpenMessageBuilder()
                    .setOpen(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                        .open.object.OpenBuilder()
                        .setDeadTimer(DEADTIMER)
                        .setKeepalive(KEEP_ALIVE)
                        .setSessionId((short) 0)
                        .build())
                    .build())
                .build();
        this.kaMsg = new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build();
        this.startTlsMsg = new StarttlsBuilder().setStartTlsMessage(new StartTlsMessageBuilder().build()).build();
        this.closeMsg = new CloseBuilder().setCCloseMessage(
                new CCloseMessageBuilder().setCClose(new CCloseBuilder().setReason((short) 6).build()).build()).build();


        this.listener = new SimpleSessionListener();
    }

}
