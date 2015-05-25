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
import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Gauge64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.StatsReportsMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.AdjRibsInRoutesTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.DuplicatePrefixAdvertisementsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.DuplicateWithdrawsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.InvalidatedAsConfedLoopTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.InvalidatedAsPathLoopTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.InvalidatedClusterListLoopTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.InvalidatedOriginatorIdTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.LocRibRoutesTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.RejectedPrefixesTlvBuilder;

public class StatisticsReportHandlerTest extends AbstractBmpMessageTest {

    private static final byte[] STATS_REPORT = {
        /*
         * 03 <- bmp version
         * 00 00 00 82 <- total length of initiation message + common header lenght
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
         * 00 00 00 09 <- TLVs count
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
         */
        (byte) 0x03,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x82,
        (byte) 0x01,

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

        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10,
        (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0B, (byte) 0x00, (byte) 0x03, (byte) 0x00,
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x35, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x42, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x46, (byte) 0x00,
        (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x37, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x08,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64
    };

    @Test
    public void testSerializeStatsReportMessage() throws BmpDeserializationException {
        final ByteBuf buffer = Unpooled.buffer(STATS_REPORT.length);
        getBmpMessageRegistry().serializeMessage(createStatsReportMsg(), buffer);
        assertArrayEquals(STATS_REPORT, ByteArray.readAllBytes(buffer));
    }

    @Test
    public void testParseStatsReportMessage() throws BmpDeserializationException {
        final StatsReportsMessage parsedInitMsg = (StatsReportsMessage) getBmpMessageRegistry().parseMessage(Unpooled.copiedBuffer(STATS_REPORT));
        assertEquals(createStatsReportMsg(), parsedInitMsg);
    }

    private static StatsReportsMessage createStatsReportMsg() {
        final StatsReportsMessageBuilder statsReportMsgBuilder = new StatsReportsMessageBuilder();
        statsReportMsgBuilder.setPeerHeader(RouteMonitoringMessageHandlerTest.createPeerHeader());
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        tlvsBuilder.setAdjRibsInRoutesTlv(new AdjRibsInRoutesTlvBuilder().setCount(new Gauge64(BigInteger.valueOf(10L))).build());
        tlvsBuilder.setDuplicatePrefixAdvertisementsTlv(new DuplicatePrefixAdvertisementsTlvBuilder().setCount(new Counter32(16L)).build());
        tlvsBuilder.setDuplicateWithdrawsTlv(new DuplicateWithdrawsTlvBuilder().setCount(new Counter32(11L)).build());
        tlvsBuilder.setInvalidatedAsConfedLoopTlv(new InvalidatedAsConfedLoopTlvBuilder().setCount(new Counter32(55L)).build());
        tlvsBuilder.setInvalidatedAsPathLoopTlv(new InvalidatedAsPathLoopTlvBuilder().setCount(new Counter32(66L)).build());
        tlvsBuilder.setInvalidatedClusterListLoopTlv(new InvalidatedClusterListLoopTlvBuilder().setCount(new Counter32(53L)).build());
        tlvsBuilder.setInvalidatedOriginatorIdTlv(new InvalidatedOriginatorIdTlvBuilder().setCount(new Counter32(70L)).build());
        tlvsBuilder.setLocRibRoutesTlv(new LocRibRoutesTlvBuilder().setCount(new Gauge64(BigInteger.valueOf(100L))).build());
        tlvsBuilder.setRejectedPrefixesTlv(new RejectedPrefixesTlvBuilder().setCount(new Counter32(8L)).build());
        return statsReportMsgBuilder.setTlvs(tlvsBuilder.build()).build();
    }
}
