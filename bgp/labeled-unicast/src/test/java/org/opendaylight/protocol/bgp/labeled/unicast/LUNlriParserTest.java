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

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;

public class LUNlriParserTest {

    /*label stacks with multiple labels.
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
    private static final byte[] LU_REACH_NLRI_IPv4 = new byte[]{
        (byte) 0x60,
        (byte) 0x00, (byte) 0x16, (byte) 0x30,
        (byte) 0x00, (byte) 0x16, (byte) 0x40,
        (byte) 0x00, (byte) 0x16, (byte) 0x51,
        (byte) 0x22, (byte) 0x01, (byte) 0x16,
    };

    /*label value for withdraw message.
    *
    * label stack:
    * 30       <- length 48
    * 80 00 00 <- labelValue for withdraw
    * 22 01 16 <- prefixType IPV4=34.1.22.0/24
    */
   private static final byte[] LU_UNREACH_NLRI_IPv4 = new byte[] {
       (byte) 0x30,
       (byte) 0x80, (byte) 0x00, (byte) 0x00,
       (byte) 0x22, (byte) 0x01, (byte) 0x16,
   };

    /*label stacks with multiple labels.
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
    private static final byte[] LU_REACH_NLRI_IPv4_ADD_PATH = new byte[]{
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x60,
        (byte) 0x00, (byte) 0x16, (byte) 0x30,
        (byte) 0x00, (byte) 0x16, (byte) 0x40,
        (byte) 0x00, (byte) 0x16, (byte) 0x51,
        (byte) 0x22, (byte) 0x01, (byte) 0x16,
    };

    /*label value for withdraw message.
    *
    * label stack:
    * 1        <- Path Id
    * 30       <- length 48
    * 80 00 00 <- labelValue for withdraw
    * 22 01 16 <- prefixType IPV4=34.1.22.0/24
    */
    private static final byte[] LU_UNREACH_NLRI_IPv4_ADD_PATH = new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x30,
        (byte) 0x80, (byte) 0x00, (byte) 0x00,
        (byte) 0x22, (byte) 0x01, (byte) 0x16,
    };

    /*label stacks with multiple labels.
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
    private static final byte[] LU_REACH_NLRI_IPv6 = new byte[]{
        (byte) 0xC8,
        (byte) 0x00, (byte) 0x16, (byte) 0x30,
        (byte) 0x00, (byte) 0x16, (byte) 0x40,
        (byte) 0x00, (byte) 0x16, (byte) 0x51,
        (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    /*label value for withdraw message.
    *
    * label stack:
    * 98       <- length 152
    * 80 00 00 <- labelValue for withdraw
    * 20 01 D B8 0 1 0 2 0 0 0 0 0 0 0 0 80  <- prefixType IPV6=2001:db8:1:2::/128
    */
   private static final byte[] LU_UNREACH_NLRI_IPv6 = new byte[] {
       (byte) 0x98,
       (byte) 0x80, (byte) 0x00, (byte) 0x00,
       (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
       (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x02,
       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
   };

    /*label stacks with multiple labels.
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
    private static final byte[] LU_REACH_NLRI_IPv6_ADD_PATH = new byte[]{
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

    /*label value for withdraw message.
    *
    * label stack:
    * 98       <- length 152
    * 80 00 00 <- labelValue for withdraw
    * 20 01 D B8 0 1 0 2 0 0 0 0 0 0 0 0 80  <- prefixType IPV6=2001:db8:1:2::/128
    */
   private static final byte[] LU_UNREACH_NLRI_IPv6_ADD_PATH = new byte[] {
       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
       (byte) 0x98,
       (byte) 0x80, (byte) 0x00, (byte) 0x00,
       (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
       (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x02,
       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
   };

    private static final List<LabelStack> LABEL_STACK = Lists.newArrayList(
        new LabelStackBuilder().setLabelValue(new MplsLabel(355L)).build(),
        new LabelStackBuilder().setLabelValue(new MplsLabel(356L)).build(),
        new LabelStackBuilder().setLabelValue(new MplsLabel(357L)).build());

    private static final IpPrefix IPv4_PREFIX = new IpPrefix(new Ipv4Prefix("34.1.22.0/24"));
    private static final IpPrefix IPv6_PREFIX = new IpPrefix(new Ipv6Prefix("2001:db8:1:2::/128"));
    private static final PathId PATH_ID = new PathId(1L);

    @Mock
    private PeerSpecificParserConstraint constraint;
    @Mock
    private MultiPathSupport muliPathSupport;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(Optional.of(this.muliPathSupport)).when(this.constraint).getPeerConstraint(Mockito.any());
        Mockito.doReturn(true).when(this.muliPathSupport).isTableTypeSupported(Mockito.any());
    }

    @Test
    public void testMpReachNlriIpv4() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        final CLabeledUnicastDestination lu = new CLabeledUnicastDestinationBuilder().setPrefix(IPv4_PREFIX)
            .setLabelStack(LABEL_STACK).build();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(Lists.newArrayList(lu)).build())
                .build()).build());
        final MpReachNlri mpReachExpected = mpBuilder.build();

        //test parser
        final MpReachNlriBuilder testBuilder = new MpReachNlriBuilder();
        testBuilder.setAfi(Ipv4AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_REACH_NLRI_IPv4), testBuilder);
        assertEquals(mpReachExpected, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class,
            new Attributes1Builder().setMpReachNlri(mpReachExpected).build()).build(), output);
        assertArrayEquals(LU_REACH_NLRI_IPv4, ByteArray.readAllBytes(output));
    }

    @Test
    public void testMpReachNlriIpv4Constraint() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        final CLabeledUnicastDestination lu = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPv4_PREFIX).setLabelStack
            (LABEL_STACK).build();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(Lists.newArrayList(lu))
                    .build()).build()).build());
        final MpReachNlri mpReachExpected = mpBuilder.build();

        //test parser
        final MpReachNlriBuilder testBuilder = new MpReachNlriBuilder();
        testBuilder.setAfi(Ipv4AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_REACH_NLRI_IPv4_ADD_PATH), testBuilder, this.constraint);
        assertEquals(mpReachExpected, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class,
            new Attributes1Builder().setMpReachNlri(mpReachExpected).build()).build(), output);
        assertArrayEquals(LU_REACH_NLRI_IPv4_ADD_PATH, ByteArray.readAllBytes(output));
    }

    @Test
    public void testMpUnreachNlriIpv4() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpUnreachNlriBuilder mpBuilder = new MpUnreachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);

        final CLabeledUnicastDestination lu1 = new CLabeledUnicastDestinationBuilder().setPrefix(IPv4_PREFIX).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder()
                .setDestinationLabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.labeled.unicast._case.DestinationLabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(Lists.newArrayList(lu1)).build())
                .build()).build());
        final MpUnreachNlri mpUnreachExpected1 = mpBuilder.build();

        final CLabeledUnicastDestination lu2 = new CLabeledUnicastDestinationBuilder().setPrefix(IPv4_PREFIX)
            .setLabelStack(LABEL_STACK).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder()
                .setDestinationLabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.
                    destination.labeled.unicast._case.DestinationLabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(Lists.newArrayList(lu2)).build())
                .build()).build());
        final MpUnreachNlri mpUnreachExpected2 = mpBuilder.build();

        //test parser
        final MpUnreachNlriBuilder testBuilder = new MpUnreachNlriBuilder();
        testBuilder.setAfi(Ipv4AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_UNREACH_NLRI_IPv4), testBuilder);
        assertEquals(mpUnreachExpected1, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class,
            new Attributes2Builder().setMpUnreachNlri(mpUnreachExpected2).build()).build(), output);
        assertArrayEquals(LU_UNREACH_NLRI_IPv4, ByteArray.readAllBytes(output));

        final ByteBuf output1 = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class,
            new Attributes2Builder().setMpUnreachNlri(mpUnreachExpected1).build()).build(), output1);
        assertArrayEquals(LU_UNREACH_NLRI_IPv4, ByteArray.readAllBytes(output1));
    }

    @Test
    public void testMpUnreachNlriIpv4Constraint() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpUnreachNlriBuilder mpBuilder = new MpUnreachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);

        final CLabeledUnicastDestination lu1 = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPv4_PREFIX).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder()
                .setDestinationLabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.labeled.unicast._case.DestinationLabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(Lists.newArrayList(lu1)).build())
                .build()).build());
        final MpUnreachNlri mpUnreachExpected1 = mpBuilder.build();

        final CLabeledUnicastDestination lu2 = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPv4_PREFIX).setLabelStack(LABEL_STACK).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder()
                .setDestinationLabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.labeled.unicast._case.DestinationLabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(Lists.newArrayList(lu2)).build())
                .build()).build());
        final MpUnreachNlri mpUnreachExpected2 = mpBuilder.build();

        //test parser
        final MpUnreachNlriBuilder testBuilder = new MpUnreachNlriBuilder();
        testBuilder.setAfi(Ipv4AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_UNREACH_NLRI_IPv4_ADD_PATH), testBuilder, this.constraint);
        assertEquals(mpUnreachExpected1, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class,
            new Attributes2Builder().setMpUnreachNlri(mpUnreachExpected2).build()).build(), output);
        assertArrayEquals(LU_UNREACH_NLRI_IPv4_ADD_PATH, ByteArray.readAllBytes(output));

        final ByteBuf output1 = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class,
            new Attributes2Builder().setMpUnreachNlri(mpUnreachExpected1).build()).build(), output1);
        assertArrayEquals(LU_UNREACH_NLRI_IPv4_ADD_PATH, ByteArray.readAllBytes(output1));
    }

    @Test
    public void testMpReachNlriIpv6() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        final CLabeledUnicastDestination lu = new CLabeledUnicastDestinationBuilder().setPrefix(IPv6_PREFIX)
            .setLabelStack(LABEL_STACK).build();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv6LabeledUnicastCaseBuilder()
            .setDestinationIpv6LabeledUnicast(new DestinationIpv6LabeledUnicastBuilder()
                .setCLabeledUnicastDestination(Lists.newArrayList(lu)).build())
            .build()).build());
        final MpReachNlri mpReachExpected = mpBuilder.build();

        //test parser
        final MpReachNlriBuilder testBuilder = new MpReachNlriBuilder();
        testBuilder.setAfi(Ipv6AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_REACH_NLRI_IPv6), testBuilder);
        assertEquals(mpReachExpected, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class,
            new Attributes1Builder().setMpReachNlri(mpReachExpected).build()).build(), output);
        assertArrayEquals(LU_REACH_NLRI_IPv6, ByteArray.readAllBytes(output));
    }

    @Test
    public void testMpReachNlriIpv6Constraint() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        final CLabeledUnicastDestination lu = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPv6_PREFIX).setLabelStack(LABEL_STACK).build();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                new DestinationIpv6LabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(Lists.newArrayList(lu)).build()).build())
            .build());
        final MpReachNlri mpReachExpected = mpBuilder.build();

        //test parser
        final MpReachNlriBuilder testBuilder = new MpReachNlriBuilder();
        testBuilder.setAfi(Ipv6AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_REACH_NLRI_IPv6_ADD_PATH), testBuilder, this.constraint);
        assertEquals(mpReachExpected, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class,
            new Attributes1Builder().setMpReachNlri(mpReachExpected).build()).build(), output);
        assertArrayEquals(LU_REACH_NLRI_IPv6_ADD_PATH, ByteArray.readAllBytes(output));
    }

    @Test
    public void testMpUnreachNlriIpv6() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpUnreachNlriBuilder mpBuilder = new MpUnreachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);

        final CLabeledUnicastDestination lu1 = new CLabeledUnicastDestinationBuilder().setPrefix(IPv6_PREFIX).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder()
                .setDestinationIpv6LabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(Lists.newArrayList(lu1)).build()).build()).build());
        final MpUnreachNlri mpUnreachExpected1 = mpBuilder.build();

        final CLabeledUnicastDestination lu2 = new CLabeledUnicastDestinationBuilder().setPrefix(IPv6_PREFIX)
            .setLabelStack(LABEL_STACK).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(new org.opendaylight.yang.gen.v1
            .urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach
                .nlri.withdrawn.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder()
            .setDestinationIpv6LabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                .labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                .destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(Lists.newArrayList(lu2)).build()).build()).build());
        final MpUnreachNlri mpUnreachExpected2 = mpBuilder.build();

        //test parser
        final MpUnreachNlriBuilder testBuilder = new MpUnreachNlriBuilder();
        testBuilder.setAfi(Ipv6AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_UNREACH_NLRI_IPv6), testBuilder);
        assertEquals(mpUnreachExpected1, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class,
            new Attributes2Builder().setMpUnreachNlri(mpUnreachExpected2).build()).build(), output);
        assertArrayEquals(LU_UNREACH_NLRI_IPv6, ByteArray.readAllBytes(output));

        final ByteBuf output1 = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class,
            new Attributes2Builder().setMpUnreachNlri(mpUnreachExpected1).build()).build(), output1);
        assertArrayEquals(LU_UNREACH_NLRI_IPv6, ByteArray.readAllBytes(output1));
    }

    @Test
    public void testMpUnreachNlriIpv6Constraint() throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpUnreachNlriBuilder mpBuilder = new MpUnreachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);

        final CLabeledUnicastDestination lu1 = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPv6_PREFIX).build();
        mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder()
                .setDestinationIpv6LabeledUnicast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(Lists.newArrayList(lu1)).build()).build()).build());
        final MpUnreachNlri mpUnreachExpected1 = mpBuilder.build();

        final CLabeledUnicastDestination lu2 = new CLabeledUnicastDestinationBuilder().setPathId(PATH_ID)
            .setPrefix(IPv6_PREFIX).setLabelStack(LABEL_STACK).build();
            mpBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525
                    .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525
                        .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.ipv6.labeled
                        .unicast._case.DestinationIpv6LabeledUnicastBuilder()
                        .setCLabeledUnicastDestination(Lists.newArrayList(lu2)).build()).build()).build());
            final MpUnreachNlri mpUnreachExpected2 = mpBuilder.build();

        //test parser
        final MpUnreachNlriBuilder testBuilder = new MpUnreachNlriBuilder();
        testBuilder.setAfi(Ipv6AddressFamily.class);
        testBuilder.setSafi(LabeledUnicastSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.copiedBuffer(LU_UNREACH_NLRI_IPv6_ADD_PATH), testBuilder, this.constraint);
        assertEquals(mpUnreachExpected1, testBuilder.build());

        //test serializer
        final ByteBuf output = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class,
            new Attributes2Builder().setMpUnreachNlri(mpUnreachExpected2).build()).build(), output);
        assertArrayEquals(LU_UNREACH_NLRI_IPv6_ADD_PATH, ByteArray.readAllBytes(output));

        final ByteBuf output1 = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class,
            new Attributes2Builder().setMpUnreachNlri(mpUnreachExpected1).build()).build(), output1);
        assertArrayEquals(LU_UNREACH_NLRI_IPv6_ADD_PATH, ByteArray.readAllBytes(output1));
    }
}
