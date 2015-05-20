/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.message;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bmp.parser.BMPDeserializationException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.ReceivedOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.ReceivedOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.SentOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.SentOpenBuilder;

public class PeerUpHandlerTest {

    private static PeerUpHandler peerUpHandler;

    private final byte[] peerUpNotifData = {
        /*
         * 03 <- bmp version
         * 00 00 00 48 <- total length of peer up notification + common header lenght
         * 03 <- bmp message type (3 - peer up notification)
         *
         */
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x98, (byte) 0x03,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0xDC, (byte) 0x13, (byte) 0x88,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, (byte) 0x2B, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x48, (byte) 0x03, (byte) 0xE8, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x0E, (byte) 0x02, (byte) 0x0C, (byte) 0x41, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x46, (byte) 0x41, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, (byte) 0x2B, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x48, (byte) 0x07, (byte) 0xD0, (byte) 0x14, (byte) 0x1E, (byte) 0x14, (byte) 0x1E, (byte) 0x0E, (byte) 0x02, (byte) 0x0C, (byte) 0x41, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x46, (byte) 0x41, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50,
    };

    @BeforeClass
    public static void init() {
        final BGPActivator bgpActivator = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        bgpActivator.start(context);
        peerUpHandler = new PeerUpHandler(context.getMessageRegistry());
    }

    @Test
    public void testSerializePeerUpNotification() throws BMPDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        peerUpHandler.serializeMessage(createPeerUpNotification(), buffer);
        final byte[] serializedNotif = new byte[buffer.writerIndex()];
        buffer.readBytes(serializedNotif);
        assertArrayEquals(peerUpNotifData, serializedNotif);
    }

    @Ignore
    @Test
    public void testParsePeerUPNotification() throws BMPDeserializationException {
        final ByteBuf dataWithoutHeader = Unpooled.copiedBuffer(peerUpNotifData).skipBytes(InitiationHandlerTest.HEADER_LENGTH);
        final PeerDownNotification parsedPeerDownNotif = (PeerDownNotification) peerUpHandler.parseMessageBody(dataWithoutHeader);
        assertEquals(createPeerUpNotification(), parsedPeerDownNotif);
    }

    private static final PeerUpNotification createPeerUpNotification() {
        final PeerUpNotificationBuilder peerUpNotifBuilder = new PeerUpNotificationBuilder()
            .setLocalAddress(new IpAddress(new Ipv4Address("10.10.10.10")))
            .setLocalPort(new PortNumber(220))
            .setPeerHeader(RouteMonitoringMessageHandlerTest.createPeerHeader())
            .setReceivedOpen(createReceivedOpen())
            .setRemotePort(new PortNumber(5000))
            .setSentOpen(createSentOpen());

        return peerUpNotifBuilder.build();
    }

    private static final ReceivedOpen createReceivedOpen() {
        final ReceivedOpenBuilder receivedOpenBuilder = new ReceivedOpenBuilder()
            .setBgpIdentifier(new Ipv4Address("20.20.20.20"))
            .setHoldTimer(1000)
            .setMyAsNumber(72)
            .setVersion(new ProtocolVersion((short) 1))
            .setBgpParameters(createBgpParameters());

        return receivedOpenBuilder.build();
    }

    private static final List<BgpParameters> createBgpParameters() {
        final BgpParametersBuilder bgpParamBuilder = new BgpParametersBuilder()
            .setOptionalCapabilities(createOptionalCapabilities());
        final List<BgpParameters> bgpParameters = Lists.newArrayList();
        bgpParameters.add(bgpParamBuilder.build());

        return bgpParameters;
    }

    private static final List<OptionalCapabilities> createOptionalCapabilities() {
        final OptionalCapabilitiesBuilder optCapabilitiesBuilder = new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(70L)).build()).build());
        final OptionalCapabilitiesBuilder optCapabilitiesBuilder2 = new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(80L)).build()).build());
        final List<OptionalCapabilities> optCapabilities = Lists.newArrayList();
        optCapabilities.add(optCapabilitiesBuilder.build());
        optCapabilities.add(optCapabilitiesBuilder2.build());

        return optCapabilities;
    }

    private static final SentOpen createSentOpen() {
        final SentOpenBuilder sentOpenBuilder = new SentOpenBuilder()
            .setBgpIdentifier(new Ipv4Address("20.30.20.30"))
            .setHoldTimer(2000)
            .setMyAsNumber(72)
            .setVersion(new ProtocolVersion((short) 1))
            .setBgpParameters(createBgpParameters());

        return sentOpenBuilder.build();
    }
}
