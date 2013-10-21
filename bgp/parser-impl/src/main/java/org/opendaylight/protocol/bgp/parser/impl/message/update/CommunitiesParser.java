/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.Arrays;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CAsSpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CInet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.COpaqueExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CRouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.CRouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.as.specific.extended.community.AsSpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.inet4.specific.extended.community.Inet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.opaque.extended.community.OpaqueExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.route.origin.extended.community.RouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.route.target.extended.community.RouteTargetExtendedCommunityBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for Extended Communities Path Attribute.
 */
public final class CommunitiesParser {

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
	static Community parseCommunity(final byte[] bytes) throws BGPDocumentedException {
		if (bytes.length != COMMUNITY_LENGTH) {
			throw new BGPDocumentedException("Community with wrong length: " + bytes.length, BGPError.OPT_ATTR_ERROR);
		}
		if (Arrays.equals(bytes, new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x01 })) {
			return CommunityUtil.NO_EXPORT;
		} else if (Arrays.equals(bytes, new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x02 })) {
			return CommunityUtil.NO_ADVERTISE;
		} else if (Arrays.equals(bytes, new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x03 })) {
			return CommunityUtil.NO_EXPORT_SUBCONFED;
		}
		return CommunityUtil.create((ByteArray.bytesToLong(Arrays.copyOfRange(bytes, 0, AS_NUMBER_LENGTH))),
				ByteArray.bytesToInt(Arrays.copyOfRange(bytes, AS_NUMBER_LENGTH, AS_NUMBER_LENGTH + AS_NUMBER_LENGTH)));
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
		final int type = UnsignedBytes.toInt(bytes[0]);
		final int subType = UnsignedBytes.toInt(bytes[1]);
		final byte[] value = ByteArray.subByte(bytes, TYPE_LENGTH, bytes.length - TYPE_LENGTH);

		switch (type) {
		case 0:
			if (subType == 2) {
				return new CRouteTargetExtendedCommunityBuilder().setRouteTargetExtendedCommunity(
						new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(
								new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
								ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build();
			} else if (subType == 3) {
				return new CRouteOriginExtendedCommunityBuilder().setRouteOriginExtendedCommunity(
						new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(
								new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
								ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build();
			} else {
				return new CAsSpecificExtendedCommunityBuilder().setAsSpecificExtendedCommunity(
						new AsSpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(
								new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
								ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build();
			}
		case 40: // 01000000
			return new CAsSpecificExtendedCommunityBuilder().setAsSpecificExtendedCommunity(
					new AsSpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(
							new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
							ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build();
		case 2:
			if (subType == 2) {
				return new CRouteTargetExtendedCommunityBuilder().setRouteTargetExtendedCommunity(
						new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(
								new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
								ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build();
			} else if (subType == 3) {
				return new CRouteOriginExtendedCommunityBuilder().setRouteOriginExtendedCommunity(
						new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(
								new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
								ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build();
			} else {
				throw new BGPDocumentedException("Could not parse Extended Community subtype: " + subType, BGPError.OPT_ATTR_ERROR);
			}
		case 1:
			if (subType == 2) {
				return new CRouteTargetExtendedCommunityBuilder().setRouteTargetExtendedCommunity(
						new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(
								new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
								ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build();
			} else if (subType == 3) {
				return new CRouteOriginExtendedCommunityBuilder().setRouteOriginExtendedCommunity(
						new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(
								new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
								ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build();
			} else {
				return new CInet4SpecificExtendedCommunityBuilder().setInet4SpecificExtendedCommunity(
						new Inet4SpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(
								Ipv4Util.addressForBytes(ByteArray.subByte(value, 0, 4))).setLocalAdministrator(
								ByteArray.subByte(value, 4, 2)).build()).build();
			}
		case 41: // 01000001
			return new CInet4SpecificExtendedCommunityBuilder().setInet4SpecificExtendedCommunity(
					new Inet4SpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(
							Ipv4Util.addressForBytes(ByteArray.subByte(value, 0, 4))).setLocalAdministrator(ByteArray.subByte(value, 4, 2)).build()).build();
		case 3:
			return new COpaqueExtendedCommunityBuilder().setOpaqueExtendedCommunity(
					new OpaqueExtendedCommunityBuilder().setTransitive(false).setValue(value).build()).build();
		case 43: // 01000011
			return new COpaqueExtendedCommunityBuilder().setOpaqueExtendedCommunity(
					new OpaqueExtendedCommunityBuilder().setTransitive(true).setValue(value).build()).build();
		default:
			throw new BGPDocumentedException("Could not parse Extended Community type: " + type, BGPError.OPT_ATTR_ERROR);
		}
	}
}
