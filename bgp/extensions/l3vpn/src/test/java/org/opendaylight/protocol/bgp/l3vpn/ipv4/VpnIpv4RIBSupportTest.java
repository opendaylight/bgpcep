/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn.ipv4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.bgp.l3vpn.ipv4.VpnIpv4NlriParserTest.DISTINGUISHER;
import static org.opendaylight.protocol.bgp.l3vpn.ipv4.VpnIpv4NlriParserTest.IPV4_PREFIX;
import static org.opendaylight.protocol.bgp.l3vpn.ipv4.VpnIpv4NlriParserTest.IPV4_VPN;
import static org.opendaylight.protocol.bgp.l3vpn.ipv4.VpnIpv4NlriParserTest.LABEL_STACK;
import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev171207.bgp.rib.rib.loc.rib.tables.routes.VpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev171207.bgp.rib.rib.loc.rib.tables.routes.VpnIpv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev171207.l3vpn.ipv4.destination.VpnIpv4DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev171207.l3vpn.ipv4.routes.VpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev171207.l3vpn.ipv4.routes.VpnIpv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev171207.l3vpn.ip.route.VpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev171207.l3vpn.ip.route.VpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev171207.l3vpn.ip.route.VpnRouteKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;

public class VpnIpv4RIBSupportTest extends AbstractRIBSupportTest {
    private static final VpnIpv4RIBSupport RIB_SUPPORT = new VpnIpv4RIBSupport();

    private static final DestinationVpnIpv4Case REACH_NLRI = new DestinationVpnIpv4CaseBuilder().setVpnIpv4Destination(
        new VpnIpv4DestinationBuilder().setVpnDestination(Collections.singletonList(IPV4_VPN)).build()).build();

    private static final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev171207.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv4Case UNREACH_NLRI =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev171207.update
                    .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv4CaseBuilder()
                    .setVpnIpv4Destination(new VpnIpv4DestinationBuilder()
                            .setVpnDestination(Collections.singletonList(IPV4_VPN)).build()).build();
    private static final VpnRouteKey ROUTE_KEY = new VpnRouteKey("WAABAQIDBAECIgEW");

    private static final VpnRoute ROUTE = new VpnRouteBuilder().setPathId(new PathId(NON_PATH_ID))
            .setAttributes(ATTRIBUTES).setPrefix(IPV4_PREFIX)
        .setLabelStack(LABEL_STACK).setRouteDistinguisher(DISTINGUISHER).setKey(ROUTE_KEY).build();
    private static final VpnIpv4Routes ROUTES = new VpnIpv4RoutesBuilder()
            .setVpnRoute(Collections.singletonList(ROUTE)).build();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setUpTestCustomizer(RIB_SUPPORT);
    }

    @Test
    public void testDeleteRoutes() {
        RIB_SUPPORT.deleteRoutes(this.tx, getTablePath(), createNlriWithDrawnRoute(UNREACH_NLRI));
        @SuppressWarnings("unchecked")
        final InstanceIdentifier<VpnRoute> instanceIdentifier =
                (InstanceIdentifier<VpnRoute>) this.deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(VpnRoute.class));
    }

    @Test
    public void testPutRoutes() {
        RIB_SUPPORT.putRoutes(this.tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final VpnRoute route = (VpnRoute) this.insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }


    @Test
    public void testEmptyRoute() throws Exception {
        final Routes empty = new VpnIpv4RoutesCaseBuilder().setVpnIpv4Routes(new VpnIpv4RoutesBuilder()
                .setVpnRoute(Collections.emptyList()).build()).build();
        final ChoiceNode emptyRoutes = RIB_SUPPORT.emptyRoutes();
        assertEquals(createRoutes(empty), emptyRoutes);
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Update update = RIB_SUPPORT.buildUpdate(Collections.emptyList(), createRoutes(ROUTES), ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().getAugmentation(Attributes2.class)
            .getMpUnreachNlri().getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().getAugmentation(Attributes1.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Update update = RIB_SUPPORT.buildUpdate(createRoutes(ROUTES), Collections.emptyList(), ATTRIBUTES);
        assertEquals(REACH_NLRI, update.getAttributes().getAugmentation(Attributes1.class).getMpReachNlri()
                .getAdvertizedRoutes().getDestinationType());
        assertNull(update.getAttributes().getAugmentation(Attributes2.class));
    }

    @Test
    public void testIsComplexRoute() {
        assertTrue(RIB_SUPPORT.isComplexRoute());
    }

    @Test
    public void testCacheableNlriObjects() {
        assertEquals(ImmutableSet.of(), RIB_SUPPORT.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        assertEquals(ImmutableSet.of(), RIB_SUPPORT.cacheableAttributeObjects());
    }

    @Test
    public void testRouteIdAddPath() {
        Assert.assertNull(RIB_SUPPORT.getRouteIdAddPath(AbstractRIBSupportTest.PATH_ID, null));
    }

    @Test
    public void testRoutePath() {
        final NodeIdentifierWithPredicates prefixNii = createRouteNIWP(ROUTES);
        assertEquals(getRoutePath().node(prefixNii), RIB_SUPPORT
                .routePath(getTablePath().node(Routes.QNAME), prefixNii));
    }

    @Test
    public void testExtractPathId() {
        assertEquals((Long) NON_PATH_ID, RIB_SUPPORT.extractPathId(null));
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(QName.create(VpnIpv4Routes.QNAME,
                Attributes.QNAME.getLocalName().intern())), RIB_SUPPORT.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(VpnIpv4RoutesCase.class, RIB_SUPPORT.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(VpnIpv4Routes.class, RIB_SUPPORT.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(VpnRoute.class, RIB_SUPPORT.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new VpnIpv4RoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        assertTrue(RIB_SUPPORT.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes = new VpnIpv4RoutesCaseBuilder().setVpnIpv4Routes(
                new VpnIpv4RoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        assertTrue(RIB_SUPPORT.changedRoutes(tree).isEmpty());

        final Routes routes = new VpnIpv4RoutesCaseBuilder().setVpnIpv4Routes(ROUTES).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = RIB_SUPPORT.changedRoutes(tree);
        Assert.assertFalse(result.isEmpty());
    }
}