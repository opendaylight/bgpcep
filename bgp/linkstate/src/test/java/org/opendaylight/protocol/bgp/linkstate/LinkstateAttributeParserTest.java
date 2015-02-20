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
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.attribute.LinkAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.NodeAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.PrefixAttributesParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;

public class LinkstateAttributeParserTest {

    private static final byte[] LINK_ATTR =  { 0x04, 0x04, 0, 0x04, 0x2a, 0x2a, 0x2a, 0x2a, 0x04, 0x06, 0, 0x04, 0x2b, 0x2b, 0x2b, 0x2b,
        0x04, 0x40, 0, 0x04, 0, 0, 0, 0, 0x04, 0x41, 0, 0x04, 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, 0x04, 0x42, 0, 0x04,
        0x46, 0x43, 0x50, 0, 0x04, 0x43, 0, 0x20, 0x46, 0x43, 0x50, 0, 0x46, 0x43, 0x50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x04, 0x44, 0, 0x08, 0, 0, 0, 0, 0, 0, 0, 0, 0x04, 0x45, 0, 0x02, 0, 0x08, 0x04, 0x46, 0, 0x01,
        (byte) 0xc0, 0x04, 0x47, 0, 0x03, 0, 0, 0x0a, 0x04, 0x48, 0, 0x08, 0x12, 0x34, 0x56, 0x78, 0x10, 0x30, 0x50, 0x70, 0x04, 0x4a,
        0, 0x05, 0x31, 0x32, 0x4b, 0x2d, 0x32, 0x04, (byte) 0x88, 0, 0x01, 0x0a };

    private static final byte[] NODE_ATTR = { 0x01, 0x07, 0, 0x04, 0, 0x2a, 0, 0x2b, 0x04, 0, 0, 0x01, (byte) 0xb0, 0x04, 0x02, 0,
        0x05, 0x31, 0x32, 0x4b, 0x2d, 0x32, 0x04, 0x03, 0, 0x01, 0x72, 0x04, 0x03, 0, 0x01, 0x73, 0x04, 0x04, 0, 0x04,
        0x29, 0x29, 0x29, 0x29, 0x04, (byte) 0x88, 0, 0x01, 0x0a };

    private static final byte[] NODE_ATTR_S = { 0x01, 0x07, 0, 0x04, 0, 0x2a, 0, 0x2b, 0x04, 0, 0, 0x01, (byte) 0xb0, 0x04, 0x02, 0,
        0x05, 0x31, 0x32, 0x4b, 0x2d, 0x32, 0x04, 0x03, 0, 0x01, 0x73, 0x04, 0x03, 0, 0x01, 0x72, 0x04, 0x04, 0, 0x04,
        0x29, 0x29, 0x29, 0x29};

    private static final byte[] P4_ATTR = { 0x04, (byte) 0x80, 0, 0x01, (byte) 0x80, 0x04, (byte) 0x81, 0, 0x08, 0x12, 0x34, 0x56, 0x78,
        0x10, 0x30, 0x50, 0x70, 0x04, (byte) 0x82, 0, 0x08, 0x12, 0x34, 0x56, 0x78, 0x10, 0x30, 0x50, 0x70, 0x04, (byte) 0x84, 0, 0x04, 0x0a, 0x19, 0x02, 0x1b,
        0x04, (byte) 0x83, 0, 0x04, 0, 0, 0, 0x0a, 0x04, (byte) 0x88, 0, 0x01, 0x0a };

    private final LinkstateAttributeParser parser = new LinkstateAttributeParser(false);

    private static PathAttributesBuilder createBuilder(final NlriType type) {
        return new PathAttributesBuilder().addAugmentation(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1.class,
            new PathAttributes1Builder().setMpReachNlri(
                new MpReachNlriBuilder().setAfi(LinkstateAddressFamily.class).setSafi(LinkstateSubsequentAddressFamily.class).setAdvertizedRoutes(
                    new AdvertizedRoutesBuilder().setDestinationType(
                        new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                            new DestinationLinkstateBuilder().setCLinkstateDestination(
                                Lists.newArrayList(new CLinkstateDestinationBuilder().setNlriType(type).build())).build()).build()).build()).build()).build());
    }

    private static PathAttributesBuilder createUnreachBuilder(final NlriType type) {
        return new PathAttributesBuilder().addAugmentation(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2.class,
            new PathAttributes2Builder().setMpUnreachNlri(
                new MpUnreachNlriBuilder().setAfi(LinkstateAddressFamily.class).setSafi(LinkstateSubsequentAddressFamily.class).setWithdrawnRoutes(
                    new WithdrawnRoutesBuilder().setDestinationType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder().setCLinkstateDestination(
                                Lists.newArrayList(new CLinkstateDestinationBuilder().setNlriType(type).build())).build()).build()).build()).build()).build());
    }

    @Test
    public void testGetNlriType() throws BGPParsingException {
        final ByteBuf b = Unpooled.buffer();
        PathAttributesBuilder builder = new PathAttributesBuilder();
        this.parser.parseAttribute(b, builder);
        assertEquals(0, b.readableBytes());
        builder = new PathAttributesBuilder();

        final PathAttributes1Builder builder1 = new PathAttributes1Builder();
        builder.addAugmentation(PathAttributes1.class, builder1.build());
        this.parser.parseAttribute(b, builder);
        assertEquals(0, b.readableBytes());
        builder = new PathAttributesBuilder();

        builder.addAugmentation(PathAttributes1.class, builder1.setMpReachNlri(
            new MpReachNlriBuilder().setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationIpv4CaseBuilder().build()).build()).build()).build());
        this.parser.parseAttribute(b, builder);
        assertEquals(0, b.readableBytes());
        builder = new PathAttributesBuilder();

        final PathAttributes2Builder builder2 = new PathAttributes2Builder();
        builder.addAugmentation(PathAttributes2.class, builder2.build());
        this.parser.parseAttribute(b, builder);
        assertEquals(0, b.readableBytes());
        builder = new PathAttributesBuilder();

        builder.addAugmentation(PathAttributes2.class, builder2.setMpUnreachNlri(
            new MpUnreachNlriBuilder().setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(new DestinationIpv6CaseBuilder().build()).build()).build()).build());
        this.parser.parseAttribute(b, builder);
        assertEquals(0, b.readableBytes());
    }

    @Test
    public void testPositiveLinks() throws BGPParsingException {
        final PathAttributesBuilder builder = createBuilder(NlriType.Link);
        this.parser.parseAttribute(Unpooled.copiedBuffer(LINK_ATTR), builder);
        final PathAttributes1 attrs = builder.getAugmentation(PathAttributes1.class);
        final LinkAttributes ls = ((LinkAttributesCase) attrs.getLinkstatePathAttribute().getLinkStateAttribute()).getLinkAttributes();
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

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        this.parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        // there is unresolved TLV at the end, that needs to be cut off
        assertArrayEquals(ByteArray.subByte(LINK_ATTR, 0, LINK_ATTR.length -5), ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPositiveNodes() throws BGPParsingException {
        final PathAttributesBuilder builder = createBuilder(NlriType.Node);
        this.parser.parseAttribute(Unpooled.copiedBuffer(NODE_ATTR), builder);

        final PathAttributes1 attrs = builder.getAugmentation(PathAttributes1.class);
        final NodeAttributes ls = ((NodeAttributesCase) attrs.getLinkstatePathAttribute().getLinkStateAttribute()).getNodeAttributes();
        assertNotNull(ls);

        assertEquals(2, ls.getTopologyIdentifier().size());
        assertEquals(42, ls.getTopologyIdentifier().get(0).getValue().intValue());
        assertTrue(ls.getNodeFlags().isOverload());
        assertFalse(ls.getNodeFlags().isAttached());
        assertTrue(ls.getNodeFlags().isExternal());
        assertTrue(ls.getNodeFlags().isAbr());

        assertEquals("12K-2", ls.getDynamicHostname());
        assertEquals(2, ls.getIsisAreaId().size());
        assertEquals("41.41.41.41", ls.getIpv4RouterId().getValue());

        //serialization
        final ByteBuf buff = Unpooled.buffer();
        this.parser.serializeAttribute(builder.build(), buff);
        buff.skipBytes(3);
        // there is unresolved TLV at the end, that needs to be cut off
        assertTrue(Arrays.equals(ByteArray.subByte(NODE_ATTR, 0, NODE_ATTR.length -5), ByteArray.getAllBytes(buff)) || Arrays.equals(NODE_ATTR_S, ByteArray.getAllBytes(buff)));
    }

    @Test
    public void testPositiveV4Prefixes() throws BGPParsingException {
        final PathAttributesBuilder builder = createUnreachBuilder(NlriType.Ipv4Prefix);
        this.parser.parseAttribute(Unpooled.copiedBuffer(P4_ATTR), builder);

        final PathAttributes1 attrs = builder.getAugmentation(PathAttributes1.class);
        final PrefixAttributes ls = ((PrefixAttributesCase) attrs.getLinkstatePathAttribute().getLinkStateAttribute()).getPrefixAttributes();
        assertNotNull(ls);

        assertTrue(ls.getIgpBits().getUpDown().isUpDown());
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
        assertArrayEquals(ByteArray.subByte(P4_ATTR, 0, P4_ATTR.length -5), ByteArray.getAllBytes(buff));
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
