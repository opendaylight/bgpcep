/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute;

import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint32;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrLinkAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.AdministrativeGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv6RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Loss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.MplsProtocolMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.LinkMinMaxDelay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.LinkMinMaxDelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.PeerAdjSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.PeerAdjSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.PeerNodeSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.PeerNodeSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.PeerSetSids;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.PeerSetSidsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.SrAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.SrLanAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.LinkAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class LinkAttributesParser {
    public static final int SR_LAN_ADJ_ID = 1100;

    private static final Logger LOG = LoggerFactory.getLogger(LinkAttributesParser.class);
    private static final int UNRESERVED_BW_COUNT = 8;

    private static final int BANDWIDTH_LENGTH = 4;

    // MPLS protection mask bits
    private static final int FLAGS_SIZE = 8;

    private static final int LDP_BIT = 0;
    private static final int RSVP_BIT = 1;

    // Link Attribute TLVs
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
    private static final int PEER_NODE_SID_CODE = 1101;
    private static final int PEER_ADJ_SID_CODE = 1102;
    private static final int PEER_SET_SID_CODE = 1103;

    // RFC9104 Traffic Engineering Extended Administrative Groups
    private static final int EXTENDED_ADMIN_GROUP = 1173;

    // Performance Metrics
    private static final int LINK_DELAY = 1114;
    private static final int LINK_MIN_MAX_DELAY = 1115;
    private static final int DELAY_VARIATION = 1116;
    private static final int LINK_LOSS = 1117;
    private static final int RESIDUAL_BANDWIDTH = 1118;
    private static final int AVAILABLE_BANDWIDTH = 1119;
    private static final int UTILIZED_BANDWIDTH = 1120;


    private LinkAttributesParser() {
        // Hidden on purpose
    }

    @FunctionalInterface
    private interface SerializerInterface {
        void check(Object cont);
    }

    /**
     * Parse Link Attributes.
     *
     * @param attributes key is the tlv type and value is the value of the tlv
     * @param protocolId to differentiate parsing methods
     * @return {@link LinkStateAttribute}
     */
    static LinkStateAttribute parseLinkAttributes(final Multimap<Integer, ByteBuf> attributes,
            final ProtocolId protocolId) {
        final LinkAttributesBuilder builder = new LinkAttributesBuilder();
        final List<SrAdjIds> srAdjIds = new ArrayList<>();
        final List<SrLanAdjIds> srLanAdjIds = new ArrayList<>();
        final List<PeerSetSids> peerSetSids = new ArrayList<>();
        for (final Entry<Integer, ByteBuf> entry : attributes.entries()) {
            LOG.trace("Link attribute TLV {}", entry.getKey());
            final int key = entry.getKey();
            final ByteBuf value = entry.getValue();
            switch (key) {
                case TlvUtil.LOCAL_IPV4_ROUTER_ID:
                    builder.setLocalIpv4RouterId(new Ipv4RouterIdentifier(Ipv4Util.addressForByteBuf(value)));
                    LOG.debug("Parsed IPv4 Router-ID of local node: {}", builder.getLocalIpv4RouterId());
                    break;
                case TlvUtil.LOCAL_IPV6_ROUTER_ID:
                    builder.setLocalIpv6RouterId(new Ipv6RouterIdentifier(Ipv6Util.addressForByteBuf(value)));
                    LOG.debug("Parsed IPv6 Router-ID of local node: {}", builder.getLocalIpv6RouterId());
                    break;
                case REMOTE_IPV4_ROUTER_ID:
                    builder.setRemoteIpv4RouterId(new Ipv4RouterIdentifier(Ipv4Util.addressForByteBuf(value)));
                    LOG.debug("Parsed IPv4 Router-ID of remote node: {}", builder.getRemoteIpv4RouterId());
                    break;
                case REMOTE_IPV6_ROUTER_ID:
                    builder.setRemoteIpv6RouterId(new Ipv6RouterIdentifier(Ipv6Util.addressForByteBuf(value)));
                    LOG.debug("Parsed IPv6 Router-ID of remote node: {}", builder.getRemoteIpv6RouterId());
                    break;
                case ADMIN_GROUP:
                    builder.setAdminGroup(new AdministrativeGroup(readUint32(value)));
                    LOG.debug("Parsed Administrative Group {}", builder.getAdminGroup());
                    break;
                case EXTENDED_ADMIN_GROUP:
                    // FIXME: BGPCEP-895: add proper implementation
                    LOG.info("Support for Extended Administrative Group not implemented, ignoring it");
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
                    parseUnreservedBandwidth(value, builder);
                    break;
                case TE_METRIC:
                    builder.setTeMetric(new TeMetric(readUint32(value)));
                    LOG.debug("Parsed Metric {}", builder.getTeMetric());
                    break;
                case LINK_PROTECTION_TYPE:
                    builder.setLinkProtection(LinkProtectionType.forValue(value.readShort()));
                    LOG.debug("Parsed Link Protection Type {}", builder.getLinkProtection());
                    break;
                case MPLS_PROTOCOL:
                    final BitArray bits = BitArray.valueOf(value, FLAGS_SIZE);
                    builder.setMplsProtocol(new MplsProtocolMask(bits.get(LDP_BIT), bits.get(RSVP_BIT)));
                    LOG.debug("Parsed MPLS Protocols: {}", builder.getMplsProtocol());
                    break;
                case METRIC:
                    // length can 3, 2 or 1
                    builder.setMetric(new Metric(Uint32.valueOf(ByteArray.bytesToLong(ByteArray.readAllBytes(value)))));
                    LOG.debug("Parsed Metric {}", builder.getMetric());
                    break;
                case SHARED_RISK_LINK_GROUP:
                    parseSrlg(value, builder);
                    break;
                case LINK_OPAQUE:
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Parsed Opaque value : {}", ByteBufUtil.hexDump(value));
                    }
                    break;
                case LINK_NAME:
                    builder.setLinkName(new String(ByteArray.readAllBytes(value), StandardCharsets.US_ASCII));
                    LOG.debug("Parsed Link Name : {}", builder.getLinkName());
                    break;
                case SR_ADJ_ID:
                    srAdjIds.add(SrLinkAttributesParser.parseAdjacencySegmentIdentifier(value, protocolId));
                    LOG.debug("Parsed Adjacency Segment Identifier :{}", srAdjIds.get(srAdjIds.size() - 1));
                    break;
                case SR_LAN_ADJ_ID:
                    srLanAdjIds.add(SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(value, protocolId));
                    LOG.debug("Parsed Adjacency Segment Identifier :{}", srLanAdjIds.get(srLanAdjIds.size() - 1));
                    break;
                case PEER_NODE_SID_CODE:
                    builder.setPeerNodeSid(new PeerNodeSidBuilder(
                        SrLinkAttributesParser.parseEpeAdjacencySegmentIdentifier(value)).build());
                    LOG.debug("Parsed Peer Segment Identifier :{}", builder.getPeerNodeSid());
                    break;
                case PEER_ADJ_SID_CODE:
                    builder.setPeerAdjSid(new PeerAdjSidBuilder(
                        SrLinkAttributesParser.parseEpeAdjacencySegmentIdentifier(value)).build());
                    LOG.debug("Parsed Peer Segment Identifier :{}", builder.getPeerAdjSid());
                    break;
                case PEER_SET_SID_CODE:
                    peerSetSids.add(new PeerSetSidsBuilder(
                        SrLinkAttributesParser.parseEpeAdjacencySegmentIdentifier(value)).build());
                    LOG.debug("Parsed Peer Set Sid :{}", peerSetSids.get(peerSetSids.size() - 1));
                    break;
                    // Performance Metrics
                case LINK_DELAY:
                    builder.setLinkDelay(new Delay(readUint32(value)));
                    LOG.debug("Parsed Link Delay {}", builder.getLinkDelay());
                    break;
                case LINK_MIN_MAX_DELAY:
                    builder.setLinkMinMaxDelay(new LinkMinMaxDelayBuilder()
                        .setMinDelay(new Delay(readUint32(value)))
                        .setMaxDelay(new Delay(readUint32(value)))
                        .build());
                    LOG.debug("Parsed Link Min/Max Delay {}", builder.getLinkMinMaxDelay());
                    break;
                case DELAY_VARIATION:
                    builder.setDelayVariation(new Delay(readUint32(value)));
                    LOG.debug("Parsed Delay Variation {}", builder.getDelayVariation());
                    break;
                case LINK_LOSS:
                    builder.setLinkLoss(new Loss(readUint32(value)));
                    LOG.debug("Parsed Link Loss {}", builder.getLinkLoss());
                    break;
                case RESIDUAL_BANDWIDTH:
                    builder.setResidualBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed Residual Bandwidth {}", builder.getResidualBandwidth());
                    break;
                case AVAILABLE_BANDWIDTH:
                    builder.setAvailableBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed Available Bandwidth {}", builder.getAvailableBandwidth());
                    break;
                case UTILIZED_BANDWIDTH:
                    builder.setUtilizedBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed Utilized Bandwidth {}", builder.getUtilizedBandwidth());
                    break;
                default:
                    LOG.warn("TLV {} is not a recognized link attribute, ignoring it", key);
            }
        }
        if (!srAdjIds.isEmpty()) {
            builder.setSrAdjIds(srAdjIds);
        }
        if (!srLanAdjIds.isEmpty()) {
            builder.setSrLanAdjIds(srLanAdjIds);
        }
        if (!peerSetSids.isEmpty()) {
            builder.setPeerSetSids(peerSetSids);
        }
        LOG.trace("Finished parsing Link Attributes.");
        return new LinkAttributesCaseBuilder().setLinkAttributes(builder.build()).build();
    }

    private static void parseUnreservedBandwidth(final ByteBuf value, final LinkAttributesBuilder builder) {
        final var unreservedBandwidth =
            BindingMap.<UnreservedBandwidthKey, UnreservedBandwidth>orderedBuilder(UNRESERVED_BW_COUNT);
        for (int i = 0; i < UNRESERVED_BW_COUNT; i++) {
            final ByteBuf v = value.readSlice(BANDWIDTH_LENGTH);
            unreservedBandwidth.add(new UnreservedBandwidthBuilder()
                .setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)))
                .setPriority(Uint8.valueOf(i))
                .build());
        }
        builder.setUnreservedBandwidth(unreservedBandwidth.build());
        LOG.debug("Parsed Unreserved Bandwidth {}", builder.getUnreservedBandwidth());
    }

    private static void parseSrlg(final ByteBuf value, final LinkAttributesBuilder builder) {
        final var sharedRiskLinkGroups = ImmutableSet.<SrlgId>builder();
        while (value.isReadable()) {
            sharedRiskLinkGroups.add(new SrlgId(readUint32(value)));
        }
        builder.setSharedRiskLinkGroups(sharedRiskLinkGroups.build());
        LOG.debug("Parsed Shared Risk Link Groups {}", builder.getSharedRiskLinkGroups());
    }

    private static void ifPresentApply(final Object tlv, final SerializerInterface serializer) {
        if (tlv != null) {
            serializer.check(tlv);
        }
    }

    static void serializeLinkAttributes(final LinkAttributesCase linkAttributesCase, final ByteBuf output) {
        final LinkAttributes linkAttributes = linkAttributesCase.getLinkAttributes();
        LOG.trace("Started serializing Link Attributes");
        ifPresentApply(linkAttributes.getLocalIpv4RouterId(), value -> TlvUtil.writeTLV(TlvUtil.LOCAL_IPV4_ROUTER_ID,
            Ipv4Util.byteBufForAddress((Ipv4AddressNoZone) value), output));
        ifPresentApply(linkAttributes.getLocalIpv6RouterId(), value -> TlvUtil.writeTLV(TlvUtil.LOCAL_IPV6_ROUTER_ID,
            Ipv6Util.byteBufForAddress((Ipv6AddressNoZone) value), output));
        ifPresentApply(linkAttributes.getRemoteIpv4RouterId(), value -> TlvUtil.writeTLV(REMOTE_IPV4_ROUTER_ID,
            Ipv4Util.byteBufForAddress((Ipv4AddressNoZone) value), output));
        ifPresentApply(linkAttributes.getRemoteIpv6RouterId(), value -> TlvUtil.writeTLV(REMOTE_IPV6_ROUTER_ID,
            Ipv6Util.byteBufForAddress((Ipv6AddressNoZone) value), output));
        ifPresentApply(linkAttributes.getAdminGroup(), value -> TlvUtil.writeTLV(ADMIN_GROUP,
            Unpooled.copyInt(((AdministrativeGroup) value).getValue().intValue()), output));
        ifPresentApply(linkAttributes.getMaxLinkBandwidth(), value -> TlvUtil.writeTLV(MAX_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        ifPresentApply(linkAttributes.getMaxReservableBandwidth(), value -> TlvUtil.writeTLV(MAX_RESERVABLE_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        serializeUnreservedBw(linkAttributes.getUnreservedBandwidth(), output);
        ifPresentApply(linkAttributes.getTeMetric(), value -> TlvUtil.writeTLV(TE_METRIC,
            Unpooled.copyLong(((TeMetric) value).getValue().toJava()), output));
        ifPresentApply(linkAttributes.getLinkProtection(), value -> TlvUtil.writeTLV(LINK_PROTECTION_TYPE,
            Unpooled.copyShort(((LinkProtectionType) value).getIntValue()), output));
        serializeMplsProtocolMask(linkAttributes.getMplsProtocol(), output);
        ifPresentApply(linkAttributes.getMetric(), value -> TlvUtil.writeTLV(METRIC,
            Unpooled.copyMedium(((Metric) value).getValue().intValue()), output));
        serializeSrlg(linkAttributes.getSharedRiskLinkGroups(), output);
        ifPresentApply(linkAttributes.getLinkName(), value -> TlvUtil.writeTLV(LINK_NAME,
            Unpooled.wrappedBuffer(StandardCharsets.UTF_8.encode((String) value)), output));
        ifPresentApply(linkAttributes.getSrAdjIds(),
            value -> SrLinkAttributesParser.serializeAdjacencySegmentIdentifiers((List<SrAdjIds>) value, SR_ADJ_ID,
                output));
        ifPresentApply(linkAttributes.getSrLanAdjIds(),
            value -> SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifiers((List<SrLanAdjIds>) value, output));
        ifPresentApply(linkAttributes.getPeerNodeSid(), value -> TlvUtil.writeTLV(PEER_NODE_SID_CODE,
            SrLinkAttributesParser.serializeAdjacencySegmentIdentifier((PeerNodeSid) value), output));
        ifPresentApply(linkAttributes.getPeerAdjSid(), value -> TlvUtil.writeTLV(PEER_ADJ_SID_CODE,
            SrLinkAttributesParser.serializeAdjacencySegmentIdentifier((PeerAdjSid) value), output));
        ifPresentApply(linkAttributes.getPeerSetSids(),
            value -> SrLinkAttributesParser.serializeAdjacencySegmentIdentifiers((List<PeerSetSids>) value,
                PEER_SET_SID_CODE, output));
        // Performance Metrics
        ifPresentApply(linkAttributes.getLinkDelay(), value -> TlvUtil.writeTLV(LINK_DELAY,
            Unpooled.copyInt(((Delay) value).getValue().intValue()), output));
        serializeLinkMinMaxDelay(linkAttributes.getLinkMinMaxDelay(), output);
        ifPresentApply(linkAttributes.getDelayVariation(), value -> TlvUtil.writeTLV(DELAY_VARIATION,
            Unpooled.copyInt(((Delay) value).getValue().intValue()), output));
        ifPresentApply(linkAttributes.getLinkLoss(), value -> TlvUtil.writeTLV(LINK_LOSS,
            Unpooled.copyInt(((Loss) value).getValue().intValue()), output));
        ifPresentApply(linkAttributes.getResidualBandwidth(), value -> TlvUtil.writeTLV(RESIDUAL_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        ifPresentApply(linkAttributes.getAvailableBandwidth(), value -> TlvUtil.writeTLV(AVAILABLE_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        ifPresentApply(linkAttributes.getUtilizedBandwidth(), value -> TlvUtil.writeTLV(UTILIZED_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        LOG.trace("Finished serializing Link Attributes");
    }

    private static void serializeUnreservedBw(final Map<UnreservedBandwidthKey, UnreservedBandwidth> ubList,
            final ByteBuf byteAggregator) {
        // this sub-TLV contains eight 32-bit IEEE floating point numbers
        if (ubList != null) {
            final ByteBuf unreservedBandwithBuf = Unpooled.buffer();
            for (final UnreservedBandwidth unreservedBandwidth : ubList.values()) {
                unreservedBandwithBuf.writeBytes(unreservedBandwidth.getBandwidth().getValue());
            }
            TlvUtil.writeTLV(UNRESERVED_BANDWIDTH, unreservedBandwithBuf, byteAggregator);
        }
    }

    private static void serializeSrlg(final Set<SrlgId> srlgList, final ByteBuf byteAggregator) {
        if (srlgList != null) {
            final ByteBuf sharedRLGBuf = Unpooled.buffer();
            for (final SrlgId srlgId : srlgList) {
                writeUint32(sharedRLGBuf, srlgId.getValue());
            }
            TlvUtil.writeTLV(SHARED_RISK_LINK_GROUP, sharedRLGBuf, byteAggregator);
        }
    }

    private static void serializeMplsProtocolMask(final MplsProtocolMask mplsProtocolMask,
            final ByteBuf byteAggregator) {
        if (mplsProtocolMask != null) {
            final ByteBuf mplsProtocolMaskBuf = Unpooled.buffer(1);
            final BitArray mask = new BitArray(FLAGS_SIZE);
            mask.set(LDP_BIT, mplsProtocolMask.getLdp());
            mask.set(RSVP_BIT, mplsProtocolMask.getRsvpte());
            mask.toByteBuf(mplsProtocolMaskBuf);
            TlvUtil.writeTLV(MPLS_PROTOCOL, mplsProtocolMaskBuf, byteAggregator);
        }
    }

    private static void serializeLinkMinMaxDelay(final LinkMinMaxDelay linkMinMaxDelay, final ByteBuf byteAggregator) {
        if (linkMinMaxDelay != null) {
            final ByteBuf linkMinMaxDelayBuf = Unpooled.buffer(8);
            writeUint32(linkMinMaxDelayBuf, linkMinMaxDelay.getMinDelay().getValue());
            writeUint32(linkMinMaxDelayBuf, linkMinMaxDelay.getMaxDelay().getValue());
            TlvUtil.writeTLV(LINK_MIN_MAX_DELAY, linkMinMaxDelayBuf, byteAggregator);
        }
    }
}
