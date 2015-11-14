/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BGPSessionImplTest {

    private static final int HOLD_TIMER = 3;
    private static final AsNumber AS_NUMBER = new AsNumber(30L);
    private static final Ipv4Address BGP_ID = new Ipv4Address("1.1.1.2");
    private static final String LOCAL_IP = "1.1.1.4";
    private static final int LOCAL_PORT = 12345;

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
        this.classicOpen = new OpenBuilder().setMyAsNumber(AS_NUMBER.getValue().intValue()).setHoldTimer(HOLD_TIMER)
                .setVersion(new ProtocolVersion((short) 4)).setBgpParameters(tlvs).setBgpIdentifier(BGP_ID).build();

        final List<OptionalCapabilities> capa = Lists.newArrayList();
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                .setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi()).build())
                .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder().build()).build())
                .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(AS_NUMBER).build()).build()).build()
        );
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capa).build());

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
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Runnable command = (Runnable) invocation.getArguments()[0];
                final long delay = (long) invocation.getArguments()[1];
                final TimeUnit unit = (TimeUnit) invocation.getArguments()[2];
                GlobalEventExecutor.INSTANCE.schedule(command, delay, unit);
                return null;
            }
        }).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn("TestingChannel").when(this.speakerListener).toString();
        doReturn(new InetSocketAddress(InetAddress.getByName(BGP_ID.getValue()), 179)).when(this.speakerListener).remoteAddress();
        doReturn(new InetSocketAddress(InetAddress.getByName(LOCAL_IP), LOCAL_PORT)).when(this.speakerListener).localAddress();
        doReturn(this.pipeline).when(this.speakerListener).pipeline();
        doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class), any(ChannelHandler.class));
        doReturn(this.pipeline).when(this.pipeline).addLast(any(ChannelHandler.class));
        doReturn(mock(ChannelFuture.class)).when(this.speakerListener).close();
        this.listener = new SimpleSessionListener();
        this.bgpSession = new BGPSessionImpl(this.listener, this.speakerListener, this.classicOpen, this.classicOpen.getHoldTimer(), null);
    }

    @Test
    public void testBGPSession() {
        this.bgpSession.sessionUp();
        assertEquals(BGPSessionImpl.State.UP, this.bgpSession.getState());
        assertEquals(AS_NUMBER, this.bgpSession.getAsNumber());
        assertEquals(BGP_ID, this.bgpSession.getBgpId());
        assertEquals(1, this.bgpSession.getAdvertisedTableTypes().size());
        assertTrue(this.listener.up);
        //test stats
        final BgpSessionState state = this.bgpSession.getBgpSesionState();
        assertEquals(HOLD_TIMER, state.getHoldtimeCurrent().intValue());
        assertEquals(1, state.getKeepaliveCurrent().intValue());
        assertEquals(BGPSessionImpl.State.UP.name(), state.getSessionState());
        assertEquals(BGP_ID.getValue(), state.getPeerPreferences().getAddress());
        assertEquals(AS_NUMBER.getValue(), state.getPeerPreferences().getAs());
        assertEquals(BGP_ID.getValue(), state.getPeerPreferences().getBgpId());
        assertEquals(1, state.getPeerPreferences().getAdvertizedTableTypes().size());
        assertEquals(HOLD_TIMER, state.getPeerPreferences().getHoldtime().intValue());
        assertTrue(state.getPeerPreferences().getFourOctetAsCapability().booleanValue());
        assertTrue(state.getPeerPreferences().getGrCapability());
        assertEquals(LOCAL_IP, state.getSpeakerPreferences().getAddress());
        assertEquals(LOCAL_PORT, state.getSpeakerPreferences().getPort().intValue());
        assertEquals(0, state.getMessagesStats().getTotalMsgs().getReceived().getCount().longValue());
        assertEquals(0, state.getMessagesStats().getTotalMsgs().getSent().getCount().longValue());

        this.bgpSession.handleMessage(new UpdateBuilder().build());
        assertEquals(1, this.listener.getListMsg().size());
        assertTrue(this.listener.getListMsg().get(0) instanceof Update);
        assertEquals(1, state.getMessagesStats().getTotalMsgs().getReceived().getCount().longValue());
        assertEquals(1, state.getMessagesStats().getUpdateMsgs().getReceived().getCount().longValue());
        assertEquals(0, state.getMessagesStats().getUpdateMsgs().getSent().getCount().longValue());

        this.bgpSession.handleMessage(new KeepaliveBuilder().build());
        this.bgpSession.handleMessage(new KeepaliveBuilder().build());
        assertEquals(3, state.getMessagesStats().getTotalMsgs().getReceived().getCount().longValue());
        assertEquals(2, state.getMessagesStats().getKeepAliveMsgs().getReceived().getCount().longValue());
        assertEquals(0, state.getMessagesStats().getKeepAliveMsgs().getSent().getCount().longValue());

        this.bgpSession.close();
        assertEquals(BGPSessionImpl.State.IDLE, this.bgpSession.getState());
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(0);
        assertEquals(BGPError.CEASE.getCode(), error.getErrorCode().shortValue());
        assertEquals(BGPError.CEASE.getSubcode(), error.getErrorSubcode().shortValue());
        Mockito.verify(this.speakerListener).close();
        assertEquals(3, state.getMessagesStats().getTotalMsgs().getReceived().getCount().longValue());
        assertEquals(1, state.getMessagesStats().getTotalMsgs().getSent().getCount().longValue());
        assertEquals(1, state.getMessagesStats().getErrorMsgs().getErrorSent().getCount().longValue());
        assertEquals(BGPError.CEASE.getCode(), state.getMessagesStats().getErrorMsgs().getErrorSent().getCode().shortValue());
        assertEquals(BGPError.CEASE.getSubcode(), state.getMessagesStats().getErrorMsgs().getErrorSent().getSubCode().shortValue());

        this.bgpSession.resetSessionStats();
        assertEquals(0, state.getMessagesStats().getTotalMsgs().getReceived().getCount().longValue());
        assertEquals(0, state.getMessagesStats().getTotalMsgs().getSent().getCount().longValue());
        assertEquals(0, state.getMessagesStats().getErrorMsgs().getErrorSent().getCount().longValue());
    }

    @Test
    public void testHandleOpenMsg() {
        this.bgpSession.handleMessage(this.classicOpen);
        Assert.assertEquals(BGPSessionImpl.State.IDLE, this.bgpSession.getState());
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
        assertEquals(1, this.bgpSession.getBgpSesionState().getMessagesStats().getErrorMsgs().getErrorReceived().getCount().longValue());
        assertEquals(BGPError.BAD_BGP_ID.getCode(), this.bgpSession.getBgpSesionState().getMessagesStats().getErrorMsgs().getErrorReceived().getCode().shortValue());
        assertEquals(BGPError.BAD_BGP_ID.getSubcode(), this.bgpSession.getBgpSesionState().getMessagesStats().getErrorMsgs().getErrorReceived().getSubCode().shortValue());
        Assert.assertEquals(BGPSessionImpl.State.IDLE, this.bgpSession.getState());
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
        Assert.assertEquals(BGPSessionImpl.State.IDLE, this.bgpSession.getState());
        Assert.assertEquals(3, this.receivedMsgs.size());
        Assert.assertTrue(this.receivedMsgs.get(2) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(2);
        Assert.assertEquals(BGPError.HOLD_TIMER_EXPIRED.getCode(), error.getErrorCode().shortValue());
        Assert.assertEquals(BGPError.HOLD_TIMER_EXPIRED.getSubcode(), error.getErrorSubcode().shortValue());
        Mockito.verify(this.speakerListener).close();
    }
}
