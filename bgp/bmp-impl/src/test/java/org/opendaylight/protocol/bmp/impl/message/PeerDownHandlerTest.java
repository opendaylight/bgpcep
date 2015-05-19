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
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bmp.parser.BMPDeserializationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.FsmEventCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.NotificationBuilder;

public class PeerDownHandlerTest {

    private final MessageRegistry msgRegistry = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry();
    private final PeerDownHandler peerDownHandler = new PeerDownHandler(msgRegistry);

    private final byte[] peerDownNotifFSMData = {
        /*
         * 03 <- bmp version
         * 00 00 00 09 <- total length of peer down notification + common header lenght
         * 02 <- bmp message type (2 - peer down notification)
         * 02 <- Reason why the session was terminated (2 - the local system closed the session)
         * 18 <- the code of FSM event (24 - NotifMsgVerErr - An event is generated when a Notification message with "version error" is received.)
         */
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x18 // <- data should have 2 byte
    };

    private final byte[] peerDownNotifData = {
        /*
         * 03 <- bmp version
         * 00 00 00 08 <- total length of peer down notification + common header lenght
         * 02 <- bmp message type (2 - peer down notification)
         * 01 <- Reason why the session was terminated (1 - the local system closed the session + BGP Notification)
         * 18 <- the code of FSM event (24 - NotifMsgVerErr - An event is generated when a Notification message with "version error" is received.)
         */
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x18 // <- data should have 2 byte
    };


    @Test
    public void testSerializePeerDownNotificationFSM() throws BMPDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        peerDownHandler.serializeMessage(createPeerDownNotificationFSM(), buffer);
        final byte[] serializedNotif = new byte[buffer.writerIndex()];
        buffer.readBytes(serializedNotif);
        assertArrayEquals(peerDownNotifFSMData, serializedNotif);
    }

    @Ignore
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

    @Ignore
    @Test
    public void testParsePeerDownNotification() throws BMPDeserializationException {
        final ByteBuf dataWithoutHeader = Unpooled.copiedBuffer(peerDownNotifData).skipBytes(InitiationHandlerTest.HEADER_LENGTH);
        final PeerDownNotification parsedPeerDownNotifFSM = (PeerDownNotification) peerDownHandler.parseMessageBody(dataWithoutHeader);
        assertEquals(createPeerDownNotificationFSM(), parsedPeerDownNotifFSM);
    }

    private static final PeerDownNotification createPeerDownNotificationFSM() {
        final PeerDownNotificationBuilder peerDownNotifBuilder = new PeerDownNotificationBuilder()
            .setData(new FsmEventCodeBuilder().setFsmEventCode(24).build())
            .setLocalSystemClosed(true);
        return peerDownNotifBuilder.build();
    }

    private static final PeerDownNotification createPeerDownNotification() {
        final NotificationBuilder notifBuilder = new NotificationBuilder()
            .setNotification(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.notification.NotificationBuilder()
            .setData(new byte[] { (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03 })
            .setErrorCode((short) 2)
            .setErrorSubcode((short) 3).build());
        final PeerDownNotificationBuilder peerDownNotifBuilder = new PeerDownNotificationBuilder()
            .setData(notifBuilder.build())
            .setLocalSystemClosed(true);

        return peerDownNotifBuilder.build();
    }
}
