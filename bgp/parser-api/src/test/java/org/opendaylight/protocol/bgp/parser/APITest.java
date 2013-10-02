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
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
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
import org.opendaylight.protocol.concepts.Metric;
import org.opendaylight.protocol.concepts.TEMetric;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.util.DefaultingTypesafeContainer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.CAs4Bytes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.CAs4BytesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.c.as4.bytes.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.c.multiprotocol.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.c.multiprotocol.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.c.ipv4.next.hop.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class APITest {

	BaseBGPObjectState objState = new BaseBGPObjectState(BgpOrigin.Egp, null);
	NetworkObjectState netObjState = new NetworkObjectState(null, Collections.<Community> emptySet(), Collections.<ExtendedCommunity> emptySet());

	@Test
	public void testAPI() {
		final BGPRouteState route1 = new BGPRouteState(this.objState, new NetworkRouteState(new CIpv4NextHopBuilder().setIpv4NextHop(
				new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("192.168.5.4")).build()).build()));
		final BGPRouteState route2 = new BGPRouteState(new BaseBGPObjectState(BgpOrigin.Igp, null), new NetworkRouteState(new CIpv4NextHopBuilder().setIpv4NextHop(
				new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("172.168.5.42")).build()).build()));

		assertEquals(route1, route1.newInstance());
		assertNotSame(route1.hashCode(), new BGPRouteState(route2).hashCode());
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

		final BGPTableType t = new BGPTableType(LinkstateAddressFamily.class, UnicastSubsequentAddressFamily.class);
		final BGPTableType t1 = new BGPTableType(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

		final MultiprotocolCapability cap = new MultiprotocolCapabilityBuilder().setAfi(LinkstateAddressFamily.class).setSafi(
				UnicastSubsequentAddressFamily.class).build();
		final CParameters tlv1 = new CMultiprotocolBuilder().setMultiprotocolCapability(cap).build();

		final MultiprotocolCapability cap1 = new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.class).setSafi(
				UnicastSubsequentAddressFamily.class).build();
		final CParameters tlv2 = new CMultiprotocolBuilder().setMultiprotocolCapability(cap1).build();

		final Map<BGPTableType, Boolean> tt = Maps.newHashMap();
		tt.put(t, true);
		tt.put(t1, false);

		// final BGPParameter tlv3 = new GracefulCapability(false, 0, tt);

		final CParameters tlv4 = new CAs4BytesBuilder().setAs4BytesCapability(
				new As4BytesCapabilityBuilder().setAsNumber(new AsNumber((long) 40)).build()).build();

		// assertFalse(((GracefulCapability) tlv3).isRestartFlag());

		// assertEquals(0, ((GracefulCapability) tlv3).getRestartTimerValue());

		assertFalse(tlv1.equals(tlv2));

		// assertNotSame(tlv1.hashCode(), tlv3.hashCode());

		// assertNotSame(tlv2.toString(), tlv3.toString());

		// assertEquals(((GracefulCapability) tlv3).getTableTypes(), tt);

		assertEquals(cap.getSafi(), cap1.getSafi());

		assertNotSame(cap.getAfi(), cap1.getAfi());

		assertEquals(40, ((CAs4Bytes) tlv4).getAs4BytesCapability().getAsNumber().getValue().longValue());

		// FIXME: no generated toString
		// assertEquals(new As4BytesBuilder().setCAs4Bytes(new CAs4BytesBuilder().setAsNumber(new AsNumber((long)
		// 40)).build()).build().toString(), tlv4.toString());
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
		final Notification msg = new KeepaliveBuilder().build();
		assertTrue(msg instanceof Keepalive);
	}

	@Test
	public void testBGPNotificationMessage() {
		final Notify msg = new NotifyBuilder().setErrorCode(BGPError.AS_PATH_MALFORMED.getCode()).setErrorSubcode(
				BGPError.AS_PATH_MALFORMED.getSubcode()).build();
		assertTrue(msg instanceof Notify);
		assertEquals(BGPError.AS_PATH_MALFORMED.getCode(), msg.getErrorCode().shortValue());
		assertEquals(BGPError.AS_PATH_MALFORMED.getSubcode(), msg.getErrorSubcode().shortValue());
		assertNull(msg.getData());
	}

	@Test
	public void testBGPOpenMessage() {
		final Notification msg = new OpenBuilder().setMyAsNumber(58).setHoldTimer(5).build();
		assertNull(((Open) msg).getBgpParameters());
	}

	@Test
	public void testToString() {
		final Notification o = new OpenBuilder().setMyAsNumber(58).setHoldTimer(5).build();
		final Notification n = new NotifyBuilder().setErrorCode(BGPError.AS_PATH_MALFORMED.getCode()).setErrorSubcode(
				BGPError.AS_PATH_MALFORMED.getSubcode()).build();
		assertNotSame(o.toString(), n.toString());
	}
}
