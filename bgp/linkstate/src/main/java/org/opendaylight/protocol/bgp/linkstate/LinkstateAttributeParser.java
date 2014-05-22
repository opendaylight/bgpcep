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
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.opendaylight.protocol.bgp.concepts.util.NextHopUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.util.ByteArray;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributesBuilder;
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
    public static final int ATTR_FLAGS = 192;

    private static final Logger LOG = LoggerFactory.getLogger(LinkstateAttributeParser.class);

    private static final int ROUTE_TAG_LENGTH = 4;

    private static final int EXTENDED_ROUTE_TAG_LENGTH = 8;

    private static final int SRLG_LENGTH = 4;

    private static final int UNRESERVED_BW_COUNT = 8;

    private static final int BANDWIDTH_LENGTH = 4;
    public static final int MAX_ATTR_LENGTH_FOR_SINGLE_BYTE = 127;
    public static final int LENGTH_IN_TWO_BYTES_BIT = 16;
    public static final int OVERLOAD_BIT = 128;
    public static final int ATTACHED_BIT = 64;
    public static final int EXTERNAL_BIT = 32;
    public static final int ABBR_BIT = 16;

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
                final LinkProtectionType lpt = LinkProtectionType.forValue(UnsignedBytes.toInt(value[0]));
                if (lpt == null) {
                    LOG.warn("Link Protection Type not recognized: {}", UnsignedBytes.toInt(value[0]));
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
                final boolean upDownBit = flags[2];
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
     * @param attribute      DataObject representing LinkstatePathAttribute
     * @param byteAggregator ByteBuf where all serialized data are aggregated
     * @return {@link LinkStateAttribute}
     */

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        if (((PathAttributes) attribute).getAugmentation(PathAttributes1.class) == null) {
            return;
        }
        PathAttributes1 pathAttributes1 = ((PathAttributes) attribute).getAugmentation(PathAttributes1.class);
        LinkstatePathAttribute linkState = pathAttributes1.getLinkstatePathAttribute();

        if (linkState.getLinkStateAttribute() instanceof LinkAttributesCase) {
            serializeLinkAttributes((LinkAttributesCase) linkState.getLinkStateAttribute(), byteAggregator);
        }
        if (linkState.getLinkStateAttribute() instanceof NodeAttributesCase) {
            serializeNodeAttributes((NodeAttributesCase) linkState.getLinkStateAttribute(), byteAggregator);
        }
        if (linkState.getLinkStateAttribute() instanceof PrefixAttributesCase) {
            serializePrefixAttributes((PrefixAttributesCase) linkState.getLinkStateAttribute(), byteAggregator);
        }
    }

    private void serializeLinkAttributes(LinkAttributesCase linkAttributesCase, ByteBuf byteAggregator) {

        LinkAttributes linkAttributes = linkAttributesCase.getLinkAttributes();

        ByteBuf linkStateBuffer = Unpooled.buffer();
        if (linkAttributes.getAdminGroup() != null) {
            ByteBuf adminBuff = Unpooled.buffer();
            adminBuff.writeInt(linkAttributes.getAdminGroup().getValue().intValue());
            NextHopUtil.writeTLV(TlvCode.ADMIN_GROUP, adminBuff, linkStateBuffer);
        }

        if (linkAttributes.getMaxLinkBandwidth() != null) {
            NextHopUtil.writeTLV(TlvCode.MAX_BANDWIDTH, Unpooled.copiedBuffer(linkAttributes.getMaxLinkBandwidth().getValue()), linkStateBuffer);

        }
        if (linkAttributes.getMaxReservableBandwidth() != null) {
            NextHopUtil.writeTLV(TlvCode.MAX_RESERVABLE_BANDWIDTH, Unpooled.copiedBuffer(linkAttributes.getMaxReservableBandwidth().getValue()), linkStateBuffer);
        }

        // this sub-TLV contains eight 32-bit IEEE floating point numbers
        if (linkAttributes.getUnreservedBandwidth() != null) {
            ByteBuf unreservedBandwithBuf = Unpooled.buffer();
            if (linkAttributes.getUnreservedBandwidth() != null) {
                for (UnreservedBandwidth unreservedBandwidth : linkAttributes.getUnreservedBandwidth()) {
                    unreservedBandwithBuf.writeBytes(unreservedBandwidth.getBandwidth().getValue());
                }
            }
            NextHopUtil.writeTLV(TlvCode.UNRESERVED_BANDWIDTH, unreservedBandwithBuf, linkStateBuffer);
        }

        if (linkAttributes.getLinkProtection() != null) {
            ByteBuf linkProtectionTypeBuf = Unpooled.buffer();
            linkProtectionTypeBuf.writeShort(linkAttributes.getLinkProtection().getIntValue());
            NextHopUtil.writeTLV(TlvCode.LINK_PROTECTION_TYPE, linkProtectionTypeBuf, linkStateBuffer);
        }

        if (linkAttributes.getMplsProtocol() != null) {
            ByteBuf mplsProtocolMaskBuf = Unpooled.buffer();
            int bitMask = 0;
            if (linkAttributes.getMplsProtocol().isLdp()) {
                bitMask += 128;
            }
            if (linkAttributes.getMplsProtocol().isRsvpte()) {
                bitMask += 64;
            }
            mplsProtocolMaskBuf.writeByte(bitMask);
            NextHopUtil.writeTLV(TlvCode.MPLS_PROTOCOL, mplsProtocolMaskBuf, linkStateBuffer);
        }
        if (linkAttributes.getMetric() != null) {
            ByteBuf metricBuf = Unpooled.buffer();
            metricBuf.writeMedium(linkAttributes.getMetric().getValue().intValue());
            NextHopUtil.writeTLV(TlvCode.METRIC, metricBuf, linkStateBuffer);
        }
        if (linkAttributes.getSharedRiskLinkGroups() != null) {
            ByteBuf sharedRLGBuf = Unpooled.buffer();
            if (linkAttributes.getSharedRiskLinkGroups() != null) {
                for (SrlgId srlgId : linkAttributes.getSharedRiskLinkGroups()) {
                    sharedRLGBuf.writeInt(srlgId.getValue().intValue());
                }
            }
            NextHopUtil.writeTLV(TlvCode.SHARED_RISK_LINK_GROUP, sharedRLGBuf, linkStateBuffer);
        }

        if (linkAttributes.getLocalIpv4RouterId() != null) {
            ByteBuf localNodeDescBuf = Unpooled.buffer();
            localNodeDescBuf.writeBytes(linkAttributes.getLocalIpv4RouterId().getValue().getBytes());
            NextHopUtil.writeTLV(TlvCode.LOCAL_IPV4_ROUTER_ID, localNodeDescBuf, linkStateBuffer);
        }

        if (linkAttributes.getLocalIpv6RouterId() != null) {
            ByteBuf localNodeDescBuf = Unpooled.buffer();
            localNodeDescBuf.writeBytes(linkAttributes.getLocalIpv6RouterId().getValue().getBytes());
            NextHopUtil.writeTLV(TlvCode.LOCAL_IPV6_ROUTER_ID, localNodeDescBuf, linkStateBuffer);
        }

        if (linkAttributes.getRemoteIpv4RouterId() != null) {
            ByteBuf remoteNodeDescBuf = Unpooled.buffer();
            remoteNodeDescBuf.writeBytes(linkAttributes.getRemoteIpv4RouterId().getValue().getBytes());
            NextHopUtil.writeTLV(TlvCode.REMOTE_IPV4_ROUTER_ID, remoteNodeDescBuf, linkStateBuffer);
        }
        if (linkAttributes.getRemoteIpv6RouterId() != null) {
            ByteBuf remoteNodeDescBuf = Unpooled.buffer();
            remoteNodeDescBuf.writeBytes(linkAttributes.getRemoteIpv6RouterId().getValue().getBytes());
            NextHopUtil.writeTLV(TlvCode.REMOTE_IPV6_ROUTER_ID, remoteNodeDescBuf, linkStateBuffer);
        }

        if (linkStateBuffer.writerIndex() > MAX_ATTR_LENGTH_FOR_SINGLE_BYTE) {
            byteAggregator.writeByte(ATTR_FLAGS + LENGTH_IN_TWO_BYTES_BIT);
            byteAggregator.writeByte(TYPE);
            byteAggregator.writeShort(linkStateBuffer.writerIndex());
        } else {
            byteAggregator.writeByte(ATTR_FLAGS);
            byteAggregator.writeByte(TYPE);
            byteAggregator.writeByte(linkStateBuffer.writerIndex());
        }
        byteAggregator.writeBytes(linkStateBuffer);
    }

    private void serializeNodeAttributes(NodeAttributesCase nodeAttributesCase, ByteBuf byteAggregator) {
        if (nodeAttributesCase.getNodeAttributes().getTopologyIdentifier() != null) {
            ByteBuf mpIdBuf = Unpooled.buffer();
            if (nodeAttributesCase.getNodeAttributes().getTopologyIdentifier() != null) {
                for (TopologyIdentifier topologyIdentifier : nodeAttributesCase.getNodeAttributes().getTopologyIdentifier()) {
                    mpIdBuf.writeShort(topologyIdentifier.getValue());
                }
            }
            NextHopUtil.writeTLV(TlvCode.MULTI_TOPOLOGY_ID, mpIdBuf, byteAggregator);
        }

        if (nodeAttributesCase.getNodeAttributes().getNodeFlags() != null) {
            ByteBuf nodeFlagBuf = Unpooled.buffer();
            int otea = 0;
            if (nodeAttributesCase.getNodeAttributes().getNodeFlags().isOverload()) {
                otea += OVERLOAD_BIT;
            }
            if (nodeAttributesCase.getNodeAttributes().getNodeFlags().isAttached()) {
                otea += ATTACHED_BIT;
            }
            if (nodeAttributesCase.getNodeAttributes().getNodeFlags().isExternal()) {
                otea += EXTERNAL_BIT;
            }
            if (nodeAttributesCase.getNodeAttributes().getNodeFlags().isAbr()) {
                otea += ABBR_BIT;
            }
            nodeFlagBuf.writeByte(otea);
            NextHopUtil.writeTLV(TlvCode.NODE_FLAG_BITS, nodeFlagBuf, byteAggregator);
        }

        if (nodeAttributesCase.getNodeAttributes().getDynamicHostname() != null) {

        }

        if (nodeAttributesCase.getNodeAttributes().getIsisAreaId() != null) {
            ByteBuf isIsBuf = Unpooled.buffer();
            if (nodeAttributesCase.getNodeAttributes().getIsisAreaId() != null) {
                for (IsisAreaIdentifier isisAreaIdentifier : nodeAttributesCase.getNodeAttributes().getIsisAreaId()) {
                    isIsBuf.writeBytes(isisAreaIdentifier.getValue());
                }
            }
            NextHopUtil.writeTLV(TlvCode.ISIS_AREA_IDENTIFIER, isIsBuf, byteAggregator);
        }
        if (nodeAttributesCase.getNodeAttributes().getIpv4RouterId() != null) {
            ByteBuf routerIdBuf = Unpooled.buffer();
            routerIdBuf.writeBytes(nodeAttributesCase.getNodeAttributes().getIpv4RouterId().getValue().getBytes());
            NextHopUtil.writeTLV(TlvCode.LOCAL_IPV4_ROUTER_ID, routerIdBuf, byteAggregator);
        }

        if (nodeAttributesCase.getNodeAttributes().getIpv6RouterId() != null) {
            ByteBuf routerIdBuf = Unpooled.buffer();
            routerIdBuf.writeBytes(nodeAttributesCase.getNodeAttributes().getIpv6RouterId().getValue().getBytes());
            NextHopUtil.writeTLV(TlvCode.LOCAL_IPV6_ROUTER_ID, routerIdBuf, byteAggregator);
        }
    }

    private void serializePrefixAttributes(PrefixAttributesCase prefixAttributesCase, ByteBuf byteAggregator) {
        if (prefixAttributesCase.getPrefixAttributes().getIgpBits() != null) {
            int dBit = prefixAttributesCase.getPrefixAttributes().getIgpBits().getUpDown().isUpDown() ? 128 : 0;
            ByteBuf igpBuf = Unpooled.buffer();
            igpBuf.writeByte(dBit);
            NextHopUtil.writeTLV(TlvCode.IGP_FLAGS, igpBuf, byteAggregator);
        }
        if (prefixAttributesCase.getPrefixAttributes().getRouteTags() != null) {
            ByteBuf routeTagsBuf = Unpooled.buffer();
            if (prefixAttributesCase.getPrefixAttributes().getRouteTags() != null) {
                for (RouteTag routeTag : prefixAttributesCase.getPrefixAttributes().getRouteTags()) {
                    routeTagsBuf.writeBytes(routeTag.getValue());
                }
            }
            NextHopUtil.writeTLV(TlvCode.ROUTE_TAG, routeTagsBuf, byteAggregator);
        }
        if (prefixAttributesCase.getPrefixAttributes().getExtendedTags() != null) {
            ByteBuf extendedBuf = Unpooled.buffer();
            if (prefixAttributesCase.getPrefixAttributes().getExtendedTags() != null) {
                for (ExtendedRouteTag extendedRouteTag :
                        prefixAttributesCase.getPrefixAttributes().getExtendedTags()) {
                    extendedBuf.writeBytes(extendedRouteTag.getValue());
                }
            }
            NextHopUtil.writeTLV(TlvCode.EXTENDED_ROUTE_TAG, extendedBuf, byteAggregator);
        }

        if (prefixAttributesCase.getPrefixAttributes().getPrefixMetric() != null) {
            ByteBuf prefixMetrixBuf = Unpooled.buffer();
            prefixMetrixBuf.writeInt(prefixAttributesCase.getPrefixAttributes().getPrefixMetric().getValue().intValue());
            NextHopUtil.writeTLV(TlvCode.PREFIX_METRIC, prefixMetrixBuf, byteAggregator);
        }

        if (prefixAttributesCase.getPrefixAttributes().getOspfForwardingAddress() != null) {
            ByteBuf ospfBuf = Unpooled.buffer();
            if (prefixAttributesCase.getPrefixAttributes().getOspfForwardingAddress().getIpv4Address() != null) {
                ospfBuf.writeBytes(Ipv4Util.bytesForAddress(prefixAttributesCase.getPrefixAttributes().getOspfForwardingAddress().getIpv4Address()));
            }
            if (prefixAttributesCase.getPrefixAttributes().getOspfForwardingAddress().getIpv6Address() != null) {
                ospfBuf.writeBytes(Ipv6Util.bytesForAddress(prefixAttributesCase.getPrefixAttributes().getOspfForwardingAddress().getIpv6Address()));
            }
            NextHopUtil.writeTLV(TlvCode.FORWARDING_ADDRESS, ospfBuf, byteAggregator);
        }
    }
}
