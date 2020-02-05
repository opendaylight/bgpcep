/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GenericFutureListener;
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
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.ProtocolVersion;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class FSMTest {
    @Mock
    private EventLoop eventLoop;

    private BGPClientSessionNegotiator clientSession;

    @Mock
    private Channel speakerListener;

    @Mock
    private ChannelPipeline pipeline;

    private final BgpTableType ipv4tt
            = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private final BgpTableType linkstatett
            = new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);

    private final List<Notification> receivedMsgs = new ArrayList<>();

    private Open classicOpen;

    @Before
    public void setUp() throws UnknownHostException {
        MockitoAnnotations.initMocks(this);
        final List<BgpParameters> tlvs = new ArrayList<>();
        final List<OptionalCapabilities> capas = new ArrayList<>();

        capas.add(new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
                        new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                                .setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi()).build()).build())
                        .build()).build());
        capas.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder()
                .addAugmentation(CParameters1.class,
                        new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                                .setAfi(this.linkstatett.getAfi()).setSafi(this.linkstatett.getSafi())
                                .build()).build()).build()).build());
        capas.add(new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder()
                .setAs4BytesCapability(new As4BytesCapabilityBuilder()
                    .setAsNumber(new AsNumber(Uint32.valueOf(30)))
                    .build())
                .build())
            .build());
        capas.add(new OptionalCapabilitiesBuilder()
                .setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        capas.add(new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
                        new CParameters1Builder().setGracefulRestartCapability(new GracefulRestartCapabilityBuilder()
                                .build()).build()).build()).build());


        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capas).build());
        final BGPSessionPreferences prefs = new BGPSessionPreferences(new AsNumber(Uint32.valueOf(30)), (short) 3,
                new BgpId("1.1.1.1"), new AsNumber(Uint32.valueOf(30)), tlvs);

        final ChannelFuture f = mock(ChannelFuture.class);
        doReturn(null).when(f).addListener(any(GenericFutureListener.class));

        final InetAddress peerAddress = InetAddress.getByName("1.1.1.2");
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            FSMTest.this.receivedMsgs.add((Notification) args[0]);
            return f;
        }).when(this.speakerListener).writeAndFlush(any(Notification.class));
        doReturn(this.eventLoop).when(this.speakerListener).eventLoop();
        doReturn(null).when(this.eventLoop).schedule(any(Runnable.class), any(long.class),
                any(TimeUnit.class));
        doReturn("TestingChannel").when(this.speakerListener).toString();
        doReturn(new InetSocketAddress(peerAddress, 179)).when(this.speakerListener).remoteAddress();
        doReturn(new InetSocketAddress(peerAddress, 179)).when(this.speakerListener).localAddress();
        doReturn(this.pipeline).when(this.speakerListener).pipeline();
        doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class),
                any(ChannelHandler.class));
        doReturn(null).when(this.pipeline).replace(ArgumentMatchers.<Class<ChannelHandler>>any(), any(String.class),
                any(ChannelHandler.class));
        doReturn(this.pipeline).when(this.pipeline).addLast(any(ChannelHandler.class));
        doReturn(mock(ChannelFuture.class)).when(this.speakerListener).close();

        final BGPPeerRegistry peerRegistry = new StrictBGPPeerRegistry();
        peerRegistry.addPeer(new IpAddressNoZone(new Ipv4AddressNoZone(peerAddress.getHostAddress())),
                new SimpleSessionListener(), prefs);

        this.clientSession = new BGPClientSessionNegotiator(new DefaultPromise<>(GlobalEventExecutor.INSTANCE),
                this.speakerListener, peerRegistry);

        this.classicOpen = new OpenBuilder()
                .setMyAsNumber(Uint16.valueOf(30))
                .setHoldTimer(Uint16.valueOf(3))
                .setVersion(new ProtocolVersion(Uint8.valueOf(4)))
                .setBgpParameters(tlvs)
                .setBgpIdentifier(new Ipv4AddressNoZone("1.1.1.2"))
                .build();
    }

    @Test
    public void testDenyPeer() {
        this.clientSession = new BGPClientSessionNegotiator(new DefaultPromise<>(GlobalEventExecutor.INSTANCE),
                this.speakerListener, new StrictBGPPeerRegistry());
        this.clientSession.channelActive(null);
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Notify);
    }

    @Test
    public void testAccSessionChar() {
        this.clientSession.channelActive(null);
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Open);
        this.clientSession.handleMessage(this.classicOpen);
        assertEquals(2, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(1) instanceof Keepalive);
        this.clientSession.handleMessage(new KeepaliveBuilder().build());
        assertEquals(this.clientSession.getState(), BGPClientSessionNegotiator.State.FINISHED);
    }

    @Test
    public void testNotAccChars() {
        this.clientSession.channelActive(null);
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Open);
        this.clientSession.handleMessage(new OpenBuilder()
            .setMyAsNumber(Uint16.valueOf(30))
            .setHoldTimer(Uint16.ONE)
            .setBgpIdentifier(new Ipv4AddressNoZone("127.0.0.1"))
            .setVersion(new ProtocolVersion(Uint8.valueOf(4)))
            .build());
        assertEquals(2, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(1) instanceof Notify);
        final Notification m = this.receivedMsgs.get(this.receivedMsgs.size() - 1);
        assertEquals(BGPError.UNSPECIFIC_OPEN_ERROR,
                BGPError.forValue(((Notify) m).getErrorCode(), ((Notify) m).getErrorSubcode()));
    }

    @Test
    public void testNoAs4BytesCapability() {
        this.clientSession.channelActive(null);
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Open);

        final List<BgpParameters> tlvs = new ArrayList<>();
        final List<OptionalCapabilities> capas = new ArrayList<>();
        capas.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder()
                .addAugmentation(CParameters1.class,
                        new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                                .setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi())
                                .build()).build()).build()).build());
        capas.add(new OptionalCapabilitiesBuilder()
                .setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capas).build());
        // Open Message without advertised four-octet AS Number capability
        this.clientSession.handleMessage(new OpenBuilder()
            .setMyAsNumber(Uint16.valueOf(30))
            .setHoldTimer(Uint16.ONE)
            .setVersion(new ProtocolVersion(Uint8.valueOf(4)))
            .setBgpParameters(tlvs)
            .setBgpIdentifier(new Ipv4AddressNoZone("1.1.1.2"))
            .build());
        assertEquals(2, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(1) instanceof Notify);
        final Notification m = this.receivedMsgs.get(this.receivedMsgs.size() - 1);
        assertEquals(BGPError.UNSUPPORTED_CAPABILITY,
                BGPError.forValue(((Notify) m).getErrorCode(), ((Notify) m).getErrorSubcode()));
        assertNotNull(((Notify) m).getData());
    }

    @Test
    public void testBgpExtendedMessageCapability() {
        this.clientSession.channelActive(null);
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Open);

        final List<BgpParameters> tlvs = new ArrayList<>();
        final List<OptionalCapabilities> capas = new ArrayList<>();
        capas.add(new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder()
                .addAugmentation(CParameters1.class, new CParameters1Builder()
                    .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                        .setAfi(this.ipv4tt.getAfi())
                        .setSafi(this.ipv4tt.getSafi())
                        .build())
                    .build())
                .build())
            .build());
        capas.add(new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder()
                .setAs4BytesCapability(new As4BytesCapabilityBuilder()
                    .setAsNumber(new AsNumber(Uint32.valueOf(30)))
                    .build())
                .build())
            .build());
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capas).build());
        this.clientSession.handleMessage(new OpenBuilder()
            .setMyAsNumber(Uint16.valueOf(30))
            .setHoldTimer(Uint16.ONE)
            .setVersion(new ProtocolVersion(Uint8.valueOf(4)))
            .setBgpParameters(tlvs)
            .setBgpIdentifier(new Ipv4AddressNoZone("1.1.1.2"))
            .build());
        assertEquals(2, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(1) instanceof Keepalive);
    }

    @Test
    public void sendNotification() {
        this.clientSession.channelActive(null);
        this.clientSession.handleMessage(this.classicOpen);
        this.clientSession.handleMessage(new KeepaliveBuilder().build());
        assertEquals(this.clientSession.getState(), BGPClientSessionNegotiator.State.FINISHED);
        this.clientSession.handleMessage(new OpenBuilder()
            .setMyAsNumber(Uint16.valueOf(30))
            .setHoldTimer(Uint16.valueOf(3))
            .setVersion(new ProtocolVersion(Uint8.valueOf(4)))
            .build());
        assertEquals(3, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(2) instanceof Notify);
        final Notification m = this.receivedMsgs.get(2);
        assertEquals(BGPError.FSM_ERROR.getCode(), ((Notify) m).getErrorCode());
        assertEquals(BGPError.FSM_ERROR.getSubcode(), ((Notify) m).getErrorSubcode());
    }

    @Test
    public void sameBGPIDs() {
        this.clientSession.channelActive(null);
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Open);

        this.clientSession.handleMessage(new OpenBuilder(this.classicOpen)
                .setBgpIdentifier(new Ipv4AddressNoZone("1.1.1.1")).build());
        assertEquals(2, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(1) instanceof Notify);
        final Notification m = this.receivedMsgs.get(this.receivedMsgs.size() - 1);
        assertEquals(BGPError.BAD_BGP_ID, BGPError.forValue(((Notify) m).getErrorCode(),
                ((Notify) m).getErrorSubcode()));
    }
}
