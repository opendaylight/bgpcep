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
import org.opendaylight.yangtools.yang.common.Uint8;

public class AbstractPCEPSessionTest {

    protected static final Uint8 KEEP_ALIVE = Uint8.valueOf(15);
    protected static final Uint8 DEADTIMER = Uint8.valueOf(40);

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
    protected final List<Notification<?>> msgsSend = new ArrayList<>();

    protected Open openMsg;

    protected Close closeMsg;

    protected Starttls startTlsMsg;

    protected Keepalive kaMsg;

    protected SimpleSessionListener listener;

    @SuppressWarnings("unchecked")
    @Before
    public final void setUp() {
        MockitoAnnotations.initMocks(this);
        final ChannelFuture cfuture = new DefaultChannelPromise(channel);
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            AbstractPCEPSessionTest.this.msgsSend.add((Notification<?>) args[0]);
            return cfuture;
        }).when(channel).writeAndFlush(any(Notification.class));
        doReturn(channelFuture).when(channel).closeFuture();
        doReturn(channelFuture).when(channelFuture).addListener(any(GenericFutureListener.class));
        doReturn("TestingChannel").when(channel).toString();
        doReturn(pipeline).when(channel).pipeline();
        doReturn(address).when(channel).localAddress();
        doReturn(address).when(channel).remoteAddress();
        doReturn(eventLoop).when(channel).eventLoop();
        doReturn(true).when(future).cancel(false);
        doReturn(future).when(eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn(pipeline).when(pipeline).replace(any(ChannelHandler.class), any(String.class),
            any(ChannelHandler.class));
        doReturn(pipeline).when(pipeline).addFirst(any(ChannelHandler.class));
        doReturn(true).when(channel).isActive();
        doReturn(mock(ChannelFuture.class)).when(channel).close();
        doReturn(new InetSocketAddress(ipAddress, port)).when(channel).remoteAddress();
        doReturn(new InetSocketAddress(ipAddress, port)).when(channel).localAddress();
        openMsg = new OpenBuilder()
                .setOpenMessage(new OpenMessageBuilder()
                    .setOpen(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                        .open.object.OpenBuilder()
                        .setDeadTimer(DEADTIMER)
                        .setKeepalive(KEEP_ALIVE)
                        .setSessionId(Uint8.ZERO)
                        .build())
                    .build())
                .build();
        kaMsg = new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build();
        startTlsMsg = new StarttlsBuilder().setStartTlsMessage(new StartTlsMessageBuilder().build()).build();
        closeMsg = new CloseBuilder().setCCloseMessage(new CCloseMessageBuilder()
            .setCClose(new CCloseBuilder().setReason(Uint8.valueOf(6)).build()).build()).build();

        listener = new SimpleSessionListener();
    }

}
