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
import static org.opendaylight.protocol.bmp.parser.message.TestUtil.createPeerUpNotification;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerUpNotification;

public class PeerUpHandlerTest extends AbstractBmpMessageTest {

    private static final byte[] PEER_UP_NOTIFICATION = {
        /*
         * 03 <- bmp version
         * 00 00 00 D3 <- total length of peer up message + common header lenght
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
         *
         * 00 00 00 04 <- information tlv - type and length
         * 61 61 61 61 <- value
         */
        (byte) 0x03,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xA2,
        (byte) 0x03,

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
        (byte) 0x02, (byte) 0x0C, (byte) 0x41, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x46, (byte) 0x41, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04,
        (byte) 0x61, (byte) 0x61, (byte) 0x61, (byte) 0x61
    };

    @Test
    public void testSerializePeerUpNotification() throws BmpDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        getBmpMessageRegistry().serializeMessage(createPeerUpNotification(), buffer);
        assertArrayEquals(PEER_UP_NOTIFICATION, ByteArray.readAllBytes(buffer));
    }

    @Test
    public void testParsePeerUpNotification() throws BmpDeserializationException {
        final PeerUpNotification parsedPeerUpNotif = (PeerUpNotification) getBmpMessageRegistry().parseMessage(
                Unpooled.copiedBuffer(PEER_UP_NOTIFICATION));
        assertEquals(createPeerUpNotification(), parsedPeerUpNotif);
    }
}
