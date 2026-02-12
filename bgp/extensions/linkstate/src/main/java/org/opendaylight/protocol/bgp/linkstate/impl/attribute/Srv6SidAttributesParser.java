/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute;

import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint8;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint8;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SRv6AttributesParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.Srv6SidAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.Srv6SidAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.srv6.sid.attributes._case.Srv6SidAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.attributes.Srv6BgpPeerNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.attributes.Srv6BgpPeerNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.attributes.Srv6EndpointBehavior;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.attributes.Srv6EndpointBehaviorBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class Srv6SidAttributesParser {

    private static final Logger LOG = LoggerFactory.getLogger(Srv6SidAttributesParser.class);

    private static final int RESERVED = 2;
    private static final int FLAGS_SIZE = 1;
    private static final int ENDPOINT_BEHAVIOR = 1250;
    private static final int BGP_PEER_NODE = 1251;
    private static final int SID_STRUCTURE = 1252;

    private Srv6SidAttributesParser() {

    }

    /**
     * Parse Srv6 SID Attributes.
     *
     * @param attributes key is the tlv type and value is the value of the tlv
     * @param protocolId to differentiate parsing methods
     * @return {@link LinkStateAttribute}
     */
    static LinkStateAttribute parseSrv6SidAttributes(final Multimap<Integer, ByteBuf> attributes,
            final ProtocolId protocolId) {
        final var srv6AttrBuilder = new Srv6SidAttributesBuilder();
        for (var entry : attributes.entries()) {
            final int key = entry.getKey();
            final ByteBuf value = entry.getValue();
            LOG.trace("Parse SRv6 attribute TLV {}", key);
            switch (key) {
                case ENDPOINT_BEHAVIOR -> {
                    srv6AttrBuilder.setSrv6EndpointBehavior(parseSrv6EndpointBehavior(value));
                    LOG.debug("Parsed SRv6 Endpoint Behavior {}", srv6AttrBuilder.getSrv6EndpointBehavior());
                }
                case BGP_PEER_NODE -> {
                    srv6AttrBuilder.setSrv6BgpPeerNode(parseSrv6BgpPeerNode(value));
                    LOG.debug("Parsed SRv6 BGP Peer Node {}", srv6AttrBuilder.getSrv6BgpPeerNode());
                }
                case SID_STRUCTURE -> {
                    srv6AttrBuilder.setSrv6SidStructure(SRv6AttributesParser.parseSrv6SidStructure(value));
                    LOG.debug("Parsed SRv6 SID Structure {}", srv6AttrBuilder.getSrv6SidStructure());
                }
                default -> LOG.warn("Unexpected TLV type {} in Srv6 SID Attributes. Skipping.", key);
            }
        }

        LOG.trace("Finished parsing Srv6 SID Attributes.");
        return new Srv6SidAttributesCaseBuilder()
            .setSrv6SidAttributes(srv6AttrBuilder.build())
            .build();
    }

    private static Srv6EndpointBehavior parseSrv6EndpointBehavior(final ByteBuf buffer) {
        final var sebBuilder = new Srv6EndpointBehaviorBuilder();
        sebBuilder.setEndpointBehavior(readUint16(buffer));
        // No Flags has been defined in RFC9514 (https://www.rfc-editor.org/rfc/rfc9514.html#section-7.1)
        buffer.skipBytes(FLAGS_SIZE);
        sebBuilder.setAlgo(readUint8(buffer));
        return sebBuilder.build();
    }

    private static Srv6BgpPeerNode parseSrv6BgpPeerNode(final ByteBuf buffer) {
        final var bpnBuilder = new Srv6BgpPeerNodeBuilder();
        bpnBuilder.setFlags(SRv6AttributesParser.parseSrv6Flags(buffer));
        bpnBuilder.setWeight(readUint8(buffer));
        buffer.skipBytes(RESERVED);
        bpnBuilder.setPeerAsNumber(new AsNumber(readUint32(buffer)));
        bpnBuilder.setPeerBgpId(Ipv4Util.addressForByteBuf(buffer));
        return bpnBuilder.build();
    }

    static void serializeSrv6SidAttributes(final Srv6SidAttributesCase srv6SidAttributesCase,
        final ByteBuf buffer) {
        LOG.trace("Started serializing Srv6 SID Attributes");
        final var srv6Attributes = srv6SidAttributesCase.getSrv6SidAttributes();
        if (srv6Attributes == null) {
            LOG.warn("SRv6 SID Attributes is null. Skipping serialization.");
            return;
        }

        final var epb = srv6Attributes.getSrv6EndpointBehavior();
        if (epb != null) {
            serializeSrv6EndpointBehavior(epb, buffer);
        }
        final var bgpPeerNode = srv6Attributes.getSrv6BgpPeerNode();
        if (bgpPeerNode != null) {
            serializeSrv6BgpPeerNode(bgpPeerNode, buffer);
        }
        final var sidStructure = srv6Attributes.getSrv6SidStructure();
        if (sidStructure != null) {
            SRv6AttributesParser.serializeSrv6SidStructure(sidStructure, buffer);
        }
        LOG.trace("Finished serializing Srv6 SID Attributes");
    }

    private static void serializeSrv6EndpointBehavior(final Srv6EndpointBehavior endpointBehavior,
        final ByteBuf output) {
        final var buffer = Unpooled.buffer();
        writeUint16(buffer, endpointBehavior.getEndpointBehavior());
        // No Flags has been defined in RFC9514 (https://www.rfc-editor.org/rfc/rfc9514.html#section-7.1)
        buffer.writeZero(FLAGS_SIZE);
        writeUint8(buffer, endpointBehavior.getAlgo());
        TlvUtil.writeTLV(ENDPOINT_BEHAVIOR, buffer, output);
    }

    private static void serializeSrv6BgpPeerNode(final Srv6BgpPeerNode srv6BgpPeerNode, final ByteBuf output) {
        final var buffer = Unpooled.buffer();
        SRv6AttributesParser.serializeSrv6Flags(srv6BgpPeerNode.getFlags(), buffer);
        writeUint8(buffer, srv6BgpPeerNode.getWeight());
        buffer.writeZero(RESERVED);
        ByteBufUtils.write(buffer, srv6BgpPeerNode.getPeerAsNumber().getValue());
        Ipv4Util.writeIpv4Address(srv6BgpPeerNode.getPeerBgpId(), buffer);
        TlvUtil.writeTLV(BGP_PEER_NODE, buffer, output);
    }

}
