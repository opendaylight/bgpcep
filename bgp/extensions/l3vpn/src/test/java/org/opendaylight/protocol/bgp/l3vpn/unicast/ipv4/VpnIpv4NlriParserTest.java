/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.unicast.ipv4;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.l3vpn.ipv4.destination.VpnIpv4DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.destination.type.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.destination.type.VpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.common.Uint32;

public class VpnIpv4NlriParserTest {

    private static final VpnIpv4NlriParser PARSER = new VpnIpv4NlriParser();

    /* Reach NLRI prefix value.
    *
    * prefix contents:
    * 70          <- length 112
    * 00 16 3     <- labelValue 355
    * 1           <- bottomBit 1
    * 00 01       <- routeDistinguisher Type=1
    * 01 02 03 04 <- routeDistinguisher IPV4=1.2.3.4
    * 01 02       <- routeDistinguisher AS=258
    * 22 01 16    <- prefixType IPV4=34.1.22.0/24
    */
    private static final byte[] REACH_NLRI = new byte[] {
        (byte) 0x70,
        (byte) 0x00, (byte) 0x16, (byte) 0x31,
        0, 1, 1, 2, 3, 4, 1, 2,
        (byte) 0x22, (byte) 0x01, (byte) 0x16,
    };

    /* Unreach NLRI prefix value.
    *
    * prefix contents:
    * 70          <- length 112
    * 80 00 00    <- labelValue for withdraw
    * 00 01       <- routeDistinguisher Type=1
    * 01 02 03 04 <- routeDistinguisher IPV4=1.2.3.4
    * 01 02       <- routeDistinguisher AS=258
    * 22 01 16    <- prefixType IPV4=34.1.22.0/24
    */
    private static final byte[] UNREACH_NLRI = new byte[] {
        (byte) 0x70,
        (byte) 0x80, (byte) 0x00, (byte) 0x00,
        0, 1, 1, 2, 3, 4, 1, 2,
        (byte) 0x22, (byte) 0x01, (byte) 0x16,
    };

    static final IpPrefix IPV4_PREFIX = new IpPrefix(new Ipv4Prefix("34.1.22.0/24"));
    static final List<LabelStack> LABEL_STACK = List.of(
        new LabelStackBuilder().setLabelValue(new MplsLabel(Uint32.valueOf(355))).build());
    static final RouteDistinguisher DISTINGUISHER = RouteDistinguisherBuilder
            .getDefaultInstance("1.2.3.4:258");
    static final VpnDestination IPV4_VPN = new VpnDestinationBuilder().setRouteDistinguisher(DISTINGUISHER)
            .setPrefix(IPV4_PREFIX).setPathId(NON_PATH_ID).setLabelStack(LABEL_STACK).build();
    private static final VpnDestination IPV4_VPN_WITHOUT_LABELS = new VpnDestinationBuilder()
            .setRouteDistinguisher(DISTINGUISHER).setPrefix(IPV4_PREFIX).build();

    @Test
    public void testMpReachNlri() throws BGPParsingException {
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(MplsLabeledVpnSubsequentAddressFamily.VALUE);
        mpBuilder.setAdvertizedRoutes(
            new AdvertizedRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.update
                        .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv4CaseBuilder()
                        .setVpnIpv4Destination(new VpnIpv4DestinationBuilder().setVpnDestination(
                                List.of(new VpnDestinationBuilder(IPV4_VPN).setPathId(null)
                                        .build())).build()).build()).build()).build();

        final MpReachNlri mpReachExpected = mpBuilder.build();

        final MpReachNlriBuilder testBuilder = new MpReachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(MplsLabeledVpnSubsequentAddressFamily.VALUE);
        PARSER.parseNlri(Unpooled.copiedBuffer(REACH_NLRI), testBuilder, null);
        assertEquals(mpReachExpected, testBuilder.build());

        final ByteBuf output = Unpooled.buffer();
        PARSER.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpReachExpected).build())
            .build(), output);
        assertArrayEquals(REACH_NLRI, ByteArray.readAllBytes(output));
    }

    @Test
    public void testMpUnreachNlri() throws BGPParsingException {
        final MpUnreachNlriBuilder mpBuilder = new MpUnreachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(MplsLabeledVpnSubsequentAddressFamily.VALUE);

        mpBuilder.setWithdrawnRoutes(
            new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv4CaseBuilder()
                        .setVpnIpv4Destination(new VpnIpv4DestinationBuilder().setVpnDestination(
                                List.of(IPV4_VPN_WITHOUT_LABELS)).build()).build()).build()).build();
        final MpUnreachNlri mpUnreachExpected1 = mpBuilder.build();

        mpBuilder.setWithdrawnRoutes(
            new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv4CaseBuilder()
                        .setVpnIpv4Destination(new VpnIpv4DestinationBuilder().setVpnDestination(
                                List.of(IPV4_VPN)).build()).build()).build()).build();
        final MpUnreachNlri mpUnreachExpected2 = mpBuilder.build();

        final MpUnreachNlriBuilder testBuilder = new MpUnreachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(MplsLabeledVpnSubsequentAddressFamily.VALUE);
        PARSER.parseNlri(Unpooled.copiedBuffer(UNREACH_NLRI), testBuilder, null);
        assertEquals(mpUnreachExpected1, testBuilder.build());

        final ByteBuf output = Unpooled.buffer();
        PARSER.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreachExpected2).build())
            .build(), output);
        assertArrayEquals(UNREACH_NLRI, ByteArray.readAllBytes(output));
    }
}
