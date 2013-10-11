/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathSegmentParser.SegmentType;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.CAListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.CASetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.c.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.c.ipv4.next.hop.Ipv4NextHopBuilder;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

/**
 * 
 * Parser for different Path Attributes. Each attributes has its own method for parsing.
 * 
 */
public class PathAttributeParser {

	private static final int FLAGS_LENGTH = 1;

	private static final int TYPE_LENGTH = 1;

	private static AttributeRegistry reg = SimpleAttributeRegistry.INSTANCE;

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
	public static PathAttributes parseAttribute(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		if (bytes == null || bytes.length == 0) {
			throw new BGPParsingException("Insufficient length of byte array: " + bytes.length);
		}
		int byteOffset = 0;
		final PathAttributesBuilder builder = new PathAttributesBuilder();
		while (byteOffset < bytes.length) {
			final boolean[] bits = ByteArray.parseBits(bytes[0]);
			final boolean optional = bits[0];
			final int attrLength = (bits[3]) ? ByteArray.bytesToInt(ByteArray.subByte(bytes, 2, 2)) : UnsignedBytes.toInt(bytes[2]);
			final int hdrLength = FLAGS_LENGTH + TYPE_LENGTH + ((bits[3]) ? 2 : 1);

			final byte[] attrBody = ByteArray.subByte(bytes, hdrLength, attrLength);

			boolean found = reg.parseAttribute(UnsignedBytes.toInt(bytes[1]), attrBody, builder);
			if (!optional && !found) {
				throw new BGPDocumentedException("Well known attribute not recognized.", BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED);
			}

			byteOffset += hdrLength + attrLength;
		}
		return builder.build();
	}

	/**
	 * Parses ORIGIN from bytes.
	 * 
	 * @param bytes byte array to be parsed
	 * @return {@link Origin} BGP origin value
	 * @throws BGPDocumentedException
	 */
	static Origin parseOrigin(final byte[] bytes) throws BGPDocumentedException {
		final BgpOrigin borigin = BgpOrigin.forValue(UnsignedBytes.toInt(bytes[0]));
		if (borigin == null) {
			throw new BGPDocumentedException("Unknown Origin type.", BGPError.ORIGIN_ATTR_NOT_VALID, new byte[] { (byte) 0x01, (byte) 0x01,
					bytes[0] });
		}
		return new OriginBuilder().setValue(borigin).build();
	}

	/**
	 * Parses AS_PATH from bytes.
	 * 
	 * @param bytes byte array to be parsed
	 * @return new ASPath object
	 * @throws BGPDocumentedException if there is no AS_SEQUENCE present (mandatory)
	 * @throws BGPParsingException
	 */
	static AsPath parseAsPath(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		int byteOffset = 0;
		final List<Segments> ases = Lists.newArrayList();
		boolean isSequence = false;
		while (byteOffset < bytes.length) {
			final int type = UnsignedBytes.toInt(bytes[byteOffset]);
			final SegmentType segmentType = AsPathSegmentParser.parseType(type);
			if (segmentType == null) {
				throw new BGPParsingException("AS Path segment type unknown : " + type);
			}
			byteOffset += AsPathSegmentParser.TYPE_LENGTH;

			final int count = UnsignedBytes.toInt(bytes[byteOffset]);
			byteOffset += AsPathSegmentParser.LENGTH_SIZE;

			if (segmentType == SegmentType.AS_SEQUENCE) {
				final List<AsSequence> numbers = AsPathSegmentParser.parseAsSequence(count,
						ByteArray.subByte(bytes, byteOffset, count * AsPathSegmentParser.AS_NUMBER_LENGTH));
				ases.add(new SegmentsBuilder().setCSegment(new CAListBuilder().setAsSequence(numbers).build()).build());
				isSequence = true;
			} else {
				final List<AsNumber> list = AsPathSegmentParser.parseAsSet(count,
						ByteArray.subByte(bytes, byteOffset, count * AsPathSegmentParser.AS_NUMBER_LENGTH));
				ases.add(new SegmentsBuilder().setCSegment(new CASetBuilder().setAsSet(list).build()).build());

			}
			byteOffset += count * AsPathSegmentParser.AS_NUMBER_LENGTH;
		}

		if (!isSequence && bytes.length != 0) {
			throw new BGPDocumentedException("AS_SEQUENCE must be present in AS_PATH attribute.", BGPError.AS_PATH_MALFORMED);
		}
		return new AsPathBuilder().setSegments(ases).build();
	}

	/**
	 * Parse NEXT_HOP from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return new NextHop object, it's always IPv4 (basic BGP-4)
	 */
	static CNextHop parseNextHop(final byte[] bytes) {
		return new CIpv4NextHopBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(Ipv4Util.addressForBytes(bytes)).build()).build();
	}

	/**
	 * Parse MULTI_EXIT_DISC (integer) from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return integer representing MULTI_EXIT_DISC path attribute
	 */
	static MultiExitDisc parseMultiExitDisc(final byte[] bytes) {
		return new MultiExitDiscBuilder().setMed(ByteArray.bytesToLong(bytes)).build();
	}

	/**
	 * Parse LOCAL_PREF (integer) from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return integer representing LOCAL_PREF path attribute
	 */
	static LocalPref parseLocalPref(final byte[] bytes) {
		return new LocalPrefBuilder().setPref(ByteArray.bytesToLong(bytes)).build();
	}

	/**
	 * Parse AGGREGATOR from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return {@link Aggregator} BGP Aggregator
	 */
	static Aggregator parseAggregator(final byte[] bytes) {
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
	static void parseMPReach(final PathAttributesBuilder b, final byte[] bytes) throws BGPDocumentedException {

		try {
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1 a = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1Builder().setMpReachNlri(
					MPReachParser.parseMPReach(bytes)).build();

			b.addAugmentation(
					org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1.class, a);
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
	static void parseMPUnreach(final PathAttributesBuilder b, final byte[] bytes) throws BGPDocumentedException {
		try {
			final PathAttributes2 a = new PathAttributes2Builder().setMpUnreachNlri(MPReachParser.parseMPUnreach(bytes)).build();

			b.addAugmentation(PathAttributes2.class, a);
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
	static List<ExtendedCommunities> parseExtendedCommunities(final byte[] bytes) throws BGPDocumentedException {
		final List<ExtendedCommunities> set = Lists.newArrayList();
		int i = 0;
		while (i < bytes.length) {
			set.add((ExtendedCommunities) CommunitiesParser.parseExtendedCommunity(ByteArray.subByte(bytes, i,
					CommunitiesParser.EXTENDED_COMMUNITY_LENGTH)));
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
	static List<Communities> parseCommunities(final byte[] bytes) throws BGPDocumentedException {
		final List<Communities> set = Lists.newArrayList();
		int i = 0;
		while (i < bytes.length) {
			set.add((Communities) CommunitiesParser.parseCommunity(ByteArray.subByte(bytes, i, CommunitiesParser.COMMUNITY_LENGTH)));
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
	static List<ClusterIdentifier> parseClusterList(final byte[] bytes) {
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
	static byte[] parseOriginatorId(final byte[] bytes) {
		if (bytes.length != 4) {
			throw new IllegalArgumentException("Length of byte array for ORIGINATOR_ID should be 4, but is " + bytes.length);
		}
		return bytes;
	}

	/**
	 * Parse LINK_STATE from bytes
	 * 
	 * @param bytes byte array to be parsed
	 * @return Map, where the key is the type of a tlv and the value is the value of the tlv
	 * @throws BGPParsingException
	 */
	static void parseLinkState(final PathAttributesBuilder builder, final byte[] bytes) throws BGPParsingException {
		final PathAttributes1 a = new PathAttributes1Builder().setLinkstatePathAttribute(LinkStateParser.parseLinkState(bytes)).build();
		builder.addAugmentation(PathAttributes1.class, a);
	}
}
