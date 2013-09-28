/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.mock;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BGPObject;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.linkstate.NetworkObjectState;
import org.opendaylight.protocol.bgp.linkstate.NetworkRouteState;
import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.parser.BGPRoute;
import org.opendaylight.protocol.bgp.parser.BGPUpdateMessage;
import org.opendaylight.protocol.bgp.parser.impl.BGPUpdateMessageImpl;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.bgp.parser.parameter.MultiprotocolCapability;
import org.opendaylight.protocol.bgp.util.BGPIPv6RouteImpl;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.IPv6Prefix;
import org.opendaylight.protocol.concepts.Identifier;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AsPathSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.CAListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.c.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.c.a.list.AsSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv6NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.c.ipv6.next.hop.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class BGPMessageParserMockTest {

	private final byte[][] inputBytes = new byte[11][];
	private final List<BGPUpdateMessage> messages = new ArrayList<BGPUpdateMessage>();

	@Before
	public void init() throws Exception {
		// Creating input bytes and update messages
		for (int i = 0; i < this.inputBytes.length; i++) {
			this.inputBytes[i] = this.fillInputBytes(i);
			this.messages.add(this.fillMessages(i));
		}
	}

	/**
	 * Test if mock implementation of parser returns correct message
	 * 
	 * @throws DocumentedException
	 * @throws DeserializerException
	 * @throws IOException
	 */
	@Test
	public void testGetUpdateMessage() throws DeserializerException, DocumentedException, IOException {
		final Map<byte[], List<Notification>> updateMap = Maps.newHashMap();
		for (int i = 0; i < this.inputBytes.length; i++) {
			updateMap.put(this.inputBytes[i], Lists.newArrayList((Notification) this.messages.get(i)));
		}

		final BGPMessageParserMock mockParser = new BGPMessageParserMock(updateMap);

		for (int i = 0; i < this.inputBytes.length; i++) {
			assertEquals(this.messages.get(i), mockParser.parse(this.inputBytes[i]).get(0));
		}
		assertThat(this.messages.get(3), not(mockParser.parse(this.inputBytes[8]).get(0)));
	}

	/**
	 * Test if method throws IllegalArgumentException after finding no BGPUpdateMessage associated with given byte[] key
	 * 
	 * @throws DocumentedException
	 * @throws DeserializerException
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testGetUpdateMessageException() throws DeserializerException, DocumentedException, IOException {
		final Map<byte[], List<Notification>> updateMap = Maps.newHashMap();
		for (int i = 0; i < this.inputBytes.length; i++) {
			updateMap.put(this.inputBytes[i], Lists.newArrayList((Notification) this.messages.get(i)));
		}

		final BGPMessageParserMock mockParser = new BGPMessageParserMock(updateMap);
		mockParser.parse(new byte[] { 7, 4, 6 });
	}

	/**
	 * Helper method to fill inputBytes variable
	 * 
	 * @param fileNumber parameter to distinguish between files from which bytes are read
	 */
	private byte[] fillInputBytes(final int fileNumber) throws Exception {

		final InputStream is = this.getClass().getResourceAsStream("/up" + fileNumber + ".bin");
		final ByteArrayOutputStream bis = new ByteArrayOutputStream();
		final byte[] data = new byte[60];
		int nRead = 0;

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			bis.write(data, 0, nRead);
		}
		bis.flush();
		return bis.toByteArray();
	}

	/**
	 * Helper method to fill messages variable
	 * 
	 * @param asn this parameter is passed to ASNumber constructor
	 */
	private BGPUpdateMessage fillMessages(final long asn) throws UnknownHostException {

		final List<AsSequence> asnums = Lists.newArrayList(new AsSequenceBuilder().setAs(new AsNumber(asn)).build());
		final List<AsPathSegment> asPath = Lists.newArrayList();
		asPath.add(new SegmentsBuilder().setCSegment(new CAListBuilder().setAsSequence(asnums).build()).build());
		final CNextHop nextHop = new CIpv6NextHopBuilder().setIpv6NextHop(
				new Ipv6NextHopBuilder().setGlobal(new Ipv6Address("2001:db8::1")).setLinkLocal(new Ipv6Address("fe80::c001:bff:fe7e:0")).build()).build();

		final Prefix<IPv6Address> pref1 = new IPv6Prefix(new IPv6Address(InetAddress.getByName("2001:db8:1:2::")), 64);
		final Prefix<IPv6Address> pref2 = new IPv6Prefix(new IPv6Address(InetAddress.getByName("2001:db8:1:1::")), 64);
		final Prefix<IPv6Address> pref3 = new IPv6Prefix(new IPv6Address(InetAddress.getByName("2001:db8:1::")), 64);

		final Set<BGPObject> addedObjects = new HashSet<BGPObject>();

		final NetworkRouteState nstate = new NetworkRouteState(new NetworkObjectState(asPath, Collections.<Community> emptySet(), Collections.<ExtendedCommunity> emptySet()), nextHop);
		final BaseBGPObjectState state = new BaseBGPObjectState(BgpOrigin.Igp, null);

		final BGPRoute route1 = new BGPIPv6RouteImpl(pref1, state, nstate);
		final BGPRoute route2 = new BGPIPv6RouteImpl(pref2, state, nstate);
		final BGPRoute route3 = new BGPIPv6RouteImpl(pref3, state, nstate);
		addedObjects.add(route1);
		addedObjects.add(route2);
		addedObjects.add(route3);

		return new BGPUpdateMessageImpl(addedObjects, Collections.<Identifier> emptySet());
	}

	@Test
	public void testGetOpenMessage() throws DeserializerException, DocumentedException, IOException {
		final Map<byte[], List<Notification>> openMap = Maps.newHashMap();

		final Set<BGPTableType> type = Sets.newHashSet();
		type.add(new BGPTableType(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class));

		final List<BGPParameter> params = Lists.newArrayList();
		params.add(new MultiprotocolCapability(new BGPTableType(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class)));

		final byte[] input = new byte[] { 5, 8, 13, 21 };

		openMap.put(input, Lists.newArrayList((Notification) new BGPOpenMessage(new AsNumber((long) 30), (short) 30, null, params)));

		final BGPMessageParserMock mockParser = new BGPMessageParserMock(openMap);

		final Set<BGPTableType> result = Sets.newHashSet();
		for (final BGPParameter p : ((BGPOpenMessage) mockParser.parse(input).get(0)).getOptParams()) {
			if (p instanceof MultiprotocolCapability) {
				result.add(((MultiprotocolCapability) p).getTableType());
			}
		}

		assertEquals(type, result);
	}
}
