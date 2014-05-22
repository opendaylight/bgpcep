/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map.Entry;

import org.opendaylight.protocol.bgp.parser.AttributeFlags;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.AdministrativeGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.ExtendedRouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.IgpBits.UpDown;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.Ipv6RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.MplsProtocolMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NodeFlagBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.RouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.link.state.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.link.state.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.prefix.state.IgpBitsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.LinkstatePathAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.LinkstatePathAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IgpMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for Link State Path Attribute.
 *
 * @see <a href="http://tools.ietf.org/html/draft-gredler-idr-ls-distribution-04">BGP-LS draft</a>
 */
public class LinkstateAttributeParser implements AttributeParser, AttributeSerializer {
    // TODO: replace with actual values by IANA
    public static final int TYPE = 99;

    private static final Logger LOG = LoggerFactory.getLogger(LinkstateAttributeParser.class);

    private static final int ROUTE_TAG_LENGTH = 4;

    private static final int EXTENDED_ROUTE_TAG_LENGTH = 8;

    private static final int SRLG_LENGTH = 4;

    private static final int UNRESERVED_BW_COUNT = 8;

    private static final int BANDWIDTH_LENGTH = 4;

    // node flag bits
    private static final int OVERLOAD_BIT = 7;
    private static final int ATTACHED_BIT = 6;
    private static final int EXTERNAL_BIT = 5;
    private static final int ABBR_BIT = 4;

    // MPLS protection mask bits
    private static final int LDP_BIT = 7;
    private static final int RSVP_BIT = 6;

    private NlriType getNlriType(final PathAttributesBuilder pab) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1 mpr = pab.getAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1.class);
        if (mpr != null && mpr.getMpReachNlri() != null) {
            final DestinationType dt = mpr.getMpReachNlri().getAdvertizedRoutes().getDestinationType();
            if (dt instanceof DestinationLinkstateCase) {
                for (final CLinkstateDestination d : ((DestinationLinkstateCase) dt).getDestinationLinkstate().getCLinkstateDestination()) {
                    return d.getNlriType();
                }
            }
        } else {
            LOG.debug("No MP_REACH attribute present");
        }
        final PathAttributes2 mpu = pab.getAugmentation(PathAttributes2.class);
        if (mpu != null && mpu.getMpUnreachNlri() != null) {
            final DestinationType dt = mpu.getMpUnreachNlri().getWithdrawnRoutes().getDestinationType();
            if (dt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase) {
                for (final CLinkstateDestination d : ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase) dt).getDestinationLinkstate().getCLinkstateDestination()) {
                    return d.getNlriType();
                }
            }
        } else {
            LOG.debug("No MP_UNREACH attribute present");
        }
        return null;
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) throws BGPParsingException {
        final NlriType type = getNlriType(builder);
        if (type == null) {
            LOG.warn("No Linkstate NLRI found, not parsing Linkstate attribute");
            return;
        }
        final PathAttributes1 a = new PathAttributes1Builder().setLinkstatePathAttribute(parseLinkState(type, buffer)).build();
        builder.addAugmentation(PathAttributes1.class, a);
    }

    private static LinkstatePathAttribute parseLinkState(final NlriType nlri, final ByteBuf buffer) throws BGPParsingException {
        /*
         * e.g. IS-IS Area Identifier TLV can occur multiple times
         */
        final Multimap<Integer, byte[]> map = HashMultimap.create();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final byte[] value = ByteArray.readBytes(buffer, length);
            map.put(type, value);
        }
        final LinkstatePathAttributeBuilder builder = new LinkstatePathAttributeBuilder();

        switch (nlri) {
        case Ipv4Prefix:
        case Ipv6Prefix:
            builder.setLinkStateAttribute(parsePrefixAttributes(map));
            return builder.build();
        case Link:
            builder.setLinkStateAttribute(parseLinkAttributes(map));
            return builder.build();
        case Node:
            builder.setLinkStateAttribute(parseNodeAttributes(map));
            return builder.build();
        }
        throw new IllegalStateException("Unhandled NLRI type " + nlri);
    }

    /**
     * Parse Link Attributes.
     *
     * @param attributes key is the tlv type and value is the value of the tlv
     * @return {@link LinkStateAttribute}
     */
    private static LinkStateAttribute parseLinkAttributes(final Multimap<Integer, byte[]> attributes) {
        final LinkAttributesBuilder builder = new LinkAttributesBuilder();
        for (final Entry<Integer, byte[]> entry : attributes.entries()) {
            LOG.trace("Link attribute TLV {}", entry.getKey());
            final int key = entry.getKey();
            final byte[] value = entry.getValue();
            switch (key) {
            case TlvCode.LOCAL_IPV4_ROUTER_ID:
                final Ipv4RouterIdentifier lipv4 = new Ipv4RouterIdentifier(Ipv4Util.addressForBytes(value));
                builder.setLocalIpv4RouterId(lipv4);
                LOG.debug("Parsed IPv4 Router-ID of local node: {}", lipv4);
                break;
            case TlvCode.LOCAL_IPV6_ROUTER_ID:
                final Ipv6RouterIdentifier lipv6 = new Ipv6RouterIdentifier(Ipv6Util.addressForBytes(value));
                builder.setLocalIpv6RouterId(lipv6);
                LOG.debug("Parsed IPv6 Router-ID of local node: {}", lipv6);
                break;
            case TlvCode.REMOTE_IPV4_ROUTER_ID:
                final Ipv4RouterIdentifier ripv4 = new Ipv4RouterIdentifier(Ipv4Util.addressForBytes(value));
                builder.setRemoteIpv4RouterId(ripv4);
                LOG.debug("Parsed IPv4 Router-ID of remote node: {}", ripv4);
                break;
            case TlvCode.REMOTE_IPV6_ROUTER_ID:
                final Ipv6RouterIdentifier ripv6 = new Ipv6RouterIdentifier(Ipv6Util.addressForBytes(value));
                builder.setRemoteIpv6RouterId(ripv6);
                LOG.debug("Parsed IPv6 Router-ID of remote node: {}", ripv6);
                break;
            case TlvCode.ADMIN_GROUP:
                builder.setAdminGroup(new AdministrativeGroup(ByteArray.bytesToLong(value)));
                LOG.debug("Parsed Administrative Group {}", builder.getAdminGroup());
                break;
            case TlvCode.MAX_BANDWIDTH:
                builder.setMaxLinkBandwidth(new Bandwidth(value));
                LOG.debug("Parsed Max Bandwidth {}", builder.getMaxLinkBandwidth());
                break;
            case TlvCode.MAX_RESERVABLE_BANDWIDTH:
                builder.setMaxReservableBandwidth(new Bandwidth(value));
                LOG.debug("Parsed Max Reservable Bandwidth {}", builder.getMaxReservableBandwidth());
                break;
            case TlvCode.UNRESERVED_BANDWIDTH:
                int index = 0;
                final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.link.state.UnreservedBandwidth> unreservedBandwidth = Lists.newArrayList();
                for (int i = 0; i < UNRESERVED_BW_COUNT; i++) {
                    final byte[] v = ByteArray.subByte(value, index, BANDWIDTH_LENGTH);
                    unreservedBandwidth.add(new UnreservedBandwidthBuilder().setBandwidth(new Bandwidth(v)).setPriority((short) i).build());
                    index += BANDWIDTH_LENGTH;
                }
                builder.setUnreservedBandwidth(unreservedBandwidth);
                LOG.debug("Parsed Unreserved Bandwidth {}", builder.getUnreservedBandwidth());
                break;
            case TlvCode.TE_METRIC:
                builder.setTeMetric(new TeMetric(ByteArray.bytesToLong(value)));
                LOG.debug("Parsed Metric {}", builder.getTeMetric());
                break;
            case TlvCode.LINK_PROTECTION_TYPE:
                final LinkProtectionType lpt = LinkProtectionType.forValue(UnsignedBytes.toInt(value[1]));
                if (lpt == null) {
                    LOG.warn("Link Protection Type not recognized: {}", UnsignedBytes.toInt(value[1]));
                    break;
                }
                builder.setLinkProtection(lpt);
                LOG.debug("Parsed Link Protection Type {}", lpt);
                break;
            case TlvCode.MPLS_PROTOCOL:
                final boolean[] bits = ByteArray.parseBits(value[0]);
                builder.setMplsProtocol(new MplsProtocolMask(bits[0], bits[1]));
                LOG.debug("Parsed MPLS Protocols: {}", builder.getMplsProtocol());
                break;
            case TlvCode.METRIC:
                builder.setMetric(new Metric(ByteArray.bytesToLong(value)));
                LOG.debug("Parsed Metric {}", builder.getMetric());
                break;
            case TlvCode.SHARED_RISK_LINK_GROUP:
                int i = 0;
                final List<SrlgId> sharedRiskLinkGroups = Lists.newArrayList();
                while (i != value.length) {
                    sharedRiskLinkGroups.add(new SrlgId(ByteArray.bytesToLong(ByteArray.subByte(value, i, SRLG_LENGTH))));
                    i += SRLG_LENGTH;
                }
                builder.setSharedRiskLinkGroups(sharedRiskLinkGroups);
                LOG.debug("Parsed Shared Risk Link Groups {}", Arrays.toString(sharedRiskLinkGroups.toArray()));
                break;
            case TlvCode.LINK_OPAQUE:
                final byte[] opaque = value;
                LOG.debug("Parsed Opaque value : {}", Arrays.toString(opaque));
                break;
            case TlvCode.LINK_NAME:
                final String name = new String(value, Charsets.US_ASCII);
                builder.setLinkName(name);
                LOG.debug("Parsed Link Name : {}", name);
                break;
            default:
                LOG.warn("TLV {} is not a valid link attribute, ignoring it", key);
            }
        }
        LOG.trace("Finished parsing Link Attributes.");
        return new LinkAttributesCaseBuilder().setLinkAttributes(builder.build()).build();
    }

    /**
     * Parse Node Attributes.
     *
     * @param attributes key is the tlv type and value is the value of the tlv
     * @return {@link LinkStateAttribute}
     */
    private static LinkStateAttribute parseNodeAttributes(final Multimap<Integer, byte[]> attributes) {
        final List<TopologyIdentifier> topologyMembership = Lists.newArrayList();
        final List<IsisAreaIdentifier> areaMembership = Lists.newArrayList();
        final NodeAttributesBuilder builder = new NodeAttributesBuilder();
        for (final Entry<Integer, byte[]> entry : attributes.entries()) {
            final int key = entry.getKey();
            final byte[] value = entry.getValue();
            LOG.trace("Node attribute TLV {}", key);
            switch (key) {
            case TlvCode.MULTI_TOPOLOGY_ID:
                int i = 0;
                while (i != value.length) {
                    final TopologyIdentifier topId = new TopologyIdentifier(ByteArray.bytesToInt(ByteArray.subByte(value, i, 2)) & 0x3fff);
                    topologyMembership.add(topId);
                    LOG.debug("Parsed Topology Identifier: {}", topId);
                    i += 2;
                }
                break;
            case TlvCode.NODE_FLAG_BITS:
                final boolean[] flags = ByteArray.parseBits(value[0]);
                builder.setNodeFlags(new NodeFlagBits(flags[0], flags[1], flags[2], flags[3]));
                LOG.debug("Parsed Overload bit: {}, attached bit: {}, external bit: {}, area border router: {}.", flags[0], flags[1],
                        flags[2], flags[3]);
                break;
            case TlvCode.NODE_OPAQUE:
                LOG.debug("Ignoring opaque value: {}.", Arrays.toString(value));
                break;
            case TlvCode.DYNAMIC_HOSTNAME:
                builder.setDynamicHostname(new String(value, Charsets.US_ASCII));
                LOG.debug("Parsed Node Name {}", builder.getDynamicHostname());
                break;
            case TlvCode.ISIS_AREA_IDENTIFIER:
                final IsisAreaIdentifier ai = new IsisAreaIdentifier(value);
                areaMembership.add(ai);
                LOG.debug("Parsed AreaIdentifier {}", ai);
                break;
            case TlvCode.LOCAL_IPV4_ROUTER_ID:
                final Ipv4RouterIdentifier ip4 = new Ipv4RouterIdentifier(Ipv4Util.addressForBytes(value));
                builder.setIpv4RouterId(ip4);
                LOG.debug("Parsed IPv4 Router Identifier {}", ip4);
                break;
            case TlvCode.LOCAL_IPV6_ROUTER_ID:
                final Ipv6RouterIdentifier ip6 = new Ipv6RouterIdentifier(Ipv6Util.addressForBytes(value));
                builder.setIpv6RouterId(ip6);
                LOG.debug("Parsed IPv6 Router Identifier {}", ip6);
                break;
            default:
                LOG.warn("TLV {} is not a valid node attribute, ignoring it", key);
            }
        }
        LOG.trace("Finished parsing Node Attributes.");
        builder.setTopologyIdentifier(topologyMembership);
        builder.setIsisAreaId(areaMembership);
        return new NodeAttributesCaseBuilder().setNodeAttributes(builder.build()).build();
    }

    /**
     * Parse prefix attributes.
     *
     * @param attributes key is the tlv type and value are the value bytes of the tlv
     * @return {@link LinkStateAttribute}
     */
    private static LinkStateAttribute parsePrefixAttributes(final Multimap<Integer, byte[]> attributes) {
        final PrefixAttributesBuilder builder = new PrefixAttributesBuilder();
        final List<RouteTag> routeTags = Lists.newArrayList();
        final List<ExtendedRouteTag> exRouteTags = Lists.newArrayList();
        for (final Entry<Integer, byte[]> entry : attributes.entries()) {
            final int key = entry.getKey();
            final byte[] value = entry.getValue();
            LOG.trace("Prefix attribute TLV {}", key);
            switch (key) {
            case TlvCode.IGP_FLAGS:
                final boolean[] flags = ByteArray.parseBits(value[0]);
                final boolean upDownBit = flags[0];
                builder.setIgpBits(new IgpBitsBuilder().setUpDown(new UpDown(upDownBit)).build());
                LOG.debug("Parsed IGP flag (up/down bit) : {}", upDownBit);
                break;
            case TlvCode.ROUTE_TAG:
                int offset = 0;
                while (offset != value.length) {
                    final RouteTag routeTag = new RouteTag(ByteArray.subByte(value, offset, ROUTE_TAG_LENGTH));
                    routeTags.add(routeTag);
                    LOG.debug("Parsed Route Tag: {}", routeTag);
                    offset += ROUTE_TAG_LENGTH;
                }
                break;
            case TlvCode.EXTENDED_ROUTE_TAG:
                offset = 0;
                while (offset != value.length) {
                    final ExtendedRouteTag exRouteTag = new ExtendedRouteTag(ByteArray.subByte(value, offset, EXTENDED_ROUTE_TAG_LENGTH));
                    exRouteTags.add(exRouteTag);
                    LOG.debug("Parsed Extended Route Tag: {}", exRouteTag);
                    offset += EXTENDED_ROUTE_TAG_LENGTH;
                }
                break;
            case TlvCode.PREFIX_METRIC:
                final IgpMetric metric = new IgpMetric(ByteArray.bytesToLong(value));
                builder.setPrefixMetric(metric);
                LOG.debug("Parsed Metric: {}", metric);
                break;
            case TlvCode.FORWARDING_ADDRESS:
                IpAddress fwdAddress = null;
                switch (value.length) {
                case Ipv4Util.IP4_LENGTH:
                    fwdAddress = new IpAddress(Ipv4Util.addressForBytes(value));
                    break;
                case Ipv6Util.IPV6_LENGTH:
                    fwdAddress = new IpAddress(Ipv6Util.addressForBytes(value));
                    break;
                default:
                    LOG.debug("Ignoring unsupported forwarding address length {}", value.length);
                }
                builder.setOspfForwardingAddress(fwdAddress);
                LOG.debug("Parsed FWD Address: {}", fwdAddress);
                break;
            case TlvCode.PREFIX_OPAQUE:
                LOG.debug("Parsed Opaque value: {}, not preserving it", ByteArray.bytesToHexString(value));
                break;
            default:
                LOG.warn("TLV {} is not a valid prefix attribute, ignoring it", key);
            }
        }
        LOG.trace("Finished parsing Prefix Attributes.");
        builder.setRouteTags(routeTags);
        builder.setExtendedTags(exRouteTags);
        return new PrefixAttributesCaseBuilder().setPrefixAttributes(builder.build()).build();
    }

    /**
     * Serialize linkstate attributes.
     *
     * @param attribute DataObject representing LinkstatePathAttribute
     * @param byteAggregator ByteBuf where all serialized data are aggregated
     */

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf linkstateAttrBuffer) {
        final PathAttributes1 pathAttributes1 = ((PathAttributes) attribute).getAugmentation(PathAttributes1.class);
        if (pathAttributes1 == null) {
            return;
        }
        final LinkStateAttribute linkState = pathAttributes1.getLinkstatePathAttribute().getLinkStateAttribute();
        final ByteBuf byteAggregator = Unpooled.buffer();
        if (linkState instanceof LinkAttributesCase) {
            serializeLinkAttributes((LinkAttributesCase) linkState, byteAggregator);
        } else if (linkState instanceof NodeAttributesCase) {
            serializeNodeAttributes((NodeAttributesCase) linkState, byteAggregator);
        } else if (linkState instanceof PrefixAttributesCase) {
            serializePrefixAttributes((PrefixAttributesCase) linkState, byteAggregator);
        }
        if (byteAggregator.writerIndex() > Values.UNSIGNED_BYTE_MAX_VALUE) {
            linkstateAttrBuffer.writeByte(AttributeFlags.OPTIONAL | AttributeFlags.EXTENDED);
            linkstateAttrBuffer.writeByte(TYPE);
            linkstateAttrBuffer.writeShort(byteAggregator.writerIndex());
        } else {
            linkstateAttrBuffer.writeByte(AttributeFlags.OPTIONAL);
            linkstateAttrBuffer.writeByte(TYPE);
            linkstateAttrBuffer.writeByte(byteAggregator.writerIndex());
        }
        linkstateAttrBuffer.writeBytes(byteAggregator);
    }

    private void serializeLinkAttributes(final LinkAttributesCase linkAttributesCase, final ByteBuf byteAggregator) {
        final LinkAttributes linkAttributes = linkAttributesCase.getLinkAttributes();
        LOG.trace("Started serializing Link Attributes");
        if (linkAttributes.getLocalIpv4RouterId() != null) {
            writeTLV(TlvCode.LOCAL_IPV4_ROUTER_ID, Unpooled.wrappedBuffer(InetAddresses.forString(linkAttributes.getLocalIpv4RouterId().getValue()).getAddress()), byteAggregator);
        }
        if (linkAttributes.getLocalIpv6RouterId() != null) {
            writeTLV(TlvCode.LOCAL_IPV6_ROUTER_ID, Unpooled.wrappedBuffer(InetAddresses.forString(linkAttributes.getLocalIpv6RouterId().getValue()).getAddress()), byteAggregator);
        }
        if (linkAttributes.getRemoteIpv4RouterId() != null) {
            writeTLV(TlvCode.REMOTE_IPV4_ROUTER_ID, Unpooled.wrappedBuffer(InetAddresses.forString(linkAttributes.getRemoteIpv4RouterId().getValue()).getAddress()), byteAggregator);
        }
        if (linkAttributes.getRemoteIpv6RouterId() != null) {
            writeTLV(TlvCode.REMOTE_IPV6_ROUTER_ID, Unpooled.wrappedBuffer(InetAddresses.forString(linkAttributes.getRemoteIpv6RouterId().getValue()).getAddress()), byteAggregator);
        }
        if (linkAttributes.getAdminGroup() != null) {
            writeTLV(TlvCode.ADMIN_GROUP, Unpooled.copyInt(linkAttributes.getAdminGroup().getValue().intValue()), byteAggregator);
        }
        if (linkAttributes.getMaxLinkBandwidth() != null) {
            writeTLV(TlvCode.MAX_BANDWIDTH, Unpooled.wrappedBuffer(linkAttributes.getMaxLinkBandwidth().getValue()), byteAggregator);
        }
        if (linkAttributes.getMaxReservableBandwidth() != null) {
            writeTLV(TlvCode.MAX_RESERVABLE_BANDWIDTH, Unpooled.wrappedBuffer(linkAttributes.getMaxReservableBandwidth().getValue()), byteAggregator);
        }
        // this sub-TLV contains eight 32-bit IEEE floating point numbers
        if (linkAttributes.getUnreservedBandwidth() != null) {
            final ByteBuf unreservedBandwithBuf = Unpooled.buffer();
            for (final UnreservedBandwidth unreservedBandwidth : linkAttributes.getUnreservedBandwidth()) {
                unreservedBandwithBuf.writeBytes(unreservedBandwidth.getBandwidth().getValue());
            }
            writeTLV(TlvCode.UNRESERVED_BANDWIDTH, unreservedBandwithBuf, byteAggregator);
        }
        if (linkAttributes.getTeMetric() != null) {
            writeTLV(TlvCode.TE_METRIC, Unpooled.copyLong(linkAttributes.getTeMetric().getValue().longValue()), byteAggregator);
        }
        if (linkAttributes.getLinkProtection() != null) {
            writeTLV(TlvCode.LINK_PROTECTION_TYPE, Unpooled.copyShort(linkAttributes.getLinkProtection().getIntValue()), byteAggregator);
        }
        MplsProtocolMask mplsProtocolMask = linkAttributes.getMplsProtocol();
        if (mplsProtocolMask != null) {
            final ByteBuf mplsProtocolMaskBuf = Unpooled.buffer();
            final BitSet mask = new BitSet();
            if (mplsProtocolMask.isLdp() != null) {
                mask.set(LDP_BIT, mplsProtocolMask.isLdp());
            }
            if (mplsProtocolMask.isRsvpte() != null) {
                mask.set(RSVP_BIT, mplsProtocolMask.isRsvpte());
            }
            mplsProtocolMaskBuf.writeBytes(mask.toByteArray());
            writeTLV(TlvCode.MPLS_PROTOCOL, mplsProtocolMaskBuf, byteAggregator);
        }
        if (linkAttributes.getMetric() != null) {
            // size of metric can be 1,2 or 3 depending on the protocol
            writeTLV(TlvCode.METRIC, Unpooled.copyMedium(linkAttributes.getMetric().getValue().intValue()), byteAggregator);
        }
        if (linkAttributes.getSharedRiskLinkGroups() != null) {
            final ByteBuf sharedRLGBuf = Unpooled.buffer();
            for (final SrlgId srlgId : linkAttributes.getSharedRiskLinkGroups()) {
                sharedRLGBuf.writeInt(srlgId.getValue().intValue());
            }
            writeTLV(TlvCode.SHARED_RISK_LINK_GROUP, sharedRLGBuf, byteAggregator);
        }
        if (linkAttributes.getLinkName() != null) {
            writeTLV(TlvCode.LINK_NAME, Unpooled.wrappedBuffer(linkAttributes.getLinkName().getBytes()), byteAggregator);
        }
        LOG.trace("Finished serializing Link Attributes");
    }

    private void serializeNodeAttributes(final NodeAttributesCase nodeAttributesCase, final ByteBuf byteAggregator) {
        LOG.trace("Started serializing Node Attributes");
        final NodeAttributes nodeAttributes = nodeAttributesCase.getNodeAttributes();
        if (nodeAttributes.getTopologyIdentifier() != null) {
            final ByteBuf mpIdBuf = Unpooled.buffer();
            for (final TopologyIdentifier topologyIdentifier : nodeAttributes.getTopologyIdentifier()) {
                mpIdBuf.writeShort(topologyIdentifier.getValue());
            }
            writeTLV(TlvCode.MULTI_TOPOLOGY_ID, mpIdBuf, byteAggregator);
        }
        final NodeFlagBits nodeFlagBits = nodeAttributes.getNodeFlags();
        if (nodeFlagBits != null) {
            final ByteBuf nodeFlagBuf = Unpooled.buffer();
            final BitSet flags = new BitSet();
            if (nodeFlagBits.isOverload() != null) {
                flags.set(OVERLOAD_BIT, nodeFlagBits.isOverload());
            }
            if (nodeFlagBits.isAttached() != null) {
                flags.set(ATTACHED_BIT, nodeFlagBits.isAttached());
            }
            if (nodeFlagBits.isExternal() != null) {
                flags.set(EXTERNAL_BIT, nodeFlagBits.isExternal());
            }
            if (nodeFlagBits.isAbr() != null) {
                flags.set(ABBR_BIT, nodeFlagBits.isAbr());
            }
            nodeFlagBuf.writeBytes(flags.toByteArray());
            writeTLV(TlvCode.NODE_FLAG_BITS, nodeFlagBuf, byteAggregator);
        }
        if (nodeAttributes.getDynamicHostname() != null) {
            writeTLV(TlvCode.DYNAMIC_HOSTNAME, Unpooled.wrappedBuffer(nodeAttributes.getDynamicHostname().getBytes()), byteAggregator);
        }
        if (nodeAttributes.getIsisAreaId() != null) {
            for (final IsisAreaIdentifier isisAreaIdentifier : nodeAttributes.getIsisAreaId()) {
                writeTLV(TlvCode.ISIS_AREA_IDENTIFIER, Unpooled.wrappedBuffer(isisAreaIdentifier.getValue()), byteAggregator);
            }
        }
        if (nodeAttributes.getIpv4RouterId() != null) {
            writeTLV(TlvCode.LOCAL_IPV4_ROUTER_ID, Unpooled.wrappedBuffer(InetAddresses.forString(nodeAttributes.getIpv4RouterId().getValue()).getAddress()), byteAggregator);
        }
        if (nodeAttributes.getIpv6RouterId() != null) {
            writeTLV(TlvCode.LOCAL_IPV6_ROUTER_ID, Unpooled.wrappedBuffer(InetAddresses.forString(nodeAttributes.getIpv6RouterId().getValue()).getAddress()), byteAggregator);
        }
        LOG.trace("Finished serializing Node Attributes");
    }

    private void serializePrefixAttributes(final PrefixAttributesCase prefixAttributesCase, final ByteBuf byteAggregator) {
        final PrefixAttributes prefixAtrributes = prefixAttributesCase.getPrefixAttributes();
        if (prefixAtrributes.getIgpBits() != null) {
            BitSet igpBit = new BitSet();
            Boolean bit = prefixAtrributes.getIgpBits().getUpDown().isUpDown();
            if (bit != null) {
                igpBit.set(7, bit);
            }
            writeTLV(TlvCode.IGP_FLAGS, Unpooled.wrappedBuffer(igpBit.toByteArray()), byteAggregator);
        }
        if (prefixAtrributes.getRouteTags() != null) {
            final ByteBuf routeTagsBuf = Unpooled.buffer();
            for (final RouteTag routeTag : prefixAtrributes.getRouteTags()) {
                routeTagsBuf.writeBytes(routeTag.getValue());
            }
            writeTLV(TlvCode.ROUTE_TAG, routeTagsBuf, byteAggregator);
        }
        if (prefixAtrributes.getExtendedTags() != null) {
            final ByteBuf extendedBuf = Unpooled.buffer();
            for (final ExtendedRouteTag extendedRouteTag : prefixAtrributes.getExtendedTags()) {
                extendedBuf.writeBytes(extendedRouteTag.getValue());
            }
            writeTLV(TlvCode.EXTENDED_ROUTE_TAG, extendedBuf, byteAggregator);
        }
        if (prefixAtrributes.getPrefixMetric() != null) {
            writeTLV(TlvCode.PREFIX_METRIC, Unpooled.copyInt(prefixAtrributes.getPrefixMetric().getValue().intValue()), byteAggregator);
        }
        IpAddress forwardingAddress = prefixAtrributes.getOspfForwardingAddress();
        if (forwardingAddress != null) {
            final ByteBuf ospfBuf = Unpooled.buffer();
            if (forwardingAddress.getIpv4Address() != null) {
                ospfBuf.writeBytes(Ipv4Util.bytesForAddress(forwardingAddress.getIpv4Address()));
            } else if (forwardingAddress.getIpv6Address() != null) {
                ospfBuf.writeBytes(Ipv6Util.bytesForAddress(forwardingAddress.getIpv6Address()));
            }
            writeTLV(TlvCode.FORWARDING_ADDRESS, ospfBuf, byteAggregator);
        }
    }

    /**
     * Util method for writing TLV header.
     * @param type TLV type
     * @param value TLV value
     * @param byteAggregator final ByteBuf where the tlv should be serialized
     */
    private static void writeTLV(final int type, final ByteBuf value, final ByteBuf byteAggregator){
        byteAggregator.writeShort(type);
        byteAggregator.writeShort(value.writerIndex());
        byteAggregator.writeBytes(value);
        value.readerIndex(0);
        LOG.debug("Serialized tlv type {} to: {}", type, ByteBufUtil.hexDump(value));
    }
}
