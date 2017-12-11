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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkIdleState;

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.State;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapabilityBuilder;
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
        new EmbeddedChannel();
        MockitoAnnotations.initMocks(this);
        final List<BgpParameters> tlvs = Lists.newArrayList();
        this.classicOpen = new OpenBuilder().setMyAsNumber(AS_NUMBER.getValue().intValue()).setHoldTimer(HOLD_TIMER)
                .setVersion(new ProtocolVersion((short) 4)).setBgpParameters(tlvs).setBgpIdentifier(BGP_ID).build();

        final List<OptionalCapabilities> capa = Lists.newArrayList();
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                .setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi()).build())
                .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder().build()).build())
                .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(AS_NUMBER).build()).build()).build());
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capa).build());

        final ChannelFuture f = mock(ChannelFuture.class);
        doReturn(null).when(f).addListener(Mockito.any());

        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            BGPSessionImplTest.this.receivedMsgs.add((Notification) args[0]);
            return f;
        }).when(this.speakerListener).writeAndFlush(Mockito.any(Notification.class));
        doReturn(this.eventLoop).when(this.speakerListener).eventLoop();
        doReturn(true).when(this.speakerListener).isActive();
        doAnswer(invocation -> {
            final Runnable command = (Runnable) invocation.getArguments()[0];
            final long delay = (long) invocation.getArguments()[1];
            final TimeUnit unit = (TimeUnit) invocation.getArguments()[2];
            GlobalEventExecutor.INSTANCE.schedule(command, delay, unit);
            return null;
        }).when(this.eventLoop).schedule(Mockito.any(Runnable.class), Mockito.any(long.class), Mockito.any(TimeUnit.class));
        doReturn("TestingChannel").when(this.speakerListener).toString();
        doReturn(true).when(this.speakerListener).isWritable();
        doReturn(new InetSocketAddress(InetAddress.getByName(BGP_ID.getValue()), 179)).when(this.speakerListener).remoteAddress();
        doReturn(new InetSocketAddress(InetAddress.getByName(LOCAL_IP), LOCAL_PORT)).when(this.speakerListener).localAddress();
        doReturn(this.pipeline).when(this.speakerListener).pipeline();
        doReturn(this.pipeline).when(this.pipeline).replace(Mockito.any(ChannelHandler.class), Mockito.any(String.class), Mockito.any(ChannelHandler.class));
        doReturn(null).when(this.pipeline).replace(Matchers.<Class<ChannelHandler>>any(), Mockito.any(String.class), Mockito.any(ChannelHandler.class));
        doReturn(this.pipeline).when(this.pipeline).addLast(Mockito.any(ChannelHandler.class));
        final ChannelFuture futureChannel = mock(ChannelFuture.class);
        doReturn(null).when(futureChannel).addListener(Mockito.any());
        doReturn(futureChannel).when(this.speakerListener).close();
        this.listener = new SimpleSessionListener();
        this.bgpSession = new BGPSessionImpl(this.listener, this.speakerListener, this.classicOpen, this.classicOpen.getHoldTimer(), null);
        this.bgpSession.setChannelExtMsgCoder(this.classicOpen);
    }

    @Test
    public void testBGPSession() throws BGPDocumentedException {
        this.bgpSession.sessionUp();
        assertEquals(State.UP, this.bgpSession.getState());
        assertEquals(AS_NUMBER, this.bgpSession.getAsNumber());
        assertEquals(BGP_ID, this.bgpSession.getBgpId());
        assertEquals(1, this.bgpSession.getAdvertisedTableTypes().size());
        Assert.assertEquals(State.UP, this.listener.getState());

        //test stats
        final BgpSessionState state = this.bgpSession.getBgpSessionState();
        assertEquals(HOLD_TIMER, state.getHoldtimeCurrent().intValue());
        assertEquals(1, state.getKeepaliveCurrent().intValue());
        assertEquals(State.UP.name(), state.getSessionState());
        assertEquals(BGP_ID.getValue(), new String(state.getLocalPeerPreferences().getHost().getValue()));
        assertEquals(AS_NUMBER.getValue(), state.getLocalPeerPreferences().getAs().getValue());
        assertTrue(state.getLocalPeerPreferences().getBgpExtendedMessageCapability());
        assertEquals(BGP_ID.getValue(), state.getLocalPeerPreferences().getBgpId().getValue());
        assertEquals(1, state.getLocalPeerPreferences().getAdvertizedTableTypes().size());
        assertEquals(HOLD_TIMER, state.getLocalPeerPreferences().getHoldtimer().intValue());
        assertTrue(state.getLocalPeerPreferences().getFourOctetAsCapability().booleanValue());
        assertTrue(state.getLocalPeerPreferences().getBgpExtendedMessageCapability().booleanValue());
        assertTrue(state.getLocalPeerPreferences().getGrCapability());
        assertEquals(LOCAL_IP, new String(state.getRemotePeerPreferences().getHost().getValue()));
        assertEquals(LOCAL_PORT, state.getRemotePeerPreferences().getPort().getValue().intValue());
        assertEquals(0, state.getMessagesStats().getTotalMsgs().getReceived().getCount().getValue().longValue());
        assertEquals(0, state.getMessagesStats().getTotalMsgs().getSent().getCount().getValue().longValue());

        this.bgpSession.handleMessage(new UpdateBuilder().build());
        assertEquals(1, this.listener.getListMsg().size());
        assertTrue(this.listener.getListMsg().get(0) instanceof Update);
        assertEquals(1, state.getMessagesStats().getTotalMsgs().getReceived().getCount().getValue().longValue());
        assertEquals(1, state.getMessagesStats().getUpdateMsgs().getReceived().getCount().getValue().longValue());
        assertEquals(0, state.getMessagesStats().getUpdateMsgs().getSent().getCount().getValue().longValue());

        this.bgpSession.handleMessage(new KeepaliveBuilder().build());
        this.bgpSession.handleMessage(new KeepaliveBuilder().build());
        assertEquals(3, state.getMessagesStats().getTotalMsgs().getReceived().getCount().getValue().longValue());
        assertEquals(2, state.getMessagesStats().getKeepAliveMsgs().getReceived().getCount().getValue().longValue());
        assertEquals(0, state.getMessagesStats().getKeepAliveMsgs().getSent().getCount().getValue().longValue());

        this.bgpSession.close();
        assertEquals(State.IDLE, this.bgpSession.getState());
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(0);
        assertEquals(BGPError.CEASE.getCode(), error.getErrorCode().shortValue());
        assertEquals(BGPError.CEASE.getSubcode(), error.getErrorSubcode().shortValue());
        Mockito.verify(this.speakerListener).close();
        assertEquals(3, state.getMessagesStats().getTotalMsgs().getReceived().getCount().getValue().longValue());
        assertEquals(1, state.getMessagesStats().getTotalMsgs().getSent().getCount().getValue().longValue());
        assertEquals(1, state.getMessagesStats().getErrorMsgs().getErrorSentTotal().getCount().getValue().longValue());
        assertEquals(1, state.getMessagesStats().getErrorMsgs().getErrorSent().get(0).getCount().getValue().longValue());
        assertEquals(BGPError.CEASE.getCode(), state.getMessagesStats().getErrorMsgs().getErrorSent().get(0).getErrorCode().shortValue());
        assertEquals(BGPError.CEASE.getSubcode(), state.getMessagesStats().getErrorMsgs().getErrorSent().get(0).getErrorSubcode().shortValue());

        this.bgpSession.resetBgpSessionStats();
        assertEquals(0, state.getMessagesStats().getTotalMsgs().getReceived().getCount().getValue().longValue());
        assertEquals(0, state.getMessagesStats().getTotalMsgs().getSent().getCount().getValue().longValue());
        assertEquals(0, state.getMessagesStats().getErrorMsgs().getErrorSentTotal().getCount().getValue().longValue());
    }

    @Test
    public void testHandleOpenMsg() throws BGPDocumentedException {
        this.bgpSession.handleMessage(this.classicOpen);
        Assert.assertEquals(State.IDLE, this.bgpSession.getState());
        Assert.assertEquals(1, this.receivedMsgs.size());
        Assert.assertTrue(this.receivedMsgs.get(0) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(0);
        Assert.assertEquals(BGPError.FSM_ERROR.getCode(), error.getErrorCode().shortValue());
        Assert.assertEquals(BGPError.FSM_ERROR.getSubcode(), error.getErrorSubcode().shortValue());
        Mockito.verify(this.speakerListener).close();
    }

    @Test
    public void testHandleNotifyMsg() throws BGPDocumentedException {
        this.bgpSession.handleMessage(new NotifyBuilder().setErrorCode(BGPError.BAD_BGP_ID.getCode()).setErrorSubcode(BGPError.BAD_BGP_ID.getSubcode()).build());
        assertEquals(1, this.bgpSession.getBgpSessionState().getMessagesStats().getErrorMsgs().getErrorReceivedTotal().getCount().getValue().longValue());
        assertEquals(1, this.bgpSession.getBgpSessionState().getMessagesStats().getErrorMsgs().getErrorReceived().get(0).getCount().getValue().longValue());
        assertEquals(BGPError.BAD_BGP_ID.getCode(), this.bgpSession.getBgpSessionState().getMessagesStats().getErrorMsgs().getErrorReceived().get(0).getErrorCode().shortValue());
        assertEquals(BGPError.BAD_BGP_ID.getSubcode(), this.bgpSession.getBgpSessionState().getMessagesStats().getErrorMsgs().getErrorReceived().get(0).getErrorSubcode().shortValue());
        Assert.assertEquals(State.IDLE, this.bgpSession.getState());
        Mockito.verify(this.speakerListener).close();
    }

    @Test
    public void testEndOfInput() throws InterruptedException {
        this.bgpSession.sessionUp();
        Assert.assertEquals(State.UP, this.listener.getState());
        this.bgpSession.endOfInput();
        checkIdleState(this.listener);
    }

    @Test
    public void testHoldTimerExpire() throws InterruptedException {
        this.bgpSession.sessionUp();
        checkIdleState(this.listener);
        Assert.assertEquals(3, this.receivedMsgs.size());
        Assert.assertTrue(this.receivedMsgs.get(2) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(2);
        Assert.assertEquals(BGPError.HOLD_TIMER_EXPIRED.getCode(), error.getErrorCode().shortValue());
        Assert.assertEquals(BGPError.HOLD_TIMER_EXPIRED.getSubcode(), error.getErrorSubcode().shortValue());
        Mockito.verify(this.speakerListener).close();
    }

    @Test
    public void testSessionRecoveryOnException() throws Exception {
        this.listener = Mockito.spy(new SimpleExceptionSessionListener());
        this.bgpSession = Mockito.spy(new BGPSessionImpl(this.listener, this.speakerListener, this.classicOpen, this.classicOpen.getHoldTimer(), null));
        this.bgpSession.setChannelExtMsgCoder(this.classicOpen);

        Mockito.verify(this.bgpSession, Mockito.never()).handleException(Matchers.any());
        Mockito.verify(this.bgpSession, Mockito.never()).writeAndFlush(Matchers.any(Notification.class));
        Mockito.verify(this.bgpSession, Mockito.never()).terminate(Matchers.any(BGPDocumentedException.class));
        try {
            this.bgpSession.sessionUp();
            Assert.fail();  // expect the exception to be populated
        } catch (final RuntimeException ignored) {}
        Assert.assertNotEquals(State.UP, this.listener.getState());
        Mockito.verify(this.bgpSession).handleException(Matchers.any());
        Mockito.verify(this.bgpSession).writeAndFlush(Matchers.any(Notification.class));
        Mockito.verify(this.bgpSession).terminate(Matchers.any(BGPDocumentedException.class));
        Mockito.verify(this.listener).onSessionTerminated(this.bgpSession, new BGPTerminationReason(BGPError.CEASE));
    }

    private static class SimpleExceptionSessionListener  extends SimpleSessionListener {
        @Override
        public synchronized void onSessionUp(final BGPSession session) {
            super.onSessionUp(session);
            throw new RuntimeException("Mocked runtime exception.");
        }
    }
}
