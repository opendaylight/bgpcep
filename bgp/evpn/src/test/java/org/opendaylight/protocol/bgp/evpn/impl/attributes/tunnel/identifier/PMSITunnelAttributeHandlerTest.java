/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.BIDIR_PIM_EXPECTED;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.INGRESS_REPLICATION_EXPECTED;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.M_LDP_MP_2_MP_LSP_EXPECTED;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.M_LDP_MP_2_MP_LSP_WRONG;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.M_LDP_P2MP_LSP_EXPECTED_IPV4;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.M_LDP_P2MP_LSP_EXPECTED_IPV4_2;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.M_LDP_P2MP_LSP_EXPECTED_IPV6;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.M_LDP_P2MP_LSP_EXPECTED_L2VPN;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.M_LDP_P2MP_LSP_EXPECTED_WRONG_FAMILY;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.MockTunnelIdentifier;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.NO_TUNNEL_INFORMATION_PRESENT_EXPECTED;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.PIM_SM_TREE_EXPECTED;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.PIM_SSM_TREE_EXPECTED;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.RSVP_TE_P2MP_LSP_LSP_EXPECTED;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildBidirPimTreeAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildIngressReplicationAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildMLDpMp2mPLspAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildMldpMP2mpLspWrongAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildMldpP2mpLspIpv4Attribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildMldpP2mpLspIpv6Attribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildMldpp2MPLspL2vpnAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildNoSupportedFamilyAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildNoSupportedOpaqueAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildPimSMTreeAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildPimSSMTreeAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildRsvpTep2MPLspAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.buildWOTunnelInfAttribute;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.TunnelIdentifierHandler.NO_TUNNEL_INFORMATION_PRESENT;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.BGPActivator;
import org.opendaylight.protocol.bgp.evpn.impl.attributes.PMSITunnelAttributeHandler;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;

public class PMSITunnelAttributeHandlerTest {

    private AttributeRegistry handler;

    @Before
    public void setUp() {
        final BGPExtensionProviderContext ctx = new SimpleBGPExtensionProviderContext();

        final org.opendaylight.protocol.bgp.parser.impl.BGPActivator inetActivator =
                new org.opendaylight.protocol.bgp.parser.impl.BGPActivator();
        inetActivator.start(ctx);
        final BGPActivator bgpActivator = new BGPActivator();
        bgpActivator.start(ctx);
        this.handler = ctx.getAttributeRegistry();
    }

    @Test
    public void testBidirPimTree() throws Exception {
        final Attributes attributes = buildBidirPimTreeAttribute();
        final ByteBuf actual = Unpooled.buffer();
        this.handler.serializeAttribute(attributes, actual);
        assertArrayEquals(BIDIR_PIM_EXPECTED, ByteArray.readAllBytes(actual));
        final Attributes expected = buildBidirPimTreeAttribute();
        final Attributes actualAttr = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(BIDIR_PIM_EXPECTED), null);
        assertEquals(expected, actualAttr);
    }

    @Test
    public void testPimSMTree() throws Exception {
        final Attributes attributes = buildPimSMTreeAttribute();
        final ByteBuf actual = Unpooled.buffer();
        this.handler.serializeAttribute(attributes, actual);
        assertArrayEquals(PIM_SM_TREE_EXPECTED, ByteArray.readAllBytes(actual));
        final Attributes expected = buildPimSMTreeAttribute();
        final Attributes actualAttr = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(PIM_SM_TREE_EXPECTED), null);
        assertEquals(expected, actualAttr);
    }

    @Test
    public void serializePimSSMTree() throws Exception {
        final Attributes attributes = buildPimSSMTreeAttribute();
        final ByteBuf actual = Unpooled.buffer();
        this.handler.serializeAttribute(attributes, actual);
        assertArrayEquals(PIM_SSM_TREE_EXPECTED, ByteArray.readAllBytes(actual));
    }

    @Test
    public void parsePimSSMTree() throws Exception {
        final Attributes expected = buildPimSSMTreeAttribute();
        final Attributes actual = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(PIM_SSM_TREE_EXPECTED), null);
        assertEquals(expected, actual);
    }

    @Test
    public void testMldpP2MpLsp() throws Exception {
        final ByteBuf actualIpv4 = Unpooled.buffer();
        final Attributes expectedIpv4Att = buildMldpP2mpLspIpv4Attribute();

        final BGPExtensionProviderContext providerContext = ServiceLoaderBGPExtensionProviderContext
                .getSingletonInstance();
        providerContext.getAttributeRegistry().serializeAttribute(expectedIpv4Att, actualIpv4);
        assertArrayEquals(M_LDP_P2MP_LSP_EXPECTED_IPV4, ByteArray.readAllBytes(actualIpv4));

        final Attributes actualIpv4Attribute = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(M_LDP_P2MP_LSP_EXPECTED_IPV4_2), null);
        assertEquals(expectedIpv4Att, actualIpv4Attribute);

        final Attributes expectedIpv6Att = buildMldpP2mpLspIpv6Attribute();
        final ByteBuf actualIpv6 = Unpooled.buffer();
        this.handler.serializeAttribute(expectedIpv6Att, actualIpv6);
        assertArrayEquals(M_LDP_P2MP_LSP_EXPECTED_IPV6, ByteArray.readAllBytes(actualIpv6));

        final Attributes actualIpv6Attribute = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(M_LDP_P2MP_LSP_EXPECTED_IPV6), null);
        assertEquals(expectedIpv6Att, actualIpv6Attribute);

        final ByteBuf actualL2vpn = Unpooled.buffer();
        this.handler.serializeAttribute(buildNoSupportedFamilyAttribute(), actualL2vpn);
        assertArrayEquals(new byte[0], ByteArray.readAllBytes(actualIpv4));


        final Attributes actualWrongFamily = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(M_LDP_P2MP_LSP_EXPECTED_WRONG_FAMILY), null);
        assertEquals(buildWOTunnelInfAttribute(), actualWrongFamily);

        final Attributes expectedL2vpnAtt = buildMldpp2MPLspL2vpnAttribute();
        final Attributes actualL2vpnAttribute = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(M_LDP_P2MP_LSP_EXPECTED_L2VPN), null);
        assertEquals(expectedL2vpnAtt, actualL2vpnAttribute);

        final ByteBuf actualL2vp = Unpooled.buffer();
        this.handler.serializeAttribute(expectedL2vpnAtt, actualL2vp);
        assertArrayEquals(M_LDP_P2MP_LSP_EXPECTED_L2VPN, ByteArray.readAllBytes(actualL2vp));

        final ByteBuf actualNonSupportedOpaques = Unpooled.buffer();
        this.handler.serializeAttribute(buildNoSupportedOpaqueAttribute(), actualNonSupportedOpaques);
        assertArrayEquals(NO_TUNNEL_INFORMATION_PRESENT_EXPECTED, ByteArray.readAllBytes(actualNonSupportedOpaques));
    }

    @Test
    public void testRsvpteP2MplspLsp() throws Exception {
        final Attributes expected = buildRsvpTep2MPLspAttribute();
        final ByteBuf actual = Unpooled.buffer();
        this.handler.serializeAttribute(expected, actual);
        assertArrayEquals(RSVP_TE_P2MP_LSP_LSP_EXPECTED, ByteArray.readAllBytes(actual));
        final Attributes actualAttr = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(RSVP_TE_P2MP_LSP_LSP_EXPECTED), null);
        assertEquals(expected, actualAttr);
    }

    @Test
    public void testIngressReplication() throws Exception {
        final Attributes expected = buildIngressReplicationAttribute();
        final ByteBuf actual = Unpooled.buffer();
        this.handler.serializeAttribute(expected, actual);
        assertArrayEquals(INGRESS_REPLICATION_EXPECTED, ByteArray.readAllBytes(actual));
        final Attributes actualAttr = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(INGRESS_REPLICATION_EXPECTED), null);
        assertEquals(expected, actualAttr);
    }

    @Test
    public void testMldpmP2MpLsp() throws Exception {
        final Attributes expected = buildMLDpMp2mPLspAttribute();
        final ByteBuf actual = Unpooled.buffer();
        this.handler.serializeAttribute(expected, actual);
        assertArrayEquals(M_LDP_MP_2_MP_LSP_EXPECTED, ByteArray.readAllBytes(actual));

        final Attributes actualAttr = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(M_LDP_MP_2_MP_LSP_EXPECTED), null);
        assertEquals(expected, actualAttr);

        final Attributes actualWrong = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(M_LDP_MP_2_MP_LSP_WRONG), null);
        assertEquals(buildWOTunnelInfAttribute(), actualWrong);

        final Attributes wrongAttribute = buildMldpMP2mpLspWrongAttribute();
        final ByteBuf actualWrongBuf = Unpooled.buffer();
        this.handler.serializeAttribute(wrongAttribute, actualWrongBuf);
        assertArrayEquals(NO_TUNNEL_INFORMATION_PRESENT_EXPECTED, ByteArray.readAllBytes(actualWrongBuf));
    }


    @Test
    public void serializeNoTunnelInfPresentExpected() throws Exception {
        final Attributes attributes = buildWOTunnelInfAttribute();
        final ByteBuf actual = Unpooled.buffer();
        this.handler.serializeAttribute(attributes, actual);
        assertArrayEquals(NO_TUNNEL_INFORMATION_PRESENT_EXPECTED, ByteArray.readAllBytes(actual));
        final Attributes expected = buildWOTunnelInfAttribute();
        final Attributes actualAttr = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(NO_TUNNEL_INFORMATION_PRESENT_EXPECTED), null);
        assertEquals(expected, actualAttr);
    }

    @Test
    public void testTunnelIdentifierUtil() {
        final TunnelIdentifierHandler tunnelIdentifierHandler =
                new TunnelIdentifierHandler(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
                        .getAddressFamilyRegistry());
        assertNull(tunnelIdentifierHandler.parse(1, Unpooled.buffer()));
        assertNull(tunnelIdentifierHandler.parse(125, Unpooled.buffer()));
        assertEquals(NO_TUNNEL_INFORMATION_PRESENT, tunnelIdentifierHandler
                .serialize(new MockTunnelIdentifier(), Unpooled.buffer()));
    }

    @Test
    public void testPMSITunnelAttributeParser() {
        final PMSITunnelAttributeHandler pmsiHandler =
                new PMSITunnelAttributeHandler(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
                        .getAddressFamilyRegistry());
        assertEquals(22, pmsiHandler.getType());
        final AttributesBuilder builder = new AttributesBuilder();
        final ByteBuf emptyBuffer = Unpooled.buffer();
        pmsiHandler.parseAttribute(emptyBuffer, builder);
        final Attributes emptyAttributes = new AttributesBuilder().build();
        assertEquals(emptyAttributes, builder.build());
        pmsiHandler.serializeAttribute(emptyAttributes, emptyBuffer);
        assertEquals(Unpooled.buffer(), emptyBuffer);
    }
}