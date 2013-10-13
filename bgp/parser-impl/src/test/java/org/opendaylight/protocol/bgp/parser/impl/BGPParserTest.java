/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPMessageFactory;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.concepts.IGPMetric;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.concepts.Metric;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.DefaultingTypesafeContainer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AsPathSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.CAListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.CASetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.c.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.c.a.list.AsSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CInet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.inet4.specific.extended.community.Inet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv4NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv6NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv6NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.c.ipv4.next.hop.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.c.ipv6.next.hop.Ipv6NextHopBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class BGPParserTest {

	/**
	 * Used by other tests as well
	 */
	static final List<byte[]> inputBytes = new ArrayList<byte[]>();

	private static int COUNTER = 17;

	private static int MAX_SIZE = 300;

	private final BGPUpdateMessageParser updateParser = BGPUpdateMessageParser.PARSER;

	@BeforeClass
	public static void setUp() throws Exception {

		for (int i = 1; i <= COUNTER; i++) {
			final String name = "/up" + i + ".bin";
			final InputStream is = BGPParserTest.class.getResourceAsStream(name);
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
	 * 64 <- attribute flags
	 * 06 <- attribute type code (atomic aggregate)
	 * 00 <- attribute length
	 * 64 <- attribute flags
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
	@Ignore
	// FIXME: to be fixed in testing phase
	public void testGetUpdateMessage1() throws Exception {

		final byte[] body = ByteArray.cutBytes(inputBytes.get(0), MessageUtil.COMMON_HEADER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(0), MessageUtil.MARKER_LENGTH,
				MessageUtil.LENGTH_FIELD_LENGTH));
		final Update message = updateParser.parseMessageBody(body, messageLength);

		// check fields

		assertNull(message.getWithdrawnRoutes());

		// attributes

		final List<AsSequence> asnums = Lists.newArrayList(new AsSequenceBuilder().setAs(new AsNumber(65002L)).build());
		final List<AsPathSegment> asPath = Lists.newArrayList();
		asPath.add(new SegmentsBuilder().setCSegment(new CAListBuilder().setAsSequence(asnums).build()).build());

		final CIpv4NextHop nextHop = new CIpv4NextHopBuilder().setIpv4NextHop(
				new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("10.0.0.2")).build()).build();

		final Set<Community> comms = new HashSet<>();
		comms.add(CommunityUtil.NO_EXPORT);
		comms.add(CommunityUtil.NO_ADVERTISE);
		comms.add(CommunityUtil.NO_EXPORT_SUBCONFED);
		comms.add(CommunityUtil.create(0xFFFF, 0xFF10));

		// check path attributes

		// final PathAttribute originAttr = new PathAttribute(TypeCode.ORIGIN, false,
		// true, false, false, BGPOrigin.IGP);
		// assertEquals(originAttr, attrs.get(0));
		//
		// final PathAttribute asPathAttr = new PathAttribute(TypeCode.AS_PATH, false,
		// true, false, false, asPath);
		// assertEquals(asPathAttr, attrs.get(1));
		//
		// final PathAttribute nextHopAttr = new PathAttribute(TypeCode.NEXT_HOP, false,
		// true, false, false, nextHop);
		// assertEquals(nextHopAttr, attrs.get(2));
		//
		// final PathAttribute multiExitDisc = new PathAttribute(
		// TypeCode.MULTI_EXIT_DISC, true, false, false, false, 0);
		// assertEquals(multiExitDisc, attrs.get(3));
		//
		// final PathAttribute atomic = new PathAttribute(TypeCode.ATOMIC_AGGREGATE, false,
		// true, true, false, null);
		// assertEquals(atomic, attrs.get(4));
		//
		// final PathAttribute comm = new PathAttribute(TypeCode.COMMUNITIES, false,
		// true, true, false, comms);
		// assertEquals(comm, attrs.get(5));

		// check nlri

		// final Set<IPv4Prefix> nlri = Sets.newHashSet(pref1, pref2, pref3);
		// assertEquals(nlri, ret.getBgpUpdateMessageBuilder().getNlri());

		//		final BaseBGPObjectState state = new BaseBGPObjectState(BgpOrigin.Igp, null);
		//		final NetworkRouteState routeState = new NetworkRouteState(new NetworkObjectState(asPath, comms, Collections.<ExtendedCommunity> emptySet()), nextHop);

		// check API message

		//		final Set<BGPObject> addedObjects = Sets.newHashSet();
		//
		//		final BGPRoute route1 = new BGPIPv4RouteImpl(IPv4.FAMILY.prefixForString("172.17.2.0/24"), state, routeState);
		//
		//		addedObjects.add(route1);
		//
		//		final BGPRoute route2 = new BGPIPv4RouteImpl(IPv4.FAMILY.prefixForString("172.17.1.0/24"), state, routeState);
		//
		//		addedObjects.add(route2);
		//
		//		final BGPRoute route3 = new BGPIPv4RouteImpl(IPv4.FAMILY.prefixForString("172.17.0.0/24"), state, routeState);
		//
		//		addedObjects.add(route3);
	}

	/*
	 * Tests IPv6 NEXT_HOP, NLRI, ORIGIN.IGP, MULTI_EXIT_DISC, ORIGINATOR-ID, CLUSTER_LIST.
	 * 
	 * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
	 * 00 80 <- length (128) - including header
	 * 02 <- message type
	 * 00 00 <- withdrawn routes length
	 * 00 69 <- total path attribute length (105)
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
	 * 40 <- attribute length
	 * 00 02 <- AFI (Ipv6)
	 * 01 <- SAFI (Unicast)
	 * 20 <- length of next hop
	 * 20 01 0d b8 00 00 00 00 00 00 00 00 00 00 00 01 <- global
	 * fe 80 00 00 00 00 00 00 c0 01 0b ff fe 7e 00 <- link local
	 * 00 <- reserved
	 * 
	 * //NLRI
	 * 40 20 01 0d b8 00 01 00 02 <- IPv6 Prefix (2001:db8:1:2:: / 64)
	 * 40 20 01 0d b8 00 01 00 01 <- IPv6 Prefix (2001:db8:1:1:: / 64)
	 * 40 20 01 0d b8 00 01 00 00 <- IPv6 Prefix (2001:db8:1:: / 64)
	 * 
	 */
	@Test
	@Ignore
	// FIXME: to be fixed in testing phase
	public void testGetUpdateMessage2() throws Exception {
		final byte[] body = ByteArray.cutBytes(inputBytes.get(1), MessageUtil.COMMON_HEADER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(1), MessageUtil.MARKER_LENGTH,
				MessageUtil.LENGTH_FIELD_LENGTH));
		final Update message = updateParser.parseMessageBody(body, messageLength);
		// check fields

		assertNull(message.getWithdrawnRoutes());

		// attributes
		final List<AsSequence> asnums = Lists.newArrayList(new AsSequenceBuilder().setAs(new AsNumber(65001L)).build());
		final List<AsPathSegment> asPath = Lists.newArrayList();
		asPath.add(new SegmentsBuilder().setCSegment(new CAListBuilder().setAsSequence(asnums).build()).build());

		final CIpv6NextHop nextHop = new CIpv6NextHopBuilder().setIpv6NextHop(
				new Ipv6NextHopBuilder().setGlobal(new Ipv6Address("2001:db8::1")).setLinkLocal(new Ipv6Address("fe80::c001:bff:fe7e:0")).build()).build();

		// final List<ClusterIdentifier> clusters = Lists.newArrayList(
		// new ClusterIdentifier(new byte[] { 1, 2, 3, 4}),
		// new ClusterIdentifier(new byte[] { 5, 6, 7, 8}));

		// check path attributes

		// final PathAttribute originAttr = new PathAttribute(TypeCode.ORIGIN, false,
		// true, false, false, BGPOrigin.IGP);
		// assertEquals(originAttr, attrs.get(0));
		//
		// final PathAttribute asPathAttr = new PathAttribute(TypeCode.AS_PATH, false,
		// true, false, false, asPath);
		// assertEquals(asPathAttr, attrs.get(1));
		//
		// final PathAttribute multiExitDisc = new PathAttribute(
		// TypeCode.MULTI_EXIT_DISC, true, false, false, false, 0);
		// assertEquals(multiExitDisc, attrs.get(2));
		//
		// final PathAttribute originatorAttr = new PathAttribute(
		// TypeCode.ORIGINATOR_ID, true, false, false, false, IPv4.FAMILY.addressForString("127.0.0.1"));
		// assertEquals(originatorAttr, attrs.get(3));
		//
		// final PathAttribute clusterAttr = new PathAttribute(
		// TypeCode.CLUSTER_LIST, true, false, false, false, clusters);
		// assertEquals(clusterAttr, attrs.get(4));

		//		final BaseBGPObjectState state = new BaseBGPObjectState(BgpOrigin.Igp, null);
		//		final NetworkRouteState routeState = new NetworkRouteState(new NetworkObjectState(asPath, Collections.<Community> emptySet(), Collections.<ExtendedCommunity> emptySet()), nextHop);

		// check API message

		//		final Set<BGPObject> addedObjects = Sets.newHashSet();
		//
		//		final BGPRoute route1 = new BGPIPv6RouteImpl(IPv6.FAMILY.prefixForString("2001:db8:1:2::/64"), state, routeState);
		//
		//		addedObjects.add(route1);
		//
		//		final BGPRoute route2 = new BGPIPv6RouteImpl(IPv6.FAMILY.prefixForString("2001:db8:1:1::/64"), state, routeState);
		//
		//		addedObjects.add(route2);
		//
		//		final BGPRoute route3 = new BGPIPv6RouteImpl(IPv6.FAMILY.prefixForString("2001:db8:1::/64"), state, routeState);
		//
		//		addedObjects.add(route3);
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
	@Ignore
	// FIXME: to be fixed in testing phase
	public void testGetUpdateMessage3() throws Exception {
		final byte[] body = ByteArray.cutBytes(inputBytes.get(2), MessageUtil.COMMON_HEADER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(2), MessageUtil.MARKER_LENGTH,
				MessageUtil.LENGTH_FIELD_LENGTH));
		final Update message = updateParser.parseMessageBody(body, messageLength);

		// check fields
		assertNull(message.getWithdrawnRoutes());

		// attributes

		final List<AsSequence> asnums = Lists.newArrayList(new AsSequenceBuilder().setAs(new AsNumber(30L)).build());
		final List<AsPathSegment> asPath = Lists.newArrayList();
		asPath.add(new SegmentsBuilder().setCSegment(new CAListBuilder().setAsSequence(asnums).build()).build());
		asPath.add(new SegmentsBuilder().setCSegment(
				new CASetBuilder().setAsSet(Lists.newArrayList(new AsNumber(10L), new AsNumber(20L))).build()).build());

		final BgpAggregator aggregator = new AggregatorBuilder().setAsNumber(new AsNumber((long) 30)).setNetworkAddress(
				new Ipv4Address("10.0.0.9")).build();
		final CIpv4NextHop nextHop = new CIpv4NextHopBuilder().setIpv4NextHop(
				new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("10.0.0.9")).build()).build();

		final IPv4Prefix pref1 = IPv4.FAMILY.prefixForString("172.16.0.0/21");

		// check path attributes

		// final PathAttribute originAttr = new PathAttribute(TypeCode.ORIGIN, false,
		// true, false, false, BGPOrigin.INCOMPLETE);
		// assertEquals(originAttr, attrs.get(0));
		//
		// final PathAttribute asPathAttr = new PathAttribute(TypeCode.AS_PATH, false,
		// true, false, false, asPath);
		// assertEquals(asPathAttr, attrs.get(1));
		//
		// final PathAttribute nextHopAttr = new PathAttribute(TypeCode.NEXT_HOP, false,
		// true, false, false, nextHop);
		// assertEquals(nextHopAttr, attrs.get(2));
		//
		// final PathAttribute multiExitDisc = new PathAttribute(
		// TypeCode.MULTI_EXIT_DISC, true, false, false, false, 0);
		// assertEquals(multiExitDisc, attrs.get(3));
		//
		// final PathAttribute agg = new PathAttribute(TypeCode.AGGREGATOR, true, true,
		// false, false, aggregator);
		// assertEquals(agg, attrs.get(4));
		//
		// // check nlri
		//
		// final Set<IPv4Prefix> nlri = Sets.newHashSet(pref1);
		// assertEquals(nlri, ret.getBgpUpdateMessageBuilder().getNlri());
		//
		//		final BaseBGPObjectState state = new BaseBGPObjectState(BgpOrigin.Incomplete, aggregator);
		//		final NetworkRouteState routeState = new NetworkRouteState(new NetworkObjectState(asPath, Collections.<Community> emptySet(), Collections.<ExtendedCommunity> emptySet()), nextHop);

		// check API message

		//		final Set<BGPObject> addedObjects = Sets.newHashSet();
		//
		//		final BGPRoute route1 = new BGPIPv4RouteImpl(pref1, state, routeState);
		//
		//		addedObjects.add(route1);
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
	 * 80 <- attribute flags
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
	@Ignore
	// FIXME: to be fixed in testing phase
	public void testGetUpdateMessage4() throws Exception {
		final byte[] body = ByteArray.cutBytes(inputBytes.get(3), MessageUtil.COMMON_HEADER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(3), MessageUtil.MARKER_LENGTH,
				MessageUtil.LENGTH_FIELD_LENGTH));
		final Update message = updateParser.parseMessageBody(body, messageLength);

		// check fields

		assertNull(message.getWithdrawnRoutes());

		// attributes

		final CIpv4NextHop nextHop = new CIpv4NextHopBuilder().setIpv4NextHop(
				new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("3.3.3.3")).build()).build();

		final Set<ExtendedCommunity> comms = Sets.newHashSet();
		comms.add(new CInet4SpecificExtendedCommunityBuilder().setInet4SpecificExtendedCommunity(
				new Inet4SpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(new Ipv4Address("192.168.1.0")).setLocalAdministrator(
						new byte[] { 0x12, 0x34 }).build()).build());

		// check path attributes

		// final PathAttribute originAttr = new PathAttribute(TypeCode.ORIGIN, false,
		// true, false, false, BGPOrigin.EGP);
		// assertEquals(originAttr, attrs.get(0));
		//
		// final PathAttribute asPathAttr = new PathAttribute(TypeCode.AS_PATH, false,
		// true, false, false, asPath);
		// assertEquals(asPathAttr, attrs.get(1));
		//
		// final PathAttribute nextHopAttr = new PathAttribute(TypeCode.NEXT_HOP, false,
		// true, false, false, nextHop);
		// assertEquals(nextHopAttr, attrs.get(2));
		//
		// final PathAttribute multiExitDisc = new PathAttribute(
		// TypeCode.MULTI_EXIT_DISC, true, false, false, false, 0);
		// assertEquals(multiExitDisc, attrs.get(3));
		//
		// final PathAttribute localPref = new PathAttribute(TypeCode.LOCAL_PREF, false,
		// true, false, false, 100);
		// assertEquals(localPref, attrs.get(4));

		// check nlri
		//
		// final Set<IPv4Prefix> nlri = Sets.newHashSet(pref1, pref2, pref3);
		// assertEquals(nlri, ret.getBgpUpdateMessageBuilder().getNlri());

		//		final BaseBGPObjectState state = new BaseBGPObjectState(BgpOrigin.Egp, null);
		//		final NetworkRouteState routeState = new NetworkRouteState(new NetworkObjectState(Collections.<AsPathSegment> emptyList(), Collections.<Community> emptySet(), comms), nextHop);

		// check API message

		//		final Set<BGPObject> addedObjects = Sets.newHashSet();
		//
		//		final BGPRoute route1 = new BGPIPv4RouteImpl(IPv4.FAMILY.prefixForString("10.30.3.0/24"), state, routeState);
		//
		//		addedObjects.add(route1);
		//
		//		final BGPRoute route2 = new BGPIPv4RouteImpl(IPv4.FAMILY.prefixForString("10.30.2.0/24"), state, routeState);
		//
		//		addedObjects.add(route2);
		//
		//		final BGPRoute route3 = new BGPIPv4RouteImpl(IPv4.FAMILY.prefixForString("10.30.1.0/24"), state, routeState);
		//
		//		addedObjects.add(route3);
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
	@Ignore
	// FIXME: to be fixed in testing phase
	public void testGetUpdateMessage5() throws Exception {
		final byte[] body = ByteArray.cutBytes(inputBytes.get(4), MessageUtil.COMMON_HEADER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(4), MessageUtil.MARKER_LENGTH,
				MessageUtil.LENGTH_FIELD_LENGTH));
		final Update message = updateParser.parseMessageBody(body, messageLength);

		// attributes

		List<Ipv4Prefix> prefs = Lists.newArrayList(new Ipv4Prefix("172.16.0.4/30"));

		// check API message

		final Update expectedMessage = new UpdateBuilder().setWithdrawnRoutes(new WithdrawnRoutesBuilder().setWithdrawnRoutes(prefs).build()).build();

		assertEquals(expectedMessage.getWithdrawnRoutes().getWithdrawnRoutes().get(0).toString(), message.getWithdrawnRoutes().getWithdrawnRoutes().get(0).toString());
	}

	/*
	 * Test EOR for IPv4.
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
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(5), MessageUtil.MARKER_LENGTH,
				MessageUtil.LENGTH_FIELD_LENGTH));
		final Update message = updateParser.parseMessageBody(body, messageLength);

		assertEquals(new UpdateBuilder().build(), message);
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
	@Ignore
	//FIXME: to be fixed in testing phase
	public void testEORIpv6() throws Exception {
		final byte[] body = ByteArray.cutBytes(inputBytes.get(6), MessageUtil.COMMON_HEADER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(6), MessageUtil.MARKER_LENGTH,
				MessageUtil.LENGTH_FIELD_LENGTH));
		final Update message = updateParser.parseMessageBody(body, messageLength);

		// check fields

		Class<? extends AddressFamily> afi = message.getPathAttributes().getAugmentation(PathAttributes1.class).getMpReachNlri().getAfi();
		SubsequentAddressFamily safi = message.getPathAttributes().getAugmentation(PathAttributes1.class).getMpReachNlri().getSafi().newInstance();

		assertEquals(Ipv6AddressFamily.class, afi);
		assertEquals(UnicastSubsequentAddressFamily.INSTANCE, safi);
	}

	/*
	 * End of Rib for LS consists of empty MP_UNREACH_NLRI, with AFI 16388 and SAFI 71
	 * 
	 * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
	 * 00 1d <- length (29) - including header
	 * 02 <- message type
	 * 00 00 <- withdrawn routes length
	 * 00 06 <- total path attribute length
	 * 80 <- attribute flags
	 * 0f <- attribute type (15 - MP_UNREACH_NLRI)
	 * 03 <- attribute length
	 * 40 04 <- value (AFI 16388: LS)
	 * 47 <- value (SAFI 71)
	 */
	@Test
	@Ignore
	//FIXME: to be fixed in testing phase
	public void testEORLS() throws Exception {
		final byte[] body = ByteArray.cutBytes(inputBytes.get(7), MessageUtil.COMMON_HEADER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(7), MessageUtil.MARKER_LENGTH,
				MessageUtil.LENGTH_FIELD_LENGTH));
		final Update message = updateParser.parseMessageBody(body, messageLength);

		Class<? extends AddressFamily> afi = message.getPathAttributes().getAugmentation(PathAttributes1.class).getMpReachNlri().getAfi();
		SubsequentAddressFamily safi = message.getPathAttributes().getAugmentation(PathAttributes1.class).getMpReachNlri().getSafi().newInstance();

		assertEquals(LinkstateAddressFamily.class, afi);
		assertEquals(LinkstateSubsequentAddressFamily.INSTANCE, safi);
	}

	/*
	 *  Tests BGP Link Ipv4
	 * 
		00 00 <- withdrawn routes length
		01 48 <- total path attribute length (328)
		90 <- attribute flags
		0e <- attribute type code (MP reach)
		01 2c <- attribute extended length (300)
		40 04 <- AFI (16388 - Linkstate)
		47 <- SAFI (71 - Linkstate)
		04 <- next hop length
		19 19 19 01 <- nexthop (25.25.25.1)
		00 <- reserved

		00 02 <- NLRI type (2 - linkNLRI)
		00 5d <- NLRI length (93)
		03 <- ProtocolID - OSPF
		00 00 00 00 00 00 00 01 <- identifier

		01 00 <- local node descriptor type (256)
		00 24 <- length (36)
		02 00 <- node descriptor type (member AS - 512)
		00 04 <- length
		00 00 00 64 <- value (100)
		02 01 <- node descriptor type (bgpId - 513)
		00 04 <- length
		19 19 19 01 <- bgpId (25.25.25.1)
		02 02 <- node descriptor type (areaId - 514)
		00 04 <- length
		00 00 00 00 <- value
		02 03 <- node descriptor type (routeId - 515)
		00 08 <- length
		03 03 03 04 0b 0b 0b 03 <- OSPF Router Id

		01 01 <- remote node descriptor type (257)
		00 20 <- length (32)
		02 00 <- node descriptor type (member AS - 512)
		00 04 <- length
		00 00 00 64 <- value (100)
		02 01 <- node descriptor type (bgpId - 513)
		00 04 <- length
		19 19 19 01 <- bgpId (25.25.25.1)
		02 02 <- node descriptor type (areaId - 514)
		00 04 <- length
		00 00 00 00 <- value
		02 03 <- node descriptor type (routeId - 515)
		00 04 <- length
		03 03 03 04 <- OSPF Router Id

		01 03 <- link descriptor type (IPv4 interface address - 259)
		00 04 <- length (4)
		0b 0b 0b 03 <- value (11.11.11.3)

		00 02 <- NLRI type (2 - linkNLRI)
		00 5d <- NLRI length (93)
		03 <- ProtocolID - OSPF
		00 00 00 00 00 00 00 01 <- identifier

		01 00 <- local node descriptor type (256)
		00 24 <- length (36)
		02 00 <- node descriptor type (member AS - 512)
		00 04 <- length
		00 00 00 64 <- value (100)
		02 01 <- node descriptor type (bgpId - 513)
		00 04 <- length
		19 19 19 01 <- bgpId (25.25.25.1)
		02 02 <- node descriptor type (areaId - 514)
		00 04 <- length
		00 00 00 00 <- value
		02 03 <- node descriptor type (routeId - 515)
		00 08 <- length
		03 03 03 04 0b 0b 0b 03 <- OSPF Router Id

		01 01 <- remote node descriptor type (257)
		00 20 <- length (32)
		02 00 <- node descriptor type (member AS - 512)
		00 04 <- length
		00 00 00 64 <- value (100)
		02 01 <- node descriptor type (bgpId - 513)
		00 04 <- length
		19 19 19 01 <- bgpId (25.25.25.1)
		02 02 <- node descriptor type (areaId - 514)
		00 04 <- length
		00 00 00 00 <- value
		02 03 <- node descriptor type (routeId - 515)
		00 04 <- length
		01 01 01 02 <- OSPF Router Id

		01 03 <- link descriptor type (IPv4 interface address - 259)
		00 04 <- length
		0b 0b 0b 01 <- value (11.11.11.1)

		00 02 <- NLRI type (2 - linkNLRI)
		00 5d <- NLRI length (93)
		03 <- ProtocolID - OSPF
		00 00 00 00 00 00 00 01 <- identifier

		01 00 <- local node descriptor type (256)
		00 20 <- length (32)
		02 00 <- node descriptor type (member AS - 512)
		00 04 <- length
		00 00 00 64 <- value (100)
		02 01 <- node descriptor type (bgpId - 513)
		00 04 <- length
		19 19 19 01 <- bgpId (25.25.25.1)
		02 02 <- node descriptor type (areaId - 514)
		00 04 <- length
		00 00 00 00 <- value
		02 03 <- node descriptor type (routeId - 515)
		00 04 <- length
		01 01 01 02 <- OSPF Router Id

		01 01 <- remote node descriptor type (257)
		00 24 <- length (36)
		02 00 <- node descriptor type (member AS - 512)
		00 04 <- length
		00 00 00 64 <- value (100)
		02 01 <- node descriptor type (bgpId - 513)
		00 04 <- length
		19 19 19 01 <- bgpId (25.25.25.1)
		02 02 <- node descriptor type (areaId - 514)
		00 04 <- length
		00 00 00 00 <- value
		02 03 <- node descriptor type (routeId - 515)
		00 08 <- length
		03 03 03 04 0b 0b 0b 03 <- OSPF Router Id

		01 03 <- link descriptor type (IPv4 interface address - 259)
		00 04 <- length
		0b 0b 0b 01 <- value (11.11.11.1)

		40 <- attribute flags
		01 <- attribute type (Origin)
		01 <- attribute length
		00 <- value (IGP)
		40 <- attribute flags
		02 <- attribute type (AS Path)
		00 <- length
		40 <- attribute flags
		05 <- attribute type (local pref)
		04 <- length
		00 00 00 64 <- value
		c0 <- attribute flags
		63 <- attribute type (Link STATE - 99)
		07 <- length
		04 47 <- link attribute (1095 - Metric)
		00 03 <- length
		00 00 01 <- value
	 */
	@Test
	@Ignore
	// FIXME: to be fixed in testing phase
	public void testBGPLink() throws Exception {
		final byte[] body = ByteArray.cutBytes(inputBytes.get(8), MessageUtil.COMMON_HEADER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(8), MessageUtil.MARKER_LENGTH,
				MessageUtil.LENGTH_FIELD_LENGTH));
		final Update message = updateParser.parseMessageBody(body, messageLength);

		// check fields

		assertNull(message.getWithdrawnRoutes());

		// network object state
		//		final NetworkObjectState objState = new NetworkObjectState(Collections.<AsPathSegment> emptyList(), Collections.<Community> emptySet(), Collections.<ExtendedCommunity> emptySet());
		//		final BaseBGPObjectState state = new BaseBGPObjectState(BgpOrigin.Igp, null);

		// network link state
		final DefaultingTypesafeContainer<Metric<?>> container = new DefaultingTypesafeContainer<Metric<?>>();
		container.setDefaultEntry(new IGPMetric(1));
		//final NetworkLinkState linkState = new NetworkLinkState(objState, container, null, LinkProtectionType.UNPROTECTED, null, null, null);

		//		final NodeIdentifierFactory f100 = new NodeIdentifierFactory(new AsNumber((long) 100), new DomainIdentifier(new byte[] { 25, 25,
		//				25, 1 }), new AreaIdentifier(new byte[] { 0, 0, 0, 0 }));
		//
		//		final NodeIdentifier nodeid1 = f100.identifierForRouter(new OSPFv3LANIdentifier(new OSPFRouterIdentifier(new byte[] { 3, 3, 3, 4 }), new OSPFInterfaceIdentifier(new byte[] {
		//				0x0b, 0x0b, 0x0b, 0x03 })));
		//		final NodeIdentifier nodeid2 = f100.identifierForRouter(new OSPFRouterIdentifier(new byte[] { 3, 3, 3, 4 }));
		//
		//		final NodeIdentifier nodeid3 = f100.identifierForRouter(new OSPFRouterIdentifier(new byte[] { 1, 1, 1, 2 }));

		// check API message

		//		final LinkIdentifier linkId1 = new LinkIdentifier(null, new LinkAnchor(nodeid1, new IPv4InterfaceIdentifier(IPv4.FAMILY.addressForString("11.11.11.3"))), new LinkAnchor(nodeid2, null));
		//		final LinkIdentifier linkId2 = new LinkIdentifier(null, new LinkAnchor(nodeid1, new IPv4InterfaceIdentifier(IPv4.FAMILY.addressForString("11.11.11.1"))), new LinkAnchor(nodeid3, null));
		//		final LinkIdentifier linkId3 = new LinkIdentifier(null, new LinkAnchor(nodeid3, new IPv4InterfaceIdentifier(IPv4.FAMILY.addressForString("11.11.11.1"))), new LinkAnchor(nodeid1, null));
		//
		//		final BGPLink link1 = new BGPLinkImpl(state, linkId1, linkState);
		//		final BGPLink link2 = new BGPLinkImpl(state, linkId2, linkState);
		//		final BGPLink link3 = new BGPLinkImpl(state, linkId3, linkState);
		//
		//		final BGPUpdateMessage expectedMessage = new BGPUpdateMessageImpl(Sets.newHashSet((BGPObject) link1, (BGPObject) link2,
		//				(BGPObject) link3), Collections.<Identifier> emptySet());
		//
		//		assertEquals(expectedMessage, message);
	}

	/*
	 * TEST BGP Node
	 * 
	 *  00 00 <- withdrawn routes length
		00 b2 <- total path attribute length (178)
		90 <- attribute flags
		0e <- attribute type code (MP reach)
		00 a0 <- attribute extended length (160)
		40 04 <- AFI (16388 - Linkstate)
		47 <- SAFI (71 - Linkstate)
		04 <- next hop length
		19 19 19 01 - nexthop (25.25.25.1)
		00 <- reserved
		00 01 <- NLRI type (1 - nodeNLRI)
		00 31 <- NLRI length (49)
		03 <- ProtocolID - OSPF
		00 00 00 00 00 00 00 01 <- identifier

		01 00 <- local node descriptor type (256)
		00 24 <- length (36)
		02 00 <- node descriptor type (member AS - 512)
		00 04 <- length
		00 00 00 64 <- value (100)
		02 01 <- node descriptor type (bgpId - 513)
		00 04 <- length
		19 19 19 01 <- bgpId (25.25.25.1)
		02 02 <- node descriptor type (areaId - 514)
		00 04 <- length
		00 00 00 00 <- value
		02 03 <- node descriptor type (routeId - 515)
		00 08 <- length
		03 03 03 04 0b 0b 0b 03 <- OSPF Router Id

		00 01 <- NLRI type (1 - nodeNLRI)
		00 2d <- NLRI length (45)
		03 <- ProtocolID - OSPF
		00 00 00 00 00 00 00 01 <- identifier

		01 00 <- local node descriptor type (256)
		00 20 <- length (32)
		02 00 <- node descriptor type (member AS - 512)
		00 04 <- length
		00 00 00 64 <- value (100)
		02 01 <- node descriptor type (bgpId - 513)
		00 04 <- length
		19 19 19 01 <- bgpId (25.25.25.1)
		02 02 <- node descriptor type (areaId - 514)
		00 04 <- length
		00 00 00 00 <- value
		02 03 <- node descriptor type (routeId - 515)
		00 04 <- length
		03 03 03 04 <- OSPF Router Id

		00 01 <- NLRI type (1 - nodeNLRI)
		00 2d <- NLRI length (45)
		03 <- ProtocolID - OSPF
		00 00 00 00 00 00 00 01 <- identifier
		01 00 <- local node descriptor type (256)
		00 20 <- length (32)
		02 00 <- node descriptor type (member AS - 512)
		00 04 <- length
		00 00 00 64 <- value (100)
		02 01 <- node descriptor type (bgpId - 513)
		00 04 <- length
		19 19 19 01 <- bgpId (25.25.25.1)
		02 02 <- node descriptor type (areaId - 514)
		00 04 <- length
		00 00 00 00 <- value
		02 03 <- node descriptor type (routeId - 515)
		00 04 <- length
		01 01 01 02  <- OSPF Router Id

		40 <- attribute flags
		01 <- attribute type (Origin)
		01 <- attribute length
		00 <- value (IGP)
		40 <- attribute flags
		02 <- attribute type (AS Path)
		00 <- length
		40 <- attribute flags
		05 <- attribute type (local pref)
		04 <- length
		00 00 00 64 <- value
	 */
	@Test
	@Ignore
	// FIXME: to be fixed in testing phase
	public void testBGPNode() throws Exception {
		final byte[] body = ByteArray.cutBytes(inputBytes.get(9), MessageUtil.COMMON_HEADER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(9), MessageUtil.MARKER_LENGTH,
				MessageUtil.LENGTH_FIELD_LENGTH));
		final Update message = updateParser.parseMessageBody(body, messageLength);

		// check fields

		assertNull(message.getWithdrawnRoutes());

		// network object state
		//		final NetworkObjectState objState = new NetworkObjectState(Collections.<AsPathSegment> emptyList(), Collections.<Community> emptySet(), Collections.<ExtendedCommunity> emptySet());
		//		final BaseBGPObjectState state = new BaseBGPObjectState(BgpOrigin.Igp, null);
		//		final NetworkNodeState nstate = new NetworkNodeState(objState, Collections.<TopologyIdentifier> emptySet(), Collections.<ISISAreaIdentifier> emptySet(), false, false, false, false, Collections.<RouterIdentifier> emptySet(), null);

		// network link state

		//		final NodeIdentifierFactory f100 = new NodeIdentifierFactory(new AsNumber((long) 100), new DomainIdentifier(new byte[] { 25, 25,
		//				25, 1 }), new AreaIdentifier(new byte[] { 0, 0, 0, 0 }));
		//
		//		final NodeIdentifier nodeid1 = f100.identifierForRouter(new OSPFv3LANIdentifier(new OSPFRouterIdentifier(new byte[] { 3, 3, 3, 4 }), new OSPFInterfaceIdentifier(new byte[] {
		//				0x0b, 0x0b, 0x0b, 0x03 })));
		//		final NodeIdentifier nodeid2 = f100.identifierForRouter(new OSPFRouterIdentifier(new byte[] { 3, 3, 3, 4 }));
		//
		//		final NodeIdentifier nodeid3 = f100.identifierForRouter(new OSPFRouterIdentifier(new byte[] { 1, 1, 1, 2 }));

		// check API message

		//		final BGPNode node1 = new BGPNodeImpl(state, nodeid1, nstate);
		//		final BGPNode node2 = new BGPNodeImpl(state, nodeid2, nstate);
		//		final BGPNode node3 = new BGPNodeImpl(state, nodeid3, nstate);
		//
		//		final BGPUpdateMessage expectedMessage = new BGPUpdateMessageImpl(Sets.newHashSet((BGPObject) node1, (BGPObject) node2,
		//				(BGPObject) node3), Collections.<Identifier> emptySet());
		//
		//		assertEquals(expectedMessage, message);
	}

	/*
	 * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
	 * 00 98 <- length (69) - including header
	 * 01 <- message type
	 * 04 <- BGP version
	 * 00 64 <- My AS Number (AS TRANS in this case)
	 * 00 b4 <- Hold Time
	 * 00 00 00 00 <- BGP Identifier
	 * 28 <- Optional Parameters Length
	 * 02 <- opt. param. type (capabilities)
	 * 06 <- length
	 * 01 <- capability code (MP Extensions for BGP4)
	 * 04 <- length
	 * 00 01 00 01 <- AFI 1, SAFI 1
	 * 02 <- opt. param. type (capabilities)
	 * 06 <- length
	 * 01 <- capability code (MP Extensions for BGP4)
	 * 04 <- length
	 * 00 02 00 01 <- AFI 2, SAFI 1
	 * 02 <- opt. param. type (capabilities)
	 * 06 <- length
	 * 01 <- capability code (MP Extensions for BGP4)
	 * 04 <- length
	 * 40 04 00 47 <- AFI 16388, SAFI 71
	 * 02 <- opt. param. type (capabilities)
	 * 02 <- length
	 * 80 <- capability code (private)
	 * 00 <- length
	 * 02 <- opt. param. type (capabilities)
	 * 02 <- length
	 * 02 <- capability code (Route refresh)
	 * 00 <- length
	 * 02 <- opt. param. type (capabilities)
	 * 06 <- length
	 * 41 <- capability code (AS4 octet support)
	 * 04 <- length
	 * 00 00 00 64 <- AS number
	 */
	@Test
	public void testOpenMessage() throws Exception {
		final BGPMessageFactory msgFactory = SimpleBGPMessageFactory.getInstance();
		final Open open = (Open) msgFactory.parse(inputBytes.get(13)).get(0);
		final Set<BgpTableType> types = Sets.newHashSet();
		for (final BgpParameters param : open.getBgpParameters()) {
			final CParameters p = param.getCParameters();
			if (p instanceof CMultiprotocol) {
				final BgpTableType type = new BgpTableTypeImpl(((CMultiprotocol) p).getMultiprotocolCapability().getAfi(), ((CMultiprotocol) p).getMultiprotocolCapability().getSafi());
				types.add(type);
			}
		}
		final Set<BgpTableType> expected = Sets.newHashSet();
		expected.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
		expected.add(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class));
		expected.add(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));
		assertEquals(expected, types);
	}
}
