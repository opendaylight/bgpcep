/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathSegmentParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ExtendedCommunitiesAttributeParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.NoopReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCapability.RestartFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.AsSpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.AsSpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.OpaqueExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.OpaqueExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as.specific.extended.community._case.AsSpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.opaque.extended.community._case.OpaqueExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.extended.community._case.RouteTargetExtendedCommunityBuilder;

public class ComplementaryTest {

    @Test
    public void testBGPParameter() {

        final MultiprotocolCapability cap = new MultiprotocolCapabilityBuilder().setAfi(Ipv6AddressFamily.class).setSafi(
            UnicastSubsequentAddressFamily.class).build();
        final CParameters tlv1 = new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder()
            .setMultiprotocolCapability(cap).build()).build();
        final MultiprotocolCapability cap1 = new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.class).setSafi(
            UnicastSubsequentAddressFamily.class).build();
        final CParameters tlv2 = new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder()
            .setMultiprotocolCapability(cap1).build()).build();

        final List<Tables> tt = new ArrayList<>();
        tt.add(new TablesBuilder().setAfi(Ipv6AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build());
        tt.add(new TablesBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build());

        final GracefulRestartCapability tlv3 = new GracefulRestartCapabilityBuilder().setRestartFlags(new RestartFlags(Boolean.FALSE)).setRestartTime(0).setTables(tt).build();

        final CParameters tlv4 = new CParametersBuilder().setAs4BytesCapability(
            new As4BytesCapabilityBuilder().setAsNumber(new AsNumber((long) 40)).build()).build();

        assertFalse(tlv3.getRestartFlags().isRestartState());

        assertEquals(0, tlv3.getRestartTime().intValue());

        assertFalse(tlv1.equals(tlv2));

        assertNotSame(tlv1.hashCode(), tlv3.hashCode());

        assertNotSame(tlv2.toString(), tlv3.toString());

        assertEquals(tlv3.getTables(), tt);

        assertEquals(cap.getSafi(), cap1.getSafi());

        assertNotSame(cap.getAfi(), cap1.getAfi());

        assertEquals(40, tlv4.getAs4BytesCapability().getAsNumber().getValue().longValue());

        assertEquals(new CParametersBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber((long) 40)).build()).build(), tlv4);
    }

    @Test
    public void testBGPAggregatorImpl() {
        final BgpAggregator ipv4 = new AggregatorBuilder().setAsNumber(new AsNumber((long) 5524)).setNetworkAddress(
                new Ipv4Address("124.55.42.1")).build();
        final BgpAggregator ipv4i = new AggregatorBuilder().setAsNumber(new AsNumber((long) 5525)).setNetworkAddress(
                new Ipv4Address("124.55.42.1")).build();

        assertNotSame(ipv4.hashCode(), ipv4i.hashCode());

        assertNotSame(ipv4.getAsNumber(), ipv4i.getAsNumber());

        assertEquals(ipv4.getNetworkAddress(), ipv4i.getNetworkAddress());
    }

    private static final byte[] communitiesBytes = { 0, 5, 0, 54, 0, 0, 1, 76,
        40, 5, 0, 54, 0, 0, 1, 76,
        1, 2, 0, 35, 4, 2, 8, 7,
        0, 3, 0, 24, 4, 2, 8, 7,
        41, 6, 12, 51, 2, 5, 21, 45,
        3, 6, 21, 45, 5, 4, 3, 1,
        43, 6, 21, 45, 5, 4, 3, 1 };

    @Test
    public void testExtendedCommunitiesParser() {
        final ExtendedCommunitiesAttributeParser parser = new ExtendedCommunitiesAttributeParser(NoopReferenceCache.getInstance());
        final AttributesBuilder pa = new AttributesBuilder();
        try {
            parser.parseAttribute(Unpooled.copiedBuffer(communitiesBytes), pa);
        } catch (final BGPDocumentedException e1) {
            fail("Not expected exception: " + e1);
        }
        AsSpecificExtendedCommunityCase expected = new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
                new AsSpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(new ShortAsNumber(54L)).setLocalAdministrator(
                        new byte[] { 0, 0, 1, 76 }).build()).build();
        ExtendedCommunities ex = pa.getExtendedCommunities().get(0);
        AsSpecificExtendedCommunityCase result = (AsSpecificExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(expected.getAsSpecificExtendedCommunity().isTransitive(), result.getAsSpecificExtendedCommunity().isTransitive());
        assertEquals(expected.getAsSpecificExtendedCommunity().getGlobalAdministrator(),
                result.getAsSpecificExtendedCommunity().getGlobalAdministrator());
        assertArrayEquals(expected.getAsSpecificExtendedCommunity().getLocalAdministrator(),
                result.getAsSpecificExtendedCommunity().getLocalAdministrator());
        assertEquals(0, ex.getCommType().intValue());

        expected = new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
                new AsSpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(new ShortAsNumber(54L)).setLocalAdministrator(
                        new byte[] { 0, 0, 1, 76 }).build()).build();
        ex = pa.getExtendedCommunities().get(1);
        result = (AsSpecificExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(expected.getAsSpecificExtendedCommunity().isTransitive(), result.getAsSpecificExtendedCommunity().isTransitive());
        assertEquals(40, ex.getCommType().intValue());

        final RouteTargetExtendedCommunityCase rexpected = new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
                new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(35L)).setLocalAdministrator(
                        new byte[] { 4, 2, 8, 7 }).build()).build();
        ex = pa.getExtendedCommunities().get(2);
        final RouteTargetExtendedCommunityCase rresult = (RouteTargetExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(rexpected.getRouteTargetExtendedCommunity().getGlobalAdministrator(),
                rresult.getRouteTargetExtendedCommunity().getGlobalAdministrator());
        assertArrayEquals(rexpected.getRouteTargetExtendedCommunity().getLocalAdministrator(),
                rresult.getRouteTargetExtendedCommunity().getLocalAdministrator());
        assertEquals(1, ex.getCommType().intValue());
        assertEquals(2, ex.getCommSubType().intValue());

        final RouteOriginExtendedCommunityCase oexpected = new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
                new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(24L)).setLocalAdministrator(
                        new byte[] { 4, 2, 8, 7 }).build()).build();
        ex = pa.getExtendedCommunities().get(3);
        final RouteOriginExtendedCommunityCase oresult = (RouteOriginExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(oexpected.getRouteOriginExtendedCommunity().getGlobalAdministrator(),
                oresult.getRouteOriginExtendedCommunity().getGlobalAdministrator());
        assertArrayEquals(oexpected.getRouteOriginExtendedCommunity().getLocalAdministrator(),
                oresult.getRouteOriginExtendedCommunity().getLocalAdministrator());
        assertEquals(0, ex.getCommType().intValue());
        assertEquals(3, ex.getCommSubType().intValue());

        final Inet4SpecificExtendedCommunityCase iexpected = new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                new Inet4SpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(new Ipv4Address("12.51.2.5")).setLocalAdministrator(
                        new byte[] { 21, 45 }).build()).build();
        ex = pa.getExtendedCommunities().get(4);
        final Inet4SpecificExtendedCommunityCase iresult = (Inet4SpecificExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(iexpected.getInet4SpecificExtendedCommunity().isTransitive(),
                iresult.getInet4SpecificExtendedCommunity().isTransitive());
        assertEquals(iexpected.getInet4SpecificExtendedCommunity().getGlobalAdministrator(),
                iresult.getInet4SpecificExtendedCommunity().getGlobalAdministrator());
        assertArrayEquals(iexpected.getInet4SpecificExtendedCommunity().getLocalAdministrator(),
                iresult.getInet4SpecificExtendedCommunity().getLocalAdministrator());
        assertEquals(41, ex.getCommType().intValue());

        final OpaqueExtendedCommunityCase oeexpected = new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(
                new OpaqueExtendedCommunityBuilder().setTransitive(false).setValue(new byte[] { 21, 45, 5, 4, 3, 1 }).build()).build();
        ex = pa.getExtendedCommunities().get(5);
        final OpaqueExtendedCommunityCase oeresult = (OpaqueExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(oeexpected.getOpaqueExtendedCommunity().isTransitive(), oeresult.getOpaqueExtendedCommunity().isTransitive());
        assertArrayEquals(oeexpected.getOpaqueExtendedCommunity().getValue(), oeresult.getOpaqueExtendedCommunity().getValue());
        assertEquals(3, ex.getCommType().intValue());

        final OpaqueExtendedCommunityCase oeexpected1 = new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(
                new OpaqueExtendedCommunityBuilder().setTransitive(true).setValue(new byte[] { 21, 45, 5, 4, 3, 1 }).build()).build();
        ex = pa.getExtendedCommunities().get(6);
        final OpaqueExtendedCommunityCase oeresult1 = (OpaqueExtendedCommunityCase) ex.getExtendedCommunity();
        assertEquals(oeexpected1.getOpaqueExtendedCommunity().isTransitive(), oeresult1.getOpaqueExtendedCommunity().isTransitive());
        assertArrayEquals(oeexpected1.getOpaqueExtendedCommunity().getValue(), oeresult1.getOpaqueExtendedCommunity().getValue());
        assertEquals(43, ex.getCommType().intValue());

        final ByteBuf serializedBuffer = Unpooled.buffer();
        parser.serializeAttribute(pa.build(), serializedBuffer);
        assertArrayEquals(new byte[]{ (byte)192, 16, 56, 0, 5, 0, 54, 0, 0, 1, 76,
            40, 5, 0, 54, 0, 0, 1, 76,
            1, 2, 0, 35, 4, 2, 8, 7,
            0, 3, 0, 24, 4, 2, 8, 7,
            41, 6, 12, 51, 2, 5, 21, 45,
            3, 6, 21, 45, 5, 4, 3, 1,
            43, 6, 21, 45, 5, 4, 3, 1 }, ByteArray.readAllBytes(serializedBuffer));

        try {
            parser.parseAttribute(Unpooled.copiedBuffer(new byte[] { 11, 11, 21, 45, 5, 4, 3, 1 }), pa);
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Could not parse Extended Community type: 11", e.getMessage());
        }
    }


    @Test(expected=UnsupportedOperationException.class)
    public void testAsPathSegmentParserPrivateConstructor() throws Throwable {
        final Constructor<AsPathSegmentParser> c = AsPathSegmentParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
