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

import io.netty.buffer.Unpooled;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkProtectionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;

public class LinkstateAttributeParserTest {

    private static final byte[] LINK_ATTR = new byte[] { (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0x2a, (byte) 0x2a,
        (byte) 0x2a, (byte) 0x2a, (byte) 0x04, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x2b, (byte) 0x2b, (byte) 0x2b, (byte) 0x2b,
        (byte) 0x04, (byte) 0x40, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x41,
        (byte) 0x00, (byte) 0x04, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x04, (byte) 0x42, (byte) 0x00, (byte) 0x04,
        (byte) 0x46, (byte) 0x43, (byte) 0x50, (byte) 0x00, (byte) 0x04, (byte) 0x43, (byte) 0x00, (byte) 0x20, (byte) 0x46, (byte) 0x43,
        (byte) 0x50, (byte) 0x00, (byte) 0x46, (byte) 0x43, (byte) 0x50, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x04, (byte) 0x44, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x45, (byte) 0x00,
        (byte) 0x01, (byte) 0x08, (byte) 0x04, (byte) 0x46, (byte) 0x00, (byte) 0x01, (byte) 0xc0, (byte) 0x04, (byte) 0x47, (byte) 0x00,
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x04, (byte) 0x48, (byte) 0x00, (byte) 0x08, (byte) 0x12, (byte) 0x34,
        (byte) 0x56, (byte) 0x78, (byte) 0x10, (byte) 0x30, (byte) 0x50, (byte) 0x70, (byte) 0x04, (byte) 0x4a, (byte) 0x00, (byte) 0x05,
        (byte) 0x31, (byte) 0x32, (byte) 0x4b, (byte) 0x2d, (byte) 0x32,

        (byte) 0x04, (byte) 0x88, (byte) 0x00, (byte) 0x01, (byte) 0x0a };

    private static final byte[] NODE_ATTR = new byte[] { (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x2a,
        (byte) 0x00, (byte) 0x2b, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xb0, (byte) 0x04, (byte) 0x02, (byte) 0x00,
        (byte) 0x05, (byte) 0x31, (byte) 0x32, (byte) 0x4b, (byte) 0x2d, (byte) 0x32, (byte) 0x04, (byte) 0x03, (byte) 0x00, (byte) 0x01,
        (byte) 0x72, (byte) 0x04, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x73, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x04,
        (byte) 0x29, (byte) 0x29, (byte) 0x29, (byte) 0x29, (byte) 0x04, (byte) 0x88, (byte) 0x00, (byte) 0x01, (byte) 0x0a };

    private static final byte[] P4_ATTR = new byte[] { (byte) 0x04, (byte) 0x80, (byte) 0x00, (byte) 0x01, (byte) 0xff, (byte) 0x04,
        (byte) 0x81, (byte) 0x00, (byte) 0x08, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x10, (byte) 0x30, (byte) 0x50,
        (byte) 0x70, (byte) 0x04, (byte) 0x82, (byte) 0x00, (byte) 0x08, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x10,
        (byte) 0x30, (byte) 0x50, (byte) 0x70, (byte) 0x04, (byte) 0x83, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x0a, (byte) 0x04, (byte) 0x84, (byte) 0x00, (byte) 0x04, (byte) 0x0a, (byte) 0x19, (byte) 0x02, (byte) 0x1b, (byte) 0x04,
        (byte) 0x85, (byte) 0x00, (byte) 0x01, (byte) 0x0a, (byte) 0x04, (byte) 0x88, (byte) 0x00, (byte) 0x01, (byte) 0x0a };

    private final LinkstateAttributeParser parser = new LinkstateAttributeParser();

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

    @Test
    public void testPositiveLinks() throws BGPParsingException {
        final PathAttributesBuilder builder = createBuilder(NlriType.Link);
        this.parser.parseAttribute(Unpooled.copiedBuffer(LINK_ATTR), builder);
        final PathAttributes1 attrs = builder.getAugmentation(PathAttributes1.class);
        final LinkAttributes ls = ((LinkAttributesCase) attrs.getLinkstatePathAttribute().getLinkStateAttribute()).getLinkAttributes();
        assertNotNull(ls);

        assertEquals("42.42.42.42", ls.getLocalIpv4RouterId().getValue());
        assertEquals(new Long(10), ls.getMetric().getValue());
        assertEquals(new Long(0), ls.getAdminGroup().getValue());
        assertEquals("43.43.43.43", ls.getRemoteIpv4RouterId().getValue());
        assertArrayEquals(new byte[] { (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80 }, ls.getMaxLinkBandwidth().getValue());
        assertArrayEquals(new byte[] { (byte) 0x46, (byte) 0x43, (byte) 0x50, (byte) 0x00 }, ls.getMaxReservableBandwidth().getValue());
        assertNotNull(ls.getUnreservedBandwidth());
        assertEquals(8, ls.getUnreservedBandwidth().size());
        assertEquals(LinkProtectionType.Dedicated1to1, ls.getLinkProtection());
        assertTrue(ls.getMplsProtocol().isLdp());
        assertTrue(ls.getMplsProtocol().isRsvpte());
        assertEquals(2, ls.getSharedRiskLinkGroups().size());
        assertEquals(305419896, ls.getSharedRiskLinkGroups().get(0).getValue().intValue());
        assertEquals("12K-2", ls.getLinkName());
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
    }

    @Test
    public void testPositiveV4Prefixes() throws BGPParsingException {
        final PathAttributesBuilder builder = createBuilder(NlriType.Ipv4Prefix);
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
    }
}
