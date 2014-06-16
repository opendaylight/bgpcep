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

import com.google.common.collect.Maps;

import io.netty.buffer.Unpooled;

import java.util.Map;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunitiesParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.NoopReferenceCache;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.as4.bytes._case.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.MultiprotocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.multiprotocol._case.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.multiprotocol._case.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
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

    private final ReferenceCache ref = NoopReferenceCache.getInstance();

    @Test
    public void testBGPParameter() {

        final BgpTableType t = new BgpTableTypeImpl(LinkstateAddressFamily.class, UnicastSubsequentAddressFamily.class);
        final BgpTableType t1 = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

        final MultiprotocolCapability cap = new MultiprotocolCapabilityBuilder().setAfi(LinkstateAddressFamily.class).setSafi(
                UnicastSubsequentAddressFamily.class).build();
        final CParameters tlv1 = new MultiprotocolCaseBuilder().setMultiprotocolCapability(cap).build();

        final MultiprotocolCapability cap1 = new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.class).setSafi(
                UnicastSubsequentAddressFamily.class).build();
        final CParameters tlv2 = new MultiprotocolCaseBuilder().setMultiprotocolCapability(cap1).build();

        final Map<BgpTableType, Boolean> tt = Maps.newHashMap();
        tt.put(t, true);
        tt.put(t1, false);

        // FIXME: BUG-196: revive test for graceful capability tlv
        // final BGPParameter tlv3 = new GracefulCapability(false, 0, tt);

        final CParameters tlv4 = new As4BytesCaseBuilder().setAs4BytesCapability(
                new As4BytesCapabilityBuilder().setAsNumber(new AsNumber((long) 40)).build()).build();

        // assertFalse(((GracefulCapability) tlv3).isRestartFlag());

        // assertEquals(0, ((GracefulCapability) tlv3).getRestartTimerValue());

        assertFalse(tlv1.equals(tlv2));

        // assertNotSame(tlv1.hashCode(), tlv3.hashCode());

        // assertNotSame(tlv2.toString(), tlv3.toString());

        // assertEquals(((GracefulCapability) tlv3).getTableTypes(), tt);

        assertEquals(cap.getSafi(), cap1.getSafi());

        assertNotSame(cap.getAfi(), cap1.getAfi());

        assertEquals(40, ((As4BytesCase) tlv4).getAs4BytesCapability().getAsNumber().getValue().longValue());

        assertEquals(new As4BytesCaseBuilder().setAs4BytesCapability(
                new As4BytesCapabilityBuilder().setAsNumber(new AsNumber((long) 40)).build()).build(), tlv4);
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

    @Test
    public void testCommunitiesParser() {
        ExtendedCommunities as = null;
        try {
            as = CommunitiesParser.parseExtendedCommunity(this.ref, Unpooled.copiedBuffer(new byte[] { 0, 5, 0, 54, 0, 0, 1, 76 }));
        } catch (final BGPDocumentedException e1) {
            fail("Not expected exception: " + e1);
        }
        AsSpecificExtendedCommunityCase expected = new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
                new AsSpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(new ShortAsNumber(54L)).setLocalAdministrator(
                        new byte[] { 0, 0, 1, 76 }).build()).build();
        AsSpecificExtendedCommunityCase result = (AsSpecificExtendedCommunityCase) as.getExtendedCommunity();
        assertEquals(expected.getAsSpecificExtendedCommunity().isTransitive(), result.getAsSpecificExtendedCommunity().isTransitive());
        assertEquals(expected.getAsSpecificExtendedCommunity().getGlobalAdministrator(),
                result.getAsSpecificExtendedCommunity().getGlobalAdministrator());
        assertArrayEquals(expected.getAsSpecificExtendedCommunity().getLocalAdministrator(),
                result.getAsSpecificExtendedCommunity().getLocalAdministrator());
        assertEquals(0, as.getCommType().intValue());

        try {
            as = CommunitiesParser.parseExtendedCommunity(this.ref, Unpooled.copiedBuffer(new byte[] { 40, 5, 0, 54, 0, 0, 1, 76 }));
        } catch (final BGPDocumentedException e1) {
            fail("Not expected exception: " + e1);
        }
        expected = new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
                new AsSpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(new ShortAsNumber(54L)).setLocalAdministrator(
                        new byte[] { 0, 0, 1, 76 }).build()).build();
        result = (AsSpecificExtendedCommunityCase) as.getExtendedCommunity();
        assertEquals(expected.getAsSpecificExtendedCommunity().isTransitive(), result.getAsSpecificExtendedCommunity().isTransitive());
        assertEquals(40, as.getCommType().intValue());

        ExtendedCommunities rtc = null;
        try {
            rtc = CommunitiesParser.parseExtendedCommunity(this.ref, Unpooled.copiedBuffer(new byte[] { 1, 2, 0, 35, 4, 2, 8, 7 }));
        } catch (final BGPDocumentedException e1) {
            fail("Not expected exception: " + e1);
        }
        final RouteTargetExtendedCommunityCase rexpected = new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
                new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(35L)).setLocalAdministrator(
                        new byte[] { 4, 2, 8, 7 }).build()).build();
        final RouteTargetExtendedCommunityCase rresult = (RouteTargetExtendedCommunityCase) rtc.getExtendedCommunity();
        assertEquals(rexpected.getRouteTargetExtendedCommunity().getGlobalAdministrator(),
                rresult.getRouteTargetExtendedCommunity().getGlobalAdministrator());
        assertArrayEquals(rexpected.getRouteTargetExtendedCommunity().getLocalAdministrator(),
                rresult.getRouteTargetExtendedCommunity().getLocalAdministrator());
        assertEquals(1, rtc.getCommType().intValue());
        assertEquals(2, rtc.getCommSubType().intValue());

        ExtendedCommunities roc = null;
        try {
            roc = CommunitiesParser.parseExtendedCommunity(this.ref, Unpooled.copiedBuffer(new byte[] { 0, 3, 0, 24, 4, 2, 8, 7 }));
        } catch (final BGPDocumentedException e1) {
            fail("Not expected exception: " + e1);
        }
        final RouteOriginExtendedCommunityCase oexpected = new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
                new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(24L)).setLocalAdministrator(
                        new byte[] { 4, 2, 8, 7 }).build()).build();
        final RouteOriginExtendedCommunityCase oresult = (RouteOriginExtendedCommunityCase) roc.getExtendedCommunity();
        assertEquals(oexpected.getRouteOriginExtendedCommunity().getGlobalAdministrator(),
                oresult.getRouteOriginExtendedCommunity().getGlobalAdministrator());
        assertArrayEquals(oexpected.getRouteOriginExtendedCommunity().getLocalAdministrator(),
                oresult.getRouteOriginExtendedCommunity().getLocalAdministrator());
        assertEquals(0, roc.getCommType().intValue());
        assertEquals(3, roc.getCommSubType().intValue());

        ExtendedCommunities sec = null;
        try {
            sec = CommunitiesParser.parseExtendedCommunity(this.ref, Unpooled.copiedBuffer(new byte[] { 41, 6, 12, 51, 2, 5, 21, 45 }));
        } catch (final BGPDocumentedException e1) {
            fail("Not expected exception: " + e1);
        }
        final Inet4SpecificExtendedCommunityCase iexpected = new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                new Inet4SpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(new Ipv4Address("12.51.2.5")).setLocalAdministrator(
                        new byte[] { 21, 45 }).build()).build();
        final Inet4SpecificExtendedCommunityCase iresult = (Inet4SpecificExtendedCommunityCase) sec.getExtendedCommunity();
        assertEquals(iexpected.getInet4SpecificExtendedCommunity().isTransitive(),
                iresult.getInet4SpecificExtendedCommunity().isTransitive());
        assertEquals(iexpected.getInet4SpecificExtendedCommunity().getGlobalAdministrator(),
                iresult.getInet4SpecificExtendedCommunity().getGlobalAdministrator());
        assertArrayEquals(iexpected.getInet4SpecificExtendedCommunity().getLocalAdministrator(),
                iresult.getInet4SpecificExtendedCommunity().getLocalAdministrator());
        assertEquals(41, sec.getCommType().intValue());

        ExtendedCommunities oec = null;
        try {
            oec = CommunitiesParser.parseExtendedCommunity(this.ref, Unpooled.copiedBuffer(new byte[] { 3, 6, 21, 45, 5, 4, 3, 1 }));
        } catch (final BGPDocumentedException e1) {
            fail("Not expected exception: " + e1);
        }
        final OpaqueExtendedCommunityCase oeexpected = new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(
                new OpaqueExtendedCommunityBuilder().setTransitive(false).setValue(new byte[] { 21, 45, 5, 4, 3, 1 }).build()).build();
        final OpaqueExtendedCommunityCase oeresult = (OpaqueExtendedCommunityCase) oec.getExtendedCommunity();
        assertEquals(oeexpected.getOpaqueExtendedCommunity().isTransitive(), oeresult.getOpaqueExtendedCommunity().isTransitive());
        assertArrayEquals(oeexpected.getOpaqueExtendedCommunity().getValue(), oeresult.getOpaqueExtendedCommunity().getValue());
        assertEquals(3, oec.getCommType().intValue());

        try {
            oec = CommunitiesParser.parseExtendedCommunity(this.ref, Unpooled.copiedBuffer(new byte[] { 43, 6, 21, 45, 5, 4, 3, 1 }));
        } catch (final BGPDocumentedException e1) {
            fail("Not expected exception: " + e1);
        }
        final OpaqueExtendedCommunityCase oeexpected1 = new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(
                new OpaqueExtendedCommunityBuilder().setTransitive(true).setValue(new byte[] { 21, 45, 5, 4, 3, 1 }).build()).build();
        final OpaqueExtendedCommunityCase oeresult1 = (OpaqueExtendedCommunityCase) oec.getExtendedCommunity();
        assertEquals(oeexpected1.getOpaqueExtendedCommunity().isTransitive(), oeresult1.getOpaqueExtendedCommunity().isTransitive());
        assertArrayEquals(oeexpected1.getOpaqueExtendedCommunity().getValue(), oeresult1.getOpaqueExtendedCommunity().getValue());
        assertEquals(43, oec.getCommType().intValue());

        try {
            CommunitiesParser.parseExtendedCommunity(this.ref, Unpooled.copiedBuffer(new byte[] { 11, 11, 21, 45, 5, 4, 3, 1 }));
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Could not parse Extended Community type: 11", e.getMessage());
        }
    }

    @Test
    public void testBGPHeaderParser() throws Exception {
        final MessageRegistry msgReg = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry();
        try {
            msgReg.parseMessage(Unpooled.copiedBuffer(new byte[] { (byte) 0, (byte) 0 }));
            fail("Exception should have occured.");
        } catch (final IllegalArgumentException e) {
            assertEquals("Too few bytes in passed array. Passed: 2. Expected: >= 19.", e.getMessage());
        }
    }

    @Test
    public void testMessageParser() throws Exception {
        final MessageRegistry msgReg = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry();
        String ex = "";
        try {
            msgReg.serializeMessage(null, Unpooled.EMPTY_BUFFER);
        } catch (final NullPointerException e) {
            ex = e.getMessage();
        }
        assertEquals("BGPMessage is mandatory.", ex);
    }
}
