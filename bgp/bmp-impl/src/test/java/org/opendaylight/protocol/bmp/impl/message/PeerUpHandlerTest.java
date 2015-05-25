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
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.ReceivedOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.ReceivedOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.SentOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.SentOpenBuilder;

public class PeerUpHandlerTest extends AbstractBmpMessageTest {

    private static final byte[] PEER_UP_NOTIFICATION = {
        /*
         * 03 <- bmp version
         * 00 00 00 D3 <- total length of initiation message + common header lenght
         * 00 <- bmp message type Route Monitor
         *
         * 00 <- global type
         * 00 <- flags (L and V flag)
         * 00 00 <- post flag padding - 2 bytes skipped
         * 00 00 00 00 <- 4 bytes skipped (because global type) - without distinguisher
         * 00 00 00 00 00 00 00 00 00 00 00 00 <- skip IPV6_LENGTH - IPV4_LENGTH - 12 bytes
         * 0A 0A 0A 0A <- IPV4 address - 4 bytes
         * 00 00 00 48 <- as number
         * 0A 0A 0A 0A <- bgp id - ipv4 address - 4 bytes
         * 00 00 00 05 <- time stamp - 4 bytes
         * 00 00 00 0A <- time stamp micro - 4 bytes
         *
         * 00 00 00 00 00 00 00 00 00 00 00 00 <- skipped bytes
         * 0A 0A 0A 0A <- notification Ipv4 local address
         * 00 DC <- local port number
         * 13 88 <- remote port number
         *
         * FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF <- marker
         * 00 43 <- message length
         * 01 <- Open message
         * 04 <- bgp version
         * 00 48 <- as number (72)
         * 03 E8 <- hold time (1000)
         * 14 14 14 14 <- bgp id
         * 0E <- number of opt
         * 02 0C 41 04 00 00 00 46 41 04 00 00 00 50 <- opt values
         */
        (byte) 0x03,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x98,
        (byte) 0x03,

        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0A,

        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0xDC,
        (byte) 0x13, (byte) 0x88,

        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, (byte) 0x2B,
        (byte) 0x01,
        (byte) 0x04,
        (byte) 0x00, (byte) 0x48,
        (byte) 0x03, (byte) 0xE8,
        (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x14,
        (byte) 0x0E,
        (byte) 0x02, (byte) 0x0C, (byte) 0x41, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x46, (byte) 0x41, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50,

        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, (byte) 0x2B,
        (byte) 0x01,
        (byte) 0x04,
        (byte) 0x00, (byte) 0x48,
        (byte) 0x03, (byte) 0xE8,
        (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x14,
        (byte) 0x0E,
        (byte) 0x02, (byte) 0x0C, (byte) 0x41, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x46, (byte) 0x41, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50
    };

    @Test
    public void testSerializePeerUpNotification() throws BmpDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        getBmpMessageRegistry().serializeMessage(createPeerUpNotification(), buffer);
        assertArrayEquals(PEER_UP_NOTIFICATION, ByteArray.readAllBytes(buffer));
    }

    @Test
    public void testParsePeerUPNotification() throws BmpDeserializationException {
        final PeerUpNotification parsedPeerDownNotif = (PeerUpNotification) getBmpMessageRegistry().parseMessage(
                Unpooled.copiedBuffer(PEER_UP_NOTIFICATION));
        assertEquals(createPeerUpNotification(), parsedPeerDownNotif);
    }

    private static PeerUpNotification createPeerUpNotification() {
        final PeerUpNotificationBuilder peerUpNotifBuilder = new PeerUpNotificationBuilder()
            .setLocalAddress(new IpAddress(new Ipv4Address("10.10.10.10")))
            .setLocalPort(new PortNumber(220))
            .setPeerHeader(RouteMonitoringMessageHandlerTest.createPeerHeader())
            .setReceivedOpen(createReceivedOpen())
            .setRemotePort(new PortNumber(5000))
            .setSentOpen(createSentOpen());

        return peerUpNotifBuilder.build();
    }

    private static ReceivedOpen createReceivedOpen() {
        final ReceivedOpenBuilder receivedOpenBuilder = new ReceivedOpenBuilder()
            .setBgpIdentifier(new Ipv4Address("20.20.20.20"))
            .setHoldTimer(1000)
            .setMyAsNumber(72)
            .setBgpParameters(createBgpParameters());

        return receivedOpenBuilder.build();
    }

    private static List<BgpParameters> createBgpParameters() {
        final BgpParametersBuilder bgpParamBuilder = new BgpParametersBuilder()
            .setOptionalCapabilities(createOptionalCapabilities());
        final List<BgpParameters> bgpParameters = Lists.newArrayList();
        bgpParameters.add(bgpParamBuilder.build());

        return bgpParameters;
    }

    private static List<OptionalCapabilities> createOptionalCapabilities() {
        final OptionalCapabilitiesBuilder optCapabilitiesBuilder = new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(70L)).build()).build());
        final OptionalCapabilitiesBuilder optCapabilitiesBuilder2 = new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(80L)).build()).build());
        final List<OptionalCapabilities> optCapabilities = Lists.newArrayList();
        optCapabilities.add(optCapabilitiesBuilder.build());
        optCapabilities.add(optCapabilitiesBuilder2.build());

        return optCapabilities;
    }

    private static SentOpen createSentOpen() {
        final SentOpenBuilder sentOpenBuilder = new SentOpenBuilder()
            .setBgpIdentifier(new Ipv4Address("20.20.20.20"))
            .setHoldTimer(1000)
            .setMyAsNumber(72)
            .setBgpParameters(createBgpParameters());

        return sentOpenBuilder.build();
    }
}
