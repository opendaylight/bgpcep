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
import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4.FlowspecL3vpnIpv4RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.ipv4.route.FlowspecL3vpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RdTwoOctetAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;

public class FlowspecL3vpnIpv4RIBSupportTest extends AbstractRIBSupportTest {

    private static final FlowspecL3vpnIpv4RIBSupport RIB_SUPPORT;
    private static final FlowspecL3vpnRoute ROUTE;
    private static final FlowspecL3vpnRouteKey ROUTE_KEY;
    private static final PathId PATH_ID = new PathId(1L);
    private static final RouteDistinguisher RD = new RouteDistinguisher(new RdTwoOctetAs("0:5:3"));

    private static final DestinationPrefixCase DEST_PREFIX = new DestinationPrefixCaseBuilder()
        .setDestinationPrefix(new Ipv4Prefix("172.17.1.0/24")).build();
    private static final List<Flowspec> FLOW_LIST = Collections.singletonList(new FlowspecBuilder().setFlowspecType(DEST_PREFIX).build());
    private static final DestinationFlowspecL3vpnIpv4 DEST_FLOW = new DestinationFlowspecL3vpnIpv4Builder().setRouteDistinguisher(RD)
        .setFlowspec(FLOW_LIST).setPathId(PATH_ID).build();
    private static final DestinationFlowspecL3vpnIpv4Case REACH_NLRI = new DestinationFlowspecL3vpnIpv4CaseBuilder().setDestinationFlowspecL3vpnIpv4(DEST_FLOW).build();
    private static final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.
        DestinationFlowspecL3vpnIpv4Case UNREACH_NLRI = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.
        DestinationFlowspecL3vpnIpv4CaseBuilder().setDestinationFlowspecL3vpnIpv4(DEST_FLOW).build();

    static {
        final SimpleFlowspecExtensionProviderContext providerContext = new SimpleFlowspecExtensionProviderContext();
        RIB_SUPPORT = FlowspecL3vpnIpv4RIBSupport.getInstance(providerContext);
        ROUTE_KEY = new FlowspecL3vpnRouteKey(PATH_ID, "[l3vpn with route-distinguisher 0:5:3] all packets to 172.17.1.0/24");
        ROUTE = new FlowspecL3vpnRouteBuilder().setKey(ROUTE_KEY).setPathId(PATH_ID).setFlowspec(FLOW_LIST)
            .setAttributes(new AttributesBuilder().build()).setRouteDistinguisher(RD).build();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setUpTestCustomizer(RIB_SUPPORT);
    }

    @Test
    public void testDeleteRoutes() {
        RIB_SUPPORT.deleteRoutes(this.tx, getTablePath(), createNlriWithDrawnRoute(UNREACH_NLRI));
        final InstanceIdentifier<FlowspecL3vpnRoute> instanceIdentifier = (InstanceIdentifier<FlowspecL3vpnRoute>) this.deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(FlowspecL3vpnRoute.class));
    }

    @Test
    public void testPutRoutes() {
        RIB_SUPPORT.putRoutes(this.tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final FlowspecL3vpnRoute route = (FlowspecL3vpnRoute) this.insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }

    @Test
    public void testEmptyRoute() throws Exception {
        final Routes empty = new FlowspecL3vpnIpv4RoutesCaseBuilder().setFlowspecL3vpnIpv4Routes(
            new FlowspecL3vpnIpv4RoutesBuilder().setFlowspecL3vpnRoute(Collections.emptyList()).build()).build();
        final ChoiceNode emptyRoutes = RIB_SUPPORT.emptyRoutes();
        assertEquals(createRoutes(empty), emptyRoutes);
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Update update = RIB_SUPPORT.buildUpdate(Collections.emptyList(), createRoutes(
            new FlowspecL3vpnIpv4RoutesBuilder().setFlowspecL3vpnRoute(Collections.singletonList(ROUTE)).build()), ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().getAugmentation(Attributes2.class)
            .getMpUnreachNlri().getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().getAugmentation(Attributes1.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Update update = RIB_SUPPORT.buildUpdate(createRoutes(
            new FlowspecL3vpnIpv4RoutesBuilder().setFlowspecL3vpnRoute(Collections.singletonList(ROUTE)).build()), Collections.emptyList(), ATTRIBUTES);
        final AdvertizedRoutes advertised = update.getAttributes().getAugmentation(Attributes1.class).getMpReachNlri().getAdvertizedRoutes();
        assertEquals(REACH_NLRI, advertised.getDestinationType());
        assertNull(update.getAttributes().getAugmentation(Attributes2.class));
    }

    @Test
    public void testIsComplexRoute() {
        Assert.assertTrue(RIB_SUPPORT.isComplexRoute());
    }

    @Test
    public void testCacheableNlriObjects() {
        Assert.assertEquals(ImmutableSet.of(), RIB_SUPPORT.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        Assert.assertEquals(ImmutableSet.of(), RIB_SUPPORT.cacheableAttributeObjects());
    }

    @Test
    public void testRouteIdAddPath() {
        final YangInstanceIdentifier.NodeIdentifierWithPredicates expected = createRouteNIWP(new FlowspecL3vpnIpv4RoutesBuilder().
            setFlowspecL3vpnRoute(Collections.singletonList(ROUTE)).build());
        final YangInstanceIdentifier.NodeIdentifierWithPredicates prefixNii = new YangInstanceIdentifier.NodeIdentifierWithPredicates(RIB_SUPPORT.routeQName(),
            ImmutableMap.of(RIB_SUPPORT.routeKeyQName(), ROUTE_KEY.getRouteKey()));
        Assert.assertEquals(expected, RIB_SUPPORT.getRouteIdAddPath(AbstractRIBSupportTest.PATH_ID, prefixNii));
    }

    @Test
    public void testRoutePath() {
        final YangInstanceIdentifier.NodeIdentifierWithPredicates prefixNii = createRouteNIWP(new FlowspecL3vpnIpv4RoutesBuilder().setFlowspecL3vpnRoute(Collections.singletonList(ROUTE)).build());
        Assert.assertEquals(getRoutePath().node(prefixNii), RIB_SUPPORT.routePath(getTablePath().node(Routes.QNAME), prefixNii));
    }

    @Test
    public void testExtractPathId() {
        Assert.assertEquals((Long) NON_PATH_ID, RIB_SUPPORT.extractPathId(null));
    }

    @Test
    public void testRouteAttributesIdentifier() {
        Assert.assertEquals(new YangInstanceIdentifier.NodeIdentifier(QName.create(FlowspecL3vpnIpv4Routes.QNAME,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes.QNAME.getLocalName().intern())),
            RIB_SUPPORT.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        Assert.assertEquals(FlowspecL3vpnIpv4RoutesCase.class, RIB_SUPPORT.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        Assert.assertEquals(FlowspecL3vpnIpv4Routes.class, RIB_SUPPORT.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        Assert.assertEquals(FlowspecL3vpnRoute.class, RIB_SUPPORT.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new FlowspecL3vpnIpv4RoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyCase)).getRootNode();
        Assert.assertTrue(RIB_SUPPORT.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes = new FlowspecL3vpnIpv4RoutesCaseBuilder().setFlowspecL3vpnIpv4Routes(new FlowspecL3vpnIpv4RoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        Assert.assertTrue(RIB_SUPPORT.changedRoutes(tree).isEmpty());

        final Routes routes = new FlowspecL3vpnIpv4RoutesCaseBuilder().setFlowspecL3vpnIpv4Routes(new FlowspecL3vpnIpv4RoutesBuilder()
            .setFlowspecL3vpnRoute(Collections.singletonList(ROUTE)).build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = RIB_SUPPORT.changedRoutes(tree);
        Assert.assertFalse(result.isEmpty());
    }
}
