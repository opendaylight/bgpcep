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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.LinkAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.routes.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.routes.linkstate.routes.linkstate.route.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.Graph;
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

    private LinkstateGraphBuilder builder;

    @Before
    public void setUp() {
        doReturn(connectedGraph).when(connectedGraphProvider).createConnectedGraph(eq("ted://test-topology"),
            eq(Graph.DomainScope.IntraDomain));
        builder = new LinkstateGraphBuilder(dataBroker, ribReference, TOPOLOGY_ID, connectedGraphProvider);
    }

    @Test
    public void testIpv6ToKey() {
        assertEquals(Uint64.valueOf(0x08090A0B0C0D0E0FL),
            LinkstateGraphBuilder.ipv6ToKey(new Ipv6InterfaceIdentifier("0001:0203:0405:0607:0809:0A0B:0C0D:0E0F")));
    }

    @Test
    public void testCreateEdgeNoPrefix() {
        final LinkstateRoute route = createLinkstateRoute(null);
        builder.createObject(null, null, route);
        verify(connectedGraph, never()).addEdge(any());
    }

    @Test
    public void testCreateEdgeIpv4Prefix() {
        final LinkstateRoute route = createLinkstateRoute("192.168.1.1");
        builder.createObject(null, null, route);
        verify(connectedGraph, never()).addEdge(any());
    }

    @Test
    public void testCreateEdgeIpv6Prefix() {
        final LinkstateRoute route = createLinkstateRoute("2001:db8::1");
        builder.createObject(null, null, route);
        verify(connectedGraph, never()).addEdge(any());
    }

    private static LinkstateRoute createLinkstateRoute(final String prefix) {
        final var linkDescriptorsBuilder = new LinkDescriptorsBuilder()
            .setLinkLocalIdentifier(Uint32.valueOf(123));
        if (prefix != null) {
            linkDescriptorsBuilder.setIpv4InterfaceAddress(new Ipv4InterfaceIdentifier(prefix));
        }
        final var linkCase = new LinkCaseBuilder()
            .setLocalNodeDescriptors(new LocalNodeDescriptorsBuilder().build())
            .setRemoteNodeDescriptors(new RemoteNodeDescriptorsBuilder().build())
            .setLinkDescriptors(linkDescriptorsBuilder.build())
            .build();
        final var linkAttributesCase = new LinkAttributesCaseBuilder()
            .setLinkAttributes(new LinkAttributesBuilder().build())
            .build();
        final var attributes = new AttributesBuilder()
            .addAugmentation(new Attributes1Builder().setLinkStateAttribute(linkAttributesCase).build())
            .build();
        return new LinkstateRouteBuilder()
            .setPathId(new PathId(Uint32.ZERO))
            .setRouteKey("route-key")
            .setObjectType(linkCase)
            .setAttributes(attributes)
            .build();
    }
}
