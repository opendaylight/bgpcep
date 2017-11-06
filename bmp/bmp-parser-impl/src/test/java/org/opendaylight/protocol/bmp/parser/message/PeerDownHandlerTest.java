/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.parser.message;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bmp.parser.message.TestUtil.createPeerDownFSM;
import static org.opendaylight.protocol.bmp.parser.message.TestUtil.createPeerDownNotification;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotification;

public class PeerDownHandlerTest extends AbstractBmpMessageTest {

    private static final byte[] PEER_DOWN_FSM_DATA = {
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
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x33, (byte) 0x02,

        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0A,

        (byte) 0x02, (byte) 0x00, (byte) 0x18
    };

    private static final byte[] PEER_DOWN_NOTIFICATION = {
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
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x46, (byte) 0x02,

        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
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

    @Test
    public void testSerializePeerDownNotificationFSM() throws BmpDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        getBmpMessageRegistry().serializeMessage(createPeerDownFSM(), buffer);
        assertArrayEquals(PEER_DOWN_FSM_DATA, ByteArray.readAllBytes(buffer));
    }

    @Test
    public void testSerializePeerDownNotification() throws BmpDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        getBmpMessageRegistry().serializeMessage(createPeerDownNotification(), buffer);
        assertArrayEquals(PEER_DOWN_NOTIFICATION, ByteArray.readAllBytes(buffer));
    }

    @Test
    public void testParsePeerDownNotificationFSM() throws BmpDeserializationException {
        final PeerDownNotification parsedPeerDownFSM = (PeerDownNotification) getBmpMessageRegistry().parseMessage(
                Unpooled.copiedBuffer(PEER_DOWN_FSM_DATA));
        assertEquals(createPeerDownFSM(), parsedPeerDownFSM);
    }

    @Test
    public void testParsePeerDownNotification() throws BmpDeserializationException {
        final PeerDownNotification parsedPeerDownNotif = (PeerDownNotification) getBmpMessageRegistry().parseMessage(
                Unpooled.copiedBuffer(PEER_DOWN_NOTIFICATION));
        assertEquals(createPeerDownNotification(), parsedPeerDownNotif);
    }
}
