/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.keepalive.message.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yangtools.yang.binding.Notification;

public class PCEPSessionImplTest {

    @Mock
    private Channel channel;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private ScheduledFuture<?> future;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private SocketAddress address;

    private final List<Notification> messagesSend = Lists.newArrayList();

    private Open openMsg;

    private Keepalive kaMsg;

    private PCEPSessionImpl session;

    private SimpleSessionListener listener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final ChannelFuture future = new DefaultChannelPromise(this.channel);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                PCEPSessionImplTest.this.messagesSend.add((Notification) args[0]);
                return future;
            }
        }).when(this.channel).writeAndFlush(any(Notification.class));
        doReturn("TestingChannel").when(this.channel).toString();
        doReturn(this.pipeline).when(this.channel).pipeline();
        doReturn(this.address).when(this.channel).localAddress();
        doReturn(this.address).when(this.channel).remoteAddress();
        doReturn(this.eventLoop).when(this.channel).eventLoop();
        doReturn(true).when(this.future).cancel(false);
        doReturn(this.future).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class), any(ChannelHandler.class));
        doReturn(true).when(this.channel).isActive();
        doReturn(mock(ChannelFuture.class)).when(this.channel).close();
        doReturn(InetSocketAddress.createUnresolved("127.0.0.1", 4189)).when(this.channel).remoteAddress();
        doReturn(InetSocketAddress.createUnresolved("127.0.0.1", 4189)).when(this.channel).localAddress();
        this.openMsg = new OpenBuilder().setOpenMessage(
                new OpenMessageBuilder().setOpen(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder().setDeadTimer(
                                (short) 40).setKeepalive((short) 10).build()).build()).build();
        this.kaMsg = new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build();

        this.listener = new SimpleSessionListener();
        this.session = new PCEPSessionImpl(this.listener, 0, this.channel, this.openMsg.getOpenMessage().getOpen(), this.openMsg.getOpenMessage().getOpen());
        this.session.sessionUp();
    }

    @After
    public void tearDown() {
        this.session.tearDown();
    }

    @Test
    public void testPcepSessionImpl() throws InterruptedException {
        Assert.assertTrue(this.listener.up);

        Assert.assertEquals(40, this.session.getDeadTimerValue().intValue());
        Assert.assertEquals(10, this.session.getKeepAliveTimerValue().intValue());
        this.session.handleMessage(this.kaMsg);
        Assert.assertEquals(1, this.session.getReceivedMsgCount().intValue());

        this.session.handleMessage(new PcreqBuilder().build());
        Assert.assertEquals(2, this.session.getReceivedMsgCount().intValue());
        Assert.assertEquals(1, this.listener.messages.size());
        Assert.assertTrue(this.listener.messages.get(0) instanceof Pcreq);

        this.session.handleMessage(new CloseBuilder().build());
        Assert.assertEquals(3, this.session.getReceivedMsgCount().intValue());
        Assert.assertEquals(1, this.listener.messages.size());
        Assert.assertTrue(this.channel.isActive());
        Mockito.verify(this.channel, Mockito.times(1)).close();
    }

    @Test
    public void testAttemptSecondSession() {
        this.session.handleMessage(this.openMsg);
        Assert.assertEquals(1, this.session.getReceivedMsgCount().intValue());
        Assert.assertEquals(1, this.messagesSend.size());
        Assert.assertTrue(this.messagesSend.get(0) instanceof Pcerr);
        final Pcerr pcErr = (Pcerr) this.messagesSend.get(0);
        final ErrorObject errorObj = pcErr.getPcerrMessage().getErrors().get(0).getErrorObject();
        Assert.assertEquals(PCEPErrors.ATTEMPT_2ND_SESSION, PCEPErrors.forValue(errorObj.getType(), errorObj.getValue()));
    }

    @Test
    public void testCapabilityNotSupported() {
        this.session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        Assert.assertEquals(2, this.messagesSend.size());
        Assert.assertTrue(this.messagesSend.get(0) instanceof Pcerr);
        final Pcerr pcErr = (Pcerr) this.messagesSend.get(0);
        final ErrorObject errorObj = pcErr.getPcerrMessage().getErrors().get(0).getErrorObject();
        Assert.assertEquals(PCEPErrors.CAPABILITY_NOT_SUPPORTED, PCEPErrors.forValue(errorObj.getType(), errorObj.getValue()));
        Assert.assertEquals(1, this.session.getUnknownMessagesTimes().size());
        // exceeded max. unknown messages count - terminate session
        Assert.assertTrue(this.messagesSend.get(1) instanceof CloseMessage);
        final CloseMessage closeMsg = (CloseMessage) this.messagesSend.get(1);
        Assert.assertEquals(TerminationReason.TooManyUnknownMsg, TerminationReason.forValue(closeMsg.getCCloseMessage().getCClose().getReason()));
        Mockito.verify(this.channel, Mockito.times(1)).close();
    }

    @Test
    public void testEndoOfInput() {
        Assert.assertTrue(this.listener.up);
        this.session.endOfInput();
        Assert.assertFalse(this.listener.up);
    }

    @Test
    public void voidTestCloseSessionWithReason() {
        this.session.close(TerminationReason.Unknown);
        Assert.assertEquals(1, this.messagesSend.size());
        Assert.assertTrue(this.messagesSend.get(0) instanceof CloseMessage);
        final CloseMessage closeMsg = (CloseMessage) this.messagesSend.get(0);
        Assert.assertEquals(TerminationReason.Unknown, TerminationReason.forValue(closeMsg.getCCloseMessage().getCClose().getReason()));
        Mockito.verify(this.channel, Mockito.times(1)).close();
    }
}
