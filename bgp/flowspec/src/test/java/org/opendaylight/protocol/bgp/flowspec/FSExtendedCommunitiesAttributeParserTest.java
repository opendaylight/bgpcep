/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.NoopReferenceCache;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.redirect.extended.community.RedirectExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.action.extended.community.TrafficActionExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.marking.extended.community.TrafficMarkingExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.rate.extended.community.TrafficRateExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;

public class FSExtendedCommunitiesAttributeParserTest {

    private final ReferenceCache ref = NoopReferenceCache.getInstance();

    private static final byte[] communitiesBytes = { (byte)128, 6, 0, 72, 0, 1, 2, 3,
        (byte)128, 7, 0, 0, 0, 0, 0, 3,
        (byte)128, 8, 0, 35, 4, 2, 8, 7,
        (byte)128, 9, 0, 0, 0, 0, 0, 63 };

    @Test
    public void testExtendedCommunitiesParser() {
        final FSExtendedCommunitiesAttributeParser parser = new FSExtendedCommunitiesAttributeParser(this.ref);
        final AttributesBuilder pa = new AttributesBuilder();
        try {
            parser.parseAttribute(Unpooled.copiedBuffer(communitiesBytes), pa);
        } catch (final BGPDocumentedException e1) {
            fail("Not expected exception: " + e1);
        }

        final TrafficRateExtendedCommunityCase expected = new TrafficRateExtendedCommunityCaseBuilder().setTrafficRateExtendedCommunity(
                new TrafficRateExtendedCommunityBuilder().setInformativeAs(new ShortAsNumber(72L))
                    .setLocalAdministrator(new Bandwidth(new byte[] { 0, 1, 2, 3 })).build()).build();
        ExtendedCommunities ex = pa.getExtendedCommunities().get(0);
        final TrafficRateExtendedCommunityCase result = (TrafficRateExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(expected.getTrafficRateExtendedCommunity().getInformativeAs(), result.getTrafficRateExtendedCommunity().getInformativeAs());
        assertArrayEquals(expected.getTrafficRateExtendedCommunity().getLocalAdministrator().getValue(),
                result.getTrafficRateExtendedCommunity().getLocalAdministrator().getValue());

        final TrafficActionExtendedCommunityCase expected1 = new TrafficActionExtendedCommunityCaseBuilder().setTrafficActionExtendedCommunity(
            new TrafficActionExtendedCommunityBuilder().setSample(true).setTerminalAction(true).build()).build();
        ex = pa.getExtendedCommunities().get(1);
        final TrafficActionExtendedCommunityCase result1 = (TrafficActionExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(expected1.getTrafficActionExtendedCommunity().isSample(), result1.getTrafficActionExtendedCommunity().isSample());
        assertEquals(expected1.getTrafficActionExtendedCommunity().isTerminalAction(), result1.getTrafficActionExtendedCommunity().isTerminalAction());

        final RedirectExtendedCommunityCase expected2 = new RedirectExtendedCommunityCaseBuilder().setRedirectExtendedCommunity(
                new RedirectExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(35L)).setLocalAdministrator(
                        new byte[] { 4, 2, 8, 7 }).build()).build();
        ex = pa.getExtendedCommunities().get(2);
        final RedirectExtendedCommunityCase result2 = (RedirectExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(expected2.getRedirectExtendedCommunity().getGlobalAdministrator(),
                result2.getRedirectExtendedCommunity().getGlobalAdministrator());
        assertArrayEquals(expected2.getRedirectExtendedCommunity().getLocalAdministrator(),
                result2.getRedirectExtendedCommunity().getLocalAdministrator());

        final TrafficMarkingExtendedCommunityCase expected3 = new TrafficMarkingExtendedCommunityCaseBuilder().setTrafficMarkingExtendedCommunity(
                new TrafficMarkingExtendedCommunityBuilder().setGlobalAdministrator(new Dscp((short) 63)).build()).build();
        ex = pa.getExtendedCommunities().get(3);
        final TrafficMarkingExtendedCommunityCase result3 = (TrafficMarkingExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(expected3.getTrafficMarkingExtendedCommunity().getGlobalAdministrator(),
                result3.getTrafficMarkingExtendedCommunity().getGlobalAdministrator());

        final ByteBuf serializedBuffer = Unpooled.buffer();
        parser.serializeAttribute(pa.build(), serializedBuffer);
        assertArrayEquals(new byte[]{ (byte)192, 16, 32, (byte)128, 6, 0, 72, 0, 1, 2, 3,
            (byte)128, 7, 0, 0, 0, 0, 0, 3,
            (byte)128, 8, 0, 35, 4, 2, 8, 7,
            (byte)128, 9, 0, 0, 0, 0, 0, 63 }, ByteArray.readAllBytes(serializedBuffer));

        try {
            parser.parseAttribute(Unpooled.copiedBuffer(new byte[] { 11, 11, 21, 45, 5, 4, 3, 1 }), pa);
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Could not parse Extended Community type: 11", e.getMessage());
        }
    }
}
