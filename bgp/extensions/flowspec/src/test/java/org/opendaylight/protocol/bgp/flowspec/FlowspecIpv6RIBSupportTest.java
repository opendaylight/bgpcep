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

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.destination.ipv6.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.destination.ipv6.DestinationFlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.route.FlowspecRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.route.FlowspecRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.flowspec.ipv6.routes.FlowspecIpv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;

public class FlowspecIpv6RIBSupportTest extends AbstractRIBSupportTest<FlowspecIpv6RoutesCase, FlowspecIpv6Routes,
        FlowspecRoute, FlowspecRouteKey> {

    private static final PathId PATH_ID = new PathId(1L);
    private static final DestinationIpv6PrefixCase DEST_PREFIX = new DestinationIpv6PrefixCaseBuilder()
            .setDestinationPrefix(new Ipv6Prefix("2001:db8:1:2::/64")).build();
    private static final List<Flowspec> FLOW_LIST = Collections.singletonList(new FlowspecBuilder()
            .setFlowspecType(DEST_PREFIX).build());
    private static final DestinationFlowspec DEST_FLOW = new DestinationFlowspecBuilder()
            .setFlowspec(FLOW_LIST).setPathId(PATH_ID).build();
    private static final DestinationFlowspecIpv6Case REACH_NLRI = new DestinationFlowspecIpv6CaseBuilder()
            .setDestinationFlowspec(DEST_FLOW).build();
    private static final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6Case UNREACH_NLRI
            = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6CaseBuilder()
            .setDestinationFlowspec(DEST_FLOW).build();
    private FlowspecRoute route;
    private FlowspecRouteKey routeKey;
    private FlowspecIpv6Routes routes;
    private FlowspecIpv6RIBSupport ribSupport;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final SimpleFlowspecExtensionProviderContext fsContext = new SimpleFlowspecExtensionProviderContext();
        final FlowspecActivator activator = new FlowspecActivator(fsContext);
        final BGPActivator act = new BGPActivator(activator);
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        act.start(context);

        final SimpleFlowspecIpv6NlriParser parser = new SimpleFlowspecIpv6NlriParser(
                fsContext.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4,
                        SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC));
        this.routeKey = new FlowspecRouteKey(PATH_ID, parser.stringNlri(FLOW_LIST));
        this.route = new FlowspecRouteBuilder().withKey(this.routeKey).setPathId(PATH_ID).setFlowspec(FLOW_LIST)
                .setAttributes(new AttributesBuilder().build()).build();
        this.routes = new FlowspecIpv6RoutesBuilder().setFlowspecRoute(Collections.singletonList(this.route)).build();
        this.ribSupport = FlowspecIpv6RIBSupport.getInstance(fsContext, this.mappingService);
        setUpTestCustomizer(this.ribSupport);
    }

    @Test
    public void testDeleteRoutes() {
        this.ribSupport.deleteRoutes(this.tx, getTablePath(), createNlriWithDrawnRoute(UNREACH_NLRI));
        final InstanceIdentifier<FlowspecRoute> instanceIdentifier = this.deletedRoutes.get(0);
        assertEquals(this.routeKey, instanceIdentifier.firstKeyOf(FlowspecRoute.class));
    }

    @Test
    public void testPutRoutes() {
        this.ribSupport.putRoutes(this.tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final FlowspecRoute route = (FlowspecRoute) this.insertedRoutes.get(0).getValue();
        assertEquals(this.route, route);
    }

    @Test
    public void testEmptyRoute() {
        final Routes empty = new FlowspecIpv6RoutesCaseBuilder().setFlowspecIpv6Routes(
            new FlowspecIpv6RoutesBuilder().setFlowspecRoute(Collections.emptyList()).build()).build();
        assertEquals(createEmptyTable(empty), this.ribSupport.emptyTable());
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Update update = this.ribSupport.buildUpdate(Collections.emptyList(),
                createRoutes(this.routes), ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().augmentation(Attributes2.class)
            .getMpUnreachNlri().getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(Attributes1.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Update update = this.ribSupport.buildUpdate(createRoutes(this.routes),
                Collections.emptyList(), ATTRIBUTES);
        assertEquals(REACH_NLRI, update.getAttributes().augmentation(Attributes1.class).getMpReachNlri()
                .getAdvertizedRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(Attributes2.class));
    }

    @Test
    public void testCacheableNlriObjects() {
        assertEquals(ImmutableSet.of(), this.ribSupport.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        assertEquals(ImmutableSet.of(), this.ribSupport.cacheableAttributeObjects());
    }

    @Test
    public void testRouteIdAddPath() {
        assertEquals(this.routeKey, this.ribSupport.createRouteListKey(1L, this.routeKey.getRouteKey()));
    }

    @Test
    public void testRoutePath() {
        final NodeIdentifierWithPredicates prefixNii = createRouteNIWP(this.routes);
        assertEquals(getRoutePath().node(prefixNii),
                this.ribSupport.routePath(getTablePath().node(Routes.QNAME), prefixNii));
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(QName.create(FlowspecIpv6Routes.QNAME,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes
                    .QNAME.getLocalName().intern())),
            this.ribSupport.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(FlowspecIpv6RoutesCase.class, this.ribSupport.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(FlowspecIpv6Routes.class, this.ribSupport.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(FlowspecRoute.class, this.ribSupport.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new FlowspecIpv6RoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        Assert.assertTrue(this.ribSupport.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes = new FlowspecIpv6RoutesCaseBuilder()
                .setFlowspecIpv6Routes(new FlowspecIpv6RoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        Assert.assertTrue(this.ribSupport.changedRoutes(tree).isEmpty());

        final Routes routes = new FlowspecIpv6RoutesCaseBuilder()
                .setFlowspecIpv6Routes(new FlowspecIpv6RoutesBuilder()
            .setFlowspecRoute(Collections.singletonList(this.route)).build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = this.ribSupport.changedRoutes(tree);
        Assert.assertFalse(result.isEmpty());
    }
}
