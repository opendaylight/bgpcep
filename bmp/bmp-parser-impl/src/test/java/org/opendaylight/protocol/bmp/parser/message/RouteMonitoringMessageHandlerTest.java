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
import static org.opendaylight.protocol.bmp.parser.message.TestUtil.createRouteMonitMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.RouteMonitoringMessage;

public class RouteMonitoringMessageHandlerTest extends AbstractBmpMessageTest {

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
     * FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF <- marker
     * 00 A5 <- message length
     * 02 <- Update message
     */
    private static final byte[] ROUTE_MONIT_MSG = {
        (byte) 0x03,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB4,
        (byte) 0x00,
        (byte) 0x00,

        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0A,

        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, (byte) 0x84,
        (byte) 0x02,
        (byte) 0x00, (byte) 0x0C, (byte) 0x18, (byte) 0x0A, (byte) 0x0A, (byte) 0x14, (byte) 0x18, (byte) 0x14, (byte) 0x14,
        (byte) 0x0A, (byte) 0x18, (byte) 0x1E, (byte) 0x0A, (byte) 0x0A, (byte) 0x00, (byte) 0x55, (byte) 0x40,
        (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x80, (byte) 0x1A, (byte) 0x0B, (byte) 0x01, (byte) 0x00,
        (byte) 0x0B, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x40, (byte) 0x02, (byte) 0x00, (byte) 0x40, (byte) 0x03, (byte) 0x04, (byte) 0x64,
        (byte) 0x64, (byte) 0x64, (byte) 0x64, (byte) 0x80, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x7B, (byte) 0x40, (byte) 0x05, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x02, (byte) 0x40, (byte) 0x06, (byte) 0x00, (byte) 0xC0, (byte) 0x07, (byte) 0x08, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0xC0,
        (byte) 0x08, (byte) 0x08, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x65, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0x66, (byte) 0x80, (byte) 0x09, (byte) 0x04, (byte) 0x0C, (byte) 0x0C, (byte) 0x0C,
        (byte) 0x0C, (byte) 0x80, (byte) 0x0A, (byte) 0x08, (byte) 0x1E, (byte) 0x1E, (byte) 0x1E, (byte) 0x1E,
        (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x18, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x18, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x18, (byte) 0x1E, (byte) 0x1E, (byte) 0x1E
    };

    @Test
    public void testSerializeRouteMonitMessage() throws BmpDeserializationException  {
        final ByteBuf buffer = Unpooled.buffer();
        getBmpMessageRegistry().serializeMessage(createRouteMonitMsg(false), buffer);
        assertArrayEquals(ROUTE_MONIT_MSG, ByteArray.readAllBytes(buffer));
    }

    @Test
    public void testParseRouteMonitMessage() throws BmpDeserializationException {
        final RouteMonitoringMessage parsedInitMsg = (RouteMonitoringMessage) getBmpMessageRegistry().parseMessage(Unpooled.copiedBuffer(ROUTE_MONIT_MSG));
        assertEquals(createRouteMonitMsg(true), parsedInitMsg);
    }
}
