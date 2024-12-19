/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute;

import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint64;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint64;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SRv6AttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrFlexAlgoParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrLinkAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.AbmLength;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.AdministrativeGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.AslaBitMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Ipv6RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Loss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.MplsProtocolMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.asla.tlv.AslaSubtlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.asla.tlv.AslaSubtlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.l2.bundle.member.L2Subtlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.l2.bundle.member.L2SubtlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.Asla;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.AslaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.L2BundleMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.L2BundleMemberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.LinkMsd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.PeerAdjSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.PeerAdjSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.PeerNodeSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.PeerNodeSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.PeerSetSids;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.PeerSetSidsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.Srv6EndXSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.Srv6LanEndXSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.LinkAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.performance.metric.LinkMinMaxDelay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.performance.metric.LinkMinMaxDelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrLanAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.standard.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.standard.attributes.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.standard.attributes.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.EpeSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.ExtendedAdminGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.lan.end.x.sid.neighbor.type.IsisNeighborCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.lan.end.x.sid.neighbor.type.Ospfv3NeighborCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class LinkAttributesParser {

    private static final Logger LOG = LoggerFactory.getLogger(LinkAttributesParser.class);

    // MPLS protection mask bits
    private static final int FLAGS_SIZE = 8;
    private static final int LDP_BIT = 0;
    private static final int RSVP_BIT = 1;

    // Link Remote ID
    private static final int REMOTE_IPV4_ROUTER_ID = 1030;
    private static final int REMOTE_IPV6_ROUTER_ID = 1031;

    // Link Attribute TLVs RFC9552 #5.3.2
    private static final int UNRESERVED_BW_COUNT = 8;
    private static final int BANDWIDTH_LENGTH = 4;

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

    // Segment Routing RFC9085, RFC9086 & RFC8814
    private static final int SR_LINK_MSD = 267;
    public static final int SR_ADJ_SID = 1099;
    public static final int SR_LAN_ADJ_SID = 1100;
    private static final int PEER_NODE_SID_CODE = 1101;
    private static final int PEER_ADJ_SID_CODE = 1102;
    private static final int PEER_SET_SID_CODE = 1103;

    // SRv6 RFC9514
    private static final int SRV6_END_X_SID = 1106;
    private static final int ISIS_SRV6_LAN_SID = 1107;
    private static final int OSPFV3_SRV6_LAN_SID = 1108;

    // Performance Metrics RFC8571
    private static final int LINK_DELAY = 1114;
    private static final int LINK_MIN_MAX_DELAY = 1115;
    private static final int DELAY_VARIATION = 1116;
    private static final int LINK_LOSS = 1117;
    private static final int RESIDUAL_BANDWIDTH = 1118;
    private static final int AVAILABLE_BANDWIDTH = 1119;
    private static final int UTILIZED_BANDWIDTH = 1120;

    // Application Specific Link Attributes RFC9294
    private static final int ASLA = 1122;
    private static final int FLAGS_FOUR_SIZE = 32;
    private static final int FLAGS_EIGHT_SIZE = 64;
    private static final int SABM_RSVP_FLAG = 0;
    private static final int SABM_SR_FLAG = 1;
    private static final int SABM_LFA_FLAG = 2;
    private static final int RESERVED = 2;


    // L2 bundle RFC9085
    private static final int L2_BUNDLE_MEMBER = 1172;

    // Traffic Engineering Extended Administrative Groups RFC9104
    private static final int EXTENDED_ADMIN_GROUP = 1173;

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
        final List<SrAdjIds> srAdjIds = new ArrayList<SrAdjIds>();
        final List<SrLanAdjIds> srLanAdjIds = new ArrayList<SrLanAdjIds>();
        final List<PeerSetSids> peerSetSids = new ArrayList<PeerSetSids>();
        final List<L2BundleMember> l2bundles = new ArrayList<L2BundleMember>();
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
                // Standard Metric
                case ADMIN_GROUP:
                    builder.setAdminGroup(new AdministrativeGroup(readUint32(value)));
                    LOG.debug("Parsed Administrative Group: {}", builder.getAdminGroup());
                    break;
                case MAX_BANDWIDTH:
                    builder.setMaxLinkBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed Max Bandwidth: {}", builder.getMaxLinkBandwidth());
                    break;
                case MAX_RESERVABLE_BANDWIDTH:
                    builder.setMaxReservableBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed Max Reservable Bandwidth: {}", builder.getMaxReservableBandwidth());
                    break;
                case UNRESERVED_BANDWIDTH:
                    builder.setUnreservedBandwidth(parseUnreservedBandwidth(value));
                    LOG.debug("Parsed Unreserved Bandwidth: {}", builder.getUnreservedBandwidth());
                    break;
                case TE_METRIC:
                    builder.setTeMetric(new TeMetric(readUint32(value)));
                    LOG.debug("Parsed Metric: {}", builder.getTeMetric());
                    break;
                case LINK_PROTECTION_TYPE:
                    builder.setLinkProtection(LinkProtectionType.forValue(value.readShort()));
                    LOG.debug("Parsed Link Protection Type: {}", builder.getLinkProtection());
                    break;
                case MPLS_PROTOCOL:
                    final BitArray bits = BitArray.valueOf(value, FLAGS_SIZE);
                    builder.setMplsProtocol(new MplsProtocolMask(bits.get(LDP_BIT), bits.get(RSVP_BIT)));
                    LOG.debug("Parsed MPLS Protocols: {}", builder.getMplsProtocol());
                    break;
                case METRIC:
                    // length can 3, 2 or 1
                    builder.setMetric(new Metric(Uint32.valueOf(ByteArray.bytesToLong(ByteArray.readAllBytes(value)))));
                    LOG.debug("Parsed Metric: {}", builder.getMetric());
                    break;
                case SHARED_RISK_LINK_GROUP:
                    builder.setSharedRiskLinkGroups(parseSrlg(value));
                    LOG.debug("Parsed Shared Risk Link Groups: {}", builder.getSharedRiskLinkGroups());
                    break;
                case LINK_OPAQUE:
                    builder.setOpaqueLink(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed Opaque value: {}", builder.getOpaqueLink());
                    break;
                case LINK_NAME:
                    builder.setLinkName(new String(ByteArray.readAllBytes(value), StandardCharsets.US_ASCII));
                    LOG.debug("Parsed Link Name: {}", builder.getLinkName());
                    break;
                // Segment Routing
                case SR_ADJ_SID:
                    srAdjIds.add(SrLinkAttributesParser.parseAdjacencySegmentIdentifier(value, protocolId));
                    LOG.debug("Parsed Adjacency Segment Identifier: {}", srAdjIds.get(srAdjIds.size() - 1));
                    break;
                case SR_LAN_ADJ_SID:
                    srLanAdjIds.add(SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(value, protocolId));
                    LOG.debug("Parsed Adjacency Segment Identifier: {}", srLanAdjIds.get(srLanAdjIds.size() - 1));
                    break;
                case SR_LINK_MSD:
                    builder.setLinkMsd(SrLinkAttributesParser.parseSrLinkMsd(value));
                    LOG.debug("Parsed SR Link MSD: {}", builder.getLinkMsd());
                    break;
                // Egress Peer Engineering
                case PEER_NODE_SID_CODE:
                    builder.setPeerNodeSid(new PeerNodeSidBuilder(
                        SrLinkAttributesParser.parseEpeSegmentIdentifier(value)).build());
                    LOG.debug("Parsed Peer Node Segment Identifier: {}", builder.getPeerNodeSid());
                    break;
                case PEER_ADJ_SID_CODE:
                    builder.setPeerAdjSid(new PeerAdjSidBuilder(
                        SrLinkAttributesParser.parseEpeSegmentIdentifier(value)).build());
                    LOG.debug("Parsed Peer Ajacency Segment Identifier: {}", builder.getPeerAdjSid());
                    break;
                case PEER_SET_SID_CODE:
                    peerSetSids.add(new PeerSetSidsBuilder(
                        SrLinkAttributesParser.parseEpeSegmentIdentifier(value)).build());
                    LOG.debug("Parsed Peer Set Sid: {}", peerSetSids.get(peerSetSids.size() - 1));
                    break;
                // SRv6
                case SRV6_END_X_SID:
                    builder.setSrv6EndXSid(SRv6AttributesParser.parseSrv6EndXSid(value));
                    LOG.debug("Parsed SRv6 End X SID: {}", builder.getSrv6EndXSid());
                    break;
                case ISIS_SRV6_LAN_SID:
                case OSPFV3_SRV6_LAN_SID:
                    builder.setSrv6LanEndXSid(SRv6AttributesParser.parseSrv6LanEndXSid(value, protocolId));
                    LOG.debug("Parsed SRv6 LAN End X SID: {}", builder.getSrv6LanEndXSid());
                    break;
                // Performance Metrics
                case LINK_DELAY:
                    builder.setLinkDelay(new Delay(readUint32(value)));
                    LOG.debug("Parsed Link Delay: {}", builder.getLinkDelay());
                    break;
                case LINK_MIN_MAX_DELAY:
                    builder.setLinkMinMaxDelay(new LinkMinMaxDelayBuilder()
                        .setMinDelay(new Delay(readUint32(value)))
                        .setMaxDelay(new Delay(readUint32(value)))
                        .build());
                    LOG.debug("Parsed Link Min/Max Delay: {}", builder.getLinkMinMaxDelay());
                    break;
                case DELAY_VARIATION:
                    builder.setDelayVariation(new Delay(readUint32(value)));
                    LOG.debug("Parsed Delay Variation: {}", builder.getDelayVariation());
                    break;
                case LINK_LOSS:
                    builder.setLinkLoss(new Loss(readUint32(value)));
                    LOG.debug("Parsed Link Loss: {}", builder.getLinkLoss());
                    break;
                case RESIDUAL_BANDWIDTH:
                    builder.setResidualBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed Residual Bandwidth: {}", builder.getResidualBandwidth());
                    break;
                case AVAILABLE_BANDWIDTH:
                    builder.setAvailableBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed Available Bandwidth: {}", builder.getAvailableBandwidth());
                    break;
                case UTILIZED_BANDWIDTH:
                    builder.setUtilizedBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed Utilized Bandwidth: {}", builder.getUtilizedBandwidth());
                    break;
                // Application Specific Link Attributes
                case ASLA:
                    builder.setAsla(parseAsla(value));
                    LOG.debug("Parsed Application Specific Link Attribute (ASLA): {}", builder.getAsla());
                    break;
                case EXTENDED_ADMIN_GROUP:
                    builder.setExtendedAdminGroup(SrFlexAlgoParser.parseExtendedAdminGroup(value));
                    LOG.debug("Parsed Extended Administrative Group: {}", builder.getExtendedAdminGroup());
                    break;
                case L2_BUNDLE_MEMBER:
                    l2bundles.add(parseL2BundleMember(value, protocolId));
                    LOG.debug("Parsed L2 Bundle Member: {}", builder.getL2BundleMember());
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
        if (!l2bundles.isEmpty()) {
            builder.setL2BundleMember(l2bundles);
        }
        LOG.trace("Finished parsing Link Attributes.");
        return new LinkAttributesCaseBuilder().setLinkAttributes(builder.build()).build();
    }

    private static Map<UnreservedBandwidthKey, UnreservedBandwidth> parseUnreservedBandwidth(final ByteBuf value) {
        final var unreservedBandwidth =
            BindingMap.<UnreservedBandwidthKey, UnreservedBandwidth>orderedBuilder(UNRESERVED_BW_COUNT);
        for (int i = 0; i < UNRESERVED_BW_COUNT; i++) {
            final ByteBuf v = value.readSlice(BANDWIDTH_LENGTH);
            unreservedBandwidth.add(new UnreservedBandwidthBuilder()
                .setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)))
                .setPriority(Uint8.valueOf(i))
                .build());
        }
        return unreservedBandwidth.build();
    }

    private static Set<SrlgId> parseSrlg(final ByteBuf value) {
        final var sharedRiskLinkGroups = ImmutableSet.<SrlgId>builder();
        while (value.isReadable()) {
            sharedRiskLinkGroups.add(new SrlgId(readUint32(value)));
        }
        return sharedRiskLinkGroups.build();
    }


    private static Asla parseAsla(final ByteBuf buffer) {
        final AslaBuilder aslaBuilder = new AslaBuilder();

        // Start reading Standard and User Defined Bit Mask length ...
        final int alength = buffer.readByte();
        final int ulength = buffer.readByte();
        buffer.skipBytes(RESERVED);
        final BitArray flags;
        // .. to determine how many bytes must be read in the buffer i.e. 0, 4 or 8 bytes
        switch (alength) {
            case 4:
                aslaBuilder.setSabmLength(AbmLength.Four);
                flags = BitArray.valueOf(buffer, FLAGS_FOUR_SIZE);
                aslaBuilder.setStandardBitMask(new AslaBitMask(flags.get(SABM_RSVP_FLAG), flags.get(SABM_SR_FLAG),
                    flags.get(SABM_LFA_FLAG)));
                break;
            case 8:
                aslaBuilder.setSabmLength(AbmLength.Eight);
                flags = BitArray.valueOf(buffer, FLAGS_EIGHT_SIZE);
                aslaBuilder.setStandardBitMask(new AslaBitMask(flags.get(SABM_RSVP_FLAG), flags.get(SABM_SR_FLAG),
                    flags.get(SABM_LFA_FLAG)));
                break;
            default:
                aslaBuilder.setSabmLength(AbmLength.Zero);
        }
        switch (ulength) {
            case 4:
                aslaBuilder.setUdabmLength(AbmLength.Four);
                aslaBuilder.setUserBitMask(readUint32(buffer).toUint64());
                break;
            case 8:
                aslaBuilder.setUdabmLength(AbmLength.Eight);
                aslaBuilder.setUserBitMask(readUint64(buffer));
                break;
            default:
                aslaBuilder.setUdabmLength(AbmLength.Zero);
        }

        // Then read ASLA subTLVs
        final AslaSubtlvsBuilder subTlvBuilder = new AslaSubtlvsBuilder();
        final Multimap<Integer, ByteBuf> attributes = LinkstateAttributeParser.getAttributesMap(buffer);
        for (final Entry<Integer, ByteBuf> entry : attributes.entries()) {
            final int type = entry.getKey();
            final ByteBuf value = entry.getValue();
            switch (type) {
                case ADMIN_GROUP:
                    subTlvBuilder.setAdminGroup(new AdministrativeGroup(readUint32(value)));
                    LOG.debug("Parsed ASLA Administrative Group {}", subTlvBuilder.getAdminGroup());
                    break;
                case TE_METRIC:
                    subTlvBuilder.setTeMetric(new TeMetric(readUint32(value)));
                    LOG.debug("Parsed ASLA Metric {}", subTlvBuilder.getTeMetric());
                    break;
                case SHARED_RISK_LINK_GROUP:
                    subTlvBuilder.setSharedRiskLinkGroups(parseSrlg(value));
                    LOG.debug("Parsed ASLA Shared Risk Link Group (SRLG) {}", subTlvBuilder.getSharedRiskLinkGroups());
                    break;
                // Performance Metrics
                case LINK_DELAY:
                    subTlvBuilder.setLinkDelay(new Delay(readUint32(value)));
                    LOG.debug("Parsed ASLA Link Delay {}", subTlvBuilder.getLinkDelay());
                    break;
                case LINK_MIN_MAX_DELAY:
                    subTlvBuilder.setLinkMinMaxDelay(new LinkMinMaxDelayBuilder()
                        .setMinDelay(new Delay(readUint32(value)))
                        .setMaxDelay(new Delay(readUint32(value)))
                        .build());
                    LOG.debug("Parsed ASLA Link Min/Max Delay {}", subTlvBuilder.getLinkMinMaxDelay());
                    break;
                case DELAY_VARIATION:
                    subTlvBuilder.setDelayVariation(new Delay(readUint32(value)));
                    LOG.debug("Parsed ASLA Delay Variation {}", subTlvBuilder.getDelayVariation());
                    break;
                case LINK_LOSS:
                    subTlvBuilder.setLinkLoss(new Loss(readUint32(value)));
                    LOG.debug("Parsed ASLA Link Loss {}", subTlvBuilder.getLinkLoss());
                    break;
                case RESIDUAL_BANDWIDTH:
                    subTlvBuilder.setResidualBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed ASLA Residual Bandwidth {}", subTlvBuilder.getResidualBandwidth());
                    break;
                case AVAILABLE_BANDWIDTH:
                    subTlvBuilder.setAvailableBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed ASLA Available Bandwidth {}", subTlvBuilder.getAvailableBandwidth());
                    break;
                case UTILIZED_BANDWIDTH:
                    subTlvBuilder.setUtilizedBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed ASLA Utilized Bandwidth {}", subTlvBuilder.getUtilizedBandwidth());
                    break;
                case EXTENDED_ADMIN_GROUP:
                    subTlvBuilder.setExtendedAdminGroup(SrFlexAlgoParser.parseExtendedAdminGroup(value));
                    LOG.debug("Parsed ASLA Extended Administrative Group {}", subTlvBuilder.getExtendedAdminGroup());
                    break;
                default:
                    LOG.debug("Unsupported ASLA subTLVs: {}", type);
            }
        }
        aslaBuilder.setAslaSubtlvs(subTlvBuilder.build());

        return aslaBuilder.build();
    }

    private static L2BundleMember parseL2BundleMember(final ByteBuf buffer, final ProtocolId protocolId) {
        final L2BundleMemberBuilder l2Builder = new L2BundleMemberBuilder();

        l2Builder.setDescriptor(readUint32(buffer));

        // Parse SubTLVs
        final L2SubtlvsBuilder builder = new L2SubtlvsBuilder();
        final Multimap<Integer, ByteBuf> attributes = LinkstateAttributeParser.getAttributesMap(buffer);
        final List<SrAdjIds> srAdjIds = new ArrayList<SrAdjIds>();
        final List<SrLanAdjIds> srLanAdjIds = new ArrayList<SrLanAdjIds>();
        for (final Entry<Integer, ByteBuf> entry : attributes.entries()) {
            final int type = entry.getKey();
            final ByteBuf value = entry.getValue();
            switch (type) {
                case ADMIN_GROUP:
                    builder.setAdminGroup(new AdministrativeGroup(readUint32(value)));
                    LOG.debug("Parsed L2 Administrative Group {}", builder.getAdminGroup());
                    break;
                case MAX_BANDWIDTH:
                    builder.setMaxLinkBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed L2 Max Bandwidth {}", builder.getMaxLinkBandwidth());
                    break;
                case MAX_RESERVABLE_BANDWIDTH:
                    builder.setMaxReservableBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed L2 Max Reservable Bandwidth {}", builder.getMaxReservableBandwidth());
                    break;
                case UNRESERVED_BANDWIDTH:
                    builder.setUnreservedBandwidth(parseUnreservedBandwidth(value));
                    LOG.debug("Parsed L2 Unreserved Bandwidth {}", builder.getUnreservedBandwidth());
                    break;
                case TE_METRIC:
                    builder.setTeMetric(new TeMetric(readUint32(value)));
                    LOG.debug("Parsed L2 Metric {}", builder.getTeMetric());
                    break;
                case LINK_PROTECTION_TYPE:
                    builder.setLinkProtection(LinkProtectionType.forValue(value.readShort()));
                    LOG.debug("Parsed L2 Link Protection Type {}", builder.getLinkProtection());
                    break;
                case SR_ADJ_SID:
                    srAdjIds.add(SrLinkAttributesParser.parseAdjacencySegmentIdentifier(value, protocolId));
                    LOG.debug("Parsed L2 Adjacency Segment Identifier :{}", srAdjIds.get(srAdjIds.size() - 1));
                    break;
                case SR_LAN_ADJ_SID:
                    srLanAdjIds.add(SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(value, protocolId));
                    LOG.debug("Parsed L2 Adjacency Segment Identifier :{}", srLanAdjIds.get(srLanAdjIds.size() - 1));
                    break;
                // Performance Metrics
                case LINK_DELAY:
                    builder.setLinkDelay(new Delay(readUint32(value)));
                    LOG.debug("Parsed L2 Link Delay {}", builder.getLinkDelay());
                    break;
                case LINK_MIN_MAX_DELAY:
                    builder.setLinkMinMaxDelay(new LinkMinMaxDelayBuilder()
                        .setMinDelay(new Delay(readUint32(value)))
                        .setMaxDelay(new Delay(readUint32(value)))
                        .build());
                    LOG.debug("Parsed L2 Link Min/Max Delay {}", builder.getLinkMinMaxDelay());
                    break;
                case DELAY_VARIATION:
                    builder.setDelayVariation(new Delay(readUint32(value)));
                    LOG.debug("Parsed L2 Delay Variation {}", builder.getDelayVariation());
                    break;
                case LINK_LOSS:
                    builder.setLinkLoss(new Loss(readUint32(value)));
                    LOG.debug("Parsed L2 Link Loss {}", builder.getLinkLoss());
                    break;
                case RESIDUAL_BANDWIDTH:
                    builder.setResidualBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed L2 Residual Bandwidth {}", builder.getResidualBandwidth());
                    break;
                case AVAILABLE_BANDWIDTH:
                    builder.setAvailableBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed L2 Available Bandwidth {}", builder.getAvailableBandwidth());
                    break;
                case UTILIZED_BANDWIDTH:
                    builder.setUtilizedBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    LOG.debug("Parsed L2 Utilized Bandwidth {}", builder.getUtilizedBandwidth());
                    break;
                default:
                    LOG.debug("Unsupported L2 Bundle subTLVs: {}", type);
            }
        }
        if (!srAdjIds.isEmpty()) {
            builder.setSrAdjIds(srAdjIds);
        }
        if (!srLanAdjIds.isEmpty()) {
            builder.setSrLanAdjIds(srLanAdjIds);
        }
        l2Builder.setL2Subtlvs(builder.build());

        return l2Builder.build();
    }

    private static void ifPresentApply(final Object tlv, final SerializerInterface serializer) {
        if (tlv != null) {
            serializer.check(tlv);
        }
    }

    @SuppressWarnings("unchecked")
    static void serializeLinkAttributes(final LinkAttributesCase linkAttributesCase, final ByteBuf output) {
        final LinkAttributes linkAttributes = linkAttributesCase.getLinkAttributes();
        LOG.trace("Started serializing Link Attributes");
        // Standard Attributes
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
        ifPresentApply(linkAttributes.getOpaqueLink(), value -> TlvUtil.writeTLV(LINK_OPAQUE,
            Unpooled.wrappedBuffer((byte[]) value), output));
        ifPresentApply(linkAttributes.getLinkName(), value -> TlvUtil.writeTLV(LINK_NAME,
            Unpooled.wrappedBuffer(StandardCharsets.UTF_8.encode((String) value)), output));
        // Segment Routing
        ifPresentApply(linkAttributes.getSrAdjIds(),
            value -> SrLinkAttributesParser.serializeAdjacencySegmentIdentifiers((List<SrAdjIds>) value, SR_ADJ_SID,
                output));
        ifPresentApply(linkAttributes.getSrLanAdjIds(),
            value -> SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifiers((List<SrLanAdjIds>) value,
                SR_LAN_ADJ_SID, output));
        ifPresentApply(linkAttributes.getLinkMsd(), value -> TlvUtil.writeTLV(SR_LINK_MSD,
            SrLinkAttributesParser.serializeSrLinkMsd((LinkMsd) value), output));
        // EPE Peer Node
        ifPresentApply(linkAttributes.getPeerNodeSid(), value -> TlvUtil.writeTLV(PEER_NODE_SID_CODE,
            SrLinkAttributesParser.serializeEpeSegmentIdentifier((PeerNodeSid) value), output));
        ifPresentApply(linkAttributes.getPeerAdjSid(), value -> TlvUtil.writeTLV(PEER_ADJ_SID_CODE,
            SrLinkAttributesParser.serializeEpeSegmentIdentifier((PeerAdjSid) value), output));
        ifPresentApply(linkAttributes.getPeerSetSids(),
            value -> SrLinkAttributesParser.serializeEpeSegmentIdentifiers((List<EpeSidTlv>) value,
                PEER_SET_SID_CODE, output));
        // SRv6
        serializedSrv6EndXSID(linkAttributes.getSrv6EndXSid(), output);
        serializedSrv6LanEndXSID(linkAttributes.getSrv6LanEndXSid(), output);
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
        // Application Specific Link Attributes
        serializeAsla(linkAttributes.getAsla(), output);
        // Extended Admin Group
        serializeExtendedAdminGroup(linkAttributes.getExtendedAdminGroup(), output);
        // L2 Bundle Member
        serializeL2BundleMembers(linkAttributes.getL2BundleMember(), output);
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
            final ByteBuf srlgBuff = Unpooled.buffer();
            srlgList.forEach(srlgId -> writeUint32(srlgBuff, srlgId.getValue()));
            TlvUtil.writeTLV(SHARED_RISK_LINK_GROUP, srlgBuff, byteAggregator);
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

    private static void serializedSrv6EndXSID(Srv6EndXSid srv6EndXSid, final ByteBuf byteAggregator) {
        if (srv6EndXSid != null) {
            final ByteBuf output = Unpooled.buffer();
            SRv6AttributesParser.serialiseSrv6EndXSid(srv6EndXSid, output);
            TlvUtil.writeTLV(SRV6_END_X_SID, output, byteAggregator);
        }
    }

    private static void serializedSrv6LanEndXSID(Srv6LanEndXSid srv6LanEndXSid, final ByteBuf byteAggregator) {
        if (srv6LanEndXSid != null) {
            final ByteBuf output = Unpooled.buffer();
            SRv6AttributesParser.serialiseSrv6LanEndXSid(srv6LanEndXSid, output);
            if (srv6LanEndXSid.getNeighborType() instanceof IsisNeighborCase) {
                TlvUtil.writeTLV(ISIS_SRV6_LAN_SID, output, byteAggregator);
            } else if (srv6LanEndXSid.getNeighborType() instanceof Ospfv3NeighborCase) {
                TlvUtil.writeTLV(OSPFV3_SRV6_LAN_SID, output, byteAggregator);
            }
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

    private static void serializeAsla(final Asla asla, final ByteBuf byteAggregator) {
        if (asla == null) {
            return;
        }

        final ByteBuf output = Unpooled.buffer();
        final AslaSubtlvs aslaSubtlv = asla.getAslaSubtlvs();

        LOG.trace("Started serializing ASLA attributes");
        output.writeByte(asla.getSabmLength().getIntValue());
        output.writeByte(asla.getUdabmLength().getIntValue());
        output.writeZero(RESERVED);
        if (asla.getSabmLength() != AbmLength.Zero) {
            final BitArray bs = new BitArray(FLAGS_FOUR_SIZE);
            bs.set(SABM_RSVP_FLAG, asla.getStandardBitMask().getRsvpTe());
            bs.set(SABM_SR_FLAG, asla.getStandardBitMask().getSr());
            bs.set(SABM_LFA_FLAG, asla.getStandardBitMask().getLfa());
            bs.toByteBuf(output);
        }
        if (asla.getSabmLength() == AbmLength.Eight) {
            output.writeZero(RESERVED);
        }
        if (asla.getUdabmLength() == AbmLength.Four) {
            writeUint32(output, asla.getUserBitMask().toUint32());
        } else if (asla.getUdabmLength() == AbmLength.Eight) {
            writeUint64(output, asla.getUserBitMask());
        }

        // Asla SubTlvs
        ifPresentApply(aslaSubtlv.getAdminGroup(), value -> TlvUtil.writeTLV(ADMIN_GROUP,
                Unpooled.copyInt(((AdministrativeGroup) value).getValue().intValue()), output));
        ifPresentApply(aslaSubtlv.getTeMetric(), value -> TlvUtil.writeTLV(TE_METRIC,
                Unpooled.copyLong(((TeMetric) value).getValue().toJava()), output));
        serializeSrlg(aslaSubtlv.getSharedRiskLinkGroups(), output);
        // Performance Metrics
        ifPresentApply(aslaSubtlv.getLinkDelay(), value -> TlvUtil.writeTLV(LINK_DELAY,
            Unpooled.copyInt(((Delay) value).getValue().intValue()), output));
        serializeLinkMinMaxDelay(aslaSubtlv.getLinkMinMaxDelay(), output);
        ifPresentApply(aslaSubtlv.getDelayVariation(), value -> TlvUtil.writeTLV(DELAY_VARIATION,
            Unpooled.copyInt(((Delay) value).getValue().intValue()), output));
        ifPresentApply(aslaSubtlv.getLinkLoss(), value -> TlvUtil.writeTLV(LINK_LOSS,
            Unpooled.copyInt(((Loss) value).getValue().intValue()), output));
        ifPresentApply(aslaSubtlv.getResidualBandwidth(), value -> TlvUtil.writeTLV(RESIDUAL_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        ifPresentApply(aslaSubtlv.getAvailableBandwidth(), value -> TlvUtil.writeTLV(AVAILABLE_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        ifPresentApply(aslaSubtlv.getUtilizedBandwidth(), value -> TlvUtil.writeTLV(UTILIZED_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        // Extended Admin Group
        serializeExtendedAdminGroup(aslaSubtlv.getExtendedAdminGroup(), output);

        LOG.trace("Finished serializing L2 Bundle");
        TlvUtil.writeTLV(ASLA, output, byteAggregator);
    }

    private static void serializeExtendedAdminGroup(final Set<ExtendedAdminGroup> eags, final ByteBuf byteAggregator) {
        if (eags != null) {
            final ByteBuf extendedBuf = Unpooled.buffer();
            eags.forEach(id -> writeUint32(extendedBuf, id.getValue()));
            TlvUtil.writeTLV(EXTENDED_ADMIN_GROUP, extendedBuf, byteAggregator);
        }
    }

    private static void serializeL2BundleMembers(final List<L2BundleMember> bundles, final ByteBuf byteAggregator) {
        if (bundles != null) {
            bundles.forEach(
                bundle -> TlvUtil.writeTLV(L2_BUNDLE_MEMBER, serializeL2BundleMember(bundle), byteAggregator));
        }
    }

    @SuppressWarnings("unchecked")
    private static ByteBuf serializeL2BundleMember(final L2BundleMember bundle) {
        final ByteBuf output = Unpooled.buffer();
        final L2Subtlvs tlvs = bundle.getL2Subtlvs();

        LOG.trace("Started serializing L2 Bundle Member attributes");
        writeUint32(output, bundle.getDescriptor());

        // Standard Attributes
        ifPresentApply(tlvs.getAdminGroup(), value -> TlvUtil.writeTLV(ADMIN_GROUP,
            Unpooled.copyInt(((AdministrativeGroup) value).getValue().intValue()), output));
        ifPresentApply(tlvs.getMaxLinkBandwidth(), value -> TlvUtil.writeTLV(MAX_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        ifPresentApply(tlvs.getMaxReservableBandwidth(), value -> TlvUtil.writeTLV(MAX_RESERVABLE_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        serializeUnreservedBw(tlvs.getUnreservedBandwidth(), output);
        ifPresentApply(tlvs.getTeMetric(), value -> TlvUtil.writeTLV(TE_METRIC,
            Unpooled.copyLong(((TeMetric) value).getValue().toJava()), output));
        ifPresentApply(tlvs.getLinkProtection(), value -> TlvUtil.writeTLV(LINK_PROTECTION_TYPE,
            Unpooled.copyShort(((LinkProtectionType) value).getIntValue()), output));

        // Segment Routing
        ifPresentApply(tlvs.getSrAdjIds(),
            value -> SrLinkAttributesParser.serializeAdjacencySegmentIdentifiers((List<SrAdjIds>) value, SR_ADJ_SID,
                output));
        ifPresentApply(tlvs.getSrLanAdjIds(),
            value -> SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifiers((List<SrLanAdjIds>) value,
                SR_LAN_ADJ_SID, output));

        // Performance Metrics
        ifPresentApply(tlvs.getLinkDelay(), value -> TlvUtil.writeTLV(LINK_DELAY,
            Unpooled.copyInt(((Delay) value).getValue().intValue()), output));
        serializeLinkMinMaxDelay(tlvs.getLinkMinMaxDelay(), output);
        ifPresentApply(tlvs.getDelayVariation(), value -> TlvUtil.writeTLV(DELAY_VARIATION,
            Unpooled.copyInt(((Delay) value).getValue().intValue()), output));
        ifPresentApply(tlvs.getLinkLoss(), value -> TlvUtil.writeTLV(LINK_LOSS,
            Unpooled.copyInt(((Loss) value).getValue().intValue()), output));
        ifPresentApply(tlvs.getResidualBandwidth(), value -> TlvUtil.writeTLV(RESIDUAL_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        ifPresentApply(tlvs.getAvailableBandwidth(), value -> TlvUtil.writeTLV(AVAILABLE_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        ifPresentApply(tlvs.getUtilizedBandwidth(), value -> TlvUtil.writeTLV(UTILIZED_BANDWIDTH,
            Unpooled.wrappedBuffer(((Bandwidth) value).getValue()), output));
        LOG.trace("Finished serializing L2 Bundle");

        return output;
    }

}
