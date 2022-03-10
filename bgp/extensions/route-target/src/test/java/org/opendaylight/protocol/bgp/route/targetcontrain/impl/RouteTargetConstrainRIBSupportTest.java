/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.bgp.rib.rib.loc.rib.tables.routes.RouteTargetConstrainRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.bgp.rib.rib.loc.rib.tables.routes.RouteTargetConstrainRoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.RouteTargetConstrainChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.destination.RouteTargetConstrainDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.destination.RouteTargetConstrainDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainAs4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.RouteTargetConstrainRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.RouteTargetConstrainRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.route.target.constrain.routes.RouteTargetConstrainRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.route.target.constrain.routes.RouteTargetConstrainRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.route.target.constrain.routes.RouteTargetConstrainRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetConstrainAdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetConstrainAdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.route.target.constrain.advertized._case.DestinationRouteTargetConstrainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetConstrainWithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetConstrainWithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.route.target.extended.community.grouping.As4RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.spec.common.As4SpecificCommon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.spec.common.As4SpecificCommonBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

public class RouteTargetConstrainRIBSupportTest extends AbstractRIBSupportTest<RouteTargetConstrainRoutesCase,
        RouteTargetConstrainRoutes, RouteTargetConstrainRoute> {
    private static final AsNumber ORIGIN_AS = new AsNumber(Uint32.valueOf(72));
    private static final PathId PATH_ID = new PathId(Uint32.ZERO);
    private static final As4SpecificCommon AS_COMMON = new As4SpecificCommonBuilder()
            .setAsNumber(new AsNumber(Uint32.valueOf(20)))
            .setLocalAdministrator(Uint16.valueOf(100))
            .build();
    private static final RouteTargetConstrainChoice RT = new RouteTargetConstrainAs4ExtendedCommunityCaseBuilder()
            .setAs4RouteTargetExtendedCommunity(new As4RouteTargetExtendedCommunityBuilder()
                    .setAs4SpecificCommon(AS_COMMON).build())
            .build();
    private static final RouteTargetConstrainRouteKey ROUTE_KEY
            = new RouteTargetConstrainRouteKey(PATH_ID, "AgIAAAAUAGQ=");
    private static final RouteTargetConstrainRoute ROUTE = new RouteTargetConstrainRouteBuilder()
            .setRouteKey(ROUTE_KEY.getRouteKey())
            .setPathId(ROUTE_KEY.getPathId())
            .setAttributes(ATTRIBUTES)
            .setRouteTargetConstrainChoice(RT)
            .setOriginAs(ORIGIN_AS)
            .build();
    private static final RouteTargetConstrainRoutes RT_ROUTES = new RouteTargetConstrainRoutesBuilder()
            .setRouteTargetConstrainRoute(Map.of(ROUTE.key(), ROUTE))
            .build();

    private static final RouteTargetConstrainDestination RT_DESTINATION = new RouteTargetConstrainDestinationBuilder()
            .setRouteTargetConstrainChoice(RT)
            .setOriginAs(ORIGIN_AS)
            .setPathId(PATH_ID)
            .build();
    private static final DestinationRouteTargetConstrainAdvertizedCase REACH_NLRI
            = new DestinationRouteTargetConstrainAdvertizedCaseBuilder()
            .setDestinationRouteTargetConstrain(new DestinationRouteTargetConstrainBuilder()
                    .setRouteTargetConstrainDestination(List.of(RT_DESTINATION)).build()).build();
    private static final DestinationRouteTargetConstrainWithdrawnCase UNREACH_NLRI
            = new DestinationRouteTargetConstrainWithdrawnCaseBuilder()
            .setDestinationRouteTargetConstrain(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .route.target.constrain.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination
                    .type.destination.route.target.constrain.withdrawn._case.DestinationRouteTargetConstrainBuilder()
                    .setRouteTargetConstrainDestination(List.of(RT_DESTINATION)).build()).build();

    private RouteTargetConstrainRIBSupport ribSupport;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ribSupport = new RouteTargetConstrainRIBSupport(adapter.currentSerializer());
        setUpTestCustomizer(ribSupport);
    }

    @Test
    public void testDeleteRoutes() {
        final ContainerNode withdraw = createNlriWithDrawnRoute(UNREACH_NLRI);
        ribSupport.deleteRoutes(tx, getTablePath(), withdraw);
        final InstanceIdentifier<RouteTargetConstrainRoute> instanceIdentifier = deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(RouteTargetConstrainRoute.class));
    }

    @Test
    public void testPutRoutes() {
        ribSupport.putRoutes(tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final RouteTargetConstrainRoute route = (RouteTargetConstrainRoute) insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }


    @Test
    public void testEmptyRoute() {
        assertEquals(createEmptyTable(), ribSupport.emptyTable());
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Collection<MapEntryNode> routes = createRoutes(RT_ROUTES);
        final Update update = ribSupport.buildUpdate(List.of(), routes, ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().augmentation(AttributesUnreach.class).getMpUnreachNlri()
                .getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(AttributesReach.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Collection<MapEntryNode> routes = createRoutes(RT_ROUTES);
        final Update update = ribSupport.buildUpdate(routes, List.of(), ATTRIBUTES);
        assertEquals(REACH_NLRI, update.getAttributes().augmentation(AttributesReach.class).getMpReachNlri()
                .getAdvertizedRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(AttributesUnreach.class));
    }

    @Test
    public void testCacheableNlriObjects() {
        assertEquals(Set.of(RouteTargetConstrainRoutesCase.class), ribSupport.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        assertEquals(Set.of(), ribSupport.cacheableAttributeObjects());
    }

    @Test
    public void testRoutePath() {
        final YangInstanceIdentifier.NodeIdentifierWithPredicates prefixNii = createRouteNIWP(RT_ROUTES);
        final YangInstanceIdentifier expected = getRoutePath().node(prefixNii);
        final YangInstanceIdentifier actual = ribSupport.routePath(getTablePath(), prefixNii);
        assertEquals(expected, actual);
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(Attributes.QNAME.bindTo(RouteTargetConstrainRoutesCase.QNAME.getModule())),
                ribSupport.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(RouteTargetConstrainRoutesCase.class, ribSupport.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(RouteTargetConstrainRoutes.class, ribSupport.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(RouteTargetConstrainRoute.class, ribSupport.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new RouteTargetConstrainRoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes = new RouteTargetConstrainRoutesCaseBuilder()
                .setRouteTargetConstrainRoutes(new RouteTargetConstrainRoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes routes = new RouteTargetConstrainRoutesCaseBuilder()
                .setRouteTargetConstrainRoutes(RT_ROUTES).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = ribSupport.changedRoutes(tree);
        assertFalse(result.isEmpty());
    }
}
