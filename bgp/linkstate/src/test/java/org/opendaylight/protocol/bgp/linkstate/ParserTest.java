/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.LinkAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Metric;
import org.opendaylight.yangtools.yang.binding.Notification;

public class ParserTest {

    /**
     * Used by other tests as well
     */
    private static final List<byte[]> inputBytes = new ArrayList<>();

    private static final int COUNTER = 4;

    private static final int MAX_SIZE = 300;

    private static BGPUpdateMessageParser updateParser;

    private static final int LENGTH_FIELD_LENGTH = 2;

    @Before
    public void setUp() throws Exception {
        updateParser = new BGPUpdateMessageParser(ServiceLoaderBGPExtensionProviderContext
            .getSingletonInstance().getAttributeRegistry());
        for (int i = 1; i <= COUNTER; i++) {
            final String name = "/up" + i + ".bin";
            try (final InputStream is = ParserTest.class.getResourceAsStream(name)){
                if (is == null) {
                    throw new IOException("Failed to get resource " + name);
                }
                final ByteArrayOutputStream bis = new ByteArrayOutputStream();
                final byte[] data = new byte[MAX_SIZE];
                int nRead;
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    bis.write(data, 0, nRead);
                }
                bis.flush();

                inputBytes.add(bis.toByteArray());
                is.close();
            }
        }
    }


    @Test
    public void testResource() {
        assertNotNull(inputBytes);
    }

    /*
     * End of Rib for LS consists of empty MP_UNREACH_NLRI, with AFI 16388 and SAFI 71
     *
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 1d <- length (29) - including header
     * 02 <- message type
     * 00 00 <- withdrawn routes length
     * 00 06 <- total path attribute length
     * 80 <- attribute flags
     * 0f <- attribute type (15 - MP_UNREACH_NLRI)
     * 03 <- attribute length
     * 40 04 <- value (AFI 16388: LS)
     * 47 <- value (SAFI 71)
     */
    @Test
    public void testEORLS() throws Exception {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(0), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(0),
            MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = ParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        final Class<? extends AddressFamily> afi = message.getAttributes()
            .getAugmentation(Attributes2.class).getMpUnreachNlri().getAfi();
        final Class<? extends SubsequentAddressFamily> safi = message.getAttributes()
            .getAugmentation(Attributes2.class).getMpUnreachNlri().getSafi();

        assertEquals(LinkstateAddressFamily.class, afi);
        assertEquals(LinkstateSubsequentAddressFamily.class, safi);

        final ByteBuf buffer = Unpooled.buffer();
        ParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(0), ByteArray.readAllBytes(buffer));
    }

    /*
     * Tests BGP Link Ipv4
     *
     * 00 00 <- withdrawn routes length
     * 01 48 <- total path attribute length (328)
     * 90 <- attribute flags
        0e <- attribute type code (MP reach)
        01 2c <- attribute extended length (300)
        40 04 <- AFI (16388 - Linkstate)
        47 <- SAFI (71 - Linkstate)
        04 <- next hop length
        19 19 19 01 <- nexthop (25.25.25.1)
        00 <- reserved

        00 02 <- NLRI type (2 - linkNLRI)
        00 5d <- NLRI length (93)
        03 <- ProtocolID - OSPF
        00 00 00 00 00 00 00 01 <- identifier

        01 00 <- local node descriptor type (256)
        00 24 <- length (36)
        02 00 <- node descriptor type (member AS - 512)
        00 04 <- length
        00 00 00 64 <- value (100)
        02 01 <- node descriptor type (bgpId - 513)
        00 04 <- length
        19 19 19 01 <- bgpId (25.25.25.1)
        02 02 <- node descriptor type (areaId - 514)
        00 04 <- length
        00 00 00 00 <- value
        02 03 <- node descriptor type (routeId - 515)
        00 08 <- length
        03 03 03 04 0b 0b 0b 03 <- OSPF Router Id

        01 01 <- remote node descriptor type (257)
        00 20 <- length (32)
        02 00 <- node descriptor type (member AS - 512)
        00 04 <- length
        00 00 00 64 <- value (100)
        02 01 <- node descriptor type (bgpId - 513)
        00 04 <- length
        19 19 19 01 <- bgpId (25.25.25.1)
        02 02 <- node descriptor type (areaId - 514)
        00 04 <- length
        00 00 00 00 <- value
        02 03 <- node descriptor type (routeId - 515)
        00 04 <- length
        03 03 03 04 <- OSPF Router Id

        01 03 <- link descriptor type (IPv4 interface address - 259)
        00 04 <- length (4)
        0b 0b 0b 03 <- value (11.11.11.3)

        00 02 <- NLRI type (2 - linkNLRI)
        00 5d <- NLRI length (93)
        03 <- ProtocolID - OSPF
        00 00 00 00 00 00 00 01 <- identifier

        01 00 <- local node descriptor type (256)
        00 24 <- length (36)
        02 00 <- node descriptor type (member AS - 512)
        00 04 <- length
        00 00 00 64 <- value (100)
        02 01 <- node descriptor type (bgpId - 513)
        00 04 <- length
        19 19 19 01 <- bgpId (25.25.25.1)
        02 02 <- node descriptor type (areaId - 514)
        00 04 <- length
        00 00 00 00 <- value
        02 03 <- node descriptor type (routeId - 515)
        00 08 <- length
        03 03 03 04 0b 0b 0b 03 <- OSPF Router Id

        01 01 <- remote node descriptor type (257)
        00 20 <- length (32)
        02 00 <- node descriptor type (member AS - 512)
        00 04 <- length
        00 00 00 64 <- value (100)
        02 01 <- node descriptor type (bgpId - 513)
        00 04 <- length
        19 19 19 01 <- bgpId (25.25.25.1)
        02 02 <- node descriptor type (areaId - 514)
        00 04 <- length
        00 00 00 00 <- value
        02 03 <- node descriptor type (routeId - 515)
        00 04 <- length
        01 01 01 02 <- OSPF Router Id

        01 03 <- link descriptor type (IPv4 interface address - 259)
        00 04 <- length
        0b 0b 0b 01 <- value (11.11.11.1)

        00 02 <- NLRI type (2 - linkNLRI)
        00 5d <- NLRI length (93)
        03 <- ProtocolID - OSPF
        00 00 00 00 00 00 00 01 <- identifier

        01 00 <- local node descriptor type (256)
        00 20 <- length (32)
        02 00 <- node descriptor type (member AS - 512)
        00 04 <- length
        00 00 00 64 <- value (100)
        02 01 <- node descriptor type (bgpId - 513)
        00 04 <- length
        19 19 19 01 <- bgpId (25.25.25.1)
        02 02 <- node descriptor type (areaId - 514)
        00 04 <- length
        00 00 00 00 <- value
        02 03 <- node descriptor type (routeId - 515)
        00 04 <- length
        01 01 01 02 <- OSPF Router Id

        01 01 <- remote node descriptor type (257)
        00 24 <- length (36)
        02 00 <- node descriptor type (member AS - 512)
        00 04 <- length
        00 00 00 64 <- value (100)
        02 01 <- node descriptor type (bgpId - 513)
        00 04 <- length
        19 19 19 01 <- bgpId (25.25.25.1)
        02 02 <- node descriptor type (areaId - 514)
        00 04 <- length
        00 00 00 00 <- value
        02 03 <- node descriptor type (routeId - 515)
        00 08 <- length
        03 03 03 04 0b 0b 0b 03 <- OSPF Router Id

        01 03 <- link descriptor type (IPv4 interface address - 259)
        00 04 <- length
        0b 0b 0b 01 <- value (11.11.11.1)

        40 <- attribute flags
        01 <- attribute type (Origin)
        01 <- attribute length
        00 <- value (IGP)
        40 <- attribute flags
        02 <- attribute type (AS Path)
        00 <- length
        40 <- attribute flags
        05 <- attribute type (local pref)
        04 <- length
        00 00 00 64 <- value
        c0 <- attribute flags
        1D <- attribute type (Link STATE - 29)
        07 <- length
        04 47 <- link attribute (1095 - Metric)
        00 03 <- length
        00 00 01 <- value
     */
    @Test
    public void testBGPLink() throws Exception {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(1), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(1),
            MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = ParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        final UpdateBuilder builder = new UpdateBuilder();

        // check fields

        assertNull(message.getWithdrawnRoutes());

        final Ipv4NextHopCase nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(
            new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("25.25.25.1")).build()).build();

        final LocalNodeDescriptorsBuilder ndBuilder = new LocalNodeDescriptorsBuilder()
            .setAsNumber(new AsNumber((long) 100)).setDomainId(
            new DomainIdentifier(0x19191901L)).setAreaId(new AreaIdentifier(0L));

        final RemoteNodeDescriptorsBuilder rdBuilder = new RemoteNodeDescriptorsBuilder()
            .setAsNumber(new AsNumber((long) 100)).setDomainId(
            new DomainIdentifier(0x19191901L)).setAreaId(new AreaIdentifier(0L));

        final CLinkstateDestinationBuilder clBuilder = new CLinkstateDestinationBuilder();
        clBuilder.setIdentifier(new Identifier(BigInteger.ONE));
        clBuilder.setProtocolId(ProtocolId.Ospf);

        final Attributes1Builder lsBuilder = new Attributes1Builder();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(LinkstateAddressFamily.class);
        mpBuilder.setSafi(LinkstateSubsequentAddressFamily.class);
        mpBuilder.setCNextHop(nextHop);

        final List<CLinkstateDestination> linkstates = Lists.newArrayList();
        final LinkCaseBuilder lCase = new LinkCaseBuilder().setLocalNodeDescriptors(ndBuilder.setCRouterIdentifier(
            new OspfPseudonodeCaseBuilder().setOspfPseudonode(
                new OspfPseudonodeBuilder().setOspfRouterId(0x03030304L)
                    .setLanInterface(new OspfInterfaceIdentifier(0x0b0b0b03L)).build()).build()).build());
        lCase.setRemoteNodeDescriptors(rdBuilder.setCRouterIdentifier(
            new OspfNodeCaseBuilder().setOspfNode(new OspfNodeBuilder()
                .setOspfRouterId(0x03030304L).build()).build()).build());
        lCase.setLinkDescriptors(new LinkDescriptorsBuilder().setIpv4InterfaceAddress(
            new Ipv4InterfaceIdentifier(new Ipv4Address("11.11.11.3"))).build());
        linkstates.add(clBuilder.setObjectType(lCase.build()).build());

        lCase.setRemoteNodeDescriptors(rdBuilder.setCRouterIdentifier(
            new OspfNodeCaseBuilder().setOspfNode(new OspfNodeBuilder()
                .setOspfRouterId(0x01010102L).build()).build()).build());
        lCase.setLinkDescriptors(new LinkDescriptorsBuilder().setIpv4InterfaceAddress(
            new Ipv4InterfaceIdentifier(new Ipv4Address("11.11.11.1"))).build());
        linkstates.add(clBuilder.setObjectType(lCase.build()).build());

        lCase.setLocalNodeDescriptors(ndBuilder.setCRouterIdentifier(
            new OspfNodeCaseBuilder().setOspfNode(new OspfNodeBuilder()
                .setOspfRouterId(0x01010102L).build()).build()).build());
        lCase.setRemoteNodeDescriptors(rdBuilder.setCRouterIdentifier(
            new OspfPseudonodeCaseBuilder().setOspfPseudonode(new OspfPseudonodeBuilder()
                .setOspfRouterId(0x03030304L)
                .setLanInterface(new OspfInterfaceIdentifier(0x0b0b0b03L)).build()).build()).build());
        linkstates.add(clBuilder.setObjectType(lCase.build()).build());

        lsBuilder.setMpReachNlri(mpBuilder.build());

        // check path attributes
        final Attributes attrs = message.getAttributes();

        final AttributesBuilder paBuilder = new AttributesBuilder();

        paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build());
        assertEquals(paBuilder.getOrigin(), attrs.getOrigin());

        paBuilder.setAsPath(new AsPathBuilder().setSegments(Collections.emptyList()).build());
        assertEquals(paBuilder.getAsPath(), attrs.getAsPath());

        paBuilder.setLocalPref(new LocalPrefBuilder().setPref(100L).build());
        assertEquals(paBuilder.getLocalPref(), attrs.getLocalPref());

        final MpReachNlri mp = attrs.getAugmentation(Attributes1.class).getMpReachNlri();
        assertEquals(mpBuilder.getAfi(), mp.getAfi());
        assertEquals(mpBuilder.getSafi(), mp.getSafi());
        assertEquals(mpBuilder.getCNextHop(), mp.getCNextHop());

        final DestinationLinkstateBuilder dBuilder = new DestinationLinkstateBuilder();
        dBuilder.setCLinkstateDestination(linkstates);

        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLinkstateCaseBuilder().setDestinationLinkstate(dBuilder.build()).build()).build());
        lsBuilder.setMpReachNlri(mpBuilder.build());

        paBuilder.addAugmentation(Attributes1.class, lsBuilder.build());

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.
            Attributes1Builder lsAttrBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.
            bgp.linkstate.rev171207.Attributes1Builder();

        lsAttrBuilder.setLinkStateAttribute(
            new LinkAttributesCaseBuilder().setLinkAttributes(new LinkAttributesBuilder()
                .setMetric(new Metric(1L)).build()).build());
        paBuilder.addAugmentation(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.Attributes1.class,
            lsAttrBuilder.build());
        paBuilder.setUnrecognizedAttributes(Collections.emptyList());

        assertEquals(
            lsAttrBuilder.build(),
            attrs.getAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                .linkstate.rev171207.Attributes1.class));

        final List<CLinkstateDestination> dests = ((DestinationLinkstateCase) mp.getAdvertizedRoutes()
            .getDestinationType()).getDestinationLinkstate().getCLinkstateDestination();

        assertEquals(linkstates.size(), dests.size());

        assertEquals(linkstates, dests);
        // check API message
        builder.setAttributes(paBuilder.build());
        assertEquals(builder.build(), message);

        final ByteBuf buffer = Unpooled.buffer();
        ParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(1), ByteArray.readAllBytes(buffer));
    }

    /*
     * TEST BGP Node
     *
     *  00 00 <- withdrawn routes length
        00 b1 <- total path attribute length (177)
        80 <- attribute flags
        0e <- attribute type code (MP reach)
        a0 <- attribute length (160)
        40 04 <- AFI (16388 - Linkstate)
        47 <- SAFI (71 - Linkstate)
        04 <- next hop length
        19 19 19 01 - nexthop (25.25.25.1)
        00 <- reserved

        00 01 <- NLRI type (1 - nodeNLRI)
        00 31 <- NLRI length (49)
        03 <- ProtocolID - OSPF
        00 00 00 00 00 00 00 01 <- identifier
        01 00 <- local node descriptor type (256)
        00 24 <- length (36)
        02 00 <- node descriptor type (member AS - 512)
        00 04 <- length
        00 00 00 64 <- value (100)
        02 01 <- node descriptor type (bgpId - 513)
        00 04 <- length
        19 19 19 01 <- bgpId (25.25.25.1)
        02 02 <- node descriptor type (areaId - 514)
        00 04 <- length
        00 00 00 00 <- value
        02 03 <- node descriptor type (routeId - 515)
        00 08 <- length
        03 03 03 04 0b 0b 0b 03 <- OSPF Router Id

        00 01 <- NLRI type (1 - nodeNLRI)
        00 2d <- NLRI length (45)
        03 <- ProtocolID - OSPF
        00 00 00 00 00 00 00 01 <- identifier
        01 00 <- local node descriptor type (256)
        00 20 <- length (32)
        02 00 <- node descriptor type (member AS - 512)
        00 04 <- length
        00 00 00 64 <- value (100)
        02 01 <- node descriptor type (bgpId - 513)
        00 04 <- length
        19 19 19 01 <- bgpId (25.25.25.1)
        02 02 <- node descriptor type (areaId - 514)
        00 04 <- length
        00 00 00 00 <- value
        02 03 <- node descriptor type (routeId - 515)
        00 04 <- length
        03 03 03 04 <- OSPF Router Id

        00 01 <- NLRI type (1 - nodeNLRI)
        00 2d <- NLRI length (45)
        03 <- ProtocolID - OSPF
        00 00 00 00 00 00 00 01 <- identifier
        01 00 <- local node descriptor type (256)
        00 20 <- length (32)
        02 00 <- node descriptor type (member AS - 512)
        00 04 <- length
        00 00 00 64 <- value (100)
        02 01 <- node descriptor type (bgpId - 513)
        00 04 <- length
        19 19 19 01 <- bgpId (25.25.25.1)
        02 02 <- node descriptor type (areaId - 514)
        00 04 <- length
        00 00 00 00 <- value
        02 03 <- node descriptor type (routeId - 515)
        00 04 <- length
        01 01 01 02  <- OSPF Router Id

        40 <- attribute flags
        01 <- attribute type (Origin)
        01 <- attribute length
        00 <- value (IGP)
        40 <- attribute flags
        02 <- attribute type (AS Path)
        00 <- length
        40 <- attribute flags
        05 <- attribute type (local pref)
        04 <- length
        00 00 00 64 <- value
     */
    @Test
    public void testBGPNode() throws Exception {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(2), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray
            .bytesToInt(ByteArray.subByte(inputBytes.get(2), MessageUtil.MARKER_LENGTH, LENGTH_FIELD_LENGTH));
        final Update message = ParserTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);

        final UpdateBuilder builder = new UpdateBuilder();

        // check fields

        assertNull(message.getWithdrawnRoutes());

        // attributes

        final Ipv4NextHopCase nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(
            new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("25.25.25.1")).build()).build();

        final CLinkstateDestinationBuilder clBuilder = new CLinkstateDestinationBuilder();
        clBuilder.setIdentifier(new Identifier(BigInteger.ONE));
        clBuilder.setProtocolId(ProtocolId.Ospf);

        final NodeDescriptorsBuilder n = new NodeDescriptorsBuilder();
        n.setAsNumber(new AsNumber((long) 100)).setDomainId(new DomainIdentifier(0x19191901L))
            .setAreaId(new AreaIdentifier(0L));

        final List<CLinkstateDestination> linkstates = Lists.newArrayList();
        final NodeCaseBuilder nCase = new NodeCaseBuilder();
        nCase.setNodeDescriptors(n.setCRouterIdentifier(
            new OspfPseudonodeCaseBuilder().setOspfPseudonode(
                new OspfPseudonodeBuilder().setOspfRouterId(0x03030304L)
                    .setLanInterface(new OspfInterfaceIdentifier(0x0b0b0b03L)).build()).build()).build());
        linkstates.add(clBuilder.setObjectType(nCase.build()).build());

        nCase.setNodeDescriptors(n.setCRouterIdentifier(
            new OspfNodeCaseBuilder().setOspfNode(new OspfNodeBuilder()
                .setOspfRouterId(0x03030304L).build()).build()).build());
        linkstates.add(clBuilder.setObjectType(nCase.build()).build());

        nCase.setNodeDescriptors(n.setCRouterIdentifier(
            new OspfNodeCaseBuilder().setOspfNode(new OspfNodeBuilder()
                .setOspfRouterId(0x01010102L).build()).build()).build());
        linkstates.add(clBuilder.setObjectType(nCase.build()).build());

        final Attributes1Builder lsBuilder = new Attributes1Builder();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(LinkstateAddressFamily.class);
        mpBuilder.setSafi(LinkstateSubsequentAddressFamily.class);
        mpBuilder.setCNextHop(nextHop);

        final DestinationLinkstateBuilder dBuilder = new DestinationLinkstateBuilder();
        dBuilder.setCLinkstateDestination(linkstates);

        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLinkstateCaseBuilder().setDestinationLinkstate(dBuilder.build()).build()).build());
        lsBuilder.setMpReachNlri(mpBuilder.build());

        // check path attributes
        final Attributes attrs = message.getAttributes();

        final AttributesBuilder paBuilder = new AttributesBuilder();

        paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build());
        assertEquals(paBuilder.getOrigin(), attrs.getOrigin());

        paBuilder.setAsPath(new AsPathBuilder().setSegments(Collections.emptyList()).build());
        assertEquals(paBuilder.getAsPath(), attrs.getAsPath());

        paBuilder.setLocalPref(new LocalPrefBuilder().setPref(100L).build());
        assertEquals(paBuilder.getLocalPref(), attrs.getLocalPref());

        paBuilder.addAugmentation(Attributes1.class, lsBuilder.build());
        paBuilder.setUnrecognizedAttributes(Collections.emptyList());

        final MpReachNlri mp = attrs.getAugmentation(Attributes1.class).getMpReachNlri();
        assertEquals(mpBuilder.getAfi(), mp.getAfi());
        assertEquals(mpBuilder.getSafi(), mp.getSafi());
        assertEquals(mpBuilder.getCNextHop(), mp.getCNextHop());

        final List<CLinkstateDestination> dests = ((DestinationLinkstateCase) mp.getAdvertizedRoutes()
            .getDestinationType()).getDestinationLinkstate().getCLinkstateDestination();

        assertEquals(linkstates.size(), dests.size());

        assertEquals(linkstates, dests);

        // check API message
        builder.setAttributes(paBuilder.build());
        assertEquals(builder.build(), message);

        final ByteBuf buffer = Unpooled.buffer();
        ParserTest.updateParser.serializeMessage(message, buffer);
        assertArrayEquals(inputBytes.get(2), ByteArray.readAllBytes(buffer));
    }

    /*
     * ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff <- marker
     * 00 3d <- length (61) - including header
     * 01 <- message type
     * 04 <- BGP version
     * 00 64 <- My AS Number (AS TRANS in this case)
     * 00 b4 <- Hold Time
     * 00 00 00 00 <- BGP Identifier
     * 20 <- Optional Parameters Length
     * 02 <- opt. param. type (capabilities)
     * 06 <- length
     * 01 <- capability code (MP Extensions for BGP4)
     * 04 <- length
     * 00 01 00 01 <- AFI 1, SAFI 1
     * 02 <- opt. param. type (capabilities)
     * 06 <- length
     * 01 <- capability code (MP Extensions for BGP4)
     * 04 <- length
     * 00 02 00 01 <- AFI 2, SAFI 1
     * 02 <- opt. param. type (capabilities)
     * 06 <- length
     * 01 <- capability code (MP Extensions for BGP4)
     * 04 <- length
     * 40 04 00 47 <- AFI 16388, SAFI 71
     * 02 <- opt. param. type (capabilities)
     * 06 <- length
     * 41 <- capability code (AS4 octet support)
     * 04 <- length
     * 00 00 00 64 <- AS number
     */
    @Test
    public void testOpenMessage() throws Exception {
        final MessageRegistry msgReg = ServiceLoaderBGPExtensionProviderContext
            .getSingletonInstance().getMessageRegistry();
        final Notification o = msgReg.parseMessage(Unpooled.copiedBuffer(inputBytes.get(3)), null);
        final Open open = (Open) o;
        final Set<BgpTableType> types = Sets.newHashSet();
        for (final BgpParameters param : open.getBgpParameters()) {
            for (final OptionalCapabilities optCapa : param.getOptionalCapabilities()) {
                final CParameters cParam = optCapa.getCParameters();
                if(cParam != null && cParam.getAugmentation(CParameters1.class) != null && cParam.getAugmentation
                    (CParameters1.class).getMultiprotocolCapability() != null) {
                    final MultiprotocolCapability mp = cParam
                        .getAugmentation(CParameters1.class).getMultiprotocolCapability();
                    final BgpTableType type = new BgpTableTypeImpl(mp.getAfi(), mp.getSafi());
                    types.add(type);
                }
            }
        }
        final Set<BgpTableType> expected = Sets.newHashSet();
        expected.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        expected.add(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class));
        expected.add(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));
        assertEquals(expected, types);

        final ByteBuf buffer = Unpooled.buffer();
        msgReg.serializeMessage(o, buffer);
        assertArrayEquals(inputBytes.get(3), ByteArray.readAllBytes(buffer));
    }
}
