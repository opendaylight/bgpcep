/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.Set;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.IPv4MP;
import org.opendaylight.protocol.bgp.parser.impl.IPv6MP;
import org.opendaylight.protocol.bgp.parser.impl.MPReach;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv4NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv6NextHop;
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

	private MPReachParser() {

	}

	static MPReach<?> parseMPUnreach(final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		final BgpAddressFamily afi = BgpAddressFamily.forValue(ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset,
				ADDRESS_FAMILY_IDENTIFIER_SIZE)));
		if (afi == null)
			throw new BGPParsingException("Address Family Identifier: '"
					+ ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, ADDRESS_FAMILY_IDENTIFIER_SIZE)) + "' not supported.");
		byteOffset += ADDRESS_FAMILY_IDENTIFIER_SIZE;
		final BgpSubsequentAddressFamily safi = BgpSubsequentAddressFamily.forValue(UnsignedBytes.toInt(bytes[byteOffset]));
		if (safi == null)
			throw new BGPParsingException("Subsequent Address Family Identifier: '" + UnsignedBytes.toInt(bytes[byteOffset])
					+ "' not supported.");
		byteOffset += SUBSEQUENT_ADDRESS_FAMILY_IDENTIFIER_SIZE;
		return chooseUnreachParser(afi, safi, ByteArray.subByte(bytes, byteOffset, bytes.length - byteOffset));
	}

	static MPReach<?> parseMPReach(final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		final BgpAddressFamily afi = BgpAddressFamily.forValue(ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset,
				ADDRESS_FAMILY_IDENTIFIER_SIZE)));
		if (afi == null)
			throw new BGPParsingException("Address Family Identifier: '"
					+ ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, ADDRESS_FAMILY_IDENTIFIER_SIZE)) + "' not supported.");
		byteOffset += ADDRESS_FAMILY_IDENTIFIER_SIZE;
		final BgpSubsequentAddressFamily safi = BgpSubsequentAddressFamily.forValue(UnsignedBytes.toInt(bytes[byteOffset]));
		if (safi == null)
			throw new BGPParsingException("Subsequent Address Family Identifier: '" + UnsignedBytes.toInt(bytes[byteOffset])
					+ "' not supported.");
		byteOffset += SUBSEQUENT_ADDRESS_FAMILY_IDENTIFIER_SIZE;
		final int nextHopLength = UnsignedBytes.toInt(bytes[byteOffset]);
		byteOffset += NEXT_HOP_LENGTH_SIZE;
		final CNextHop nextHop = parseNextHop(ByteArray.subByte(bytes, byteOffset, nextHopLength));
		byteOffset += nextHopLength + RESERVED_SIZE;
		return chooseReachParser(afi, safi, nextHop, ByteArray.subByte(bytes, byteOffset, bytes.length - (byteOffset)));
	}

	private static MPReach<?> chooseUnreachParser(final BgpAddressFamily afi, final BgpSubsequentAddressFamily safi, final byte[] bytes)
			throws BGPParsingException {
		switch (afi) {
		case Ipv4:
			final Set<Prefix<IPv4Address>> nlri4 = IPv4.FAMILY.prefixListForBytes(bytes);
			return new IPv4MP(false, null, nlri4);
		case Ipv6:
			final Set<Prefix<IPv6Address>> nlri6 = IPv6.FAMILY.prefixListForBytes(bytes);
			return new IPv6MP(false, null, nlri6);
		case Linkstate:
			return LinkStateParser.parseLSNlri(false, safi, null, bytes);
		default:
			return null;
		}
	}

	private static MPReach<?> chooseReachParser(final BgpAddressFamily afi, final BgpSubsequentAddressFamily safi, final CNextHop nextHop,
			final byte[] bytes) throws BGPParsingException {
		switch (afi) {
		case Ipv4:
			final Set<Prefix<IPv4Address>> nlri4 = IPv4.FAMILY.prefixListForBytes(bytes);
			return new IPv4MP(true, (CIpv4NextHop) nextHop, nlri4);
		case Ipv6:
			final Set<Prefix<IPv6Address>> nlri6 = IPv6.FAMILY.prefixListForBytes(bytes);
			return new IPv6MP(true, (CIpv6NextHop) nextHop, nlri6);
		case Linkstate:
			return LinkStateParser.parseLSNlri(true, safi, nextHop, bytes);
		default:
			return null;
		}
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
