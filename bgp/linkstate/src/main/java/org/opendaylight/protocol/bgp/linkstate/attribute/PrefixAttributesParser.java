/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.BindingSidLabelParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.RangeTlvParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SrPrefixAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ExtendedRouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.IgpBits.UpDown;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.RouteTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.PrefixAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.IgpBitsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrBindingSidLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IgpMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class PrefixAttributesParser {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixAttributesParser.class);

    private PrefixAttributesParser() {
        throw new UnsupportedOperationException();
    }

    private static final int ROUTE_TAG_LENGTH = 4;
    private static final int EXTENDED_ROUTE_TAG_LENGTH = 8;

    private static final int FLAGS_SIZE = 8;
    private static final int UP_DOWN_BIT = 0;

    /* Prefix Attribute TLVs */
    private static final int IGP_FLAGS = 1152;
    private static final int ROUTE_TAG = 1153;
    private static final int EXTENDED_ROUTE_TAG = 1154;
    private static final int PREFIX_METRIC = 1155;
    private static final int FORWARDING_ADDRESS = 1156;
    private static final int PREFIX_OPAQUE = 1157;

    /* Segment routing TLV */
    public static final int PREFIX_SID = 1158;
    public static final int RANGE = 1159;
    public static final int BINDING_SID = 1160;

    /**
     * Parse prefix attributes.
     *
     * @param attributes key is the tlv type and value are the value bytes of the tlv
     * @return {@link LinkStateAttribute}
     */
    static LinkStateAttribute parsePrefixAttributes(final Multimap<Integer, ByteBuf> attributes) {
        final PrefixAttributesBuilder builder = new PrefixAttributesBuilder();
        final List<RouteTag> routeTags = new ArrayList<>();
        final List<ExtendedRouteTag> exRouteTags = new ArrayList<>();
        for (final Entry<Integer, ByteBuf> entry : attributes.entries()) {
            final int key = entry.getKey();
            final ByteBuf value = entry.getValue();
            LOG.trace("Prefix attribute TLV {}", key);
            switch (key) {
            case IGP_FLAGS:
                final BitArray flags = BitArray.valueOf(value, FLAGS_SIZE);
                final boolean upDownBit = flags.get(UP_DOWN_BIT);
                builder.setIgpBits(new IgpBitsBuilder().setUpDown(new UpDown(upDownBit)).build());
                LOG.debug("Parsed IGP flag (up/down bit) : {}", upDownBit);
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
                final SrPrefix prefix = SrPrefixAttributesParser.parseSrPrefix(value);
                builder.setSrPrefix(prefix);
                LOG.debug("Parsed SR Prefix: {}", prefix);
                break;
            case RANGE:
                final SrRange range = RangeTlvParser.parseSrRange(value);
                builder.setSrRange(range);
                LOG.debug("Parsed SR Range: {}", range);
                break;
            case BINDING_SID:
                final SrBindingSidLabel label = BindingSidLabelParser.parseBindingSidLabel(value);
                builder.setSrBindingSidLabel(label);
                LOG.debug("Parsed SR Binding SID {}", label);
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
            igpBit.set(UP_DOWN_BIT, prefixAtrributes.getIgpBits().getUpDown().isUpDown());
            TlvUtil.writeTLV(IGP_FLAGS, Unpooled.wrappedBuffer(igpBit.array()), byteAggregator);
        }
        serializeRouteTags(prefixAtrributes.getRouteTags(), byteAggregator);
        serializeExtendedRouteTags(prefixAtrributes.getExtendedTags(), byteAggregator);
        if (prefixAtrributes.getPrefixMetric() != null) {
            TlvUtil.writeTLV(PREFIX_METRIC, Unpooled.copyInt(prefixAtrributes.getPrefixMetric().getValue().intValue()), byteAggregator);
        }
        serializeForwardingAddress(prefixAtrributes.getOspfForwardingAddress(), byteAggregator);
        if (prefixAtrributes.getSrPrefix() != null) {
            final ByteBuf buffer = Unpooled.buffer();
            SrPrefixAttributesParser.serializeSrPrefix(prefixAtrributes.getSrPrefix(), buffer);
            TlvUtil.writeTLV(PREFIX_SID, buffer, byteAggregator);
        }
        if (prefixAtrributes.getSrRange() != null) {
            final ByteBuf sidBuffer = Unpooled.buffer();
            RangeTlvParser.serializeSrRange(prefixAtrributes.getSrRange(), sidBuffer);
            TlvUtil.writeTLV(RANGE, sidBuffer, byteAggregator);
        }
        if (prefixAtrributes.getSrBindingSidLabel() != null) {
            final ByteBuf sidBuffer = Unpooled.buffer();
            BindingSidLabelParser.serializeBindingSidLabel(prefixAtrributes.getSrBindingSidLabel(), sidBuffer);
            TlvUtil.writeTLV(BINDING_SID, sidBuffer, byteAggregator);
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
