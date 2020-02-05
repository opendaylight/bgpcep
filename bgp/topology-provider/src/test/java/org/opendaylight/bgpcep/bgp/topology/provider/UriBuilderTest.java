/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public class UriBuilderTest {
    private static final RouteDistinguisher DISTINGUISHER
            = RouteDistinguisherBuilder.getDefaultInstance("1.2.3.4:258");

    @Test
    public void test() {
        final LinkstateRouteBuilder routeB = new LinkstateRouteBuilder().setProtocolId(ProtocolId.Direct)
                .setIdentifier(new Identifier(Uint64.TEN));
        final UriBuilder a = new UriBuilder(routeB.build());
        assertEquals("bgpls://Direct:10/", a.toString());

        routeB.setRouteDistinguisher(DISTINGUISHER);
        final UriBuilder b = new UriBuilder(routeB.build());
        assertEquals("bgpls://1.2.3.4:258:Direct:10/", b.toString());

        final UriBuilder c = new UriBuilder(b, "foo");
        assertEquals("bgpls://1.2.3.4:258:Direct:10/type=foo", c.toString());

        a.add("foo", 25L);
        assertEquals("bgpls://Direct:10/&foo=25", a.toString());

        final LinkCaseBuilder linkB = new LinkCaseBuilder();
        linkB.setLinkDescriptors(new LinkDescriptorsBuilder()
                .setIpv4InterfaceAddress(new Ipv4InterfaceIdentifier("127.0.0.1"))
                .setIpv4NeighborAddress(new Ipv4InterfaceIdentifier("20.20.20.20"))
                .setMultiTopologyId(new TopologyIdentifier(Uint16.valueOf(55)))
                .setLinkLocalIdentifier(Uint32.ONE)
                .setLinkRemoteIdentifier(Uint32.TWO)
                .build());
        final LocalNodeDescriptorsBuilder nodeB = new LocalNodeDescriptorsBuilder();
        nodeB.setAsNumber(new AsNumber(Uint32.valueOf(12))).setDomainId(new DomainIdentifier(Uint32.valueOf(15)))
                .setAreaId(new AreaIdentifier(Uint32.valueOf(17)));
        nodeB.setCRouterIdentifier(new OspfNodeCaseBuilder().setOspfNode(new OspfNodeBuilder()
                .setOspfRouterId(Uint32.valueOf(22)).build()).build());
        linkB.setLocalNodeDescriptors(nodeB.build());
        final RemoteNodeDescriptorsBuilder nodeR = new RemoteNodeDescriptorsBuilder();
        nodeR.setCRouterIdentifier(new IsisPseudonodeCaseBuilder().setIsisPseudonode(new IsisPseudonodeBuilder()
                .setIsIsRouterIdentifier(new IsIsRouterIdentifierBuilder()
                        .setIsoSystemId(new IsoSystemIdentifier(new byte[]{1, 2, 3, 4, 5, 6}))
                        .build()).setPsn(Uint8.TWO).build()).build());
        linkB.setRemoteNodeDescriptors(nodeR.build());
        c.add(linkB.build());
        assertEquals("bgpls://1.2.3.4:258:Direct:10/type=foo&local-as=12&local-domain=15&local-area=17&"
                + "local-router=22&remote-router=0102.0304.0506.02&ipv4-iface=127.0.0.1&ipv4-neigh=20.20.20.20&m"
                + "t=55&local-id=1&remote-id=2", c.toString());
    }
}
