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
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.LinkAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.NodeAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.PrefixAttributesParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.rsvp.parser.impl.RSVPActivator;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderContext;
import org.opendaylight.protocol.rsvp.parser.spi.pojo.SimpleRSVPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.TeLspAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.te.lsp.attributes._case.TeLspAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.IgpBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.adj.flags.flags.IsisAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.adj.flags.flags.IsisAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.LocalLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AssociationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.association.object.AssociationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.tspec.object.TspecObject;

public class LinkstateAttributeParserTest {

    private static final byte[] TE_LSP_ATTR = {0x00, (byte) 0x63, 0x00, (byte) 0x30, // TE LSP Attribute Type, lenght, value
        0x00, (byte) 0x20, (byte) 0x0c, 0x02,  // Lenght, Class, Ctype
        0x00, 0x00, 0x00, 0x07,
        0x01, 0x00, 0x00, 0x06,
        (byte) 0x7f, 0x00, 0x00, 0x05,
        0x00, 0x00, 0x00, 0x01, //Token Bucket Rate
        0x00, 0x00, 0x00, 0x02, //Token Bucket Size
        0x00, 0x00, 0x00, 0x03, //Peak Data Rate
        0x00, 0x00, 0x00, 0x04, //Minimum Policed Unit
        0x00, 0x00, 0x00, 0x05, //Maximum Packet Size
        0x00, (byte) 0x08, (byte) 0xc7, 0x01,  // Lenght, Class, Ctype
        0x00, 0x01, 0x00, 0x02,
        0x01, 0x02, 0x03, 0x04,};

    private static final byte[] LINK_ATTR = {0x04, 0x04, 0, 0x04, 0x2a, 0x2a, 0x2a, 0x2a, 0x04, 0x06, 0, 0x04, 0x2b, 0x2b, 0x2b, 0x2b,
        0x04, 0x40, 0, 0x04, 0, 0, 0, 0, 0x04, 0x41, 0, 0x04, 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, 0x04, 0x42, 0, 0x04,
        0x46, 0x43, 0x50, 0, 0x04, 0x43, 0, 0x20, 0x46, 0x43, 0x50, 0, 0x46, 0x43, 0x50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x04, 0x44, 0, 0x08, 0, 0, 0, 0, 0, 0, 0, 0, 0x04, 0x45, 0, 0x02, 0, 0x08, 0x04, 0x46, 0, 0x01,
        (byte) 0xc0, 0x04, 0x47, 0, 0x03, 0, 0, 0x0a, 0x04, 0x48, 0, 0x08, 0x12, 0x34, 0x56, 0x78, 0x10, 0x30, 0x50, 0x70, 0x04, 0x4a,
        0, 0x05, 0x31, 0x32, 0x4b, 0x2d, 0x32,
        0x04, 0x4b, 0, 0x07, (byte)-80, 10, 0, 0, (byte)0x0f, (byte)0xff, (byte)0xff, // sr-adj
        0x04, 0x4b, 0, 0x07, (byte)-80, 10, 0, 0, (byte)0x0f, (byte)0xff, (byte)0xef, // sr-adj
        0x04, 0x4c, 0, 0x0d, (byte)-80, 10, 0, 0, 1, 2, 3, 4, 5, 6, (byte)0x0f, (byte)0xff, (byte)0xff, // sr-lan-adj
        0x04, 0x4c, 0, 0x0d, (byte)-80, 10, 0, 0, 1, 2, 3, 4, 5, 6, (byte)0x0f, (byte)0xff, (byte)0xef, // sr-lan-adj
        0x04, 0x4d, 0, 0x08, 0, 0x05, 0, 0, 0x0a, 0x0b, 0x0c, 0x0d, // peer-node-sid
        0x04, 0x4e, 0, 0x08, 0, 0x05, 0, 0, 0x0a, 0x0b, 0x0c, 0x0f, // peer-adj-sid
        0x04, 0x4f, 0, 0x08, 0, 0x05, 0, 0, 0x0a, 0x0b, 0x0c, 0x0e, // peer-set-sid
        0x04, (byte) 0x88, 0, 0x01, 0x0a };

    private static final byte[] NODE_ATTR = { 0x01, 0x07, 0, 0x04, 0, 0x2a, 0, 0x2b, 0x04, 0, 0, 0x01, (byte) 0xbc, 0x04, 0x02, 0,
        0x05, 0x31, 0x32, 0x4b, 0x2d, 0x32, 0x04, 0x03, 0, 0x01, 0x72, 0x04, 0x03, 0, 0x01, 0x73, 0x04, 0x04, 0, 0x04,
        0x29, 0x29, 0x29, 0x29, 0x04, (byte) 0x88, 0, 0x01, 0x0a,
        4, 0x0a, 0, 0x0c, (byte)0xe0, 0, 1, 2, 3, 4, (byte)0x89, 0, 3, 1, 2, 0, // sr-caps
        4, 0x0b, 0, 2, 0, 1 // sr-algorythms
        };

    private static final byte[] NODE_ATTR_S = { 0x01, 0x07, 0, 0x04, 0, 0x2a, 0, 0x2b, 0x04, 0, 0, 0x01, (byte) 0xbc, 0x04, 0x02, 0,
        0x05, 0x31, 0x32, 0x4b, 0x2d, 0x32, 0x04, 0x03, 0, 0x01, 0x72, 0x04, 0x03, 0, 0x01, 0x73, 0x04, 0x04, 0, 0x04,
        0x29, 0x29, 0x29, 0x29,
        4, 0x0a, 0, 0x0c, (byte)0xe0, 0, 1, 2, 3, 4, (byte)0x89, 0, 3, 1, 2, 0, // sr-caps
        4, 0x0b, 0, 2, 0, 1 // sr-algorythms
        };

    private static final byte[] P4_ATTR = { 0x04, (byte) 0x80, 0, 0x01, (byte) 0xF0, 0x04, (byte) 0x81, 0, 0x08, 0x12, 0x34, 0x56, 0x78,
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
        this.context = new SimpleRSVPExtensionProviderContext();
        this.rsvpActivator = new RSVPActivator();
        this.rsvpActivator.start(this.context);
        this.parser = new LinkstateAttributeParser(false,this.context.getRsvpRegistry());
    }
    private static AttributesBuilder createBuilder(final ObjectType type) {
        return new AttributesBuilder().addAugmentation(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1.class,
            new Attributes1Builder().setMpReachNlri(
                new MpReachNlriBuilder().setAfi(LinkstateAddressFamily.class).setSafi(LinkstateSubsequentAddressFamily.class).setAdvertizedRoutes(
                    new AdvertizedRoutesBuilder().setDestinationType(
                        new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                            new DestinationLinkstateBuilder().setCLinkstateDestination(
                                Lists.newArrayList(new CLinkstateDestinationBuilder().setObjectType(type).setProtocolId(ProtocolId.IsisLevel1).build())).build()).build()).build()).build()).build());
    }

    private static AttributesBuilder createUnreachBuilder(final ObjectType type) {
        return new AttributesBuilder().addAugmentation(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2.class,
            new Attributes2Builder().setMpUnreachNlri(
                new MpUnreachNlriBuilder().setAfi(LinkstateAddressFamily.class).setSafi(LinkstateSubsequentAddressFamily.class).setWithdrawnRoutes(
                    new WithdrawnRoutesBuilder().setDestinationType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder().setCLinkstateDestination(
                                Lists.newArrayList(new CLinkstateDestinationBuilder().setObjectType(type).setProtocolId(ProtocolId.IsisLevel1).build())).build()).build()).build()).build()).build());
    }

    @Test
    public void testGetNlriType() throws BGPParsingException {
        final ByteBuf b = Unpooled.buffer();
        AttributesBuilder builder = new AttributesBuilder();
        this.parser.parseAttribute(b, builder);
        assertEquals(0, b.readableBytes());
        builder = new AttributesBuilder();

        final Attributes1Builder builder1 = new Attributes1Builder();
        builder.addAugmentation(Attributes1.class, builder1.build());
        this.parser.parseAttribute(b, builder);
        assertEquals(0, b.readableBytes());
        builder = new AttributesBuilder();

        builder.addAugmentation(Attributes1.class, builder1.setMpReachNlri(
            new MpReachNlriBuilder().setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationIpv4CaseBuilder().build()).build()).build()).build());
        this.parser.parseAttribute(b, builder);
        assertEquals(0, b.readableBytes());
        builder = new AttributesBuilder();

        final Attributes2Builder builder2 = new Attributes2Builder();
        builder.addAugmentation(Attributes2.class, builder2.build());
        this.parser.parseAttribute(b, builder);
        assertEquals(0, b.readableBytes());
        builder = new AttributesBuilder();

        builder.addAugmentation(Attributes2.class, builder2.setMpUnreachNlri(
            new MpUnreachNlriBuilder().setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6CaseBuilder().build()).build()).build()).build());
        this.parser.parseAttribute(b, builder);
        assertEquals(0, b.readableBytes());
    }

    @Test
    public void testPositiveLinks() throws BGPParsingException {
        final AttributesBuilder builder = createBuilder(new LinkCaseBuilder().build());
        this.parser.parseAttribute(Unpooled.copiedBuffer(LINK_ATTR), builder);
        final Attributes1 attrs = builder.getAugmentation(Attributes1.class);
        final LinkAttributes ls = ((LinkAttributesCase) attrs.getLinkStateAttribute()).getLinkAttributes();
        assertNotNull(ls);

        assertEquals("42.42.42.42", ls.getLocalIpv4RouterId().getValue());
        assertEquals("43.43.43.43", ls.getRemoteIpv4RouterId().getValue());
        assertEquals(Long.valueOf(0L), ls.getAdminGroup().getValue());
        assertArrayEquals(new byte[] { (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80 }, ls.getMaxLinkBandwidth().getValue());
        assertArrayEquals(new byte[] { (byte) 0x46, (byte) 0x43, (byte) 0x50, (byte) 0x00 }, ls.getMaxReservableBandwidth().getValue());
        assertNotNull(ls.getUnreservedBandwidth());
        assertEquals(8, ls.getUnreservedBandwidth().size());
        assertEquals(LinkProtectionType.Dedicated1to1, ls.getLinkProtection());
        assertTrue(ls.getMplsProtocol().isLdp());
        assertTrue(ls.getMplsProtocol().isRsvpte());
        assertEquals(new Long(10), ls.getMetric().getValue());
        assertEquals(2, ls.getSharedRiskLinkGroups().size());
        assertEquals(305419896, ls.getSharedRiskLinkGroups().get(0).getValue().intValue());
        assertEquals("12K-2", ls.getLinkName());
        final IsisAdjFlagsCase flags = new IsisAdjFlagsCaseBuilder().setAddressFamily(Boolean.TRUE).setBackup(Boolean.FALSE).setSet(Boolean.FALSE).build();
        assertEquals(flags, ls.getSrAdjIds().get(0).getFlags());
        assertEquals(flags, ls.getSrAdjIds().get(1).getFlags());
        assertEquals(new Long(1048575L), ((LocalLabelCase)ls.getSrAdjIds().get(0).getSidLabelIndex()).getLocalLabel().getValue());
        assertEquals(new Long(1048559L), ((LocalLabelCase)ls.getSrAdjIds().get(1).getSidLabelIndex()).getLocalLabel().getValue());
        assertEquals(new Long(168496141L), ((SidCase) ls.getPeerNodeSid().getSidLabelIndex()).getSid());
        assertEquals(new Short("5"), ls.getPeerNodeSid().getWeight().getValue());
        assertEquals(new Long(168496142L), ((SidCase) ls.getPeerSetSids().get(0).getSidLabelIndex()).getSid());
        assertEquals(new Short("5"), ls.getPeerSetSids().get(0).getWeight().getValue());
        assertEquals(new Long(168496143L), ((SidCase) ls.getPeerAdjSid().getSidLabelIndex()).getSid());
        assertEquals(new Short("5"), ls.getPeerAdjSid().getWeight().getValue());

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        this.parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        // there is unresolved TLV at the end, that needs to be cut off

        assertArrayEquals(ByteArray.subByte(LINK_ATTR, 0, LINK_ATTR.length -5), ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPositiveNodes() throws BGPParsingException {
        final AttributesBuilder builder = createBuilder(new NodeCaseBuilder().build());
        this.parser.parseAttribute(Unpooled.copiedBuffer(NODE_ATTR), builder);

        final Attributes1 attrs = builder.getAugmentation(Attributes1.class);
        final NodeAttributes ls = ((NodeAttributesCase) attrs.getLinkStateAttribute()).getNodeAttributes();
        assertNotNull(ls);

        assertEquals(2, ls.getTopologyIdentifier().size());
        assertEquals(42, ls.getTopologyIdentifier().get(0).getValue().intValue());
        assertTrue(ls.getNodeFlags().isOverload());
        assertFalse(ls.getNodeFlags().isAttached());
        assertTrue(ls.getNodeFlags().isExternal());
        assertTrue(ls.getNodeFlags().isAbr());
        assertTrue(ls.getNodeFlags().isRouter());
        assertTrue(ls.getNodeFlags().isV6());

        assertEquals("12K-2", ls.getDynamicHostname());
        assertEquals(2, ls.getIsisAreaId().size());
        assertEquals("41.41.41.41", ls.getIpv4RouterId().getValue());

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        this.parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        assertArrayEquals(NODE_ATTR_S, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPositiveV4Prefixes() throws BGPParsingException {
        final AttributesBuilder builder = createUnreachBuilder(new PrefixCaseBuilder().setPrefixDescriptors(
            new PrefixDescriptorsBuilder().setIpReachabilityInformation(new IpPrefix(new Ipv4Prefix("127.0.0.1/32"))).build()).build());
        this.parser.parseAttribute(Unpooled.copiedBuffer(P4_ATTR), builder);

        final Attributes1 attrs = builder.getAugmentation(Attributes1.class);
        final PrefixAttributes ls = ((PrefixAttributesCase) attrs.getLinkStateAttribute()).getPrefixAttributes();
        assertNotNull(ls);

        assertNotNull(ls.getSrRange());
        assertFalse(ls.getSrRange().isInterArea());
        assertEquals(1, ls.getSrRange().getSubTlvs().size());
        assertNotNull(ls.getSrBindingSidLabels());
        final IgpBits ispBits = ls.getIgpBits();
        assertTrue(ispBits.getUpDown().isUpDown());
        assertTrue(ispBits.isIsIsUpDown());
        assertTrue(ispBits.isOspfNoUnicast());
        assertTrue(ispBits.isOspfLocalAddress());
        assertTrue(ispBits.isOspfPropagateNssa());
        assertEquals(2, ls.getRouteTags().size());
        assertArrayEquals(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 }, ls.getRouteTags().get(0).getValue());
        assertEquals(1, ls.getExtendedTags().size());
        assertArrayEquals(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x10, (byte) 0x30, (byte) 0x50,
            (byte) 0x70 }, ls.getExtendedTags().get(0).getValue());
        assertEquals(10, ls.getPrefixMetric().getValue().intValue());
        assertEquals("10.25.2.27", ls.getOspfForwardingAddress().getIpv4Address().getValue());

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        this.parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        // there is unresolved TLV at the end, that needs to be cut off
        assertArrayEquals(P4_ATTR, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPositiveTELspAttribute() throws BGPParsingException {
        final AttributesBuilder builder = createBuilder(new TeLspCaseBuilder().build());
        this.parser.parseAttribute(Unpooled.copiedBuffer(TE_LSP_ATTR), builder);

        final Attributes1 attrs = builder.getAugmentation(Attributes1.class);
        final TeLspAttributes teLspAttributes = ((TeLspAttributesCase) attrs.getLinkStateAttribute()).getTeLspAttributes();
        assertNotNull(teLspAttributes);
        final TspecObject tSpec = teLspAttributes.getTspecObject();
        assertNotNull(tSpec);
        assertEquals(new Float32(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01}), tSpec.getTokenBucketRate());
        assertEquals(new Float32(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02}), teLspAttributes.getTspecObject().getTokenBucketSize());
        assertEquals(new Float32(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03}), tSpec.getPeakDataRate());
        assertEquals(new Long("4"), tSpec.getMinimumPolicedUnit());
        assertEquals(new Long("5"), tSpec.getMaximumPacketSize());

        final AssociationObject associationObject = teLspAttributes.getAssociationObject();
        assertEquals(AssociationType.Recovery, associationObject.getAssociationType());
        final IpAddress ipv4 = new IpAddress(Ipv4Util.addressForByteBuf(Unpooled.copiedBuffer(new byte[]{0x01, 0x02, 0x03, 0x04})));
        assertEquals(ipv4, associationObject.getIpAddress());
        final short associationId = 2;
        assertEquals(associationId, associationObject.getAssociationId().shortValue());

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        this.parser.serializeAttribute(builder.build(), buff);
        assertArrayEquals(TE_LSP_ATTR, ByteArray.getAllBytes(buff));
        assertTrue(Arrays.equals(TE_LSP_ATTR, ByteArray.getAllBytes(buff)));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testLinkAttributesPrivateConstructor() throws Throwable {
        final Constructor<LinkAttributesParser> c = LinkAttributesParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testNodeAttributesPrivateConstructor() throws Throwable {
        final Constructor<NodeAttributesParser> c = NodeAttributesParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPrefixAttributesPrivateConstructor() throws Throwable {
        final Constructor<PrefixAttributesParser> c = PrefixAttributesParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
