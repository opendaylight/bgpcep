/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AdvertizedRoutesSerializer;
import org.opendaylight.protocol.bgp.parser.impl.message.update.Ipv4NlriParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.WithdrawnRoutesSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.NlriBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class Ipv4NlriParserTest {
    private final Ipv4NlriParser parser = new Ipv4NlriParser();

    private final String ipPrefix1 = "1.2.3.4/32";
    private final String ipPrefix2 = "1.2.3.5/32";
    private final String additionalIpWD = "1.2.3.6/32";

    private final List<Ipv4Prefixes> prefixes = new ArrayList<>();
    private final ByteBuf inputBytes = Unpooled.buffer();

    private static final byte[] MP_NLRI_BYTES = new byte[] {
        0x0, 0x0, 0x0, 0x1, 0x18, 0x1, 0x1, 0x1,
        0x0, 0x0, 0x0, 0x2, 0x18, 0x1, 0x1, 0x1};

    private static final Ipv4Prefix DESTINATION = new Ipv4Prefix("1.1.1.0/24");

    private static final ArrayList<Ipv4Prefixes> PREFIXES = Lists.newArrayList(
            createIpv4Prefix(1L, DESTINATION),
            createIpv4Prefix(2L, DESTINATION));

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4Case ip4caseWD;
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4Case ip4caseWDWrong;
    private DestinationIpv4Case ip4caseAD;
    private DestinationIpv4Case ip4caseADWrong;

    private Nlri nlri;
    private Nlri nlriWrong;

    @Mock
    private PeerSpecificParserConstraint constraint;

    @Mock
    private MultiPathSupport muliPathSupport;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Ipv4Prefix prefix1 = new Ipv4Prefix(this.ipPrefix1);
        final Ipv4Prefix prefix2 = new Ipv4Prefix(this.ipPrefix2);
        final Ipv4Prefix wrongPrefix = new Ipv4Prefix(this.additionalIpWD);
        this.prefixes.add(new Ipv4PrefixesBuilder().setPrefix(prefix1).build());
        this.prefixes.add(new Ipv4PrefixesBuilder().setPrefix(prefix2).build());

        this.ip4caseWD = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
            new DestinationIpv4Builder().setIpv4Prefixes(this.prefixes).build()).build();
        this.ip4caseAD = new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(this.prefixes).build()).build();

        final ArrayList<Ipv4Prefixes> fakePrefixes = new ArrayList<Ipv4Prefixes>(this.prefixes);
        fakePrefixes.add(new Ipv4PrefixesBuilder().setPrefix(wrongPrefix).build());
        this.ip4caseWDWrong = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
            new DestinationIpv4Builder().setIpv4Prefixes(fakePrefixes).build()).build();
        this.ip4caseADWrong = new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(fakePrefixes).build()).build();

        this.inputBytes.writeBytes(Ipv4Util.bytesForPrefixBegin(prefix1));
        this.inputBytes.writeBytes(Ipv4Util.bytesForPrefixBegin(prefix2));

        final List<Ipv4Prefix> prefixList = new ArrayList<Ipv4Prefix>();
        prefixList.add(prefix1);
        prefixList.add(prefix2);
        this.nlri = new NlriBuilder().setNlri(prefixList).build();
        final List<Ipv4Prefix> prefixWrongList = Lists.newArrayList(prefixList.iterator());
        prefixWrongList.add(wrongPrefix);
        this.nlriWrong = new NlriBuilder().setNlri(prefixWrongList).build();
        Mockito.doReturn(Optional.of(this.muliPathSupport)).when(constraint).getPeerConstraint(Mockito.any());
        Mockito.doReturn(true).when(this.muliPathSupport).isTableTypeSupported(Mockito.any());
    }

    @Test
    public void prefixesTest() {
        assertEquals(this.ipPrefix1, this.prefixes.get(0).getPrefix().getValue());
        assertEquals(this.ipPrefix2, this.prefixes.get(1).getPrefix().getValue());
        assertEquals(2, this.prefixes.size());
    }

    @Test
    public void serializeAttributeTest() throws UnsupportedEncodingException {
        final ByteBuf outputBytes = Unpooled.buffer();

        this.parser.serializeAttribute(this.nlri, outputBytes);
        assertArrayEquals(this.inputBytes.array(), outputBytes.array());
        this.parser.serializeAttribute(this.nlriWrong, outputBytes);
        assertFalse(Arrays.equals(this.inputBytes.array(), outputBytes.array()));
    }

    @Test
    public void parseUnreachedNlriTest() {
        final MpUnreachNlriBuilder b = new MpUnreachNlriBuilder();
        b.setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class);
        this.parser.parseNlri(this.inputBytes, b);
        assertNotNull("Withdrawn routes, destination type should not be null", b.getWithdrawnRoutes().getDestinationType());

        assertEquals(this.ip4caseWD.hashCode(), b.getWithdrawnRoutes().getDestinationType().hashCode());
        assertFalse(this.ip4caseWDWrong.hashCode() == b.getWithdrawnRoutes().getDestinationType().hashCode());

        assertTrue(this.ip4caseWD.toString().equals(b.getWithdrawnRoutes().getDestinationType().toString()));
        assertFalse(this.ip4caseWDWrong.toString().equals(b.getWithdrawnRoutes().getDestinationType().toString()));
    }

    @Test
    public void parseReachedNlriTest() throws BGPParsingException {
        final MpReachNlriBuilder b = new MpReachNlriBuilder();
        b.setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class);
        this.parser.parseNlri(this.inputBytes, b);
        assertNotNull("Advertized routes, destination type should not be null", b.getAdvertizedRoutes().getDestinationType());

        assertEquals(this.ip4caseAD.hashCode(), b.getAdvertizedRoutes().getDestinationType().hashCode());
        assertFalse(this.ip4caseADWrong.hashCode() == b.getAdvertizedRoutes().getDestinationType().hashCode());

        assertTrue(this.ip4caseAD.toString().equals(b.getAdvertizedRoutes().getDestinationType().toString()));
        assertFalse(this.ip4caseADWrong.toString().equals(b.getAdvertizedRoutes().getDestinationType().toString()));
    }

    @Test
    public void parseReachNlriMultiPathTest() {
        final MpReachNlri mpReachNlri = new MpReachNlriBuilder().setAdvertizedRoutes(
                new AdvertizedRoutesBuilder().setDestinationType(
                        new DestinationIpv4CaseBuilder().setDestinationIpv4(
                                new DestinationIpv4Builder().setIpv4Prefixes(
                                        PREFIXES).build()).build()).build()).build();
        final MpReachNlriBuilder mpReachNlriBuilder = new MpReachNlriBuilder();
        mpReachNlriBuilder.setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class);
        this.parser.parseNlri(Unpooled.wrappedBuffer(MP_NLRI_BYTES), mpReachNlriBuilder, this.constraint);
        mpReachNlriBuilder.setAfi(null).setSafi(null);
        Assert.assertEquals(mpReachNlri, mpReachNlriBuilder.build());

        final AdvertizedRoutesSerializer serializer = new AdvertizedRoutesSerializer();
        final ByteBuf output = Unpooled.buffer(MP_NLRI_BYTES.length);
        final Attributes attributes = new AttributesBuilder().addAugmentation(Attributes1.class,
                new Attributes1Builder().setMpReachNlri(mpReachNlri).build()).build();
        serializer.serializeAttribute(attributes, output);
        Assert.assertArrayEquals(MP_NLRI_BYTES, output.array());
    }

    @Test
    public void parseUnreachNlriMultiPathTest() {
        final MpUnreachNlri mpUnreachNlri = new MpUnreachNlriBuilder().setWithdrawnRoutes(
                new WithdrawnRoutesBuilder().setDestinationType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
                                new DestinationIpv4Builder().setIpv4Prefixes(
                                        PREFIXES).build()).build()).build()).build();
        final MpUnreachNlriBuilder mpUnreachNlriBuilder = new MpUnreachNlriBuilder();
        mpUnreachNlriBuilder.setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class);
        this.parser.parseNlri(Unpooled.wrappedBuffer(MP_NLRI_BYTES), mpUnreachNlriBuilder, this.constraint);
        mpUnreachNlriBuilder.setAfi(null).setSafi(null);
        Assert.assertEquals(mpUnreachNlri, mpUnreachNlriBuilder.build());

        final WithdrawnRoutesSerializer serializer = new WithdrawnRoutesSerializer();
        final ByteBuf output = Unpooled.buffer(MP_NLRI_BYTES.length);
        final Attributes attributes = new AttributesBuilder().addAugmentation(Attributes2.class,
                new Attributes2Builder().setMpUnreachNlri(mpUnreachNlri).build()).build();
        serializer.serializeAttribute(attributes, output);
        Assert.assertArrayEquals(MP_NLRI_BYTES, output.array());
    }

    private static Ipv4Prefixes createIpv4Prefix(final long pathId, final Ipv4Prefix prefix) {
        return new Ipv4PrefixesBuilder().setPathId(new PathId(pathId)).setPrefix(prefix).build();
    }
}
