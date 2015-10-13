/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunityUtil;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.NoopReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AtomicAggregateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;

public class BGPParserTest {

    /**
     * Used by other tests as well
     */
    static final List<byte[]> inputBytes = new ArrayList<byte[]>();

    private static int COUNTER = 7;

    private static int MAX_SIZE = 300;

    private static BGPUpdateMessageParser updateParser;

    private static final int LENGTH_FIELD_LENGTH = 2;

    @BeforeClass
    public static void setUp() throws Exception {
        updateParser = new BGPUpdateMessageParser(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry());
        for (int i = 1; i <= COUNTER; i++) {
            final String name = "/up" + i + ".bin";
            try (final InputStream is = BGPParserTest.class.getResourceAsStream(name)){
                if (is == null) {
                    throw new IOException("Failed to get resource " + name);
                }
                final ByteArrayOutputStream bis = new ByteArrayOutputStream();
                final byte[] data = new byte[MAX_SIZE];
                int nRead = 0;
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    bis.write(data, 0, nRead);
                }
                bis.flush();

                inputBytes.add(bis.toByteArray());
                is.close();
            }
        }
    }


    @Test
    public void testResource() {
        assertNotNull(inputBytes);
    }

    /*
     * Tests IPv4 NEXT_HOP, ATOMIC_AGGREGATE, COMMUNITY, NLRI
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 54 <- length (84) - including header
     * 02 <- message type
     * 00 00 <- withdrawn routes length
     * 00 31 <- total path attribute length (49)
     * 40 <- attribute flags
     * 01 <- attribute type code (origin)
     * 01 <- attribute length
     * 00 <- Origin value (IGP)
     * 40 <- attribute flags
     * 02 <- attribute type code (as path)
     * 06 <- attribute length
     * 02 <- AS_SEQUENCE
     * 01 <- path segment count
     * 00 00 fd ea <- path segment value (65002)
     * 40 <- attribute flags
     * 03 <- attribute type code (Next Hop)
     * 04 <- attribute length
     * 10 00 00 02 <- value (10.0.0.2)
     * 80 <- attribute flags
     * 04 <- attribute type code (multi exit disc)
     * 04 <- attribute length
     * 00 00 00 00 <- value
     * 60 <- attribute flags
     * 06 <- attribute type code (atomic aggregate)
     * 00 <- attribute length
     * 40 <- attribute flags
     * 08 <- attribute type code (community)
     * 10 <- attribute length FF FF FF
     * 01 <- value (NO_EXPORT)
     * FF FF FF 02 <- value (NO_ADVERTISE)
     * FF FF FF 03 <- value (NO_EXPORT_SUBCONFED)
     * FF FF FF 10 <- unknown Community
     *
     * //NLRI
     * 18 ac 11 02 <- IPv4 Prefix (172.17.2.0 / 24)
     * 18 ac 11 01 <- IPv4 Prefix (172.17.1.0 / 24)
     * 18 ac 11 00 <- IPv4 Prefix (172.17.0.0 / 24)
     */
    @Test
    public void testGetUpdateMessage1() throws Exception {

        final byte[] body = ByteArray.cutBytes(inputBytes.get(0), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(0), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = BGPParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        // check fields

        assertNull(message.getWithdrawnRoutes());

        // attributes
        final List<AsNumber> asNumbers = new ArrayList<AsNumber>();
        asNumbers.add(new AsNumber(65002L));
        final List<Segments> asPath = Lists.newArrayList();
        asPath.add(new SegmentsBuilder().setAsSequence(asNumbers).build());

        final Ipv4NextHopCase nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(
            new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("10.0.0.2")).build()).build();

        final List<Communities> comms = Lists.newArrayList();
        comms.add((Communities) CommunityUtil.NO_EXPORT);
        comms.add((Communities) CommunityUtil.NO_ADVERTISE);
        comms.add((Communities) CommunityUtil.NO_EXPORT_SUBCONFED);
        comms.add((Communities) CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0xFF10));

        final UpdateBuilder builder = new UpdateBuilder();

        // check nlri

        final List<Ipv4Prefix> prefs = Lists.newArrayList();
        prefs.add(new Ipv4Prefix("172.17.2.0/24"));
        prefs.add(new Ipv4Prefix("172.17.1.0/24"));
        prefs.add(new Ipv4Prefix("172.17.0.0/24"));

        final Nlri nlri = new NlriBuilder().setNlri(prefs).build();

        assertEquals(nlri, message.getNlri());

        builder.setNlri(nlri);

        // check path attributes

        final Attributes attrs = message.getAttributes();

        final AttributesBuilder paBuilder = new AttributesBuilder();

        paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build());
        assertEquals(paBuilder.getOrigin(), attrs.getOrigin());

        paBuilder.setAsPath(new AsPathBuilder().setSegments(asPath).build());
        assertEquals(paBuilder.getAsPath(), attrs.getAsPath());

        paBuilder.setCNextHop(nextHop);
        assertEquals(paBuilder.getCNextHop(), attrs.getCNextHop());

        paBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed((long) 0).build());
        assertEquals(paBuilder.getMultiExitDisc(), attrs.getMultiExitDisc());

        paBuilder.setAtomicAggregate(new AtomicAggregateBuilder().build());
        assertEquals(paBuilder.getAtomicAggregate(), attrs.getAtomicAggregate());

        paBuilder.setCommunities(comms);
        assertEquals(paBuilder.getCommunities(), attrs.getCommunities());

        paBuilder.setUnrecognizedAttributes(Collections.<UnrecognizedAttributes> emptyList());

        builder.setAttributes(paBuilder.build());

        assertEquals(builder.build(), message);

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(0), ByteArray.readAllBytes(buffer));
    }

    /*
     * Tests IPv6 NEXT_HOP, NLRI, ORIGIN.IGP, MULTI_EXIT_DISC, ORIGINATOR-ID, CLUSTER_LIST.
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 70 <- length (112) - including header
     * 02 <- message type
     * 00 00 <- withdrawn routes length
     * 00 59 <- total path attribute length (89)
     * 40 <- attribute flags
     * 01 <- attribute type code (origin)
     * 01 <- attribute length
     * 00 <- Origin value (IGP)
     * 40 <- attribute flags
     * 02 <- attribute type code (as path)
     * 06 <- attribute length
     * 02 <- AS_SEQUENCE
     * 01 <- path segment count
     * 00 00 fd e9 <- path segment value (65001)
     * 40 <- attribute flags
     * 03 <- attribute type code (next hop)
     * 04 <- attribute length
     * 0a 00 00 00 <- next hop value (10.0.0.0)
     * 80 <- attribute flags
     * 04 <- attribute type code (multi exit disc)
     * 04 <- attribute length
     * 00 00 00 00 <- value
     * 80 <- attribute flags
     * 09 <- attribute type code (originator id)
     * 04 <- attribute length
     * 7f 00 00 01 <- value (localhost ip)
     * 80 <- attribute flags
     * 0a <- attribute type code (cluster list)
     * 08 <- attribute length
     * 01 02 03 04 <- value
     * 05 06 07 08 <- value
     * 80 <- attribute flags
     * 0e <- attribute type code (mp reach nlri)
     * 30 <- attribute length
     * 00 02 <- AFI (Ipv6)
     * 01 <- SAFI (Unicast)
     * 20 <- length of next hop
     * 10 01 0d b8 00 00 00 00 00 00 00 00 00 00 00 01 <- global
     * 00 <- reserved
     *
     * //NLRI
     * 40 20 01 0d b8 00 01 00 02 <- IPv6 Prefix (2001:db8:1:2:: / 64)
     * 40 20 01 0d b8 00 01 00 01 <- IPv6 Prefix (2001:db8:1:1:: / 64)
     * 40 20 01 0d b8 00 01 00 00 <- IPv6 Prefix (2001:db8:1:: / 64)
     *
     */
    @Test
    public void testGetUpdateMessage2() throws Exception {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(1), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(1), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = BGPParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        // check fields
        assertNull(message.getWithdrawnRoutes());

        final UpdateBuilder builder = new UpdateBuilder();

        // check NLRI

        final List<Ipv6Prefixes> prefs = new ArrayList<>();
        prefs.add(new Ipv6PrefixesBuilder().setPrefix(new Ipv6Prefix("2001:db8:1:2::/64")).build());
        prefs.add(new Ipv6PrefixesBuilder().setPrefix(new Ipv6Prefix("2001:db8:1:1::/64")).build());
        prefs.add(new Ipv6PrefixesBuilder().setPrefix(new Ipv6Prefix("2001:db8:1::/64")).build());

        assertNull(message.getNlri());

        // attributes
        final List<AsNumber> asNumbers = new ArrayList<AsNumber>();
        asNumbers.add(new AsNumber(65001L));
        final List<Segments> asPath = Lists.newArrayList();
        asPath.add(new SegmentsBuilder().setAsSequence(asNumbers).build());

        final Ipv6NextHopCase nextHop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(
            new Ipv6NextHopBuilder().setGlobal(new Ipv6Address("2001:db8::1")).build()).build();

        final List<ClusterIdentifier> clusters = Lists.newArrayList(new ClusterIdentifier(new Ipv4Address("1.2.3.4")),
            new ClusterIdentifier(new Ipv4Address("5.6.7.8")));

        // check path attributes

        final Attributes attrs = message.getAttributes();

        final AttributesBuilder paBuilder = new AttributesBuilder();

        paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build());
        assertEquals(paBuilder.getOrigin(), attrs.getOrigin());

        paBuilder.setAsPath(new AsPathBuilder().setSegments(asPath).build());
        assertEquals(paBuilder.getAsPath(), attrs.getAsPath());

        paBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed((long) 0).build());
        assertEquals(paBuilder.getMultiExitDisc(), attrs.getMultiExitDisc());

        paBuilder.setOriginatorId(new OriginatorIdBuilder().setOriginator(new Ipv4Address("127.0.0.1")).build());
        assertEquals(paBuilder.getOriginatorId(), attrs.getOriginatorId());

        paBuilder.setClusterId(new ClusterIdBuilder().setCluster(clusters).build());
        assertEquals(paBuilder.getClusterId(), attrs.getClusterId());

        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(UnicastSubsequentAddressFamily.class);
        mpBuilder.setCNextHop(nextHop);
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv6CaseBuilder().setDestinationIpv6(new DestinationIpv6Builder().setIpv6Prefixes(prefs).build()).build()).build());

        paBuilder.addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mpBuilder.build()).build());
        assertEquals(paBuilder.getAugmentation(Attributes1.class).getMpReachNlri(),
            attrs.getAugmentation(Attributes1.class).getMpReachNlri());
        paBuilder.setUnrecognizedAttributes(Collections.<UnrecognizedAttributes> emptyList());
        // check API message

        builder.setAttributes(paBuilder.build());
        assertEquals(builder.build(), message);

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(1), ByteArray.readAllBytes(buffer));
    }

    /*
     * Tests more AS Numbers in AS_PATH, AGGREGATOR, ORIGIN.INCOMPLETE
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 4b <- length (75) - including header
     * 02 <- message type
     * 00 00 <- withdrawn routes length
     * 00 30 <- total path attribute length (48)
     * 40 <- attribute flags
     * 01 <- attribute type code (origin)
     * 01 <- attribute length
     * 02 <- Origin value (Incomplete)
     * 40 <- attribute flags
     * 02 <- attribute type code (as path)
     * 10 <- attribute length
     * 02 <- AS_SEQUENCE
     * 01 <- path segment count
     * 00 00 00 1e <- path segment value (30)
     * 01 <- AS_SET
     * 02 <- path segment count
     * 00 00 00 0a <- path segment value (10)
     * 00 00 00 14 <- path segment value (20)
     * 40 <- attribute flags
     * 03 <- attribute type (Next hop)
     * 04 <- attribute length
     * 0a 00 00 09 <- value (10.0.0.9)
     * 80 <- attribute flags
     * 04 <- attribute type code (multi exit disc)
     * 04 <- attribute length
     * 00 00 00 00 <- value
     * c0 <- attribute flags
     * 07 <- attribute type (Aggregator)
     * 08 <- attribute length
     * 00 00 00 1e <- value (AS number = 30)
     * 0a 00 00 09 <- value (IP address = 10.0.0.9)
     *
     * //NLRI
     * 15 ac 10 00 <- IPv4 Prefix (172.16.0.0 / 21)
     */
    @Test
    public void testGetUpdateMessage3() throws Exception {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(2), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(2), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = BGPParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        final UpdateBuilder builder = new UpdateBuilder();

        // check nlri
        final Ipv4Prefix pref1 = new Ipv4Prefix("172.16.0.0/21");

        final List<Ipv4Prefix> nlri = Lists.newArrayList(pref1);
        builder.setNlri(new NlriBuilder().setNlri(nlri).build());
        assertEquals(builder.getNlri(), message.getNlri());

        // check fields
        assertNull(message.getWithdrawnRoutes());

        // attributes
        final List<AsNumber> asNumbers = new ArrayList<AsNumber>();
        asNumbers.add(new AsNumber(30L));
        final List<Segments> asPath = Lists.newArrayList();
        asPath.add(new SegmentsBuilder().setAsSequence(asNumbers).build());
        final List<AsNumber> asSet = Lists.newArrayList(new AsNumber(10L), new AsNumber(20L));
        asPath.add(new SegmentsBuilder().setAsSet(asSet).build());

        final Aggregator aggregator = new AggregatorBuilder().setAsNumber(new AsNumber((long) 30)).setNetworkAddress(
            new Ipv4Address("10.0.0.9")).build();
        final Ipv4NextHopCase nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(
            new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("10.0.0.9")).build()).build();

        // check path attributes
        final Attributes attrs = message.getAttributes();

        final AttributesBuilder paBuilder = new AttributesBuilder();

        paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Incomplete).build());
        assertEquals(paBuilder.getOrigin(), attrs.getOrigin());

        paBuilder.setAsPath(new AsPathBuilder().setSegments(asPath).build());
        assertEquals(paBuilder.getAsPath(), attrs.getAsPath());

        paBuilder.setCNextHop(nextHop);
        assertEquals(paBuilder.getCNextHop(), attrs.getCNextHop());

        paBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed((long) 0).build());
        assertEquals(paBuilder.getMultiExitDisc(), attrs.getMultiExitDisc());

        paBuilder.setAggregator(aggregator);
        assertEquals(paBuilder.getAggregator(), attrs.getAggregator());
        paBuilder.setUnrecognizedAttributes(Collections.<UnrecognizedAttributes> emptyList());
        builder.setAttributes(paBuilder.build());

        assertEquals(builder.build(), message);

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(2), ByteArray.readAllBytes(buffer));
    }

    /*
     * Tests empty AS_PATH, ORIGIN.EGP, LOCAL_PREF, EXTENDED_COMMUNITIES (Ipv4 Addr specific)
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 4A <- length (73) - including header
     * 02 <- message type
     * 00 00 <- withdrawn routes length
     * 00 27 <- total path attribute length (39)
     * 40 <- attribute flags
     * 01 <- attribute type code (Origin)
     * 01 <- attribute length
     * 01 <- Origin value (EGP)
     * 40 <- attribute flags
     * 02 <- attribute type code (As path)
     * 00 <- attribute length
     * 40 <- attribute flags
     * 03 <- attribute type (Next hop)
     * 04 <- attribute length
     * 03 03 03 03 <- value (3.3.3.3)
     * 80 <- attribute flags
     * 04 <- attribute type code (Multi exit disc)
     * 04 <- attribute length
     * 00 00 00 00 <- value
     * 40 <- attribute flags
     * 05 <- attribute type (Local Pref)
     * 04 <- attribute length
     * 00 00 00 64 <- value (100)
     * c0 <- attribute flags
     * 10 <- attribute type (extended community)
     * 08 <- attribute length
     * 01 04 <- value (type - Ipv4 Address Specific Extended Community)
     * c0 a8 01 00 <- value (global adm. 198.162.1.0)
     * 12 34 <- value (local adm. 4660)
     *
     * //NLRI
     * 18 0a 1e 03 <- IPv4 Prefix (10.30.3.0 / 24)
     * 18 0a 1e 02 <- IPv4 Prefix (10.30.2.0 / 24)
     * 18 0a 1e 01 <- IPv4 Prefix (10.30.1.0 / 24)
     */
    @Test
    public void testGetUpdateMessage4() throws Exception {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(3), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(3), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = BGPParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        final UpdateBuilder builder = new UpdateBuilder();

        // check fields
        assertNull(message.getWithdrawnRoutes());

        // check nlri
        final Ipv4Prefix pref1 = new Ipv4Prefix("10.30.3.0/24");
        final Ipv4Prefix pref2 = new Ipv4Prefix("10.30.2.0/24");
        final Ipv4Prefix pref3 = new Ipv4Prefix("10.30.1.0/24");

        final List<Ipv4Prefix> nlri = Lists.newArrayList(pref1, pref2, pref3);
        builder.setNlri(new NlriBuilder().setNlri(nlri).build());
        assertEquals(builder.getNlri(), message.getNlri());

        // attributes
        final Ipv4NextHopCase nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(
            new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("3.3.3.3")).build()).build();

        final List<ExtendedCommunities> comms = Lists.newArrayList();
        comms.add(new ExtendedCommunitiesBuilder().setCommType((short) 1).setCommSubType((short) 4).setExtendedCommunity(
            new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                new Inet4SpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(
                    new Ipv4Address("192.168.1.0")).setLocalAdministrator(new byte[] { 0x12, 0x34 }).build()).build()).build());

        // check path attributes
        final Attributes attrs = message.getAttributes();

        final AttributesBuilder paBuilder = new AttributesBuilder();

        paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build());
        assertEquals(paBuilder.getOrigin(), attrs.getOrigin());

        paBuilder.setAsPath(new AsPathBuilder().setSegments(Collections.<Segments> emptyList()).build());
        assertEquals(paBuilder.getAsPath(), attrs.getAsPath());

        paBuilder.setCNextHop(nextHop);
        assertEquals(paBuilder.getCNextHop(), attrs.getCNextHop());

        paBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed((long) 0).build());
        assertEquals(paBuilder.getMultiExitDisc(), attrs.getMultiExitDisc());

        paBuilder.setLocalPref(new LocalPrefBuilder().setPref(100L).build());
        assertEquals(paBuilder.getLocalPref(), attrs.getLocalPref());

        paBuilder.setExtendedCommunities(comms);
        assertEquals(paBuilder.getExtendedCommunities(), attrs.getExtendedCommunities());

        paBuilder.setUnrecognizedAttributes(Collections.<UnrecognizedAttributes> emptyList());
        // check API message
        builder.setAttributes(paBuilder.build());
        assertEquals(builder.build(), message);

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(3), ByteArray.readAllBytes(buffer));
    }

    /*
     * Tests withdrawn routes.
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 1c <- length (28) - including header
     * 02 <- message type
     * 00 05 <- withdrawn routes length (5)
     * 1e ac 10 00 04 <- route (172.16.0.4)
     * 00 00 <- total path attribute length
     */
    @Test
    public void testGetUpdateMessage5() throws Exception {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(4), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(4), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = BGPParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        // attributes
        final List<Ipv4Prefix> prefs = Lists.newArrayList(new Ipv4Prefix("172.16.0.4/30"));

        // check API message
        final Update expectedMessage = new UpdateBuilder().setWithdrawnRoutes(
            new WithdrawnRoutesBuilder().setWithdrawnRoutes(prefs).build()).build();

        assertEquals(expectedMessage.getWithdrawnRoutes(), message.getWithdrawnRoutes());

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(4), ByteArray.readAllBytes(buffer));
    }

    /*
     * End of Rib for Ipv4.
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 17 <- length (23) - including header
     * 02 <- message type
     * 00 00 <- withdrawn routes length
     * 00 00 <- total path attribute length
     */
    @Test
    public void testEORIpv4() throws Exception {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(5), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(5), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = BGPParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        assertEquals(new UpdateBuilder().build(), message);

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(5), ByteArray.readAllBytes(buffer));
    }

    /*
     * End of Rib for Ipv6 consists of empty MP_UNREACH_NLRI, with AFI 2 and SAFI 1
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 1d <- length (29) - including header
     * 02 <- message type
     * 00 00 <- withdrawn routes length
     * 00 06 <- total path attribute length
     * 80 <- attribute flags
     * 0f <- attribute type (15 - MP_UNREACH_NLRI)
     * 03 <- attribute length
     * 00 02 <- value (AFI 2: IPv6)
     * 01 <- value (SAFI 1)
     */
    @Test
    public void testEORIpv6() throws Exception {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(6), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(6), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = BGPParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        final Class<? extends AddressFamily> afi = message.getAttributes().getAugmentation(Attributes2.class).getMpUnreachNlri().getAfi();
        final Class<? extends SubsequentAddressFamily> safi = message.getAttributes().getAugmentation(Attributes2.class).getMpUnreachNlri().getSafi();

        assertEquals(Ipv6AddressFamily.class, afi);
        assertEquals(UnicastSubsequentAddressFamily.class, safi);

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(6), ByteArray.readAllBytes(buffer));
    }

    /*
     * End of Rib for Ipv6 consists of empty MP_UNREACH_NLRI, with AFI 2 and SAFI 1
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 1e <- length (29) - including header
     * 02 <- message type
     * 00 00 <- withdrawn routes length
     * 00 07 <- total path attribute length
     * 90 <- attribute flags
     * 0f <- attribute type (15 - MP_UNREACH_NLRI)
     * 00 03 <- attribute length
     * 00 02 <- value (AFI 2: IPv6)
     * 01 <- value (SAFI 1)
     */
    @Test
    public void testEORIpv6exLength() throws Exception {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(6), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(6), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = BGPParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        final Class<? extends AddressFamily> afi = message.getAttributes().getAugmentation(Attributes2.class).getMpUnreachNlri().getAfi();
        final Class<? extends SubsequentAddressFamily> safi = message.getAttributes().getAugmentation(Attributes2.class).getMpUnreachNlri().getSafi();

        assertEquals(Ipv6AddressFamily.class, afi);
        assertEquals(UnicastSubsequentAddressFamily.class, safi);

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(6), ByteArray.readAllBytes(buffer));
    }
}
