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
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SRv6AttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrFlexAlgoParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrLinkAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.LinkIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.AbmLength;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.AdministrativeGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv6RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Loss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.MplsProtocolMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.asla.tlv.AslaSubtlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.asla.tlv.AslaSubtlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.asla.tlv.StandardBitMaskBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.l2.bundle.member.L2Subtlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.l2.bundle.member.L2SubtlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.AslaMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.AslaMetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.EgressPeerEngineeringBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.L2BundleMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.L2BundleMemberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.PerformanceMetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.SrAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.Srv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.StandardMetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.srv6.Srv6EndXSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.srv6.Srv6LanEndXSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.LinkAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.peer.engineering.PeerAdjSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.peer.engineering.PeerNodeSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.peer.engineering.PeerSetSids;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.peer.engineering.PeerSetSidsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.performance.attributes.LinkMinMaxDelay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.performance.attributes.LinkMinMaxDelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.sr.attributes.SrAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.sr.attributes.SrLanAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.standard.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.standard.attributes.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.standard.attributes.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.ExtendedAdminGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.srv6.lan.end.x.sid.neighbor.type.IsisNeighborCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.srv6.lan.end.x.sid.neighbor.type.Ospfv3NeighborCase;
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
    private static final int FLAGS_FOUR_SIZE = 4;
    private static final int FLAGS_EIGHT_SIZE = 8;
    private static final int SABM_RSVP_FLAG = 0;
    private static final int SABM_SR_FLAG = 1;
    private static final int SABM_LFA_FLAG = 2;
    private static final int SABM_FLEX_ALGO_FLAG = 3;
    private static final int RESERVED = 2;


    // L2 bundle RFC9085
    private static final int L2_BUNDLE_MEMBER = 1172;

    // Traffic Engineering Extended Administrative Groups RFC9104
    private static final int EXTENDED_ADMIN_GROUP = 1173;

    private LinkAttributesParser() {
        // Hidden on purpose
    }

    @FunctionalInterface
    private interface SerializerInterface<T> {

        void check(@NonNull T cont);
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
        final StandardMetricBuilder smBuilder = new StandardMetricBuilder();
        final PerformanceMetricBuilder pmBuilder = new PerformanceMetricBuilder();
        final SrAttributeBuilder srBuilder = new SrAttributeBuilder();
        final EgressPeerEngineeringBuilder epeBuilder = new EgressPeerEngineeringBuilder();
        final Srv6Builder srv6Builder = new Srv6Builder();
        final var srAdjIds = new ArrayList<SrAdjIds>();
        final var srLanAdjIds = new ArrayList<SrLanAdjIds>();
        final var peerSetSids = new ArrayList<PeerSetSids>();
        final var l2bundles = new ArrayList<L2BundleMember>();
        boolean standardMetric = false;
        boolean performanceMetric = false;
        boolean srAttribute = false;
        boolean epe = false;
        boolean srv6 = false;

        for (var entry : attributes.entries()) {
            final int key = entry.getKey();
            LOG.trace("Link attribute TLV {}", key);

            final ByteBuf value = entry.getValue();
            switch (key) {
                case LinkIdTlvParser.LINK_LR_IDENTIFIERS:
                    builder.setLinkLocalIdentifier(readUint32(value));
                    builder.setLinkRemoteIdentifier(readUint32(value));
                    LOG.debug("Parsed Link Local and Remote Identifier: {} / {}", builder.getLinkLocalIdentifier(),
                        builder.getLinkRemoteIdentifier());
                    break;
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
                    smBuilder.setAdminGroup(new AdministrativeGroup(readUint32(value)));
                    standardMetric = true;
                    LOG.debug("Parsed Administrative Group: {}", smBuilder.getAdminGroup());
                    break;
                case MAX_BANDWIDTH:
                    smBuilder.setMaxLinkBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    standardMetric = true;
                    LOG.debug("Parsed Max Bandwidth: {}", smBuilder.getMaxLinkBandwidth());
                    break;
                case MAX_RESERVABLE_BANDWIDTH:
                    smBuilder.setMaxReservableBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    standardMetric = true;
                    LOG.debug("Parsed Max Reservable Bandwidth: {}", smBuilder.getMaxReservableBandwidth());
                    break;
                case UNRESERVED_BANDWIDTH:
                    smBuilder.setUnreservedBandwidth(parseUnreservedBandwidth(value));
                    standardMetric = true;
                    LOG.debug("Parsed Unreserved Bandwidth: {}", smBuilder.getUnreservedBandwidth());
                    break;
                case TE_METRIC:
                    smBuilder.setTeMetric(new TeMetric(readUint32(value)));
                    standardMetric = true;
                    LOG.debug("Parsed Metric: {}", smBuilder.getTeMetric());
                    break;
                case LINK_PROTECTION_TYPE:
                    smBuilder.setLinkProtection(LinkProtectionType.forValue(value.readShort()));
                    standardMetric = true;
                    LOG.debug("Parsed Link Protection Type: {}", smBuilder.getLinkProtection());
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
                    srAttribute = true;
                    LOG.debug("Parsed Adjacency Segment Identifier: {}", srAdjIds.get(srAdjIds.size() - 1));
                    break;
                case SR_LAN_ADJ_SID:
                    srLanAdjIds.add(SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(value, protocolId));
                    srAttribute = true;
                    LOG.debug("Parsed Adjacency Segment Identifier: {}", srLanAdjIds.get(srLanAdjIds.size() - 1));
                    break;
                case SR_LINK_MSD:
                    srBuilder.setLinkMsd(SrLinkAttributesParser.parseSrLinkMsd(value));
                    srAttribute = true;
                    LOG.debug("Parsed SR Link MSD: {}", srBuilder.getLinkMsd());
                    break;
                // Egress Peer Engineering
                case PEER_NODE_SID_CODE:
                    epeBuilder.setPeerNodeSid(new PeerNodeSidBuilder(
                        SrLinkAttributesParser.parseEpeSegmentIdentifier(value)).build());
                    epe = true;
                    LOG.debug("Parsed Peer Node Segment Identifier: {}", epeBuilder.getPeerNodeSid());
                    break;
                case PEER_ADJ_SID_CODE:
                    epeBuilder.setPeerAdjSid(new PeerAdjSidBuilder(
                        SrLinkAttributesParser.parseEpeSegmentIdentifier(value)).build());
                    epe = true;
                    LOG.debug("Parsed Peer Ajacency Segment Identifier: {}", epeBuilder.getPeerAdjSid());
                    break;
                case PEER_SET_SID_CODE:
                    peerSetSids.add(new PeerSetSidsBuilder(
                        SrLinkAttributesParser.parseEpeSegmentIdentifier(value)).build());
                    epe = true;
                    LOG.debug("Parsed Peer Set Sid: {}", peerSetSids.get(peerSetSids.size() - 1));
                    break;
                // SRv6
                case SRV6_END_X_SID:
                    srv6Builder.setSrv6EndXSid(SRv6AttributesParser.parseSrv6EndXSid(value));
                    srv6 = true;
                    LOG.debug("Parsed SRv6 End X SID: {}", srv6Builder.getSrv6EndXSid());
                    break;
                case ISIS_SRV6_LAN_SID:
                case OSPFV3_SRV6_LAN_SID:
                    srv6Builder.setSrv6LanEndXSid(SRv6AttributesParser.parseSrv6LanEndXSid(value, protocolId));
                    srv6 = true;
                    LOG.debug("Parsed SRv6 LAN End X SID: {}", srv6Builder.getSrv6LanEndXSid());
                    break;
                // Performance Metrics
                case LINK_DELAY:
                    pmBuilder.setLinkDelay(new Delay(readUint32(value)));
                    performanceMetric = true;
                    LOG.debug("Parsed Link Delay: {}", pmBuilder.getLinkDelay());
                    break;
                case LINK_MIN_MAX_DELAY:
                    pmBuilder.setLinkMinMaxDelay(new LinkMinMaxDelayBuilder()
                        .setMinDelay(new Delay(readUint32(value)))
                        .setMaxDelay(new Delay(readUint32(value)))
                        .build());
                    performanceMetric = true;
                    LOG.debug("Parsed Link Min/Max Delay: {}", pmBuilder.getLinkMinMaxDelay());
                    break;
                case DELAY_VARIATION:
                    pmBuilder.setDelayVariation(new Delay(readUint32(value)));
                    performanceMetric = true;
                    LOG.debug("Parsed Delay Variation: {}", pmBuilder.getDelayVariation());
                    break;
                case LINK_LOSS:
                    pmBuilder.setLinkLoss(new Loss(readUint32(value)));
                    performanceMetric = true;
                    LOG.debug("Parsed Link Loss: {}", pmBuilder.getLinkLoss());
                    break;
                case RESIDUAL_BANDWIDTH:
                    pmBuilder.setResidualBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    performanceMetric = true;
                    LOG.debug("Parsed Residual Bandwidth: {}", pmBuilder.getResidualBandwidth());
                    break;
                case AVAILABLE_BANDWIDTH:
                    pmBuilder.setAvailableBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    performanceMetric = true;
                    LOG.debug("Parsed Available Bandwidth: {}", pmBuilder.getAvailableBandwidth());
                    break;
                case UTILIZED_BANDWIDTH:
                    pmBuilder.setUtilizedBandwidth(new Bandwidth(ByteArray.readAllBytes(value)));
                    performanceMetric = true;
                    LOG.debug("Parsed Utilized Bandwidth: {}", pmBuilder.getUtilizedBandwidth());
                    break;
                // Application Specific Link Attributes
                case ASLA:
                    builder.setAslaMetric(parseAsla(value));
                    LOG.debug("Parsed Application Specific Link Attribute (ASLA): {}", builder.getAslaMetric());
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
            srBuilder.setSrAdjIds(srAdjIds);
        }
        if (!srLanAdjIds.isEmpty()) {
            srBuilder.setSrLanAdjIds(srLanAdjIds);
        }
        if (!peerSetSids.isEmpty()) {
            epeBuilder.setPeerSetSids(peerSetSids);
        }
        if (!l2bundles.isEmpty()) {
            builder.setL2BundleMember(l2bundles);
        }
        if (standardMetric) {
            builder.setStandardMetric(smBuilder.build());
        }
        if (performanceMetric) {
            builder.setPerformanceMetric(pmBuilder.build());
        }
        if (srAttribute) {
            builder.setSrAttribute(srBuilder.build());
        }
        if (epe) {
            builder.setEgressPeerEngineering(epeBuilder.build());
        }
        if (srv6) {
            builder.setSrv6(srv6Builder.build());
        }
        LOG.trace("Finished parsing Link Attributes.");
        return new LinkAttributesCaseBuilder().setLinkAttributes(builder.build()).build();
    }

    private static Map<UnreservedBandwidthKey, UnreservedBandwidth> parseUnreservedBandwidth(final ByteBuf value) {
        final var unreservedBandwidth =
            BindingMap.<UnreservedBandwidthKey, UnreservedBandwidth>orderedBuilder(UNRESERVED_BW_COUNT);
        for (int i = 0; i < UNRESERVED_BW_COUNT; i++) {
            final var v = value.readSlice(BANDWIDTH_LENGTH);
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

    private static AslaMetric parseAsla(final ByteBuf buffer) {
        final AslaMetricBuilder aslaBuilder = new AslaMetricBuilder();

        // Start reading Standard and User Defined Bit Mask length ...
        final int alength = buffer.readByte();
        final int ulength = buffer.readByte();
        buffer.skipBytes(RESERVED);
        // .. to determine how many bytes must be read in the buffer i.e. 0, 4 or 8 bytes
        if (alength == FLAGS_FOUR_SIZE || alength == FLAGS_EIGHT_SIZE) {
            aslaBuilder.setSabmLength(AbmLength.forValue(alength));
            final var flags = BitArray.valueOf(buffer, alength * FLAGS_SIZE);
            aslaBuilder.setStandardBitMask(new StandardBitMaskBuilder()
                .setRsvpTe(flags.get(SABM_RSVP_FLAG))
                .setSr(flags.get(SABM_SR_FLAG))
                .setLfa(flags.get(SABM_LFA_FLAG))
                .setFlexAlgo(flags.get(SABM_FLEX_ALGO_FLAG))
                .build());
        }
        if (ulength == FLAGS_FOUR_SIZE) {
            aslaBuilder.setUdabmLength(AbmLength.Four);
            aslaBuilder.setUserBitMask(readUint32(buffer).toUint64());
        } else if (ulength == FLAGS_EIGHT_SIZE) {
            aslaBuilder.setUdabmLength(AbmLength.Eight);
            aslaBuilder.setUserBitMask(readUint64(buffer));
        }

        // Then read ASLA subTLVs
        final var subTlvBuilder = new AslaSubtlvsBuilder();
        final var attributes = LinkstateAttributeParser.getAttributesMap(buffer);
        for (var entry : attributes.entries()) {
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
        final var l2Builder = new L2BundleMemberBuilder();

        l2Builder.setDescriptor(readUint32(buffer));

        // Parse SubTLVs
        final var builder = new L2SubtlvsBuilder();
        final var attributes = LinkstateAttributeParser.getAttributesMap(buffer);
        final var srAdjIds = new ArrayList<SrAdjIds>();
        final var srLanAdjIds = new ArrayList<SrLanAdjIds>();
        for (var entry : attributes.entries()) {
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

    private static <T> void ifPresentApply(final T tlv, final SerializerInterface<T> serializer) {
        if (tlv != null) {
            serializer.check(tlv);
        }
    }

    static void serializeLinkAttributes(final LinkAttributesCase linkAttributesCase, final ByteBuf output) {
        final var linkAttributes = linkAttributesCase.getLinkAttributes();
        LOG.trace("Started serializing Link Attributes");
        // Standard Attributes
        ifPresentApply(linkAttributes.getLocalIpv4RouterId(), value -> TlvUtil.writeTLV(TlvUtil.LOCAL_IPV4_ROUTER_ID,
            Ipv4Util.byteBufForAddress(value), output));
        ifPresentApply(linkAttributes.getLocalIpv6RouterId(), value -> TlvUtil.writeTLV(TlvUtil.LOCAL_IPV6_ROUTER_ID,
            Ipv6Util.byteBufForAddress(value), output));
        ifPresentApply(linkAttributes.getRemoteIpv4RouterId(), value -> TlvUtil.writeTLV(REMOTE_IPV4_ROUTER_ID,
            Ipv4Util.byteBufForAddress(value), output));
        ifPresentApply(linkAttributes.getRemoteIpv6RouterId(), value -> TlvUtil.writeTLV(REMOTE_IPV6_ROUTER_ID,
            Ipv6Util.byteBufForAddress(value), output));
        final var sm = linkAttributes.getStandardMetric();
        if (sm != null) {
            ifPresentApply(sm.getAdminGroup(), value -> TlvUtil.writeTLV(ADMIN_GROUP,
                Unpooled.copyInt(value.getValue().intValue()), output));
            ifPresentApply(sm.getMaxLinkBandwidth(), value -> TlvUtil.writeTLV(MAX_BANDWIDTH,
                Unpooled.wrappedBuffer(value.getValue()), output));
            ifPresentApply(sm.getMaxReservableBandwidth(), value -> TlvUtil.writeTLV(MAX_RESERVABLE_BANDWIDTH,
                Unpooled.wrappedBuffer(value.getValue()), output));
            serializeUnreservedBw(sm.getUnreservedBandwidth(), output);
            ifPresentApply(sm.getTeMetric(), value -> TlvUtil.writeTLV(TE_METRIC,
                Unpooled.copyLong(value.getValue().toJava()), output));
            ifPresentApply(sm.getLinkProtection(), value -> TlvUtil.writeTLV(LINK_PROTECTION_TYPE,
                Unpooled.copyShort(value.getIntValue()), output));
        }
        serializeMplsProtocolMask(linkAttributes.getMplsProtocol(), output);
        ifPresentApply(linkAttributes.getMetric(), value -> TlvUtil.writeTLV(METRIC,
            Unpooled.copyMedium(value.getValue().intValue()), output));
        serializeSrlg(linkAttributes.getSharedRiskLinkGroups(), output);
        ifPresentApply(linkAttributes.getOpaqueLink(), value -> TlvUtil.writeTLV(LINK_OPAQUE,
            Unpooled.wrappedBuffer(value), output));
        ifPresentApply(linkAttributes.getLinkName(), value -> TlvUtil.writeTLV(LINK_NAME,
            Unpooled.wrappedBuffer(StandardCharsets.UTF_8.encode(value)), output));
        // Segment Routing
        final var sr = linkAttributes.getSrAttribute();
        if (sr != null) {
            ifPresentApply(sr.getSrAdjIds(),
                value -> SrLinkAttributesParser.serializeAdjacencySegmentIdentifiers(value, SR_ADJ_SID, output));
            ifPresentApply(sr.getSrLanAdjIds(),
                value -> SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifiers(value, SR_LAN_ADJ_SID, output));
            ifPresentApply(sr.getLinkMsd(), value -> TlvUtil.writeTLV(SR_LINK_MSD,
                SrLinkAttributesParser.serializeSrLinkMsd(value), output));
        }
        // EPE Peer Node
        final var epe = linkAttributes.getEgressPeerEngineering();
        if (epe != null) {
            ifPresentApply(epe.getPeerNodeSid(), value -> TlvUtil.writeTLV(PEER_NODE_SID_CODE,
                SrLinkAttributesParser.serializeEpeSegmentIdentifier(value), output));
            ifPresentApply(epe.getPeerAdjSid(), value -> TlvUtil.writeTLV(PEER_ADJ_SID_CODE,
                SrLinkAttributesParser.serializeEpeSegmentIdentifier(value), output));
            ifPresentApply(epe.getPeerSetSids(),
                value -> SrLinkAttributesParser.serializeEpeSegmentIdentifiers(value, PEER_SET_SID_CODE, output));
        }
        // SRv6
        final var srv6 = linkAttributes.getSrv6();
        if (srv6 != null) {
            serializedSrv6EndXSID(srv6.getSrv6EndXSid(), output);
            serializedSrv6LanEndXSID(srv6.getSrv6LanEndXSid(), output);
        }
        // Performance Metrics
        final var pm = linkAttributes.getPerformanceMetric();
        if (pm != null) {
            ifPresentApply(pm.getLinkDelay(), value -> TlvUtil.writeTLV(LINK_DELAY,
                Unpooled.copyInt(value.getValue().intValue()), output));
            serializeLinkMinMaxDelay(pm.getLinkMinMaxDelay(), output);
            ifPresentApply(pm.getDelayVariation(), value -> TlvUtil.writeTLV(DELAY_VARIATION,
                Unpooled.copyInt(value.getValue().intValue()), output));
            ifPresentApply(pm.getLinkLoss(), value -> TlvUtil.writeTLV(LINK_LOSS,
                Unpooled.copyInt(value.getValue().intValue()), output));
            ifPresentApply(pm.getResidualBandwidth(), value -> TlvUtil.writeTLV(RESIDUAL_BANDWIDTH,
                Unpooled.wrappedBuffer(value.getValue()), output));
            ifPresentApply(pm.getAvailableBandwidth(), value -> TlvUtil.writeTLV(AVAILABLE_BANDWIDTH,
                Unpooled.wrappedBuffer(value.getValue()), output));
            ifPresentApply(pm.getUtilizedBandwidth(), value -> TlvUtil.writeTLV(UTILIZED_BANDWIDTH,
                Unpooled.wrappedBuffer(value.getValue()), output));
        }
        // Application Specific Link Attributes
        serializeAsla(linkAttributes.getAslaMetric(), output);
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
            for (var unreservedBandwidth : ubList.values()) {
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

    private static void serializedSrv6EndXSID(final Srv6EndXSid srv6EndXSid, final ByteBuf byteAggregator) {
        if (srv6EndXSid != null) {
            final ByteBuf output = Unpooled.buffer();
            SRv6AttributesParser.serialiseSrv6EndXSid(srv6EndXSid, output);
            TlvUtil.writeTLV(SRV6_END_X_SID, output, byteAggregator);
        }
    }

    private static void serializedSrv6LanEndXSID(final Srv6LanEndXSid srv6LanEndXSid, final ByteBuf byteAggregator) {
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

    private static void serializeAsla(final AslaMetric asla, final ByteBuf byteAggregator) {
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
        } else if (asla.getSabmLength() == AbmLength.Eight) {
            output.writeZero(RESERVED);
        }
        if (asla.getUdabmLength() == AbmLength.Four) {
            writeUint32(output, asla.getUserBitMask().toUint32());
        } else if (asla.getUdabmLength() == AbmLength.Eight) {
            writeUint64(output, asla.getUserBitMask());
        }

        // Asla SubTlvs
        ifPresentApply(aslaSubtlv.getAdminGroup(), value -> TlvUtil.writeTLV(ADMIN_GROUP,
            Unpooled.copyInt(value.getValue().intValue()), output));
        ifPresentApply(aslaSubtlv.getTeMetric(), value -> TlvUtil.writeTLV(TE_METRIC,
            Unpooled.copyLong(value.getValue().toJava()), output));
        serializeSrlg(aslaSubtlv.getSharedRiskLinkGroups(), output);
        // Performance Metrics
        ifPresentApply(aslaSubtlv.getLinkDelay(), value -> TlvUtil.writeTLV(LINK_DELAY,
            Unpooled.copyInt(value.getValue().intValue()), output));
        serializeLinkMinMaxDelay(aslaSubtlv.getLinkMinMaxDelay(), output);
        ifPresentApply(aslaSubtlv.getDelayVariation(), value -> TlvUtil.writeTLV(DELAY_VARIATION,
            Unpooled.copyInt(value.getValue().intValue()), output));
        ifPresentApply(aslaSubtlv.getLinkLoss(), value -> TlvUtil.writeTLV(LINK_LOSS,
            Unpooled.copyInt(value.getValue().intValue()), output));
        ifPresentApply(aslaSubtlv.getResidualBandwidth(), value -> TlvUtil.writeTLV(RESIDUAL_BANDWIDTH,
            Unpooled.wrappedBuffer(value.getValue()), output));
        ifPresentApply(aslaSubtlv.getAvailableBandwidth(), value -> TlvUtil.writeTLV(AVAILABLE_BANDWIDTH,
            Unpooled.wrappedBuffer(value.getValue()), output));
        ifPresentApply(aslaSubtlv.getUtilizedBandwidth(), value -> TlvUtil.writeTLV(UTILIZED_BANDWIDTH,
            Unpooled.wrappedBuffer(value.getValue()), output));
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

    private static ByteBuf serializeL2BundleMember(final L2BundleMember bundle) {
        final ByteBuf output = Unpooled.buffer();
        final L2Subtlvs tlvs = bundle.getL2Subtlvs();

        LOG.trace("Started serializing L2 Bundle Member attributes");
        writeUint32(output, bundle.getDescriptor());

        // Standard Attributes
        ifPresentApply(tlvs.getAdminGroup(), value -> TlvUtil.writeTLV(ADMIN_GROUP,
            Unpooled.copyInt(value.getValue().intValue()), output));
        ifPresentApply(tlvs.getMaxLinkBandwidth(), value -> TlvUtil.writeTLV(MAX_BANDWIDTH,
            Unpooled.wrappedBuffer(value.getValue()), output));
        ifPresentApply(tlvs.getMaxReservableBandwidth(), value -> TlvUtil.writeTLV(MAX_RESERVABLE_BANDWIDTH,
            Unpooled.wrappedBuffer(value.getValue()), output));
        serializeUnreservedBw(tlvs.getUnreservedBandwidth(), output);
        ifPresentApply(tlvs.getTeMetric(), value -> TlvUtil.writeTLV(TE_METRIC,
            Unpooled.copyLong(value.getValue().toJava()), output));
        ifPresentApply(tlvs.getLinkProtection(), value -> TlvUtil.writeTLV(LINK_PROTECTION_TYPE,
            Unpooled.copyShort(value.getIntValue()), output));

        // Segment Routing
        ifPresentApply(tlvs.getSrAdjIds(),
            value -> SrLinkAttributesParser.serializeAdjacencySegmentIdentifiers(value, SR_ADJ_SID, output));
        ifPresentApply(tlvs.getSrLanAdjIds(),
            value -> SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifiers(value, SR_LAN_ADJ_SID, output));

        // Performance Metrics
        ifPresentApply(tlvs.getLinkDelay(), value -> TlvUtil.writeTLV(LINK_DELAY,
            Unpooled.copyInt(value.getValue().intValue()), output));
        serializeLinkMinMaxDelay(tlvs.getLinkMinMaxDelay(), output);
        ifPresentApply(tlvs.getDelayVariation(), value -> TlvUtil.writeTLV(DELAY_VARIATION,
            Unpooled.copyInt(value.getValue().intValue()), output));
        ifPresentApply(tlvs.getLinkLoss(), value -> TlvUtil.writeTLV(LINK_LOSS,
            Unpooled.copyInt(value.getValue().intValue()), output));
        ifPresentApply(tlvs.getResidualBandwidth(), value -> TlvUtil.writeTLV(RESIDUAL_BANDWIDTH,
            Unpooled.wrappedBuffer(value.getValue()), output));
        ifPresentApply(tlvs.getAvailableBandwidth(), value -> TlvUtil.writeTLV(AVAILABLE_BANDWIDTH,
            Unpooled.wrappedBuffer(value.getValue()), output));
        ifPresentApply(tlvs.getUtilizedBandwidth(), value -> TlvUtil.writeTLV(UTILIZED_BANDWIDTH,
            Unpooled.wrappedBuffer(value.getValue()), output));
        LOG.trace("Finished serializing L2 Bundle");

        return output;
    }
}
