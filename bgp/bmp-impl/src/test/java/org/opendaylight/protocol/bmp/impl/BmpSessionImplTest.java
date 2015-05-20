/*
 *
 *  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bmp.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.StatsReportsMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Termination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessageBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Created by cgasparini on 21.5.2015.
 */
public class BmpSessionImplTest {

    private final List<Notification> sendMessages = Lists.newArrayList();
    @Mock
    private Channel speakerListener;
    @Mock
    private EventLoop eventLoop;
    private BmpSessionImpl bmpSession;
    private Notification initiation;
    private TerminationMessage termination;
    private StatsReportsMessage statRepoMessage;
    @Mock
    private SimpleSessionListener listener;

    @Before
    public void setUp() throws UnknownHostException {
        MockitoAnnotations.initMocks(this);

        this.listener = new SimpleSessionListener();
        this.bmpSession = new BmpSessionImpl(this.listener, speakerListener);


        final ChannelFuture f = mock(ChannelFuture.class);
        doReturn(null).when(f).addListener(Mockito.<GenericFutureListener<? extends Future<? super Void>>>any());


        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                BmpSessionImplTest.this.sendMessages.add((Notification) args[0]);
                return f;
            }
        }).when(this.speakerListener).writeAndFlush(any(Notification.class));


        doReturn(this.eventLoop).when(this.speakerListener).eventLoop();

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Runnable command = (Runnable) invocation.getArguments()[0];
                final long delay = (long) invocation.getArguments()[1];
                final TimeUnit unit = (TimeUnit) invocation.getArguments()[2];
                GlobalEventExecutor.INSTANCE.schedule(command, delay, unit);
                return null;
            }
        }).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn("TestingChannel").when(this.speakerListener).toString();
        doReturn(mock(ChannelFuture.class)).when(this.speakerListener).close();

        final List<String> list = new ArrayList<String>();
        list.add("info1");
        list.add("info2");
        initiation = new InitiationMessageBuilder().setDescription("Desc").setName("Name").setStringInfo(list).build();


        final List<String> tlvStrings = new ArrayList<String>();
        list.add("info1");
        list.add("info2");
        this.termination = new TerminationMessageBuilder().setReason(Termination.Reason.forValue(1)
        ).setStringInfo(tlvStrings).build();

        this.statRepoMessage = new StatsReportsMessageBuilder().build();
    }

    @Test
    public void testHandleIinitiationMsg() {

        Assert.assertEquals(BmpSessionImpl.State.UP, this.bmpSession.getState());
        this.bmpSession.handleMessage(this.initiation);
        Assert.assertEquals(BmpSessionImpl.State.INITIATED, this.bmpSession.getState());

        Assert.assertTrue(this.listener.getListMsg().get(0) instanceof InitiationMessage);

        this.bmpSession.handleMessage(this.statRepoMessage);
        Assert.assertTrue(this.listener.getListMsg().get(1) instanceof StatsReportsMessage);

        this.bmpSession.handleMessage(this.termination);
        Mockito.verify(this.speakerListener).close();
        Assert.assertTrue(this.listener.getListMsg().get(2) instanceof TerminationMessage);
        Assert.assertEquals(BmpSessionImpl.State.IDLE, this.bmpSession.getState());

        this.bmpSession.handleMessage(this.statRepoMessage);
        assertEquals(3, this.listener.getListMsg().size());

        Mockito.verify(this.speakerListener).close();
    }

    @Test
    public void testEndOfInput() {
        this.bmpSession.sessionUp();
        Assert.assertTrue(this.listener.up);
        this.bmpSession.endOfInput();
        Assert.assertFalse(this.listener.up);
    }

}