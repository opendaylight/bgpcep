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
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.rsvp.parser.impl.RSVPActivator;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderContext;
import org.opendaylight.protocol.rsvp.parser.spi.pojo.SimpleRSVPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.TeLspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.TeLspAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.te.lsp.attributes._case.TeLspAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.IgpBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.IsisAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.IsisAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.isis.adj.flags._case.IsisAdjFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.LocalLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AssociationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.association.object.AssociationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.tspec.object.TspecObject;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class LinkstateAttributeParserTest {

    private static final byte[] TE_LSP_ATTR = {
        0x00, (byte) 0x63, 0x00, (byte) 0x30, // TE LSP Attribute Type, length, value
        0x00, (byte) 0x20, (byte) 0x0c, 0x02,  // Length, Class, Ctype
        0x00, 0x00, 0x00, 0x07,
        0x01, 0x00, 0x00, 0x06,
        (byte) 0x7f, 0x00, 0x00, 0x05,
        0x00, 0x00, 0x00, 0x01, //Token Bucket Rate
        0x00, 0x00, 0x00, 0x02, //Token Bucket Size
        0x00, 0x00, 0x00, 0x03, //Peak Data Rate
        0x00, 0x00, 0x00, 0x04, //Minimum Policed Unit
        0x00, 0x00, 0x00, 0x05, //Maximum Packet Size
        0x00, (byte) 0x08, (byte) 0xc7, 0x01,  // Length, Class, Ctype
        0x00, 0x01, 0x00, 0x02,
        0x01, 0x02, 0x03, 0x04
    };

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
        0x04, (byte) 0x88, 0, 0x01, 0x0a
    };

    private static final byte[] NODE_ATTR = {
        0x01, 0x07, 0, 0x04, 0, 0x2a, 0, 0x2b, 0x04, 0, 0, 0x01, (byte) 0xbc, 0x04, 0x02, 0, 0x05, 0x31, 0x32, 0x4b,
        0x2d, 0x32, 0x04, 0x03, 0, 0x01, 0x72, 0x04, 0x03, 0, 0x01, 0x73, 0x04, 0x04, 0, 0x04, 0x29, 0x29, 0x29, 0x29,
        0x04, (byte) 0x88, 0, 0x01, 0x0a,
        4, 0x0a, 0, 0x0c, (byte)0xe0, 0, 1, 2, 3, 4, (byte)0x89, 0, 3, 1, 2, 0, // sr-caps
        4, 0x0b, 0, 2, 0, 1 // sr-algorythms
    };

    private static final byte[] NODE_ATTR_S = {
        0x01, 0x07, 0, 0x04, 0, 0x2a, 0, 0x2b, 0x04, 0, 0, 0x01, (byte) 0xbc, 0x04, 0x02, 0, 0x05, 0x31, 0x32, 0x4b,
        0x2d, 0x32, 0x04, 0x03, 0, 0x01, 0x72, 0x04, 0x03, 0, 0x01, 0x73, 0x04, 0x04, 0, 0x04, 0x29, 0x29, 0x29, 0x29,
        4, 0x0a, 0, 0x0c, (byte)0xe0, 0, 1, 2, 3, 4, (byte)0x89, 0, 3, 1, 2, 0, // sr-caps
        4, 0x0b, 0, 2, 0, 1 // sr-algorythms
    };

    private static final byte[] P4_ATTR = {
        0x04, (byte) 0x80, 0, 0x01, (byte) 0xF0, 0x04, (byte) 0x81, 0, 0x08, 0x12, 0x34, 0x56, 0x78,
        0x10, 0x30, 0x50, 0x70, 0x04, (byte) 0x82, 0, 0x08, 0x12, 0x34, 0x56, 0x78, 0x10, 0x30, 0x50, 0x70,
        0x04, (byte) 0x83, 0, 0x04, 0, 0, 0, 0x0a, 0x04, (byte) 0x84, 0, 0x04, 0x0a, 0x19, 0x02, 0x1b,
        4, (byte)0x86, 0,8, (byte)0xf0, 0, 0,0, 1,2,3,4, // prefix-sid tlv
        4, (byte)0x87, 0,0x0c, 0, 0, 0, 5, 4, (byte)0x89, 0, 4, 1,2,3,4, // range tlv
        4, (byte)0x88, 0, 4, 1, (byte)0xf0, 0, 0 // binding sid tlv
    };

    private RSVPExtensionProviderContext context;
    private RSVPActivator rsvpActivator;
    private LinkstateAttributeParser parser;

    @Before
    public final void setUp() {
        context = new SimpleRSVPExtensionProviderContext();
        rsvpActivator = new RSVPActivator();
        rsvpActivator.start(context);
        parser = new LinkstateAttributeParser(false,context.getRsvpRegistry());
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
                        .linkstate.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                        .DestinationLinkstateCaseBuilder()
                            .setDestinationLinkstate(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                                .yang.bgp.linkstate.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes
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
                    .setSet(Boolean.FALSE)
                    .build())
                .build();
        assertEquals(flags, ls.getSrAdjIds().get(0).getFlags());
        assertEquals(flags, ls.getSrAdjIds().get(1).getFlags());
        assertEquals(Uint32.valueOf(1048575L),
            ((LocalLabelCase)ls.getSrAdjIds().get(0).getSidLabelIndex()).getLocalLabel().getValue());
        assertEquals(Uint32.valueOf(1048559L),
            ((LocalLabelCase)ls.getSrAdjIds().get(1).getSidLabelIndex()).getLocalLabel().getValue());
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

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        // The LINK_ATTR buffer is now greater than 255 bytes. Need to skip one more byte
        buff.skipBytes(4);
        // there is unresolved TLV at the end, that needs to be cut off

        assertArrayEquals(ByteArray.subByte(LINK_ATTR, 0, LINK_ATTR.length - 5), ByteArray.getAllBytes(buff));
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

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        assertArrayEquals(NODE_ATTR_S, ByteArray.getAllBytes(buff));
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
        assertFalse(ls.getSrRange().getInterArea());
        assertEquals(1, ls.getSrRange().getSubTlvs().size());
        assertNotNull(ls.getSrBindingSidLabels());
        final IgpBits ispBits = ls.getIgpBits();
        assertTrue(ispBits.getUpDown().getUpDown());
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

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        // there is unresolved TLV at the end, that needs to be cut off
        assertArrayEquals(P4_ATTR, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPositiveTELspAttribute() throws BGPParsingException, BGPDocumentedException {
        final AttributesBuilder builder = createBuilder(new TeLspCaseBuilder().build());
        parser.parseAttribute(Unpooled.copiedBuffer(TE_LSP_ATTR), builder, null);

        final Attributes1 attrs = builder.augmentation(Attributes1.class);
        final TeLspAttributes teLspAttributes = ((TeLspAttributesCase) attrs.getLinkStateAttribute())
                .getTeLspAttributes();
        assertNotNull(teLspAttributes);
        final TspecObject tSpec = teLspAttributes.getTspecObject();
        assertNotNull(tSpec);
        assertEquals(new Float32(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01}),
            tSpec.getTokenBucketRate());
        assertEquals(new Float32(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02}),
            teLspAttributes.getTspecObject().getTokenBucketSize());
        assertEquals(new Float32(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03}),
            tSpec.getPeakDataRate());
        assertEquals(Uint32.valueOf(4), tSpec.getMinimumPolicedUnit());
        assertEquals(Uint32.valueOf(5), tSpec.getMaximumPacketSize());

        final AssociationObject associationObject = teLspAttributes.getAssociationObject();
        assertEquals(AssociationType.Recovery, associationObject.getAssociationType());
        final IpAddressNoZone ipv4 = new IpAddressNoZone(Ipv4Util.addressForByteBuf(Unpooled.copiedBuffer(
            new byte[]{0x01, 0x02, 0x03, 0x04})));
        assertEquals(ipv4, associationObject.getIpAddress());
        final short associationId = 2;
        assertEquals(associationId, associationObject.getAssociationId().shortValue());

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeAttribute(builder.build(), buff);
        assertArrayEquals(TE_LSP_ATTR, ByteArray.getAllBytes(buff));
        assertTrue(Arrays.equals(TE_LSP_ATTR, ByteArray.getAllBytes(buff)));
    }
}
