/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.NodeNlriParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.c.next.hop.LinkstateNextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListNodeBuilder;

public class LinkstateRIBSupportTest {

    private static final Ipv4Address ipv4 = new Ipv4Address("42.42.42.42");
    private static final NodeIdentifier ROUTES_NODE_ID = new NodeIdentifier(Routes.QNAME);
    final LinkstateRIBSupport link = LinkstateRIBSupport.getInstance();
    final List<MapEntryNode> linkList = new ArrayList<>();
    private List<YangInstanceIdentifier> routes;

    @Mock
    private DOMDataWriteTransaction tx;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        routes = new ArrayList<>();
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                LinkstateRIBSupportTest.this.routes.add((YangInstanceIdentifier) args[1]);
                return args[1];
            }
        }).when(this.tx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                LinkstateRIBSupportTest.this.routes.remove(args[1]);
                return args[1];
            }
        }).when(this.tx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class));
    }

    @Test
    public void testbuildReach() throws BGPParsingException {
        final CNextHop hop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(ipv4).build()).build();
        final MpReachNlri result = link.buildReach(linkList, hop);
        assertEquals(LinkstateAddressFamily.class, result.getAfi());
        assertEquals(LinkstateSubsequentAddressFamily.class, result.getSafi());
        assertEquals(new LinkstateNextHopCaseBuilder().setIpv4NextHop(new org.opendaylight.yang.gen.v1.urn
            .opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.c.next.hop.linkstate.next.hop._case.Ipv4NextHopBuilder().setGlobal(new Ipv4Address
            ("42.42.42.42")).build()).build(), result.getCNextHop());
    }

    @Test
    public void testBuildUnreach() {
        final MpUnreachNlri result = link.buildUnreach(linkList);
        assertEquals(LinkstateAddressFamily.class, result.getAfi());
        assertEquals(LinkstateSubsequentAddressFamily.class, result.getSafi());
    }

    @Test
    public void testDestinationRoutes() {
        final YangInstanceIdentifier yangIdentifier = YangInstanceIdentifier.of(Routes.QNAME);

        final DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> linkstateBI = ImmutableUnkeyedListEntryNodeBuilder.create();
        linkstateBI.withNodeIdentifier(new NodeIdentifier(CLinkstateDestination.QNAME));

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
        areaID.withValue(2697513L);
        nodeDescriptors.addChild(areaID.build());

        final ImmutableLeafNodeBuilder<Long> domainID = new ImmutableLeafNodeBuilder<>();
        domainID.withNodeIdentifier(NodeNlriParser.DOMAIN_NID);
        domainID.withValue(0x28282828L);
        nodeDescriptors.addChild(domainID.build());

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> crouterId = Builders.choiceBuilder();
        crouterId.withNodeIdentifier(new NodeIdentifier(CRouterIdentifier.QNAME));

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> isisNode = Builders.containerBuilder();
        isisNode.withNodeIdentifier(NodeNlriParser.ISIS_PSEUDONODE_NID);

        final ImmutableLeafNodeBuilder<byte[]> isoSystemID = new ImmutableLeafNodeBuilder<>();
        isoSystemID.withNodeIdentifier(NodeNlriParser.ISO_SYSTEM_NID);
        isoSystemID.withValue(new byte[]{0, 0, 0, 0, 0, (byte) 0x39});
        isisNode.addChild(isoSystemID.build());
        isisNode.addChild(Builders.leafBuilder().withNodeIdentifier(NodeNlriParser.PSN_NID).withValue((short) 5).build());
        crouterId.addChild(isisNode.build());

        nodeDescriptors.addChild(crouterId.build());
        objectType.addChild(nodeDescriptors.build());
        linkstateBI.addChild(objectType.build());

        final UnkeyedListNode routes = ImmutableUnkeyedListNodeBuilder.create()
            .withNodeIdentifier(new NodeIdentifier(CLinkstateDestination.QNAME))
            .addChild(linkstateBI.build())
            .build();

        final ContainerNode destination = ImmutableContainerNodeBuilder.create().
            addChild(routes)
            .withNodeIdentifier(new NodeIdentifier(CLinkstateDestination.QNAME))
            .build();

        final ContainerNode attributes = ImmutableContainerNodeBuilder.create()
            .withNodeIdentifier(new NodeIdentifier(Attributes.QNAME))
            .build();

        link.putDestinationRoutes(tx, yangIdentifier, destination, attributes, ROUTES_NODE_ID);

        Assert.assertEquals(1, this.routes.size());

        link.deleteDestinationRoutes(tx, yangIdentifier, destination, ROUTES_NODE_ID);

        Assert.assertEquals(0, this.routes.size());
    }

}