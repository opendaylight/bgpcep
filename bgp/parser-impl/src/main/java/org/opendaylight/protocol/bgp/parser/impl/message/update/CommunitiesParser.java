/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.Arrays;

import org.opendaylight.protocol.bgp.concepts.ASSpecificExtendedCommunity;
import org.opendaylight.protocol.bgp.concepts.ExtendedCommunity;
import org.opendaylight.protocol.bgp.concepts.Inet4SpecificExtendedCommunity;
import org.opendaylight.protocol.bgp.concepts.OpaqueExtendedCommunity;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.CommunityImpl;
import org.opendaylight.protocol.bgp.parser.impl.RouteOriginCommunity;
import org.opendaylight.protocol.bgp.parser.impl.RouteTargetCommunity;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for Extended Communities Path Attribute.
 */
public class CommunitiesParser {

	public static final int EXTENDED_COMMUNITY_LENGTH = 8; // bytes

	public static final int COMMUNITY_LENGTH = 4; // bytes

	private static final int TYPE_LENGTH = 2; // bytes

	private static final int AS_NUMBER_LENGTH = 2; // bytes

	private static final int AS_LOCAL_ADMIN_LENGTH = 4; // bytes

	private CommunitiesParser() {

	}

	/**
	 * Parse known Community, if unknown, a new one will be created.
	 * 
	 * @param bytes byte array to be parsed
	 * @return new Community
	 * @throws BGPDocumentedException
	 */
	static CommunityImpl parseCommunity(final byte[] bytes) throws BGPDocumentedException {
		if (bytes.length != COMMUNITY_LENGTH)
			throw new BGPDocumentedException("Community with wrong length: " + bytes.length, BGPError.OPT_ATTR_ERROR);
		if (Arrays.equals(bytes, new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x01 })) {
			return CommunityImpl.NO_EXPORT;
		} else if (Arrays.equals(bytes, new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x02 })) {
			return CommunityImpl.NO_ADVERTISE;
		} else if (Arrays.equals(bytes, new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x03 })) {
			return CommunityImpl.NO_EXPORT_SUBCONFED;
		}
		return new CommunityImpl(new AsNumber(ByteArray.bytesToLong(Arrays.copyOfRange(bytes, 0, AS_NUMBER_LENGTH))), ByteArray.bytesToInt(Arrays.copyOfRange(
				bytes, AS_NUMBER_LENGTH, AS_NUMBER_LENGTH + AS_NUMBER_LENGTH)));
	}

	/**
	 * Parse Extended Community according to their type.
	 * 
	 * @param bytes byte array to be parsed
	 * @return new Specific Extended Community
	 * @throws BGPDocumentedException if the type is not recognized
	 */
	@VisibleForTesting
	public static ExtendedCommunity parseExtendedCommunity(final byte[] bytes) throws BGPDocumentedException {
		// final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, 0, TYPE_LENGTH));
		final int type = UnsignedBytes.toInt(bytes[0]);
		final int subType = UnsignedBytes.toInt(bytes[1]);
		final byte[] value = ByteArray.subByte(bytes, TYPE_LENGTH, bytes.length - TYPE_LENGTH);
		switch (type) {
		case 0:
			if (subType == 2) {
				return new RouteTargetCommunity(new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH))), ByteArray.subByte(
						value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH));
			} else if (subType == 3) {
				return new RouteOriginCommunity(new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH))), ByteArray.subByte(
						value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH));
			} else
				return new ASSpecificExtendedCommunity(false, subType, new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0,
						AS_NUMBER_LENGTH))), ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH));
		case 40: // 01000000
			return new ASSpecificExtendedCommunity(true, subType, new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0,
					AS_NUMBER_LENGTH))), ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH));
		case 2:
			if (subType == 2) {
				return new RouteTargetCommunity(new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH))), ByteArray.subByte(
						value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH));
			} else if (subType == 3) {
				return new RouteOriginCommunity(new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH))), ByteArray.subByte(
						value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH));
			} else
				throw new BGPDocumentedException("Could not parse Extended Community subtype: " + subType, BGPError.OPT_ATTR_ERROR);
		case 1:
			if (subType == 2) {
				return new RouteTargetCommunity(new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH))), ByteArray.subByte(
						value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH));
			} else if (subType == 3) {
				return new RouteOriginCommunity(new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH))), ByteArray.subByte(
						value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH));
			} else
				return new Inet4SpecificExtendedCommunity(false, subType, new IPv4Address(ByteArray.subByte(value, 0, 4)), ByteArray.subByte(
						value, 4, 2));
		case 41: // 01000001
			return new Inet4SpecificExtendedCommunity(true, subType, new IPv4Address(ByteArray.subByte(value, 0, 4)), ByteArray.subByte(
					value, 4, 2));
		case 3:
			return new OpaqueExtendedCommunity(false, subType, value);
		case 43: // 01000011
			return new OpaqueExtendedCommunity(true, subType, value);
		default:
			throw new BGPDocumentedException("Could not parse Extended Community type: " + type, BGPError.OPT_ATTR_ERROR);
		}
	}
}
