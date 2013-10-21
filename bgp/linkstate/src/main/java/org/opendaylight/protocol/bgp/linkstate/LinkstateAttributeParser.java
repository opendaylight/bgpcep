/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.AdministrativeGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.ExtendedRouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.IgpBits.UpDown;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.Ipv6RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.MplsProtocolMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.NodeFlagBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.RouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.link.state.UnreservedBandwidthBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.IgpMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.TeMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgId;
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
	public static final int TYPE = 99;

	private static final Logger logger = LoggerFactory.getLogger(LinkstateAttributeParser.class);

	private static final int TYPE_LENGTH = 2;

	private static final int LENGTH_SIZE = 2;

	private static final Set<Integer> nodeTlvs = Sets.newHashSet(263, 1024, 1025, 1026, 1027, 1028, 1029);

	private static final Set<Integer> linkTlvs = Sets.newHashSet(1028, 1029, 1030, 1031, 1088, 1089, 1090, 1091, 1092, 1093, 1094, 1095,
			1096, 1097, 1098);

	private static final Set<Integer> prefixTlvs = Sets.newHashSet(1152, 1153, 1154, 1155, 1156, 1157);

	@Override
	public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPParsingException {
		final PathAttributes1 a = new PathAttributes1Builder().setLinkstatePathAttribute(parseLinkState(bytes)).build();
		builder.addAugmentation(PathAttributes1.class, a);
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

	protected static LinkstatePathAttribute parseLinkState(final byte[] bytes) throws BGPParsingException {
		final Map<Integer, ByteList> map = new HashMap<Integer, ByteList>();
		int byteOffset = 0;
		while (byteOffset != bytes.length) {
			final int type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_LENGTH));
			byteOffset += TYPE_LENGTH;
			final int length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_SIZE));
			byteOffset += LENGTH_SIZE;
			final byte[] value = ByteArray.subByte(bytes, byteOffset, length);
			final ByteList values = map.containsKey(type) ? map.get(type) : new ByteList();
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
					final List<SrlgId> sharedRiskLinkGroups = Lists.newArrayList();
					while (i != value.length) {
						sharedRiskLinkGroups.add(new SrlgId(ByteArray.bytesToLong(ByteArray.subByte(value, i, 4))));
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
