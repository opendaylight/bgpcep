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
import static org.junit.Assert.assertNull;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.NodeNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.PrefixNlriParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListEntryNodeBuilder;

public class LinkstateNlriParserTest {

    private static final NodeIdentifier C_LINKSTATE_NID = new NodeIdentifier(CLinkstateDestination.QNAME);
    private static final NodeIdentifier C_ROUTER_ID_NID = new NodeIdentifier(CRouterIdentifier.QNAME);

    private final byte[] nodeNlri = new byte[] { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x30, (byte) 0x02, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x23,
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x02, (byte) 0x01,
        (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x04,
        (byte) 0x00, (byte) 0x29, (byte) 0x29, (byte) 0x29, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x39, (byte) 0x05 };

    private final byte[] linkNlri = new byte[] { (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x65, (byte) 0x02, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x1a,
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x02, (byte) 0x01,
        (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x42, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x18,
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x02, (byte) 0x01,
        (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x04,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40, 1, 2, 0, 8, 1, 2, 3, 4, 0x0a, 0x0b, 0x0c, 0x0d,
        (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0xc5, (byte) 0x14,
        (byte) 0xa0, (byte) 0x2a, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0xc5, (byte) 0x14, (byte) 0xa0, (byte) 0x28,
        1, 7, 0, 2, 0, 3};

    private final byte[] prefixNlri = new byte[] { (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x39, (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x02,
        (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x03, (byte) 0x00,
        (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x42, (byte) 0x01, (byte) 0x07, (byte) 0x00,
        (byte) 0x02, (byte) 0x00, (byte) 0x0F, (byte) 0x01, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x09,
        (byte) 0x00, (byte) 0x03, (byte) 0x10, (byte) 0xFF, (byte) 0xFF };

    private CLinkstateDestination dest;

    private void setUp(final byte[] data) throws BGPParsingException {
        final LinkstateNlriParser parser = new LinkstateNlriParser(false);
        final MpReachNlriBuilder builder = new MpReachNlriBuilder();
        parser.parseNlri(Unpooled.copiedBuffer(data), builder);

        final DestinationLinkstate ls = ((DestinationLinkstateCase) builder.getAdvertizedRoutes().getDestinationType()).getDestinationLinkstate();
        assertEquals(1, ls.getCLinkstateDestination().size());

        this.dest = ls.getCLinkstateDestination().get(0);
    }

    @Test
    public void testNodeNlri() throws BGPParsingException {
        setUp(this.nodeNlri);

        // test BA form
        assertNull(this.dest.getDistinguisher());
        assertEquals(ProtocolId.IsisLevel2, this.dest.getProtocolId());
        assertEquals(BigInteger.ONE, this.dest.getIdentifier().getValue());
        final NodeCase nCase = ((NodeCase) this.dest.getObjectType());

        final NodeDescriptors nodeD = nCase.getNodeDescriptors();
        assertEquals(new AsNumber(72L), nodeD.getAsNumber());
        assertEquals(new DomainIdentifier(0x28282828L), nodeD.getDomainId());
        assertEquals(new IsisPseudonodeCaseBuilder().setIsisPseudonode(
            new IsisPseudonodeBuilder().setPsn((short) 5).setIsIsRouterIdentifier(
               new IsIsRouterIdentifierBuilder().setIsoSystemId(new IsoSystemIdentifier(new byte[] { 0, 0, 0, 0, 0, (byte) 0x39 })).build()).build()).build(), nodeD.getCRouterIdentifier());

        final ByteBuf buffer = Unpooled.buffer();
        LinkstateNlriParser.serializeNlri(this.dest, buffer);
        assertArrayEquals(this.nodeNlri, ByteArray.readAllBytes(buffer));

        // test BI form
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, UnkeyedListEntryNode> linkstateBI = ImmutableUnkeyedListEntryNodeBuilder
                .create();
        linkstateBI.withNodeIdentifier(C_LINKSTATE_NID);

        final ImmutableLeafNodeBuilder<String> protocolId = new ImmutableLeafNodeBuilder<>();
        protocolId.withNodeIdentifier(LinkstateNlriParser.PROTOCOL_ID_NID);
        protocolId.withValue("isis-level2");
        linkstateBI.addChild(protocolId.build());

        final ImmutableLeafNodeBuilder<BigInteger> identifier = new ImmutableLeafNodeBuilder<>();
        identifier.withNodeIdentifier(LinkstateNlriParser.IDENTIFIER_NID);
        identifier.withValue(BigInteger.ONE);
        linkstateBI.addChild(identifier.build());

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> objectType = Builders.choiceBuilder();
        objectType.withNodeIdentifier(LinkstateNlriParser.OBJECT_TYPE_NID);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> nodeDescriptors = Builders.containerBuilder();
        nodeDescriptors.withNodeIdentifier(LinkstateNlriParser.NODE_DESCRIPTORS_NID);

        final ImmutableLeafNodeBuilder<Long> asNumber = new ImmutableLeafNodeBuilder<>();
        asNumber.withNodeIdentifier(NodeNlriParser.AS_NUMBER_NID);
        asNumber.withValue(72L);
        nodeDescriptors.addChild(asNumber.build());

        final ImmutableLeafNodeBuilder<Long> areaID = new ImmutableLeafNodeBuilder<>();
        areaID.withNodeIdentifier(NodeNlriParser.AREA_NID);
        areaID.withValue(2697513L); nodeDescriptors.addChild(areaID.build());

        final ImmutableLeafNodeBuilder<Long> domainID = new ImmutableLeafNodeBuilder<>();
        domainID.withNodeIdentifier(NodeNlriParser.DOMAIN_NID);
        domainID.withValue(0x28282828L);
        nodeDescriptors.addChild(domainID.build());

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> crouterId = Builders.choiceBuilder();
        crouterId.withNodeIdentifier(C_ROUTER_ID_NID);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> isisNode = Builders.containerBuilder();
        isisNode.withNodeIdentifier(NodeNlriParser.ISIS_PSEUDONODE_NID);

        final ImmutableLeafNodeBuilder<byte[]> isoSystemID = new ImmutableLeafNodeBuilder<>();
        isoSystemID.withNodeIdentifier(NodeNlriParser.ISO_SYSTEM_NID);
        isoSystemID.withValue(new byte[]{0, 0, 0, 0, 0, (byte) 0x39});

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> isisPseudoRouter = Builders.containerBuilder();
        isisPseudoRouter.withNodeIdentifier(NodeNlriParser.ISIS_ROUTER_NID);
        isisPseudoRouter.addChild(isoSystemID.build());

        isisNode.addChild(isisPseudoRouter.build());
        isisNode.addChild(Builders.leafBuilder().withNodeIdentifier(NodeNlriParser.PSN_NID).withValue((short) 5).build());
        crouterId.addChild(isisNode.build());

        nodeDescriptors.addChild(crouterId.build());
        objectType.addChild(nodeDescriptors.build());
        linkstateBI.addChild(objectType.build());

        assertEquals(this.dest, LinkstateNlriParser.extractLinkstateDestination(linkstateBI.build()));
    }

    @Test
    public void testLinkNlri() throws BGPParsingException {
        setUp(this.linkNlri);

        // test BA form
        assertNull(this.dest.getDistinguisher());
        assertEquals(ProtocolId.IsisLevel2, this.dest.getProtocolId());
        assertEquals(BigInteger.ONE, this.dest.getIdentifier().getValue());

        final LinkCase lCase = ((LinkCase) this.dest.getObjectType());

        final LocalNodeDescriptors local = lCase.getLocalNodeDescriptors();
        assertEquals(new AsNumber(72L), local.getAsNumber());
        assertEquals(new DomainIdentifier(0x28282828L), local.getDomainId());
        assertEquals(
            new IsisNodeCaseBuilder().setIsisNode(
                new IsisNodeBuilder().setIsoSystemId(
                    new IsoSystemIdentifier(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x42 })).build()).build(), local.getCRouterIdentifier());
        final RemoteNodeDescriptors remote = lCase.getRemoteNodeDescriptors();
        assertEquals(new AsNumber(72L), remote.getAsNumber());
        assertEquals(new DomainIdentifier(0x28282828L), remote.getDomainId());
        assertEquals(new OspfNodeCaseBuilder().setOspfNode(new OspfNodeBuilder().setOspfRouterId(0x00000040L).build()).build(),
            remote.getCRouterIdentifier());
        final LinkDescriptors ld = lCase.getLinkDescriptors();
        assertEquals("197.20.160.42", ld.getIpv4InterfaceAddress().getValue());
        assertEquals("197.20.160.40", ld.getIpv4NeighborAddress().getValue());

        final ByteBuf buffer = Unpooled.buffer();
        LinkstateNlriParser.serializeNlri(this.dest, buffer);
        assertArrayEquals(this.linkNlri, ByteArray.readAllBytes(buffer));

        // test BI form
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, UnkeyedListEntryNode> linkstateBI = ImmutableUnkeyedListEntryNodeBuilder.create();
        linkstateBI.withNodeIdentifier(C_LINKSTATE_NID);

        final ImmutableLeafNodeBuilder<String> protocolId = new ImmutableLeafNodeBuilder<>();
        protocolId.withNodeIdentifier(LinkstateNlriParser.PROTOCOL_ID_NID);
        protocolId.withValue("isis-level2");
        linkstateBI.addChild(protocolId.build());

        final ImmutableLeafNodeBuilder<BigInteger> identifier = new ImmutableLeafNodeBuilder<>();
        identifier.withNodeIdentifier(LinkstateNlriParser.IDENTIFIER_NID);
        identifier.withValue(BigInteger.ONE);
        linkstateBI.addChild(identifier.build());

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> objectType = Builders.choiceBuilder();
        objectType.withNodeIdentifier(LinkstateNlriParser.OBJECT_TYPE_NID);

        // local node descriptors
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> localNodeDescriptors = Builders.containerBuilder();
        localNodeDescriptors.withNodeIdentifier(LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID);

        final ImmutableLeafNodeBuilder<Long> asNumber = new ImmutableLeafNodeBuilder<>();
        asNumber.withNodeIdentifier(NodeNlriParser.AS_NUMBER_NID);
        asNumber.withValue(72L);
        localNodeDescriptors.addChild(asNumber.build());

        final ImmutableLeafNodeBuilder<Long> domainID = new ImmutableLeafNodeBuilder<>();
        domainID.withNodeIdentifier(NodeNlriParser.DOMAIN_NID);
        domainID.withValue(0x28282828L);
        localNodeDescriptors.addChild(domainID.build());

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> crouterId = Builders.choiceBuilder();
        crouterId.withNodeIdentifier(C_ROUTER_ID_NID);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> isisNode = Builders.containerBuilder();
        isisNode.withNodeIdentifier(NodeNlriParser.ISIS_NODE_NID);

        final ImmutableLeafNodeBuilder<byte[]> isoSystemID = new ImmutableLeafNodeBuilder<>();
        isoSystemID.withNodeIdentifier(NodeNlriParser.ISO_SYSTEM_NID);
        isoSystemID.withValue(new byte[] { 0, 0, 0, 0, 0, (byte) 0x42 });

        isisNode.addChild(isoSystemID.build());
        crouterId.addChild(isisNode.build());
        localNodeDescriptors.addChild(crouterId.build());

        // remote descriptors
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> remoteNodeDescriptors = Builders.containerBuilder();
        remoteNodeDescriptors.withNodeIdentifier(LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID);
        remoteNodeDescriptors.addChild(asNumber.build());
        remoteNodeDescriptors.addChild(domainID.build());

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> crouterId2 = Builders.choiceBuilder();
        crouterId2.withNodeIdentifier(C_ROUTER_ID_NID);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> ospfNode = Builders.containerBuilder();
        ospfNode.withNodeIdentifier(NodeNlriParser.OSPF_NODE_NID);

        final ImmutableLeafNodeBuilder<Long> ospfRouterId = new ImmutableLeafNodeBuilder<>();
        ospfRouterId.withNodeIdentifier(NodeNlriParser.OSPF_ROUTER_NID);
        ospfRouterId.withValue(0x00000040L);
        ospfNode.addChild(ospfRouterId.build());
        crouterId2.addChild(ospfNode.build());
        remoteNodeDescriptors.addChild(crouterId2.build());

        // link descritpors
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> linkDescriptors = Builders.containerBuilder();
        linkDescriptors.withNodeIdentifier(LinkstateNlriParser.LINK_DESCRIPTORS_NID);

        final ImmutableLeafNodeBuilder<Long> linkLocalIdentifier = new ImmutableLeafNodeBuilder<>();
        linkLocalIdentifier.withNodeIdentifier(LinkNlriParser.LINK_LOCAL_NID);
        linkLocalIdentifier.withValue(16909060L);

        final ImmutableLeafNodeBuilder<Long> linkRemoteIdentifier = new ImmutableLeafNodeBuilder<>();
        linkRemoteIdentifier.withNodeIdentifier(LinkNlriParser.LINK_REMOTE_NID);
        linkRemoteIdentifier.withValue(168496141L);

        final ImmutableLeafNodeBuilder<String> ipv4InterfaceAddress = new ImmutableLeafNodeBuilder<>();
        ipv4InterfaceAddress.withNodeIdentifier(LinkNlriParser.IPV4_IFACE_NID);
        ipv4InterfaceAddress.withValue("197.20.160.42");

        final ImmutableLeafNodeBuilder<String> ipv4NeighborAddress = new ImmutableLeafNodeBuilder<>();
        ipv4NeighborAddress.withNodeIdentifier(LinkNlriParser.IPV4_NEIGHBOR_NID);
        ipv4NeighborAddress.withValue("197.20.160.40");

        final ImmutableLeafNodeBuilder<Integer> multiTopologyId = new ImmutableLeafNodeBuilder<>();
        multiTopologyId.withNodeIdentifier(TlvUtil.MULTI_TOPOLOGY_NID);
        multiTopologyId.withValue(3);

        linkDescriptors.addChild(linkLocalIdentifier.build());
        linkDescriptors.addChild(linkRemoteIdentifier.build());
        linkDescriptors.addChild(ipv4InterfaceAddress.build());
        linkDescriptors.addChild(ipv4NeighborAddress.build());
        linkDescriptors.addChild(multiTopologyId.build());

        objectType.addChild(localNodeDescriptors.build());
        objectType.addChild(remoteNodeDescriptors.build());
        objectType.addChild(linkDescriptors.build());

        linkstateBI.addChild(objectType.build());
        assertEquals(this.dest, LinkstateNlriParser.extractLinkstateDestination(linkstateBI.build()));
    }

    @Test
    public void testPrefixNlri() throws BGPParsingException {
        setUp(this.prefixNlri);

        // test BA form
        assertNull(this.dest.getDistinguisher());
        assertEquals(ProtocolId.IsisLevel2, this.dest.getProtocolId());
        assertEquals(BigInteger.ONE, this.dest.getIdentifier().getValue());

        final PrefixCase pCase = ((PrefixCase) this.dest.getObjectType());

        final AdvertisingNodeDescriptors local = pCase.getAdvertisingNodeDescriptors();
        assertEquals(new AsNumber(72L), local.getAsNumber());
        assertEquals(new DomainIdentifier(0x28282828L), local.getDomainId());
        assertEquals(
            new IsisNodeCaseBuilder().setIsisNode(
                new IsisNodeBuilder().setIsoSystemId(
                    new IsoSystemIdentifier(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x42 })).build()).build(), local.getCRouterIdentifier());

        final PrefixDescriptors pd = pCase.getPrefixDescriptors();
        assertEquals(OspfRouteType.External1, pd.getOspfRouteType());
        assertEquals(new TopologyIdentifier(15), pd.getMultiTopologyId());
        assertEquals(new Ipv4Prefix("255.255.0.0/16"), pd.getIpReachabilityInformation().getIpv4Prefix());

        final ByteBuf buffer = Unpooled.buffer();
        LinkstateNlriParser.serializeNlri(this.dest, buffer);
        assertArrayEquals(this.prefixNlri, ByteArray.readAllBytes(buffer));

        // test BI form
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, UnkeyedListEntryNode> linkstateBI = ImmutableUnkeyedListEntryNodeBuilder.create();
        linkstateBI.withNodeIdentifier(C_LINKSTATE_NID);

        final ImmutableLeafNodeBuilder<String> protocolId = new ImmutableLeafNodeBuilder<>();
        protocolId.withNodeIdentifier(LinkstateNlriParser.PROTOCOL_ID_NID);
        protocolId.withValue("isis-level2");
        linkstateBI.addChild(protocolId.build());

        final ImmutableLeafNodeBuilder<BigInteger> identifier = new ImmutableLeafNodeBuilder<>();
        identifier.withNodeIdentifier(LinkstateNlriParser.IDENTIFIER_NID);
        identifier.withValue(BigInteger.ONE);
        linkstateBI.addChild(identifier.build());

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> objectType = Builders.choiceBuilder();
        objectType.withNodeIdentifier(LinkstateNlriParser.OBJECT_TYPE_NID);

        // advertising node descriptors
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> advertisingNodeDescriptors = Builders.containerBuilder();
        advertisingNodeDescriptors.withNodeIdentifier(LinkstateNlriParser.ADVERTISING_NODE_DESCRIPTORS_NID);

        final ImmutableLeafNodeBuilder<Long> asNumber = new ImmutableLeafNodeBuilder<>();
        asNumber.withNodeIdentifier(NodeNlriParser.AS_NUMBER_NID);
        asNumber.withValue(72L);
        advertisingNodeDescriptors.addChild(asNumber.build());

        final ImmutableLeafNodeBuilder<Long> domainID = new ImmutableLeafNodeBuilder<>();
        domainID.withNodeIdentifier(NodeNlriParser.DOMAIN_NID);
        domainID.withValue(673720360L);
        advertisingNodeDescriptors.addChild(domainID.build());

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> crouterId = Builders.choiceBuilder();
        crouterId.withNodeIdentifier(C_ROUTER_ID_NID);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> isisNode = Builders.containerBuilder();
        isisNode.withNodeIdentifier(NodeNlriParser.ISIS_NODE_NID);

        final ImmutableLeafNodeBuilder<byte[]> isoSystemID = new ImmutableLeafNodeBuilder<>();
        isoSystemID.withNodeIdentifier(NodeNlriParser.ISO_SYSTEM_NID);
        isoSystemID.withValue(new byte[] { 0, 0, 0, 0, 0, (byte) 0x42 });

        isisNode.addChild(isoSystemID.build());
        crouterId.addChild(isisNode.build());
        advertisingNodeDescriptors.addChild(crouterId.build());

        // prefix descriptors
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> prefixDescriptors = Builders.containerBuilder();
        prefixDescriptors.withNodeIdentifier(LinkstateNlriParser.PREFIX_DESCRIPTORS_NID);
        prefixDescriptors.addChild(asNumber.build());
        prefixDescriptors.addChild(domainID.build());

        final ImmutableLeafNodeBuilder<Integer> multiTopologyId = new ImmutableLeafNodeBuilder<>();
        multiTopologyId.withNodeIdentifier(TlvUtil.MULTI_TOPOLOGY_NID);
        multiTopologyId.withValue(15);

        prefixDescriptors.addChild(multiTopologyId.build());

        final ImmutableLeafNodeBuilder<String> ipReachabilityInformation = new ImmutableLeafNodeBuilder<>();
        ipReachabilityInformation.withNodeIdentifier(PrefixNlriParser.IP_REACH_NID);
        ipReachabilityInformation.withValue("255.255.0.0/16");

        prefixDescriptors.addChild(ipReachabilityInformation.build());

        final ImmutableLeafNodeBuilder<String> ospfRouteType = new ImmutableLeafNodeBuilder<>();
        ospfRouteType.withNodeIdentifier(PrefixNlriParser.OSPF_ROUTE_NID);
        ospfRouteType.withValue("external1");

        prefixDescriptors.addChild(ospfRouteType.build());

        objectType.addChild(advertisingNodeDescriptors.build());
        objectType.addChild(prefixDescriptors.build());

        linkstateBI.addChild(objectType.build());
        assertEquals(this.dest, LinkstateNlriParser.extractLinkstateDestination(linkstateBI.build()));
    }

    @Test
    public void testSerializeAttribute() throws BGPParsingException {
        final LinkstateNlriParser parser = new LinkstateNlriParser(true);
        setUp(this.prefixNlri);
        final List<CLinkstateDestination> dests = Lists.newArrayList(this.dest);
        final DestinationLinkstateCase dlc = new DestinationLinkstateCaseBuilder().setDestinationLinkstate(new DestinationLinkstateBuilder().setCLinkstateDestination(dests).build()).build();
        final AdvertizedRoutes aroutes = new AdvertizedRoutesBuilder().setDestinationType(dlc).build();
        final Attributes1 reach = new Attributes1Builder().setMpReachNlri(new MpReachNlriBuilder().setAdvertizedRoutes(aroutes).build()).build();

        Attributes pa = new AttributesBuilder().addAugmentation(Attributes1.class, reach).build();

        ByteBuf result = Unpooled.buffer();
        parser.serializeAttribute(pa, result);
        assertArrayEquals(this.prefixNlri, ByteArray.getAllBytes(result));

        setUp(this.nodeNlri);
        final List<CLinkstateDestination> destsU = Lists.newArrayList(this.dest);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase dlcU =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder().setCLinkstateDestination(destsU).build()).build();
        final WithdrawnRoutes wroutes = new WithdrawnRoutesBuilder().setDestinationType(dlcU).build();
        final Attributes2 unreach = new Attributes2Builder().setMpUnreachNlri(new MpUnreachNlriBuilder().setWithdrawnRoutes(wroutes).build()).build();

        pa = new AttributesBuilder().addAugmentation(Attributes2.class, unreach).build();

        result = Unpooled.buffer();
        parser.serializeAttribute(pa, result);
        assertArrayEquals(this.nodeNlri, ByteArray.getAllBytes(result));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLinkNlriPrivateConstructor() throws Throwable {
        final Constructor<LinkNlriParser> c = LinkNlriParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNodeNlriPrivateConstructor() throws Throwable {
        final Constructor<NodeNlriParser> c = NodeNlriParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPrefixNlriPrivateConstructor() throws Throwable {
        final Constructor<PrefixNlriParser> c = PrefixNlriParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
