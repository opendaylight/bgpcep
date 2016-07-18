/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import java.math.BigInteger;
import java.util.Collections;
import java.util.charset.StandardCharsets;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AdministrativeGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.LinkAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.NodeAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.PrefixAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.linkstate.route.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IgpMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LinkstateTopologyBuilderTest extends AbstractTopologyBuilderTest {

    private static final byte[] LINKSTATE_ROUTE_KEY = Unpooled.wrappedBuffer(StandardCharsets.UTF_8.encode("linkstate-route")).array();
    private static final String ROUTER_1_ID = "127.0.0.1";
    private static final String ROUTER_2_ID = "127.0.0.2";
    private static final String NODE_1_PREFIX = "127.0.1.1/32";
    private static final AsNumber NODE_1_AS = new AsNumber(1L);
    private static final AsNumber NODE_2_AS = new AsNumber(2L);
    private static final String NODE_1_ISIS_ID = "bgpls://IsisLevel2:1/type=node&as=1&router=0000.0102.0304";
    private static final String NODE_2_ISIS_ID = "bgpls://IsisLevel2:1/type=node&as=2";
    private static final String NODE_1_OSPF_ID = "bgpls://Ospf:1/type=node&as=1&router=0000.0102.0304";
    private static final String NODE_2_OSPF_ID = "bgpls://Ospf:1/type=node&as=2";
    private static final Identifier IDENTIFIER = new Identifier(new BigInteger("1"));

    private LinkstateTopologyBuilder linkstateTopoBuilder;
    private InstanceIdentifier<LinkstateRoute> linkstateRouteIID;


    @Override
    protected void setupWithDataBroker(final DataBroker dataBroker) {
        super.setupWithDataBroker(dataBroker);
        this.linkstateTopoBuilder = new LinkstateTopologyBuilder(dataBroker, LOC_RIB_REF, TEST_TOPOLOGY_ID);
        this.linkstateTopoBuilder.start(dataBroker, LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);
        final InstanceIdentifier<Tables> path = this.linkstateTopoBuilder.tableInstanceIdentifier(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);
        this.linkstateRouteIID = path.builder().child((Class)LinkstateRoutes.class).child(LinkstateRoute.class, new LinkstateRouteKey(LINKSTATE_ROUTE_KEY)).build();
    }

    @Test
    public void testLinkstateTopologyBuilderTopologyTypes() {
        final Optional<Topology> topologyMaybe = getTopology(this.linkstateTopoBuilder.getInstanceIdentifier());
        assertTrue(topologyMaybe.isPresent());
        final Topology topology = topologyMaybe.get();
        assertNotNull(topology.getTopologyTypes().getAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1.class));
        assertNotNull(topology.getTopologyTypes().getAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1.class).getBgpLinkstateTopology());
    }

    @Test
    public void testIsisLinkstateTopologyBuilder() throws TransactionCommitFailedException {
        // create node
        updateLinkstateRoute(createLinkstateNodeRoute(ProtocolId.IsisLevel2, "node1", NODE_1_AS, ROUTER_1_ID));
        final Optional<Topology> topologyMaybe = getTopology(this.linkstateTopoBuilder.getInstanceIdentifier());
        assertTrue(topologyMaybe.isPresent());
        final Topology topology1 = topologyMaybe.get();
        assertEquals(1, topology1.getNode().size());
        final Node node1 = topology1.getNode().get(0);
        assertEquals(NODE_1_ISIS_ID, node1.getNodeId().getValue());
        final IgpNodeAttributes igpNode1 = node1.getAugmentation(Node1.class).getIgpNodeAttributes();
        assertEquals(ROUTER_1_ID, igpNode1.getRouterId().get(0).getIpv4Address().getValue());
        assertEquals("node1", igpNode1.getName().getValue());
        final IgpNodeAttributes1 igpNodeAttributes1 = igpNode1.getAugmentation(IgpNodeAttributes1.class);
        assertEquals("0000.0102.0304", igpNodeAttributes1.getIsisNodeAttributes().getIso().getIsoSystemId().getValue());
        assertEquals(ROUTER_1_ID, igpNodeAttributes1.getIsisNodeAttributes().getTed().getTeRouterIdIpv4().getValue());
        assertEquals("47.0000.0000.0000.0000.0102.0304", igpNodeAttributes1.getIsisNodeAttributes().getNet().get(0).getValue());
        assertNull(igpNode1.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1.class));

        // create link
        updateLinkstateRoute(createLinkstateLinkRoute(ProtocolId.IsisLevel2, NODE_1_AS, NODE_2_AS, "link1"));
        final Topology topology2 = getTopology(this.linkstateTopoBuilder.getInstanceIdentifier()).get();
        assertEquals(1, topology2.getLink().size());
        final Link link1 = topology2.getLink().get(0);
        assertEquals(2, topology2.getNode().size());
        assertEquals(1, topology2.getNode().get(0).getTerminationPoint().size());
        assertEquals(1, topology2.getNode().get(1).getTerminationPoint().size());
        assertEquals("bgpls://IsisLevel2:1/type=link&local-as=1&local-router=0000.0102.0304&remote-as=2&mt=1", link1.getLinkId().getValue());
        assertEquals(NODE_1_ISIS_ID, link1.getSource().getSourceNode().getValue());
        assertEquals(NODE_2_ISIS_ID, link1.getDestination().getDestNode().getValue());
        final IgpLinkAttributes igpLink1 = link1.getAugmentation(Link1.class).getIgpLinkAttributes();
        assertEquals("link1", igpLink1.getName());
        assertEquals((short) 1, igpLink1.getAugmentation(IgpLinkAttributes1.class).getIsisLinkAttributes().getMultiTopologyId().shortValue());
        assertNull(igpLink1.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1.class));

        // update node
        updateLinkstateRoute(createLinkstateNodeRoute(ProtocolId.IsisLevel2, "updated-node", NODE_1_AS, ROUTER_2_ID));
        final Topology topology3 = getTopology(this.linkstateTopoBuilder.getInstanceIdentifier()).get();
        final IgpNodeAttributes igpNode2 = topology3.getNode().get(0).getAugmentation(Node1.class).getIgpNodeAttributes();
        assertEquals(ROUTER_2_ID, igpNode2.getRouterId().get(0).getIpv4Address().getValue());
        assertEquals("updated-node", igpNode2.getName().getValue());

        // remove
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, this.linkstateRouteIID);
        wTx.submit();
        final Topology topology4 = getTopology(this.linkstateTopoBuilder.getInstanceIdentifier()).get();
        assertEquals(0, topology4.getNode().size());
        assertEquals(0, topology4.getLink().size());
    }

    @Test
    public void testOspfLinkstateTopologyBuilder() throws TransactionCommitFailedException {
        // create node
        updateLinkstateRoute(createLinkstateNodeRoute(ProtocolId.Ospf, "node1", NODE_1_AS, ROUTER_1_ID));
        final Optional<Topology> topologyMaybe = getTopology(this.linkstateTopoBuilder.getInstanceIdentifier());
        assertTrue(topologyMaybe.isPresent());
        final Topology topology1 = topologyMaybe.get();
        assertEquals(1, topology1.getNode().size());
        final Node node1 = topology1.getNode().get(0);
        assertEquals(NODE_1_OSPF_ID, node1.getNodeId().getValue());
        final IgpNodeAttributes igpNode1 = node1.getAugmentation(Node1.class).getIgpNodeAttributes();
        assertEquals(ROUTER_1_ID, igpNode1.getRouterId().get(0).getIpv4Address().getValue());
        assertEquals("node1", igpNode1.getName().getValue());
        assertNull(igpNode1.getAugmentation(IgpNodeAttributes1.class));
        assertEquals(ROUTER_1_ID, igpNode1.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1.class).getOspfNodeAttributes().getTed().getTeRouterIdIpv4().getValue());

        // update node with prefix
        updateLinkstateRoute(createLinkstatePrefixRoute(ProtocolId.Ospf, NODE_1_AS, NODE_1_PREFIX, 500L, ROUTER_1_ID));
        final Topology topology3 = getTopology(this.linkstateTopoBuilder.getInstanceIdentifier()).get();
        final IgpNodeAttributes igpNode2 = topology3.getNode().get(0).getAugmentation(Node1.class).getIgpNodeAttributes();
        assertEquals(1, igpNode2.getPrefix().size());
        final Prefix prefix = igpNode2.getPrefix().get(0);
        assertEquals(NODE_1_PREFIX, prefix.getPrefix().getIpv4Prefix().getValue());
        assertEquals(500L, prefix.getMetric().longValue());

        // create link
        updateLinkstateRoute(createLinkstateLinkRoute(ProtocolId.Ospf, NODE_1_AS, NODE_2_AS, "link1"));
        final Topology topology2 = getTopology(this.linkstateTopoBuilder.getInstanceIdentifier()).get();
        assertEquals(1, topology2.getLink().size());
        final Link link1 = topology2.getLink().get(0);
        assertEquals(2, topology2.getNode().size());
        assertEquals(1, topology2.getNode().get(0).getTerminationPoint().size());
        assertEquals(1, topology2.getNode().get(1).getTerminationPoint().size());
        assertEquals("bgpls://Ospf:1/type=link&local-as=1&local-router=0000.0102.0304&remote-as=2&mt=1", link1.getLinkId().getValue());
        assertEquals(NODE_1_OSPF_ID, link1.getSource().getSourceNode().getValue());
        assertEquals(NODE_2_OSPF_ID, link1.getDestination().getDestNode().getValue());
        final IgpLinkAttributes igpLink1 = link1.getAugmentation(Link1.class).getIgpLinkAttributes();
        assertEquals("link1", igpLink1.getName());
        assertNull(igpLink1.getAugmentation(IgpLinkAttributes1.class));
        assertEquals((short) 1, igpLink1.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1.class).getOspfLinkAttributes().getMultiTopologyId().shortValue());
        assertEquals(2, igpLink1.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1.class).getOspfLinkAttributes().getTed().getSrlg().getSrlgValues().size());

        this.linkstateTopoBuilder.close();
        assertFalse(getTopology(this.linkstateTopoBuilder.getInstanceIdentifier()).isPresent());
    }

    private void updateLinkstateRoute(final LinkstateRoute data) {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, this.linkstateRouteIID, data, true);
        wTx.submit();
    }

    private LinkstateRoute createLinkstateNodeRoute(final ProtocolId protocolId, final String nodeName, final AsNumber asNumber, final String ipv4RouterId) {
        return createBaseBuilder(protocolId)
            .setObjectType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCaseBuilder().setNodeDescriptors(new NodeDescriptorsBuilder().setCRouterIdentifier(new IsisNodeCaseBuilder().setIsisNode(new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(new byte[]{ 0, 0, 1, 2, 3, 4 })).build()).build()).setAsNumber(asNumber).build()).build())
            .setAttributes(new AttributesBuilder()
                .addAugmentation(Attributes1.class, new Attributes1Builder().setLinkStateAttribute(new NodeAttributesCaseBuilder().setNodeAttributes(new NodeAttributesBuilder()
                    .setDynamicHostname(nodeName)
                    .setIpv4RouterId(new Ipv4RouterIdentifier(ipv4RouterId))
                    .setIsisAreaId(Collections.singletonList(new IsisAreaIdentifier(new byte[]{0x47}))).build()).build()).build()).build()).build();
    }

    private LinkstateRoute createLinkstatePrefixRoute(final ProtocolId protocolId, final AsNumber asNumber, final String ipv4Prefix, final long igpMetric, final String ospfFwdAddress) {
        return createBaseBuilder(protocolId)
            .setObjectType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCaseBuilder()
                .setAdvertisingNodeDescriptors(new AdvertisingNodeDescriptorsBuilder().setAsNumber(asNumber).build())
                .setPrefixDescriptors(new PrefixDescriptorsBuilder().setIpReachabilityInformation(new IpPrefix(new Ipv4Prefix(ipv4Prefix))).build())
                .build())
            .setAttributes(new AttributesBuilder()
                .addAugmentation(Attributes1.class, new Attributes1Builder().setLinkStateAttribute(new PrefixAttributesCaseBuilder().setPrefixAttributes(new PrefixAttributesBuilder().setOspfForwardingAddress(new IpAddress(new Ipv4Address(ospfFwdAddress))).setPrefixMetric(new IgpMetric(igpMetric)).build()).build()).build()).build())
             .build();
    }

    private LinkstateRoute createLinkstateLinkRoute(final ProtocolId protocolId, final AsNumber localAs, final AsNumber remoteAs, final String linkName) {
        return createBaseBuilder(protocolId)
            .setObjectType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCaseBuilder()
                .setLocalNodeDescriptors(new LocalNodeDescriptorsBuilder().setAsNumber(localAs).setCRouterIdentifier(new IsisNodeCaseBuilder().setIsisNode(new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(new byte[]{ 0, 0, 1, 2, 3, 4 })).build()).build()).build())
                .setRemoteNodeDescriptors(new RemoteNodeDescriptorsBuilder().setAsNumber(remoteAs).build())
                .setLinkDescriptors(new LinkDescriptorsBuilder().setMultiTopologyId(new TopologyIdentifier(1)).build())
                .build())
            .setAttributes(new AttributesBuilder()
                .addAugmentation(Attributes1.class, new Attributes1Builder().setLinkStateAttribute(new LinkAttributesCaseBuilder().setLinkAttributes(
                    new LinkAttributesBuilder().setSharedRiskLinkGroups(Lists.newArrayList(new SrlgId(5L), new SrlgId(15L))).setAdminGroup(new AdministrativeGroup(0L))
                        .setMaxLinkBandwidth(new Bandwidth(new byte[]{0x00, 0x00, (byte) 0xff, (byte) 0xff}))
                        .setMaxReservableBandwidth(new Bandwidth(new byte[]{0x00, 0x00, (byte) 0xff, (byte) 0x1f}))
                        .setUnreservedBandwidth(Lists.newArrayList(new UnreservedBandwidthBuilder().setKey(new UnreservedBandwidthKey((short) 1)).setBandwidth(new Bandwidth(new byte[]{0x00, 0x00, 0x00, (byte) 0xff})).build()))
                        .setTeMetric(new TeMetric(100L)).setLinkName(linkName).build()).build()).build())
                .build())
            .build();
    }

    private LinkstateRouteBuilder createBaseBuilder(final ProtocolId protocolId) {
        return new LinkstateRouteBuilder()
            .setIdentifier(IDENTIFIER)
            .setKey(new LinkstateRouteKey(LINKSTATE_ROUTE_KEY))
            .setRouteKey(LINKSTATE_ROUTE_KEY)
            .setProtocolId(protocolId);
    }
}
