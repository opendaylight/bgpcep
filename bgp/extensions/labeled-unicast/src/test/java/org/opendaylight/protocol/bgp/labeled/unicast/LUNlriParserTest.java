/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class LUNlriParserTest {

    /*
     * label stacks with multiple labels.
     *
     * label stack:
     * 60       <- length 96
     * 00 16 3  <- labelValue 355
     * 0        <- etc&bottomBit 0
     * 00 16 4  <- labelValue 356
     * 0        <- etc&bottomBit 0
     * 00 16 6  <- labelValue 357
     * 1        <- bottomBit 1
     * 22 01 16 <- prefixType IPV4=34.1.22.0/24
     */
    private static final byte[] LU_REACH_NLRI_IPV4 = new byte[]{
        (byte) 0x60,
        (byte) 0x00, (byte) 0x16, (byte) 0x30,
        (byte) 0x00, (byte) 0x16, (byte) 0x40,
        (byte) 0x00, (byte) 0x16, (byte) 0x51,
        (byte) 0x22, (byte) 0x01, (byte) 0x16,
    };

    /*
     * label value for withdraw message.
     *
     * label stack:
     * 30       <- length 48
     * 80 00 00 <- labelValue for withdraw
     * 22 01 16 <- prefixType IPV4=34.1.22.0/24
     */
    private static final byte[] LU_UNREACH_NLRI_IPV4 = new byte[] {
        (byte) 0x30,
        (byte) 0x80, (byte) 0x00, (byte) 0x00,
        (byte) 0x22, (byte) 0x01, (byte) 0x16,
    };

    /*
     * label stacks with multiple labels.
     *
     * label stack:
     * 1        <- Path Id
     * 60       <- length 96
     * 00 16 3  <- labelValue 355
     * 0        <- etc&bottomBit 0
     * 00 16 4  <- labelValue 356
     * 0        <- etc&bottomBit 0
     * 00 16 6  <- labelValue 357
     * 1        <- bottomBit 1
     * 22 01 16 <- prefixType IPV4=34.1.22.0/24
     */
    private static final byte[] LU_REACH_NLRI_IPV4_ADD_PATH = new byte[]{
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x60,
        (byte) 0x00, (byte) 0x16, (byte) 0x30,
        (byte) 0x00, (byte) 0x16, (byte) 0x40,
        (byte) 0x00, (byte) 0x16, (byte) 0x51,
        (byte) 0x22, (byte) 0x01, (byte) 0x16,
    };

    /*
     * label value for withdraw message.
     *
     * label stack:
     * 1        <- Path Id
     * 30       <- length 48
     * 80 00 00 <- labelValue for withdraw
     * 22 01 16 <- prefixType IPV4=34.1.22.0/24
     */
    private static final byte[] LU_UNREACH_NLRI_IPV4_ADD_PATH = new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x30,
        (byte) 0x80, (byte) 0x00, (byte) 0x00,
        (byte) 0x22, (byte) 0x01, (byte) 0x16,
    };

    /*
     * label stacks with multiple labels.
     *
     * label stack:
     * C8       <- length 200
     * 00 16 3  <- labelValue 355
     * 0        <- etc&bottomBit 0
     * 00 16 4  <- labelValue 356
     * 0        <- etc&bottomBit 0
     * 00 16 6  <- labelValue 357
     * 1        <- bottomBit 1
     * 20 01 D B8 0 1 0 2 0 0 0 0 0 0 0 0 80  <- prefixType IPV6=2001:db8:1:2::/128
     */
    private static final byte[] LU_REACH_NLRI_IPV6 = new byte[]{
        (byte) 0xC8,
        (byte) 0x00, (byte) 0x16, (byte) 0x30,
        (byte) 0x00, (byte) 0x16, (byte) 0x40,
        (byte) 0x00, (byte) 0x16, (byte) 0x51,
        (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    /*
     * label value for withdraw message.
     *
     * label stack:
     * 98       <- length 152
     * 80 00 00 <- labelValue for withdraw
     * 20 01 D B8 0 1 0 2 0 0 0 0 0 0 0 0 80  <- prefixType IPV6=2001:db8:1:2::/128
     */
    private static final byte[] LU_UNREACH_NLRI_IPV6 = new byte[] {
        (byte) 0x98,
        (byte) 0x80, (byte) 0x00, (byte) 0x00,
        (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    /*
     * label stacks with multiple labels.
     *
     * label stack:
     * C8       <- length 200
     * 00 16 3  <- labelValue 355
     * 0        <- etc&bottomBit 0
     * 00 16 4  <- labelValue 356
     * 0        <- etc&bottomBit 0
     * 00 16 6  <- labelValue 357
     * 1        <- bottomBit 1
     * 20 01 D B8 0 1 0 2 0 0 0 0 0 0 0 0 80  <- prefixType IPV6=2001:db8:1:2::/128
     */
    private static final byte[] LU_REACH_NLRI_IPV6_ADD_PATH = new byte[]{
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0xC8,
        (byte) 0x00, (byte) 0x16, (byte) 0x30,
        (byte) 0x00, (byte) 0x16, (byte) 0x40,
        (byte) 0x00, (byte) 0x16, (byte) 0x51,
        (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    /*
     * label value for withdraw message.
     *
     * label stack:
     * 98       <- length 152
     * 80 00 00 <- labelValue for withdraw
     * 20 01 D B8 0 1 0 2 0 0 0 0 0 0 0 0 80  <- prefixType IPV6=2001:db8:1:2::/128
     */
    private static final byte[] LU_UNREACH_NLRI_IPV6_ADD_PATH = new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x98,
        (byte) 0x80, (byte) 0x00, (byte) 0x00,
        (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    private static final List<LabelStack> LABEL_STACK = List.of(
        new LabelStackBuilder().setLabelValue(new MplsLabel(Uint32.valueOf(355))).build(),
        new LabelStackBuilder().setLabelValue(new MplsLabel(Uint32.valueOf(356))).build(),
        new LabelStackBuilder().setLabelValue(new MplsLabel(Uint32.valueOf(357))).build());

    private static final IpPrefix IPV4_PREFIX = new IpPrefix(new Ipv4Prefix("34.1.22.0/24"));
    private static final IpPrefix IPV6_PREFIX = new IpPrefix(new Ipv6Prefix("2001:db8:1:2::/128"));
    private static final PathId PATH_ID = new PathId(Uint32.ONE);

    @Mock
    private PeerSpecificParserConstraint constraint;
    @Mock
    private MultiPathSupport muliPathSupport;

    @Before
    public void setUp() {
        doReturn(Optional.of(muliPathSupport)).when(constraint).getPeerConstraint(any());
        doReturn(true).when(muliPathSupport).isTableTypeSupported(any());
    }

    @Test
    public void testMpReachNlriIpv4() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        final CLabeledUnicastDestination lu = new CLabeledUnicastDestinationBuilder().setPrefix(IPV4_PREFIX)
            .setLabelStack(LABEL_STACK).build();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(List.of(lu)).build())
                .build()).build());
        final MpReachNlri mpReachExpected = mpBuilder.build();

        //test parser
        final MpReachNlriBuilder testBuilder = new MpReachNlriBuilder();
        testBuilder.setAfi(Ipv4AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_REACH_NLRI_IPV4), testBuilder, null);
        assertEquals(mpReachExpected, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpReachExpected).build())
            .build(), output);
        assertArrayEquals(LU_REACH_NLRI_IPV4, ByteArray.readAllBytes(output));
    }

    @Test
    public void testMpReachNlriIpv4Constraint() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        final CLabeledUnicastDestination lu = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPV4_PREFIX).setLabelStack(LABEL_STACK).build();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(List.of(lu))
                    .build()).build()).build());
        final MpReachNlri mpReachExpected = mpBuilder.build();

        //test parser
        final MpReachNlriBuilder testBuilder = new MpReachNlriBuilder();
        testBuilder.setAfi(Ipv4AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_REACH_NLRI_IPV4_ADD_PATH), testBuilder, constraint);
        assertEquals(mpReachExpected, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpReachExpected).build())
            .build(), output);
        assertArrayEquals(LU_REACH_NLRI_IPV4_ADD_PATH, ByteArray.readAllBytes(output));
    }

    @Test
    public void testMpUnreachNlriIpv4() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpUnreachNlriBuilder mpBuilder = new MpUnreachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);

        final CLabeledUnicastDestination lu1 = new CLabeledUnicastDestinationBuilder().setPrefix(IPV4_PREFIX).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder()
                .setDestinationLabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.labeled.unicast._case.DestinationLabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(List.of(lu1)).build())
                .build()).build());
        final MpUnreachNlri mpUnreachExpected1 = mpBuilder.build();

        final CLabeledUnicastDestination lu2 = new CLabeledUnicastDestinationBuilder().setPrefix(IPV4_PREFIX)
            .setLabelStack(LABEL_STACK).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder()
                .setDestinationLabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.labeled.unicast._case.DestinationLabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(List.of(lu2)).build())
                .build()).build());
        final MpUnreachNlri mpUnreachExpected2 = mpBuilder.build();

        //test parser
        final MpUnreachNlriBuilder testBuilder = new MpUnreachNlriBuilder();
        testBuilder.setAfi(Ipv4AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_UNREACH_NLRI_IPV4), testBuilder, null);
        assertEquals(mpUnreachExpected1, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreachExpected2).build())
            .build(), output);
        assertArrayEquals(LU_UNREACH_NLRI_IPV4, ByteArray.readAllBytes(output));

        final ByteBuf output1 = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreachExpected1).build())
            .build(), output1);
        assertArrayEquals(LU_UNREACH_NLRI_IPV4, ByteArray.readAllBytes(output1));
    }

    @Test
    public void testMpUnreachNlriIpv4Constraint() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpUnreachNlriBuilder mpBuilder = new MpUnreachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);

        final CLabeledUnicastDestination lu1 = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPV4_PREFIX).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder()
                .setDestinationLabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.labeled.unicast._case.DestinationLabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(List.of(lu1)).build())
                .build()).build());
        final MpUnreachNlri mpUnreachExpected1 = mpBuilder.build();

        final CLabeledUnicastDestination lu2 = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPV4_PREFIX).setLabelStack(LABEL_STACK).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder()
                .setDestinationLabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.labeled.unicast._case.DestinationLabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(List.of(lu2)).build())
                .build()).build());
        final MpUnreachNlri mpUnreachExpected2 = mpBuilder.build();

        //test parser
        final MpUnreachNlriBuilder testBuilder = new MpUnreachNlriBuilder();
        testBuilder.setAfi(Ipv4AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_UNREACH_NLRI_IPV4_ADD_PATH), testBuilder, constraint);
        assertEquals(mpUnreachExpected1, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreachExpected2).build())
            .build(), output);
        assertArrayEquals(LU_UNREACH_NLRI_IPV4_ADD_PATH, ByteArray.readAllBytes(output));

        final ByteBuf output1 = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreachExpected1).build())
            .build(), output1);
        assertArrayEquals(LU_UNREACH_NLRI_IPV4_ADD_PATH, ByteArray.readAllBytes(output1));
    }

    @Test
    public void testMpReachNlriIpv6() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        final CLabeledUnicastDestination lu = new CLabeledUnicastDestinationBuilder().setPrefix(IPV6_PREFIX)
            .setLabelStack(LABEL_STACK).build();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv6LabeledUnicastCaseBuilder()
            .setDestinationIpv6LabeledUnicast(new DestinationIpv6LabeledUnicastBuilder()
                .setCLabeledUnicastDestination(List.of(lu)).build())
            .build()).build());
        final MpReachNlri mpReachExpected = mpBuilder.build();

        //test parser
        final MpReachNlriBuilder testBuilder = new MpReachNlriBuilder();
        testBuilder.setAfi(Ipv6AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_REACH_NLRI_IPV6), testBuilder, null);
        assertEquals(mpReachExpected, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpReachExpected).build())
            .build(), output);
        assertArrayEquals(LU_REACH_NLRI_IPV6, ByteArray.readAllBytes(output));
    }

    @Test
    public void testMpReachNlriIpv6Constraint() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        final CLabeledUnicastDestination lu = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPV6_PREFIX).setLabelStack(LABEL_STACK).build();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                new DestinationIpv6LabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(List.of(lu)).build()).build())
            .build());
        final MpReachNlri mpReachExpected = mpBuilder.build();

        //test parser
        final MpReachNlriBuilder testBuilder = new MpReachNlriBuilder();
        testBuilder.setAfi(Ipv6AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_REACH_NLRI_IPV6_ADD_PATH), testBuilder, constraint);
        assertEquals(mpReachExpected, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpReachExpected).build())
            .build(), output);
        assertArrayEquals(LU_REACH_NLRI_IPV6_ADD_PATH, ByteArray.readAllBytes(output));
    }

    @Test
    public void testMpUnreachNlriIpv6() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpUnreachNlriBuilder mpBuilder = new MpUnreachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);

        final CLabeledUnicastDestination lu1 = new CLabeledUnicastDestinationBuilder().setPrefix(IPV6_PREFIX).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder()
                .setDestinationIpv6LabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.labeled.unicast.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(List.of(lu1)).build()).build()).build());
        final MpUnreachNlri mpUnreachExpected1 = mpBuilder.build();

        final CLabeledUnicastDestination lu2 = new CLabeledUnicastDestinationBuilder().setPrefix(IPV6_PREFIX)
            .setLabelStack(LABEL_STACK).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(new org.opendaylight.yang.gen.v1
            .urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.unreach
                .nlri.withdrawn.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder()
            .setDestinationIpv6LabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                .labeled.unicast.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                .destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(List.of(lu2)).build()).build()).build());
        final MpUnreachNlri mpUnreachExpected2 = mpBuilder.build();

        //test parser
        final MpUnreachNlriBuilder testBuilder = new MpUnreachNlriBuilder();
        testBuilder.setAfi(Ipv6AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_UNREACH_NLRI_IPV6), testBuilder, null);
        assertEquals(mpUnreachExpected1, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreachExpected2).build())
            .build(), output);
        assertArrayEquals(LU_UNREACH_NLRI_IPV6, ByteArray.readAllBytes(output));

        final ByteBuf output1 = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreachExpected1).build())
            .build(), output1);
        assertArrayEquals(LU_UNREACH_NLRI_IPV6, ByteArray.readAllBytes(output1));
    }

    @Test
    public void testMpUnreachNlriIpv6Constraint() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpUnreachNlriBuilder mpBuilder = new MpUnreachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);

        final CLabeledUnicastDestination lu1 = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPV6_PREFIX).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder()
                .setDestinationIpv6LabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.labeled.unicast.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(List.of(lu1)).build()).build()).build());
        final MpUnreachNlri mpUnreachExpected1 = mpBuilder.build();

        final CLabeledUnicastDestination lu2 = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPV6_PREFIX).setLabelStack(LABEL_STACK).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329
            .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
            .DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329
                .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.ipv6.labeled
                .unicast._case.DestinationIpv6LabeledUnicastBuilder()
                .setCLabeledUnicastDestination(List.of(lu2)).build()).build()).build());
        final MpUnreachNlri mpUnreachExpected2 = mpBuilder.build();

        //test parser
        final MpUnreachNlriBuilder testBuilder = new MpUnreachNlriBuilder();
        testBuilder.setAfi(Ipv6AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_UNREACH_NLRI_IPV6_ADD_PATH), testBuilder, constraint);
        assertEquals(mpUnreachExpected1, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreachExpected2).build())
            .build(), output);
        assertArrayEquals(LU_UNREACH_NLRI_IPV6_ADD_PATH, ByteArray.readAllBytes(output));

        final ByteBuf output1 = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreachExpected1).build())
            .build(), output1);
        assertArrayEquals(LU_UNREACH_NLRI_IPV6_ADD_PATH, ByteArray.readAllBytes(output1));
    }
}
