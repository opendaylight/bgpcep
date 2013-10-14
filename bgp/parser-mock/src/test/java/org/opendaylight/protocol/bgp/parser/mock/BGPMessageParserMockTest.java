/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.mock;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.c.multiprotocol.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class BGPMessageParserMockTest {

	private final byte[][] inputBytes = new byte[11][];
	private final List<Update> messages = Lists.newArrayList();

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
	@Ignore
	// FIXME : BUG-94
	public void testGetUpdateMessage() throws DeserializerException, DocumentedException, IOException {
		final Map<byte[], Notification> updateMap = Maps.newHashMap();
		for (int i = 0; i < this.inputBytes.length; i++) {
			updateMap.put(this.inputBytes[i], this.messages.get(i));
		}

		final BGPMessageParserMock mockParser = new BGPMessageParserMock(updateMap);

		for (int i = 0; i < this.inputBytes.length; i++) {
			assertEquals(this.messages.get(i), mockParser.parse(this.inputBytes[i]));
		}
		// assertThat(this.messages.get(3), not(mockParser.parse(this.inputBytes[8]).get(0)));
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
		final Map<byte[], Notification> updateMap = Maps.newHashMap();
		for (int i = 0; i < this.inputBytes.length; i++) {
			updateMap.put(this.inputBytes[i], this.messages.get(i));
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
	private Update fillMessages(final long asn) throws UnknownHostException {

		// FIXME: to be fixed in testing phase
		/*final List<AsSequence> asnums = Lists.newArrayList(new AsSequenceBuilder().setAs(new AsNumber(asn)).build());
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

		return new BGPUpdateMessageImpl(addedObjects, Collections.<Identifier> emptySet());*/

		return new UpdateBuilder().build();
	}

	@Test
	public void testGetOpenMessage() throws DeserializerException, DocumentedException, IOException {
		final Map<byte[], Notification> openMap = Maps.newHashMap();

		final Set<BgpTableType> type = Sets.newHashSet();
		type.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class));

		final List<BgpParameters> params = Lists.newArrayList();

		final CParameters par = new CMultiprotocolBuilder().setMultiprotocolCapability(
				new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.class).setSafi(MplsLabeledVpnSubsequentAddressFamily.class).build()).build();
		params.add(new BgpParametersBuilder().setCParameters(par).build());

		final byte[] input = new byte[] { 5, 8, 13, 21 };

		openMap.put(
				input,
				new OpenBuilder().setMyAsNumber(30).setHoldTimer(30).setBgpParameters(params).setVersion(new ProtocolVersion((short) 4)).build());

		final BGPMessageParserMock mockParser = new BGPMessageParserMock(openMap);

		final Set<BgpTableType> result = Sets.newHashSet();
		for (final BgpParameters p : ((Open) mockParser.parse(input)).getBgpParameters()) {
			final CParameters cp = p.getCParameters();
			final BgpTableType t = new BgpTableTypeImpl(((CMultiprotocol) cp).getMultiprotocolCapability().getAfi(), ((CMultiprotocol) cp).getMultiprotocolCapability().getSafi());
			result.add(t);
		}

		assertEquals(type, result);
	}
}
