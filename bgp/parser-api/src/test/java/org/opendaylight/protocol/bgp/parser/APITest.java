/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.concepts.Community;
import org.opendaylight.protocol.bgp.concepts.ExtendedCommunity;
import org.opendaylight.protocol.bgp.concepts.IPv4NextHop;
import org.opendaylight.protocol.bgp.linkstate.ISISAreaIdentifier;
import org.opendaylight.protocol.bgp.linkstate.LinkProtectionType;
import org.opendaylight.protocol.bgp.linkstate.NetworkLinkState;
import org.opendaylight.protocol.bgp.linkstate.NetworkNodeState;
import org.opendaylight.protocol.bgp.linkstate.NetworkObjectState;
import org.opendaylight.protocol.bgp.linkstate.NetworkPrefixState;
import org.opendaylight.protocol.bgp.linkstate.NetworkRouteState;
import org.opendaylight.protocol.bgp.linkstate.RouteTag;
import org.opendaylight.protocol.bgp.linkstate.RouterIdentifier;
import org.opendaylight.protocol.bgp.linkstate.TopologyIdentifier;
import org.opendaylight.protocol.bgp.parser.message.BGPKeepAliveMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPNotificationMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.bgp.parser.parameter.AS4BytesCapability;
import org.opendaylight.protocol.bgp.parser.parameter.CapabilityParameter;
import org.opendaylight.protocol.bgp.parser.parameter.GracefulCapability;
import org.opendaylight.protocol.bgp.parser.parameter.MultiprotocolCapability;
import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.Metric;
import org.opendaylight.protocol.concepts.TEMetric;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.util.DefaultingTypesafeContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class APITest {

	BaseBGPObjectState objState = new BaseBGPObjectState(BgpOrigin.Egp, null);
	NetworkObjectState netObjState = new NetworkObjectState(null, Collections.<Community> emptySet(), Collections.<ExtendedCommunity> emptySet());

	@Test
	public void testAPI() {
		final BGPRouteState<IPv4Address> route1 = new BGPRouteState<IPv4Address>(this.objState, new NetworkRouteState<IPv4Address>(new IPv4NextHop(IPv4.FAMILY.addressForString("192.168.5.4"))));
		final BGPRouteState<IPv4Address> route2 = new BGPRouteState<IPv4Address>(new BaseBGPObjectState(BgpOrigin.Igp, null), new NetworkRouteState<IPv4Address>(new IPv4NextHop(IPv4.FAMILY.addressForString("172.168.5.42"))));

		assertEquals(route1, route1.newInstance());
		assertNotSame(route1.hashCode(), new BGPRouteState<IPv4Address>(route2).hashCode());
		assertEquals(route1.toString(), route1.toString());
		assertNull(route1.getAggregator());
		assertNotNull(route1.getObjectState().getNextHop());
		assertEquals(route1.getOrigin(), BgpOrigin.Egp);

		final BGPParsingException e = new BGPParsingException("Some error message.");
		assertEquals("Some error message.", e.getError());

		final BGPParsingException e2 = new BGPParsingException("Some error message.", new IllegalAccessException());
		assertEquals("Some error message.", e2.getError());
		assertTrue(e2.getCause() instanceof IllegalAccessException);
	}

	@Test
	public void testPrefixes() throws UnknownHostException {
		final NetworkPrefixState state = new NetworkPrefixState(this.netObjState, Sets.<RouteTag> newTreeSet(), null);

		final BGPPrefixState ipv4 = new BGPPrefixState(this.objState, state);
		final BGPPrefixState ipv6 = new BGPPrefixState(new BaseBGPObjectState(BgpOrigin.Egp, null), state);

		assertEquals(ipv4.toString(), ipv4.newInstance().toString());
		assertNotSame(ipv4, new BGPPrefixState(ipv6));
	}

	@Test
	public void testNodeState() {
		final BGPNodeState n1 = new BGPNodeState(this.objState, new NetworkNodeState(this.netObjState, Collections.<TopologyIdentifier> emptySet(), Collections.<ISISAreaIdentifier> emptySet(), false, false, false, false, Collections.<RouterIdentifier> emptySet(), ""));
		assertEquals(n1, n1.newInstance());
		assertEquals(n1, new BGPNodeState(n1));
	}

	@Test
	public void testLinkState() {
		final DefaultingTypesafeContainer<Metric<?>> m = new DefaultingTypesafeContainer<Metric<?>>();
		m.setDefaultEntry(new TEMetric(15L));
		final BGPLinkState l1 = new BGPLinkState(this.objState, new NetworkLinkState(this.netObjState, m, null, null, null, LinkProtectionType.UNPROTECTED, null, null, null, null, Collections.<RouterIdentifier> emptySet(), Collections.<RouterIdentifier> emptySet()));
		assertEquals(l1, l1.newInstance());
		assertEquals(l1, new BGPLinkState(l1));
	}

	@Test
	public void testBGPParameter() {

		final BGPTableType t = new BGPTableType(BGPAddressFamily.LinkState, BGPSubsequentAddressFamily.Unicast);
		final BGPTableType t1 = new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.Unicast);

		final BGPParameter tlv1 = new MultiprotocolCapability(t);

		final BGPParameter tlv2 = new MultiprotocolCapability(t1);

		final Map<BGPTableType, Boolean> tt = Maps.newHashMap();
		tt.put(t, true);
		tt.put(t1, false);

		final BGPParameter tlv3 = new GracefulCapability(false, 0, tt);

		final BGPParameter tlv4 = new AS4BytesCapability(new ASNumber(40));

		assertFalse(((GracefulCapability) tlv3).isRestartFlag());

		assertEquals(0, ((GracefulCapability) tlv3).getRestartTimerValue());

		assertEquals(tlv1.getType(), tlv2.getType());

		assertFalse(tlv1.equals(tlv2));

		assertNotSame(tlv1.hashCode(), tlv3.hashCode());

		assertNotSame(tlv2.toString(), tlv3.toString());

		assertEquals(((GracefulCapability) tlv3).getTableTypes(), tt);

		assertNotSame(((CapabilityParameter) tlv1).getCode(), ((CapabilityParameter) tlv3).getCode());

		assertEquals(((MultiprotocolCapability) tlv1).getSafi(), ((MultiprotocolCapability) tlv2).getSafi());

		assertNotSame(((MultiprotocolCapability) tlv1).getAfi(), ((MultiprotocolCapability) tlv2).getAfi());

		assertEquals(40, ((AS4BytesCapability) tlv4).getASNumber().getAsn());

		assertEquals(new AS4BytesCapability(new ASNumber(40)).toString(), tlv4.toString());
	}

	@Test
	public void testDocumentedException() {
		final DocumentedException de = new BGPDocumentedException("Some message", BGPError.BAD_BGP_ID);
		assertEquals("Some message", de.getMessage());
		assertEquals(BGPError.BAD_BGP_ID, ((BGPDocumentedException) de).getError());
		assertNull(((BGPDocumentedException) de).getData());
	}

	@Test
	public void testBGPKeepAliveMessage() {
		final BGPMessage msg = new BGPKeepAliveMessage();
		assertTrue(msg instanceof BGPKeepAliveMessage);
	}

	@Test
	public void testBGPNotificationMessage() {
		final BGPMessage msg = new BGPNotificationMessage(BGPError.AS_PATH_MALFORMED);
		assertTrue(msg instanceof BGPNotificationMessage);
		assertEquals(BGPError.AS_PATH_MALFORMED, ((BGPNotificationMessage) msg).getError());
		assertNull(((BGPNotificationMessage) msg).getData());
	}

	@Test
	public void testBGPOpenMessage() {
		final BGPMessage msg = new BGPOpenMessage(new ASNumber(58), (short) 5, null, null);
		assertNull(((BGPOpenMessage) msg).getOptParams());
	}

	@Test
	public void testToString() {
		final BGPMessage o = new BGPOpenMessage(new ASNumber(58), (short) 5, null, null);
		final BGPMessage n = new BGPNotificationMessage(BGPError.ATTR_FLAGS_MISSING);
		assertNotSame(o.toString(), n.toString());
	}
}
