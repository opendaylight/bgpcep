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

import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefreshBuilder;
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

    private final BgpTableType ipv4tt = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private Open classicOpen;

    private BGPSessionImpl bgpSession;

    private SimpleSessionListener listener;

    private EmbeddedChannel embeddedChannel;

    private MessageCollector collector;

    @Before
    public void setUp() throws UnknownHostException {
        this.collector = new MessageCollector();
        this.embeddedChannel = Mockito.spy(new EmbeddedChannel());
        final BGPHandlerFactory hf = new BGPHandlerFactory(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry());
        this.embeddedChannel.pipeline().addLast(hf.getDecoders());
        this.embeddedChannel.pipeline().addLast(this.collector);
        Mockito.doReturn(new InetSocketAddress(BGP_ID.getValue(), LOCAL_PORT)).when(this.embeddedChannel).remoteAddress();
        Mockito.doReturn(new InetSocketAddress(LOCAL_IP, LOCAL_PORT)).when(this.embeddedChannel).localAddress();

        final List<BgpParameters> tlvs = Lists.newArrayList();
        final List<OptionalCapabilities> capa = Lists.newArrayList();
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                .setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi()).build()).build()).build()).build());
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(
                new CParametersBuilder().addAugmentation(
                        CParameters1.class, new CParameters1Builder().setGracefulRestartCapability(
                                new GracefulRestartCapabilityBuilder().build()).build()).build()).build());
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(
                new CParametersBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(AS_NUMBER).build()).build()).build());
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capa).build());
        this.classicOpen = new OpenBuilder().setMyAsNumber(AS_NUMBER.getValue().intValue()).setHoldTimer(HOLD_TIMER)
                .setVersion(new ProtocolVersion((short) 4)).setBgpParameters(tlvs).setBgpIdentifier(BGP_ID).build();

        this.listener = new SimpleSessionListener();
        this.bgpSession = new BGPSessionImpl(this.listener, this.embeddedChannel, this.classicOpen, this.classicOpen.getHoldTimer(), null);
        this.embeddedChannel.pipeline().addFirst(this.bgpSession);
    }

    @Test
    public void testBGPSession() {
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
        assertTrue(state.getPeerPreferences().getBgpExtendedMessageCapability());
        assertEquals(BGP_ID.getValue(), state.getPeerPreferences().getBgpId());
        assertEquals(1, state.getPeerPreferences().getAdvertizedTableTypes().size());
        assertEquals(HOLD_TIMER, state.getPeerPreferences().getHoldtime().intValue());
        assertTrue(state.getPeerPreferences().getFourOctetAsCapability().booleanValue());
        assertTrue(state.getPeerPreferences().getBgpExtendedMessageCapability().booleanValue());
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

        this.bgpSession.handleMessage(new RouteRefreshBuilder().build());
        assertEquals(2, this.listener.getListMsg().size());
        assertEquals(1, state.getMessagesStats().getRouteRefreshMsgs().getReceived().getCount().longValue());
        assertEquals(0, state.getMessagesStats().getRouteRefreshMsgs().getSent().getCount().longValue());

        this.bgpSession.handleMessage(new KeepaliveBuilder().build());
        this.bgpSession.handleMessage(new KeepaliveBuilder().build());
        assertEquals(4, state.getMessagesStats().getTotalMsgs().getReceived().getCount().longValue());
        assertEquals(2, state.getMessagesStats().getKeepAliveMsgs().getReceived().getCount().longValue());
        assertEquals(0, state.getMessagesStats().getKeepAliveMsgs().getSent().getCount().longValue());

        this.bgpSession.close();
        assertEquals(BGPSessionImpl.State.IDLE, this.bgpSession.getState());
        assertEquals(1, this.collector.receivedMsgs.size());
        assertTrue(this.collector.receivedMsgs.get(0) instanceof Notify);
        final Notify error = (Notify) this.collector.receivedMsgs.get(0);
        assertEquals(BGPError.CEASE.getCode(), error.getErrorCode().shortValue());
        assertEquals(BGPError.CEASE.getSubcode(), error.getErrorSubcode().shortValue());
        Mockito.verify(this.embeddedChannel).close();
        assertEquals(4, state.getMessagesStats().getTotalMsgs().getReceived().getCount().longValue());
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
        Assert.assertEquals(1, this.collector.receivedMsgs.size());
        Assert.assertTrue(this.collector.receivedMsgs.get(0) instanceof Notify);
        final Notify error = (Notify) this.collector.receivedMsgs.get(0);
        Assert.assertEquals(BGPError.FSM_ERROR.getCode(), error.getErrorCode().shortValue());
        Assert.assertEquals(BGPError.FSM_ERROR.getSubcode(), error.getErrorSubcode().shortValue());
        Mockito.verify(this.embeddedChannel).close();
    }

    @Test
    public void testHandleNotifyMsg() {
        this.bgpSession.handleMessage(new NotifyBuilder().setErrorCode(BGPError.BAD_BGP_ID.getCode()).setErrorSubcode(BGPError.BAD_BGP_ID.getSubcode()).build());
        assertEquals(1, this.bgpSession.getBgpSesionState().getMessagesStats().getErrorMsgs().getErrorReceived().getCount().longValue());
        assertEquals(BGPError.BAD_BGP_ID.getCode(), this.bgpSession.getBgpSesionState().getMessagesStats().getErrorMsgs().getErrorReceived().getCode().shortValue());
        assertEquals(BGPError.BAD_BGP_ID.getSubcode(), this.bgpSession.getBgpSesionState().getMessagesStats().getErrorMsgs().getErrorReceived().getSubCode().shortValue());
        Assert.assertEquals(BGPSessionImpl.State.IDLE, this.bgpSession.getState());
        Mockito.verify(this.embeddedChannel).close();
    }

    @Test
    public void testEndOfInput() {
        Assert.assertFalse(this.listener.down);
        this.bgpSession.endOfInput();
        Assert.assertTrue(this.listener.down);
    }

    @Test
    public void testHoldTimerExpire() throws InterruptedException {
        while (this.embeddedChannel.runScheduledPendingTasks() != -1) {
        }
        Assert.assertEquals(BGPSessionImpl.State.IDLE, this.bgpSession.getState());
        Assert.assertEquals(3, this.collector.receivedMsgs.size());
        Assert.assertTrue(this.collector.receivedMsgs.get(2) instanceof Notify);
        final Notify error = (Notify) this.collector.receivedMsgs.get(2);
        Assert.assertEquals(BGPError.HOLD_TIMER_EXPIRED.getCode(), error.getErrorCode().shortValue());
        Assert.assertEquals(BGPError.HOLD_TIMER_EXPIRED.getSubcode(), error.getErrorSubcode().shortValue());
        Mockito.verify(this.embeddedChannel).close();
    }

    private static final class MessageCollector extends ChannelOutboundHandlerAdapter {

        private final List<Notification> receivedMsgs = new ArrayList<>();

        @Override
        public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
            this.receivedMsgs.add((Notification) msg);
            super.write(ctx, msg, promise);
        }
    }
}
