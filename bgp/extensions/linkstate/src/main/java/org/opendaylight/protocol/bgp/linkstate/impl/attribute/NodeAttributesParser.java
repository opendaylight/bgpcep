/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.nio.charset.StandardCharsets;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrNodeAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.Ipv6RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.NodeFlagBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.NodeAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.state.SrAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.state.SrCapabilities;
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
    private static final int SR_CAPABILITIES = 1034;
    private static final int SR_ALGORITHMS = 1035;
    
    private NodeAttributesParser() {
        throw new UnsupportedOperationException();
    }

    /**
     * Parse Node Attributes.
     *
     * @param attributes key is the tlv type and value is the value of the tlv
     * @param protocolId to differentiate parsing methods
     * @return {@link LinkStateAttribute}
     */
    static LinkStateAttribute parseNodeAttributes(final Multimap<Integer, ByteBuf> attributes, final ProtocolId protocolId) {
        final List<TopologyIdentifier> topologyMembership = new ArrayList<>();
        final List<IsisAreaIdentifier> areaMembership = new ArrayList<>();
        final NodeAttributesBuilder builder = new NodeAttributesBuilder();
        for (final Entry<Integer, ByteBuf> entry : attributes.entries()) {
            final int key = entry.getKey();
            final ByteBuf value = entry.getValue();
            LOG.trace("Node attribute TLV {}", key);
            switch (key) {
            case TlvUtil.MULTI_TOPOLOGY_ID:
                parseTopologyId(topologyMembership, value);
                break;
            case NODE_FLAG_BITS:
                parseNodeFlags(value, builder);
                break;
            case NODE_OPAQUE:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring opaque value: {}.", ByteBufUtil.hexDump(value));
                }
                break;
            case DYNAMIC_HOSTNAME:
                builder.setDynamicHostname(new String(ByteArray.readAllBytes(value), StandardCharsets.US_ASCII));
                LOG.debug("Parsed Node Name {}", builder.getDynamicHostname());
                break;
            case ISIS_AREA_IDENTIFIER:
                final IsisAreaIdentifier ai = new IsisAreaIdentifier(ByteArray.readAllBytes(value));
                areaMembership.add(ai);
                LOG.debug("Parsed AreaIdentifier {}", ai);
                break;
            case TlvUtil.LOCAL_IPV4_ROUTER_ID:
                final Ipv4RouterIdentifier ip4 = new Ipv4RouterIdentifier(Ipv4Util.addressForByteBuf(value));
                builder.setIpv4RouterId(ip4);
                LOG.debug("Parsed IPv4 Router Identifier {}", ip4);
                break;
            case TlvUtil.LOCAL_IPV6_ROUTER_ID:
                final Ipv6RouterIdentifier ip6 = new Ipv6RouterIdentifier(Ipv6Util.addressForByteBuf(value));
                builder.setIpv6RouterId(ip6);
                LOG.debug("Parsed IPv6 Router Identifier {}", ip6);
                break;
            case SR_CAPABILITIES:
                final SrCapabilities caps = SrNodeAttributesParser.parseSrCapabilities(value, protocolId);
                builder.setSrCapabilities(caps);
                LOG.debug("Parsed SR Capabilities {}", caps);
                break;
            case SR_ALGORITHMS:
                final SrAlgorithm algs = SrNodeAttributesParser.parseSrAlgorithms(value);
                builder.setSrAlgorithm(algs);
                LOG.debug("Parsed SR Algorithms {}", algs);
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

    private static void parseNodeFlags(final ByteBuf value, final NodeAttributesBuilder builder) {
        final BitArray flags = BitArray.valueOf(value, FLAGS_SIZE);
        builder.setNodeFlags(new NodeFlagBits(flags.get(OVERLOAD_BIT), flags.get(ATTACHED_BIT), flags.get
            (EXTERNAL_BIT), flags.get(ABBR_BIT), flags.get(ROUTER_BIT), flags.get(V6_BIT)));
        LOG.debug("Parsed Overload bit: {}, attached bit: {}, external bit: {}, area border router: {}.",
            flags.get(OVERLOAD_BIT), flags.get(ATTACHED_BIT), flags.get(EXTERNAL_BIT), flags.get(ABBR_BIT));
    }

    private static void parseTopologyId(final List<TopologyIdentifier> topologyMembership, final ByteBuf value) {
        while (value.isReadable()) {
            final TopologyIdentifier topId = new TopologyIdentifier(value.readUnsignedShort() & TlvUtil.TOPOLOGY_ID_OFFSET);
            topologyMembership.add(topId);
            LOG.debug("Parsed Topology Identifier: {}", topId);
        }
    }

    static void serializeNodeAttributes(final NodeAttributesCase nodeAttributesCase, final ByteBuf byteAggregator) {
        LOG.trace("Started serializing Node Attributes");
        final NodeAttributes nodeAttributes = nodeAttributesCase.getNodeAttributes();
        serializeTopologyId(nodeAttributes.getTopologyIdentifier(), byteAggregator);
        serializeNodeFlagBits(nodeAttributes.getNodeFlags(), byteAggregator);
        if (nodeAttributes.getDynamicHostname() != null) {
            TlvUtil.writeTLV(DYNAMIC_HOSTNAME, Unpooled.wrappedBuffer(StandardCharsets.UTF_8.encode(nodeAttributes.getDynamicHostname())), byteAggregator);
        }
        final List<IsisAreaIdentifier> isisList = nodeAttributes.getIsisAreaId();
        if (isisList != null) {
            for (final IsisAreaIdentifier isisAreaIdentifier : isisList) {
                TlvUtil.writeTLV(ISIS_AREA_IDENTIFIER, Unpooled.wrappedBuffer(isisAreaIdentifier.getValue()), byteAggregator);
            }
        }
        if (nodeAttributes.getIpv4RouterId() != null) {
            TlvUtil.writeTLV(TlvUtil.LOCAL_IPV4_ROUTER_ID, Ipv4Util.byteBufForAddress(nodeAttributes.getIpv4RouterId()), byteAggregator);
        }
        if (nodeAttributes.getIpv6RouterId() != null) {
            TlvUtil.writeTLV(TlvUtil.LOCAL_IPV6_ROUTER_ID, Ipv6Util.byteBufForAddress(nodeAttributes.getIpv6RouterId()), byteAggregator);
        }
        if (nodeAttributes.getSrCapabilities() != null) {
            final ByteBuf capBuffer = Unpooled.buffer();
            SrNodeAttributesParser.serializeSrCapabilities(nodeAttributes.getSrCapabilities(), capBuffer);
            TlvUtil.writeTLV(SR_CAPABILITIES, capBuffer, byteAggregator);
        }
        if (nodeAttributes.getSrAlgorithm() != null) {
            final ByteBuf capBuffer = Unpooled.buffer();
            SrNodeAttributesParser.serializeSrAlgorithms(nodeAttributes.getSrAlgorithm(), capBuffer);
            TlvUtil.writeTLV(SR_ALGORITHMS, capBuffer, byteAggregator);
        }
        LOG.trace("Finished serializing Node Attributes");
    }

    private static void serializeTopologyId(final List<TopologyIdentifier> topList, final ByteBuf byteAggregator) {
        if (topList != null) {
            final ByteBuf mpIdBuf = Unpooled.buffer();
            for (final TopologyIdentifier topologyIdentifier : topList) {
                mpIdBuf.writeShort(topologyIdentifier.getValue());
            }
            TlvUtil.writeTLV(TlvUtil.MULTI_TOPOLOGY_ID, mpIdBuf, byteAggregator);
        }
    }

    private static void serializeNodeFlagBits(final NodeFlagBits nodeFlagBits, final ByteBuf byteAggregator) {
        if (nodeFlagBits != null) {
            final ByteBuf nodeFlagBuf = Unpooled.buffer(1);
            final BitArray flags = new BitArray(FLAGS_SIZE);
            flags.set(OVERLOAD_BIT, nodeFlagBits.isOverload());
            flags.set(ATTACHED_BIT, nodeFlagBits.isAttached());
            flags.set(EXTERNAL_BIT, nodeFlagBits.isExternal());
            flags.set(ABBR_BIT, nodeFlagBits.isAbr());
            flags.set(ROUTER_BIT, nodeFlagBits.isRouter());
            flags.set(V6_BIT, nodeFlagBits.isV6());
            flags.toByteBuf(nodeFlagBuf);
            TlvUtil.writeTLV(NODE_FLAG_BITS, nodeFlagBuf, byteAggregator);
        }
    }
}
