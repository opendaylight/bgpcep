/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SRv6AttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrFlexAlgoParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrPrefixAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrRangeParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ExtendedRouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.RouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.PrefixAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.AttributeFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.AttributeFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.FlexAlgoPrefixMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.IgpBitsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.SrRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.Srv6Locator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.prefix.attribute.flags.igp.attribute.flags.IsisAttributeFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.prefix.attribute.flags.igp.attribute.flags.IsisAttributeFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.prefix.attribute.flags.igp.attribute.flags.OspfAttributeFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.prefix.attribute.flags.igp.attribute.flags.OspfAttributeFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.prefix.attribute.flags.igp.attribute.flags.Ospfv3AttributeFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.prefix.attribute.flags.igp.attribute.flags.Ospfv3AttributeFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IgpMetric;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class PrefixAttributesParser {
    private static final Logger LOG = LoggerFactory.getLogger(PrefixAttributesParser.class);

    /* Segment routing TLV */
    private static final int ROUTE_TAG_LENGTH = 4;
    private static final int EXTENDED_ROUTE_TAG_LENGTH = 8;
    private static final int FLAGS_SIZE = 8;
    private static final int UP_DOWN_BIT = 0;
    private static final int OSPF_NO_UNICAST = 1;
    private static final int OSPF_LOCAL_ADDRESS = 2;
    private static final int OSPF_PROPAGATE_ADDRESS = 3;

    /* Prefix Attribute Flags */
    private static final int ISIS_EXTERNAL_FLAG = 0;
    private static final int ISIS_RE_ADVERTISEMENT_FLAG = 1;
    private static final int ISIS_NODE_FLAG = 2;
    private static final int OSPF_ATTACH_FLAG = 0;
    private static final int OSPF_NODE_FLAG = 1;
    private static final int OSPFV3_NODE_FLAG = 2;
    private static final int OSPFV3_DN_FLAG = 3;
    private static final int OSPFV3_PROPAGATE_FLAG = 4;
    private static final int OSPFV3_LOCAL_ADDRESS_FLAG = 6;
    private static final int OSPFV3_NO_UNICAST_FLAG = 7;

    /* Prefix Attribute TLVs */
    private static final int FLEX_ALGO_PREFIX_METRIC = 1044;
    private static final int IGP_FLAGS = 1152;
    private static final int ROUTE_TAG = 1153;
    private static final int EXTENDED_ROUTE_TAG = 1154;
    private static final int PREFIX_METRIC = 1155;
    private static final int FORWARDING_ADDRESS = 1156;
    private static final int PREFIX_OPAQUE = 1157;
    public static final int PREFIX_SID = 1158;
    private static final int RANGE = 1159;
    private static final int SRV6_LOCATOR = 1162;
    private static final int PREFIX_ATTRIBUTE_FLAGS = 1170;
    private static final int SOURCE_ROUTER_ID = 1171;
    private static final int SOURCE_OSPF_ROUTER_ID = 1174;

    private PrefixAttributesParser() {
        // Hidden on purpose
    }

    /**
     * Parse prefix attributes.
     *
     * @param attributes key is the tlv type and value are the value bytes of the tlv
     * @param protocolId to differentiate parsing methods
     * @return {@link LinkStateAttribute}
     */
    static LinkStateAttribute parsePrefixAttributes(final Multimap<Integer, ByteBuf> attributes,
            final ProtocolId protocolId) {
        final PrefixAttributesBuilder builder = new PrefixAttributesBuilder();
        final var routeTags = new ArrayList<RouteTag>();
        final var exRouteTags = new ArrayList<ExtendedRouteTag>();
        for (var entry : attributes.entries()) {
            final int key = entry.getKey();
            final ByteBuf value = entry.getValue();
            LOG.trace("Prefix attribute TLV {}", key);
            parseAttribute(key, value, protocolId, builder, routeTags, exRouteTags);
        }
        LOG.trace("Finished parsing Prefix Attributes.");
        builder.setRouteTags(ImmutableSet.copyOf(routeTags));
        builder.setExtendedTags(ImmutableSet.copyOf(exRouteTags));
        return new PrefixAttributesCaseBuilder().setPrefixAttributes(builder.build()).build();
    }

    private static void parseAttribute(final int key, final ByteBuf value, final ProtocolId protocolId,
            final PrefixAttributesBuilder builder, final List<RouteTag> routeTags,
            final List<ExtendedRouteTag> exRouteTags) {
        switch (key) {
            case IGP_FLAGS -> parseIgpFlags(builder, value);
            case ROUTE_TAG -> parseRouteTags(routeTags, value);
            case EXTENDED_ROUTE_TAG -> parseExtendedRouteTags(exRouteTags, value);
            case PREFIX_METRIC -> {
                final var metric = new IgpMetric(ByteBufUtils.readUint32(value));
                LOG.debug("Parsed Metric: {}", metric);
                builder.setPrefixMetric(metric);
            }
            case FORWARDING_ADDRESS -> {
                final var fwdAddress = parseForwardingAddress(value);
                LOG.debug("Parsed FWD Address: {}", fwdAddress);
                builder.setOspfForwardingAddress(fwdAddress);
            }
            case PREFIX_OPAQUE -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Parsed Opaque value: {}, not preserving it", ByteBufUtil.hexDump(value));
                }
            }
            case PREFIX_SID -> {
                final var prefix = SrPrefixAttributesParser.parseSrPrefix(value, protocolId);
                builder.setSrPrefix(prefix);
                LOG.debug("Parsed SR Prefix: {}", prefix);
            }
            case RANGE -> {
                final var range = SrRangeParser.parseSrRange(value, protocolId);
                LOG.debug("Parsed SR Range: {}", range);
                builder.setSrRange(range);
            }
            case FLEX_ALGO_PREFIX_METRIC -> {
                final var fapm = SrFlexAlgoParser.parseFlexAlgoPrefixMetric(value, protocolId);
                LOG.debug("Parsed Flex Algo Prefix Metric {}", fapm);
                builder.setFlexAlgoPrefixMetric(fapm);
            }
            case SRV6_LOCATOR -> {
                final var srv6Locator = SRv6AttributesParser.parseSrv6Locator(value);
                LOG.debug("Parsed SRv6 Locator {}", srv6Locator);
                builder.setSrv6Locator(srv6Locator);
            }
            case PREFIX_ATTRIBUTE_FLAGS -> {
                final var flags = parseAttributeFlags(value, protocolId);
                LOG.debug("Parsed Attribute Flags {}", flags);
                builder.setAttributeFlags(flags);
            }
            case SOURCE_ROUTER_ID -> {
                final var sourceRouterID = parseForwardingAddress(value);
                LOG.debug("Parsed Source Router ID {}", sourceRouterID);
                builder.setSourceRouterId(sourceRouterID);
            }
            case SOURCE_OSPF_ROUTER_ID -> {
                final var ori = new Ipv4AddressNoZone(Ipv4Util.addressForByteBuf(value));
                LOG.debug("Parsed OSPF Source Router ID {}", ori);
                builder.setSourceOspfRouterId(ori);
            }
            default ->LOG.warn("TLV {} is not a valid prefix attribute, ignoring it", key);
        }
    }

    private static void parseIgpFlags(final PrefixAttributesBuilder builder, final ByteBuf value) {
        final var flags = BitArray.valueOf(value, FLAGS_SIZE);
        final boolean upDownBit = flags.get(UP_DOWN_BIT);
        LOG.debug("Parsed IGP flag (up/down bit) : {}", upDownBit);
        builder.setIgpBits(new IgpBitsBuilder()
            .setIsIsUpDown(upDownBit)
            .setOspfNoUnicast(flags.get(OSPF_NO_UNICAST))
            .setOspfLocalAddress(flags.get(OSPF_LOCAL_ADDRESS))
            .setOspfPropagateNssa(flags.get(OSPF_PROPAGATE_ADDRESS))
            .build());
    }

    private static void parseRouteTags(final List<RouteTag> routeTags, final ByteBuf value) {
        while (value.isReadable()) {
            final var routeTag = new RouteTag(ByteArray.readBytes(value, ROUTE_TAG_LENGTH));
            routeTags.add(routeTag);
            LOG.debug("Parsed Route Tag: {}", routeTag);
        }
    }

    private static void parseExtendedRouteTags(final List<ExtendedRouteTag> exRouteTags, final ByteBuf value) {
        while (value.isReadable()) {
            final var exRouteTag = new ExtendedRouteTag(ByteArray.readBytes(value, EXTENDED_ROUTE_TAG_LENGTH));
            LOG.debug("Parsed Extended Route Tag: {}", exRouteTag);
            exRouteTags.add(exRouteTag);
        }
    }

    private static IpAddressNoZone parseForwardingAddress(final ByteBuf value) {
        return switch (value.readableBytes()) {
            case Ipv4Util.IP4_LENGTH -> new IpAddressNoZone(Ipv4Util.addressForByteBuf(value));
            case Ipv6Util.IPV6_LENGTH -> new IpAddressNoZone(Ipv6Util.addressForByteBuf(value));
            default -> {
                LOG.debug("Ignoring unsupported forwarding address length {}", value.readableBytes());
                yield null;
            }
        };
    }

    private static AttributeFlags parseAttributeFlags(final ByteBuf value, final ProtocolId protocolId) {
        final var flags = BitArray.valueOf(value, FLAGS_SIZE);
        final var afBuilder = new AttributeFlagsBuilder();
        switch (protocolId) {
            case IsisLevel1, IsisLevel2 -> {
                afBuilder.setNodeFlag(flags.get(ISIS_NODE_FLAG))
                    .setIgpAttributeFlags(new IsisAttributeFlagsCaseBuilder()
                        .setExternalFlag(flags.get(ISIS_EXTERNAL_FLAG))
                        .setReAdvertisementFlag(flags.get(ISIS_RE_ADVERTISEMENT_FLAG))
                        .build());
            }
            case Ospf -> {
                afBuilder.setNodeFlag(flags.get(OSPF_NODE_FLAG))
                    .setIgpAttributeFlags(new OspfAttributeFlagsCaseBuilder()
                        .setAttachFlag(flags.get(OSPF_ATTACH_FLAG))
                        .build());
            }
            case OspfV3 -> {
                afBuilder.setNodeFlag(flags.get(OSPFV3_NODE_FLAG))
                    .setIgpAttributeFlags(new Ospfv3AttributeFlagsCaseBuilder()
                        .setDnFlag(flags.get(OSPFV3_DN_FLAG))
                        .setLocalAddressFlag(flags.get(OSPFV3_LOCAL_ADDRESS_FLAG))
                        .setPropagateFlag(flags.get(OSPFV3_PROPAGATE_FLAG))
                        .setNoUnicastFlag(flags.get(OSPFV3_NO_UNICAST_FLAG))
                        .build());
            }
            default -> {
                return null;
            }
        }
        return afBuilder.build();
    }

    static void serializePrefixAttributes(final PrefixAttributesCase prefixAttributesCase,
            final ByteBuf byteAggregator) {
        final var prefixAttributes = prefixAttributesCase.getPrefixAttributes();
        final var igpBits = prefixAttributes.getIgpBits();
        if (igpBits != null) {
            final var igpBit = new BitArray(FLAGS_SIZE);
            igpBit.set(UP_DOWN_BIT, igpBits.getIsIsUpDown());
            igpBit.set(OSPF_NO_UNICAST, igpBits.getOspfNoUnicast());
            igpBit.set(OSPF_LOCAL_ADDRESS, igpBits.getOspfLocalAddress());
            igpBit.set(OSPF_PROPAGATE_ADDRESS, igpBits.getOspfPropagateNssa());
            TlvUtil.writeTLV(IGP_FLAGS, Unpooled.wrappedBuffer(igpBit.array()), byteAggregator);
        }
        serializeRouteTags(prefixAttributes.getRouteTags(), byteAggregator);
        serializeExtendedRouteTags(prefixAttributes.getExtendedTags(), byteAggregator);
        serializePrefixMetric(prefixAttributes.getPrefixMetric(), byteAggregator);
        serializeForwardingAddress(prefixAttributes.getOspfForwardingAddress(), byteAggregator);
        serializeSrPrefix(prefixAttributes.getSrPrefix(), byteAggregator);
        serializeSrRange(prefixAttributes.getSrRange(), byteAggregator);
        serializeFlexAlgoPrefixMetric(prefixAttributes.getFlexAlgoPrefixMetric(), byteAggregator);
        serializeSrv6Locator(prefixAttributes.getSrv6Locator(), byteAggregator);
        serializeAttributeFlags(prefixAttributes.getAttributeFlags(), byteAggregator);
        serializeSourceRouterId(prefixAttributes.getSourceRouterId(), byteAggregator);
        serializeSourceOspfRouterId(prefixAttributes.getSourceOspfRouterId(), byteAggregator);
    }

    private static void serializeSrRange(final SrRange srRange, final ByteBuf byteAggregator) {
        if (srRange != null) {
            final var sidBuffer = Unpooled.buffer();
            SrRangeParser.serializeSrRange(srRange, sidBuffer);
            TlvUtil.writeTLV(RANGE, sidBuffer, byteAggregator);
        }
    }

    private static void serializeSrPrefix(final SrPrefix srPrefix, final ByteBuf byteAggregator) {
        if (srPrefix != null) {
            final ByteBuf buffer = Unpooled.buffer();
            SrPrefixAttributesParser.serializeSrPrefix(srPrefix, buffer);
            TlvUtil.writeTLV(PREFIX_SID, buffer, byteAggregator);
        }
    }

    private static void serializeFlexAlgoPrefixMetric(final FlexAlgoPrefixMetric fapm, final ByteBuf byteAggregator) {
        if (fapm != null) {
            final var buffer = Unpooled.buffer();
            SrFlexAlgoParser.serializeFlexAlgoPrefixMetric(fapm, buffer);
            TlvUtil.writeTLV(FLEX_ALGO_PREFIX_METRIC, buffer, byteAggregator);
        }
    }

    private static void serializeSrv6Locator(final Srv6Locator srv6Locator, final ByteBuf byteAggregator) {
        if (srv6Locator != null) {
            final var buffer = Unpooled.buffer();
            SRv6AttributesParser.serializeSrv6Locator(srv6Locator, buffer);
            TlvUtil.writeTLV(SRV6_LOCATOR, buffer, byteAggregator);
        }
    }

    private static void serializePrefixMetric(final IgpMetric prefixMetric, final ByteBuf byteAggregator) {
        if (prefixMetric != null) {
            TlvUtil.writeTLV(PREFIX_METRIC, Unpooled.copyInt(prefixMetric.getValue().intValue()), byteAggregator);
        }
    }

    private static void serializeRouteTags(final Set<RouteTag> routeTags, final ByteBuf byteAggregator) {
        if (routeTags != null) {
            final var routeTagsBuf = Unpooled.buffer();
            for (var routeTag : routeTags) {
                routeTagsBuf.writeBytes(routeTag.getValue());
            }
            TlvUtil.writeTLV(ROUTE_TAG, routeTagsBuf, byteAggregator);
        }
    }

    private static void serializeExtendedRouteTags(final Set<ExtendedRouteTag> exRouteTags,
            final ByteBuf byteAggregator) {
        if (exRouteTags != null) {
            final var extendedBuf = Unpooled.buffer();
            for (var exRouteTag : exRouteTags) {
                extendedBuf.writeBytes(exRouteTag.getValue());
            }
            TlvUtil.writeTLV(EXTENDED_ROUTE_TAG, extendedBuf, byteAggregator);
        }
    }

    private static void serializeForwardingAddress(final IpAddressNoZone forwardingAddress,
            final ByteBuf byteAggregator) {
        if (forwardingAddress != null) {
            final var ospfBuf = Unpooled.buffer();
            if (forwardingAddress.getIpv4AddressNoZone() != null) {
                ospfBuf.writeBytes(Ipv4Util.bytesForAddress(forwardingAddress.getIpv4AddressNoZone()));
            } else if (forwardingAddress.getIpv6AddressNoZone() != null) {
                ospfBuf.writeBytes(Ipv6Util.bytesForAddress(forwardingAddress.getIpv6AddressNoZone()));
            }
            TlvUtil.writeTLV(FORWARDING_ADDRESS, ospfBuf, byteAggregator);
        }
    }

    private static void serializeAttributeFlags(final AttributeFlags af, final ByteBuf byteAggregator) {
        if (af != null) {
            final BitArray afBit = new BitArray(FLAGS_SIZE);
            final var flags = af.getIgpAttributeFlags();
            if (flags instanceof IsisAttributeFlagsCase isis) {
                afBit.set(ISIS_NODE_FLAG, af.getNodeFlag());
                afBit.set(ISIS_EXTERNAL_FLAG, isis.getExternalFlag());
                afBit.set(ISIS_RE_ADVERTISEMENT_FLAG, isis.getReAdvertisementFlag());
            } else if (flags instanceof OspfAttributeFlagsCase ospf) {
                afBit.set(OSPF_NODE_FLAG, af.getNodeFlag());
                afBit.set(OSPF_ATTACH_FLAG, ospf.getAttachFlag());
            } else if (af.getIgpAttributeFlags() instanceof Ospfv3AttributeFlagsCase ospf3) {
                afBit.set(OSPFV3_NODE_FLAG, af.getNodeFlag());
                afBit.set(OSPFV3_DN_FLAG, ospf3.getDnFlag());
                afBit.set(OSPFV3_LOCAL_ADDRESS_FLAG, ospf3.getLocalAddressFlag());
                afBit.set(OSPFV3_PROPAGATE_FLAG, ospf3.getPropagateFlag());
                afBit.set(OSPFV3_NO_UNICAST_FLAG, ospf3.getNoUnicastFlag());
            } else {
                return;
            }
            TlvUtil.writeTLV(PREFIX_ATTRIBUTE_FLAGS, Unpooled.wrappedBuffer(afBit.array()), byteAggregator);
        }
    }

    private static void serializeSourceRouterId(final IpAddressNoZone sourceRouterId, final ByteBuf byteAggregator) {
        if (sourceRouterId != null) {
            final ByteBuf ipBuf = Unpooled.buffer();
            if (sourceRouterId.getIpv4AddressNoZone() != null) {
                ipBuf.writeBytes(Ipv4Util.bytesForAddress(sourceRouterId.getIpv4AddressNoZone()));
            } else if (sourceRouterId.getIpv6AddressNoZone() != null) {
                ipBuf.writeBytes(Ipv6Util.bytesForAddress(sourceRouterId.getIpv6AddressNoZone()));
            }
            TlvUtil.writeTLV(SOURCE_ROUTER_ID, ipBuf, byteAggregator);
        }
    }

    private static void serializeSourceOspfRouterId(final Ipv4AddressNoZone sourceOspfRouterId,
            final ByteBuf byteAggregator) {
        if (sourceOspfRouterId != null) {
            final ByteBuf ospfBuf = Unpooled.buffer();
            ospfBuf.writeBytes(Ipv4Util.bytesForAddress(sourceOspfRouterId));
            TlvUtil.writeTLV(SOURCE_OSPF_ROUTER_ID, ospfBuf, byteAggregator);
        }
    }
}
