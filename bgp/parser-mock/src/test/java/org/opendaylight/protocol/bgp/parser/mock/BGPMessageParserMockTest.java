/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.destination.destination.type.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.c.multiprotocol.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.CAListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.c.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.c.a.list.AsSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv6NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.c.ipv6.next.hop.Ipv6NextHopBuilder;
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
	public void testGetUpdateMessage() throws DeserializerException, DocumentedException, IOException {
		final Map<byte[], Notification> updateMap = Maps.newHashMap();
		for (int i = 0; i < this.inputBytes.length; i++) {
			updateMap.put(this.inputBytes[i], this.messages.get(i));
		}

		final BGPMessageParserMock mockParser = new BGPMessageParserMock(updateMap);

		for (int i = 0; i < this.inputBytes.length; i++) {
			assertEquals(this.messages.get(i), mockParser.parse(this.inputBytes[i]));
		}
		assertNotSame(this.messages.get(3), mockParser.parse(this.inputBytes[8]));
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

		final UpdateBuilder builder = new UpdateBuilder();

		final List<AsSequence> asnums = Lists.newArrayList(new AsSequenceBuilder().setAs(new AsNumber(asn)).build());
		final List<Segments> asPath = Lists.newArrayList();
		asPath.add(new SegmentsBuilder().setCSegment(new CAListBuilder().setAsSequence(asnums).build()).build());
		final CNextHop nextHop = (CNextHop) new CIpv6NextHopBuilder().setIpv6NextHop(
				new Ipv6NextHopBuilder().setGlobal(new Ipv6Address("2001:db8::1")).setLinkLocal(new Ipv6Address("fe80::c001:bff:fe7e:0")).build()).build();

		final Ipv6Prefix pref1 = new Ipv6Prefix("2001:db8:1:2::/64");
		final Ipv6Prefix pref2 = new Ipv6Prefix("2001:db8:1:1::/64");
		final Ipv6Prefix pref3 = new Ipv6Prefix("2001:db8:1::/64");

		PathAttributesBuilder paBuilder = new PathAttributesBuilder();
		paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build());
		paBuilder.setAsPath(new AsPathBuilder().setSegments(asPath).build());

		MpReachNlriBuilder mpReachBuilder = new MpReachNlriBuilder();
		mpReachBuilder.setAfi(Ipv6AddressFamily.class);
		mpReachBuilder.setSafi(UnicastSubsequentAddressFamily.class);
		mpReachBuilder.setCNextHop(nextHop);
		mpReachBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
				new DestinationIpv6Builder().setIpv6Prefixes(Lists.newArrayList(pref1, pref2, pref3)).build()).build());

		paBuilder.addAugmentation(PathAttributes1.class, new PathAttributes1Builder().setMpReachNlri(mpReachBuilder.build()).build());

		builder.setPathAttributes(paBuilder.build());

		return builder.build();
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
