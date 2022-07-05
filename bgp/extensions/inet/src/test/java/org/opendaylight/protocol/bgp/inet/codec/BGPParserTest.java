/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet.codec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandlingSupport;
import org.opendaylight.protocol.bgp.parser.spi.pojo.PeerSpecificParserConstraintImpl;
import org.opendaylight.protocol.bgp.parser.spi.pojo.RevisedErrorHandlingSupportImpl;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class BGPParserTest {

    private static final int MAX_SIZE = 300;

    private static MessageRegistry messageRegistry;

    private static List<byte[]> input;

    private static int MESSAGE_COUNT = 2;

    @BeforeClass
    public static void setUp() throws Exception {
        messageRegistry = ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst().orElseThrow()
            .getMessageRegistry();
        input = new ArrayList<>(MESSAGE_COUNT);
        for (int i = 1; i <= MESSAGE_COUNT; i++) {

            final String name = "/up" + i + ".bin";
            try (InputStream is = BGPParserTest.class.getResourceAsStream(name)) {
                if (is == null) {
                    throw new IOException("Failed to get resource " + name);
                }
                final ByteArrayOutputStream bis = new ByteArrayOutputStream();
                final byte[] data = new byte[MAX_SIZE];
                int position;
                while ((position = is.read(data, 0, data.length)) != -1) {
                    bis.write(data, 0, position);
                }
                bis.flush();

                input.add(bis.toByteArray());
                is.close();
            }
        }
    }

    /*
     * Tests IPv6 NEXT_HOP, NLRI, ORIGIN.IGP, MULTI_EXIT_DISC, ORIGINATOR-ID, CLUSTER_LIST.
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 80 <- length (128) - including header
     * 02 <- message type
     * 00 00 <- withdrawn routes length
     * 00 69 <- total path attribute length (105)
     * 40 <- attribute flags
     * 01 <- attribute type code (origin)
     * 01 <- attribute length
     * 00 <- Origin value (IGP)
     * 40 <- attribute flags
     * 02 <- attribute type code (as path)
     * 06 <- attribute length
     * 02 <- AS_SEQUENCE
     * 01 <- path segment count
     * 00 00 fd e9 <- path segment value (65001)
     * 40 <- attribute flags
     * 03 <- attribute type code (next hop)
     * 04 <- attribute length
     * 0a 00 00 00 <- next hop value (10.0.0.0)
     * 80 <- attribute flags
     * 04 <- attribute type code (multi exit disc)
     * 04 <- attribute length
     * 00 00 00 00 <- value
     * 80 <- attribute flags
     * 09 <- attribute type code (originator id)
     * 04 <- attribute length
     * 7f 00 00 01 <- value (localhost ip)
     * 80 <- attribute flags
     * 0a <- attribute type code (cluster list)
     * 08 <- attribute length
     * 01 02 03 04 <- value
     * 05 06 07 08 <- value
     * 80 <- attribute flags
     * 0e <- attribute type code (mp reach nlri)
     * 40 <- attribute length
     * 00 02 <- AFI (Ipv6)
     * 01 <- SAFI (Unicast)
     * 20 <- length of next hop
     * 20 01 0d b8 00 00 00 00 00 00 00 00 00 00 00 01 <- global
     * fe 80 00 00 00 00 00 00 c0 01 0b ff fe 7e 00 <- link local
     * 00 <- reserved
     *
     * //NLRI
     * 40 20 01 0d b8 00 01 00 02 <- IPv6 Prefix (2001:db8:1:2:: / 64)
     * 40 20 01 0d b8 00 01 00 01 <- IPv6 Prefix (2001:db8:1:1:: / 64)
     * 40 20 01 0d b8 00 01 00 00 <- IPv6 Prefix (2001:db8:1:: / 64)
     *
     */
    @Test
    public void testIPv6Nlri() throws Exception {
        final Update message = (Update) messageRegistry.parseMessage(Unpooled.wrappedBuffer(input.get(1)), null);

        // check fields
        assertNull(message.getWithdrawnRoutes());

        final UpdateBuilder builder = new UpdateBuilder();

        // check NLRI

        final List<Ipv6Prefixes> prefs = new ArrayList<>();
        prefs.add(new Ipv6PrefixesBuilder().setPrefix(new Ipv6Prefix("2001:db8:1:2::/64")).build());
        prefs.add(new Ipv6PrefixesBuilder().setPrefix(new Ipv6Prefix("2001:db8:1:1::/64")).build());
        prefs.add(new Ipv6PrefixesBuilder().setPrefix(new Ipv6Prefix("2001:db8:1::/64")).build());

        assertNull(message.getNlri());

        // attributes
        final List<AsNumber> asNumbers = new ArrayList<>();
        asNumbers.add(new AsNumber(Uint32.valueOf(65001)));
        final List<Segments> asPath = new ArrayList<>();
        asPath.add(new SegmentsBuilder().setAsSequence(asNumbers).build());

        final Ipv6NextHopCase nextHop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(
                new Ipv6NextHopBuilder().setGlobal(new Ipv6AddressNoZone("2001:db8::1"))
                        .setLinkLocal(new Ipv6AddressNoZone("fe80::c001:bff:fe7e:0")).build()).build();

        final List<ClusterIdentifier> clusters = Lists.newArrayList(
            new ClusterIdentifier(new Ipv4AddressNoZone("1.2.3.4")),
                new ClusterIdentifier(new Ipv4AddressNoZone("5.6.7.8")));

        // check path attributes

        final Attributes attrs = message.getAttributes();

        final AttributesBuilder paBuilder = new AttributesBuilder();

        paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build());
        assertEquals(paBuilder.getOrigin(), attrs.getOrigin());

        paBuilder.setAsPath(new AsPathBuilder().setSegments(asPath).build());
        assertEquals(paBuilder.getAsPath(), attrs.getAsPath());

        paBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(Uint32.ZERO).build());
        assertEquals(paBuilder.getMultiExitDisc(), attrs.getMultiExitDisc());

        paBuilder.setOriginatorId(new OriginatorIdBuilder().setOriginator(new Ipv4AddressNoZone("127.0.0.1")).build());
        assertEquals(paBuilder.getOriginatorId(), attrs.getOriginatorId());

        paBuilder.setClusterId(new ClusterIdBuilder().setCluster(clusters).build());
        assertEquals(paBuilder.getClusterId(), attrs.getClusterId());

        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder()
                .setAfi(Ipv6AddressFamily.VALUE)
                .setSafi(UnicastSubsequentAddressFamily.VALUE)
                .setCNextHop(nextHop)
                .setAdvertizedRoutes(new AdvertizedRoutesBuilder()
                    .setDestinationType(new DestinationIpv6CaseBuilder()
                        .setDestinationIpv6(new DestinationIpv6Builder().setIpv6Prefixes(prefs).build())
                        .build())
                    .build());

        paBuilder.addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpBuilder.build()).build());
        assertEquals(paBuilder.augmentation(AttributesReach.class).getMpReachNlri(),
                attrs.augmentation(AttributesReach.class).getMpReachNlri());
        paBuilder.setUnrecognizedAttributes(Collections.emptyMap());
        // check API message

        builder.setAttributes(paBuilder.build());
        assertEquals(builder.build(), message);

        final ByteBuf buffer = Unpooled.buffer();
        messageRegistry.serializeMessage(message, buffer);
        assertArrayEquals(input.get(1), ByteArray.readAllBytes(buffer));
    }

    /*
     * Tests withdrawn routes with malformed attribute.
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 36 <- length (54) - including header
     * 02 <- message type
     * 00 00 <- withdrawn routes length
     * 00 1b <- total path attribute length (27)
     * 40 <- attribute flags
     * 01 <- attribute type code (origin)
     * 01 <- WRONG attribute length
     * 00 <- Origin value (IGP)
     * 40 <- attribute flags
     * 03 <- attribute type code (Next Hop)
     * 04 <- attribute length
     * 0a 00 00 02 <- value (10.0.0.2)
     * 40 <- attribute flags
     * 0e <- attribute type code (MP_REACH)
     * 0d <- attribute length
     * 00 01 <- AFI (Ipv4)
     * 01 <- SAFI (Unicast)
     * 04 <- next hop length
     * ff ff ff ff <- next hop
     * 00 <- reserved
     * 18 <- length
     * 0a 00 01 <- prefix (10.0.1.0)
     * //NLRI
     * 18 <- length
     * 0a 00 02 <- prefix (10.0.2.0)
     */
    @Test
    public void testParseUpdateMessageWithMalformedAttributes() throws Exception {
        final PeerSpecificParserConstraintImpl constraint = new PeerSpecificParserConstraintImpl();
        constraint.addPeerConstraint(RevisedErrorHandlingSupport.class,
                RevisedErrorHandlingSupportImpl.forExternalPeer());
        final Update message = (Update) messageRegistry.parseMessage(Unpooled.wrappedBuffer(input.get(0)), constraint);
        assertNotNull(message);
        assertNull(message.getNlri());
        final List<WithdrawnRoutes> withdrawnRoutes = message.getWithdrawnRoutes();
        assertNotNull(withdrawnRoutes);
        assertEquals(1, withdrawnRoutes.size());
        final Attributes attributes = message.getAttributes();
        assertNotNull(attributes);
        assertNull(attributes.augmentation(AttributesReach.class));
        final AttributesUnreach AttributesUnreach = attributes.augmentation(AttributesUnreach.class);
        assertNotNull(AttributesUnreach);
        final MpUnreachNlri mpUnreachNlri = AttributesUnreach.getMpUnreachNlri();
        assertNotNull(mpUnreachNlri);
    }
}
