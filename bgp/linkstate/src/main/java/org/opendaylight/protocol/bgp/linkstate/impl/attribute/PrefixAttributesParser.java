/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute;

import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv4PrefixSidParser.PREFIX_SID;
import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv6PrefixSidParser.IPV6_PREFIX_SID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.BindingSidLabelParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.Ipv6SrPrefixAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.RangeTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrPrefixAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ExtendedRouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.IgpBits.UpDown;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.RouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.PrefixAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.IgpBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.IgpBitsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.Ipv6SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.SrBindingSidLabels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.SrRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IgpMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class PrefixAttributesParser {

    /* Segment routing TLV */
    public static final int BINDING_SID = 1160;
    private static final Logger LOG = LoggerFactory.getLogger(PrefixAttributesParser.class);
    private static final int ROUTE_TAG_LENGTH = 4;
    private static final int EXTENDED_ROUTE_TAG_LENGTH = 8;
    private static final int FLAGS_SIZE = 8;
    private static final int UP_DOWN_BIT = 0;
    private static final int OSPF_NO_UNICAST = 1;
    private static final int OSPF_LOCAL_ADDRESS = 2;
    private static final int OSPF_PROPAGATE_ADDRESS = 3;
    /* Prefix Attribute TLVs */
    private static final int IGP_FLAGS = 1152;
    private static final int ROUTE_TAG = 1153;
    private static final int EXTENDED_ROUTE_TAG = 1154;
    private static final int PREFIX_METRIC = 1155;
    private static final int FORWARDING_ADDRESS = 1156;
    private static final int PREFIX_OPAQUE = 1157;
    private static final int RANGE = 1159;

    private PrefixAttributesParser() {
        throw new UnsupportedOperationException();
    }

    /**
     * Parse prefix attributes.
     *
     * @param attributes key is the tlv type and value are the value bytes of the tlv
     * @param protocolId to differentiate parsing methods
     * @return {@link LinkStateAttribute}
     */
    static LinkStateAttribute parsePrefixAttributes(final Multimap<Integer, ByteBuf> attributes, final ProtocolId protocolId) {
        final PrefixAttributesBuilder builder = new PrefixAttributesBuilder();
        final List<RouteTag> routeTags = new ArrayList<>();
        final List<ExtendedRouteTag> exRouteTags = new ArrayList<>();
        for (final Entry<Integer, ByteBuf> entry : attributes.entries()) {
            final int key = entry.getKey();
            final ByteBuf value = entry.getValue();
            LOG.trace("Prefix attribute TLV {}", key);
            parseAttribute(key, value, protocolId, builder, routeTags, exRouteTags);
        }
        LOG.trace("Finished parsing Prefix Attributes.");
        builder.setRouteTags(routeTags);
        builder.setExtendedTags(exRouteTags);
        return new PrefixAttributesCaseBuilder().setPrefixAttributes(builder.build()).build();
    }

    private static void parseAttribute(final int key, final ByteBuf value, final ProtocolId protocolId, final PrefixAttributesBuilder builder, final List<RouteTag> routeTags, final List<ExtendedRouteTag> exRouteTags) {
        switch (key) {
        case IGP_FLAGS:
            parseIgpFags(builder, value);
            break;
        case ROUTE_TAG:
            parseRouteTags(routeTags, value);
            break;
        case EXTENDED_ROUTE_TAG:
            parseExtendedRouteTags(exRouteTags, value);
            break;
        case PREFIX_METRIC:
            final IgpMetric metric = new IgpMetric(value.readUnsignedInt());
            builder.setPrefixMetric(metric);
            LOG.debug("Parsed Metric: {}", metric);
            break;
        case FORWARDING_ADDRESS:
            final IpAddress fwdAddress = parseForwardingAddress(value);
            builder.setOspfForwardingAddress(fwdAddress);
            LOG.debug("Parsed FWD Address: {}", fwdAddress);
            break;
        case PREFIX_OPAQUE:
            if (LOG.isDebugEnabled()) {
                LOG.debug("Parsed Opaque value: {}, not preserving it", ByteBufUtil.hexDump(value));
            }
            break;
        case PREFIX_SID:
            final SrPrefix prefix = SrPrefixAttributesParser.parseSrPrefix(value, protocolId);
            builder.setSrPrefix(prefix);
            LOG.debug("Parsed SR Prefix: {}", prefix);
            break;
        case IPV6_PREFIX_SID:
            final Ipv6SrPrefix ipv6Prefix = Ipv6SrPrefixAttributesParser.parseSrIpv6Prefix(value);
            builder.setIpv6SrPrefix(ipv6Prefix);
            LOG.debug("Parsed Ipv6 SR Prefix: {}", ipv6Prefix);
            break;
        case RANGE:
            final SrRange range = RangeTlvParser.parseSrRange(value, protocolId);
            builder.setSrRange(range);
            LOG.debug("Parsed SR Range: {}", range);
            break;
        case BINDING_SID:
            parseBindingSid(builder, value, protocolId);
            break;
        default:
            LOG.warn("TLV {} is not a valid prefix attribute, ignoring it", key);
        }
    }

    private static void parseIgpFags(final PrefixAttributesBuilder builder, final ByteBuf value) {
        final BitArray flags = BitArray.valueOf(value, FLAGS_SIZE);
        final boolean upDownBit = flags.get(UP_DOWN_BIT);
        builder.setIgpBits(new IgpBitsBuilder().setUpDown(new UpDown(upDownBit)).setIsIsUpDown(upDownBit).setOspfNoUnicast(flags.get(OSPF_NO_UNICAST))
            .setOspfLocalAddress(flags.get(OSPF_LOCAL_ADDRESS)).setOspfPropagateNssa(flags.get(OSPF_PROPAGATE_ADDRESS)).build());
        LOG.debug("Parsed IGP flag (up/down bit) : {}", upDownBit);
    }

    private static void parseBindingSid(final PrefixAttributesBuilder builder, final ByteBuf value, final ProtocolId protocolId) {
        final List<SrBindingSidLabels> labels;
        if (builder.getSrBindingSidLabels() != null) {
            labels = builder.getSrBindingSidLabels();
        } else {
            labels = new ArrayList<>();
            builder.setSrBindingSidLabels(labels);
        }
        final SrBindingSidLabels label = BindingSidLabelParser.parseBindingSidLabel(value, protocolId);
        labels.add(label);
        LOG.debug("Parsed SR Binding SID {}", label);
    }

    private static void parseRouteTags(final List<RouteTag> routeTags, final ByteBuf value) {
        while (value.isReadable()) {
            final RouteTag routeTag = new RouteTag(ByteArray.readBytes(value, ROUTE_TAG_LENGTH));
            routeTags.add(routeTag);
            LOG.debug("Parsed Route Tag: {}", routeTag);
        }
    }

    private static void parseExtendedRouteTags(final List<ExtendedRouteTag> exRouteTags, final ByteBuf value) {
        while (value.isReadable()) {
            final ExtendedRouteTag exRouteTag = new ExtendedRouteTag(ByteArray.readBytes(value, EXTENDED_ROUTE_TAG_LENGTH));
            exRouteTags.add(exRouteTag);
            LOG.debug("Parsed Extended Route Tag: {}", exRouteTag);
        }
    }

    private static IpAddress parseForwardingAddress(final ByteBuf value) {
        IpAddress fwdAddress = null;
        switch (value.readableBytes()) {
        case Ipv4Util.IP4_LENGTH:
            fwdAddress = new IpAddress(Ipv4Util.addressForByteBuf(value));
            break;
        case Ipv6Util.IPV6_LENGTH:
            fwdAddress = new IpAddress(Ipv6Util.addressForByteBuf(value));
            break;
        default:
            LOG.debug("Ignoring unsupported forwarding address length {}", value.readableBytes());
        }
        return fwdAddress;
    }

    static void serializePrefixAttributes(final PrefixAttributesCase prefixAttributesCase, final ByteBuf byteAggregator) {
        final PrefixAttributes prefixAtrributes = prefixAttributesCase.getPrefixAttributes();
        if (prefixAtrributes.getIgpBits() != null) {
            final BitArray igpBit = new BitArray(FLAGS_SIZE);
            final IgpBits igpBits = prefixAtrributes.getIgpBits();
            igpBit.set(UP_DOWN_BIT, igpBits.getUpDown().isUpDown() || igpBits.isIsIsUpDown());
            igpBit.set(OSPF_NO_UNICAST, igpBits.isOspfNoUnicast());
            igpBit.set(OSPF_LOCAL_ADDRESS, igpBits.isOspfLocalAddress());
            igpBit.set(OSPF_PROPAGATE_ADDRESS, igpBits.isOspfPropagateNssa());
            TlvUtil.writeTLV(IGP_FLAGS, Unpooled.wrappedBuffer(igpBit.array()), byteAggregator);
        }
        serializeRouteTags(prefixAtrributes.getRouteTags(), byteAggregator);
        serializeExtendedRouteTags(prefixAtrributes.getExtendedTags(), byteAggregator);
        serializePrefixMetric(prefixAtrributes.getPrefixMetric(), byteAggregator);
        serializeForwardingAddress(prefixAtrributes.getOspfForwardingAddress(), byteAggregator);
        serializeSrPrefix(prefixAtrributes.getSrPrefix(), byteAggregator);
        serializeIpv6SrPrefix(prefixAtrributes.getIpv6SrPrefix(), byteAggregator);
        serializeSrRange(prefixAtrributes.getSrRange(), byteAggregator);
        serializeSrBindingLabel(prefixAtrributes.getSrBindingSidLabels(), byteAggregator);
    }

    private static void serializeSrBindingLabel(final List<SrBindingSidLabels> srBindingSidLabels, final ByteBuf byteAggregator) {
        if (srBindingSidLabels != null) {
            for (final SrBindingSidLabels bindingSid : srBindingSidLabels) {
                final ByteBuf sidBuffer = Unpooled.buffer();
                BindingSidLabelParser.serializeBindingSidAttributes(bindingSid.getWeight(), bindingSid.getFlags(), bindingSid.getBindingSubTlvs(), sidBuffer);
                TlvUtil.writeTLV(PrefixAttributesParser.BINDING_SID, sidBuffer, byteAggregator);
            }
        }
    }

    private static void serializeSrRange(final SrRange srRange, final ByteBuf byteAggregator) {
        if (srRange != null) {
            final ByteBuf sidBuffer = Unpooled.buffer();
            RangeTlvParser.serializeSrRange(srRange, sidBuffer);
            TlvUtil.writeTLV(RANGE, sidBuffer, byteAggregator);
        }
    }

    private static void serializeIpv6SrPrefix(final Ipv6SrPrefix ipv6SrPrefix, final ByteBuf byteAggregator) {
        if (ipv6SrPrefix != null) {
            final ByteBuf buffer = Unpooled.buffer();
            Ipv6SrPrefixAttributesParser.serializeIpv6SrPrefix(ipv6SrPrefix, buffer);
            TlvUtil.writeTLV(IPV6_PREFIX_SID, buffer, byteAggregator);
        }
    }

    private static void serializeSrPrefix(final SrPrefix srPrefix, final ByteBuf byteAggregator) {
        if (srPrefix != null) {
            final ByteBuf buffer = Unpooled.buffer();
            SrPrefixAttributesParser.serializeSrPrefix(srPrefix, buffer);
            TlvUtil.writeTLV(PREFIX_SID, buffer, byteAggregator);
        }
    }

    private static void serializePrefixMetric(final IgpMetric prefixMetric, final ByteBuf byteAggregator) {
        if (prefixMetric != null) {
            TlvUtil.writeTLV(PREFIX_METRIC, Unpooled.copyInt(prefixMetric.getValue().intValue()), byteAggregator);
        }
    }

    private static void serializeRouteTags(final List<RouteTag> routeTags, final ByteBuf byteAggregator) {
        if (routeTags != null) {
            final ByteBuf routeTagsBuf = Unpooled.buffer();
            for (final RouteTag routeTag : routeTags) {
                routeTagsBuf.writeBytes(routeTag.getValue());
            }
            TlvUtil.writeTLV(ROUTE_TAG, routeTagsBuf, byteAggregator);
        }
    }

    private static void serializeExtendedRouteTags(final List<ExtendedRouteTag> exRouteTags, final ByteBuf byteAggregator) {
        if (exRouteTags != null) {
            final ByteBuf extendedBuf = Unpooled.buffer();
            for (final ExtendedRouteTag exRouteTag : exRouteTags) {
                extendedBuf.writeBytes(exRouteTag.getValue());
            }
            TlvUtil.writeTLV(EXTENDED_ROUTE_TAG, extendedBuf, byteAggregator);
        }
    }

    private static void serializeForwardingAddress(final IpAddress forwardingAddress, final ByteBuf byteAggregator) {
        if (forwardingAddress != null) {
            final ByteBuf ospfBuf = Unpooled.buffer();
            if (forwardingAddress.getIpv4Address() != null) {
                ospfBuf.writeBytes(Ipv4Util.bytesForAddress(forwardingAddress.getIpv4Address()));
            } else if (forwardingAddress.getIpv6Address() != null) {
                ospfBuf.writeBytes(Ipv6Util.bytesForAddress(forwardingAddress.getIpv6Address()));
            }
            TlvUtil.writeTLV(FORWARDING_ADDRESS, ospfBuf, byteAggregator);
        }
    }
}
