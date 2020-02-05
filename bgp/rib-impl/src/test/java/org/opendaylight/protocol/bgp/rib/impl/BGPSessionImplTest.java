/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkIdleState;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class BGPSessionImplTest {
    private static final Uint16 HOLD_TIMER = Uint16.valueOf(3);
    private static final AsNumber AS_NUMBER = new AsNumber(Uint32.valueOf(30));
    private static final Ipv4AddressNoZone BGP_ID = new Ipv4AddressNoZone("1.1.1.2");
    private static final String LOCAL_IP = "1.1.1.4";
    private static final int LOCAL_PORT = 12345;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private Channel speakerListener;

    @Mock
    private ChannelPipeline pipeline;

    private final BgpTableType ipv4tt = new BgpTableTypeImpl(Ipv4AddressFamily.class,
        UnicastSubsequentAddressFamily.class);

    private final List<Notification> receivedMsgs = new ArrayList<>();

    private Open classicOpen;

    private BGPSessionImpl bgpSession;

    private SimpleSessionListener listener;

    @Before
    public void setUp() throws UnknownHostException {
        new EmbeddedChannel();
        MockitoAnnotations.initMocks(this);
        final List<BgpParameters> tlvs = new ArrayList<>();
        this.classicOpen = new OpenBuilder()
                .setMyAsNumber(Uint16.valueOf(AS_NUMBER.getValue()))
                .setHoldTimer(HOLD_TIMER)
                .setVersion(new ProtocolVersion(Uint8.valueOf(4)))
                .setBgpParameters(tlvs)
                .setBgpIdentifier(BGP_ID)
                .build();

        final List<OptionalCapabilities> capa = new ArrayList<>();
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder()
            .addAugmentation(CParameters1.class, new CParameters1Builder()
                .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                    .setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi()).build())
                .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder().build()).build())
            .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(AS_NUMBER).build()).build()).build());
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY)
            .build());
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capa).build());

        final ChannelFuture f = mock(ChannelFuture.class);
        doReturn(null).when(f).addListener(any());

        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            BGPSessionImplTest.this.receivedMsgs.add((Notification) args[0]);
            return f;
        }).when(this.speakerListener).writeAndFlush(any(Notification.class));
        doReturn(this.eventLoop).when(this.speakerListener).eventLoop();
        doReturn(true).when(this.speakerListener).isActive();
        doAnswer(invocation -> {
            final Runnable command = (Runnable) invocation.getArguments()[0];
            final long delay = (long) invocation.getArguments()[1];
            final TimeUnit unit = (TimeUnit) invocation.getArguments()[2];
            GlobalEventExecutor.INSTANCE.schedule(command, delay, unit);
            return null;
        }).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn("TestingChannel").when(this.speakerListener).toString();
        doReturn(true).when(this.speakerListener).isWritable();
        doReturn(new InetSocketAddress(InetAddress.getByName(BGP_ID.getValue()), 179)).when(this.speakerListener)
        .remoteAddress();
        doReturn(new InetSocketAddress(InetAddress.getByName(LOCAL_IP), LOCAL_PORT)).when(this.speakerListener)
        .localAddress();
        doReturn(this.pipeline).when(this.speakerListener).pipeline();
        doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class),
            any(ChannelHandler.class));
        doReturn(null).when(this.pipeline).replace(ArgumentMatchers.<Class<ChannelHandler>>any(), any(String.class),
            any(ChannelHandler.class));
        doReturn(this.pipeline).when(this.pipeline).addLast(any(ChannelHandler.class));
        final ChannelFuture futureChannel = mock(ChannelFuture.class);
        doReturn(null).when(futureChannel).addListener(any());
        doReturn(futureChannel).when(this.speakerListener).close();
        this.listener = new SimpleSessionListener();
        this.bgpSession = new BGPSessionImpl(this.listener, this.speakerListener, this.classicOpen,
            this.classicOpen.getHoldTimer().toJava(), null);
        this.bgpSession.setChannelExtMsgCoder(this.classicOpen);
    }

    @Test
    public void testBGPSession() throws BGPDocumentedException {
        this.bgpSession.sessionUp();
        assertEquals(State.UP, this.bgpSession.getState());
        assertEquals(AS_NUMBER, this.bgpSession.getAsNumber());
        assertEquals(BGP_ID, this.bgpSession.getBgpId());
        assertEquals(1, this.bgpSession.getAdvertisedTableTypes().size());
        assertEquals(State.UP, this.listener.getState());

        this.bgpSession.handleMessage(new UpdateBuilder().build());
        assertEquals(1, this.listener.getListMsg().size());
        assertTrue(this.listener.getListMsg().get(0) instanceof Update);
        this.bgpSession.close();
        assertEquals(State.IDLE, this.bgpSession.getState());
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(0);
        assertEquals(BGPError.CEASE.getCode(), error.getErrorCode());
        assertEquals(BGPError.CEASE.getSubcode(), error.getErrorSubcode());
        verify(this.speakerListener).close();
    }

    @Test
    public void testHandleOpenMsg() throws BGPDocumentedException {
        this.bgpSession.handleMessage(this.classicOpen);
        assertEquals(State.IDLE, this.bgpSession.getState());
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(0);
        assertEquals(BGPError.FSM_ERROR.getCode(), error.getErrorCode());
        assertEquals(BGPError.FSM_ERROR.getSubcode(), error.getErrorSubcode());
        verify(this.speakerListener).close();
    }

    @Test
    public void testHandleNotifyMsg() throws BGPDocumentedException {
        this.bgpSession.handleMessage(new NotifyBuilder().setErrorCode(BGPError.BAD_BGP_ID.getCode())
                .setErrorSubcode(BGPError.BAD_BGP_ID.getSubcode()).build());
        assertEquals(State.IDLE, this.bgpSession.getState());
        verify(this.speakerListener).close();
    }

    @Test
    public void testEndOfInput() throws InterruptedException {
        this.bgpSession.sessionUp();
        assertEquals(State.UP, this.listener.getState());
        this.bgpSession.endOfInput();
        checkIdleState(this.listener);
    }

    @Test
    public void testHoldTimerExpire() throws InterruptedException {
        this.bgpSession.sessionUp();
        checkIdleState(this.listener);
        assertEquals(3, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(2) instanceof Notify);
        final Notify error = (Notify) this.receivedMsgs.get(2);
        assertEquals(BGPError.HOLD_TIMER_EXPIRED.getCode(), error.getErrorCode());
        assertEquals(BGPError.HOLD_TIMER_EXPIRED.getSubcode(), error.getErrorSubcode());
        verify(this.speakerListener).close();
    }

    @Test
    public void testSessionRecoveryOnException() throws Exception {
        final BGPSessionListener mockListener = mock(BGPSessionListener.class);
        final IllegalStateException mockedEx = new IllegalStateException("Mocked runtime exception.");

        doThrow(mockedEx).when(mockListener).onSessionUp(any());
        doNothing().when(mockListener).onSessionTerminated(any(), any());
        this.bgpSession = spy(new BGPSessionImpl(mockListener, this.speakerListener, this.classicOpen,
                this.classicOpen.getHoldTimer().toJava(), null));
        this.bgpSession.setChannelExtMsgCoder(this.classicOpen);

        verify(this.bgpSession, never()).handleException(any());
        verify(this.bgpSession, never()).writeAndFlush(any(Notification.class));
        verify(this.bgpSession, never()).terminate(any(BGPDocumentedException.class));
        try {
            this.bgpSession.sessionUp();
            // expect the exception to be populated
            fail();
        } catch (final IllegalStateException e) {
            assertSame(mockedEx, e);
        }
        assertNotEquals(State.UP, this.bgpSession.getState());
        verify(this.bgpSession).handleException(any());
        verify(this.bgpSession).writeAndFlush(any(Notification.class));
        verify(this.bgpSession).terminate(any(BGPDocumentedException.class));
        verify(mockListener).onSessionTerminated(this.bgpSession, new BGPTerminationReason(BGPError.CEASE));
    }
}
