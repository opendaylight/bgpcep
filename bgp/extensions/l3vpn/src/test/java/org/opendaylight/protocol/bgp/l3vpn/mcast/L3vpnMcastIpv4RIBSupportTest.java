/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.mcast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.bgp.rib.rib.loc.rib.tables.routes.L3vpnMcastRoutesIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.bgp.rib.rib.loc.rib.tables.routes.L3vpnMcastRoutesIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.L3vpnMcastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.L3vpnMcastRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.L3vpnMcastRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.ipv4.L3vpnMcastRoutesIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.ipv4.L3vpnMcastRoutesIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationL3vpnMcastIpv4AdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationL3vpnMcastIpv4AdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.l3vpn.mcast.ipv4.advertized._case.DestinationIpv4L3vpnMcastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationL3vpnMcastIpv4WithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationL3vpnMcastIpv4WithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

public class L3vpnMcastIpv4RIBSupportTest extends AbstractRIBSupportTest<L3vpnMcastRoutesIpv4Case ,L3vpnMcastRoutesIpv4,
        L3vpnMcastRoute> {
    private static final PathId PATH_ID = new PathId(Uint32.ZERO);
    private static final IpPrefix IPV4_PREFIX = new IpPrefix(new Ipv4Prefix("34.1.22.0/24"));
    private static final RouteDistinguisher RD = new RouteDistinguisher(new RdIpv4("1.2.3.4:258"));
    private static final L3vpnMcastRouteKey ROUTE_KEY
            = new L3vpnMcastRouteKey(PATH_ID, "IAABAQIDBAECGCIBFgAAAAA=");
    private static final L3vpnMcastRoute ROUTE = new L3vpnMcastRouteBuilder()
            .setRouteKey(ROUTE_KEY.getRouteKey())
            .setPathId(ROUTE_KEY.getPathId())
            .setAttributes(ATTRIBUTES)
            .setRouteDistinguisher(RD)
            .setPrefix(IPV4_PREFIX)
            .build();
    private static final L3vpnMcastRoutesIpv4 MCAST_L3VPN_ROUTES = new L3vpnMcastRoutesIpv4Builder()
            .setL3vpnMcastRoute(Map.of(ROUTE.key(), ROUTE))
            .build();

    private static final L3vpnMcastDestination MCAST_L3VPN_DESTINATION = new L3vpnMcastDestinationBuilder()
            .setRouteDistinguisher(RD)
            .setPrefix(IPV4_PREFIX)
            .setPathId(PATH_ID)
            .build();
    private static final DestinationL3vpnMcastIpv4AdvertizedCase REACH_NLRI
            = new DestinationL3vpnMcastIpv4AdvertizedCaseBuilder()
            .setDestinationIpv4L3vpnMcast(new DestinationIpv4L3vpnMcastBuilder()
                    .setL3vpnMcastDestination(Collections.singletonList(MCAST_L3VPN_DESTINATION)).build()).build();
    private static final DestinationL3vpnMcastIpv4WithdrawnCase UNREACH_NLRI
            = new DestinationL3vpnMcastIpv4WithdrawnCaseBuilder()
            .setDestinationIpv4L3vpnMcast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .l3vpn.mcast.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.l3vpn.mcast.ipv4.withdrawn._case.DestinationIpv4L3vpnMcastBuilder()
                    .setL3vpnMcastDestination(Collections.singletonList(MCAST_L3VPN_DESTINATION)).build()).build();

    private L3VpnMcastIpv4RIBSupport ribSupport;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ribSupport = new L3VpnMcastIpv4RIBSupport(adapter.currentSerializer());
        setUpTestCustomizer(ribSupport);
    }

    @Test
    public void testDeleteRoutes() {
        final ContainerNode withdraw = createNlriWithDrawnRoute(UNREACH_NLRI);
        ribSupport.deleteRoutes(tx, getTablePath(), withdraw);
        final InstanceIdentifier<L3vpnMcastRoute> instanceIdentifier = deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(L3vpnMcastRoute.class));
    }

    @Test
    public void testPutRoutes() {
        ribSupport.putRoutes(tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final L3vpnMcastRoute route = (L3vpnMcastRoute) insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }

    @Test
    public void testEmptyRoute() {
        assertEquals(createEmptyTable(), ribSupport.emptyTable());
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Collection<MapEntryNode> routes = createRoutes(MCAST_L3VPN_ROUTES);
        final Update update = ribSupport.buildUpdate(Collections.emptyList(), routes, ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().augmentation(AttributesUnreach.class).getMpUnreachNlri()
                .getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(AttributesReach.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Collection<MapEntryNode> routes = createRoutes(MCAST_L3VPN_ROUTES);
        final Update update = ribSupport.buildUpdate(routes, Collections.emptyList(), ATTRIBUTES);
        assertEquals(REACH_NLRI, update.getAttributes().augmentation(AttributesReach.class).getMpReachNlri()
                .getAdvertizedRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(AttributesUnreach.class));
    }

    @Test
    public void testCacheableNlriObjects() {
        assertEquals(ImmutableSet.of(L3vpnMcastRoutesIpv4Case.class), ribSupport.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        assertEquals(ImmutableSet.of(), ribSupport.cacheableAttributeObjects());
    }

    @Test
    public void testRoutePath() {
        final NodeIdentifierWithPredicates prefixNii = createRouteNIWP(MCAST_L3VPN_ROUTES);
        final YangInstanceIdentifier expected = getRoutePath().node(prefixNii);
        final YangInstanceIdentifier actual = ribSupport.routePath(getTablePath(), prefixNii);
        assertEquals(expected, actual);
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(Attributes.QNAME.bindTo(L3vpnMcastRoutesIpv4Case.QNAME.getModule())),
            ribSupport.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(L3vpnMcastRoutesIpv4Case.class, ribSupport.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(L3vpnMcastRoutesIpv4.class, ribSupport.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(L3vpnMcastRoute.class, ribSupport.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new L3vpnMcastRoutesIpv4CaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes
                = new L3vpnMcastRoutesIpv4CaseBuilder()
                .setL3vpnMcastRoutesIpv4(new L3vpnMcastRoutesIpv4Builder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes routes = new L3vpnMcastRoutesIpv4CaseBuilder().setL3vpnMcastRoutesIpv4(MCAST_L3VPN_ROUTES).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = ribSupport.changedRoutes(tree);
        assertFalse(result.isEmpty());
    }
}
