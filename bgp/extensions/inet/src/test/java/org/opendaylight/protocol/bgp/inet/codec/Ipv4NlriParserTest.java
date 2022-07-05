/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class Ipv4NlriParserTest {
    private static final byte[] MP_NLRI_BYTES = new byte[]{
        0x0, 0x0, 0x0, 0x1, 0x18, 0x1, 0x1, 0x1,
        0x0, 0x0, 0x0, 0x2, 0x18, 0x1, 0x1, 0x1};
    private static final Ipv4Prefix DESTINATION = new Ipv4Prefix("1.1.1.0/24");
    private static final ArrayList<Ipv4Prefixes> PREFIXES = Lists.newArrayList(
            createIpv4Prefix(1L, DESTINATION),
            createIpv4Prefix(2L, DESTINATION));
    private final Ipv4NlriParser parser = new Ipv4NlriParser();
    private final String ipPrefix1 = "1.2.3.4/32";
    private final String ipPrefix2 = "1.2.3.5/32";
    private final String additionalIpWD = "1.2.3.6/32";
    private final List<Ipv4Prefixes> prefixes = new ArrayList<>();
    private final ByteBuf inputBytes = Unpooled.buffer();
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp
            .unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4Case ip4caseWD;
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp
            .unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4Case ip4caseWDWrong;
    private DestinationIpv4Case ip4caseAD;
    private DestinationIpv4Case ip4caseADWrong;

    @Mock
    private PeerSpecificParserConstraint constraint;

    @Mock
    private MultiPathSupport muliPathSupport;

    private static Ipv4Prefixes createIpv4Prefix(final long pathId, final Ipv4Prefix prefix) {
        return new Ipv4PrefixesBuilder().setPathId(new PathId(Uint32.valueOf(pathId))).setPrefix(prefix).build();
    }

    @Before
    public void setUp() {
        final Ipv4Prefix prefix1 = new Ipv4Prefix(ipPrefix1);
        final Ipv4Prefix prefix2 = new Ipv4Prefix(ipPrefix2);
        final Ipv4Prefix wrongPrefix = new Ipv4Prefix(additionalIpWD);
        prefixes.add(new Ipv4PrefixesBuilder().setPrefix(prefix1).build());
        prefixes.add(new Ipv4PrefixesBuilder().setPrefix(prefix2).build());

        ip4caseWD = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329
                .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder()
                .setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build()).build();
        ip4caseAD = new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder()
                .setIpv4Prefixes(prefixes).build()).build();

        final ArrayList<Ipv4Prefixes> fakePrefixes = new ArrayList<>(prefixes);
        fakePrefixes.add(new Ipv4PrefixesBuilder().setPrefix(wrongPrefix).build());
        ip4caseWDWrong = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329
                .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder()
                .setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(fakePrefixes).build()).build();
        ip4caseADWrong = new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder()
                .setIpv4Prefixes(fakePrefixes).build()).build();

        final ByteBuf buffer1 = Unpooled.buffer(5);
        Ipv4Util.writeMinimalPrefix(prefix1, buffer1);
        inputBytes.writeBytes(buffer1.array());

        final ByteBuf buffer2 = Unpooled.buffer(5);
        Ipv4Util.writeMinimalPrefix(prefix2, buffer2);
        inputBytes.writeBytes(buffer2.array());

        Mockito.doReturn(Optional.of(muliPathSupport)).when(constraint).getPeerConstraint(Mockito.any());
        Mockito.doReturn(true).when(muliPathSupport).isTableTypeSupported(Mockito.any());
    }

    @Test
    public void prefixesTest() {
        assertEquals(ipPrefix1, prefixes.get(0).getPrefix().getValue());
        assertEquals(ipPrefix2, prefixes.get(1).getPrefix().getValue());
        assertEquals(2, prefixes.size());
    }

    @Test
    public void parseUnreachedNlriTest() {
        final MpUnreachNlriBuilder b = new MpUnreachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE);
        parser.parseNlri(inputBytes, b, null);
        assertNotNull("Withdrawn routes, destination type should not be null", b.getWithdrawnRoutes()
                .getDestinationType());

        assertEquals(ip4caseWD.hashCode(), b.getWithdrawnRoutes().getDestinationType().hashCode());
        assertNotEquals(ip4caseWDWrong.hashCode(), b.getWithdrawnRoutes().getDestinationType().hashCode());

        assertEquals(ip4caseWD.toString(), b.getWithdrawnRoutes().getDestinationType().toString());
        assertNotEquals(ip4caseWDWrong.toString(), b.getWithdrawnRoutes().getDestinationType().toString());
    }

    @Test
    public void parseReachedNlriTest() throws BGPParsingException {
        final MpReachNlriBuilder b = new MpReachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE);
        parser.parseNlri(inputBytes, b, null);
        assertNotNull("Advertized routes, destination type should not be null", b.getAdvertizedRoutes()
                .getDestinationType());

        assertEquals(ip4caseAD.hashCode(), b.getAdvertizedRoutes().getDestinationType().hashCode());
        assertNotEquals(ip4caseADWrong.hashCode(), b.getAdvertizedRoutes().getDestinationType().hashCode());

        assertEquals(ip4caseAD.toString(), b.getAdvertizedRoutes().getDestinationType().toString());
        assertNotEquals(ip4caseADWrong.toString(), b.getAdvertizedRoutes().getDestinationType().toString());
    }

    @Test
    public void parseReachNlriMultiPathTest() {
        final MpReachNlri mpReachNlri = new MpReachNlriBuilder().setAdvertizedRoutes(
                new AdvertizedRoutesBuilder().setDestinationType(
                        new DestinationIpv4CaseBuilder().setDestinationIpv4(
                                new DestinationIpv4Builder().setIpv4Prefixes(
                                        PREFIXES).build()).build()).build()).build();
        final MpReachNlriBuilder mpReachNlriBuilder = new MpReachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE);
        parser.parseNlri(Unpooled.wrappedBuffer(MP_NLRI_BYTES), mpReachNlriBuilder, constraint);
        mpReachNlriBuilder.setAfi(null).setSafi(null);
        Assert.assertEquals(mpReachNlri, mpReachNlriBuilder.build());

        final Ipv4NlriParser serializer = new Ipv4NlriParser();
        final ByteBuf output = Unpooled.buffer(MP_NLRI_BYTES.length);
        final Attributes attributes = new AttributesBuilder()
                .addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpReachNlri).build())
                .build();
        serializer.serializeAttribute(attributes, output);
        Assert.assertArrayEquals(MP_NLRI_BYTES, output.array());
    }

    @Test
    public void parseUnreachNlriMultiPathTest() {
        final MpUnreachNlri mpUnreachNlri = new MpUnreachNlriBuilder().setWithdrawnRoutes(
                new WithdrawnRoutesBuilder().setDestinationType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update
                                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                                .DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder()
                                .setIpv4Prefixes(PREFIXES).build()).build()).build()).build();
        final MpUnreachNlriBuilder mpUnreachNlriBuilder = new MpUnreachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE);
        parser.parseNlri(Unpooled.wrappedBuffer(MP_NLRI_BYTES), mpUnreachNlriBuilder, constraint);
        mpUnreachNlriBuilder.setAfi(null).setSafi(null);
        Assert.assertEquals(mpUnreachNlri, mpUnreachNlriBuilder.build());

        final Ipv4NlriParser serializer = new Ipv4NlriParser();
        final ByteBuf output = Unpooled.buffer(MP_NLRI_BYTES.length);
        final Attributes attributes = new AttributesBuilder()
                .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreachNlri).build())
                .build();
        serializer.serializeAttribute(attributes, output);
        Assert.assertArrayEquals(MP_NLRI_BYTES, output.array());
    }
}
