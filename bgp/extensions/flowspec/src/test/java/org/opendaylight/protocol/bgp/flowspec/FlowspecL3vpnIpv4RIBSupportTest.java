/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4.FlowspecL3vpnIpv4RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdTwoOctetAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

public class FlowspecL3vpnIpv4RIBSupportTest extends AbstractRIBSupportTest<FlowspecL3vpnIpv4RoutesCase,
        FlowspecL3vpnIpv4Routes, FlowspecL3vpnRoute> {

    private static final FlowspecL3vpnRoute ROUTE;
    private static final FlowspecL3vpnRouteKey ROUTE_KEY;
    private static final PathId PATH_ID = new PathId(Uint32.ONE);
    private static final RouteDistinguisher RD = new RouteDistinguisher(new RdTwoOctetAs("0:5:3"));
    private static final DestinationPrefixCase DEST_PREFIX = new DestinationPrefixCaseBuilder()
            .setDestinationPrefix(new Ipv4Prefix("172.17.1.0/24")).build();
    private static final List<Flowspec> FLOW_LIST
            = Collections.singletonList(new FlowspecBuilder().setFlowspecType(DEST_PREFIX).build());
    private static final DestinationFlowspecL3vpnIpv4 DEST_FLOW
            = new DestinationFlowspecL3vpnIpv4Builder().setRouteDistinguisher(RD)
            .setFlowspec(FLOW_LIST).setPathId(PATH_ID).build();
    private static final DestinationFlowspecL3vpnIpv4Case REACH_NLRI
            = new DestinationFlowspecL3vpnIpv4CaseBuilder().setDestinationFlowspecL3vpnIpv4(DEST_FLOW).build();
    private static final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv4Case UNREACH_NLRI
            = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv4CaseBuilder()
            .setDestinationFlowspecL3vpnIpv4(DEST_FLOW).build();
    private FlowspecL3vpnIpv4RIBSupport ribSupport;

    static {
        ROUTE_KEY = new FlowspecL3vpnRouteKey(PATH_ID,
                "[l3vpn with route-distinguisher 0:5:3] all packets to 172.17.1.0/24");
        ROUTE = new FlowspecL3vpnRouteBuilder().withKey(ROUTE_KEY).setPathId(PATH_ID).setFlowspec(FLOW_LIST)
            .setAttributes(new AttributesBuilder().build()).setRouteDistinguisher(RD).build();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ribSupport = new FlowspecL3vpnIpv4RIBSupport(adapter.currentSerializer());
        setUpTestCustomizer(ribSupport);
    }

    @Test
    public void testDeleteRoutes() {
        ribSupport.deleteRoutes(tx, getTablePath(), createNlriWithDrawnRoute(UNREACH_NLRI));
        final InstanceIdentifier<FlowspecL3vpnRoute> instanceIdentifier = deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(FlowspecL3vpnRoute.class));
    }

    @Test
    public void testPutRoutes() {
        ribSupport.putRoutes(tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final FlowspecL3vpnRoute route = (FlowspecL3vpnRoute) insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }

    @Test
    public void testEmptyRoute() {
        assertEquals(createEmptyTable(), ribSupport.emptyTable());
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Update update = ribSupport.buildUpdate(Collections.emptyList(), createRoutes(
            new FlowspecL3vpnIpv4RoutesBuilder().setFlowspecL3vpnRoute(Map.of(ROUTE.key(), ROUTE)).build()),
            ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().augmentation(AttributesUnreach.class)
            .getMpUnreachNlri().getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(AttributesReach.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Update update = ribSupport.buildUpdate(createRoutes(
            new FlowspecL3vpnIpv4RoutesBuilder().setFlowspecL3vpnRoute(Map.of(ROUTE.key(), ROUTE)).build()),
            Collections.emptyList(), ATTRIBUTES);
        final AdvertizedRoutes advertised = update.getAttributes().augmentation(AttributesReach.class).getMpReachNlri()
                .getAdvertizedRoutes();
        assertEquals(REACH_NLRI, advertised.getDestinationType());
        assertNull(update.getAttributes().augmentation(AttributesUnreach.class));
    }

    @Test
    public void testCacheableNlriObjects() {
        assertEquals(Set.of(), ribSupport.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        assertEquals(Set.of(), ribSupport.cacheableAttributeObjects());
    }

    @Test
    public void testRoutePath() {
        final NodeIdentifierWithPredicates prefixNii
                = createRouteNIWP(new FlowspecL3vpnIpv4RoutesBuilder()
                .setFlowspecL3vpnRoute(Map.of(ROUTE.key(), ROUTE)).build());
        assertEquals(getRoutePath().node(prefixNii),
                ribSupport.routePath(getTablePath(), prefixNii));
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(QName.create(FlowspecL3vpnIpv4Routes.QNAME,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables
                    .Attributes.QNAME.getLocalName().intern())),
            ribSupport.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(FlowspecL3vpnIpv4RoutesCase.class, ribSupport.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(FlowspecL3vpnIpv4Routes.class, ribSupport.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(FlowspecL3vpnRoute.class, ribSupport.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new FlowspecL3vpnIpv4RoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        Assert.assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes = new FlowspecL3vpnIpv4RoutesCaseBuilder()
                .setFlowspecL3vpnIpv4Routes(new FlowspecL3vpnIpv4RoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        Assert.assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes routes = new FlowspecL3vpnIpv4RoutesCaseBuilder()
                .setFlowspecL3vpnIpv4Routes(new FlowspecL3vpnIpv4RoutesBuilder()
            .setFlowspecL3vpnRoute(Map.of(ROUTE.key(), ROUTE)).build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = ribSupport.changedRoutes(tree);
        Assert.assertFalse(result.isEmpty());
    }
}
