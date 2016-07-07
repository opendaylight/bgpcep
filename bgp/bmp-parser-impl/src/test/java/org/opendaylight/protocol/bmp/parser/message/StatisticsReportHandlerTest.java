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
import static org.opendaylight.protocol.bmp.parser.message.TestUtil.createStatsReportMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.StatsReportsMessage;

public class StatisticsReportHandlerTest extends AbstractBmpMessageTest {

    private static final byte[] STATS_REPORT = {
        /*
         * 03 <- bmp version
         * 00 00 00 c2 <- total length of stats message + common header lenght
         * 01 <- bmp message type - statistcs report
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
         * 00 00 00 0E <- TLVs count
         * 00 00 - rejected prefix type
         * 00 04 - length
         * 00 00 00 08 - value
         * 00 01 <- duplicate prefix adv type
         * 00 04 <- length
         * 00 00 00 10 - value
         * 00 02 <- duplicate withdraws type
         * 00 04 <- length
         * 00 00 00 0B <- value
         * 00 03 <- invalidate cluster type
         * 00 04 <- legnth
         * 00 00 00 53 <- value
         * 00 04 <- as path type
         * 00 04 <- length
         * 00 00 00 66 <- value
         * 00 05 <- invalidate originator id type
         * 00 04 <- length
         * 00 00 00 70 <- value
         * 00 06 <- invalidate as confed type
         * 00 04 <- length
         * 00 00 00 55 <- value
         * 00 07 <- adj ribs in routes type
         * 00 08 <- length
         * 00 00 00 00 00 00 00 10
         * 00 08 <- loc rib routes type
         * 00 08 <- length
         * 00 00 00 00 00 00 00 64 <- value
         * 00 09 <- per afi safi adj rib routes type
         * 00 0B <- length
         * 00 01 <- afi
         * 01    <- safi
         * 00 00 00 00 00 00 00 09 <- value
         * 00 0A <- per afi safi local rib type
         * 00 0B <- length
         * 00 01 <- afi
         * 01    <- safi
         * 00 00 00 00 00 00 00 0A <- value
         * 00 0B <- updates treated as withdraw
         * 00 04 <- length
         * 00 00 00 0B  <- value
         * 00 0C <- prefixes treated as withdraw
         * 00 04 <- length
         * 00 00 00 0C  <- value
         * 00 0D <- duplicate updates
         * 00 04 <- length
         * 00 00 00 0D  <- value
         */

        (byte) 0x03,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA,
        (byte) 0x01,

        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,    // < - element 28
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48,
        (byte) 0x0A, (byte) 0x0A, (byte) 0x0A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0A,  // <-element 48

        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0E,  // <- tlv counts
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10,
        (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0B, (byte) 0x00, (byte) 0x03, (byte) 0x00,
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x35, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x42, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x46, (byte) 0x00,
        (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x37, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x08,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64,

        (byte) 0x00, (byte) 0x09, //  <- per afi safi adj rib routes type
        (byte) 0x00, (byte) 0x0B, //  <- length
        (byte) 0x00, (byte) 0x01, //  <- afi
        (byte) 0x01,              //  <- safi
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09, //  <- value
        (byte) 0x00, (byte) 0x0A, //  <- per afi safi local rib routes type
        (byte) 0x00, (byte) 0x0B, //  <- length
        (byte) 0x00, (byte) 0x01, //  <- afi
        (byte) 0x01,              //  <- safi
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0A, //  <- value
        (byte) 0x00, (byte) 0x0B, //  <- updates treated as withdraw
        (byte) 0x00, (byte) 0x04, // <- length
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0B, //  <- value
        (byte) 0x00, (byte) 0x0C, //  <- prefixes treated as withdraw
        (byte) 0x00, (byte) 0x04,  //  <- length
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0C,//  <- value
        (byte) 0x00, (byte) 0x0D, //  <- duplicate updates
        (byte) 0x00, (byte) 0x04, //  <- length
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0D  // <- value
    };

    @Test
    public void testSerializeStatsReportMessage() throws BmpDeserializationException {
        final ByteBuf buffer = Unpooled.buffer(STATS_REPORT.length);
        getBmpMessageRegistry().serializeMessage(createStatsReportMsg(), buffer);
        assertArrayEquals(STATS_REPORT, ByteArray.readAllBytes(buffer));
    }

    @Test
    public void testParseStatsReportMessage() throws BmpDeserializationException {
        final StatsReportsMessage parsedStatsReportsMsg = (StatsReportsMessage) getBmpMessageRegistry().parseMessage(Unpooled.copiedBuffer(STATS_REPORT));
        assertEquals(createStatsReportMsg(), parsedStatsReportsMsg);
    }
}
