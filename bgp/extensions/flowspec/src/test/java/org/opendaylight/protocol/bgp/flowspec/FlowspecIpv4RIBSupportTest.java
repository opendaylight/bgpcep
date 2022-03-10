/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.SAFI;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.bgp.rib.rib.loc.rib.tables.routes.FlowspecRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.bgp.rib.rib.loc.rib.tables.routes.FlowspecRoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.ipv4.DestinationFlowspecIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.ipv4.DestinationFlowspecIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.ipv4.route.FlowspecRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.ipv4.route.FlowspecRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.routes.FlowspecRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

public class FlowspecIpv4RIBSupportTest extends AbstractRIBSupportTest<FlowspecRoutesCase, FlowspecRoutes,
        FlowspecRoute> {

    private FlowspecIpv4RIBSupport ribSupport;
    private FlowspecRoute route;
    private FlowspecRoutes routes;
    private FlowspecRouteKey routeKey;
    private static final PathId PATH_ID = new PathId(Uint32.ONE);

    private static final DestinationPrefixCase DEST_PREFIX = new DestinationPrefixCaseBuilder()
            .setDestinationPrefix(new Ipv4Prefix("10.0.1.0/32")).build();
    private static final List<Flowspec> FLOW_LIST = List.of(new FlowspecBuilder().setFlowspecType(DEST_PREFIX).build());
    private static final DestinationFlowspecIpv4 DEST_FLOW = new DestinationFlowspecIpv4Builder()
            .setFlowspec(FLOW_LIST).setPathId(PATH_ID).build();
    private static final DestinationFlowspecCase REACH_NLRI = new DestinationFlowspecCaseBuilder()
            .setDestinationFlowspecIpv4(DEST_FLOW).build();
    private static final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCase UNREACH_NLRI =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCaseBuilder()
            .setDestinationFlowspecIpv4(DEST_FLOW).build();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final BGPActivator act = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        act.start(context);
        ribSupport = new FlowspecIpv4RIBSupport(adapter.currentSerializer());

        final SimpleFlowspecIpv4NlriParser parser = new SimpleFlowspecIpv4NlriParser(SAFI.FLOWSPEC);

        routeKey = new FlowspecRouteKey(PATH_ID, parser.stringNlri(FLOW_LIST));
        route = new FlowspecRouteBuilder().withKey(routeKey).setPathId(PATH_ID).setFlowspec(FLOW_LIST)
                .setAttributes(new AttributesBuilder().build()).build();
        routes = new FlowspecRoutesBuilder().setFlowspecRoute(Map.of(route.key(), route)).build();
        setUpTestCustomizer(ribSupport);
    }

    @Test
    public void testDeleteRoutes() {
        ribSupport.deleteRoutes(tx, getTablePath(), createNlriWithDrawnRoute(UNREACH_NLRI));
        final InstanceIdentifier<FlowspecRoute> instanceIdentifier = deletedRoutes.get(0);
        assertEquals(routeKey, instanceIdentifier.firstKeyOf(FlowspecRoute.class));
    }

    @Test
    public void testPutRoutes() {
        ribSupport.putRoutes(tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        assertEquals(route, insertedRoutes.get(0).getValue());
    }

    @Test
    public void testEmptyRoute() {
        assertEquals(createEmptyTable(), ribSupport.emptyTable());
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Update update = ribSupport.buildUpdate(List.of(), createRoutes(routes), ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().augmentation(AttributesUnreach.class)
            .getMpUnreachNlri().getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(AttributesReach.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Update update = ribSupport.buildUpdate(createRoutes(routes), List.of(), ATTRIBUTES);
        assertEquals(REACH_NLRI, update.getAttributes().augmentation(AttributesReach.class).getMpReachNlri()
                .getAdvertizedRoutes().getDestinationType());
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
        final NodeIdentifierWithPredicates prefixNii = createRouteNIWP(routes);
        assertEquals(getRoutePath().node(prefixNii), ribSupport.routePath(getTablePath(), prefixNii));
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(QName.create(FlowspecRoutes.QNAME,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables
                    .Attributes.QNAME.getLocalName().intern())),
            ribSupport.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(FlowspecRoutesCase.class, ribSupport.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(FlowspecRoutes.class, ribSupport.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(FlowspecRoute.class, ribSupport.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new FlowspecRoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes
                = new FlowspecRoutesCaseBuilder().setFlowspecRoutes(new FlowspecRoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(new FlowspecRoutesCaseBuilder()
            .setFlowspecRoutes(new FlowspecRoutesBuilder().setFlowspecRoute(Map.of(route.key(), route))
                .build()).build())).getRootNode();
        final Collection<DataTreeCandidateNode> result = ribSupport.changedRoutes(tree);
        assertFalse(result.isEmpty());
    }
}
