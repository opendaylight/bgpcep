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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.IPv4InterfaceIdentifier;
import org.opendaylight.protocol.bgp.linkstate.ISISLANIdentifier;
import org.opendaylight.protocol.bgp.linkstate.ISISRouterIdentifier;
import org.opendaylight.protocol.bgp.linkstate.InterfaceIdentifier;
import org.opendaylight.protocol.bgp.linkstate.LinkAnchor;
import org.opendaylight.protocol.bgp.linkstate.LinkIdentifier;
import org.opendaylight.protocol.bgp.linkstate.NodeIdentifier;
import org.opendaylight.protocol.bgp.linkstate.NodeIdentifierFactory;
import org.opendaylight.protocol.bgp.linkstate.SourceProtocol;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunitiesParser;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.ISOSystemIdentifier;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CAsSpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CAsSpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CInet4SpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CInet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.COpaqueExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.COpaqueExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CRouteOriginExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CRouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CRouteTargetExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CRouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.as.specific.extended.community.AsSpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.inet4.specific.extended.community.Inet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.opaque.extended.community.OpaqueExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.route.origin.extended.community.RouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.route.target.extended.community.RouteTargetExtendedCommunityBuilder;

import com.google.common.collect.Sets;

public class ComplementaryTest {

	@Test
	public void testBGPAggregatorImpl() {
		final BgpAggregator ipv4 = new AggregatorBuilder().setAsNumber(new AsNumber((long) 5524)).setNetworkAddress(
				new Ipv4Address("124.55.42.1")).build();
		final BgpAggregator ipv4i = new AggregatorBuilder().setAsNumber(new AsNumber((long) 5525)).setNetworkAddress(
				new Ipv4Address("124.55.42.1")).build();

		assertNotSame(ipv4.hashCode(), ipv4i.hashCode());

		assertNotSame(ipv4.getAsNumber(), ipv4i.getAsNumber());

		assertEquals(ipv4.getNetworkAddress(), ipv4i.getNetworkAddress());
	}

	@Test
	public void testBGPLinkMP() {
		final NodeIdentifier localnodeid = new NodeIdentifier(new AsNumber((long) 25600), null, null, new ISISRouterIdentifier(new ISOSystemIdentifier(new byte[] {
				0x22, 0x22, 0x22, 0x22, 0x22, 0x22 })));
		final NodeIdentifier remotenodeid = NodeIdentifierFactory.localIdentifier(new ISISLANIdentifier(new ISOSystemIdentifier(new byte[] {
				0x22, 0x22, 0x22, 0x22, 0x22, 0x22 }), (short) 1));

		final InterfaceIdentifier ifaceid = new IPv4InterfaceIdentifier(IPv4.FAMILY.addressForString("10.1.1.1"));

		final LinkIdentifier l = new LinkIdentifier(null, new LinkAnchor(localnodeid, ifaceid), new LinkAnchor(remotenodeid, ifaceid));

		final Set<LinkIdentifier> links = Sets.newHashSet(l);

		final BGPLinkMP link = new BGPLinkMP(0, SourceProtocol.Direct, true, links);

		final BGPLinkMP link1 = new BGPLinkMP(0, SourceProtocol.Direct, true, Collections.<LinkIdentifier> emptySet());

		assertNotSame(link.hashCode(), link1.hashCode());

		assertEquals(link, new BGPLinkMP(0, SourceProtocol.Direct, true, links));

		assertEquals(link.hashCode(), (new BGPLinkMP(0, SourceProtocol.Direct, true, links)).hashCode());

		assertNotSame(link.toString(), link1.toString());
	}

	@Test
	public void testBGPUpdateMessageImpl() {
		final BGPUpdateMessageImpl msg = new BGPUpdateMessageImpl(null, null);
		final BGPUpdateMessageImpl msg1 = new BGPUpdateMessageImpl(null, null);

		assertEquals(msg, msg1);

		assertEquals(msg.hashCode(), msg1.hashCode());

		assertNotNull(msg.toString());

		assertNull(msg.getAddedObjects());
		assertNull(msg.getRemovedObjects());

		assertNotSame(msg1, null);
	}

	@Test
	public void testCommunitiesParser() {
		CAsSpecificExtendedCommunity as = null;
		try {
			as = (CAsSpecificExtendedCommunity) CommunitiesParser.parseExtendedCommunity(new byte[] { 0, 5, 0, 54, 0, 0, 1, 76 });
		} catch (final BGPDocumentedException e1) {
			fail("Not expected exception: " + e1);
		}
		CAsSpecificExtendedCommunity expected = new CAsSpecificExtendedCommunityBuilder().setAsSpecificExtendedCommunity(
				new AsSpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(new AsNumber(54L)).setLocalAdministrator(
						new byte[] { 0, 0, 1, 76 }).build()).build();
		assertEquals(expected.getAsSpecificExtendedCommunity().isTransitive(), as.getAsSpecificExtendedCommunity().isTransitive());
		assertEquals(expected.getAsSpecificExtendedCommunity().getGlobalAdministrator(),
				as.getAsSpecificExtendedCommunity().getGlobalAdministrator());
		assertArrayEquals(expected.getAsSpecificExtendedCommunity().getLocalAdministrator(),
				as.getAsSpecificExtendedCommunity().getLocalAdministrator());

		try {
			as = (CAsSpecificExtendedCommunity) CommunitiesParser.parseExtendedCommunity(new byte[] { 40, 5, 0, 54, 0, 0, 1, 76 });
		} catch (final BGPDocumentedException e1) {
			fail("Not expected exception: " + e1);
		}
		expected = new CAsSpecificExtendedCommunityBuilder().setAsSpecificExtendedCommunity(
				new AsSpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(new AsNumber(54L)).setLocalAdministrator(
						new byte[] { 0, 0, 1, 76 }).build()).build();
		assertEquals(expected.getAsSpecificExtendedCommunity().isTransitive(), as.getAsSpecificExtendedCommunity().isTransitive());

		CRouteTargetExtendedCommunity rtc = null;
		try {
			rtc = (CRouteTargetExtendedCommunity) CommunitiesParser.parseExtendedCommunity(new byte[] { 1, 2, 0, 35, 4, 2, 8, 7 });
		} catch (final BGPDocumentedException e1) {
			fail("Not expected exception: " + e1);
		}
		final CRouteTargetExtendedCommunity rexpected = new CRouteTargetExtendedCommunityBuilder().setRouteTargetExtendedCommunity(
				new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(new AsNumber(35L)).setLocalAdministrator(
						new byte[] { 4, 2, 8, 7 }).build()).build();
		assertEquals(rexpected.getRouteTargetExtendedCommunity().getGlobalAdministrator(),
				rtc.getRouteTargetExtendedCommunity().getGlobalAdministrator());
		assertArrayEquals(rexpected.getRouteTargetExtendedCommunity().getLocalAdministrator(),
				rtc.getRouteTargetExtendedCommunity().getLocalAdministrator());

		CRouteOriginExtendedCommunity roc = null;
		try {
			roc = (CRouteOriginExtendedCommunity) CommunitiesParser.parseExtendedCommunity(new byte[] { 0, 3, 0, 24, 4, 2, 8, 7 });
		} catch (final BGPDocumentedException e1) {
			fail("Not expected exception: " + e1);
		}
		final CRouteOriginExtendedCommunity oexpected = new CRouteOriginExtendedCommunityBuilder().setRouteOriginExtendedCommunity(
				new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(new AsNumber(24L)).setLocalAdministrator(
						new byte[] { 4, 2, 8, 7 }).build()).build();
		assertEquals(oexpected.getRouteOriginExtendedCommunity().getGlobalAdministrator(),
				roc.getRouteOriginExtendedCommunity().getGlobalAdministrator());
		assertArrayEquals(oexpected.getRouteOriginExtendedCommunity().getLocalAdministrator(),
				roc.getRouteOriginExtendedCommunity().getLocalAdministrator());

		CInet4SpecificExtendedCommunity sec = null;
		try {
			sec = (CInet4SpecificExtendedCommunity) CommunitiesParser.parseExtendedCommunity(new byte[] { 41, 6, 12, 51, 2, 5, 21, 45 });
		} catch (final BGPDocumentedException e1) {
			fail("Not expected exception: " + e1);
		}
		final CInet4SpecificExtendedCommunity iexpected = new CInet4SpecificExtendedCommunityBuilder().setInet4SpecificExtendedCommunity(
				new Inet4SpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(new Ipv4Address("/12.51.2.5")).setLocalAdministrator(
						new byte[] { 21, 45 }).build()).build();
		assertEquals(iexpected.getInet4SpecificExtendedCommunity().isTransitive(), sec.getInet4SpecificExtendedCommunity().isTransitive());
		assertEquals(iexpected.getInet4SpecificExtendedCommunity().getGlobalAdministrator(),
				sec.getInet4SpecificExtendedCommunity().getGlobalAdministrator());
		assertArrayEquals(iexpected.getInet4SpecificExtendedCommunity().getLocalAdministrator(),
				sec.getInet4SpecificExtendedCommunity().getLocalAdministrator());

		COpaqueExtendedCommunity oec = null;
		try {
			oec = (COpaqueExtendedCommunity) CommunitiesParser.parseExtendedCommunity(new byte[] { 3, 6, 21, 45, 5, 4, 3, 1 });
		} catch (final BGPDocumentedException e1) {
			fail("Not expected exception: " + e1);
		}
		final COpaqueExtendedCommunity oeexpected = new COpaqueExtendedCommunityBuilder().setOpaqueExtendedCommunity(
				new OpaqueExtendedCommunityBuilder().setTransitive(false).setValue(new byte[] { 21, 45, 5, 4, 3, 1 }).build()).build();
		assertEquals(oeexpected.getOpaqueExtendedCommunity().isTransitive(), oec.getOpaqueExtendedCommunity().isTransitive());
		assertArrayEquals(oeexpected.getOpaqueExtendedCommunity().getValue(), oec.getOpaqueExtendedCommunity().getValue());

		try {
			oec = (COpaqueExtendedCommunity) CommunitiesParser.parseExtendedCommunity(new byte[] { 43, 6, 21, 45, 5, 4, 3, 1 });
		} catch (final BGPDocumentedException e1) {
			fail("Not expected exception: " + e1);
		}
		final COpaqueExtendedCommunity oeexpected1 = new COpaqueExtendedCommunityBuilder().setOpaqueExtendedCommunity(
				new OpaqueExtendedCommunityBuilder().setTransitive(true).setValue(new byte[] { 21, 45, 5, 4, 3, 1 }).build()).build();
		assertEquals(oeexpected1.getOpaqueExtendedCommunity().isTransitive(), oec.getOpaqueExtendedCommunity().isTransitive());
		assertArrayEquals(oeexpected1.getOpaqueExtendedCommunity().getValue(), oec.getOpaqueExtendedCommunity().getValue());

		try {
			CommunitiesParser.parseExtendedCommunity(new byte[] { 11, 11, 21, 45, 5, 4, 3, 1 });
			fail("Exception should have occured.");
		} catch (final BGPDocumentedException e) {
			assertEquals("Could not parse Extended Community type: 11", e.getMessage());
		}
	}

	@Test
	public void testBGPHeaderParser() throws IOException {
		final BGPMessageFactoryImpl h = new BGPMessageFactoryImpl();
		try {
			h.parse(new byte[] { (byte) 0, (byte) 0 });
			fail("Exception should have occured.");
		} catch (final IllegalArgumentException e) {
			assertEquals("Too few bytes in passed array. Passed: 2. Expected: >= 19.", e.getMessage());
		} catch (final DeserializerException e) {
			fail("Not this exception should have occured:" + e);
		} catch (final DocumentedException e) {
			fail("Not this exception should have occured:" + e);
		}
	}

	@Test
	public void testByteList() {
		final ByteList b1 = new ByteList();
		b1.add(new byte[] { 3, 4, 8 });
		b1.add(new byte[] { 3, 4, 9 });

		final ByteList b2 = new ByteList();
		b2.add(new byte[] { 3, 4, 8 });
		b2.add(new byte[] { 3, 4, 9 });

		assertEquals(b1, b2);
		assertEquals(b1.toString(), b2.toString());
	}

	@Test
	public void testMessageParser() throws IOException {
		final BGPMessageFactoryImpl parser = new BGPMessageFactoryImpl();
		String ex = "";
		try {
			parser.put(null);
		} catch (final IllegalArgumentException e) {
			ex = e.getMessage();
		}
		assertEquals("BGPMessage is mandatory.", ex);
	}
}
