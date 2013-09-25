/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.ASPath;
import org.opendaylight.protocol.bgp.concepts.ExtendedCommunity;
import org.opendaylight.protocol.bgp.concepts.IPv4NextHop;
import org.opendaylight.protocol.bgp.concepts.NextHop;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.ByteList;
import org.opendaylight.protocol.bgp.parser.impl.MPReach;
import org.opendaylight.protocol.bgp.parser.impl.PathAttribute;
import org.opendaylight.protocol.bgp.parser.impl.PathAttribute.TypeCode;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathSegmentParser.SegmentType;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedBytes;

/**
 * 
 * Parser for different Path Attributes. Each attributes has its own method for parsing.
 * 
 */
public class PathAttributeParser {

	private static final int FLAGS_LENGTH = 1;

	private static final int TYPE_LENGTH = 1;

	private PathAttributeParser() {

	}

	/**
	 * Parse path attribute header (the same for all path attributes) and set type, length and value fields.
	 * 
	 * @param bytes byte array to be parsed.
	 * @return generic Path Attribute
	 * @throws BGPParsingException
	 * @throws BGPDocumentedException
	 */
	public static PathAttribute parseAttribute(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		if (bytes == null || bytes.length == 0)
			throw new BGPParsingException("Insufficient length of byte array: " + bytes.length);
		final boolean[] bits = ByteArray.parseBits(bytes[0]);
		final PathAttribute attribute = new PathAttribute(bits[0], bits[1], bits[2], bits[3]);

		final int attrLength = (attribute.getAttrLengthSize() == 1) ? UnsignedBytes.toInt(bytes[2])
				: ByteArray.bytesToInt(ByteArray.subByte(bytes, 2, 2));
		attribute.setType(TypeCode.parseType(UnsignedBytes.toInt(bytes[1])));

		if (attribute.getType() == null && !attribute.isOptional())
			throw new BGPDocumentedException("Well known attribute not recognized.", BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED);

		attribute.setLength(attrLength);
		attribute.setValue(chooseParser(attribute.getType(),
				ByteArray.subByte(bytes, FLAGS_LENGTH + TYPE_LENGTH + attribute.getAttrLengthSize(), attrLength)));
		return attribute;
	}

	/**
	 * Choose corresponding parser to the typecode, that was already parsed.
	 * 
	 * @param type typecode of the path attribute
	 * @param bytes byte array to be parsed
	 * @return Object, because there are various Path Attributes and there is no superclass or interface common for all
	 *         of them.
	 * @throws BGPDocumentedException
	 * @throws BGPParsingException
	 */
	private static Object chooseParser(final TypeCode type, final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		switch (type) {
		case ORIGIN:
			return parseOrigin(bytes);
		case AS_PATH:
			return parseAsPath(bytes);
		case NEXT_HOP:
			return parseNextHop(bytes);
		case MULTI_EXIT_DISC:
			return parseMultiExitDisc(bytes);
		case LOCAL_PREF:
			return parseLocalPref(bytes);
		case ATOMIC_AGGREGATE:
			return null;
		case AGGREGATOR:
			return parseAggregator(bytes);
		case COMMUNITIES:
			return parseCommunities(bytes);
		case ORIGINATOR_ID:
			return parseOriginatorId(bytes);
		case CLUSTER_LIST:
			return parseClusterList(bytes);
		case MP_REACH_NLRI:
			return parseMPReach(bytes);
		case MP_UNREACH_NLRI:
			return parseMPUnreach(bytes);
		case EXTENDED_COMMUNITIES:
			return parseExtendedCommunities(bytes);
		case LINK_STATE:
			return parseLinkState(bytes);
			/**
			 * Recognize, but ignore.
			 */
		case AS4_AGGREGATOR:
			/**
			 * Recognize, but ignore.
			 */
		case AS4_PATH:
			return null;
		}
		return null;
	}

	/**
	 * Parses ORIGIN from bytes.
	 * 
	 * @param bytes byte array to be parsed
	 * @return BGPOrigin enum
	 * @throws BGPParsingException if the Origin value is unknown
	 * @throws BGPDocumentedException
	 */
	private static BgpOrigin parseOrigin(final byte[] bytes) throws BGPDocumentedException {
		final BgpOrigin origin = BgpOrigin.forValue(UnsignedBytes.toInt(bytes[0]));
		if (origin == null)
			throw new BGPDocumentedException("Unknown Origin type.", BGPError.ORIGIN_ATTR_NOT_VALID, new byte[] { (byte) 0x01, (byte) 0x01,
					bytes[0] });
		return origin;
	}

	/**
	 * Parses AS_PATH from bytes.
	 * 
	 * @param bytes byte array to be parsed
	 * @return new ASPath object
	 * @throws BGPDocumentedException if there is no AS_SEQUENCE present (mandatory)
	 * @throws BGPParsingException
	 */
	private static ASPath parseAsPath(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		int byteOffset = 0;
		List<AsNumber> list = null;
		Set<AsNumber> set = null;
		while (byteOffset < bytes.length) {
			final int type = UnsignedBytes.toInt(bytes[byteOffset]);
			final SegmentType segmentType = AsPathSegmentParser.parseType(type);
			if (segmentType == null)
				throw new BGPParsingException("AS Path segment type unknown : " + type);
			byteOffset += AsPathSegmentParser.TYPE_LENGTH;

			final int count = UnsignedBytes.toInt(bytes[byteOffset]);
			byteOffset += AsPathSegmentParser.LENGTH_SIZE;

			if (segmentType == SegmentType.AS_SEQUENCE) {
				list = (List<AsNumber>) AsPathSegmentParser.parseAsPathSegment(segmentType, count,
						ByteArray.subByte(bytes, byteOffset, count * AsPathSegmentParser.AS_NUMBER_LENGTH));
			} else {
				set = (Set<AsNumber>) AsPathSegmentParser.parseAsPathSegment(segmentType, count,
						ByteArray.subByte(bytes, byteOffset, count * AsPathSegmentParser.AS_NUMBER_LENGTH));
			}
			byteOffset += count * AsPathSegmentParser.AS_NUMBER_LENGTH;
		}

		if (list == null && bytes.length != 0)
			throw new BGPDocumentedException("AS_SEQUENCE must be present in AS_PATH attribute.", BGPError.AS_PATH_MALFORMED);
		return (set != null) ? new ASPath(list, set) : (list == null) ? ASPath.EMPTY : new ASPath(list);
	}

	/**
	 * Parse NEXT_HOP from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return new NextHop object, it's always IPv4 (basic BGP-4)
	 */
	private static NextHop<IPv4Address> parseNextHop(final byte[] bytes) {
		return new IPv4NextHop(new IPv4Address(bytes));
	}

	/**
	 * Parse MULTI_EXIT_DISC (integer) from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return integer representing MULTI_EXIT_DISC path attribute
	 */
	private static int parseMultiExitDisc(final byte[] bytes) {
		return ByteArray.bytesToInt(bytes);
	}

	/**
	 * Parse LOCAL_PREF (integer) from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return integer representing LOCAL_PREF path attribute
	 */
	private static int parseLocalPref(final byte[] bytes) {
		return ByteArray.bytesToInt(bytes);
	}

	/**
	 * Parse AGGREGATOR from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return new BGPAggregator object
	 */
	private static BgpAggregator parseAggregator(final byte[] bytes) {
		final AsNumber asNumber = new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(bytes, 0, AsPathSegmentParser.AS_NUMBER_LENGTH)));
		final Ipv4Address address = new Ipv4Address(IPv4.FAMILY.addressForBytes(
				ByteArray.subByte(bytes, AsPathSegmentParser.AS_NUMBER_LENGTH, 4)).toString());
		return new AggregatorBuilder().setAsNumber(asNumber).setNetworkAddress(address).build();
	}

	/**
	 * Parse MP_REACH_NLRI from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return new specific MPReach object with reachable flag set to true
	 * @throws BGPDocumentedException
	 */
	private static MPReach<?> parseMPReach(final byte[] bytes) throws BGPDocumentedException {
		try {
			return MPReachParser.parseMPReach(bytes);
		} catch (final BGPParsingException e) {
			throw new BGPDocumentedException("Could not parse MP_REACH_NLRI: " + e.getMessage(), BGPError.OPT_ATTR_ERROR);
		}
	}

	/**
	 * Parse MP_UNREACH_NLRI from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return new specific MPReach object with reachable flag set to false
	 * @throws BGPDocumentedException
	 */
	private static MPReach<?> parseMPUnreach(final byte[] bytes) throws BGPDocumentedException {
		try {
			return MPReachParser.parseMPUnreach(bytes);
		} catch (final BGPParsingException e) {
			throw new BGPDocumentedException("Could not parse MP_UNREACH_NLRI: " + e.getMessage(), BGPError.OPT_ATTR_ERROR);
		}
	}

	/**
	 * Parse set of EXTENDED_COMMUNITIES from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return new specific Extended Community object
	 * @throws BGPDocumentedException l
	 */
	private static Set<ExtendedCommunity> parseExtendedCommunities(final byte[] bytes) throws BGPDocumentedException {
		final Set<ExtendedCommunity> set = Sets.newHashSet();
		int i = 0;
		while (i < bytes.length) {
			set.add(CommunitiesParser.parseExtendedCommunity(ByteArray.subByte(bytes, i, CommunitiesParser.EXTENDED_COMMUNITY_LENGTH)));
			i += CommunitiesParser.EXTENDED_COMMUNITY_LENGTH;
		}
		return set;
	}

	/**
	 * Parse set of COMMUNITIES from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return new specific Community object
	 * @throws BGPDocumentedException
	 */
	private static Set<Community> parseCommunities(final byte[] bytes) throws BGPDocumentedException {
		final Set<Community> set = Sets.newHashSet();
		int i = 0;
		while (i < bytes.length) {
			set.add(CommunitiesParser.parseCommunity(ByteArray.subByte(bytes, i, CommunitiesParser.COMMUNITY_LENGTH)));
			i += CommunitiesParser.COMMUNITY_LENGTH;
		}
		return set;
	}

	/**
	 * Parse list of Cluster Identifiers.
	 * 
	 * @param bytes byte array to be parsed
	 * @return new List of Cluster Identifiers
	 */
	private static List<ClusterIdentifier> parseClusterList(final byte[] bytes) {
		final List<ClusterIdentifier> list = Lists.newArrayList();
		int i = 0;
		while (i < bytes.length) {
			list.add(new ClusterIdentifier(ByteArray.subByte(bytes, i, 4)));
			i += 4;
		}
		return list;
	}

	/**
	 * Parses ORIGINATOR_ID, which is BGP Identifier, which is IP address of the speaker.
	 * 
	 * @param bytes byte array to be parsed
	 * @return IP address of the speaker
	 */
	private static NetworkAddress<?> parseOriginatorId(final byte[] bytes) {
		if (bytes.length != 4)
			throw new IllegalArgumentException("Length of byte array for ORIGINATOR_ID should be 4, but is " + bytes.length);
		return new IPv4Address(bytes);
	}

	/**
	 * Parse LINK_STATE from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return Map, where the key is the type of a tlv and the value is the value of the tlv
	 */
	private static Map<Integer, ByteList> parseLinkState(final byte[] bytes) {
		return LinkStateParser.parseLinkState(bytes);
	}
}
