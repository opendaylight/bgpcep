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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.AsSpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.OpaqueExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as.specific.extended.community._case.AsSpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.opaque.extended.community._case.OpaqueExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.extended.community._case.RouteTargetExtendedCommunityBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for Extended Communities Path Attribute.
 */
public final class CommunitiesParser {

	protected static final int EXTENDED_COMMUNITY_LENGTH = 8;

	protected static final int COMMUNITY_LENGTH = 4;

	private static final int TYPE_LENGTH = 2;

	private static final int AS_NUMBER_LENGTH = 2;

	private static final int AS_LOCAL_ADMIN_LENGTH = 4;

	protected static final short AS_TYPE_TRANS = 0;

	protected static final short AS_TYPE_NON_TRANS = 40;

	protected static final short INET_TYPE_TRANS = 1;

	protected static final short INET_TYPE_NON_TRANS = 41;

	protected static final short OPAQUE_TYPE_TRANS = 3;

	protected static final short OPAQUE_TYPE_NON_TRANS = 43;

	protected static final short ROUTE_TYPE_ONLY = 2;

	protected static final short ROUTE_TARGET_SUBTYPE = 2;

	protected static final short ROUTE_ORIGIN_SUBTYPE = 3;

	private static final byte[] NO_EXPORT = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x01 };

	private static final byte[] NO_ADVERTISE = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x02 };

	private static final byte[] NO_EXPORT_SUBCONFED = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x03 };

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
		if (Arrays.equals(bytes, NO_EXPORT)) {
			return CommunityUtil.NO_EXPORT;
		} else if (Arrays.equals(bytes, NO_ADVERTISE)) {
			return CommunityUtil.NO_ADVERTISE;
		} else if (Arrays.equals(bytes, NO_EXPORT_SUBCONFED)) {
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
	public static ExtendedCommunities parseExtendedCommunity(final byte[] bytes) throws BGPDocumentedException {
		final int type = UnsignedBytes.toInt(bytes[0]);
		final int subType = UnsignedBytes.toInt(bytes[1]);
		final byte[] value = ByteArray.subByte(bytes, TYPE_LENGTH, bytes.length - TYPE_LENGTH);

		ExtendedCommunities comm;
		switch (type) {
		case AS_TYPE_TRANS:
			if (subType == ROUTE_TARGET_SUBTYPE) {
				comm = new ExtendedCommunitiesBuilder().setCommType(AS_TYPE_TRANS).setCommSubType(ROUTE_TARGET_SUBTYPE).setExtendedCommunity(
						new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
								new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(
										new ShortAsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
												ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build()).build();
			} else if (subType == ROUTE_ORIGIN_SUBTYPE) {
				comm = new ExtendedCommunitiesBuilder().setCommType(AS_TYPE_TRANS).setCommSubType(ROUTE_ORIGIN_SUBTYPE).setExtendedCommunity(
						new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
								new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(
										new ShortAsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
												ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build()).build();
			} else {
				comm = new ExtendedCommunitiesBuilder().setCommType(AS_TYPE_TRANS).setExtendedCommunity(
						new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
								new AsSpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(
										new ShortAsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
												ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build()).build();
			}
			break;
		case AS_TYPE_NON_TRANS:
			comm = new ExtendedCommunitiesBuilder().setCommType(AS_TYPE_NON_TRANS).setExtendedCommunity(
					new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
							new AsSpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(
									new ShortAsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
											ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build()).build();
			break;
		case ROUTE_TYPE_ONLY:
			if (subType == ROUTE_TARGET_SUBTYPE) {
				comm = new ExtendedCommunitiesBuilder().setCommType(ROUTE_TYPE_ONLY).setCommSubType(ROUTE_TARGET_SUBTYPE).setExtendedCommunity(
						new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
								new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(
										new ShortAsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
												ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build()).build();
			} else if (subType == ROUTE_ORIGIN_SUBTYPE) {
				comm = new ExtendedCommunitiesBuilder().setCommType(ROUTE_TYPE_ONLY).setCommSubType(ROUTE_ORIGIN_SUBTYPE).setExtendedCommunity(
						new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
								new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(
										new ShortAsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
												ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build()).build();
			} else {
				throw new BGPDocumentedException("Could not parse Extended Community subtype: " + subType, BGPError.OPT_ATTR_ERROR);
			}
			break;
		case INET_TYPE_TRANS:
			if (subType == ROUTE_TARGET_SUBTYPE) {
				comm = new ExtendedCommunitiesBuilder().setCommType(INET_TYPE_TRANS).setCommSubType(ROUTE_TARGET_SUBTYPE).setExtendedCommunity(
						new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
								new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(
										new ShortAsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
												ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build()).build();
			} else if (subType == ROUTE_ORIGIN_SUBTYPE) {
				comm = new ExtendedCommunitiesBuilder().setCommType(INET_TYPE_TRANS).setCommSubType(ROUTE_ORIGIN_SUBTYPE).setExtendedCommunity(
						new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
								new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(
										new ShortAsNumber(ByteArray.bytesToLong(ByteArray.subByte(value, 0, AS_NUMBER_LENGTH)))).setLocalAdministrator(
												ByteArray.subByte(value, AS_NUMBER_LENGTH, AS_LOCAL_ADMIN_LENGTH)).build()).build()).build();
			} else {
				comm = new ExtendedCommunitiesBuilder().setCommType(INET_TYPE_TRANS).setExtendedCommunity(
						new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
								new Inet4SpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(
										Ipv4Util.addressForBytes(ByteArray.subByte(value, 0, 4))).setLocalAdministrator(
												ByteArray.subByte(value, 4, 2)).build()).build()).build();
			}
			break;
		case INET_TYPE_NON_TRANS:
			comm = new ExtendedCommunitiesBuilder().setCommType(INET_TYPE_NON_TRANS).setExtendedCommunity(
					new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
							new Inet4SpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(
									Ipv4Util.addressForBytes(ByteArray.subByte(value, 0, 4))).setLocalAdministrator(
											ByteArray.subByte(value, 4, 2)).build()).build()).build();
			break;
		case OPAQUE_TYPE_TRANS:
			comm = new ExtendedCommunitiesBuilder().setCommType(OPAQUE_TYPE_TRANS).setExtendedCommunity(
					new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(
							new OpaqueExtendedCommunityBuilder().setTransitive(false).setValue(value).build()).build()).build();
			break;
		case OPAQUE_TYPE_NON_TRANS:
			comm = new ExtendedCommunitiesBuilder().setCommType(OPAQUE_TYPE_NON_TRANS).setExtendedCommunity(
					new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(
							new OpaqueExtendedCommunityBuilder().setTransitive(true).setValue(value).build()).build()).build();
			break;
		default:
			throw new BGPDocumentedException("Could not parse Extended Community type: " + type, BGPError.OPT_ATTR_ERROR);
		}
		return comm;
	}
}
