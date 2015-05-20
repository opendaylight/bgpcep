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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bmp.parser.BMPDeserializationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.FsmEventCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.NotificationBuilder;

public class PeerDownHandlerTest {

    private static PeerDownHandler peerDownHandler;

    private final byte[] peerDownNotifFSMData = {
        /*
         * 03 <- bmp version
         * 00 00 00 31 <- total length of peer down notification + common header lenght
         * 02 <- bmp message type (2 - peer down notification)
         *
         * 00 <- global type
         * 00 <- flags (L and V flag)
         * 00 00 <- post flag padding - 2 bytes skipped
         * 00 00 00 00 <- 4 bytes skipped (because global type) - without distinguisher
         * 00 00 00 00 00 00 00 00 00 00 00 00 - skip IPV6_LENGTH - IPV4_LENGTH - 12 bytes
         * 01 02 02 04 01 02 02 04 01 02 02 04 01 02 02 04 - IPV6 address - 16 bytes
         * 00 00 00 48 - as number
         * 0A 0A 0A 0A - bgp id - ipv4 address - 4 bytes
         * 00 00 00 05 - time stamp - 4 bytes
         * 00 00 00 0A - time stamp micro - 4 bytes
         *
         * 02 <- Reason why the session was terminated (2 - the local system closed the session)
         * 00 18 <- the code of FSM event (24 - NotifMsgVerErr - An event is generated when a Notification message with "version error" is received.)
         */
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x31, (byte) 0x02,

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

        (byte) 0x02, (byte) 0x00, (byte) 0x18
    };

    private final byte[] peerDownNotifData = {
        /*
         * 03 <- bmp version
         * 00 00 00 44 <- total length of peer down notification + common header lenght
         * 02 <- bmp message type (2 - peer down notification)
         *
         * FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF - marker
         * 00 17 - mesage length (greater than 19 bytes == COMMON_HEADER_LENGTH)
         * 02 - message type
         * 00 00 - number of withdrawn routes
         * 00 00 - total path attributes lengths
         *
         * 00 <- global type
         * 00 <- flags (L and V flag)
         * 00 00 <- post flag padding - 2 bytes skipped
         * 00 00 00 00 <- 4 bytes skipped (because global type) - without distinguisher
         * 00 00 00 00 00 00 00 00 00 00 00 00 - skip IPV6_LENGTH - IPV4_LENGTH - 12 bytes
         * 0A 0A 0A 0A - IPV4 address - 4 bytes
         * 00 00 00 48 - as number
         * 0A 0A 0A 0A - bgp id - ipv4 address - 4 bytes
         * 00 00 00 05 - time stamp - 4 bytes
         * 00 00 00 0A - time stamp micro - 4 bytes
         *
         * 01 <- Reason why the session was terminated (1 - the local system closed the session + BGP Notification)
         *
         * FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF - marker
         * 00 15 - message length (should be > COMMON-HEADER-LENGTH), COMMON-HEADER-LENGTH = 19 bytes
         * 03 - message type NOTIFICATION
         * 01 - error code - message header error
         * 01 - connection not synchronized
         */
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x44, (byte) 0x02,

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

        (byte) 0x01,

        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, (byte) 0x15,
        (byte) 0x03,
        (byte) 0x01,
        (byte) 0x01
    };

    @BeforeClass
    public static void init() {
        final BGPActivator bgpActivator = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        bgpActivator.start(context);
        peerDownHandler = new PeerDownHandler(context.getMessageRegistry());
    }

    @Test
    public void testSerializePeerDownNotificationFSM() throws BMPDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        peerDownHandler.serializeMessage(createPeerDownNotificationFSM(), buffer);
        final byte[] serializedNotif = new byte[buffer.writerIndex()];
        buffer.readBytes(serializedNotif);
        assertArrayEquals(peerDownNotifFSMData, serializedNotif);
    }

    @Test
    public void testSerializePeerDownNotification() throws BMPDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        peerDownHandler.serializeMessage(createPeerDownNotification(), buffer);
        final byte[] serializedNotif = new byte[buffer.writerIndex()];
        buffer.readBytes(serializedNotif);
        assertArrayEquals(peerDownNotifData, serializedNotif);
    }

    @Test
    public void testParsePeerDownNotificationFSM() throws BMPDeserializationException {
        final ByteBuf dataWithoutHeader = Unpooled.copiedBuffer(peerDownNotifFSMData).skipBytes(InitiationHandlerTest.HEADER_LENGTH);
        final PeerDownNotification parsedPeerDownNotifFSM = (PeerDownNotification) peerDownHandler.parseMessageBody(dataWithoutHeader);
        assertEquals(createPeerDownNotificationFSM(), parsedPeerDownNotifFSM);
    }

    @Test
    public void testParsePeerDownNotification() throws BMPDeserializationException {
        final ByteBuf dataWithoutHeader = Unpooled.copiedBuffer(peerDownNotifData).skipBytes(InitiationHandlerTest.HEADER_LENGTH);
        final PeerDownNotification parsedPeerDownNotif = (PeerDownNotification) peerDownHandler.parseMessageBody(dataWithoutHeader);
        assertEquals(createPeerDownNotification(), parsedPeerDownNotif);
    }

    private static final PeerDownNotification createPeerDownNotificationFSM() {
        final PeerDownNotificationBuilder peerDownNotifBuilder = new PeerDownNotificationBuilder()
            .setData(new FsmEventCodeBuilder().setFsmEventCode(24).build())
            .setLocalSystemClosed(true)
            .setPeerHeader(RouteMonitoringMessageHandlerTest.createPeerHeader());
        return peerDownNotifBuilder.build();
    }

    private static final PeerDownNotification createPeerDownNotification() {
        final NotificationBuilder notifBuilder = new NotificationBuilder()
            .setNotification(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.notification.NotificationBuilder()
            .setErrorCode((short) 1)
            .setErrorSubcode((short) 1).build());
        final PeerDownNotificationBuilder peerDownNotifBuilder = new PeerDownNotificationBuilder()
            .setData(notifBuilder.build())
            .setLocalSystemClosed(true)
            .setPeerHeader(RouteMonitoringMessageHandlerTest.createPeerHeader());
        return peerDownNotifBuilder.build();
    }
}
