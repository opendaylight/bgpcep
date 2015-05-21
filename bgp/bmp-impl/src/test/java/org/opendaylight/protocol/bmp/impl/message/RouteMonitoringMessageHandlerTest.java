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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.UnrecognizedAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.UnrecognizedAttributesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.aigp.AigpTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.set._case.ASetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.AsSpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as.specific.extended.community._case.AsSpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Peer.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader.InboundPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring.message.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring.message.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.AccumulatedIgpMetric;

public class RouteMonitoringMessageHandlerTest {

    private static RouteMonitoringMessageHandler routeMonitHandler;

    private final byte[] routeMonitMsgData = {
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
         * FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF - marker
         *
         */
        (byte) 0x03,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xD3,
        (byte) 0x00,

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

        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, -91, 2, 0, 12, 24, 10, 10, 20, 24, 20, 20, 10, 24, 30, 10, 10, 0, 118, 64, 1, 1, 0, -128, 26, 11, 1, 0, 11, 0, 0, 0, 0, 0, 0, 0, 1, 64, 2, 14, 1, 3, 0, 0, 0, 72, 0, 0, 0, 80, 0, 0, 0, 90, 64, 3, 4, 100, 100, 100, 100, -128, 4, 4, 0, 0, 0, 123, 64, 5, 4, 0, 0, 0, 2, 64, 6, 0, -64, 7, 8, 0, 0, 0, 72, 20, 20, 20, 20, -64, 8, 8, 0, 72, 0, 27, 0, 80, 0, 8, -128, 9, 4, 12, 12, 12, 12, -128, 10, 8, 30, 30, 30, 30, 40, 40, 40, 40, -64, 16, 16, 2, 11, 0, 10, 0, 1, 2, 3, 22, 3, 0, 20, 40, 13, 42, 33, 24, 10, 10, 10, 24, 20, 20, 20, 24, 30, 30, 30
    };

    @BeforeClass
    public static void init() {
        final BGPActivator bgpActivator = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        bgpActivator.start(context);
        routeMonitHandler = new RouteMonitoringMessageHandler(context.getMessageRegistry());
    }

    @Test
    public void testSerializeRouteMonitMessage() throws BMPDeserializationException  {
        final ByteBuf buffer = Unpooled.buffer();
        routeMonitHandler.serializeMessage(createRouteMonitMsg(), buffer);
        final byte[] serializedMsg = new byte[buffer.writerIndex()];
        buffer.readBytes(serializedMsg);
        assertArrayEquals(routeMonitMsgData, serializedMsg);
    }

    @Ignore
    @Test
    public void testParseInitiationMessage() throws BMPDeserializationException {
        final ByteBuf dataWithoutHeader = Unpooled.copiedBuffer(routeMonitMsgData).skipBytes(InitiationHandlerTest.HEADER_LENGTH);
        final InitiationMessage parsedInitMsg = (InitiationMessage) routeMonitHandler.parseMessageBody(dataWithoutHeader);
        assertEquals(createRouteMonitMsg(), parsedInitMsg);
    }

    private static final RouteMonitoringMessage createRouteMonitMsg() {
        final RouteMonitoringMessageBuilder routeMonitMsgBuilder = new RouteMonitoringMessageBuilder()
            .setPeerHeader(createPeerHeader())
            .setUpdate(createUpdate());
        return routeMonitMsgBuilder.build();
    }

    public static final PeerHeader createPeerHeader() {
        PeerHeaderBuilder peerHeaderBuilder = new PeerHeaderBuilder()
            .setAddress(new IpAddress(new Ipv4Address("10.10.10.10")))
            .setAs(new AsNumber(72L))
            .setBgpId(new Ipv4Address("10.10.10.10"))
            .setInboundPolicy(InboundPolicy.forValue(0))
            .setTimestampMicro(new Timestamp(10L))
            .setTimestampSec(new Timestamp(5L))
            .setIpv4(true)
            .setType(Type.forValue(0));
        return peerHeaderBuilder.build();
    }

    private static final Update createUpdate() {
        final UpdateBuilder updateBuilder = new UpdateBuilder()
            .setAttributes(createAttributes())
            .setNlri(createNlri())
            .setWithdrawnRoutes(createWithdrwnRoutes());

        return updateBuilder.build();
    }

    private static final Attributes createAttributes() {
        final SegmentsBuilder segmentsBuilder = new SegmentsBuilder();
        final ASetCaseBuilder aSetCaseBuilder = new ASetCaseBuilder();
        aSetCaseBuilder.setASet(new ASetBuilder().setAsSet(Lists.newArrayList(new AsNumber(72L), new AsNumber(80L), new AsNumber(90L))).build());
        segmentsBuilder.setCSegment(aSetCaseBuilder.build());

        final AttributesBuilder attribBuilder = new AttributesBuilder()
            .setAggregator(new AggregatorBuilder().setAsNumber(new AsNumber(72L)).setNetworkAddress(new Ipv4Address("20.20.20.20")).build())
            .setAigp(new AigpBuilder().setAigpTlv(new AigpTlvBuilder().setMetric(new AccumulatedIgpMetric(BigInteger.ONE)).build()).build())
            .setAsPath(new AsPathBuilder().setSegments(Lists.newArrayList(segmentsBuilder.build())).build())
            .setAtomicAggregate(new AtomicAggregateBuilder().build())
            .setClusterId(new ClusterIdBuilder().setCluster(Lists.newArrayList(new ClusterIdentifier("30.30.30.30"), new ClusterIdentifier(new Ipv4Address("40.40.40.40")))).build())
            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("100.100.100.100")).build()).build())
            .setCommunities(createCommunities())
            .setExtendedCommunities(createExtendedCommunities())
            .setLocalPref(new LocalPrefBuilder().setPref(2L).build())
            .setMultiExitDisc(new MultiExitDiscBuilder().setMed(123L).build())
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setOriginatorId(new OriginatorIdBuilder().setOriginator(new Ipv4Address("12.12.12.12")).build())
            .setUnrecognizedAttributes(createUnrecognizedAttributes());
        return attribBuilder.build();
    }

    private static final List<UnrecognizedAttributes> createUnrecognizedAttributes() {
        final List<UnrecognizedAttributes> unrecogAttribs = Lists.newArrayList();
        final UnrecognizedAttributesBuilder unrecogAttribBuilder = new UnrecognizedAttributesBuilder()
            .setKey(new UnrecognizedAttributesKey((short) 1234))
            .setPartial(true)
            .setTransitive(true)
            .setType((short) 12)
            .setValue(new byte[] { (byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4 });
        final UnrecognizedAttributesBuilder unrecogAttribBuilder2 = new UnrecognizedAttributesBuilder()
            .setKey(new UnrecognizedAttributesKey((short) 2312))
            .setPartial(false)
            .setTransitive(false)
            .setType((short) 75)
            .setValue(new byte[] { (byte) 12, (byte) 11, (byte) 20, (byte) 93, (byte) 43 });
        unrecogAttribs.add(unrecogAttribBuilder.build());
        unrecogAttribs.add(unrecogAttribBuilder2.build());
        return unrecogAttribs;
    }

    private static final List<Communities> createCommunities() {
        final List<Communities> communities = Lists.newArrayList();
        final CommunitiesBuilder commBuilder = new CommunitiesBuilder()
            .setAsNumber(new AsNumber(72L))
            .setSemantics(27);
        final CommunitiesBuilder commBuilder2 = new CommunitiesBuilder()
            .setAsNumber(new AsNumber(80L))
            .setSemantics(8);
        communities.add(commBuilder.build());
        communities.add(commBuilder2.build());
        return communities;
    }

    private static final List<ExtendedCommunities> createExtendedCommunities() {
        final List<ExtendedCommunities> extCommunities = Lists.newArrayList();
        final ExtendedCommunitiesBuilder extCommunitiesBuilder = new ExtendedCommunitiesBuilder()
            .setCommSubType((short) 11)
            .setCommType((short) 2)
            .setExtendedCommunity(new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(new AsSpecificExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(10L)).setLocalAdministrator(new byte[] { (byte) 0, (byte) 1, (byte) 2, (byte) 3 }).setTransitive(true).build()).build());
        final ExtendedCommunitiesBuilder extCommunitiesBuilder2 = new ExtendedCommunitiesBuilder()
            .setCommSubType((short) 3)
            .setCommType((short) 22)
            .setExtendedCommunity(new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(new AsSpecificExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(20L)).setLocalAdministrator(new byte[] { (byte) 40, (byte) 13, (byte) 42, (byte) 33 }).setTransitive(false).build()).build());
        extCommunities.add(extCommunitiesBuilder.build());
        extCommunities.add(extCommunitiesBuilder2.build());
        return extCommunities;
    }

    private static final Nlri createNlri() {
        final NlriBuilder nlriBuilder = new NlriBuilder()
            .setNlri(Lists.newArrayList(new Ipv4Prefix("10.10.10.10/24"), new Ipv4Prefix("20.20.20.20/24"), new Ipv4Prefix("30.30.30.30/24")));
        return nlriBuilder.build();
    }

    private static final WithdrawnRoutes createWithdrwnRoutes() {
        final WithdrawnRoutesBuilder withDrawnBuilder = new WithdrawnRoutesBuilder()
            .setWithdrawnRoutes(Lists.newArrayList(new Ipv4Prefix("10.10.20.20/24"), new Ipv4Prefix("20.20.10.10/24"), new Ipv4Prefix("30.10.10.30/24")));
        return withDrawnBuilder.build();
    }
}
