/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.target.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.protocol.bgp.route.target.impl.activators.NlriActivator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.bgp.rib.rib.loc.rib.tables.routes.RouteTargetRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.bgp.rib.rib.loc.rib.tables.routes.RouteTargetRoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.RouteTargetChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.destination.RouteTargetDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.destination.RouteTargetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetAs4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.routes.RouteTargetRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.routes.RouteTargetRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.routes.route.target.routes.RouteTargetRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.routes.route.target.routes.RouteTargetRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.routes.route.target.routes.RouteTargetRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetAdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetAdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.route.target.advertized._case.DestinationRouteTargetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetWithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetWithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.as._4.route.target.extended.community.grouping.As4RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.as._4.spec.common.As4SpecificCommon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.as._4.spec.common.As4SpecificCommonBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;

public class RouteTargetRIBSupportTest extends AbstractRIBSupportTest<RouteTargetRoutesCase, RouteTargetRoutes,
        RouteTargetRoute, RouteTargetRouteKey> {
    private static final PathId PATH_ID = new PathId(0L);
    private static final As4SpecificCommon AS_COMMON = new As4SpecificCommonBuilder()
            .setAsNumber(new AsNumber(20L))
            .setLocalAdministrator(100)
            .build();
    private static final RouteTargetChoice RT = new RouteTargetAs4ExtendedCommunityCaseBuilder()
            .setAs4RouteTargetExtendedCommunity(new As4RouteTargetExtendedCommunityBuilder()
                    .setAs4SpecificCommon(AS_COMMON).build()).build();
    private static final RouteTargetRouteKey ROUTE_KEY
            = new RouteTargetRouteKey(PATH_ID, "AAAAFABk");
    private static final RouteTargetRoute ROUTE = new RouteTargetRouteBuilder()
            .setRouteKey(ROUTE_KEY.getRouteKey())
            .setPathId(ROUTE_KEY.getPathId())
            .setAttributes(ATTRIBUTES)
            .setRouteTargetChoice(RT)
            .build();
    private static final RouteTargetRoutes RT_ROUTES
            = new RouteTargetRoutesBuilder().setRouteTargetRoute(Collections.singletonList(ROUTE)).build();

    private static final RouteTargetDestination RT_DESTINATION = new RouteTargetDestinationBuilder()
            .setRouteTargetChoice(RT)
            .setPathId(PATH_ID)
            .build();
    private static final DestinationRouteTargetAdvertizedCase REACH_NLRI
            = new DestinationRouteTargetAdvertizedCaseBuilder()
            .setDestinationRouteTarget(new DestinationRouteTargetBuilder()
                    .setRouteTargetDestination(Collections.singletonList(RT_DESTINATION)).build()).build();
    private static final DestinationRouteTargetWithdrawnCase UNREACH_NLRI
            = new DestinationRouteTargetWithdrawnCaseBuilder()
            .setDestinationRouteTarget(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route
                    .target.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination
                    .route.target.withdrawn._case.DestinationRouteTargetBuilder()
                    .setRouteTargetDestination(Collections.singletonList(RT_DESTINATION)).build()).build();

    private RouteTargetRIBSupport ribSupport;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        NlriActivator.registerNlriParsers(new ArrayList<>());
        this.ribSupport = RouteTargetRIBSupport.getInstance(this.mappingService);
        setUpTestCustomizer(this.ribSupport);
    }

    @Test
    public void testDeleteRoutes() {
        final ContainerNode withdraw = createNlriWithDrawnRoute(UNREACH_NLRI);
        this.ribSupport.deleteRoutes(this.tx, getTablePath(), withdraw);
        final InstanceIdentifier<RouteTargetRoute> instanceIdentifier = this.deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(RouteTargetRoute.class));
    }

    @Test
    public void testPutRoutes() {
        this.ribSupport.putRoutes(this.tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final RouteTargetRoute route = (RouteTargetRoute) this.insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }


    @Test
    public void testEmptyRoute() {
        final Routes empty = new RouteTargetRoutesCaseBuilder().setRouteTargetRoutes(new RouteTargetRoutesBuilder()
                .setRouteTargetRoute(Collections.emptyList()).build()).build();
        assertEquals(createEmptyTable(empty), this.ribSupport.emptyTable());
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Collection<MapEntryNode> routes = createRoutes(RT_ROUTES);
        final Update update = this.ribSupport.buildUpdate(Collections.emptyList(), routes, ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().augmentation(Attributes2.class).getMpUnreachNlri()
                .getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(Attributes1.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Collection<MapEntryNode> routes = createRoutes(RT_ROUTES);
        final Update update = this.ribSupport.buildUpdate(routes, Collections.emptyList(), ATTRIBUTES);
        assertEquals(REACH_NLRI, update.getAttributes().augmentation(Attributes1.class).getMpReachNlri()
                .getAdvertizedRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(Attributes2.class));
    }

    @Test
    public void testCacheableNlriObjects() {
        assertEquals(ImmutableSet.of(RouteTargetRoutesCase.class), this.ribSupport.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        assertEquals(ImmutableSet.of(), this.ribSupport.cacheableAttributeObjects());
    }

    @Test
    public void testRouteIdAddPath() {
        assertEquals(ROUTE_KEY, this.ribSupport.createRouteListKey(ROUTE_KEY.getPathId().getValue(),
                ROUTE_KEY.getRouteKey()));
    }

    @Test
    public void testRoutePath() {
        final YangInstanceIdentifier.NodeIdentifierWithPredicates prefixNii = createRouteNIWP(RT_ROUTES);
        final YangInstanceIdentifier expected = getRoutePath().node(prefixNii);
        final YangInstanceIdentifier actual = this.ribSupport.routePath(getTablePath().node(Routes.QNAME), prefixNii);
        assertEquals(expected, actual);
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new YangInstanceIdentifier.NodeIdentifier(
                        Attributes.QNAME.withModule(BindingReflections.getQNameModule(RouteTargetRoutesCase.class))),
                this.ribSupport.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(RouteTargetRoutesCase.class, this.ribSupport.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(RouteTargetRoutes.class, this.ribSupport.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(RouteTargetRoute.class, this.ribSupport.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new RouteTargetRoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        assertTrue(this.ribSupport.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes = new RouteTargetRoutesCaseBuilder()
                .setRouteTargetRoutes(new RouteTargetRoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        assertTrue(this.ribSupport.changedRoutes(tree).isEmpty());

        final Routes routes = new RouteTargetRoutesCaseBuilder().setRouteTargetRoutes(RT_ROUTES).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = this.ribSupport.changedRoutes(tree);
        assertFalse(result.isEmpty());
    }
}