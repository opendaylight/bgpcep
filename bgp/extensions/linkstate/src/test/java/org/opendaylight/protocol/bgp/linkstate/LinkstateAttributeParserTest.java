/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.FlexAlgoDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.IgpBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.ExtendedAdminGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.FlexAlgo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.FlexAlgoDefinitionFlag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.FlexMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.MsdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.IsisAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.IsisAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.isis.adj.flags._case.IsisAdjFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.definitions.FlexAlgoDefinitionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.definitions.FlexAlgoDefinitionTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.definitions.flex.algo.definition.tlv.FlexAlgoSubtlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.subtlv.UnsupportedTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.node.msd.tlv.Msd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.node.msd.tlv.MsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.attribute.flags.igp.attribute.flags.IsisAttributeFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.sid.label.index.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.sid.label.index.SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.lan.end.x.sid.neighbor.type.IsisNeighborCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class LinkstateAttributeParserTest {

    private static final byte[] LINK_ATTR = {
        0x04, 0x04, 0, 0x04, 0x2a, 0x2a, 0x2a, 0x2a, 0x04, 0x06, 0, 0x04, 0x2b, 0x2b, 0x2b, 0x2b, 0x04, 0x40, 0, 0x04,
        0, 0, 0, 0, 0x04, 0x41, 0, 0x04, 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, 0x04, 0x42, 0, 0x04, 0x46, 0x43,
        0x50, 0, 0x04, 0x43, 0, 0x20, 0x46, 0x43, 0x50, 0, 0x46, 0x43, 0x50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x04, 0x44, 0, 0x08, 0, 0, 0, 0, 0, 0, 0, 0, 0x04, 0x45, 0, 0x02, 0, 0x08,
        0x04, 0x46, 0, 0x01, (byte) 0xc0, 0x04, 0x47, 0, 0x03, 0, 0, 0x0a, 0x04, 0x48, 0, 0x08, 0x12, 0x34, 0x56, 0x78,
        0x10, 0x30, 0x50, 0x70, 0x04, 0x4a, 0, 0x05, 0x31, 0x32, 0x4b, 0x2d, 0x32,
        0x04, 0x4b, 0, 0x07, (byte)-80, 10, 0, 0, (byte)0x0f, (byte)0xff, (byte)0xff, // sr-adj
        0x04, 0x4b, 0, 0x07, (byte)-80, 10, 0, 0, (byte)0x0f, (byte)0xff, (byte)0xef, // sr-adj
        0x04, 0x4c, 0, 0x0d, (byte)-80, 10, 0, 0, 1, 2, 3, 4, 5, 6, (byte)0x0f, (byte)0xff, (byte)0xff, // sr-lan-adj
        0x04, 0x4c, 0, 0x0d, (byte)-80, 10, 0, 0, 1, 2, 3, 4, 5, 6, (byte)0x0f, (byte)0xff, (byte)0xef, // sr-lan-adj
        0x04, 0x4d, 0, 0x08, 0, 0x05, 0, 0, 0x0a, 0x0b, 0x0c, 0x0d, // peer-node-sid
        0x04, 0x4e, 0, 0x08, 0, 0x05, 0, 0, 0x0a, 0x0b, 0x0c, 0x0f, // peer-adj-sid
        0x04, 0x4f, 0, 0x08, 0, 0x05, 0, 0, 0x0a, 0x0b, 0x0c, 0x0e, // peer-set-sid
        // Performace Metrics
        0x04, 0x5a, 0, 0x04, 0, 0, 0x27, 0x10, // Link Delay
        0x04, 0x5b, 0, 0x08, 0, 0, 0x13, (byte)0x88, 0, 0, 0x4e, 0x20, // Link Min-Max Delay
        0x04, 0x5c, 0, 0x04, 0, 0, 0x27, 0x10, // Link Delay Variation
        0x04, 0x5d, 0, 0x04, 0, 0, 0, 0, // Link Loss
        0x04, 0x5e, 0, 0x04, 0x46, 0x43, 0x50, 0, // Residual Bandwidth
        0x04, 0x5f, 0, 0x04, 0x46, 0x43, 0x50, 0, // Available Bandwidth
        0x04, 0x60, 0, 0x04, 0, 0, 0, 0,  // Utilized Bandwidth
        0x04, (byte) 0x95, 0, 4, 0, 1, 0, 8, // Extended Admin Group
        0x04, (byte) 0x88, 0, 0x01, 0x0a // Unknown TLV
    };

    private static final byte[] LINK_ATTR_SRV6 = {
        // Local IPv6 Router-ID
        0x04, 0x05, 0, 0x10, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0, 0, 0, 0, 0, 0, 0, 0x01,
        // Remote IPv6 Router-ID
        0x04, 0x07, 0, 0x10, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0, 0, 0, 0, 0, 0, 0, 0x02,
        // SRv6 EndX SID
        0x04, 0x52, 0, 0x1e, 0, 0x01, (byte) 0x60, 0x01, 0x0a, 0, 0, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0, 0, 0, 0, 0, 0, 0, 0x01,
        0x04, (byte) 0xe4, 0, 0x04, 0x08, 0x08, 0x04, 0x04, // SRv6 SID Structure SubTlvs
        // IS-IS SRv6 LAN EndX SID
        0x04, 0x53, 0, 0x24, 0, 1, (byte) 0xa0, 0x01, 0x0a, 0, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
        0, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0, 0, 0, 0, 0, 0, 0, 0x01,
        0x04, (byte) 0xe4, 0, 0x04, 0x08, 0x08, 0x04, 0x04 // SRv6 SID Structure SubTlvs
    };

    private static final byte[] NODE_ATTR = {
        0x01, 0x07, 0, 0x04, 0, 0x2a, 0, 0x2b, 0x04, 0, 0, 0x01, (byte) 0xbc, 0x04, 0x02, 0, 0x05, 0x31, 0x32, 0x4b,
        0x2d, 0x32, 0x04, 0x03, 0, 0x01, 0x72, 0x04, 0x03, 0, 0x01, 0x73, 0x04, 0x04, 0, 0x04, 0x29, 0x29, 0x29, 0x29
    };

    private static final byte[] NODE_ATTR_SR = {
        0x01, 0x07, 0, 0x04, 0, 0x2a, 0, 0x2b, 0x04, 0, 0, 0x01, (byte) 0xbc, 0x04, 0x02, 0, 0x05, 0x31, 0x32, 0x4b,
        0x2d, 0x32, 0x04, 0x03, 0, 0x01, 0x72, 0x04, 0x03, 0, 0x01, 0x73, 0x04, 0x04, 0, 0x04, 0x29, 0x29, 0x29, 0x29,
        0x04, 0x0a, 0, 0x0d, (byte) 0xc0, 0, 0, 0, 0x0a, 0x04, (byte) 0x89, 0, 0x04, 0x01, 0x02, 0x03, 0x04, // sr-caps
        0x04, 0x0b, 0, 0x02, 0, 0x01, // sr-algorithms
        0x04, 0x0c, 0, 0x0c, 0, 0, 0, 0, 0x0a, 0x04, (byte) 0x89, 0, 0x03, 0, 0x01, 0x02, // SRLB
        0x01, 0x0a, 0, 0x04, 0x01, 0x0a, 0x02, 0x08, // MSD
        0x04, 0x0d, 0, 0x01, (byte)0xdd, // SRMS
        0x04, 0x0f, 0, 0x32, (byte)0x80, 0x01, 0x01, 0x0a,  // Flex-Algo Definition + Flex-Algo SubTLVS bellow
        0x04, 0x10, 0, 0x04, 0, 0, 0, 0x01,    // exclude-any
        0x04, 0x11, 0, 0x04, 0, 0, 0, 0,    // include-any
        0x04, 0x12, 0, 0x04, 0, 0, 0, 0x02,    // include-all
        0x04, 0x13, 0, 0x04, (byte)0x80, 0, 0, 0,    // flags
        0x04, 0x15, 0, 0x04, 0, 0, 0, 0x0a, // exclude-SRLG
        0x04, 0x16, 0, 0x02, 0x01, 0x0a        // unsupported TLVs
    };

    private static final byte[] NODE_ATTR_SRV6 = {
        0x01, 0x07, 0, 0x04, 0, 0x2a, 0, 0x2b, 0x04, 0, 0, 0x01, (byte) 0xbc, 0x04, 0x02, 0, 0x05, 0x31, 0x32, 0x4b,
        0x2d, 0x32, 0x04, 0x03, 0, 0x01, 0x72, 0x04, 0x03, 0, 0x01, 0x73, 0x04, 0x04, 0, 0x04, 0x29, 0x29, 0x29, 0x29,
        0x04, 0x0e, 0, 0x04, 0x40, 0, 0, 0 // SRv6 Capabilities
    };

    private static final byte[] P4_ATTR = {
        0x04, (byte) 0x80, 0, 0x01, (byte) 0xF0, 0x04, (byte) 0x81, 0, 0x08, 0x12, 0x34, 0x56, 0x78,
        0x10, 0x30, 0x50, 0x70, 0x04, (byte) 0x82, 0, 0x08, 0x12, 0x34, 0x56, 0x78, 0x10, 0x30, 0x50, 0x70,
        0x04, (byte) 0x83, 0, 0x04, 0, 0, 0, 0x0a, 0x04, (byte) 0x84, 0, 0x04, 0x0a, 0x19, 0x02, 0x1b,
        0x04, (byte) 0x87, 0, 0x10, 0, 0, 0, 0x05, // range tlv
        0x04, (byte) 0x86, 0, 0x08, (byte) 0xf0, 0, 0, 0, 0x01, 0x02, 0x03, 0x04, // prefix-sid tlv
        0x04, (byte) 0x92, 0, 0x01, 0x60, // Prefix Attribute Flags
        0x04, (byte) 0x93, 0, 0x10, 0, 0x01, 0, 0x02, 0, 0x03, 0, 0x04, 0, 0, 0, 0, 0, 0, 0, 0x02, // Source Router ID
        0x04, (byte) 0x96, 0, 0x04, 0xa, 0, 0, 0x01 // Source OSPF Router ID
    };

    private static final byte[] P6_ATTR = {
        0x04, (byte) 0x80, 0, 0x01, (byte) 0xF0, 0x04, (byte) 0x81, 0, 0x08, 0x12, 0x34, 0x56, 0x78,
        0x10, 0x30, 0x50, 0x70, 0x04, (byte) 0x82, 0, 0x08, 0x12, 0x34, 0x56, 0x78, 0x10, 0x30, 0x50, 0x70,
        0x04, (byte) 0x83, 0, 0x04, 0, 0, 0, 0x0a,
        0x04, (byte) 0x8a, 0, 0x08, (byte) 0x80, 0x01, 0, 0, 0, 0, 0, 0xa // SRv6 Locator
    };

    private LinkstateAttributeParser parser;

    @Before
    public final void setUp() {
        parser = new LinkstateAttributeParser(false);
    }

    private static AttributesBuilder createBuilder(final ObjectType type) {
        return new AttributesBuilder().addAugmentation(new AttributesReachBuilder()
            .setMpReachNlri(new MpReachNlriBuilder()
                .setAfi(LinkstateAddressFamily.VALUE)
                .setSafi(LinkstateSubsequentAddressFamily.VALUE)
                .setAdvertizedRoutes(new AdvertizedRoutesBuilder()
                    .setDestinationType(new DestinationLinkstateCaseBuilder()
                        .setDestinationLinkstate(new DestinationLinkstateBuilder()
                            .setCLinkstateDestination(List.of(new CLinkstateDestinationBuilder()
                                .setObjectType(type)
                                .setProtocolId(ProtocolId.IsisLevel1)
                                .build()))
                            .build())
                        .build())
                    .build())
                .build())
            .build());
    }

    private static AttributesBuilder createUnreachBuilder(final ObjectType type) {
        return new AttributesBuilder().addAugmentation(new AttributesUnreachBuilder()
            .setMpUnreachNlri(new MpUnreachNlriBuilder()
                .setAfi(LinkstateAddressFamily.VALUE)
                .setSafi(LinkstateSubsequentAddressFamily.VALUE)
                .setWithdrawnRoutes(new WithdrawnRoutesBuilder()
                    .setDestinationType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                        .linkstate.rev241219.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                        .DestinationLinkstateCaseBuilder()
                            .setDestinationLinkstate(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                                .yang.bgp.linkstate.rev241219.update.attributes.mp.unreach.nlri.withdrawn.routes
                                .destination.type.destination.linkstate._case.DestinationLinkstateBuilder()
                                    .setCLinkstateDestination(List.of(new CLinkstateDestinationBuilder()
                                        .setObjectType(type)
                                        .setProtocolId(ProtocolId.IsisLevel1)
                                        .build()))
                                    .build())
                            .build())
                    .build())
                .build())
            .build());
    }

    @Test
    public void testGetNlriType() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf b = Unpooled.buffer();
        AttributesBuilder builder = new AttributesBuilder();
        parser.parseAttribute(b, builder, null);
        assertEquals(0, b.readableBytes());
        builder = new AttributesBuilder();

        final AttributesReachBuilder builder1 = new AttributesReachBuilder();
        builder.addAugmentation(builder1.build());
        parser.parseAttribute(b, builder, null);
        assertEquals(0, b.readableBytes());
        builder = new AttributesBuilder();

        builder.addAugmentation(builder1
            .setMpReachNlri(new MpReachNlriBuilder()
                .setAdvertizedRoutes(new AdvertizedRoutesBuilder()
                    .setDestinationType(new DestinationIpv4CaseBuilder().build())
                    .build())
                .build())
            .build());
        parser.parseAttribute(b, builder, null);
        assertEquals(0, b.readableBytes());
        builder = new AttributesBuilder();

        final AttributesUnreachBuilder builder2 = new AttributesUnreachBuilder();
        builder.addAugmentation(builder2.build());
        parser.parseAttribute(b, builder, null);
        assertEquals(0, b.readableBytes());
        builder = new AttributesBuilder();

        builder.addAugmentation(builder2
            .setMpUnreachNlri(new MpUnreachNlriBuilder()
                .setWithdrawnRoutes(new WithdrawnRoutesBuilder()
                    .setDestinationType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet
                        .rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                        .DestinationIpv6CaseBuilder().build())
                    .build())
                .build())
            .build());
        parser.parseAttribute(b, builder, null);
        assertEquals(0, b.readableBytes());
    }

    @Test
    public void testPositiveLinks() throws BGPParsingException, BGPDocumentedException {
        final AttributesBuilder builder = createBuilder(new LinkCaseBuilder().build());
        parser.parseAttribute(Unpooled.copiedBuffer(LINK_ATTR), builder, null);
        final Attributes1 attrs = builder.augmentation(Attributes1.class);
        final LinkAttributes ls = ((LinkAttributesCase) attrs.getLinkStateAttribute()).getLinkAttributes();
        assertNotNull(ls);

        assertEquals("42.42.42.42", ls.getLocalIpv4RouterId().getValue());
        assertEquals("43.43.43.43", ls.getRemoteIpv4RouterId().getValue());
        assertEquals(Uint32.ZERO, ls.getAdminGroup().getValue());
        assertArrayEquals(new byte[] { (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80 },
            ls.getMaxLinkBandwidth().getValue());
        assertArrayEquals(new byte[] { (byte) 0x46, (byte) 0x43, (byte) 0x50, (byte) 0x00 },
            ls.getMaxReservableBandwidth().getValue());
        assertNotNull(ls.getUnreservedBandwidth());
        assertEquals(8, ls.getUnreservedBandwidth().size());
        assertEquals(LinkProtectionType.Dedicated1to1, ls.getLinkProtection());
        assertTrue(ls.getMplsProtocol().getLdp());
        assertTrue(ls.getMplsProtocol().getRsvpte());
        assertEquals(Uint32.TEN, ls.getMetric().getValue());
        assertEquals(2, ls.getSharedRiskLinkGroups().size());
        assertEquals(305419896, ls.getSharedRiskLinkGroups().iterator().next().getValue().intValue());
        assertEquals("12K-2", ls.getLinkName());
        final IsisAdjFlagsCase flags = new IsisAdjFlagsCaseBuilder()
                .setIsisAdjFlags(new IsisAdjFlagsBuilder()
                    .setAddressFamily(Boolean.TRUE)
                    .setBackup(Boolean.FALSE)
                    .setValue(Boolean.TRUE)
                    .setLocal(Boolean.TRUE)
                    .setSet(Boolean.FALSE)
                    .setPersistent(Boolean.FALSE)
                    .build())
                .build();
        assertEquals(flags, ls.getSrAdjIds().get(0).getFlags());
        assertEquals(flags, ls.getSrAdjIds().get(1).getFlags());
        assertEquals(Uint32.valueOf(1048575L),
            ((LabelCase)ls.getSrAdjIds().get(0).getSidLabelIndex()).getLabel().getValue());
        assertEquals(Uint32.valueOf(1048559L),
            ((LabelCase)ls.getSrAdjIds().get(1).getSidLabelIndex()).getLabel().getValue());
        assertEquals(Uint32.valueOf(168496141L), ((SidCase) ls.getPeerNodeSid().getSidLabelIndex()).getSid());
        assertEquals(Uint8.valueOf(5), ls.getPeerNodeSid().getWeight().getValue());
        assertEquals(Uint32.valueOf(168496142L), ((SidCase) ls.getPeerSetSids().get(0).getSidLabelIndex()).getSid());
        assertEquals(Uint8.valueOf(5), ls.getPeerSetSids().get(0).getWeight().getValue());
        assertEquals(Uint32.valueOf(168496143L), ((SidCase) ls.getPeerAdjSid().getSidLabelIndex()).getSid());
        assertEquals(Uint8.valueOf(5), ls.getPeerAdjSid().getWeight().getValue());

        // Performance Metrics
        assertEquals(Uint32.valueOf(10000L), ls.getLinkDelay().getValue());
        assertEquals(Uint32.valueOf(5000L), ls.getLinkMinMaxDelay().getMinDelay().getValue());
        assertEquals(Uint32.valueOf(20000L), ls.getLinkMinMaxDelay().getMaxDelay().getValue());
        assertEquals(Uint32.valueOf(10000L), ls.getDelayVariation().getValue());
        assertEquals(Uint32.ZERO, ls.getLinkLoss().getValue());
        assertArrayEquals(new byte[] { (byte) 0x46, (byte) 0x43, (byte) 0x50, (byte) 0x00 },
                ls.getResidualBandwidth().getValue());
        assertArrayEquals(new byte[] { (byte) 0x46, (byte) 0x43, (byte) 0x50, (byte) 0x00 },
                ls.getAvailableBandwidth().getValue());
        assertArrayEquals(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 },
                ls.getUtilizedBandwidth().getValue());

        // serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        // The LINK_ATTR buffer is now greater than 255 bytes. Need to skip one more byte
        buff.skipBytes(4);
        // there is unresolved TLV at the end, that needs to be cut off

        assertArrayEquals(ByteArray.subByte(LINK_ATTR, 0, LINK_ATTR.length - 5), ByteArray.getAllBytes(buff));
    }

    @Test
    public void testSRv6Link() throws BGPParsingException, BGPDocumentedException {
        final AttributesBuilder builder = createBuilder(new LinkCaseBuilder().build());
        parser.parseAttribute(Unpooled.copiedBuffer(LINK_ATTR_SRV6), builder, null);
        final Attributes1 attrs = builder.augmentation(Attributes1.class);
        final LinkAttributes ls = ((LinkAttributesCase) attrs.getLinkStateAttribute()).getLinkAttributes();
        assertNotNull(ls);

        // Local and Remote RouterID IPv6
        assertNotNull(ls.getLocalIpv6RouterId());
        assertEquals("102:304:506:708::1", ls.getLocalIpv6RouterId().getValue());
        assertNotNull(ls.getRemoteIpv6RouterId());
        assertEquals("102:304:506:708::2", ls.getRemoteIpv6RouterId().getValue());

        // SRv6 End X SID
        assertNotNull(ls.getSrv6EndXSid());
        assertEquals(1, ls.getSrv6EndXSid().getEndpointBehavior().intValue());
        assertFalse(ls.getSrv6EndXSid().getFlags().getBackup());
        assertTrue(ls.getSrv6EndXSid().getFlags().getSet());
        assertTrue(ls.getSrv6EndXSid().getFlags().getPersistent());
        assertEquals(1, ls.getSrv6EndXSid().getAlgo().intValue());
        assertEquals(10, ls.getSrv6EndXSid().getWeight().intValue());
        assertArrayEquals(new byte [] {0, 1, 2, 3, 4, 5, 6, 7, 0, 0, 0, 0, 0, 0, 0, 1},
            ls.getSrv6EndXSid().getSid().getValue());
        assertNotNull(ls.getSrv6EndXSid().getSrv6SidStructure());
        assertEquals(8, ls.getSrv6EndXSid().getSrv6SidStructure().getLocatorBlockLength().intValue());
        assertEquals(8, ls.getSrv6EndXSid().getSrv6SidStructure().getLocatorNodeLength().intValue());
        assertEquals(4, ls.getSrv6EndXSid().getSrv6SidStructure().getFunctionLength().intValue());
        assertEquals(4, ls.getSrv6EndXSid().getSrv6SidStructure().getArgumentLength().intValue());

        // SRv6 LAN End X SID
        assertNotNull(ls.getSrv6LanEndXSid());
        assertEquals(1, ls.getSrv6LanEndXSid().getEndpointBehavior().intValue());
        assertTrue(ls.getSrv6LanEndXSid().getFlags().getBackup());
        assertFalse(ls.getSrv6LanEndXSid().getFlags().getSet());
        assertTrue(ls.getSrv6LanEndXSid().getFlags().getPersistent());
        assertEquals(1, ls.getSrv6LanEndXSid().getAlgo().intValue());
        assertEquals(10, ls.getSrv6LanEndXSid().getWeight().intValue());
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6},
            ((IsisNeighborCase) ls.getSrv6LanEndXSid().getNeighborType()).getIsoSystemId().getValue());
        assertArrayEquals(new byte [] {0, 1, 2, 3, 4, 5, 6, 7, 0, 0, 0, 0, 0, 0, 0, 1},
            ls.getSrv6EndXSid().getSid().getValue());
        assertNotNull(ls.getSrv6LanEndXSid().getSrv6SidStructure());
        assertEquals(8, ls.getSrv6LanEndXSid().getSrv6SidStructure().getLocatorBlockLength().intValue());
        assertEquals(8, ls.getSrv6LanEndXSid().getSrv6SidStructure().getLocatorNodeLength().intValue());
        assertEquals(4, ls.getSrv6LanEndXSid().getSrv6SidStructure().getFunctionLength().intValue());
        assertEquals(4, ls.getSrv6LanEndXSid().getSrv6SidStructure().getArgumentLength().intValue());

        // serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        assertArrayEquals(LINK_ATTR_SRV6, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPositiveNodes() throws BGPParsingException, BGPDocumentedException {
        final AttributesBuilder builder = createBuilder(new NodeCaseBuilder().build());
        parser.parseAttribute(Unpooled.copiedBuffer(NODE_ATTR), builder, null);

        final Attributes1 attrs = builder.augmentation(Attributes1.class);
        final NodeAttributes ls = ((NodeAttributesCase) attrs.getLinkStateAttribute()).getNodeAttributes();
        assertNotNull(ls);

        assertEquals(2, ls.getTopologyIdentifier().size());
        assertEquals(Uint16.valueOf(42), ls.getTopologyIdentifier().iterator().next().getValue());
        assertTrue(ls.getNodeFlags().getOverload());
        assertFalse(ls.getNodeFlags().getAttached());
        assertTrue(ls.getNodeFlags().getExternal());
        assertTrue(ls.getNodeFlags().getAbr());
        assertTrue(ls.getNodeFlags().getRouter());
        assertTrue(ls.getNodeFlags().getV6());

        assertEquals("12K-2", ls.getDynamicHostname());
        assertEquals(2, ls.getIsisAreaId().size());
        assertEquals("41.41.41.41", ls.getIpv4RouterId().getValue());

        // serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        assertArrayEquals(NODE_ATTR, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testSrNodesAttributes() throws BGPParsingException, BGPDocumentedException {
        final AttributesBuilder builder = createBuilder(new NodeCaseBuilder().build());
        parser.parseAttribute(Unpooled.copiedBuffer(NODE_ATTR_SR), builder, null);

        final Attributes1 attrs = builder.augmentation(Attributes1.class);
        final NodeAttributes ls = ((NodeAttributesCase) attrs.getLinkStateAttribute()).getNodeAttributes();
        assertNotNull(ls);

        // SR Capabilities
        assertNotNull(ls.getSrCapabilities());
        assertTrue(ls.getSrCapabilities().getMplsIpv4());
        assertTrue(ls.getSrCapabilities().getMplsIpv6());
        assertEquals(Uint32.valueOf(10), ls.getSrCapabilities().getSrgb().getFirst().getRangeSize().getValue());
        assertEquals(Uint32.valueOf(16909060L),
                ((SidCase) ls.getSrCapabilities().getSrgb().getFirst().getSidLabelIndex()).getSid());
        // SR Algorithm
        assertNotNull(ls.getSrAlgorithm());
        assertEquals(Set.of(Algorithm.ShortestPathFirst, Algorithm.StrictShortestPathFirst),
                ls.getSrAlgorithm().getAlgorithms());
        // SRLB
        assertNotNull(ls.getSrLocalBlock());
        assertEquals(10, ls.getSrLocalBlock().getSrlb().getFirst().getRangeSize().getValue().intValue());
        assertEquals(Uint32.valueOf(258L),
                ((LabelCase) ls.getSrLocalBlock().getSrlb().getFirst().getSidLabelIndex()).getLabel().getValue());
        // SRMS
        assertNotNull(ls.getSrms());
        assertEquals(221, ls.getSrms().getPreference().getValue().intValue());
        // Node MSD
        final List<Msd> msds = new ArrayList<Msd>();
        msds.add(new MsdBuilder().setType(MsdType.forValue(1)).setValue(Uint8.valueOf(10)).build());
        msds.add(new MsdBuilder().setType(MsdType.forValue(2)).setValue(Uint8.valueOf(8)).build());
        assertNotNull(ls.getNodeMsd());
        assertEquals(msds, ls.getNodeMsd().getMsd());
        // Flex Algo Definition
        final FlexAlgoDefinitionTlv fadTlv = new FlexAlgoDefinitionTlvBuilder()
            .setFlexAlgo(new FlexAlgo(Uint8.valueOf(128)))
            .setMetricType(FlexMetric.Delay)
            .setCalcType(Uint8.ONE)
            .setPriority(Uint8.TEN)
            .setFlexAlgoSubtlvs(new FlexAlgoSubtlvsBuilder()
                .setExcludeAny(Set.of(new ExtendedAdminGroup(Uint32.ONE)))
                .setIncludeAny(Set.of(new ExtendedAdminGroup(Uint32.ZERO)))
                .setIncludeAll(Set.of(new ExtendedAdminGroup(Uint32.TWO)))
                .setFlags(new FlexAlgoDefinitionFlag(true))
                .setExcludeSrlg(Set.of(new SrlgId(Uint32.TEN)))
                .setUnsupportedTlv(new UnsupportedTlvBuilder()
                        .setProtocolId(Uint8.valueOf(ProtocolId.IsisLevel1.getIntValue()))
                        .setProtocolType(Set.of(Uint16.TEN))
                        .build())
                .build())
            .build();
        assertNotNull(ls.getFlexAlgoDefinition());
        assertEquals(new FlexAlgoDefinitionBuilder().setFlexAlgoDefinitionTlv(List.of(fadTlv)).build(),
                ls.getFlexAlgoDefinition());

        // serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        assertArrayEquals(NODE_ATTR_SR, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testSRv6NodesAttributes() throws BGPParsingException, BGPDocumentedException {
        final AttributesBuilder builder = createBuilder(new NodeCaseBuilder().build());
        parser.parseAttribute(Unpooled.copiedBuffer(NODE_ATTR_SRV6), builder, null);

        final Attributes1 attrs = builder.augmentation(Attributes1.class);
        final NodeAttributes ls = ((NodeAttributesCase) attrs.getLinkStateAttribute()).getNodeAttributes();
        assertNotNull(ls);

        // SRv6 Capabilities
        assertNotNull(ls.getSrv6Capabilities());
        assertTrue(ls.getSrv6Capabilities().getOFlag());

        // serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        assertArrayEquals(NODE_ATTR_SRV6, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPositiveV4Prefixes() throws BGPParsingException, BGPDocumentedException {
        final AttributesBuilder builder = createUnreachBuilder(new PrefixCaseBuilder().setPrefixDescriptors(
            new PrefixDescriptorsBuilder().setIpReachabilityInformation(new IpPrefix(new Ipv4Prefix("127.0.0.1/32")))
            .build()).build());
        parser.parseAttribute(Unpooled.copiedBuffer(P4_ATTR), builder, null);

        final Attributes1 attrs = builder.augmentation(Attributes1.class);
        final PrefixAttributes ls = ((PrefixAttributesCase) attrs.getLinkStateAttribute()).getPrefixAttributes();
        assertNotNull(ls);

        assertNotNull(ls.getSrRange());
        assertEquals(5, ls.getSrRange().getRangeSize().intValue());
        final IgpBits ispBits = ls.getIgpBits();
        assertTrue(ispBits.getIsIsUpDown());
        assertTrue(ispBits.getOspfNoUnicast());
        assertTrue(ispBits.getOspfLocalAddress());
        assertTrue(ispBits.getOspfPropagateNssa());
        assertEquals(2, ls.getRouteTags().size());
        assertArrayEquals(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 },
            ls.getRouteTags().iterator().next().getValue());
        assertEquals(1, ls.getExtendedTags().size());
        assertArrayEquals(new byte[] {
            (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x10, (byte) 0x30, (byte) 0x50, (byte) 0x70
        }, ls.getExtendedTags().iterator().next().getValue());
        assertEquals(10, ls.getPrefixMetric().getValue().intValue());
        assertEquals("10.25.2.27", ls.getOspfForwardingAddress().getIpv4AddressNoZone().getValue());
        assertTrue(ls.getAttributeFlags().getNodeFlag());
        assertTrue(((IsisAttributeFlagsCase) ls.getAttributeFlags().getIgpAttributeFlags()).getReAdvertisementFlag());
        assertEquals("10.0.0.1", ls.getSourceOspfRouterId().getValue());
        assertEquals("1:2:3:4::2", ls.getSourceRouterId().getIpv6AddressNoZone().getValue());

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        assertArrayEquals(P4_ATTR, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPositiveV6Prefixes() throws BGPParsingException, BGPDocumentedException {
        final AttributesBuilder builder = createUnreachBuilder(new PrefixCaseBuilder().setPrefixDescriptors(
            new PrefixDescriptorsBuilder().setIpReachabilityInformation(
                new IpPrefix(new Ipv6Prefix("0102:0304:0506:0708::1/128"))).build())
            .build());
        parser.parseAttribute(Unpooled.copiedBuffer(P6_ATTR), builder, null);

        final Attributes1 attrs = builder.augmentation(Attributes1.class);
        final PrefixAttributes ls = ((PrefixAttributesCase) attrs.getLinkStateAttribute()).getPrefixAttributes();
        assertNotNull(ls);

        final IgpBits ispBits = ls.getIgpBits();
        assertTrue(ispBits.getIsIsUpDown());
        assertTrue(ispBits.getOspfNoUnicast());
        assertTrue(ispBits.getOspfLocalAddress());
        assertTrue(ispBits.getOspfPropagateNssa());
        assertEquals(2, ls.getRouteTags().size());
        assertArrayEquals(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 },
            ls.getRouteTags().iterator().next().getValue());
        assertEquals(1, ls.getExtendedTags().size());
        assertArrayEquals(new byte[] {
            (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x10, (byte) 0x30, (byte) 0x50, (byte) 0x70
        }, ls.getExtendedTags().iterator().next().getValue());
        assertEquals(10, ls.getPrefixMetric().getValue().intValue());

        // SRv6 Locator
        assertNotNull(ls.getSrv6Locator());
        assertTrue(ls.getSrv6Locator().getFlags().getUpDown());
        assertEquals(1, ls.getSrv6Locator().getAlgo().intValue());
        assertEquals(10, ls.getSrv6Locator().getMetric().intValue());

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        assertArrayEquals(P6_ATTR, ByteArray.getAllBytes(buff));
    }
}
