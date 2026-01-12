/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv6InterfaceIdentifier;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.LinkAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.routes.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.routes.linkstate.routes.linkstate.route.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class LinkstateGraphBuilderTest {
    private static final TopologyId TOPOLOGY_ID = new TopologyId("test-topology");

    @Mock
    private DataBroker dataBroker;
    @Mock
    private RibReference ribReference;
    @Mock
    private ConnectedGraphProvider connectedGraphProvider;
    @Mock
    private ConnectedGraph connectedGraph;
    @Mock
    private ConnectedEdge connectedEdge;

    private LinkstateGraphBuilder graphBuilder;

    @Before
    public void setUp() {
        doReturn(connectedGraph).when(connectedGraphProvider)
            .createConnectedGraph(eq("ted://" + TOPOLOGY_ID.getValue()), eq(Graph.DomainScope.IntraDomain));
        doReturn(connectedEdge).when(connectedGraph).addEdge(any(Edge.class));
        doNothing().when(connectedGraph).addPrefix(any());
        graphBuilder = new LinkstateGraphBuilder(dataBroker, ribReference, TOPOLOGY_ID, connectedGraphProvider);
    }

    @Test
    public void testIpv6ToKey() {
        assertEquals(Uint64.valueOf(0x08090A0B0C0D0E0FL),
            LinkstateGraphBuilder.ipv6ToKey(new Ipv6InterfaceIdentifier("0001:0203:0405:0607:0809:0A0B:0C0D:0E0F")));
    }

    /**
     * Tests that edge without any prefix is NOT added to graph.
     */
    @Test
    public void testCreateEdgeNoPrefix() {
        final var descriptorsBuilder = new LinkDescriptorsBuilder().setLinkLocalIdentifier(Uint32.valueOf(123));
        final var route = createLinkstateRoute(descriptorsBuilder.build());
        graphBuilder.createObject(null, null, route);
        verify(connectedGraph, never()).addEdge(any());
        verify(connectedGraph, never()).addPrefix(any());
    }

    /**
     * Tests that edge with an IPv4 prefix is added to graph.
     */
    @Test
    public void testCreateEdgeIpv4Prefix() {
        final var descriptorsBuilder = new LinkDescriptorsBuilder()
            .setLinkLocalIdentifier(Uint32.valueOf(123))
            .setIpv4InterfaceAddress(new Ipv4InterfaceIdentifier("192.168.1.1"));
        final var route = createLinkstateRoute(descriptorsBuilder.build());
        graphBuilder.createObject(null, null, route);
        verify(connectedGraph).addEdge(any());
        verify(connectedGraph).addPrefix(any());
    }

    /**
     * Tests that edge with an IPv6 prefix is added to graph.
     */
    @Test
    public void testCreateEdgeIpv6Prefix() {
        final var descriptorsBuilder = new LinkDescriptorsBuilder()
            .setLinkLocalIdentifier(Uint32.valueOf(123))
            .setIpv6InterfaceAddress(new Ipv6InterfaceIdentifier("2001:db8::1"));
        final var route = createLinkstateRoute(descriptorsBuilder.build());
        graphBuilder.createObject(null, null, route);
        verify(connectedGraph).addEdge(any());
        verify(connectedGraph).addPrefix(any());
    }

    private static LinkstateRoute createLinkstateRoute(final LinkDescriptors descriptors) {
        final var linkCase = new LinkCaseBuilder()
            .setLocalNodeDescriptors(new LocalNodeDescriptorsBuilder()
                .setCRouterIdentifier(new OspfNodeCaseBuilder()
                    .setOspfNode(new OspfNodeBuilder().setOspfRouterId(Uint32.ONE).build())
                    .build())
                .build())
            .setRemoteNodeDescriptors(new RemoteNodeDescriptorsBuilder()
                .setCRouterIdentifier(new OspfNodeCaseBuilder()
                    .setOspfNode(new OspfNodeBuilder().setOspfRouterId(Uint32.ONE).build())
                    .build())
                .build())
            .setLinkDescriptors(descriptors)
            .build();
        final var linkAttributesCase = new LinkAttributesCaseBuilder()
            .setLinkAttributes(new LinkAttributesBuilder().build())
            .build();
        final var attributes = new AttributesBuilder()
            .addAugmentation(new Attributes1Builder().setLinkStateAttribute(linkAttributesCase).build())
            .build();
        return new LinkstateRouteBuilder()
            .setPathId(new PathId(Uint32.ONE))
            .setRouteKey("route-key")
            .setObjectType(linkCase)
            .setAttributes(attributes)
            .build();
    }
}
