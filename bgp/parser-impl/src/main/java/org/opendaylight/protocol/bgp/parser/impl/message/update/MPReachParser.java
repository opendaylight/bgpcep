/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.SimpleAddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.impl.SimpleSubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.mp.reach.nlri.advertized.routes.nlri.LinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.advertized.routes.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.advertized.routes.nlri.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.reach.nlri.advertized.routes.nlri.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv6NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.c.ipv4.next.hop.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.c.ipv6.next.hop.Ipv6NextHopBuilder;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for MP_REACH or MP_UNREACH fields.
 */
public class MPReachParser {
	private static final int ADDRESS_FAMILY_IDENTIFIER_SIZE = 2;

	private static final int SUBSEQUENT_ADDRESS_FAMILY_IDENTIFIER_SIZE = 1;

	private static final int NEXT_HOP_LENGTH_SIZE = 1;

	private static final int RESERVED_SIZE = 1;

	private static final AddressFamilyRegistry afiReg = SimpleAddressFamilyRegistry.INSTANCE;
	private static final SubsequentAddressFamilyRegistry safiReg = SimpleSubsequentAddressFamilyRegistry.INSTANCE;

	private MPReachParser() {
	}

	static MpUnreachNlri parseMPUnreach(final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		final MpUnreachNlriBuilder builder = new MpUnreachNlriBuilder();

		final int afiVal = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, ADDRESS_FAMILY_IDENTIFIER_SIZE));
		final Class<? extends AddressFamily> afi = afiReg.classForFamily(afiVal);
		if (afi == null) {
			throw new BGPParsingException("Address Family Identifier: '" + afiVal + "' not supported.");
		}
		byteOffset += ADDRESS_FAMILY_IDENTIFIER_SIZE;
		builder.setAfi(afi);

		final int safiVal = UnsignedBytes.toInt(bytes[byteOffset]);
		final Class<? extends SubsequentAddressFamily> safi = safiReg.classForFamily(safiVal);
		if (safi == null) {
			throw new BGPParsingException("Subsequent Address Family Identifier: '" + safiVal + "' not supported.");
		}
		byteOffset += SUBSEQUENT_ADDRESS_FAMILY_IDENTIFIER_SIZE;
		builder.setSafi(safi);

		final WithdrawnRoutes routes = new WithdrawnRoutesBuilder().setNlri(
				(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.destination.Nlri) chooseReachParser(
						afi, safi, ByteArray.subByte(bytes, byteOffset, bytes.length - byteOffset))).build();
		builder.setWithdrawnRoutes(routes);
		return builder.build();
	}

	static MpReachNlri parseMPReach(final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		final MpReachNlriBuilder builder = new MpReachNlriBuilder();

		final int afiVal = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, ADDRESS_FAMILY_IDENTIFIER_SIZE));
		final Class<? extends AddressFamily> afi = afiReg.classForFamily(afiVal);
		if (afi == null) {
			throw new BGPParsingException("Address Family Identifier: '" + afiVal + "' not supported.");
		}
		byteOffset += ADDRESS_FAMILY_IDENTIFIER_SIZE;
		builder.setAfi(afi);

		final int safiVal = UnsignedBytes.toInt(bytes[byteOffset]);
		final Class<? extends SubsequentAddressFamily> safi = safiReg.classForFamily(safiVal);
		if (safi == null) {
			throw new BGPParsingException("Subsequent Address Family Identifier: '" + safiVal + "' not supported.");
		}
		byteOffset += SUBSEQUENT_ADDRESS_FAMILY_IDENTIFIER_SIZE;
		builder.setSafi(safi);

		final int nextHopLength = UnsignedBytes.toInt(bytes[byteOffset]);
		byteOffset += NEXT_HOP_LENGTH_SIZE;
		final CNextHop nextHop = parseNextHop(ByteArray.subByte(bytes, byteOffset, nextHopLength));
		byteOffset += nextHopLength + RESERVED_SIZE;
		builder.setCNextHop(nextHop);

		final AdvertizedRoutes routes = new AdvertizedRoutesBuilder().setNlri(
				(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.destination.Nlri) chooseReachParser(
						afi, safi, ByteArray.subByte(bytes, byteOffset, bytes.length - (byteOffset)))).build();
		builder.setAdvertizedRoutes(routes);
		return builder.build();
	}

	private static Nlri chooseReachParser(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi,
			final byte[] bytes) throws BGPParsingException {
		if (afi == Ipv4AddressFamily.class) {
			final List<Ipv4Prefix> nlri4 = Ipv4Util.prefixListForBytes(bytes);
			return new Ipv4Builder().setIpv4Prefixes(nlri4).build();
		} else if (afi == Ipv6AddressFamily.class) {
			final List<Ipv6Prefix> nlri6 = Ipv6Util.prefixListForBytes(bytes);
			return new Ipv6Builder().setIpv6Prefixes(nlri6).build();
		} else if (afi == LinkstateAddressFamily.class) {
			return new LinkstateBuilder().setCLinkstateDestination(LinkstateAttributeParser.parseLSNlri(safi, bytes)).build();
		}
		return null;
	}

	private static CNextHop parseNextHop(final byte[] bytes) throws BGPParsingException {
		final CNextHop addr;
		switch (bytes.length) {
		case 4:
			addr = new CIpv4NextHopBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(Ipv4Util.addressForBytes(bytes)).build()).build();
			break;
		case 16:
			addr = new CIpv6NextHopBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(Ipv6Util.addressForBytes(bytes)).build()).build();
			break;
		case 32:
			addr = new CIpv6NextHopBuilder().setIpv6NextHop(
					new Ipv6NextHopBuilder().setGlobal(Ipv6Util.addressForBytes(ByteArray.subByte(bytes, 0, 16))).setLinkLocal(
							Ipv6Util.addressForBytes(ByteArray.subByte(bytes, 16, 16))).build()).build();
			break;
		default:
			throw new BGPParsingException("Cannot parse NEXT_HOP attribute. Wrong bytes length: " + bytes.length);
		}
		return addr;
	}
}
