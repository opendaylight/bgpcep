/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv6;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStackBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.l3vpn.ipv6.destination.VpnIpv6DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.ip.destination.type.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.ip.destination.type.VpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;

/**
 * @author Kevin Wang
 */
public class VpnIpv6NlriParserTest {

    private static final VpnIpv6NlriParser PARSER = new VpnIpv6NlriParser();

    /* Reach NLRI prefix value.
    *
    * prefix contents:
    * 88          <- length 136
    * 00 16 3     <- labelValue 355
    * 01          <- bottomBit 1
    * 00 01       <- routeDistinguisher Type=1
    * 01 02 03 04 <- routeDistinguisher IPV4=1.2.3.4
    * 01 02       <- routeDistinguisher AS=258
    * 20 01 23 45 56 89 <- prefixType IPV6=2001:2345:5689::/48
    */
    private static final byte[] REACH_NLRI = new byte[] {
        (byte) 0x88,
        (byte) 0x00, (byte) 0x16, (byte) 0x31,
        0, 1, 1, 2, 3, 4, 1, 2,
        (byte) 0x20, (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x56, (byte) 0x89
    };

    /* Unreach NLRI prefix value.
    *
    * prefix contents:
    * 88          <- length 136
    * 80 00 00    <- labelValue for withdraw
    * 00 01       <- routeDistinguisher Type=1
    * 01 02 03 04 <- routeDistinguisher IPV4=1.2.3.4
    * 01 02       <- routeDistinguisher AS=258
    * 20 01 23 45 56 89 <- prefixType IPV6=2001:2345:5689::/48
    */
    private static final byte[] UNREACH_NLRI = new byte[] {
        (byte) 0x88,
        (byte) 0x80, (byte) 0x00, (byte) 0x00,
        0, 1, 1, 2, 3, 4, 1, 2,
        (byte) 0x20, (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x56, (byte) 0x89
    };

    static final IpPrefix IPv6_PREFIX = new IpPrefix(new Ipv6Prefix("2001:2345:5689::/48"));
    static final List<LabelStack> LABEL_STACK = Lists.newArrayList(
        new LabelStackBuilder().setLabelValue(new MplsLabel(355L)).build());
    static final RouteDistinguisher DISTINGUISHER = RouteDistinguisherBuilder.getDefaultInstance("1.2.3.4:258");
    static final VpnDestination IPV6_VPN = new VpnDestinationBuilder().setRouteDistinguisher(DISTINGUISHER).setPrefix(IPv6_PREFIX).setLabelStack(LABEL_STACK).build();
    private static final VpnDestination IPV6_VPN_WITHOUT_LABELS = new VpnDestinationBuilder().setRouteDistinguisher(DISTINGUISHER).setPrefix(IPv6_PREFIX).build();

    @Test
    public void testMpReachNlri() throws BGPParsingException {
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv6CaseBuilder().setVpnIpv6Destination(
                new VpnIpv6DestinationBuilder().setVpnDestination(Lists.newArrayList(IPV6_VPN)).build()
            ).build()
            ).build()
        ).build();

        final MpReachNlri mpReachExpected = mpBuilder.build();

        final MpReachNlriBuilder testBuilder = new MpReachNlriBuilder();
        testBuilder.setAfi(Ipv6AddressFamily.class);
        PARSER.parseNlri(Unpooled.copiedBuffer(REACH_NLRI), testBuilder);
        Assert.assertEquals(mpReachExpected, testBuilder.build());

        final ByteBuf output = Unpooled.buffer();
        PARSER.serializeAttribute(
            new AttributesBuilder().addAugmentation(Attributes1.class,
                new Attributes1Builder().setMpReachNlri(mpReachExpected).build()
            ).build(), output
        );
        Assert.assertArrayEquals(REACH_NLRI, ByteArray.readAllBytes(output));
    }

    @Test
    public void testMpUnreachNlri() throws BGPParsingException {
        final MpUnreachNlriBuilder mpBuilder = new MpUnreachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);

        mpBuilder.setWithdrawnRoutes(
            new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv6CaseBuilder().setVpnIpv6Destination(
                    new VpnIpv6DestinationBuilder().setVpnDestination(Lists.newArrayList(IPV6_VPN_WITHOUT_LABELS)).build()
                ).build()
            ).build()
        ).build();
        final MpUnreachNlri mpUnreachExpected1 = mpBuilder.build();

        mpBuilder.setWithdrawnRoutes(
            new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv6CaseBuilder().setVpnIpv6Destination(
                    new VpnIpv6DestinationBuilder().setVpnDestination(Lists.newArrayList(IPV6_VPN)).build()
                ).build()
            ).build()
        ).build();
        final MpUnreachNlri mpUnreachExpected2 = mpBuilder.build();

        final MpUnreachNlriBuilder testBuilder = new MpUnreachNlriBuilder();
        testBuilder.setAfi(Ipv6AddressFamily.class);
        PARSER.parseNlri(Unpooled.copiedBuffer(UNREACH_NLRI), testBuilder);
        Assert.assertEquals(mpUnreachExpected1, testBuilder.build());

        final ByteBuf output = Unpooled.buffer();
        PARSER.serializeAttribute(
            new AttributesBuilder().addAugmentation(Attributes2.class,
                new Attributes2Builder().setMpUnreachNlri(mpUnreachExpected2).build()
            ).build(), output
        );
        Assert.assertArrayEquals(UNREACH_NLRI, ByteArray.readAllBytes(output));
    }
}
