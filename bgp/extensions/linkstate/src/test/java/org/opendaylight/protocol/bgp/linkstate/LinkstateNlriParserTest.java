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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.bgp.linkstate.impl.tlvs.OspfRouteTlvParser.OSPF_ROUTE_NID;
import static org.opendaylight.protocol.bgp.linkstate.impl.tlvs.ReachTlvParser.IP_REACH_NID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.impl.BGPActivator;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.LinkNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.NodeNlriParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.Srv6SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6SidInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

public class LinkstateNlriParserTest {

    private static final NodeIdentifier C_LINKSTATE_NID = new NodeIdentifier(CLinkstateDestination.QNAME);
    private static final NodeIdentifier C_ROUTER_ID_NID = new NodeIdentifier(CRouterIdentifier.QNAME);

    private final byte[] nodeNlri = new byte[] {
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x30, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x23, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x48, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28,
        (byte) 0x28, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x29, (byte) 0x29,
        (byte) 0x29, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x39, (byte) 0x05
    };

    private final byte[] linkNlri = new byte[] {
        (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x85, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,

        // local node descriptors
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x2a,
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48,
        (byte) 0x02, (byte) 0x01,
        (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x03,
        (byte) 0x00, (byte) 0x06,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x42,
        2, 4, 0, 4, 1, 1, 1, 1,
        2, 5, 0, 4, 0, 0, 1, 2,
        // remote node descriptors
        (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x28,
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48,
        (byte) 0x02, (byte) 0x01,
        (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x03,
        (byte) 0x00, (byte) 0x04,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40,
        2, 4, 0, 4, 1, 1, 1, 2,
        2, 5, 0, 4, 0, 0, 1, 3,
        // link descriptors
        1, 2, 0, 8, 1, 2, 3, 4, 0x0a,
        0x0b, 0x0c, 0x0d, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0xc5, (byte) 0x14, (byte) 0xa0,
        (byte) 0x2a,
        (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0xc5, (byte) 0x14, (byte) 0xa0, (byte) 0x28,
        1, 7, 0, 2, 0, 3
    };

    private final byte[] prefixNlri = new byte[] {
        (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x39, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x48, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28,
        (byte) 0x28, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x42, (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x00,
        (byte) 0x0F, (byte) 0x01, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x09,
        (byte) 0x00, (byte) 0x03, (byte) 0x10, (byte) 0xFF, (byte) 0xFF
    };

    private final byte[] srv6Nlri = new byte[] {
        (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x64, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x23, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x48, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28,
        (byte) 0x28, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x29, (byte) 0x29,
        (byte) 0x29, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x39, (byte) 0x05,
        // SRv6 SID information TLV
        (byte) 0x02, (byte) 0x06, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03,
        (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        // SRv6 Endpoint Behavior
        (byte) 0x04, (byte) 0xe2, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01,
        // SRv6 BGP Peer Node SID
        (byte) 0x04, (byte) 0xe3, (byte) 0x00, (byte) 0x0c, (byte) 0x60, (byte) 0x0a, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50, (byte) 0x2a, (byte) 0x2a, (byte) 0x2a, (byte) 0x2a,
        // SRv5 SID Structure
        (byte) 0x04, (byte) 0xe4, (byte) 0x00, (byte) 0x04, (byte) 0x08, (byte) 0x08, (byte) 0x04, (byte) 0x04
    };

    private CLinkstateDestination dest;
    private SimpleNlriTypeRegistry registry;

    private void setUp(final byte[] data) throws BGPParsingException {
        final LinkstateNlriParser parser = new LinkstateNlriParser();
        final MpReachNlriBuilder builder = new MpReachNlriBuilder();
        registry = SimpleNlriTypeRegistry.getInstance();
        final BGPActivator act = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        act.start(context);

        parser.parseNlri(Unpooled.copiedBuffer(data), builder, null);

        final DestinationLinkstate ls = ((DestinationLinkstateCase) builder.getAdvertizedRoutes().getDestinationType())
                .getDestinationLinkstate();
        assertEquals(1, ls.nonnullCLinkstateDestination().size());

        dest = ls.nonnullCLinkstateDestination().get(0);
    }

    @Test
    public void testNodeNlri() throws BGPParsingException {
        setUp(nodeNlri);

        // test BA form
        assertNull(dest.getRouteDistinguisher());
        assertEquals(ProtocolId.IsisLevel2, dest.getProtocolId());
        assertEquals(Uint64.ONE, dest.getIdentifier().getValue());
        final NodeCase nCase = (NodeCase) dest.getObjectType();

        final NodeDescriptors nodeD = nCase.getNodeDescriptors();
        assertEquals(new AsNumber(Uint32.valueOf(72)), nodeD.getAsNumber());
        assertEquals(new DomainIdentifier(Uint32.valueOf(0x28282828L)), nodeD.getDomainId());
        assertEquals(new IsisPseudonodeCaseBuilder().setIsisPseudonode(
            new IsisPseudonodeBuilder().setPsn(Uint8.valueOf(5)).setIsIsRouterIdentifier(
                new IsIsRouterIdentifierBuilder().setIsoSystemId(
                    new IsoSystemIdentifier(new byte[] { 0, 0, 0, 0, 0, (byte) 0x39 })).build()).build()).build(),
            nodeD.getCRouterIdentifier());

        final ByteBuf buffer = Unpooled.buffer();
        registry.serializeNlriType(dest, buffer);
        assertArrayEquals(nodeNlri, ByteArray.readAllBytes(buffer));

        // test BI form
        assertEquals(dest, LinkstateNlriParser.extractLinkstateDestination(ImmutableNodes.newUnkeyedListEntryBuilder()
            .withNodeIdentifier(C_LINKSTATE_NID)
            .withChild(ImmutableNodes.leafNode(LinkstateNlriParser.PROTOCOL_ID_NID, "isis-level2"))
            .withChild(ImmutableNodes.leafNode(LinkstateNlriParser.IDENTIFIER_NID, Uint64.ONE))
            .withChild(ImmutableNodes.newChoiceBuilder()
                .withNodeIdentifier(LinkstateNlriParser.OBJECT_TYPE_NID)
                .withChild(ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(LinkstateNlriParser.NODE_DESCRIPTORS_NID)
                    .withChild(ImmutableNodes.leafNode(NodeNlriParser.AS_NUMBER_NID, Uint32.valueOf(72)))
                    .withChild(ImmutableNodes.leafNode(NodeNlriParser.AREA_NID, Uint32.valueOf(2697513L)))
                    .withChild(ImmutableNodes.leafNode(NodeNlriParser.DOMAIN_NID, Uint32.valueOf(0x28282828L)))
                    .withChild(ImmutableNodes.newChoiceBuilder()
                        .withNodeIdentifier(C_ROUTER_ID_NID)
                        .withChild(ImmutableNodes.newContainerBuilder()
                            .withNodeIdentifier(NodeNlriParser.ISIS_PSEUDONODE_NID)
                            .withChild(ImmutableNodes.newContainerBuilder()
                                .withNodeIdentifier(NodeNlriParser.ISIS_ROUTER_NID)
                                .withChild(ImmutableNodes.leafNode(NodeNlriParser.ISO_SYSTEM_NID,
                                    new byte[] { 0, 0, 0, 0, 0, (byte) 0x39 }))
                                .build())
                            .withChild(ImmutableNodes.leafNode(NodeNlriParser.PSN_NID, Uint8.valueOf(5)))
                            .build())
                        .build())
                    .build())
                .build())
            .build()));
    }

    @Test
    public void testLinkNlri() throws BGPParsingException {
        setUp(linkNlri);

        // test BA form
        assertNull(dest.getRouteDistinguisher());
        assertEquals(ProtocolId.IsisLevel2, dest.getProtocolId());
        assertEquals(Uint64.ONE, dest.getIdentifier().getValue());

        final LinkCase lCase = (LinkCase) dest.getObjectType();

        final LocalNodeDescriptors local = lCase.getLocalNodeDescriptors();
        assertEquals(new AsNumber(Uint32.valueOf(72)), local.getAsNumber());
        assertEquals(new DomainIdentifier(Uint32.valueOf(0x28282828L)), local.getDomainId());
        assertEquals(
            new IsisNodeCaseBuilder().setIsisNode(
                new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(new byte[] {
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x42 })).build()).build(),
            local.getCRouterIdentifier());
        assertEquals("1.1.1.1", local.getBgpRouterId().getValue());
        assertEquals(new AsNumber(Uint32.valueOf(258)), local.getMemberAsn());

        final RemoteNodeDescriptors remote = lCase.getRemoteNodeDescriptors();
        assertEquals(new AsNumber(Uint32.valueOf(72)), remote.getAsNumber());
        assertEquals(new DomainIdentifier(Uint32.valueOf(0x28282828L)), remote.getDomainId());
        assertEquals(new OspfNodeCaseBuilder().setOspfNode(new OspfNodeBuilder()
            .setOspfRouterId(Uint32.valueOf(0x00000040L)).build())
            .build(), remote.getCRouterIdentifier());
        assertEquals(new AsNumber(Uint32.valueOf(259)), remote.getMemberAsn());
        assertEquals("1.1.1.2", remote.getBgpRouterId().getValue());

        final LinkDescriptors ld = lCase.getLinkDescriptors();
        assertEquals("197.20.160.42", ld.getIpv4InterfaceAddress().getValue());
        assertEquals("197.20.160.40", ld.getIpv4NeighborAddress().getValue());

        final ByteBuf buffer = Unpooled.buffer();
        registry.serializeNlriType(dest, buffer);
        assertArrayEquals(linkNlri, ByteArray.readAllBytes(buffer));

        // test BI form
        final var asNumber = ImmutableNodes.leafNode(NodeNlriParser.AS_NUMBER_NID, Uint32.valueOf(72));
        final var domainID = ImmutableNodes.leafNode(NodeNlriParser.DOMAIN_NID, Uint32.valueOf(0x28282828L));

        assertEquals(dest, LinkstateNlriParser.extractLinkstateDestination(ImmutableNodes.newUnkeyedListEntryBuilder()
            .withNodeIdentifier(C_LINKSTATE_NID)
            .withChild(ImmutableNodes.leafNode(LinkstateNlriParser.PROTOCOL_ID_NID, "isis-level2"))
            .withChild(ImmutableNodes.leafNode(LinkstateNlriParser.IDENTIFIER_NID, Uint64.ONE))
            .withChild(ImmutableNodes.newChoiceBuilder()
                .withNodeIdentifier(LinkstateNlriParser.OBJECT_TYPE_NID)
                // local node descriptors
                .withChild(ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID)
                    .withChild(asNumber)
                    .withChild(domainID)
                    .withChild(ImmutableNodes.newChoiceBuilder()
                        .withNodeIdentifier(C_ROUTER_ID_NID)
                        .withChild(ImmutableNodes.newContainerBuilder()
                            .withNodeIdentifier(NodeNlriParser.ISIS_NODE_NID)
                            .withChild(ImmutableNodes.leafNode(NodeNlriParser.ISO_SYSTEM_NID,
                                new byte[] { 0, 0, 0, 0, 0, (byte) 0x42 }))
                            .build())
                        .build())
                    .withChild(ImmutableNodes.leafNode(NodeNlriParser.BGP_ROUTER_NID, "1.1.1.1"))
                    .withChild(ImmutableNodes.leafNode(NodeNlriParser.MEMBER_ASN_NID, Uint32.valueOf(258L)))
                    .build())
                // remote descriptors
                .withChild(ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID)
                    .withChild(asNumber)
                    .withChild(domainID)
                    .withChild(ImmutableNodes.newChoiceBuilder()
                        .withNodeIdentifier(C_ROUTER_ID_NID)
                        .withChild(ImmutableNodes.newContainerBuilder()
                            .withNodeIdentifier(NodeNlriParser.OSPF_NODE_NID)
                            .withChild(
                                ImmutableNodes.leafNode(NodeNlriParser.OSPF_ROUTER_NID, Uint32.valueOf(0x00000040L)))
                            .build())
                        .build())
                    .withChild(ImmutableNodes.leafNode(NodeNlriParser.BGP_ROUTER_NID, "1.1.1.2"))
                    .withChild(ImmutableNodes.leafNode(NodeNlriParser.MEMBER_ASN_NID, Uint32.valueOf(259)))
                    .build())
                // link descriptors
                .withChild(ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(LinkstateNlriParser.LINK_DESCRIPTORS_NID)
                    .withChild(ImmutableNodes.leafNode(LinkNlriParser.LINK_LOCAL_NID, Uint32.valueOf(16909060L)))
                    .withChild(ImmutableNodes.leafNode(LinkNlriParser.LINK_REMOTE_NID, Uint32.valueOf(168496141L)))
                    .withChild(ImmutableNodes.leafNode(LinkNlriParser.IPV4_IFACE_NID, "197.20.160.42"))
                    .withChild(ImmutableNodes.leafNode(LinkNlriParser.IPV4_NEIGHBOR_NID, "197.20.160.40"))
                    .withChild(ImmutableNodes.leafNode(TlvUtil.MULTI_TOPOLOGY_NID, Uint16.valueOf(3)))
                    .build())
                .build())
            .build()));
    }

    @Test
    public void testPrefixNlri() throws BGPParsingException {
        setUp(prefixNlri);

        // test BA form
        assertNull(dest.getRouteDistinguisher());
        assertEquals(ProtocolId.IsisLevel2, dest.getProtocolId());
        assertEquals(Uint64.ONE, dest.getIdentifier().getValue());

        final PrefixCase pCase = (PrefixCase) dest.getObjectType();

        final AdvertisingNodeDescriptors local = pCase.getAdvertisingNodeDescriptors();
        assertEquals(new AsNumber(Uint32.valueOf(72)), local.getAsNumber());
        assertEquals(new DomainIdentifier(Uint32.valueOf(0x28282828L)), local.getDomainId());
        assertEquals(
            new IsisNodeCaseBuilder().setIsisNode(new IsisNodeBuilder().setIsoSystemId(
                new IsoSystemIdentifier(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x42 })).build()).build(), local.getCRouterIdentifier());

        final PrefixDescriptors pd = pCase.getPrefixDescriptors();
        assertEquals(OspfRouteType.External1, pd.getOspfRouteType());
        assertEquals(new TopologyIdentifier(Uint16.valueOf(15)), pd.getMultiTopologyId());
        assertEquals(new Ipv4Prefix("255.255.0.0/16"), pd.getIpReachabilityInformation().getIpv4Prefix());

        final ByteBuf buffer = Unpooled.buffer();
        registry.serializeNlriType(dest, buffer);
        assertArrayEquals(prefixNlri, ByteArray.readAllBytes(buffer));

        // test BI form
        final var asNumber = ImmutableNodes.leafNode(NodeNlriParser.AS_NUMBER_NID, Uint32.valueOf(72));
        final var domainID = ImmutableNodes.leafNode(NodeNlriParser.DOMAIN_NID, Uint32.valueOf(673720360L));

        assertEquals(dest, LinkstateNlriParser.extractLinkstateDestination(ImmutableNodes.newUnkeyedListEntryBuilder()
            .withNodeIdentifier(C_LINKSTATE_NID)
            .withChild(ImmutableNodes.leafNode(LinkstateNlriParser.PROTOCOL_ID_NID, "isis-level2"))
            .withChild(ImmutableNodes.leafNode(LinkstateNlriParser.IDENTIFIER_NID, Uint64.ONE))
            .withChild(ImmutableNodes.newChoiceBuilder()
                .withNodeIdentifier(LinkstateNlriParser.OBJECT_TYPE_NID)
                // advertising node descriptors
                .withChild(ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(LinkstateNlriParser.ADVERTISING_NODE_DESCRIPTORS_NID)
                    .withChild(asNumber)
                    .withChild(domainID)
                    .withChild(ImmutableNodes.newChoiceBuilder()
                        .withNodeIdentifier(C_ROUTER_ID_NID)
                        .withChild(ImmutableNodes.newContainerBuilder()
                            .withNodeIdentifier(NodeNlriParser.ISIS_NODE_NID)
                            .withChild(ImmutableNodes.leafNode(NodeNlriParser.ISO_SYSTEM_NID,
                                new byte[] { 0, 0, 0, 0, 0, (byte) 0x42 }))
                            .build())
                        .build())
                    .build())
                // prefix descriptors
                .withChild(ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(LinkstateNlriParser.PREFIX_DESCRIPTORS_NID)
                    .withChild(asNumber)
                    .withChild(domainID)
                    .withChild(ImmutableNodes.leafNode(TlvUtil.MULTI_TOPOLOGY_NID, Uint16.valueOf(15)))
                    .withChild(ImmutableNodes.leafNode(IP_REACH_NID, "255.255.0.0/16"))
                    .withChild(ImmutableNodes.leafNode(OSPF_ROUTE_NID, "external1"))
                    .build())
                .build())
            .build()));
    }

    @Test
    public void testSrv6Nlri() throws BGPParsingException {
        setUp(srv6Nlri);

        assertNull(dest.getRouteDistinguisher());
        assertEquals(ProtocolId.IsisLevel2, dest.getProtocolId());
        assertEquals(Uint64.ONE, dest.getIdentifier().getValue());
        final Srv6SidCase srv6Case = (Srv6SidCase) dest.getObjectType();

        final Srv6NodeDescriptors nodeD = srv6Case.getSrv6NodeDescriptors();
        assertEquals(new AsNumber(Uint32.valueOf(72)), nodeD.getAsNumber());
        assertEquals(new DomainIdentifier(Uint32.valueOf(0x28282828L)), nodeD.getDomainId());
        assertEquals(new IsisPseudonodeCaseBuilder().setIsisPseudonode(
            new IsisPseudonodeBuilder().setPsn(Uint8.valueOf(5)).setIsIsRouterIdentifier(
                new IsIsRouterIdentifierBuilder().setIsoSystemId(
                    new IsoSystemIdentifier(new byte[] { 0, 0, 0, 0, 0, (byte) 0x39 })).build()).build()).build(),
            nodeD.getCRouterIdentifier());

        final Srv6SidInformation sidInformation = srv6Case.getSrv6SidInformation();
        assertArrayEquals(new byte [] {0, 1, 2, 3, 4, 5, 6, 7, 0, 0, 0, 0, 0, 0, 0, 1},
            sidInformation.getSrv6SidTlv().getValue());

        final Srv6Attributes sidAttr = srv6Case.getSrv6Attributes();
        assertEquals(1, sidAttr.getSrv6EndpointBehavior().getEndpointBehavior().intValue());
        assertEquals(1, sidAttr.getSrv6EndpointBehavior().getAlgo().intValue());
        assertFalse(sidAttr.getSrv6BgpPeerNode().getFlags().getBackup());
        assertTrue(sidAttr.getSrv6BgpPeerNode().getFlags().getSet());
        assertTrue(sidAttr.getSrv6BgpPeerNode().getFlags().getPersistent());
        assertEquals(10, sidAttr.getSrv6BgpPeerNode().getWeight().intValue());
        assertEquals(Uint32.valueOf(80), sidAttr.getSrv6BgpPeerNode().getPeerAsNumber().getValue());
        assertEquals("42.42.42.42", sidAttr.getSrv6BgpPeerNode().getPeerBgpId().getValue());
        assertEquals(8, sidAttr.getSrv6SidStructure().getLocatorBlockLength().intValue());
        assertEquals(8, sidAttr.getSrv6SidStructure().getLocatorNodeLength().intValue());
        assertEquals(4, sidAttr.getSrv6SidStructure().getFunctionLength().intValue());
        assertEquals(4, sidAttr.getSrv6SidStructure().getArgumentLength().intValue());

        final ByteBuf buffer = Unpooled.buffer();
        registry.serializeNlriType(dest, buffer);
        assertArrayEquals(srv6Nlri, ByteArray.readAllBytes(buffer));
    }

    @Test
    public void testSerializeAttribute() throws BGPParsingException {
        final LinkstateNlriParser parser = new LinkstateNlriParser();
        setUp(prefixNlri);
        final DestinationLinkstateCase dlc = new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
            new DestinationLinkstateBuilder().setCLinkstateDestination(List.of(dest)).build()).build();
        final AdvertizedRoutes aroutes = new AdvertizedRoutesBuilder().setDestinationType(dlc).build();

        Attributes pa = new AttributesBuilder()
                .addAugmentation(new AttributesReachBuilder()
                    .setMpReachNlri(new MpReachNlriBuilder().setAdvertizedRoutes(aroutes).build())
                    .build())
                .build();

        ByteBuf result = Unpooled.buffer();
        parser.serializeAttribute(pa, result);
        assertArrayEquals(prefixNlri, ByteArray.getAllBytes(result));

        setUp(nodeNlri);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase dlcU =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder()
                    .setDestinationLinkstate(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                        .bgp.linkstate.rev241219.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                        .destination.linkstate._case.DestinationLinkstateBuilder()
                            .setCLinkstateDestination(List.of(dest))
                        .build()).build();
        final WithdrawnRoutes wroutes = new WithdrawnRoutesBuilder().setDestinationType(dlcU).build();

        pa = new AttributesBuilder()
                .addAugmentation(new AttributesUnreachBuilder()
                    .setMpUnreachNlri(new MpUnreachNlriBuilder().setWithdrawnRoutes(wroutes).build())
                    .build())
                .build();

        result = Unpooled.buffer();
        parser.serializeAttribute(pa, result);
        assertArrayEquals(nodeNlri, ByteArray.getAllBytes(result));
    }
}
