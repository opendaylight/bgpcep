/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute;

import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint8;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint8;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SRv6AttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrFlexAlgoParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrNodeAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv6RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.NodeFlagBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.NodeAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.Srms;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class NodeAttributesParser {

    private static final Logger LOG = LoggerFactory.getLogger(NodeAttributesParser.class);
    private static final int FLAGS_SIZE = 8;
    // node flag bits
    private static final int OVERLOAD_BIT = 0;
    private static final int ATTACHED_BIT = 1;
    private static final int EXTERNAL_BIT = 2;
    private static final int ABBR_BIT = 3;
    private static final int ROUTER_BIT = 4;
    private static final int V6_BIT = 5;
    /* Node Attribute TLVs */
    private static final int NODE_FLAG_BITS = 1024;
    private static final int NODE_OPAQUE = 1025;
    private static final int DYNAMIC_HOSTNAME = 1026;
    private static final int ISIS_AREA_IDENTIFIER = 1027;
    /* Segment routing TLVs */
    private static final int SR_NODE_MSD = 266;
    private static final int SR_CAPABILITIES = 1034;
    private static final int SR_ALGORITHMS = 1035;
    private static final int SR_LOCAL_BLOCK = 1036;
    private static final int SRMS = 1037;
    private static final int SRV6_CAPABILITIES = 1038;
    private static final int SR_FLEX_ALGO = 1039;

    private NodeAttributesParser() {

    }

    /**
     * Parse Node Attributes.
     *
     * @param attributes key is the tlv type and value is the value of the tlv
     * @param protocolId to differentiate parsing methods
     * @return {@link LinkStateAttribute}
     */
    static LinkStateAttribute parseNodeAttributes(final Multimap<Integer, ByteBuf> attributes,
            final ProtocolId protocolId) {
        final var topologyMembership = ImmutableSet.<TopologyIdentifier>builder();
        final var areaMembership = ImmutableSet.<IsisAreaIdentifier>builder();
        final NodeAttributesBuilder builder = new NodeAttributesBuilder();
        final SrCapabilitiesBuilder srBuilder = new SrCapabilitiesBuilder();
        boolean srCapabilities = false;
        for (var entry : attributes.entries()) {
            final int key = entry.getKey();
            final ByteBuf value = entry.getValue();
            LOG.trace("Node attribute TLV {}", key);
            switch (key) {
                case TlvUtil.MULTI_TOPOLOGY_ID -> {
                    parseTopologyId(topologyMembership, value);
                }
                case NODE_FLAG_BITS -> {
                    parseNodeFlags(value, builder);
                }
                case NODE_OPAQUE -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Ignoring opaque value: {}.", ByteBufUtil.hexDump(value));
                    }
                }
                case DYNAMIC_HOSTNAME -> {
                    final var dh = new String(ByteArray.readAllBytes(value), StandardCharsets.US_ASCII);
                    LOG.debug("Parsed Node Name {}", dh);
                    builder.setDynamicHostname(dh);
                }
                case ISIS_AREA_IDENTIFIER -> {
                    final var ai = new IsisAreaIdentifier(ByteArray.readAllBytes(value));
                    LOG.debug("Parsed AreaIdentifier {}", ai);
                    areaMembership.add(ai);
                }
                case TlvUtil.LOCAL_IPV4_ROUTER_ID -> {
                    final var ip4 = new Ipv4RouterIdentifier(Ipv4Util.addressForByteBuf(value));
                    LOG.debug("Parsed IPv4 Router Identifier {}", ip4);
                    builder.setIpv4RouterId(ip4);
                }
                case TlvUtil.LOCAL_IPV6_ROUTER_ID -> {
                    final var ip6 = new Ipv6RouterIdentifier(Ipv6Util.addressForByteBuf(value));
                    LOG.debug("Parsed IPv6 Router Identifier {}", ip6);
                    builder.setIpv6RouterId(ip6);
                }
                case SR_CAPABILITIES -> {
                    SrNodeAttributesParser.parseSrCapabilities(srBuilder, value, protocolId);
                    LOG.debug("Parsed SR Capabilities {}", srBuilder);
                    srCapabilities = true;
                }
                case SR_ALGORITHMS -> {
                    final var algs = SrNodeAttributesParser.parseSrAlgorithms(value);
                    LOG.debug("Parsed SR Algorithms {}", algs);
                    srBuilder.setAlgorithms(algs);
                    srCapabilities = true;
                }
                case SR_LOCAL_BLOCK -> {
                    final var srlbs = SrNodeAttributesParser.parseSrLocalBlock(value);
                    LOG.debug("Parsed SR Local Block {}", srlbs);
                    srBuilder.setSrlb(srlbs);
                    srCapabilities = true;
                }
                case SR_NODE_MSD -> {
                    final var msds = SrNodeAttributesParser.parseSrNodeMsd(value);
                    LOG.debug("Parsed SR Node MSD {}", msds);
                    srBuilder.setNodeMsd(msds);
                    srCapabilities = true;
                }
                case SRMS -> {
                    final var srms = new Srms(readUint8(value));
                    LOG.debug("Parsed SRMS {}", srms);
                    srBuilder.setPreference(srms);
                }
                case SRV6_CAPABILITIES -> {
                    final var caps = SRv6AttributesParser.parseSrv6Capabilities(value);
                    LOG.debug("Parsed SRv6 Capabilities {}", caps);
                    builder.setSrv6Capabilities(caps);
                }
                case SR_FLEX_ALGO -> {
                    final var fad = SrFlexAlgoParser.parseSrFlexAlgoDefinition(value);
                    LOG.debug("Parsed Flex Algo Definition {}", fad);
                    builder.setFlexAlgoDefinition(fad);
                }
                default -> LOG.warn("TLV {} is not a valid node attribute, ignoring it", key);
            }
            if (srCapabilities) {
                builder.setSrCapabilities(srBuilder.build());
            }
        }

        LOG.trace("Finished parsing Node Attributes.");
        return new NodeAttributesCaseBuilder()
            .setNodeAttributes(builder
                .setTopologyIdentifier(topologyMembership.build())
                .setIsisAreaId(areaMembership.build())
                .build())
            .build();
    }

    private static void parseNodeFlags(final ByteBuf value, final NodeAttributesBuilder builder) {
        final var flags = BitArray.valueOf(value, FLAGS_SIZE);
        builder.setNodeFlags(new NodeFlagBits(flags.get(OVERLOAD_BIT), flags.get(ATTACHED_BIT), flags.get(EXTERNAL_BIT),
            flags.get(ABBR_BIT), flags.get(ROUTER_BIT), flags.get(V6_BIT)));
        LOG.debug("Parsed Overload bit: {}, attached bit: {}, external bit: {}, area border router: {}.",
            flags.get(OVERLOAD_BIT), flags.get(ATTACHED_BIT), flags.get(EXTERNAL_BIT), flags.get(ABBR_BIT));
    }

    private static void parseTopologyId(final ImmutableSet.Builder<TopologyIdentifier> topologyMembership,
            final ByteBuf value) {
        while (value.isReadable()) {
            final var topId = new TopologyIdentifier(
                Uint16.valueOf(value.readUnsignedShort() & TlvUtil.TOPOLOGY_ID_OFFSET));
            topologyMembership.add(topId);
            LOG.debug("Parsed Topology Identifier: {}", topId);
        }
    }

    static void serializeNodeAttributes(final NodeAttributesCase nodeAttributesCase, final ByteBuf byteAggregator) {
        LOG.trace("Started serializing Node Attributes");
        final var nodeAttributes = nodeAttributesCase.getNodeAttributes();
        serializeTopologyId(nodeAttributes.getTopologyIdentifier(), byteAggregator);
        serializeNodeFlagBits(nodeAttributes.getNodeFlags(), byteAggregator);

        final var dh = nodeAttributes.getDynamicHostname();
        if (dh != null) {
            TlvUtil.writeTLV(DYNAMIC_HOSTNAME, Unpooled.wrappedBuffer(StandardCharsets.UTF_8.encode(dh)),
                byteAggregator);
        }
        final var isisList = nodeAttributes.getIsisAreaId();
        if (isisList != null) {
            for (var isisAreaIdentifier : isisList) {
                TlvUtil.writeTLV(ISIS_AREA_IDENTIFIER, Unpooled.wrappedBuffer(isisAreaIdentifier.getValue()),
                    byteAggregator);
            }
        }
        final var ipv4 = nodeAttributes.getIpv4RouterId();
        if (ipv4 != null) {
            TlvUtil.writeTLV(TlvUtil.LOCAL_IPV4_ROUTER_ID, Ipv4Util.byteBufForAddress(ipv4), byteAggregator);
        }
        final var ipv6 = nodeAttributes.getIpv6RouterId();
        if (ipv6 != null) {
            TlvUtil.writeTLV(TlvUtil.LOCAL_IPV6_ROUTER_ID, Ipv6Util.byteBufForAddress(ipv6), byteAggregator);
        }
        final var srCapabilities = nodeAttributes.getSrCapabilities();
        if (srCapabilities != null) {
            final var capBuffer = Unpooled.buffer();
            SrNodeAttributesParser.serializeSrCapabilities(srCapabilities, capBuffer);
            TlvUtil.writeTLV(SR_CAPABILITIES, capBuffer, byteAggregator);
            final var srAlgorithm = srCapabilities.getAlgorithms();
            if (srAlgorithm != null) {
                capBuffer.clear();
                SrNodeAttributesParser.serializeSrAlgorithms(srAlgorithm, capBuffer);
                TlvUtil.writeTLV(SR_ALGORITHMS, capBuffer, byteAggregator);
            }
            final var srlb = srCapabilities.getSrlb();
            if (srlb != null) {
                capBuffer.clear();
                SrNodeAttributesParser.serializeSrLocalBlock(srlb, capBuffer);
                TlvUtil.writeTLV(SR_LOCAL_BLOCK, capBuffer, byteAggregator);
            }
            final var msd = srCapabilities.getNodeMsd();
            if (msd != null) {
                capBuffer.clear();
                SrNodeAttributesParser.serializeSrNodeMsd(msd, capBuffer);
                TlvUtil.writeTLV(SR_NODE_MSD, capBuffer, byteAggregator);
            }
            final var srms = srCapabilities.getPreference();
            if (srms != null) {
                capBuffer.clear();
                writeUint8(capBuffer, srms.getValue());
                TlvUtil.writeTLV(SRMS, capBuffer, byteAggregator);
            }
        }
        final var fad = nodeAttributes.getFlexAlgoDefinition();
        if (fad != null) {
            final var fadBuffer = Unpooled.buffer();
            SrFlexAlgoParser.serializeSrFlexAlgoDefinition(fad, fadBuffer);
            TlvUtil.writeTLV(SR_FLEX_ALGO, fadBuffer, byteAggregator);
        }
        final var srv6Cap = nodeAttributes.getSrv6Capabilities();
        if (srv6Cap != null) {
            final var srv6Buffer = Unpooled.buffer();
            SRv6AttributesParser.serialiseSrv6Capabilities(srv6Cap, srv6Buffer);
            TlvUtil.writeTLV(SRV6_CAPABILITIES, srv6Buffer, byteAggregator);
        }
        LOG.trace("Finished serializing Node Attributes");
    }

    private static void serializeTopologyId(final Set<TopologyIdentifier> topList, final ByteBuf byteAggregator) {
        if (topList != null) {
            final var mpIdBuf = Unpooled.buffer();
            for (final TopologyIdentifier topologyIdentifier : topList) {
                writeUint16(mpIdBuf, topologyIdentifier.getValue());
            }
            TlvUtil.writeTLV(TlvUtil.MULTI_TOPOLOGY_ID, mpIdBuf, byteAggregator);
        }
    }

    private static void serializeNodeFlagBits(final NodeFlagBits nodeFlagBits, final ByteBuf byteAggregator) {
        if (nodeFlagBits != null) {
            final var nodeFlagBuf = Unpooled.buffer(1);
            final var flags = new BitArray(FLAGS_SIZE);
            flags.set(OVERLOAD_BIT, nodeFlagBits.getOverload());
            flags.set(ATTACHED_BIT, nodeFlagBits.getAttached());
            flags.set(EXTERNAL_BIT, nodeFlagBits.getExternal());
            flags.set(ABBR_BIT, nodeFlagBits.getAbr());
            flags.set(ROUTER_BIT, nodeFlagBits.getRouter());
            flags.set(V6_BIT, nodeFlagBits.getV6());
            flags.toByteBuf(nodeFlagBuf);
            TlvUtil.writeTLV(NODE_FLAG_BITS, nodeFlagBuf, byteAggregator);
        }
    }
}
