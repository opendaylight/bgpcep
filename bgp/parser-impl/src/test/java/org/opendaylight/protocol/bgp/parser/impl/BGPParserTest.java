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
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunityUtil;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.util.HexDumpBGPFileParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.NoopReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AtomicAggregateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.ipv4._case.RouteTargetIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;

public class BGPParserTest {

    /**
     * Used by other tests as well
     */
    static final List<byte[]> inputBytes = new ArrayList<>();

    private static int COUNTER = 7;

    private static int MAX_SIZE = 300;

    private static BGPUpdateMessageParser updateParser;

    private static final int LENGTH_FIELD_LENGTH = 2;

    private static final String multiPathHexFile = "/bgp-update-multipath.txt";

    private static List<byte[]> updatesWithMultiplePath;

    private static PeerSpecificParserConstraint constraint;

    private static MultiPathSupport mpSupport;

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
        updatesWithMultiplePath = HexDumpBGPFileParser.parseMessages(BGPParserTest.class.getResourceAsStream(multiPathHexFile));
        constraint = mock(PeerSpecificParserConstraint.class);
        mpSupport = mock(MultiPathSupport.class);
        Mockito.doReturn(Optional.of(mpSupport)).when(constraint).getPeerConstraint(Mockito.any());
        Mockito.doReturn(true).when(mpSupport).isTableTypeSupported(Mockito.any());
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
        final List<AsNumber> asNumbers = new ArrayList<>();
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

        final List<Nlri> nlris = Lists.newArrayList();
        nlris.add(new NlriBuilder().setPrefix(new Ipv4Prefix("172.17.2.0/24")).build());
        nlris.add(new NlriBuilder().setPrefix(new Ipv4Prefix("172.17.1.0/24")).build());
        nlris.add(new NlriBuilder().setPrefix(new Ipv4Prefix("172.17.0.0/24")).build());

        assertEquals(nlris, message.getNlri());

        builder.setNlri(nlris);

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

        paBuilder.setUnrecognizedAttributes(Collections.emptyList());

        builder.setAttributes(paBuilder.build());

        assertEquals(builder.build(), message);

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(0), ByteArray.readAllBytes(buffer));
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
        final List<Nlri> nlris = Lists.newArrayList(new NlriBuilder().setPrefix(new Ipv4Prefix("172.16.0.0/21")).build());
        builder.setNlri(nlris);
        assertEquals(builder.getNlri(), message.getNlri());

        // check fields
        assertNull(message.getWithdrawnRoutes());

        // attributes
        final List<AsNumber> asNumbers = new ArrayList<>();
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
        paBuilder.setUnrecognizedAttributes(Collections.emptyList());
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
        final List<Nlri> nlris = Lists.newArrayList();
        nlris.add(new NlriBuilder().setPrefix(new Ipv4Prefix("10.30.3.0/24")).build());
        nlris.add(new NlriBuilder().setPrefix(new Ipv4Prefix("10.30.2.0/24")).build());
        nlris.add(new NlriBuilder().setPrefix(new Ipv4Prefix("10.30.1.0/24")).build());

        assertEquals(nlris, message.getNlri());

        builder.setNlri(nlris);

        // attributes
        final Ipv4NextHopCase nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(
                new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("3.3.3.3")).build()).build();

        final List<ExtendedCommunities> comms = Lists.newArrayList();
        comms.add(new ExtendedCommunitiesBuilder().setTransitive(true).setExtendedCommunity(
                new RouteTargetIpv4CaseBuilder().setRouteTargetIpv4(
                        new RouteTargetIpv4Builder().setGlobalAdministrator(
                                new Ipv4Address("192.168.1.0")).setLocalAdministrator(4660).build()).build()).build());

        // check path attributes
        final Attributes attrs = message.getAttributes();

        final AttributesBuilder paBuilder = new AttributesBuilder();

        paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build());
        assertEquals(paBuilder.getOrigin(), attrs.getOrigin());

        paBuilder.setAsPath(new AsPathBuilder().setSegments(Collections.emptyList()).build());
        assertEquals(paBuilder.getAsPath(), attrs.getAsPath());

        paBuilder.setCNextHop(nextHop);
        assertEquals(paBuilder.getCNextHop(), attrs.getCNextHop());

        paBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed((long) 0).build());
        assertEquals(paBuilder.getMultiExitDisc(), attrs.getMultiExitDisc());

        paBuilder.setLocalPref(new LocalPrefBuilder().setPref(100L).build());
        assertEquals(paBuilder.getLocalPref(), attrs.getLocalPref());

        paBuilder.setExtendedCommunities(comms);
        assertEquals(paBuilder.getExtendedCommunities(), attrs.getExtendedCommunities());

        paBuilder.setUnrecognizedAttributes(Collections.emptyList());
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
        final List<WithdrawnRoutes> withdrawnRoutes = Lists.newArrayList(new WithdrawnRoutesBuilder().setPrefix(new Ipv4Prefix("172.16.0.4/30")).build());

        // check API message
        final Update expectedMessage = new UpdateBuilder().setWithdrawnRoutes(withdrawnRoutes).build();

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

    /*
     * Tests IPv4 NEXT_HOP, ATOMIC_AGGREGATE, COMMUNITY, NLRI with multiple paths.
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 60 <- length (96) - including header
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
     * 0a 00 00 02 <- value (10.0.0.2)
     * 80 <- attribute flags
     * 04 <- attribute type code (multi exit disc)
     * 04 <- attribute length
     * 00 00 00 00 <- value
     * 40 <- attribute flags
     * 06 <- attribute type code (atomic aggregate)
     * 00 <- attribute length
     * C0 <- attribute flags
     * 08 <- attribute type code (community)
     * 10 <- attribute length
     * FF FF FF 01 <- value (NO_EXPORT)
     * FF FF FF 02 <- value (NO_ADVERTISE)
     * FF FF FF 03 <- value (NO_EXPORT_SUBCONFED)
     * FF FF FF 10 <- unknown Community
     *
     * //NLRI
     * 00 00 00 01 <- path-id (1)
     * 18 ac 11 02 <- IPv4 Prefix (172.17.1.0 / 24)
     * 00 00 00 01 <- path-id (2)
     * 18 ac 11 01 <- IPv4 Prefix (172.17.1.0 / 24)
     * 00 00 00 01 <- path-id (1)
     * 18 ac 11 00 <- IPv4 Prefix (172.17.0.0 / 24)
     */
    @Test
    public void testUpdateMessageNlriAddPath() throws Exception {
        final byte[] body = ByteArray.cutBytes(updatesWithMultiplePath.get(0), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(updatesWithMultiplePath.get(0), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = BGPParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength, constraint);

        // check fields

        assertNull(message.getWithdrawnRoutes());

        // attributes
        final List<AsNumber> asNumbers = new ArrayList<>();
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

        final List<Nlri> nlris = Lists.newArrayList();
        nlris.add(new NlriBuilder().setPrefix(new Ipv4Prefix("172.17.1.0/24")).setPathId(new PathId(1L)).build());
        nlris.add(new NlriBuilder().setPrefix(new Ipv4Prefix("172.17.1.0/24")).setPathId(new PathId(2L)).build());
        nlris.add(new NlriBuilder().setPrefix(new Ipv4Prefix("172.17.0.0/24")).setPathId(new PathId(1L)).build());

        assertEquals(nlris, message.getNlri());

        builder.setNlri(nlris);

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

        paBuilder.setUnrecognizedAttributes(Collections.emptyList());

        builder.setAttributes(paBuilder.build());

        assertEquals(builder.build(), message);

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(updatesWithMultiplePath.get(0), ByteArray.readAllBytes(buffer));
    }

    /*
     * Tests withdrawn routes with multiple paths.
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 29 <- length (41) - including header
     * 02 <- message type
     * 00 12 <- withdrawn routes length (18)
     * 00 00 00 01 <- path-id (1)
     * 1e ac 10 00 04 <- route (172.16.0.4)
     * 00 00 00 02 <- path-id (2)
     * 1e ac 10 00 04 <- route (172.16.0.4)
     * 00 00 <- total path attribute length
     */
    @Test
    public void testUpdateMessageWithdrawAddPath() throws Exception {
        final byte[] body = ByteArray.cutBytes(updatesWithMultiplePath.get(1), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(updatesWithMultiplePath.get(1), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = BGPParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength, constraint);

        // attributes
        final List<WithdrawnRoutes> withdrawnRoutes = Lists.newArrayList();
        withdrawnRoutes.add(new WithdrawnRoutesBuilder().setPrefix(new Ipv4Prefix("172.16.0.4/30")).setPathId(new PathId(1L)).build());
        withdrawnRoutes.add(new WithdrawnRoutesBuilder().setPrefix(new Ipv4Prefix("172.16.0.4/30")).setPathId(new PathId(2L)).build());

        // check API message
        final Update expectedMessage = new UpdateBuilder().setWithdrawnRoutes(withdrawnRoutes).build();

        assertEquals(expectedMessage.getWithdrawnRoutes(), message.getWithdrawnRoutes());

        final ByteBuf buffer = Unpooled.buffer();
        BGPParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(updatesWithMultiplePath.get(1), ByteArray.readAllBytes(buffer));
    }
}
