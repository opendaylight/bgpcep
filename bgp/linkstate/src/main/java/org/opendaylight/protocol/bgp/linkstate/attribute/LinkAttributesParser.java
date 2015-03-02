/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SrLinkAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AdministrativeGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv6RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.MplsProtocolMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrAdjId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class LinkAttributesParser {

    private static final Logger LOG = LoggerFactory.getLogger(LinkAttributesParser.class);

    private LinkAttributesParser() {
        throw new UnsupportedOperationException();
    }

    private static final int UNRESERVED_BW_COUNT = 8;

    private static final int BANDWIDTH_LENGTH = 4;

    // MPLS protection mask bits
    private static final int FLAGS_SIZE = 8;

    private static final int LDP_BIT = 0;
    private static final int RSVP_BIT = 1;

    /* Link Attribute TLVs */
    private static final int REMOTE_IPV4_ROUTER_ID = 1030;
    private static final int REMOTE_IPV6_ROUTER_ID = 1031;
    private static final int ADMIN_GROUP = 1088;
    private static final int MAX_BANDWIDTH = 1089;
    private static final int MAX_RESERVABLE_BANDWIDTH = 1090;
    private static final int UNRESERVED_BANDWIDTH = 1091;
    private static final int TE_METRIC = 1092;
    private static final int LINK_PROTECTION_TYPE = 1093;
    private static final int MPLS_PROTOCOL = 1094;
    private static final int METRIC = 1095;
    private static final int SHARED_RISK_LINK_GROUP = 1096;
    private static final int LINK_OPAQUE = 1097;
    private static final int LINK_NAME = 1098;
    private static final int SR_ADJ_ID = 1099;
    private static final int SR_LAN_ADJ_ID = 1100;

    /**
     * Parse Link Attributes.
     *
     * @param attributes key is the tlv type and value is the value of the tlv
     * @return {@link LinkStateAttribute}
     */
    static LinkStateAttribute parseLinkAttributes(final Multimap<Integer, ByteBuf> attributes) {
        final LinkAttributesBuilder builder = new LinkAttributesBuilder();
        for (final Entry<Integer, ByteBuf> entry : attributes.entries()) {
            LOG.trace("Link attribute TLV {}", entry.getKey());
            final int key = entry.getKey();
            final ByteBuf value = entry.getValue();
            switch (key) {
            case TlvUtil.LOCAL_IPV4_ROUTER_ID:
                final Ipv4RouterIdentifier lipv4 = new Ipv4RouterIdentifier(Ipv4Util.addressForByteBuf(value));
                builder.setLocalIpv4RouterId(lipv4);
                LOG.debug("Parsed IPv4 Router-ID of local node: {}", lipv4);
                break;
            case TlvUtil.LOCAL_IPV6_ROUTER_ID:
                final Ipv6RouterIdentifier lipv6 = new Ipv6RouterIdentifier(Ipv6Util.addressForByteBuf(value));
                builder.setLocalIpv6RouterId(lipv6);
                LOG.debug("Parsed IPv6 Router-ID of local node: {}", lipv6);
                break;
            case REMOTE_IPV4_ROUTER_ID:
                final Ipv4RouterIdentifier ripv4 = new Ipv4RouterIdentifier(Ipv4Util.addressForByteBuf(value));
                builder.setRemoteIpv4RouterId(ripv4);
                LOG.debug("Parsed IPv4 Router-ID of remote node: {}", ripv4);
                break;
            case REMOTE_IPV6_ROUTER_ID:
                final Ipv6RouterIdentifier ripv6 = new Ipv6RouterIdentifier(Ipv6Util.addressForByteBuf(value));
                builder.setRemoteIpv6RouterId(ripv6);
                LOG.debug("Parsed IPv6 Router-ID of remote node: {}", ripv6);
                break;
            case ADMIN_GROUP:
                builder.setAdminGroup(new AdministrativeGroup(value.readUnsignedInt()));
                LOG.debug("Parsed Administrative Group {}", builder.getAdminGroup());
                break;
            case MAX_BANDWIDTH:
                builder.setMaxLinkBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                LOG.debug("Parsed Max Bandwidth {}", builder.getMaxLinkBandwidth());
                break;
            case MAX_RESERVABLE_BANDWIDTH:
                builder.setMaxReservableBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                LOG.debug("Parsed Max Reservable Bandwidth {}", builder.getMaxReservableBandwidth());
                break;
            case UNRESERVED_BANDWIDTH:
                final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.UnreservedBandwidth> unreservedBandwidth = new ArrayList<>(UNRESERVED_BW_COUNT);
                for (int i = 0; i < UNRESERVED_BW_COUNT; i++) {
                    final ByteBuf v = value.readSlice(BANDWIDTH_LENGTH);
                    unreservedBandwidth.add(new UnreservedBandwidthBuilder().setBandwidth(new Bandwidth(ByteArray.readAllBytes(v))).setPriority((short) i).build());
                }
                builder.setUnreservedBandwidth(unreservedBandwidth);
                LOG.debug("Parsed Unreserved Bandwidth {}", builder.getUnreservedBandwidth());
                break;
            case TE_METRIC:
                builder.setTeMetric(new TeMetric(ByteArray.bytesToLong(ByteArray.readAllBytes(value))));
                LOG.debug("Parsed Metric {}", builder.getTeMetric());
                break;
            case LINK_PROTECTION_TYPE:
                final int l = value.readShort();
                final LinkProtectionType lpt = LinkProtectionType.forValue(l);
                if (lpt == null) {
                    LOG.warn("Link Protection Type not recognized: {}", l);
                    break;
                }
                builder.setLinkProtection(lpt);
                LOG.debug("Parsed Link Protection Type {}", lpt);
                break;
            case MPLS_PROTOCOL:
                final BitArray bits = BitArray.valueOf(value, FLAGS_SIZE);
                builder.setMplsProtocol(new MplsProtocolMask(bits.get(LDP_BIT), bits.get(RSVP_BIT)));
                LOG.debug("Parsed MPLS Protocols: {}", builder.getMplsProtocol());
                break;
            case METRIC:
                // length can 3, 2 or 1
                builder.setMetric(new Metric(ByteArray.bytesToLong(ByteArray.readAllBytes(value))));
                LOG.debug("Parsed Metric {}", builder.getMetric());
                break;
            case SHARED_RISK_LINK_GROUP:
                final List<SrlgId> sharedRiskLinkGroups = new ArrayList<>();
                while (value.isReadable()) {
                    sharedRiskLinkGroups.add(new SrlgId(value.readUnsignedInt()));
                }
                builder.setSharedRiskLinkGroups(sharedRiskLinkGroups);
                LOG.debug("Parsed Shared Risk Link Groups {}", Arrays.toString(sharedRiskLinkGroups.toArray()));
                break;
            case LINK_OPAQUE:
                LOG.debug("Parsed Opaque value : {}", ByteBufUtil.hexDump(value));
                break;
            case LINK_NAME:
                final String name = new String(ByteArray.readAllBytes(value), Charsets.US_ASCII);
                builder.setLinkName(name);
                LOG.debug("Parsed Link Name : {}", name);
                break;
            case SR_ADJ_ID:
                final SrAdjId srAdjId = SrLinkAttributesParser.parseAdjacencySegmentIdentifier(value);
                builder.setSrAdjId(srAdjId);
                LOG.debug("Parsed Adjacency Segment Identifier :{}", srAdjId);
            case SR_LAN_ADJ_ID:
                final SrLanAdjId srLanAdjId = SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(value);
                builder.setSrLanAdjId(srLanAdjId);
                LOG.debug("Parsed Adjacency Segment Identifier :{}", srLanAdjId);
            default:
                LOG.warn("TLV {} is not a valid link attribute, ignoring it", key);
            }
        }
        LOG.trace("Finished parsing Link Attributes.");
        return new LinkAttributesCaseBuilder().setLinkAttributes(builder.build()).build();
    }

    static void serializeLinkAttributes(final LinkAttributesCase linkAttributesCase, final ByteBuf byteAggregator) {
        final LinkAttributes linkAttributes = linkAttributesCase.getLinkAttributes();
        LOG.trace("Started serializing Link Attributes");
        if (linkAttributes.getLocalIpv4RouterId() != null) {
            TlvUtil.writeTLV(TlvUtil.LOCAL_IPV4_ROUTER_ID, Ipv4Util.byteBufForAddress(linkAttributes.getLocalIpv4RouterId()), byteAggregator);
        }
        if (linkAttributes.getLocalIpv6RouterId() != null) {
            TlvUtil.writeTLV(TlvUtil.LOCAL_IPV6_ROUTER_ID, Ipv6Util.byteBufForAddress(linkAttributes.getLocalIpv6RouterId()), byteAggregator);
        }
        if (linkAttributes.getRemoteIpv4RouterId() != null) {
            TlvUtil.writeTLV(REMOTE_IPV4_ROUTER_ID, Ipv4Util.byteBufForAddress(linkAttributes.getRemoteIpv4RouterId()), byteAggregator);
        }
        if (linkAttributes.getRemoteIpv6RouterId() != null) {
            TlvUtil.writeTLV(REMOTE_IPV6_ROUTER_ID, Ipv6Util.byteBufForAddress(linkAttributes.getRemoteIpv6RouterId()), byteAggregator);
        }
        if (linkAttributes.getAdminGroup() != null) {
            TlvUtil.writeTLV(ADMIN_GROUP, Unpooled.copyInt(linkAttributes.getAdminGroup().getValue().intValue()), byteAggregator);
        }
        if (linkAttributes.getMaxLinkBandwidth() != null) {
            TlvUtil.writeTLV(MAX_BANDWIDTH, Unpooled.wrappedBuffer(linkAttributes.getMaxLinkBandwidth().getValue()), byteAggregator);
        }
        if (linkAttributes.getMaxReservableBandwidth() != null) {
            TlvUtil.writeTLV(MAX_RESERVABLE_BANDWIDTH, Unpooled.wrappedBuffer(linkAttributes.getMaxReservableBandwidth().getValue()), byteAggregator);
        }
        // this sub-TLV contains eight 32-bit IEEE floating point numbers
        final List<UnreservedBandwidth> ubList = linkAttributes.getUnreservedBandwidth();
        if (ubList != null) {
            final ByteBuf unreservedBandwithBuf = Unpooled.buffer();
            for (final UnreservedBandwidth unreservedBandwidth : ubList) {
                unreservedBandwithBuf.writeBytes(unreservedBandwidth.getBandwidth().getValue());
            }
            TlvUtil.writeTLV(UNRESERVED_BANDWIDTH, unreservedBandwithBuf, byteAggregator);
        }
        if (linkAttributes.getTeMetric() != null) {
            TlvUtil.writeTLV(TE_METRIC, Unpooled.copyLong(linkAttributes.getTeMetric().getValue().longValue()), byteAggregator);
        }
        if (linkAttributes.getLinkProtection() != null) {
            TlvUtil.writeTLV(LINK_PROTECTION_TYPE, Unpooled.copyShort(linkAttributes.getLinkProtection().getIntValue()), byteAggregator);
        }
        serializeMplsProtocolMask(linkAttributes.getMplsProtocol(), byteAggregator);
        if (linkAttributes.getMetric() != null) {
            // size of metric can be 1,2 or 3 depending on the protocol
            TlvUtil.writeTLV(METRIC, Unpooled.copyMedium(linkAttributes.getMetric().getValue().intValue()), byteAggregator);
        }
        final List<SrlgId> srlgList = linkAttributes.getSharedRiskLinkGroups();
        if (srlgList != null) {
            final ByteBuf sharedRLGBuf = Unpooled.buffer();
            for (final SrlgId srlgId : srlgList) {
                sharedRLGBuf.writeInt(srlgId.getValue().intValue());
            }
            TlvUtil.writeTLV(SHARED_RISK_LINK_GROUP, sharedRLGBuf, byteAggregator);
        }
        if (linkAttributes.getLinkName() != null) {
            TlvUtil.writeTLV(LINK_NAME, Unpooled.wrappedBuffer(Charsets.UTF_8.encode(linkAttributes.getLinkName())), byteAggregator);
        }
        if (linkAttributes.getSrAdjId() != null) {
            TlvUtil.writeTLV(SR_ADJ_ID, SrLinkAttributesParser.serializeAdjacencySegmentIdentifier(linkAttributes.getSrAdjId()), byteAggregator);
        }
        if (linkAttributes.getSrLanAdjId() != null) {
            TlvUtil.writeTLV(SR_LAN_ADJ_ID, SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifier(linkAttributes.getSrLanAdjId()), byteAggregator);
        }
        LOG.trace("Finished serializing Link Attributes");
    }

    private static void serializeMplsProtocolMask(final MplsProtocolMask mplsProtocolMask, final ByteBuf byteAggregator ) {
        if (mplsProtocolMask != null) {
            final ByteBuf mplsProtocolMaskBuf = Unpooled.buffer(1);
            final BitArray mask = new BitArray(FLAGS_SIZE);
            mask.set(LDP_BIT, mplsProtocolMask.isLdp());
            mask.set(RSVP_BIT, mplsProtocolMask.isRsvpte());
            mask.toByteBuf(mplsProtocolMaskBuf);
            TlvUtil.writeTLV(MPLS_PROTOCOL, mplsProtocolMaskBuf, byteAggregator);
        }
    }
}
