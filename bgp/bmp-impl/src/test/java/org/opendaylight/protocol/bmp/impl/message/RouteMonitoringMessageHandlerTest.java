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
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.Distinguisher.DistinguisherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.DistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader.InboundPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring.message.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring.message.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.AccumulatedIgpMetric;

public class RouteMonitoringMessageHandlerTest {

    private final MessageRegistry msgRegistry = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry();
    private final RouteMonitoringMessageHandler routeMonitHandler = new RouteMonitoringMessageHandler(msgRegistry);

    /* not correct data yet */
    private final byte[] routeMonitMsgData = {
        /*
         * 03 <- bmp version
         * 00 00 00 4B <- total length of initiation message + common header lenght
         * 04 <- bmp message type
         * 00 02 <- initiation message type SYS_NAME
         * 00 0E <- the length of SYS_NAME
         * 53 79 73 4E 61 6D 65 20 74 79 70 65 20 32 <- value of SYS_NAME
         * 00 01 <- initiation message type SYS_DESCR
         * 00 0F <- the lenght of SYS_DESCR
         * 53 79 73 44 65 73 63 72 20 74 79 70 65 20 31 <- value of SYS_DESCR
         * 00 00 <- initiation message type STRING
         * 00 1C <- the length of STRING
         * 54 68 65 20 69 6E 66 6F 72 6D 61 74 69 6F 6E 20 66 69 65 6C 64 20 74 79 70 65 20 30 <- value of STRING
         */
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x4B, (byte) 0x04, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x0E,
        (byte) 0x53, (byte) 0x79, (byte) 0x73, (byte) 0x4E, (byte) 0x61, (byte) 0x6D, (byte) 0x65, (byte) 0x20, (byte) 0x74, (byte) 0x79,
        (byte) 0x70, (byte) 0x65, (byte) 0x20, (byte) 0x32, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x0F, (byte) 0x53, (byte) 0x79,
        (byte) 0x73, (byte) 0x44, (byte) 0x65, (byte) 0x73, (byte) 0x63, (byte) 0x72, (byte) 0x20, (byte) 0x74, (byte) 0x79, (byte) 0x70,
        (byte) 0x65, (byte) 0x20, (byte) 0x31, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1C, (byte) 0x54, (byte) 0x68, (byte) 0x65,
        (byte) 0x20, (byte) 0x69, (byte) 0x6E, (byte) 0x66, (byte) 0x6F, (byte) 0x72, (byte) 0x6D, (byte) 0x61, (byte) 0x74, (byte) 0x69,
        (byte) 0x6F, (byte) 0x6E, (byte) 0x20, (byte) 0x66, (byte) 0x69, (byte) 0x65, (byte) 0x6C, (byte) 0x64, (byte) 0x20, (byte) 0x74,
        (byte) 0x79, (byte) 0x70, (byte) 0x65, (byte) 0x20, (byte) 0x30
    };

    /* Not yet implemented */
    @Ignore
    @Test
    public void testSerializeRouteMonitMessage() throws BMPDeserializationException  {
        final ByteBuf buffer = Unpooled.buffer();
        routeMonitHandler.serializeMessage(createRouteMonitMsg(), buffer);
        final byte[] serializedMsg = new byte[buffer.writerIndex()];
        buffer.readBytes(serializedMsg);
        assertArrayEquals(routeMonitMsgData, serializedMsg);
    }

    /* Not yet implemented */
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
            .setBgpId(new Ipv4Address("100.100.100.100"))
            .setDistinguisher(new DistinguisherBuilder().setDistinguisherType(DistinguisherType.forValue(0)).setDistinguisher("test").build())
            .setInboundPolicy(InboundPolicy.forValue(1))
            .setTimestampMicro(new Timestamp(0L))
            .setTimestampSec(new Timestamp(1L))
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
