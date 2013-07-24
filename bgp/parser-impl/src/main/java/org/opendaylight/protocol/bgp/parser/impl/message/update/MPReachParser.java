/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.Set;


import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.IPv4NextHop;
import org.opendaylight.protocol.bgp.concepts.IPv6NextHop;
import org.opendaylight.protocol.bgp.concepts.NextHop;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.IPv4MP;
import org.opendaylight.protocol.bgp.parser.impl.IPv6MP;
import org.opendaylight.protocol.bgp.parser.impl.MPReach;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.Prefix;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for MP_REACH or MP_UNREACH fields.
 */
public class MPReachParser {

	private static final int ADDRESS_FAMILY_IDENTIFIER_SIZE = 2;

	private static final int SUBSEQUENT_ADDRESS_FAMILY_IDENTIFIER_SIZE = 1;

	private static final int NEXT_HOP_LENGTH_SIZE = 1;

	private static final int RESERVED_SIZE = 1;

	public static final int LS_AFI = 16388;

	public static final int LS_SAFI = 71;

	private MPReachParser() {

	}

	static MPReach<?> parseMPUnreach(final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		final BGPAddressFamily afi = parseAfi(ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, ADDRESS_FAMILY_IDENTIFIER_SIZE)));
		byteOffset += ADDRESS_FAMILY_IDENTIFIER_SIZE;
		final BGPSubsequentAddressFamily safi = parseSafi(UnsignedBytes.toInt(bytes[byteOffset]));
		byteOffset += SUBSEQUENT_ADDRESS_FAMILY_IDENTIFIER_SIZE;
		return chooseUnreachParser(afi, safi, ByteArray.subByte(bytes, byteOffset, bytes.length - byteOffset));
	}

	static MPReach<?> parseMPReach(final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		final BGPAddressFamily afi = parseAfi(ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, ADDRESS_FAMILY_IDENTIFIER_SIZE)));
		byteOffset += ADDRESS_FAMILY_IDENTIFIER_SIZE;
		final BGPSubsequentAddressFamily safi = parseSafi(UnsignedBytes.toInt(bytes[byteOffset]));
		byteOffset += SUBSEQUENT_ADDRESS_FAMILY_IDENTIFIER_SIZE;
		final int nextHopLength = UnsignedBytes.toInt(bytes[byteOffset]);
		byteOffset += NEXT_HOP_LENGTH_SIZE;
		final NextHop<?> nextHop = parseNextHop(ByteArray.subByte(bytes, byteOffset, nextHopLength));
		byteOffset += nextHopLength + RESERVED_SIZE;
		return chooseReachParser(afi, safi, nextHop, ByteArray.subByte(bytes, byteOffset, bytes.length - (byteOffset)));
	}

	public static BGPSubsequentAddressFamily parseSafi(final int type) throws BGPParsingException {
		switch (type) {
		case 1:
			return BGPSubsequentAddressFamily.Unicast;
		case LS_SAFI:
			return BGPSubsequentAddressFamily.Linkstate;
		case 128:
			return BGPSubsequentAddressFamily.MPLSLabeledVPN;
		default:
			throw new BGPParsingException("Subsequent Address Family Identifier: '" + type + "' not supported.");
		}
	}

	public static BGPAddressFamily parseAfi(final int type) throws BGPParsingException {
		switch (type) {
		case 1:
			return BGPAddressFamily.IPv4;
		case 2:
			return BGPAddressFamily.IPv6;
		case LS_AFI:
			return BGPAddressFamily.LinkState;
		default:
			throw new BGPParsingException("Address Family Identifier: '" + type + "' not supported.");
		}
	}

	private static MPReach<?> chooseUnreachParser(final BGPAddressFamily afi, final BGPSubsequentAddressFamily safi, final byte[] bytes)
			throws BGPParsingException {
		switch (afi) {
		case IPv4:
			final Set<Prefix<IPv4Address>> nlri4 = IPv4.FAMILY.prefixListForBytes(bytes);
			return new IPv4MP(false, null, nlri4);
		case IPv6:
			final Set<Prefix<IPv6Address>> nlri6 = IPv6.FAMILY.prefixListForBytes(bytes);
			return new IPv6MP(false, null, nlri6);
		case LinkState:
			return LinkStateParser.parseLSNlri(false, safi, null, bytes);
		default:
			return null;
		}
	}

	private static MPReach<?> chooseReachParser(final BGPAddressFamily afi, final BGPSubsequentAddressFamily safi,
			final NextHop<?> nextHop, final byte[] bytes) throws BGPParsingException {
		switch (afi) {
		case IPv4:
			final Set<Prefix<IPv4Address>> nlri4 = IPv4.FAMILY.prefixListForBytes(bytes);
			return new IPv4MP(true, (IPv4NextHop) nextHop, nlri4);
		case IPv6:
			final Set<Prefix<IPv6Address>> nlri6 = IPv6.FAMILY.prefixListForBytes(bytes);
			return new IPv6MP(true, (IPv6NextHop) nextHop, nlri6);
		case LinkState:
			return LinkStateParser.parseLSNlri(true, safi, nextHop, bytes);
		default:
			return null;
		}
	}

	private static NextHop<?> parseNextHop(final byte[] bytes) throws BGPParsingException {
		final NextHop<?> addr;
		switch (bytes.length) {
		case 4:
			addr = new IPv4NextHop(IPv4.FAMILY.addressForBytes(bytes));
			break;
		case 16:
			addr = new IPv6NextHop(IPv6.FAMILY.addressForBytes(bytes));
			break;
		case 32:
			addr = new IPv6NextHop(IPv6.FAMILY.addressForBytes(ByteArray.subByte(bytes, 0, 16)), IPv6.FAMILY.addressForBytes(ByteArray.subByte(bytes, 16, 16)));
			break;
		default:
			throw new BGPParsingException("Cannot parse NEXT_HOP attribute. Wrong bytes length: " + bytes.length);
		}
		return addr;
	}
}
