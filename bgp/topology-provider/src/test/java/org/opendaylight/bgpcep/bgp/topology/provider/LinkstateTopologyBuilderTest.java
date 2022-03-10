/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentOperational;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.AdministrativeGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv4RouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.bgp.rib.rib.loc.rib.tables.routes.LinkstateRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.SrAdjIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.LinkAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.NodeAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.PrefixAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.linkstate.routes.LinkstateRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.linkstate.routes.linkstate.route.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.LocalLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.SidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IgpMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public class LinkstateTopologyBuilderTest extends AbstractTopologyBuilderTest {

    private static final String ROUTER_1_ID = "127.0.0.1";
    private static final String ROUTER_2_ID = "127.0.0.2";
    private static final String NODE_1_PREFIX = "127.0.1.1/32";
    private static final AsNumber NODE_1_AS = new AsNumber(Uint32.ONE);
    private static final AsNumber NODE_2_AS = new AsNumber(Uint32.TWO);
    private static final String NODE_1_ISIS_ID = "bgpls://IsisLevel2:1/type=node&as=1&router=0000.0102.0304";
    private static final String NODE_2_ISIS_ID = "bgpls://IsisLevel2:1/type=node&as=2";
    private static final String NODE_1_OSPF_ID = "bgpls://Ospf:1/type=node&as=1&router=0000.0102.0304";
    private static final String NODE_2_OSPF_ID = "bgpls://Ospf:1/type=node&as=2";
    private static final Identifier IDENTIFIER = new Identifier(Uint64.ONE);
    private static final long LISTENER_RESTART_TIME = 20000;
    private static final int LISTENER_ENFORCE_COUNTER = 2;
    private static final int SRGB_START = 90000;
    private static final int SRGB_RANGE = 16;
    private static final int NODE_SID_INDEX = 4;
    private static final int ADJ_SID = 24001;

    private LinkstateTopologyBuilder linkstateTopoBuilder;
    private InstanceIdentifier<Tables> tablePathIID;
    private String linkstateNodeRouteKey;
    private String linkstatePrefixRouteKey;
    private String linkstateLinkRouteKey;
    private InstanceIdentifier<LinkstateRoute> linkstateNodeRouteIID;
    private InstanceIdentifier<LinkstateRoute> linkstatePrefixRouteIID;
    private InstanceIdentifier<LinkstateRoute> linkstateLinkRouteIID;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        linkstateTopoBuilder = new LinkstateTopologyBuilder(getDataBroker(), LOC_RIB_REF, TEST_TOPOLOGY_ID,
            LISTENER_RESTART_TIME, LISTENER_ENFORCE_COUNTER);
        linkstateTopoBuilder.start();
        tablePathIID =
                LOC_RIB_REF.getInstanceIdentifier().builder().child(LocRib.class)
                        .child(Tables.class,
                                new TablesKey(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class))
                        .build();
        linkstateNodeRouteKey = getLinkstateRouteKey("node-route");
        linkstatePrefixRouteKey = getLinkstateRouteKey("prefix-route");
        linkstateLinkRouteKey = getLinkstateRouteKey("link-route");
        linkstateNodeRouteIID = createLinkstateRouteIID(linkstateNodeRouteKey);
        linkstatePrefixRouteIID = createLinkstateRouteIID(linkstatePrefixRouteKey);
        linkstateLinkRouteIID = createLinkstateRouteIID(linkstateLinkRouteKey);
    }

    @After
    public void tearDown() throws Exception {
        linkstateTopoBuilder.close();
        checkNotPresentOperational(getDataBroker(), linkstateTopoBuilder.getInstanceIdentifier());
    }

    @Test
    public void testLinkstateTopologyBuilderTopologyTypes() throws InterruptedException, ExecutionException {
        readDataOperational(getDataBroker(), linkstateTopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(LinkstateTopologyBuilder.LINKSTATE_TOPOLOGY_TYPE, topology.getTopologyTypes());
            return topology;
        });
    }

    @Test
    public void testIsisLinkstateTopologyBuilder() throws InterruptedException, ExecutionException {
        // create node
        updateLinkstateRoute(linkstateNodeRouteIID,
                createLinkstateNodeRoute(ProtocolId.IsisLevel2, "node1", NODE_1_AS, ROUTER_1_ID));
        readDataOperational(getDataBroker(), linkstateTopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(1, topology.nonnullNode().size());
            final Node node1 = topology.nonnullNode().values().iterator().next();
            assertEquals(NODE_1_ISIS_ID, node1.getNodeId().getValue());
            final IgpNodeAttributes igpNode1 = node1.augmentation(Node1.class).getIgpNodeAttributes();
            assertEquals(ROUTER_1_ID, igpNode1.getRouterId().iterator().next().getIpv4Address().getValue());
            assertEquals("node1", igpNode1.getName().getValue());
            final IgpNodeAttributes1 igpNodeAttributes1 = igpNode1.augmentation(IgpNodeAttributes1.class);
            assertEquals("0000.0102.0304", igpNodeAttributes1.getIsisNodeAttributes().getIso().getIsoSystemId()
                    .getValue());
            assertEquals(ROUTER_1_ID, igpNodeAttributes1.getIsisNodeAttributes().getTed().getTeRouterIdIpv4()
                    .getValue());
            assertEquals("47.0000.0000.0000.0000.0102.0304", igpNodeAttributes1.getIsisNodeAttributes()
                    .getNet().iterator().next().getValue());
            assertNull(igpNode1.augmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf
                    .topology.rev131021.IgpNodeAttributes1.class));
            assertEquals(LinkstateTopologyBuilder.LINKSTATE_TOPOLOGY_TYPE, topology.getTopologyTypes());
            assertNull(node1.augmentation(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.Node1.class));
            return topology;
        });

        // create link
        updateLinkstateRoute(linkstateLinkRouteIID,
                createLinkstateLinkRoute(ProtocolId.IsisLevel2, NODE_1_AS, NODE_2_AS, "link1"));
        readDataOperational(getDataBroker(), linkstateTopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(1, topology.nonnullLink().size());
            final Link link1 = topology.nonnullLink().values().iterator().next();
            assertEquals(2, topology.nonnullNode().size());
            assertEquals(1, Iterables.get(topology.getNode().values(), 0).getTerminationPoint().size());
            assertEquals(1, Iterables.get(topology.getNode().values(), 1).getTerminationPoint().size());
            assertEquals("bgpls://IsisLevel2:1/type=link&local-as=1&local-router=0000.0102.0304&remote-as"
                    + "=2&mt=1", link1.getLinkId().getValue());
            assertEquals(NODE_1_ISIS_ID, link1.getSource().getSourceNode().getValue());
            assertEquals(NODE_2_ISIS_ID, link1.getDestination().getDestNode().getValue());
            final IgpLinkAttributes igpLink1 = link1.augmentation(Link1.class).getIgpLinkAttributes();
            assertEquals("link1", igpLink1.getName());
            assertEquals((short) 1, igpLink1.augmentation(IgpLinkAttributes1.class).getIsisLinkAttributes()
                    .getMultiTopologyId().shortValue());
            assertNull(igpLink1.augmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf
                    .topology.rev131021.IgpLinkAttributes1.class));
            assertEquals(LinkstateTopologyBuilder.SR_AWARE_LINKSTATE_TOPOLOGY_TYPE, topology.getTopologyTypes());
            assertEquals(2, topology.getNode().size());
            final Node srcNode;
            if (topology.getNode().values().iterator().next().getNodeId().getValue().contains("0000.0102.0304")) {
                srcNode = topology.getNode().values().iterator().next();
            } else {
                srcNode = Iterables.get(topology.getNode().values(), 1);
            }
            assertEquals(1, srcNode.augmentation(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.Node1.class)
                    .getSegments().size());
            assertEquals(ADJ_SID, link1.augmentation(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.Link1.class)
                    .getSegment().getValue().intValue());
            return topology;
        });
        removeLinkstateRoute(linkstateLinkRouteIID);

        // update node
        updateLinkstateRoute(linkstateNodeRouteIID,
                createLinkstateNodeRoute(ProtocolId.IsisLevel2, "updated-node", NODE_1_AS, ROUTER_2_ID));
        readDataOperational(getDataBroker(), linkstateTopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(1, topology.getNode().size());
            final IgpNodeAttributes igpNode2 = topology.getNode().values().iterator().next().augmentation(Node1.class)
                    .getIgpNodeAttributes();
            assertEquals(ROUTER_2_ID, igpNode2.getRouterId().iterator().next().getIpv4Address().getValue());
            assertEquals("updated-node", igpNode2.getName().getValue());
            return topology;
        });

        // remove
        removeLinkstateRoute(linkstateNodeRouteIID);
        readDataOperational(getDataBroker(), linkstateTopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(LinkstateTopologyBuilder.LINKSTATE_TOPOLOGY_TYPE, topology.getTopologyTypes());
            assertNull(topology.getNode());
            assertNull(topology.getLink());
            return topology;
        });
    }

    @Test
    public void testOspfLinkstateTopologyBuilder() throws InterruptedException, ExecutionException {
        // create node
        updateLinkstateRoute(linkstateNodeRouteIID,
                createLinkstateNodeRoute(ProtocolId.Ospf, "node1", NODE_1_AS, ROUTER_1_ID));
        readDataOperational(getDataBroker(), linkstateTopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(1, topology.nonnullNode().size());
            final Node node1 = topology.nonnullNode().values().iterator().next();
            assertEquals(NODE_1_OSPF_ID, node1.getNodeId().getValue());
            final IgpNodeAttributes igpNode1 = node1.augmentation(Node1.class).getIgpNodeAttributes();
            assertEquals(ROUTER_1_ID, igpNode1.getRouterId().iterator().next().getIpv4Address().getValue());
            assertEquals("node1", igpNode1.getName().getValue());
            assertNull(igpNode1.augmentation(IgpNodeAttributes1.class));
            assertEquals(ROUTER_1_ID, igpNode1.augmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns
                    .yang.ospf.topology.rev131021.IgpNodeAttributes1.class).getOspfNodeAttributes().getTed()
                    .getTeRouterIdIpv4().getValue());
            assertEquals(LinkstateTopologyBuilder.LINKSTATE_TOPOLOGY_TYPE, topology.getTopologyTypes());
            assertNull(node1.augmentation(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.Node1.class));
            return topology;
        });

        // update node with prefix
        updateLinkstateRoute(linkstatePrefixRouteIID,
                createLinkstatePrefixRoute(ProtocolId.Ospf, NODE_1_AS, NODE_1_PREFIX, 500L, ROUTER_1_ID));
        readDataOperational(getDataBroker(), linkstateTopoBuilder.getInstanceIdentifier(), topology -> {
            final Node node1 = topology.nonnullNode().values().iterator().next();
            final IgpNodeAttributes igpNode1 = node1.augmentation(Node1.class).getIgpNodeAttributes();
            assertEquals(1, igpNode1.nonnullPrefix().size());
            final Prefix prefix = igpNode1.nonnullPrefix().values().iterator().next();
            assertEquals(NODE_1_PREFIX, prefix.getPrefix().getIpv4Prefix().getValue());
            assertEquals(500L, prefix.getMetric().longValue());
            assertEquals(LinkstateTopologyBuilder.SR_AWARE_LINKSTATE_TOPOLOGY_TYPE, topology.getTopologyTypes());
            assertEquals(1, node1.augmentation(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.Node1.class)
                    .nonnullSegments().size());
            assertEquals(SRGB_START + NODE_SID_INDEX, node1.augmentation(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.Node1.class)
                    .nonnullSegments().values().iterator().next().getSegmentId().getValue().intValue());
            return topology;
        });

        // create link
        updateLinkstateRoute(linkstateLinkRouteIID,
                createLinkstateLinkRoute(ProtocolId.Ospf, NODE_1_AS, NODE_2_AS, "link1"));
        readDataOperational(getDataBroker(), linkstateTopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(1, topology.nonnullLink().size());
            final Link link1 = topology.nonnullLink().values().iterator().next();
            assertEquals(2, topology.getNode().size());
            assertEquals(1, Iterables.get(topology.getNode().values(), 0).getTerminationPoint().size());
            assertEquals(1, Iterables.get(topology.getNode().values(), 1).getTerminationPoint().size());
            assertEquals("bgpls://Ospf:1/type=link&local-as=1&local-router=0000.0102.0304&remote-as=2&mt=1",
                    link1.getLinkId().getValue());
            assertEquals(NODE_1_OSPF_ID, link1.getSource().getSourceNode().getValue());
            assertEquals(NODE_2_OSPF_ID, link1.getDestination().getDestNode().getValue());
            final IgpLinkAttributes igpLink1 = link1.augmentation(Link1.class).getIgpLinkAttributes();
            assertEquals("link1", igpLink1.getName());
            assertNull(igpLink1.augmentation(IgpLinkAttributes1.class));
            assertEquals((short) 1, igpLink1.augmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                    .ospf.topology.rev131021.IgpLinkAttributes1.class).getOspfLinkAttributes().getMultiTopologyId()
                    .shortValue());
            assertEquals(2, igpLink1.augmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns
                    .yang.ospf.topology.rev131021.IgpLinkAttributes1.class).getOspfLinkAttributes().getTed().getSrlg()
                    .getSrlgValues().size());
            assertEquals(LinkstateTopologyBuilder.SR_AWARE_LINKSTATE_TOPOLOGY_TYPE, topology.getTopologyTypes());
            assertEquals(2, topology.getNode().size());
            final Node srcNode;
            if (topology.getNode().values().iterator().next().getNodeId().getValue().contains("0000.0102.0304")) {
                srcNode = topology.getNode().values().iterator().next();
            } else {
                srcNode = Iterables.get(topology.getNode().values(), 1);
            }
            assertEquals(2, srcNode.augmentation(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.Node1.class)
                    .getSegments().size());
            assertEquals(ADJ_SID, link1.augmentation(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.Link1.class)
                    .getSegment().getValue().intValue());
            return topology;
        });
    }

    /**
     * This test is to verify if the AbstractTopologyBuilder/LinkstateTopologyBuilder is handling exception correctly.
     */
    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testRouteChangedError() throws Exception {
        final LinkstateTopologyBuilder spiedLinkstateTopologyBuilder = spy(linkstateTopoBuilder);
        doThrow(RuntimeException.class).when(spiedLinkstateTopologyBuilder).routeChanged(any(), any());
        try {
            spiedLinkstateTopologyBuilder.routeChanged(null, null);
            fail("Mockito failed to spy routeChanged() method");
        } catch (final Exception e) {
            assertTrue(e instanceof RuntimeException);
        }
        assertEquals(0L, spiedLinkstateTopologyBuilder.listenerScheduledRestartTime);
        assertEquals(0L, spiedLinkstateTopologyBuilder.listenerScheduledRestartEnforceCounter);
        // first we examine if the chain is being reset when no exception is thrown
        spiedLinkstateTopologyBuilder.onDataTreeChanged(new ArrayList<>());
        verify(spiedLinkstateTopologyBuilder, times(1)).restartTransactionChainOnDemand();
        verify(spiedLinkstateTopologyBuilder, never()).scheduleListenerRestart();
        verify(spiedLinkstateTopologyBuilder, never()).resetTransactionChain();
        assertEquals(0L, spiedLinkstateTopologyBuilder.listenerScheduledRestartTime);
        assertEquals(0L, spiedLinkstateTopologyBuilder.listenerScheduledRestartEnforceCounter);
        // now pass some invalid data to cause onDataTreeChanged fail
        final DataTreeModification<LinkstateRoute> modification = mock(DataTreeModification.class, RETURNS_SMART_NULLS);
        final List<DataTreeModification<LinkstateRoute>> changes = new ArrayList<>();
        changes.add(modification);
        spiedLinkstateTopologyBuilder.onDataTreeChanged(changes);
        // one restart transaction chain check in onDataTreeChanged()
        // we are introducing some timeout here as transaction may be executed in a delay manner
        verify(spiedLinkstateTopologyBuilder, timeout(5000).times(1)).scheduleListenerRestart();
        verify(spiedLinkstateTopologyBuilder, times(2)).restartTransactionChainOnDemand();
        assertNotEquals(0L, spiedLinkstateTopologyBuilder.listenerScheduledRestartTime);
        assertEquals(0, spiedLinkstateTopologyBuilder.listenerScheduledRestartEnforceCounter);
        final long listenerScheduledRestartTime = spiedLinkstateTopologyBuilder.listenerScheduledRestartTime;
        // call again with empty change to invoke restartTransactionChainOnDemand()
        spiedLinkstateTopologyBuilder.onDataTreeChanged(new ArrayList<>());
        verify(spiedLinkstateTopologyBuilder, times(3)).restartTransactionChainOnDemand();
        // transaction chain should be reset while listener should not
        verify(spiedLinkstateTopologyBuilder, times(1)).resetTransactionChain();
        verify(spiedLinkstateTopologyBuilder, never()).resetListener();
        // now apply a change with bad modification again
        spiedLinkstateTopologyBuilder.onDataTreeChanged(changes);
        verify(spiedLinkstateTopologyBuilder, times(4)).restartTransactionChainOnDemand();
        // listener scheduled again
        verify(spiedLinkstateTopologyBuilder, timeout(5000).times(2)).scheduleListenerRestart();
        // listener timer shouldn't have changed
        assertEquals(listenerScheduledRestartTime, spiedLinkstateTopologyBuilder.listenerScheduledRestartTime);
        assertEquals(0, spiedLinkstateTopologyBuilder.listenerScheduledRestartEnforceCounter);
        verify(spiedLinkstateTopologyBuilder, times(2)).resetTransactionChain();
        verify(spiedLinkstateTopologyBuilder, never()).resetListener();
        Thread.sleep(LISTENER_RESTART_TIME);
        // manually invoke onTransactionChainFailed() to have the listener restart scheduled again
        spiedLinkstateTopologyBuilder.onTransactionChainFailed(null, null, null);
        assertEquals(spiedLinkstateTopologyBuilder.listenerScheduledRestartTime, listenerScheduledRestartTime
                + LISTENER_RESTART_TIME);
        verify(spiedLinkstateTopologyBuilder, times(5)).restartTransactionChainOnDemand();
        verify(spiedLinkstateTopologyBuilder, times(3)).scheduleListenerRestart();
        // enforce counter get increased
        assertEquals(1, spiedLinkstateTopologyBuilder.listenerScheduledRestartEnforceCounter);
        verify(spiedLinkstateTopologyBuilder, times(3)).resetTransactionChain();
        verify(spiedLinkstateTopologyBuilder, never()).resetListener();
        // sleep to let the listener restart timer times out
        Thread.sleep(LISTENER_RESTART_TIME);
        // apply a good modification (empty change)
        spiedLinkstateTopologyBuilder.onDataTreeChanged(new ArrayList<>());
        assertEquals(0, spiedLinkstateTopologyBuilder.listenerScheduledRestartTime);
        assertEquals(0, spiedLinkstateTopologyBuilder.listenerScheduledRestartEnforceCounter);
        verify(spiedLinkstateTopologyBuilder, times(6)).restartTransactionChainOnDemand();
        // listener restarted didn't get rescheduled again
        verify(spiedLinkstateTopologyBuilder, times(3)).scheduleListenerRestart();
        verify(spiedLinkstateTopologyBuilder, times(4)).resetTransactionChain();
        verify(spiedLinkstateTopologyBuilder, times(1)).resetListener();
    }

    private static String getLinkstateRouteKey(final String routeKey) {
        return Unpooled.wrappedBuffer(StandardCharsets.UTF_8.encode(routeKey)).array().toString();
    }

    private InstanceIdentifier<LinkstateRoute> createLinkstateRouteIID(final String linkstateRouteKey) {
        return tablePathIID.builder().child(LinkstateRoutesCase.class, LinkstateRoutes.class)
                .child(LinkstateRoute.class, new LinkstateRouteKey(new PathId(Uint32.ZERO), linkstateRouteKey)).build();
    }

    private void updateLinkstateRoute(final InstanceIdentifier<LinkstateRoute> linkstateRouteIID,
            final LinkstateRoute data) {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, linkstateRouteIID, data);
        wTx.commit();
    }

    private void removeLinkstateRoute(final InstanceIdentifier<LinkstateRoute> linkstateRouteIID) {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, linkstateRouteIID);
        wTx.commit();
    }

    private LinkstateRoute createLinkstateNodeRoute(final ProtocolId protocolId, final String nodeName,
            final AsNumber asNumber, final String ipv4RouterId) {
        return createBaseBuilder(linkstateNodeRouteKey, protocolId)
                .setObjectType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate
                        .rev200120.linkstate.object.type.NodeCaseBuilder()
                        .setNodeDescriptors(new NodeDescriptorsBuilder()
                                .setCRouterIdentifier(new IsisNodeCaseBuilder().setIsisNode(new IsisNodeBuilder()
                                        .setIsoSystemId(new IsoSystemIdentifier(new byte[]{0, 0, 1, 2, 3, 4})).build())
                                        .build())
                                .setAsNumber(asNumber).build()).build())
                .setAttributes(new AttributesBuilder()
                        .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
                        .addAugmentation(new Attributes1Builder()
                                .setLinkStateAttribute(new NodeAttributesCaseBuilder()
                                        .setNodeAttributes(new NodeAttributesBuilder()
                                                .setDynamicHostname(nodeName)
                                                .setIpv4RouterId(new Ipv4RouterIdentifier(ipv4RouterId))
                                                .setIsisAreaId(Set.of(new IsisAreaIdentifier(new byte[]{0x47})))
                                                .setSrCapabilities(new SrCapabilitiesBuilder()
                                                        .setRangeSize(new Uint24(Uint32.valueOf(SRGB_RANGE)))
                                                        .setSidLabelIndex(new LocalLabelCaseBuilder()
                                                                .setLocalLabel(
                                                                        new MplsLabel(Uint32.valueOf(SRGB_START)))
                                                                .build())
                                                        .build())
                                                .build()).build()).build()).build()).build();
    }

    private LinkstateRoute createLinkstatePrefixRoute(final ProtocolId protocolId, final AsNumber asNumber,
            final String ipv4Prefix, final long igpMetric, final String ospfFwdAddress) {
        return createBaseBuilder(linkstatePrefixRouteKey, protocolId)
            .setObjectType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120
                    .linkstate.object.type.PrefixCaseBuilder()
                .setAdvertisingNodeDescriptors(new AdvertisingNodeDescriptorsBuilder()
                        .setCRouterIdentifier(new IsisNodeCaseBuilder().setIsisNode(new IsisNodeBuilder()
                                .setIsoSystemId(new IsoSystemIdentifier(new byte[]{0, 0, 1, 2, 3, 4})).build())
                                .build()).setAsNumber(asNumber).build())
                .setPrefixDescriptors(new PrefixDescriptorsBuilder()
                        .setIpReachabilityInformation(new IpPrefix(new Ipv4Prefix(ipv4Prefix))).build()).build())
            .setAttributes(new AttributesBuilder()
                .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
                .addAugmentation(new Attributes1Builder()
                        .setLinkStateAttribute(new PrefixAttributesCaseBuilder()
                            .setPrefixAttributes(new PrefixAttributesBuilder()
                                .setOspfForwardingAddress(new IpAddressNoZone(new Ipv4AddressNoZone(ospfFwdAddress)))
                                .setPrefixMetric(new IgpMetric(Uint32.valueOf(igpMetric)))
                                .setSrPrefix(new SrPrefixBuilder()
                                        .setAlgorithm(Algorithm.ShortestPathFirst)
                                        .setSidLabelIndex(new SidCaseBuilder()
                                                .setSid(Uint32.valueOf(NODE_SID_INDEX)).build())
                                        .build())
                                .build())
                            .build())
                        .build())
                .build())
            .build();
    }

    private LinkstateRoute createLinkstateLinkRoute(final ProtocolId protocolId, final AsNumber localAs,
            final AsNumber remoteAs, final String linkName) {
        return createBaseBuilder(linkstateLinkRouteKey, protocolId)
                .setObjectType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate
                        .rev200120.linkstate.object.type.LinkCaseBuilder()
                        .setLocalNodeDescriptors(new LocalNodeDescriptorsBuilder().setAsNumber(localAs)
                                .setCRouterIdentifier(new IsisNodeCaseBuilder().setIsisNode(new IsisNodeBuilder()
                                        .setIsoSystemId(new IsoSystemIdentifier(new byte[]{0, 0, 1, 2, 3, 4}))
                                        .build()).build()).build())
                        .setRemoteNodeDescriptors(new RemoteNodeDescriptorsBuilder().setAsNumber(remoteAs).build())
                        .setLinkDescriptors(new LinkDescriptorsBuilder()
                                .setMultiTopologyId(new TopologyIdentifier(Uint16.ONE)).build()).build())
                .setAttributes(new AttributesBuilder()
                        .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
                        .addAugmentation(new Attributes1Builder()
                                .setLinkStateAttribute(new LinkAttributesCaseBuilder()
                                    .setLinkAttributes(new LinkAttributesBuilder()
                                        .setSharedRiskLinkGroups(ImmutableSet.of(
                                            new SrlgId(Uint32.valueOf(5)), new SrlgId(Uint32.valueOf(15))))
                                        .setAdminGroup(new AdministrativeGroup(Uint32.ZERO))
                                        .setMaxLinkBandwidth(
                                                new Bandwidth(new byte[]{0x00, 0x00, (byte) 0xff, (byte) 0xff}))
                                        .setMaxReservableBandwidth(
                                                new Bandwidth(new byte[]{0x00, 0x00, (byte) 0xff, (byte) 0x1f}))
                                        .setUnreservedBandwidth(BindingMap.of(new UnreservedBandwidthBuilder()
                                                .withKey(new UnreservedBandwidthKey(Uint8.ONE))
                                                .setBandwidth(new Bandwidth(new byte[]{0x00, 0x00, 0x00, (byte) 0xff}))
                                                .build()))
                                        .setTeMetric(new TeMetric(Uint32.valueOf(100)))
                                        .setLinkName(linkName)
                                        .setSrAdjIds(List.of(new SrAdjIdsBuilder()
                                                .setSidLabelIndex(new LocalLabelCaseBuilder()
                                                        .setLocalLabel(new MplsLabel(Uint32.valueOf(ADJ_SID)))
                                                        .build())
                                                .build()))
                                        .build())
                                    .build())
                                .build())
                        .build())
                .build();
    }

    private static LinkstateRouteBuilder createBaseBuilder(final String linkstateRouteKey,
            final ProtocolId protocolId) {
        return new LinkstateRouteBuilder()
            .setIdentifier(IDENTIFIER)
            .withKey(new LinkstateRouteKey(new PathId(Uint32.ZERO), linkstateRouteKey))
            .setRouteKey(linkstateRouteKey)
            .setProtocolId(protocolId);
    }
}
