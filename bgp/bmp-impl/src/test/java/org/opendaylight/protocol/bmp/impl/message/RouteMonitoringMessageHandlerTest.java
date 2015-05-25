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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Timestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AigpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AtomicAggregateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.CommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.aigp.AigpTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring.message.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring.message.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.AccumulatedIgpMetric;

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
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB2,
        (byte) 0x00,
        (byte) 0x00,

        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
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

    private static RouteMonitoringMessage createRouteMonitMsg(final boolean withNormalizedIpv4Prefixes) {
        final RouteMonitoringMessageBuilder routeMonitMsgBuilder = new RouteMonitoringMessageBuilder()
            .setPeerHeader(createPeerHeader())
            .setUpdate(createUpdate(withNormalizedIpv4Prefixes));
        return routeMonitMsgBuilder.build();
    }

    public static PeerHeader createPeerHeader() {
        final PeerHeaderBuilder peerHeaderBuilder = new PeerHeaderBuilder()
            .setAddress(new IpAddress(new Ipv4Address("10.10.10.10")))
            .setAs(new AsNumber(72L))
            .setBgpId(new Ipv4Address("10.10.10.10"))
            .setAdjRibInType(AdjRibInType.forValue(0))
            .setTimestampMicro(new Timestamp(10L))
            .setTimestampSec(new Timestamp(5L))
            .setIpv4(true)
            .setType(PeerType.forValue(0));
        return peerHeaderBuilder.build();
    }

    private static Update createUpdate(final boolean withNormalizedIpv4Prefixes) {
        final UpdateBuilder updateBuilder = new UpdateBuilder()
            .setAttributes(createAttributes())
            .setWithdrawnRoutes(createWithdrwnRoutes());
        if (withNormalizedIpv4Prefixes) {
            updateBuilder.setNlri(createNlriWitNormalizedIpv4Prefixes());
        } else {
            updateBuilder.setNlri(createNlri());
        }

        return updateBuilder.build();
    }

    private static Attributes createAttributes() {
        final AsSequenceBuilder asSeqBuilder = new AsSequenceBuilder();
        asSeqBuilder.setAs(new AsNumber(72L));
        asSeqBuilder.build();
        final AsSequenceBuilder asSeqBuilder2 = new AsSequenceBuilder();
        asSeqBuilder.setAs(new AsNumber(82L));
        asSeqBuilder.build();
        final AsSequenceBuilder asSeqBuilder3 = new AsSequenceBuilder();
        asSeqBuilder.setAs(new AsNumber(92L));
        asSeqBuilder.build();

        final List<AsSequence> asSequences = Lists.newArrayList(asSeqBuilder.build(), asSeqBuilder2.build(), asSeqBuilder3.build());
        final List<Segments> segments = Lists.newArrayList();
        final SegmentsBuilder segmentsBuild = new SegmentsBuilder();
        segmentsBuild.setCSegment(new AListCaseBuilder().setAList(new AListBuilder().setAsSequence(asSequences).build()).build());

        final AttributesBuilder attribBuilder = new AttributesBuilder()
            .setAggregator(new AggregatorBuilder().setAsNumber(new AsNumber(72L)).setNetworkAddress(new Ipv4Address("20.20.20.20")).build())
            .setAigp(new AigpBuilder().setAigpTlv(new AigpTlvBuilder().setMetric(new AccumulatedIgpMetric(BigInteger.ONE)).build()).build())
            .setAsPath(new AsPathBuilder().setSegments(segments).build())
            .setAtomicAggregate(new AtomicAggregateBuilder().build())
            .setClusterId(new ClusterIdBuilder().setCluster(Lists.newArrayList(new ClusterIdentifier("30.30.30.30"),
                    new ClusterIdentifier(new Ipv4Address("40.40.40.40")))).build())
            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(
                    new Ipv4Address("100.100.100.100")).build()).build())
            .setCommunities(createCommunities())
            .setLocalPref(new LocalPrefBuilder().setPref(2L).build())
            .setMultiExitDisc(new MultiExitDiscBuilder().setMed(123L).build())
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setOriginatorId(new OriginatorIdBuilder().setOriginator(new Ipv4Address("12.12.12.12")).build())
            .setUnrecognizedAttributes(new ArrayList<UnrecognizedAttributes>());
        return attribBuilder.build();
    }

    private static final List<Communities> createCommunities() {
        final List<Communities> communities = Lists.newArrayList();
        final CommunitiesBuilder commBuilder = new CommunitiesBuilder()
            .setAsNumber(new AsNumber(65535L))
            .setSemantics(65381);
        final CommunitiesBuilder commBuilder2 = new CommunitiesBuilder()
            .setAsNumber(new AsNumber(65535L))
            .setSemantics(65382);
        communities.add(commBuilder.build());
        communities.add(commBuilder2.build());
        return communities;
    }

    private static Nlri createNlri() {
        final NlriBuilder nlriBuilder = new NlriBuilder()
            .setNlri(Lists.newArrayList(new Ipv4Prefix("10.10.10.10/24"), new Ipv4Prefix("20.20.20.20/24"), new Ipv4Prefix("30.30.30.30/24")));
        return nlriBuilder.build();
    }

    private static Nlri createNlriWitNormalizedIpv4Prefixes() {
        final NlriBuilder nlriBuilder = new NlriBuilder()
            .setNlri(Lists.newArrayList(new Ipv4Prefix("10.10.10.0/24"), new Ipv4Prefix("20.20.20.0/24"), new Ipv4Prefix("30.30.30.0/24")));
        return nlriBuilder.build();
    }

    private static WithdrawnRoutes createWithdrwnRoutes() {
        final WithdrawnRoutesBuilder withDrawnBuilder = new WithdrawnRoutesBuilder()
            .setWithdrawnRoutes(Lists.newArrayList(new Ipv4Prefix("10.10.20.0/24"), new Ipv4Prefix("20.20.10.0/24"), new Ipv4Prefix("30.10.10.0/24")));
        return withDrawnBuilder.build();
    }
}
