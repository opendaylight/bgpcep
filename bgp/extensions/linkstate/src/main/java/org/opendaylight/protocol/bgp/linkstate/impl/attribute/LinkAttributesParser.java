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
import io.netty.buffer.ByteBufUtil;
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
                case LinkIdTlvParser.LINK_LR_IDENTIFIERS -> {
                    final var lli = readUint32(value);
                    final var lri = readUint32(value);
                    LOG.debug("Parsed Link Local and Remote Identifier: {} / {}", lli, lri);
                    builder.setLinkLocalIdentifier(lli).setLinkRemoteIdentifier(lri);
                }
                case TlvUtil.LOCAL_IPV4_ROUTER_ID -> {
                    final var ri = new Ipv4RouterIdentifier(Ipv4Util.addressForByteBuf(value));
                    LOG.debug("Parsed IPv4 Router-ID of local node: {}", ri);
                    builder.setLocalIpv4RouterId(ri);
                }
                case TlvUtil.LOCAL_IPV6_ROUTER_ID -> {
                    final var ri = new Ipv6RouterIdentifier(Ipv6Util.addressForByteBuf(value));
                    LOG.debug("Parsed IPv6 Router-ID of local node: {}", ri);
                    builder.setLocalIpv6RouterId(ri);
                }
                case REMOTE_IPV4_ROUTER_ID -> {
                    final var ri = new Ipv4RouterIdentifier(Ipv4Util.addressForByteBuf(value));
                    LOG.debug("Parsed IPv4 Router-ID of remote node: {}", ri);
                    builder.setRemoteIpv4RouterId(ri);
                }
                case REMOTE_IPV6_ROUTER_ID -> {
                    final var ri = new Ipv6RouterIdentifier(Ipv6Util.addressForByteBuf(value));
                    LOG.debug("Parsed IPv6 Router-ID of remote node: {}", ri);
                    builder.setRemoteIpv6RouterId(ri);
                }

                // Standard Metric
                case ADMIN_GROUP -> {
                    final var ag = new AdministrativeGroup(readUint32(value));
                    LOG.debug("Parsed Administrative Group: {}", ag);
                    smBuilder.setAdminGroup(ag);
                    standardMetric = true;
                }
                case MAX_BANDWIDTH -> {
                    final var mb = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed Max Bandwidth: {}", mb);
                    smBuilder.setMaxLinkBandwidth(mb);
                    standardMetric = true;
                }
                case MAX_RESERVABLE_BANDWIDTH -> {
                    final var mrb = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed Max Reservable Bandwidth: {}", mrb);
                    smBuilder.setMaxReservableBandwidth(mrb);
                    standardMetric = true;
                }
                case UNRESERVED_BANDWIDTH -> {
                    final var ub = parseUnreservedBandwidth(value);
                    LOG.debug("Parsed Unreserved Bandwidth: {}", ub);
                    smBuilder.setUnreservedBandwidth(ub);
                    standardMetric = true;
                }
                case TE_METRIC -> {
                    final var tm = new TeMetric(readUint32(value));
                    LOG.debug("Parsed Metric: {}", tm);
                    smBuilder.setTeMetric(tm);
                    standardMetric = true;
                }
                case LINK_PROTECTION_TYPE -> {
                    final var lpt = LinkProtectionType.forValue(value.readShort());
                    LOG.debug("Parsed Link Protection Type: {}", lpt);
                    smBuilder.setLinkProtection(lpt);
                    standardMetric = true;
                }
                case MPLS_PROTOCOL -> {
                    final var bits = BitArray.valueOf(value, FLAGS_SIZE);
                    final var mp = new MplsProtocolMask(bits.get(LDP_BIT), bits.get(RSVP_BIT));
                    LOG.debug("Parsed MPLS Protocols: {}", mp);
                    builder.setMplsProtocol(mp);
                }
                case METRIC -> {
                    // length can 3, 2 or 1
                    final var m = new Metric(Uint32.valueOf(ByteArray.bytesToLong(ByteArray.readAllBytes(value))));
                    LOG.debug("Parsed Metric: {}", m);
                    builder.setMetric(m);
                }
                case SHARED_RISK_LINK_GROUP -> {
                    final var srlg = parseSrlg(value);
                    LOG.debug("Parsed Shared Risk Link Groups: {}", srlg);
                    builder.setSharedRiskLinkGroups(srlg);
                }
                case LINK_OPAQUE -> {
                    final var lo = ByteArray.readAllBytes(value);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Parsed Opaque value: {}", ByteBufUtil.hexDump(lo));
                    }
                    builder.setOpaqueLink(lo);
                }
                case LINK_NAME -> {
                    final var ln = new String(ByteArray.readAllBytes(value), StandardCharsets.US_ASCII);
                    LOG.debug("Parsed Link Name: {}", ln);
                    builder.setLinkName(ln);
                }

                // Segment Routing
                case SR_ADJ_SID -> {
                    final var adj = SrLinkAttributesParser.parseAdjacencySegmentIdentifier(value, protocolId);
                    LOG.debug("Parsed Adjacency Segment Identifier: {}", adj);
                    srAdjIds.add(adj);
                    srAttribute = true;
                }
                case SR_LAN_ADJ_SID -> {
                    final var adj = SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(value, protocolId);
                    LOG.debug("Parsed Adjacency Segment Identifier: {}", adj);
                    srLanAdjIds.add(adj);
                    srAttribute = true;
                }
                case SR_LINK_MSD -> {
                    final var msd = SrLinkAttributesParser.parseSrLinkMsd(value);
                    LOG.debug("Parsed SR Link MSD: {}", msd);
                    srBuilder.setLinkMsd(msd);
                    srAttribute = true;
                }

                // Egress Peer Engineering
                case PEER_NODE_SID_CODE -> {
                    final var sid = new PeerNodeSidBuilder(SrLinkAttributesParser.parseEpeSegmentIdentifier(value))
                        .build();
                    LOG.debug("Parsed Peer Node Segment Identifier: {}", sid);
                    epeBuilder.setPeerNodeSid(sid);
                    epe = true;
                }
                case PEER_ADJ_SID_CODE -> {
                    final var sid = new PeerAdjSidBuilder(SrLinkAttributesParser.parseEpeSegmentIdentifier(value))
                        .build();
                    LOG.debug("Parsed Peer Ajacency Segment Identifier: {}", sid);
                    epeBuilder.setPeerAdjSid(sid);
                    epe = true;
                }
                case PEER_SET_SID_CODE -> {
                    final var sid = new PeerSetSidsBuilder(SrLinkAttributesParser.parseEpeSegmentIdentifier(value))
                        .build();
                    LOG.debug("Parsed Peer Set Sid: {}", sid);
                    peerSetSids.add(sid);
                    epe = true;
                }

                // SRv6
                case SRV6_END_X_SID -> {
                    final var sid = SRv6AttributesParser.parseSrv6EndXSid(value);
                    LOG.debug("Parsed SRv6 End X SID: {}", sid);
                    srv6Builder.setSrv6EndXSid(sid);
                    srv6 = true;
                }
                case ISIS_SRV6_LAN_SID, OSPFV3_SRV6_LAN_SID -> {
                    final var sid = SRv6AttributesParser.parseSrv6LanEndXSid(value, protocolId);
                    LOG.debug("Parsed SRv6 LAN End X SID: {}", sid);
                    srv6Builder.setSrv6LanEndXSid(sid);
                    srv6 = true;
                }

                // Performance Metrics
                case LINK_DELAY -> {
                    final var ld = new Delay(readUint32(value));
                    LOG.debug("Parsed Link Delay: {}", ld);
                    pmBuilder.setLinkDelay(ld);
                    performanceMetric = true;
                }
                case LINK_MIN_MAX_DELAY -> {
                    final var lmmd = new LinkMinMaxDelayBuilder()
                        .setMinDelay(new Delay(readUint32(value)))
                        .setMaxDelay(new Delay(readUint32(value)))
                        .build();
                    LOG.debug("Parsed Link Min/Max Delay: {}", lmmd);
                    pmBuilder.setLinkMinMaxDelay(lmmd);
                    performanceMetric = true;
                }
                case DELAY_VARIATION -> {
                    final var dv = new Delay(readUint32(value));
                    LOG.debug("Parsed Delay Variation: {}", dv);
                    pmBuilder.setDelayVariation(dv);
                    performanceMetric = true;
                }
                case LINK_LOSS -> {
                    final var ll = new Loss(readUint32(value));
                    LOG.debug("Parsed Link Loss: {}", ll);
                    pmBuilder.setLinkLoss(ll);
                    performanceMetric = true;
                }
                case RESIDUAL_BANDWIDTH -> {
                    final var rb = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed Residual Bandwidth: {}", rb);
                    pmBuilder.setResidualBandwidth(rb);
                    performanceMetric = true;
                }
                case AVAILABLE_BANDWIDTH -> {
                    final var ab = new Bandwidth(ByteArray.readAllBytes(value));
                    pmBuilder.setAvailableBandwidth(ab);
                    LOG.debug("Parsed Available Bandwidth: {}", ab);
                    performanceMetric = true;
                }
                case UTILIZED_BANDWIDTH -> {
                    final var ub = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed Utilized Bandwidth: {}", ub);
                    pmBuilder.setUtilizedBandwidth(ub);
                    performanceMetric = true;
                }

                // Application Specific Link Attributes
                case ASLA -> {
                    final var am = parseAsla(value);
                    LOG.debug("Parsed Application Specific Link Attribute (ASLA): {}", am);
                    builder.setAslaMetric(am);
                }
                case EXTENDED_ADMIN_GROUP -> {
                    final var eag = SrFlexAlgoParser.parseExtendedAdminGroup(value);
                    LOG.debug("Parsed Extended Administrative Group: {}", eag);
                    builder.setExtendedAdminGroup(eag);
                }
                case L2_BUNDLE_MEMBER -> {
                    final var bm = parseL2BundleMember(value, protocolId);
                    LOG.debug("Parsed L2 Bundle Member: {}", bm);
                    l2bundles.add(bm);
                }
                default -> LOG.warn("TLV {} is not a recognized link attribute, ignoring it", key);
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
            aslaBuilder.setUdabmLength(AbmLength.Four).setUserBitMask(readUint32(buffer).toUint64());
        } else if (ulength == FLAGS_EIGHT_SIZE) {
            aslaBuilder.setUdabmLength(AbmLength.Eight).setUserBitMask(readUint64(buffer));
        }

        // Then read ASLA subTLVs
        final var subTlvBuilder = new AslaSubtlvsBuilder();
        for (var entry : LinkstateAttributeParser.getAttributesMap(buffer).entries()) {
            final int type = entry.getKey();
            final ByteBuf value = entry.getValue();
            switch (type) {
                case ADMIN_GROUP -> {
                    final var ag = new AdministrativeGroup(readUint32(value));
                    LOG.debug("Parsed ASLA Administrative Group {}", ag);
                    subTlvBuilder.setAdminGroup(ag);
                }
                case TE_METRIC -> {
                    final var tm = new TeMetric(readUint32(value));
                    LOG.debug("Parsed ASLA Metric {}", tm);
                    subTlvBuilder.setTeMetric(tm);
                }
                case SHARED_RISK_LINK_GROUP -> {
                    final var srlg = parseSrlg(value);
                    LOG.debug("Parsed ASLA Shared Risk Link Group (SRLG) {}", srlg);
                    subTlvBuilder.setSharedRiskLinkGroups(srlg);
                }

                // Performance Metrics
                case LINK_DELAY -> {
                    final var ld = new Delay(readUint32(value));
                    LOG.debug("Parsed ASLA Link Delay {}", ld);
                    subTlvBuilder.setLinkDelay(ld);
                }
                case LINK_MIN_MAX_DELAY -> {
                    final var lmmd = new LinkMinMaxDelayBuilder()
                        .setMinDelay(new Delay(readUint32(value)))
                        .setMaxDelay(new Delay(readUint32(value)))
                        .build();
                    LOG.debug("Parsed ASLA Link Min/Max Delay {}", lmmd);
                    subTlvBuilder.setLinkMinMaxDelay(lmmd);
                }
                case DELAY_VARIATION -> {
                    final var dv = new Delay(readUint32(value));
                    LOG.debug("Parsed ASLA Delay Variation {}", dv);
                    subTlvBuilder.setDelayVariation(dv);
                }
                case LINK_LOSS -> {
                    final var ll = new Loss(readUint32(value));
                    LOG.debug("Parsed ASLA Link Loss {}", ll);
                    subTlvBuilder.setLinkLoss(ll);
                }
                case RESIDUAL_BANDWIDTH -> {
                    final var rb = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed ASLA Residual Bandwidth {}", rb);
                    subTlvBuilder.setResidualBandwidth(rb);
                }
                case AVAILABLE_BANDWIDTH -> {
                    final var ab = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed ASLA Available Bandwidth {}", ab);
                    subTlvBuilder.setAvailableBandwidth(ab);
                }
                case UTILIZED_BANDWIDTH -> {
                    final var ub = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed ASLA Utilized Bandwidth {}", ub);
                    subTlvBuilder.setUtilizedBandwidth(ub);
                }
                case EXTENDED_ADMIN_GROUP -> {
                    final var eag = SrFlexAlgoParser.parseExtendedAdminGroup(value);
                    LOG.debug("Parsed ASLA Extended Administrative Group {}", eag);
                    subTlvBuilder.setExtendedAdminGroup(eag);
                }
                default -> LOG.debug("Unsupported ASLA subTLVs: {}", type);
            }
        }

        return aslaBuilder.setAslaSubtlvs(subTlvBuilder.build()).build();
    }

    private static L2BundleMember parseL2BundleMember(final ByteBuf buffer, final ProtocolId protocolId) {
        final var l2Builder = new L2BundleMemberBuilder();

        l2Builder.setDescriptor(readUint32(buffer));

        // Parse SubTLVs
        final var builder = new L2SubtlvsBuilder();
        final var srAdjIds = new ArrayList<SrAdjIds>();
        final var srLanAdjIds = new ArrayList<SrLanAdjIds>();
        for (var entry : LinkstateAttributeParser.getAttributesMap(buffer).entries()) {
            final int type = entry.getKey();
            final ByteBuf value = entry.getValue();
            switch (type) {
                case ADMIN_GROUP -> {
                    final var ag = new AdministrativeGroup(readUint32(value));
                    LOG.debug("Parsed L2 Administrative Group {}", ag);
                    builder.setAdminGroup(ag);
                }
                case MAX_BANDWIDTH -> {
                    final var mb = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed L2 Max Bandwidth {}", mb);
                    builder.setMaxLinkBandwidth(mb);
                }
                case MAX_RESERVABLE_BANDWIDTH -> {
                    final var mrb = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed L2 Max Reservable Bandwidth {}", mrb);
                    builder.setMaxReservableBandwidth(mrb);
                }
                case UNRESERVED_BANDWIDTH -> {
                    final var ub = parseUnreservedBandwidth(value);
                    LOG.debug("Parsed L2 Unreserved Bandwidth {}", ub);
                    builder.setUnreservedBandwidth(ub);
                }
                case TE_METRIC -> {
                    final var tm = new TeMetric(readUint32(value));
                    LOG.debug("Parsed L2 Metric {}", tm);
                    builder.setTeMetric(tm);
                }
                case LINK_PROTECTION_TYPE -> {
                    final var lpt = LinkProtectionType.forValue(value.readShort());
                    LOG.debug("Parsed L2 Link Protection Type {}", lpt);
                    builder.setLinkProtection(lpt);
                }
                case SR_ADJ_SID -> {
                    final var sid = SrLinkAttributesParser.parseAdjacencySegmentIdentifier(value, protocolId);
                    LOG.debug("Parsed L2 Adjacency Segment Identifier :{}", sid);
                    srAdjIds.add(sid);
                }
                case SR_LAN_ADJ_SID -> {
                    final var sid = SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(value, protocolId);
                    LOG.debug("Parsed L2 Adjacency Segment Identifier :{}", sid);
                    srLanAdjIds.add(sid);
                }

                // Performance Metrics
                case LINK_DELAY -> {
                    final var ld = new Delay(readUint32(value));
                    LOG.debug("Parsed L2 Link Delay {}", ld);
                    builder.setLinkDelay(ld);
                }
                case LINK_MIN_MAX_DELAY -> {
                    final var lmmd = new LinkMinMaxDelayBuilder()
                        .setMinDelay(new Delay(readUint32(value)))
                        .setMaxDelay(new Delay(readUint32(value)))
                        .build();
                    LOG.debug("Parsed L2 Link Min/Max Delay {}", lmmd);
                    builder.setLinkMinMaxDelay(lmmd);
                }
                case DELAY_VARIATION -> {
                    final var dv = new Delay(readUint32(value));
                    LOG.debug("Parsed L2 Delay Variation {}", dv);
                    builder.setDelayVariation(dv);
                }
                case LINK_LOSS -> {
                    final var ll = new Loss(readUint32(value));
                    LOG.debug("Parsed L2 Link Loss {}", ll);
                    builder.setLinkLoss(ll);
                }
                case RESIDUAL_BANDWIDTH -> {
                    final var rb = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed L2 Residual Bandwidth {}", rb);
                    builder.setResidualBandwidth(rb);
                }
                case AVAILABLE_BANDWIDTH -> {
                    final var ab = new Bandwidth(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed L2 Available Bandwidth {}", ab);
                    builder.setAvailableBandwidth(ab);
                }
                case UTILIZED_BANDWIDTH -> {
                    final var ub = new Bandwidth(ByteArray.readAllBytes(value));
                    builder.setUtilizedBandwidth(ub);
                    LOG.debug("Parsed L2 Utilized Bandwidth {}", ub);
                }
                default -> LOG.debug("Unsupported L2 Bundle subTLVs: {}", type);
            }
        }
        if (!srAdjIds.isEmpty()) {
            builder.setSrAdjIds(srAdjIds);
        }
        if (!srLanAdjIds.isEmpty()) {
            builder.setSrLanAdjIds(srLanAdjIds);
        }

        return l2Builder.setL2Subtlvs(builder.build()).build();
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
