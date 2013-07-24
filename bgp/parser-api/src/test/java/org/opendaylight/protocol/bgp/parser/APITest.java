/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.Collections;

import org.junit.Test;

import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.Metric;
import org.opendaylight.protocol.concepts.TEMetric;
import org.opendaylight.protocol.bgp.linkstate.ISISAreaIdentifier;
import org.opendaylight.protocol.bgp.linkstate.LinkProtectionType;
import org.opendaylight.protocol.bgp.linkstate.RouteTag;
import org.opendaylight.protocol.bgp.linkstate.RouterIdentifier;
import org.opendaylight.protocol.bgp.linkstate.TopologyIdentifier;
import org.opendaylight.protocol.bgp.linkstate.TopologyNodeInformation;
import org.opendaylight.protocol.bgp.linkstate.NetworkLinkState;
import org.opendaylight.protocol.bgp.linkstate.NetworkNodeState;
import org.opendaylight.protocol.bgp.linkstate.NetworkObjectState;
import org.opendaylight.protocol.bgp.linkstate.NetworkPrefixState;
import org.opendaylight.protocol.bgp.linkstate.NetworkRouteState;

import org.opendaylight.protocol.bgp.concepts.BGPOrigin;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.concepts.Community;
import org.opendaylight.protocol.bgp.concepts.ExtendedCommunity;
import org.opendaylight.protocol.bgp.concepts.IPv4NextHop;
import org.opendaylight.protocol.bgp.parser.BGPLinkState;
import org.opendaylight.protocol.bgp.parser.BGPMessageHeader;
import org.opendaylight.protocol.bgp.parser.BGPNodeState;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BGPPrefixState;
import org.opendaylight.protocol.bgp.parser.BGPRouteState;
import org.opendaylight.protocol.util.DefaultingTypesafeContainer;
import com.google.common.collect.Sets;

public class APITest {

	BaseBGPObjectState objState = new BaseBGPObjectState(BGPOrigin.EGP, null);
	NetworkObjectState netObjState = new NetworkObjectState(null, Collections.<Community> emptySet(), Collections.<ExtendedCommunity> emptySet());

	@Test
	public void testAPI() {
		final BGPRouteState<IPv4Address> route1 = new BGPRouteState<IPv4Address>(this.objState, new NetworkRouteState<IPv4Address>(new IPv4NextHop(IPv4.FAMILY.addressForString("192.168.5.4"))));
		final BGPRouteState<IPv4Address> route2 = new BGPRouteState<IPv4Address>(new BaseBGPObjectState(BGPOrigin.IGP, null), new NetworkRouteState<IPv4Address>(new IPv4NextHop(IPv4.FAMILY.addressForString("172.168.5.42"))));

		assertEquals(route1, route1.newInstance());
		assertNotSame(route1.hashCode(), new BGPRouteState<IPv4Address>(route2).hashCode());
		assertEquals(route1.toString(), route1.toString());
		assertNull(route1.getAggregator());
		assertNotNull(route1.getObjectState().getNextHop());
		assertEquals(route1.getOrigin(), BGPOrigin.EGP);

		final BGPParsingException e = new BGPParsingException("Some error message.");
		assertEquals("Some error message.", e.getError());

		final BGPParsingException e2 = new BGPParsingException("Some error message.", new IllegalAccessException());
		assertEquals("Some error message.", e2.getError());
		assertTrue(e2.getCause() instanceof IllegalAccessException);
	}

	@Test
	public void testBGPHeaderParser() {
		final BGPMessageHeader h = new BGPMessageHeader();
		try {
			h.fromBytes(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
					(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
					(byte) 0, (byte) 0 });
			fail("Exception should have occured.");
		} catch (final IllegalArgumentException e) {
			assertEquals("Too few bytes in passed array. Passed: 18. Expected: >= " + BGPMessageHeader.COMMON_HEADER_LENGTH + ".",
					e.getMessage());
		}

		h.fromBytes(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0,
				(byte) 27, (byte) 1 });

		assertEquals(h.getType(), 1);
		assertEquals(h.getLength(), 27);
		assertTrue(h.isParsed());

		final BGPMessageHeader hh = new BGPMessageHeader(1, 27);

		final byte[] expected = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0, (byte) 27, (byte) 1 };

		assertArrayEquals(expected, hh.toBytes());

		hh.setParsed();

		assertFalse(hh.isParsed());
	}

	@Test
	public void testPrefixes() throws UnknownHostException {
		final NetworkPrefixState state = new NetworkPrefixState(this.netObjState, Sets.<RouteTag> newTreeSet(), null);

		final BGPPrefixState ipv4 = new BGPPrefixState(this.objState, state);
		final BGPPrefixState ipv6 = new BGPPrefixState(new BaseBGPObjectState(BGPOrigin.EGP, null), state);

		assertEquals(ipv4.toString(), ipv4.newInstance().toString());
		assertNotSame(ipv4, new BGPPrefixState(ipv6));
	}

	@Test
	public void testNodeState() {
		final BGPNodeState n1 = new BGPNodeState(this.objState, new NetworkNodeState(this.netObjState, Collections.<TopologyIdentifier, TopologyNodeInformation> emptyMap(), Collections.<ISISAreaIdentifier> emptySet(), false, false, Collections.<RouterIdentifier> emptySet(), ""));
		assertEquals(n1, n1.newInstance());
		assertEquals(n1, new BGPNodeState(n1));
	}

	@Test
	public void testLinkState() {
		final DefaultingTypesafeContainer<Metric<?>> m = new DefaultingTypesafeContainer<Metric<?>>();
		m.setDefaultEntry(new TEMetric(15L));
		final BGPLinkState l1 = new BGPLinkState(this.objState, new NetworkLinkState(this.netObjState, m, null, null, null, LinkProtectionType.UNPROTECTED, null, null, null, null));
		assertEquals(l1, l1.newInstance());
		assertEquals(l1, new BGPLinkState(l1));
	}
}
