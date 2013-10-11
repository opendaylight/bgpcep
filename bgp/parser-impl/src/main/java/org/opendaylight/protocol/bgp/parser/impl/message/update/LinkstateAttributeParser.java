/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.ByteList;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.AdministrativeGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.ExtendedRouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.IgpBits.UpDown;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.Ipv6RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.MplsProtocolMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.NodeFlagBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.RouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.SharedRiskLinkGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.isis.lan.identifier.IsIsRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.link.state.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.PrefixDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.c.linkstate.destination.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.CIsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.CIsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.COspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.COspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.c.isis.node.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.c.isis.pseudonode.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.c.ospf.node.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.c.ospf.pseudonode.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.prefix.state.IgpBitsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.LinkstatePathAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.LinkstatePathAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.IgpMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.IsoSystemIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.TeMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for Link State information.
 * 
 * @see <a href="http://tools.ietf.org/html/draft-gredler-idr-ls-distribution-01">BGP-LS draft</a>
 */
public class LinkstateAttributeParser implements AttributeParser {
	// FIXME: update to IANA number once it is known
	static final int TYPE = 99;

	private static final Logger logger = LoggerFactory.getLogger(LinkstateAttributeParser.class);

	private static final int TYPE_LENGTH = 2;

	private static final int LENGTH_SIZE = 2;

	private static final int ROUTE_DISTINGUISHER_LENGTH = 8;

	private static final int PROTOCOL_ID_LENGTH = 1;

	private static final int IDENTIFIER_LENGTH = 8;

	private static final Set<Integer> nodeTlvs = Sets.newHashSet(263, 1024, 1025, 1026, 1027, 1028, 1029);

	private static final Set<Integer> linkTlvs = Sets.newHashSet(1028, 1029, 1030, 1031, 1088, 1089, 1090, 1091, 1092, 1093, 1094, 1095,
			1096, 1097, 1098);

	private static final Set<Integer> prefixTlvs = Sets.newHashSet(1152, 1153, 1154, 1155, 1156, 1157);

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPParsingException {
		final PathAttributes1 a = new PathAttributes1Builder().setLinkstatePathAttribute(parseLinkState(bytes)).build();
		builder.addAugmentation(PathAttributes1.class, a);
	}

	/**
	 * Parses common parts for Link State Nodes, Links and Prefixes, that includes protocol ID and identifier tlv.
	 * 
	 * @param reachable
	 * @param safi
	 * @param bytes
	 * @return BGPLinkMP or BGPNodeMP
	 * @throws BGPParsingException
	 */
	protected static CLinkstateDestination parseLSNlri(final Class<? extends SubsequentAddressFamily> safi, final byte[] bytes)
			throws BGPParsingException {
		if (bytes.length == 0) {
			return null;
		}
		int byteOffset = 0;

		final CLinkstateDestinationBuilder builder = new CLinkstateDestinationBuilder();

		while (byteOffset != bytes.length) {
			final NlriType type = NlriType.forValue(ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH)));
			builder.setNlriType(type);

			byteOffset += TYPE_LENGTH;
			// length means total length of the tlvs including route distinguisher not including the type field
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			RouteDistinguisher distinguisher = null;
			if (safi == MplsLabeledVpnSubsequentAddressFamily.class) {
				// this parses route distinguisher
				distinguisher = new RouteDistinguisher(BigInteger.valueOf(ByteArray.bytesToLong(ByteArray.subByte(bytes, byteOffset,
						ROUTE_DISTINGUISHER_LENGTH))));
				builder.setDistinguisher(distinguisher);
				byteOffset += ROUTE_DISTINGUISHER_LENGTH;
			}
			// parse source protocol
			final ProtocolId sp = ProtocolId.forValue(ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, PROTOCOL_ID_LENGTH)));
			byteOffset += PROTOCOL_ID_LENGTH;
			builder.setProtocolId(sp);

			// parse identifier
			final Identifier identifier = new Identifier(BigInteger.valueOf(ByteArray.bytesToLong(ByteArray.subByte(bytes, byteOffset,
					IDENTIFIER_LENGTH))));
			byteOffset += IDENTIFIER_LENGTH;
			builder.setIdentifier(identifier);

			// if we are dealing with linkstate nodes/links, parse local node descriptor
			NodeIdentifier localDescriptor = null;
			int locallength = 0;
			final int localtype = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			locallength = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			if (localtype == 256) {
				localDescriptor = parseNodeDescriptors(ByteArray.subByte(bytes, byteOffset, locallength));
			}
			byteOffset += locallength;
			builder.setLocalNodeDescriptors((LocalNodeDescriptors) localDescriptor);
			final int restLength = length - ((safi == MplsLabeledVpnSubsequentAddressFamily.class) ? ROUTE_DISTINGUISHER_LENGTH : 0)
					- PROTOCOL_ID_LENGTH - IDENTIFIER_LENGTH - TYPE_LENGTH - LENGTH_SIZE - locallength;
			logger.debug("Restlength {}", restLength);
			switch (type) {
			case Link:
				parseLink(builder, ByteArray.subByte(bytes, byteOffset, restLength));
				break;
			case Ipv4Prefix:
			case Ipv6Prefix:
				builder.setPrefixDescriptors(parsePrefixDescriptors(localDescriptor, ByteArray.subByte(bytes, byteOffset, restLength)));
				break;
			case Node:
				// node nlri is already parsed as it contains only the common fields for node and link nlri
				break;
			}
			byteOffset += restLength;
		}
		return builder.build();
	}

	public static boolean verifyLink(final Set<Integer> keys) {
		for (final Integer i : keys) {
			if (!linkTlvs.contains(i)) {
				logger.warn("Invalid link attribute {}", i);
				return false;
			}
		}
		return true;
	}

	public static boolean verifyNode(final Set<Integer> keys) {
		for (final Integer i : keys) {
			if (!nodeTlvs.contains(i)) {
				logger.warn("Invalid node attribute {}", i);
				return false;
			}
		}
		return true;
	}

	public static boolean verifyPrefix(final Set<Integer> keys) {
		for (final Integer i : keys) {
			if (!prefixTlvs.contains(i)) {
				logger.warn("Invalid prefix attribute {}", i);
				return false;
			}
		}
		return true;
	}

	private static NodeIdentifier parseLink(final CLinkstateDestinationBuilder builder, final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
		byteOffset += TYPE_LENGTH;
		final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
		byteOffset += LENGTH_SIZE;
		final NodeIdentifier remote = null;
		if (type == 257) {
			builder.setRemoteNodeDescriptors((RemoteNodeDescriptors) parseNodeDescriptors(ByteArray.subByte(bytes, byteOffset, length)));
			byteOffset += length;
		}
		builder.setLinkDescriptors(parseLinkDescriptors(ByteArray.subByte(bytes, byteOffset, bytes.length - byteOffset)));
		return remote;
	}

	private static LinkDescriptors parseLinkDescriptors(final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		final LinkDescriptorsBuilder builder = new LinkDescriptorsBuilder();
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			logger.debug("Parsing Link Descriptor: {}", Arrays.toString(value));
			switch (type) {
			case 258:
				builder.setLinkLocalIdentifier(ByteArray.subByte(value, 0, 4));
				builder.setLinkRemoteIdentifier(ByteArray.subByte(value, 4, 4));
				logger.trace("Parsed link local {} remote {} Identifiers.", builder.getLinkLocalIdentifier(),
						builder.getLinkRemoteIdentifier());
				break;
			case 259:
				final Ipv4InterfaceIdentifier lipv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForBytes(value));
				builder.setIpv4InterfaceAddress(lipv4);
				logger.trace("Parsed IPv4 interface address {}.", lipv4);
				break;
			case 260:
				final Ipv4InterfaceIdentifier ripv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForBytes(value));
				builder.setIpv4NeighborAddress(ripv4);
				logger.trace("Parsed IPv4 neighbor address {}.", ripv4);
				break;
			case 261:
				final Ipv6InterfaceIdentifier lipv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForBytes(value));
				builder.setIpv6InterfaceAddress(lipv6);
				logger.trace("Parsed IPv6 interface address {}.", lipv6);
				break;
			case 262:
				final Ipv6InterfaceIdentifier ripv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForBytes(value));
				builder.setIpv6NeighborAddress(ripv6);
				logger.trace("Parsed IPv6 neighbor address {}.", ripv6);
				break;
			case 263:
				final TopologyIdentifier topId = new TopologyIdentifier(ByteArray.bytesToInt(value) & 0x3fff);
				builder.setMultiTopologyId(topId);
				logger.trace("Parsed topology identifier {}.", topId);
				break;
			default:
				throw new BGPParsingException("Link Descriptor not recognized, type: " + type);
			}
			byteOffset += length;
		}
		logger.debug("Finished parsing Link descriptors.");
		return builder.build();
	}

	private static NodeIdentifier parseNodeDescriptors(final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		AsNumber asnumber = null;
		DomainIdentifier bgpId = null;
		AreaIdentifier ai = null;
		CRouterIdentifier routerId = null;
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			logger.debug("Parsing Node Descriptor: {}", Arrays.toString(value));
			switch (type) {
			case 512:
				asnumber = new AsNumber(ByteArray.bytesToLong(value));
				logger.trace("Parsed AS number {}", asnumber);
				break;
			case 513:
				bgpId = new DomainIdentifier(value);
				logger.trace("Parsed bgpId {}", bgpId);
				break;
			case 514:
				ai = new AreaIdentifier(value);
				logger.trace("Parsed area identifier {}", ai);
				break;
			case 515:
				if (value.length == 6) {
					routerId = new CIsisNodeBuilder().setIsisNode(
							new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(ByteArray.subByte(value, 0, 6))).build()).build();
				} else if (value.length == 7) {
					if (value[6] == 0) {
						logger.warn("PSN octet is 0. Ignoring System ID.");
						routerId = new CIsisNodeBuilder().setIsisNode(
								new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(ByteArray.subByte(value, 0, 6))).build()).build();
						break;
					} else {
						final IsIsRouterIdentifier iri = new IsIsRouterIdentifierBuilder().setIsoSystemId(
								new IsoSystemIdentifier(ByteArray.subByte(value, 0, 6))).build();
						routerId = new CIsisPseudonodeBuilder().setIsisPseudonode(
								new IsisPseudonodeBuilder().setIsIsRouterIdentifier(iri).setPsn((short) UnsignedBytes.toInt(value[6])).build()).build();
					}
				} else if (value.length == 4) {
					routerId = new COspfNodeBuilder().setOspfNode(
							new OspfNodeBuilder().setOspfRouterId(ByteArray.subByte(value, 0, 4)).build()).build();
				} else if (value.length == 8) {
					final byte[] o = ByteArray.subByte(value, 0, 4); // FIXME: OSPFv3 vs OSPFv2
					final OspfInterfaceIdentifier a = new OspfInterfaceIdentifier(ByteArray.subByte(value, 4, 4));
					routerId = new COspfPseudonodeBuilder().setOspfPseudonode(
							new OspfPseudonodeBuilder().setOspfRouterId(o).setLanInterface(a).build()).build();
				}
				logger.trace("Parsed Router Identifier {}", routerId);
				break;
			default:
				throw new BGPParsingException("Node Descriptor not recognized, type: " + type);
			}
			byteOffset += length;
		}
		logger.debug("Finished parsing Node descriptors.");
		return new LocalNodeDescriptorsBuilder().setAsNumber(asnumber).setDomainId(bgpId).setAreaId(ai).setCRouterIdentifier(routerId).build();
	}

	private static PrefixDescriptors parsePrefixDescriptors(final NodeIdentifier localDescriptor, final byte[] bytes)
			throws BGPParsingException {
		int byteOffset = 0;
		final PrefixDescriptorsBuilder builder = new PrefixDescriptorsBuilder();
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			logger.trace("Parsing Prefix Descriptor: {}", Arrays.toString(value));
			switch (type) {
			case 263:
				final TopologyIdentifier topologyId = new TopologyIdentifier(ByteArray.bytesToInt(value) & 0x3fff);
				builder.setMultiTopologyId(topologyId);
				logger.trace("Parsed Topology Identifier: {}", topologyId);
				break;
			case 264:
				final int rt = ByteArray.bytesToInt(value);
				final OspfRouteType routeType = OspfRouteType.forValue(rt);
				if (routeType == null) {
					throw new BGPParsingException("Unknown OSPF Route Type: " + rt);
				}
				builder.setOspfRouteType(routeType);
				logger.trace("Parser RouteType: {}", routeType);
				break;
			case 265:
				IpPrefix prefix = null;
				final int prefixLength = UnsignedBytes.toInt(value[0]);
				final int size = prefixLength / 8 + ((prefixLength % 8 == 0) ? 0 : 1);
				if (size != value.length - 1) {
					logger.debug("Expected length {}, actual length {}.", size, value.length - 1);
					throw new BGPParsingException("Illegal length of IP reachability TLV: " + (value.length - 1));
				}
				if (size == 4) {
					prefix = new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.subByte(value, 1, size), prefixLength));
				} else {
					prefix = new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.subByte(value, 1, size), prefixLength));
				}
				builder.setIpReachabilityInformation(prefix);
				logger.trace("Parsed IP reachability info: {}", prefix);
				break;
			default:
				throw new BGPParsingException("Prefix Descriptor not recognized, type: " + type);
			}
			byteOffset += length;
		}
		logger.debug("Finished parsing Prefix descriptors.");
		return builder.build();
	}

	protected static LinkstatePathAttribute parseLinkState(final byte[] bytes) throws BGPParsingException {
		final Map<Integer, ByteList> map = new HashMap<Integer, ByteList>();
		int byteOffset = 0;
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			ByteList values = map.containsKey(type) ? values = map.get(type) : new ByteList();
			values.add(value);
			map.put(type, values);
			byteOffset += length;
		}
		final LinkstatePathAttributeBuilder builder = new LinkstatePathAttributeBuilder();
		if (verifyLink(map.keySet())) {
			builder.setLinkStateAttribute(parseLinkAttributes(map));
		} else if (verifyNode(map.keySet())) {
			builder.setLinkStateAttribute(parseNodeAttributes(map));
		} else if (verifyPrefix(map.keySet())) {
			builder.setLinkStateAttribute(parsePrefixAttributes(map));
		}
		return builder.build();
	}

	/**
	 * Parse Link Attributes.
	 * 
	 * @param attributes key is the tlv type and value is the value of the tlv
	 * @return {@link LinkAttributes}
	 * @throws BGPParsingException if a link attribute is not recognized
	 */
	public static LinkAttributes parseLinkAttributes(final Map<Integer, ByteList> attributes) throws BGPParsingException {

		final LinkAttributesBuilder builder = new LinkAttributesBuilder();
		for (final Entry<Integer, ByteList> entry : attributes.entrySet()) {
			logger.debug("Link attribute TLV {}", entry.getKey());

			for (final byte[] value : entry.getValue().getBytes()) {

				switch (entry.getKey()) {
				case 1028:
					final Ipv4RouterIdentifier lipv4 = new Ipv4RouterIdentifier(Ipv4Util.addressForBytes(value));
					builder.setLocalIpv4RouterId(lipv4);
					logger.trace("Parsed IPv4 Router-ID of local node: {}", lipv4);
					break;
				case 1029:
					final Ipv6RouterIdentifier lipv6 = new Ipv6RouterIdentifier(Ipv6Util.addressForBytes(value));
					builder.setLocalIpv6RouterId(lipv6);
					logger.trace("Parsed IPv6 Router-ID of local node: {}", lipv6);
					break;
				case 1030:
					final Ipv4RouterIdentifier ripv4 = new Ipv4RouterIdentifier(Ipv4Util.addressForBytes(value));
					builder.setRemoteIpv4RouterId(ripv4);
					logger.trace("Parsed IPv4 Router-ID of remote node: {}", ripv4);
					break;
				case 1031:
					final Ipv6RouterIdentifier ripv6 = new Ipv6RouterIdentifier(Ipv6Util.addressForBytes(value));
					builder.setRemoteIpv6RouterId(ripv6);
					logger.trace("Parsed IPv6 Router-ID of remote node: {}", ripv6);
					break;
				case 1088:
					builder.setAdminGroup(new AdministrativeGroup(ByteArray.bytesToLong(value)));
					logger.trace("Parsed Administrative Group {}", builder.getAdminGroup());
					break;
				case 1089:
					builder.setMaxLinkBandwidth(new Bandwidth(value));
					logger.trace("Parsed Max Bandwidth {}", builder.getMaxLinkBandwidth());
					break;
				case 1090:
					builder.setMaxReservableBandwidth(new Bandwidth(value));
					logger.trace("Parsed Max Reservable Bandwidth {}", builder.getMaxReservableBandwidth());
					break;
				case 1091:
					int index = 0;
					final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.link.state.UnreservedBandwidth> unreservedBandwidth = Lists.newArrayList();
					for (int i = 0; i < 8; i++) {
						unreservedBandwidth.add(new UnreservedBandwidthBuilder().setBandwidth(
								new Bandwidth(ByteArray.subByte(value, index, 4))).setPriority((short) i).build());
						index += 4;
					}
					builder.setUnreservedBandwidth(unreservedBandwidth);
					logger.trace("Parsed Unreserved Bandwidth {}", builder.getUnreservedBandwidth());
					break;
				case 1092:
					builder.setTeMetric(new TeMetric(ByteArray.bytesToLong(value)));
					logger.trace("Parsed Metric {}", builder.getTeMetric());
					break;
				case 1093:
					final LinkProtectionType lpt = LinkProtectionType.forValue(UnsignedBytes.toInt(value[0]));
					if (lpt == null) {
						throw new BGPParsingException("Link Protection Type not recognized: " + UnsignedBytes.toInt(value[0]));
					}
					builder.setLinkProtection(lpt);
					logger.trace("Parsed Link Protection Type {}", lpt);
					break;
				case 1094:
					final boolean[] bits = ByteArray.parseBits(value[0]);
					builder.setMplsProtocol(new MplsProtocolMask(bits[0], bits[1]));
					logger.trace("Parsed MPLS Protocols: {}", builder.getMplsProtocol());
					break;
				case 1095:
					builder.setMetric(new Metric(ByteArray.bytesToLong(value)));
					logger.trace("Parsed Metric {}", builder.getMetric());
					break;
				case 1096:
					int i = 0;
					final List<SharedRiskLinkGroup> sharedRiskLinkGroups = Lists.newArrayList();
					while (i != value.length) {
						sharedRiskLinkGroups.add(new SharedRiskLinkGroup(ByteArray.bytesToLong(ByteArray.subByte(value, i, 4))));
						i += 4;
					}
					builder.setSharedRiskLinkGroups(sharedRiskLinkGroups);
					logger.trace("Parsed Shared Risk Link Groups {}", Arrays.toString(sharedRiskLinkGroups.toArray()));
					break;
				case 1097:
					final byte[] opaque = value;
					logger.trace("Parsed Opaque value : {}", Arrays.toString(opaque));
					break;
				case 1098:
					final String name = new String(value, Charsets.US_ASCII);
					builder.setLinkName(name);
					logger.trace("Parsed Link Name : ", name);
					break;
				default:
					throw new BGPParsingException("Link Attribute not recognized, type: " + entry.getKey());
				}
			}
		}
		logger.debug("Finished parsing Link Attributes.");
		return builder.build();
	}

	/**
	 * Parse Node Attributes.
	 * 
	 * @param attributes key is the tlv type and value is the value of the tlv
	 * @return {@link NodeAttributes}
	 * @throws BGPParsingException if a node attribute is not recognized
	 */
	public static NodeAttributes parseNodeAttributes(final Map<Integer, ByteList> attributes) throws BGPParsingException {
		final List<TopologyIdentifier> topologyMembership = Lists.newArrayList();
		final List<IsisAreaIdentifier> areaMembership = Lists.newArrayList();
		final NodeAttributesBuilder builder = new NodeAttributesBuilder();
		for (final Entry<Integer, ByteList> entry : attributes.entrySet()) {
			logger.debug("Node attribute TLV {}", entry.getKey());
			for (final byte[] value : entry.getValue().getBytes()) {
				switch (entry.getKey()) {
				case 263:
					int i = 0;
					while (i != value.length) {
						final TopologyIdentifier topId = new TopologyIdentifier(ByteArray.bytesToInt(ByteArray.subByte(value, i, 2)) & 0x3fff);
						topologyMembership.add(topId);
						logger.trace("Parsed Topology Identifier: {}", topId);
						i += 2;
					}
					break;
				case 1024:
					final boolean[] flags = ByteArray.parseBits(value[0]);
					builder.setNodeFlags(new NodeFlagBits(flags[0], flags[1], flags[2], flags[3]));
					logger.trace("Parsed External bit {}, area border router {}.", flags[2], flags[3]);
					break;
				case 1025:
					logger.debug("Ignoring opaque value: {}.", Arrays.toString(value));
					break;
				case 1026:
					builder.setDynamicHostname(new String(value, Charsets.US_ASCII));
					logger.trace("Parsed Node Name {}", builder.getDynamicHostname());
					break;
				case 1027:
					final IsisAreaIdentifier ai = new IsisAreaIdentifier(value);
					areaMembership.add(ai);
					logger.trace("Parsed AreaIdentifier {}", ai);
					break;
				case 1028:
					final Ipv4RouterIdentifier ip4 = new Ipv4RouterIdentifier(Ipv4Util.addressForBytes(value));
					builder.setIpv4RouterId(ip4);
					logger.trace("Parsed IPv4 Router Identifier {}", ip4);
					break;
				case 1029:
					final Ipv6RouterIdentifier ip6 = new Ipv6RouterIdentifier(Ipv6Util.addressForBytes(value));
					builder.setIpv6RouterId(ip6);
					logger.trace("Parsed IPv6 Router Identifier {}", ip6);
					break;
				default:
					throw new BGPParsingException("Node Attribute not recognized, type: " + entry.getKey());
				}
			}
		}
		builder.setTopologyIdentifier(topologyMembership);
		builder.setIsisAreaId(areaMembership);
		logger.debug("Finished parsing Node Attributes.");
		return builder.build();
	}

	/**
	 * Parse prefix attributes.
	 * 
	 * @param attributes key is the tlv type and value are the value bytes of the tlv
	 * @return {@link PrefixAttributes}
	 * @throws BGPParsingException if some prefix attributes is not recognized
	 */
	public static PrefixAttributes parsePrefixAttributes(final Map<Integer, ByteList> attributes) throws BGPParsingException {
		final PrefixAttributesBuilder builder = new PrefixAttributesBuilder();
		final List<RouteTag> routeTags = Lists.newArrayList();
		final List<ExtendedRouteTag> exRouteTags = Lists.newArrayList();
		for (final Entry<Integer, ByteList> entry : attributes.entrySet()) {
			logger.debug("Prefix attribute TLV {}", entry.getKey());
			for (final byte[] value : entry.getValue().getBytes()) {
				switch (entry.getKey()) {
				case 1152:
					final boolean[] flags = ByteArray.parseBits(value[0]);
					final boolean upDownBit = flags[2];
					builder.setIgpBits(new IgpBitsBuilder().setUpDown(new UpDown(upDownBit)).build());
					logger.trace("Parsed IGP flag (up/down bit) : {}", upDownBit);
					break;
				case 1153:
					int offset = 0;
					while (offset != value.length) {
						final RouteTag routeTag = new RouteTag(ByteArray.subByte(value, offset, 4));
						routeTags.add(routeTag);
						logger.trace("Parsed Route Tag: {}", routeTag);
						offset += 4;
					}
					break;
				case 1154:
					offset = 0;
					while (offset != value.length) {
						final ExtendedRouteTag exRouteTag = new ExtendedRouteTag(value);
						exRouteTags.add(exRouteTag);
						logger.trace("Parsed Extended Route Tag: {}", exRouteTag);
						offset += 4;
					}
					break;
				case 1155:
					final IgpMetric metric = new IgpMetric(ByteArray.bytesToLong(value));
					builder.setPrefixMetric(metric);
					logger.trace("Parsed Metric: {}", metric);
					break;
				case 1156:
					IpAddress fwdAddress = null;
					switch (value.length) {
					case 4:
						fwdAddress = new IpAddress(Ipv4Util.addressForBytes(value));
						break;
					case 16:
						fwdAddress = new IpAddress(Ipv6Util.addressForBytes(value));
						break;
					default:
						logger.debug("Ignoring unsupported forwarding address length {}", value.length);
					}
					logger.trace("Parsed FWD Address: {}", fwdAddress);
					break;
				case 1157:
					final byte[] opaque = value;
					logger.trace("Parsed Opaque value: {}", Arrays.toString(opaque));
					break;
				default:
					throw new BGPParsingException("Prefix Attribute not recognized, type: " + entry.getKey());
				}
			}
		}
		logger.debug("Finished parsing Prefix Attributes.");
		builder.setRouteTags(routeTags);
		builder.setExtendedTags(exRouteTags);
		return builder.build();
	}
}
