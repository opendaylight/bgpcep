/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.NextHop;
import org.opendaylight.protocol.bgp.linkstate.AdministrativeGroup;
import org.opendaylight.protocol.bgp.linkstate.AreaIdentifier;
import org.opendaylight.protocol.bgp.linkstate.DomainIdentifier;
import org.opendaylight.protocol.bgp.linkstate.ExtendedRouteTag;
import org.opendaylight.protocol.bgp.linkstate.IPv4InterfaceIdentifier;
import org.opendaylight.protocol.bgp.linkstate.IPv4PrefixIdentifier;
import org.opendaylight.protocol.bgp.linkstate.IPv4RouterIdentifier;
import org.opendaylight.protocol.bgp.linkstate.IPv6InterfaceIdentifier;
import org.opendaylight.protocol.bgp.linkstate.IPv6PrefixIdentifier;
import org.opendaylight.protocol.bgp.linkstate.IPv6RouterIdentifier;
import org.opendaylight.protocol.bgp.linkstate.ISISAreaIdentifier;
import org.opendaylight.protocol.bgp.linkstate.ISISLANIdentifier;
import org.opendaylight.protocol.bgp.linkstate.ISISNetworkPrefixState;
import org.opendaylight.protocol.bgp.linkstate.ISISRouterIdentifier;
import org.opendaylight.protocol.bgp.linkstate.InterfaceIdentifier;
import org.opendaylight.protocol.bgp.linkstate.LinkAnchor;
import org.opendaylight.protocol.bgp.linkstate.LinkIdentifier;
import org.opendaylight.protocol.bgp.linkstate.LinkProtectionType;
import org.opendaylight.protocol.bgp.linkstate.MPLSProtocol;
import org.opendaylight.protocol.bgp.linkstate.NetworkLinkImpl;
import org.opendaylight.protocol.bgp.linkstate.NetworkLinkState;
import org.opendaylight.protocol.bgp.linkstate.NetworkNodeImpl;
import org.opendaylight.protocol.bgp.linkstate.NetworkObjectState;
import org.opendaylight.protocol.bgp.linkstate.NetworkPrefixState;
import org.opendaylight.protocol.bgp.linkstate.NodeIdentifier;
import org.opendaylight.protocol.bgp.linkstate.OSPFInterfaceIdentifier;
import org.opendaylight.protocol.bgp.linkstate.OSPFNetworkPrefixState;
import org.opendaylight.protocol.bgp.linkstate.OSPFPrefixIdentifier;
import org.opendaylight.protocol.bgp.linkstate.OSPFRouteType;
import org.opendaylight.protocol.bgp.linkstate.OSPFRouterIdentifier;
import org.opendaylight.protocol.bgp.linkstate.OSPFv3LANIdentifier;
import org.opendaylight.protocol.bgp.linkstate.PrefixIdentifier;
import org.opendaylight.protocol.bgp.linkstate.RouteTag;
import org.opendaylight.protocol.bgp.linkstate.RouterIdentifier;
import org.opendaylight.protocol.bgp.linkstate.SourceProtocol;
import org.opendaylight.protocol.bgp.linkstate.TopologyIdentifier;
import org.opendaylight.protocol.bgp.linkstate.UnnumberedLinkIdentifier;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.BGPLinkMP;
import org.opendaylight.protocol.bgp.parser.impl.BGPNodeMP;
import org.opendaylight.protocol.bgp.parser.impl.ByteList;
import org.opendaylight.protocol.bgp.parser.impl.MPReach;
import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.Bandwidth;
import org.opendaylight.protocol.concepts.IGPMetric;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.IPv6Prefix;
import org.opendaylight.protocol.concepts.ISOSystemIdentifier;
import org.opendaylight.protocol.concepts.Metric;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.protocol.concepts.SharedRiskLinkGroup;
import org.opendaylight.protocol.concepts.TEMetric;
import org.opendaylight.protocol.util.ByteArray;
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
public class LinkStateParser {

	private static final Logger logger = LoggerFactory.getLogger(LinkStateParser.class);

	private static final int TYPE_LENGTH = 2;

	private static final int LENGTH_SIZE = 2;

	private static final int ROUTE_DISTINGUISHER_LENGTH = 8;

	private static final int PROTOCOL_ID_LENGTH = 1;

	private static final int IDENTIFIER_LENGTH = 8;

	private static final Set<Integer> nodeTlvs = Sets.newHashSet(263, 1024, 1025, 1026, 1027, 1028, 1029);

	private static final Set<Integer> linkTlvs = Sets.newHashSet(1028, 1029, 1030, 1031, 1088, 1089, 1090, 1091, 1092, 1093, 1094, 1095,
			1096, 1097, 1098);

	private static final Set<Integer> prefixTlvs = Sets.newHashSet(1152, 1153, 1154, 1155, 1156, 1157);

	private enum NlriType {
		LinkNLRI, NodeNLRI, IPv4Prefixes, IPv6Prefixes
	}

	private LinkStateParser() {
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
	protected static MPReach<?> parseLSNlri(final boolean reachable, final BGPSubsequentAddressFamily safi, final NextHop<?> nextHop,
			final byte[] bytes) throws BGPParsingException {
		if (bytes.length == 0)
			return null;
		int byteOffset = 0;
		final Set<LinkIdentifier> links = Sets.newHashSet();
		final Set<NodeIdentifier> nodes = Sets.newHashSet();
		final Set<PrefixIdentifier<?>> descs = Sets.newHashSet();

		long identifier = 0;
		SourceProtocol sp = null;

		while (byteOffset != bytes.length) {
			final NlriType type = parseNLRItype(ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH)));
			byteOffset += TYPE_LENGTH;
			// length means total length of the tlvs including route distinguisher not including the type field
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			if (safi == BGPSubsequentAddressFamily.MPLSLabeledVPN) {
				// this parses route distinguisher
				ByteArray.bytesToLong(ByteArray.subByte(bytes, byteOffset, ROUTE_DISTINGUISHER_LENGTH));
				byteOffset += ROUTE_DISTINGUISHER_LENGTH;
			}
			// parse source protocol
			sp = parseProtocolId(ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, PROTOCOL_ID_LENGTH)));
			byteOffset += PROTOCOL_ID_LENGTH;

			// parse identifier
			identifier = ByteArray.bytesToLong(ByteArray.subByte(bytes, byteOffset, IDENTIFIER_LENGTH));
			byteOffset += IDENTIFIER_LENGTH;

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
			final int restLength = length - ((safi == BGPSubsequentAddressFamily.MPLSLabeledVPN) ? ROUTE_DISTINGUISHER_LENGTH : 0)
					- PROTOCOL_ID_LENGTH - IDENTIFIER_LENGTH - TYPE_LENGTH - LENGTH_SIZE - locallength;
			logger.debug("Restlength {}", restLength);
			switch (type) {
			case LinkNLRI:
				links.add(parseLink(localDescriptor, sp, ByteArray.subByte(bytes, byteOffset, restLength)));
				break;
			case IPv4Prefixes:
			case IPv6Prefixes:
				descs.add(parsePrefixDescriptors(localDescriptor, ByteArray.subByte(bytes, byteOffset, restLength)));
				break;
			case NodeNLRI:
				// node nlri is already parsed as it contains only the common fields for node and link nlri
				nodes.add(localDescriptor);
				break;
			}
			byteOffset += restLength;
		}
		if (!links.isEmpty())
			return new BGPLinkMP(identifier, sp, reachable, links);
		else if (!nodes.isEmpty())
			return new BGPNodeMP(identifier, sp, reachable, nodes);
		// else if (!descs.isEmpty())
		// return new BGPIPv4PrefixMP(identifier, sp, descs, reachable);
		return null;
	}

	protected static Map<Integer, ByteList> parseLinkState(final byte[] bytes) {
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
		return map;
	}

	public static boolean verifyLink(final Set<Integer> keys) {
		for (final Integer i : keys)
			if (!linkTlvs.contains(i)) {
				logger.warn("Invalid link attribute {}", i);
				return false;
			}
		return true;
	}

	public static boolean verifyNode(final Set<Integer> keys) {
		for (final Integer i : keys)
			if (!nodeTlvs.contains(i)) {
				logger.warn("Invalid node attribute {}", i);
				return false;
			}
		return true;
	}

	public static boolean verifyPrefix(final Set<Integer> keys) {
		for (final Integer i : keys)
			if (!prefixTlvs.contains(i)) {
				logger.warn("Invalid prefix attribute {}", i);
				return false;
			}
		return true;
	}

	/**
	 * Parse protocol ID from int to enum
	 * 
	 * @param protocolId int parsed from byte array
	 * @return enum SourceProtocol
	 * @throws BGPParsingException if the type is unrecognized
	 */
	private static SourceProtocol parseProtocolId(final int protocolId) throws BGPParsingException {
		switch (protocolId) {
		case 0:
			return SourceProtocol.Unknown;
		case 1:
			return SourceProtocol.ISISLevel1;
		case 2:
			return SourceProtocol.ISISLevel2;
		case 3:
			return SourceProtocol.OSPF;
		case 4:
			return SourceProtocol.Direct;
		case 5:
			return SourceProtocol.Static;
		default:
			throw new BGPParsingException("Unknown Source Protocol ID: " + protocolId);
		}
	}

	private static OSPFRouteType parseRouteType(final int type) throws BGPParsingException {
		switch (type) {
		case 0:
			return null; // for IS-IS it needs to be 0
		case 1:
			return OSPFRouteType.Intra_Area;
		case 2:
			return OSPFRouteType.Inter_Area;
		case 3:
			return OSPFRouteType.External1;
		case 4:
			return OSPFRouteType.External2;
		case 5:
			return OSPFRouteType.NSSA1;
		case 6:
			return OSPFRouteType.NSSA2;
		default:
			throw new BGPParsingException("Unknown OSPF Route Type: " + type);
		}
	}

	private static LinkIdentifier parseLink(final NodeIdentifier local, final SourceProtocol spi, final byte[] bytes)
			throws BGPParsingException {
		int byteOffset = 0;
		final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
		byteOffset += TYPE_LENGTH;
		final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
		byteOffset += LENGTH_SIZE;
		NodeIdentifier remote = null;
		if (type == 257) {
			remote = parseNodeDescriptors(ByteArray.subByte(bytes, byteOffset, length));
			byteOffset += length;
		}

		return parseLinkDescriptors(local, remote, ByteArray.subByte(bytes, byteOffset, bytes.length - byteOffset));
	}

	/**
	 * Parse Link Descriptors.
	 * 
	 * @param topology
	 * @param localAnchor
	 * @param remoteAnchor
	 * @param bytes
	 * @return
	 * @throws BGPParsingException
	 */
	private static LinkIdentifier parseLinkDescriptors(final NodeIdentifier local, final NodeIdentifier remote, final byte[] bytes)
			throws BGPParsingException {
		int byteOffset = 0;
		final List<InterfaceIdentifier> localIdentifiers = Lists.newArrayList();
		final List<InterfaceIdentifier> remoteIdentifiers = Lists.newArrayList();
		TopologyIdentifier topId = null;
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			logger.debug("Parsing Link Descriptor: {}", Arrays.toString(value));
			switch (type) {
			case 258:
				final UnnumberedLinkIdentifier l = new UnnumberedLinkIdentifier(ByteArray.bytesToLong(ByteArray.subByte(value, 0, 4)));
				final UnnumberedLinkIdentifier r = new UnnumberedLinkIdentifier(ByteArray.bytesToLong(ByteArray.subByte(value, 4, 4)));
				localIdentifiers.add(l);
				remoteIdentifiers.add(r);
				logger.trace("Parsed link local {} remote {} Identifiers.", local, remote);
				break;
			case 259:
				final IPv4InterfaceIdentifier lipv4 = new IPv4InterfaceIdentifier(new IPv4Address(value));
				localIdentifiers.add(lipv4);
				logger.trace("Parsed IPv4 interface address {}.", lipv4);
				break;
			case 260:
				final IPv4InterfaceIdentifier ripv4 = new IPv4InterfaceIdentifier(new IPv4Address(value));
				remoteIdentifiers.add(ripv4);
				logger.trace("Parsed IPv4 neighbor address {}.", ripv4);
				break;
			case 261:
				final IPv6InterfaceIdentifier lipv6 = new IPv6InterfaceIdentifier(new IPv6Address(value));
				localIdentifiers.add(lipv6);
				logger.trace("Parsed IPv6 interface address {}.", lipv6);
				break;
			case 262:
				final IPv6InterfaceIdentifier ripv6 = new IPv6InterfaceIdentifier(new IPv6Address(value));
				remoteIdentifiers.add(ripv6);
				logger.trace("Parsed IPv6 neighbor address {}.", ripv6);
				break;
			case 263:
				topId = new TopologyIdentifier(ByteArray.bytesToLong(value) & 0x3fff);
				logger.trace("Parsed topology identifier {}.", topId);
				break;
			default:
				throw new BGPParsingException("Link Descriptor not recognized, type: " + type);
			}
			byteOffset += length;
		}
		logger.debug("Finished parsing Link descriptors.");
		if (localIdentifiers.size() != 1)
			throw new BGPParsingException("Invalid number of local interface identifiers.");
		final LinkAnchor localAnchor = new LinkAnchor(local, localIdentifiers.get(0));
		LinkAnchor remoteAnchor = null;
		if (remoteIdentifiers.size() > 0) {
			remoteAnchor = new LinkAnchor(remote, remoteIdentifiers.get(0));
		} else
			remoteAnchor = new LinkAnchor(remote, null);
		return new LinkIdentifier(topId, localAnchor, remoteAnchor);
	}

	/**
	 * Parse Node Descriptors. There can be only one TLV present from each type.
	 * 
	 * @param spi
	 * @param bytes
	 * @return
	 * @throws BGPParsingException
	 */
	private static NodeIdentifier parseNodeDescriptors(final byte[] bytes) throws BGPParsingException {
		int byteOffset = 0;
		ASNumber asnumber = null;
		DomainIdentifier bgpId = null;
		AreaIdentifier ai = null;
		RouterIdentifier routerId = null;
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			logger.debug("Parsing Node Descriptor: {}", Arrays.toString(value));
			switch (type) {
			case 512:
				asnumber = new ASNumber(ByteArray.bytesToLong(value));
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
					routerId = new ISISRouterIdentifier(new ISOSystemIdentifier(ByteArray.subByte(value, 0, 6)));
				} else if (value.length == 7) {
					if (value[6] == 0) {
						logger.warn("PSN octet is 0. Ignoring System ID.");
						routerId = new ISISRouterIdentifier(new ISOSystemIdentifier(ByteArray.subByte(value, 0, 6)));
						break;
					} else
						routerId = new ISISLANIdentifier(new ISOSystemIdentifier(ByteArray.subByte(value, 0, 6)), value[6]);
				} else if (value.length == 4) {
					routerId = new OSPFRouterIdentifier(ByteArray.subByte(value, 0, 4));
				} else if (value.length == 8) {
					final byte[] o = ByteArray.subByte(value, 0, 4); // FIXME: OSPFv3 vs OSPFv2
					final OSPFInterfaceIdentifier a = new OSPFInterfaceIdentifier(ByteArray.subByte(value, 4, 4));
					routerId = new OSPFv3LANIdentifier(new OSPFRouterIdentifier(o), a);
				}
				logger.trace("Parsed Router Identifier {}", routerId);
				break;
			default:
				throw new BGPParsingException("Node Descriptor not recognized, type: " + type);
			}
			byteOffset += length;
		}
		logger.debug("Finished parsing Node descriptors.");
		return new NodeIdentifier(asnumber, bgpId, ai, routerId);
	}

	private static PrefixIdentifier<?> parsePrefixDescriptors(final NodeIdentifier localDescriptor, final byte[] bytes)
			throws BGPParsingException {
		int byteOffset = 0;
		TopologyIdentifier topologyId = null;
		OSPFRouteType routeType = null;
		Prefix<?> prefix = null;
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			logger.trace("Parsing Prefix Descriptor: {}", Arrays.toString(value));
			switch (type) {
			case 263:
				topologyId = new TopologyIdentifier(ByteArray.bytesToLong(value) & 0x3fff);
				logger.trace("Parsed Topology Identifier: {}", topologyId);
				break;
			case 264:
				final int rt = ByteArray.bytesToInt(value);
				routeType = parseRouteType(rt);
				logger.trace("Parser RouteType: {}", routeType);
				break;
			case 265:
				final int prefixLength = UnsignedBytes.toInt(value[0]);
				final int size = prefixLength / 8 + ((prefixLength % 8 == 0) ? 0 : 1);
				if (size != value.length - 1) {
					logger.debug("Expected length {}, actual length {}.", size, value.length - 1);
					throw new BGPParsingException("Illegal length of IP reachability TLV: " + (value.length - 1));
				}
				prefix = IPv6.FAMILY.prefixForBytes(ByteArray.subByte(value, 1, size), prefixLength);
				logger.trace("Parsed IP reachability info: {}", prefix);
				break;
			default:
				throw new BGPParsingException("Prefix Descriptor not recognized, type: " + type);
			}
			byteOffset += length;
		}
		logger.debug("Finished parsing Prefix descriptors.");
		if (routeType != null) {
			if (prefix instanceof IPv4Prefix) {
				return new OSPFPrefixIdentifier<IPv4Address>(localDescriptor, (IPv4Prefix) prefix, routeType);
			} else {
				return new OSPFPrefixIdentifier<IPv6Address>(localDescriptor, (IPv6Prefix) prefix, routeType);
			}
		}
		return (prefix instanceof IPv4Prefix) ? new IPv4PrefixIdentifier(localDescriptor, (IPv4Prefix) prefix)
				: new IPv6PrefixIdentifier(localDescriptor, (IPv6Prefix) prefix);
	}

	/**
	 * Parse Link Attributes.
	 * 
	 * @param descriptors
	 * @param bytes
	 * @return
	 * @throws BGPParsingException
	 */
	public static NetworkLinkImpl parseLinkAttributes(final LinkIdentifier linkId, final Map<Integer, ByteList> attributes)
			throws BGPParsingException {

		final Set<SharedRiskLinkGroup> sharedRiskLinkGroups = Sets.newHashSet();
		final Set<MPLSProtocol> enabledMPLSProtocols = Sets.newHashSet();
		NetworkLinkState state = NetworkLinkState.EMPTY;

		final Set<RouterIdentifier> localIds = Sets.newHashSet();
		final Set<RouterIdentifier> remoteIds = Sets.newHashSet();

		String name = null;
		for (final Entry<Integer, ByteList> entry : attributes.entrySet()) {
			logger.debug("Link attribute TLV {}", entry.getKey());

			for (final byte[] value : entry.getValue().getBytes()) {

				switch (entry.getKey()) {
				case 1028:
					final IPv4RouterIdentifier lipv4 = new IPv4RouterIdentifier(new IPv4Address(value));
					localIds.add(lipv4);
					logger.trace("Parsed IPv4 Router-ID of local node: {}", lipv4);
					break;
				case 1029:
					final IPv6RouterIdentifier lipv6 = new IPv6RouterIdentifier(new IPv6Address(value));
					localIds.add(lipv6);
					logger.trace("Parsed IPv6 Router-ID of local node: {}", lipv6);
					break;
				case 1030:
					final IPv4RouterIdentifier ripv4 = new IPv4RouterIdentifier(new IPv4Address(value));
					remoteIds.add(ripv4);
					logger.trace("Parsed IPv4 Router-ID of remote node: {}", ripv4);
					break;
				case 1031:
					final IPv6RouterIdentifier ripv6 = new IPv6RouterIdentifier(new IPv6Address(value));
					remoteIds.add(ripv6);
					logger.trace("Parsed IPv6 Router-ID of remote node: {}", ripv6);
					break;
				case 1088:
					state = state.withAdministrativeGroup(new AdministrativeGroup(ByteArray.bytesToLong(value)));
					logger.trace("Parsed Administrative Group {}", state.getAdministrativeGroup());
					break;
				case 1089:
					state = state.withMaximumBandwidth(new Bandwidth(ByteArray.bytesToFloat(value)));
					logger.trace("Parsed Max Bandwidth {}", state.getMaximumBandwidth());
					break;
				case 1090:
					state = state.withReservableBandwidth(new Bandwidth(ByteArray.bytesToFloat(value)));
					logger.trace("Parsed Max Reservable Bandwidth {}", state.getMaximumReservableBandwidth());
					break;
				case 1091:
					int index = 0;
					final Bandwidth[] unreservedBandwidth = new Bandwidth[8];
					for (int i = 0; i < 8; i++) {
						unreservedBandwidth[i] = new Bandwidth(ByteArray.bytesToFloat(ByteArray.subByte(value, index, 4)));
						index += 4;
					}
					state = state.withUnreservedBandwidth(unreservedBandwidth);
					logger.trace("Parsed Unreserved Bandwidth {}", Arrays.toString(state.getUnreservedBandwidth()));
					break;
				case 1092:
					state = state.withMetric(TEMetric.class, new TEMetric(ByteArray.bytesToInt(value)));
					logger.trace("Parsed Metric {}", state.getMetric(TEMetric.class));
					break;
				case 1093:
					state = state.withProtectionType(parseLinkProtectionType(UnsignedBytes.toInt(value[0])));
					logger.trace("Parsed Link Protection Type {}", state.getProtectionType());
					break;
				case 1094:
					final boolean[] bits = ByteArray.parseBits(value[0]);
					if (bits[0] == true) {
						enabledMPLSProtocols.add(MPLSProtocol.LDP);
					}
					if (bits[1] == true) {
						enabledMPLSProtocols.add(MPLSProtocol.RSVPTE);
					}
					logger.trace("Parsed MPLS Protocols: {}", Arrays.toString(enabledMPLSProtocols.toArray()));
					break;
				case 1095:
					state = state.withDefaultMetric(new IGPMetric(ByteArray.bytesToLong(value)));
					logger.trace("Parsed Metric {}", state.getDefaultMetric());
					break;
				case 1096:
					int i = 0;
					while (i != value.length) {
						sharedRiskLinkGroups.add(new SharedRiskLinkGroup(ByteArray.bytesToLong(ByteArray.subByte(value, i, 4))));
						i += 4;
					}
					logger.trace("Parsed Shared Risk Link Groups {}", Arrays.toString(sharedRiskLinkGroups.toArray()));
					break;
				case 1097:
					final byte[] opaque = value;
					logger.trace("Parsed Opaque value : {}", Arrays.toString(opaque));
					break;
				case 1098:
					name = new String(value, Charsets.US_ASCII);
					logger.trace("Parsed Link Name : ", name);
					break;
				default:
					throw new BGPParsingException("Link Attribute not recognized, type: " + entry.getKey());
				}
			}
		}
		state = state.withLocalRouterIdentifiers(localIds);
		state = state.withRemoteRouterIdentifiers(remoteIds);
		state = state.withEnabledMPLSProtocols(enabledMPLSProtocols);
		state = state.withSharedRiskLinkGroups(sharedRiskLinkGroups);
		state = state.withSymbolicName(name);
		final NetworkLinkImpl link = new NetworkLinkImpl(linkId, state);
		logger.debug("Finished parsing Link Attributes.");
		return link;
	}

	/**
	 * Parse Node Attributes.
	 * 
	 * @param descriptors
	 * @param bytes
	 * @return
	 * @throws BGPParsingException
	 */
	public static NetworkNodeImpl parseNodeAttributes(final NodeIdentifier nodeId, final Map<Integer, ByteList> attributes)
			throws BGPParsingException {
		final Set<TopologyIdentifier> topologyMembership = Sets.newHashSet();
		final Set<ISISAreaIdentifier> areaMembership = Sets.newHashSet();
		final NetworkNodeImpl node = new NetworkNodeImpl(nodeId);
		final Set<RouterIdentifier> ids = Sets.newHashSet();
		for (final Entry<Integer, ByteList> entry : attributes.entrySet()) {
			logger.debug("Node attribute TLV {}", entry.getKey());
			for (final byte[] value : entry.getValue().getBytes()) {
				switch (entry.getKey()) {
				case 263:
					int i = 0;
					while (i != value.length) {
						final TopologyIdentifier topId = new TopologyIdentifier(ByteArray.bytesToLong(ByteArray.subByte(value, i, 2)) & 0x3fff);
						topologyMembership.add(topId);
						logger.trace("Parsed Topology Identifier: {}", topId);
						i += 2;
					}
					break;
				case 1024:
					final boolean[] flags = ByteArray.parseBits(value[0]);
					node.currentState().withOverload(flags[0]);
					node.currentState().withAttached(flags[1]);
					node.currentState().withExternal(flags[2]);
					node.currentState().withAreaBorderRouter(flags[3]);
					logger.trace("Parsed External bit {}, area border router {}.", flags[2], flags[3]);
					break;
				case 1025:
					logger.debug("Ignoring opaque value: {}.", Arrays.toString(value));
					break;
				case 1026:
					node.currentState().withDynamicHostname(new String(value, Charsets.US_ASCII));
					logger.trace("Parsed Node Name {}", node.currentState().getDynamicHostname());
					break;
				case 1027:
					final ISISAreaIdentifier ai = new ISISAreaIdentifier(value);
					areaMembership.add(ai);
					logger.trace("Parsed AreaIdentifier {}", ai);
					break;
				case 1028:
					final IPv4RouterIdentifier ip4 = new IPv4RouterIdentifier(new IPv4Address(value));
					ids.add(ip4);
					logger.trace("Parsed IPv4 Router Identifier {}", ip4);
					break;
				case 1029:
					final IPv6RouterIdentifier ip6 = new IPv6RouterIdentifier(new IPv6Address(value));
					ids.add(ip6);
					logger.trace("Parsed IPv6 Router Identifier {}", ip6);
					break;
				default:
					throw new BGPParsingException("Node Attribute not recognized, type: " + entry.getKey());
				}
			}
		}

		node.currentState().withAreaMembership(areaMembership);
		node.currentState().withIdentifierAlternatives(ids);
		node.currentState().withTopologyMembership(topologyMembership);
		logger.debug("Finished parsing Node Attributes.");
		return node;
	}

	public static NetworkPrefixState parsePrefixAttributes(final SourceProtocol src, final NetworkObjectState nos,
			final Map<Integer, ByteList> attributes) throws BGPParsingException {

		boolean upDownBit = false;
		final SortedSet<RouteTag> routeTags = Sets.newTreeSet();
		final SortedSet<ExtendedRouteTag> exRouteTags = Sets.newTreeSet();
		Metric<?> metric = null;
		IPv4Address fwdAddress4 = null;
		IPv6Address fwdAddress6 = null;
		for (final Entry<Integer, ByteList> entry : attributes.entrySet()) {
			logger.debug("Prefix attribute TLV {}", entry.getKey());
			for (final byte[] value : entry.getValue().getBytes()) {
				switch (entry.getKey()) {
				case 1152:
					final boolean[] flags = ByteArray.parseBits(value[0]);
					upDownBit = flags[2];
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
					metric = new IGPMetric(ByteArray.bytesToLong(value));
					logger.trace("Parsed Metric: {}", metric);
					break;
				case 1156:
					switch (value.length) {
					case 4:
						fwdAddress4 = new IPv4Address(value);
						logger.trace("Parsed FWD Address: {}", fwdAddress4);
						break;
					case 16:
						fwdAddress6 = new IPv6Address(value);
						logger.trace("Parsed FWD Address: {}", fwdAddress6);
						break;
					default:
						logger.debug("Ignoring unsupported forwarding address length {}", value.length);
					}

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

		final NetworkPrefixState nps = new NetworkPrefixState(nos, routeTags, metric);
		switch (src) {
		case ISISLevel1:
		case ISISLevel2:
			return new ISISNetworkPrefixState(nps, exRouteTags, upDownBit);
		case OSPF:
			if (fwdAddress4 != null)
				return new OSPFNetworkPrefixState<IPv4Address>(nps, fwdAddress4);
			if (fwdAddress6 != null)
				return new OSPFNetworkPrefixState<IPv6Address>(nps, fwdAddress6);
			logger.debug("OSPF-sourced has no forwarding address");
			return nps;
		default:
			return nps;
		}
	}

	/**
	 * Parse Link Protection Type from int to enum
	 * 
	 * @param type int parsed from byte array
	 * @return enum LinkProtectionType
	 * @throws BGPParsingException if the type is unrecognized
	 */
	private static LinkProtectionType parseLinkProtectionType(final int type) throws BGPParsingException {
		switch (type) {
		case 1:
			return LinkProtectionType.EXTRA_TRAFFIC;
		case 2:
			return LinkProtectionType.UNPROTECTED;
		case 4:
			return LinkProtectionType.SHARED;
		case 8:
			return LinkProtectionType.DEDICATED_ONE_TO_ONE;
		case 16:
			return LinkProtectionType.DEDICATED_ONE_PLUS_ONE;
		default:
			throw new BGPParsingException("Link Protection Type not recognized: " + type);
		}
	}

	/**
	 * Parse NLRI Type from int to enum
	 * 
	 * @param type int parsed from byte array
	 * @return enum NlriType
	 * @throws BGPParsingException if the type is unrecognized
	 */
	private static NlriType parseNLRItype(final int type) throws BGPParsingException {
		switch (type) {
		case 1:
			return NlriType.NodeNLRI;
		case 2:
			return NlriType.LinkNLRI;
		case 3:
			return NlriType.IPv4Prefixes;
		case 4:
			return NlriType.IPv6Prefixes;
		default:
			throw new BGPParsingException("NLRI Type not recognized: " + type);
		}
	}
}
