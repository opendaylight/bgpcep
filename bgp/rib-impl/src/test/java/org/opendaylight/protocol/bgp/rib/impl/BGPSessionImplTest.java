/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
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
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.as4.bytes._case.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.MultiprotocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.multiprotocol._case.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BGPSessionImplTest {

    private static final int HOLD_TIMER = 3;
    private static final AsNumber AS_NUMBER = new AsNumber(30L);
    private static final Ipv4Address BGP_ID = new Ipv4Address("1.1.1.2");

    @Mock
    private EventLoop eventLoop;

    @Mock
    private Channel speakerListener;

    @Mock
    private ChannelPipeline pipeline;

    private final BgpTableType ipv4tt = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private final List<Notification> receivedMsgs = Lists.newArrayList();

    private Open classicOpen;

    private BGPSessionImpl bgpSession;

    private SimpleSessionListener listener;

    @Before
    public void setUp() throws UnknownHostException {
        MockitoAnnotations.initMocks(this);
        final List<BgpParameters> tlvs = Lists.newArrayList();
        this.classicOpen = new OpenBuilder().setMyAsNumber(AS_NUMBER.getValue().intValue()).setHoldTimer(HOLD_TIMER).setVersion(new ProtocolVersion((short) 4)).setBgpParameters(
                tlvs).setBgpIdentifier(BGP_ID).build();

        tlvs.add(new BgpParametersBuilder().setCParameters(
            new MultiprotocolCaseBuilder().setMultiprotocolCapability(
                new MultiprotocolCapabilityBuilder().setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi()).build()).build()).build());
        tlvs.add(new BgpParametersBuilder().setCParameters(new As4BytesCaseBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(
            AS_NUMBER).build()).build()).build());

        final ChannelFuture f = mock(ChannelFuture.class);
        doReturn(null).when(f).addListener(Mockito.<GenericFutureListener<? extends Future<? super Void>>>any());

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                BGPSessionImplTest.this.receivedMsgs.add((Notification) args[0]);
                return f;
            }
        }).when(this.speakerListener).writeAndFlush(any(Notification.class));
        doReturn(this.eventLoop).when(this.speakerListener).eventLoop();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final Runnable command = (Runnable) invocation.getArguments()[0];
                final long delay = (long) invocation.getArguments()[1];
                final TimeUnit unit = (TimeUnit) invocation.getArguments()[2];
                GlobalEventExecutor.INSTANCE.schedule(command, delay, unit);
                return null;
            }
        }).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn("TestingChannel").when(this.speakerListener).toString();
        doReturn(new InetSocketAddress(InetAddress.getByName(BGP_ID.getValue()), 179)).when(this.speakerListener).remoteAddress();
        doReturn(this.pipeline).when(this.speakerListener).pipeline();
        doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class), any(ChannelHandler.class));
        doReturn(mock(ChannelFuture.class)).when(this.speakerListener).close();
        this.listener = new SimpleSessionListener();
        this.bgpSession = new BGPSessionImpl(this.listener, this.speakerListener, this.classicOpen, this.classicOpen.getHoldTimer(), null);
    }

    @Test
    public void testBGPSession() {
        this.bgpSession.sessionUp();
        Assert.assertEquals(BGPSessionImpl.State.Up, this.bgpSession.getState());
        Assert.assertEquals(AS_NUMBER, this.bgpSession.getAsNumber());
        Assert.assertEquals(BGP_ID, this.bgpSession.getBgpId());
        Assert.assertEquals(1, this.bgpSession.getAdvertisedTableTypes().size());
        Assert.assertTrue(this.listener.up);

        this.bgpSession.handleMessage(new UpdateBuilder().build());
        Assert.assertEquals(1, this.listener.getListMsg().size());
        Assert.assertTrue(this.listener.getListMsg().get(0) instanceof Update);

        this.bgpSession.handleMessage(new KeepaliveBuilder().build());
        this.bgpSession.handleMessage(new KeepaliveBuilder().build());

        this.bgpSession.close();
        Assert.assertEquals(BGPSessionImpl.State.Idle, this.bgpSession.getState());
        Assert.assertEquals(1, this.receivedMsgs.size());
        Assert.assertTrue(this.receivedMsgs.get(0) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(0);
        Assert.assertEquals(BGPError.CEASE.getCode(), error.getErrorCode().shortValue());
        Mockito.verify(this.speakerListener).close();
    }

    @Test
    public void testHandleOpenMsg() {
        this.bgpSession.handleMessage(this.classicOpen);
        Assert.assertEquals(BGPSessionImpl.State.Idle, this.bgpSession.getState());
        Assert.assertEquals(1, this.receivedMsgs.size());
        Assert.assertTrue(this.receivedMsgs.get(0) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(0);
        Assert.assertEquals(BGPError.FSM_ERROR.getCode(), error.getErrorCode().shortValue());
        Assert.assertEquals(BGPError.FSM_ERROR.getSubcode(), error.getErrorSubcode().shortValue());
        Mockito.verify(this.speakerListener).close();
    }

    @Test
    public void testHandleNotifyMsg() {
        this.bgpSession.handleMessage(new NotifyBuilder().setErrorCode(BGPError.BAD_BGP_ID.getCode()).setErrorSubcode(BGPError.BAD_BGP_ID.getSubcode()).build());
        Assert.assertEquals(BGPSessionImpl.State.Idle, this.bgpSession.getState());
        Mockito.verify(this.speakerListener).close();
    }

    @Test
    public void testEndOfInput() {
        this.bgpSession.sessionUp();
        Assert.assertFalse(this.listener.down);
        this.bgpSession.endOfInput();
        Assert.assertTrue(this.listener.down);
    }

    @Test
    public void testHoldTimerExpire() throws InterruptedException {
        this.bgpSession.sessionUp();
        Thread.sleep(3500);
        Assert.assertEquals(BGPSessionImpl.State.Idle, this.bgpSession.getState());
        Assert.assertEquals(3, this.receivedMsgs.size());
        Assert.assertTrue(this.receivedMsgs.get(2) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(2);
        Assert.assertEquals(BGPError.HOLD_TIMER_EXPIRED.getCode(), error.getErrorCode().shortValue());
        Assert.assertEquals(BGPError.HOLD_TIMER_EXPIRED.getSubcode(), error.getErrorSubcode().shortValue());
        Mockito.verify(this.speakerListener).close();
    }
}
