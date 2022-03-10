/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.Ipv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.ipv6.routes.Ipv6RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.ipv6.routes.Ipv6RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
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

public final class IPv6RIBSupportTest extends AbstractRIBSupportTest<Ipv6RoutesCase, Ipv6Routes, Ipv6Route> {
    private IPv6RIBSupport ribSupport;
    private static final PathId PATH_ID = new PathId(Uint32.ONE);
    private static final Ipv6Prefix PREFIX = new Ipv6Prefix("2001:db8:1:2::/64");
    private static final Ipv6RouteKey ROUTE_KEY = new Ipv6RouteKey(PATH_ID, PREFIX.getValue());
    private static final Ipv6Prefixes IPV6_PREFIXES = new Ipv6PrefixesBuilder().setPathId(PATH_ID)
            .setPrefix(PREFIX).build();
    private static final DestinationIpv6Case REACH_NLRI = new DestinationIpv6CaseBuilder()
            .setDestinationIpv6(new DestinationIpv6Builder()
                    .setIpv6Prefixes(List.of(IPV6_PREFIXES)).build()).build();

    private static final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6Case UNREACH_NLRI =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update
                    .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6CaseBuilder()
                    .setDestinationIpv6(new DestinationIpv6Builder()
                            .setIpv6Prefixes(List.of(IPV6_PREFIXES)).build()).build();

    private static final Ipv6Route ROUTE = new Ipv6RouteBuilder()
            .setRouteKey(PREFIX.getValue())
            .setAttributes(ATTRIBUTES)
            .setPathId(PATH_ID)
            .setPrefix(PREFIX).build();
    private static final Ipv6Routes ROUTES = new Ipv6RoutesBuilder().setIpv6Route(Map.of(ROUTE.key(), ROUTE)).build();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ribSupport = new IPv6RIBSupport(adapter.currentSerializer());
        setUpTestCustomizer(ribSupport);
    }

    @Test
    public void testDeleteRoutes() {
        ribSupport.deleteRoutes(tx, getTablePath(), createNlriWithDrawnRoute(UNREACH_NLRI));
        final InstanceIdentifier<Ipv6Route> instanceIdentifier =
                deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(Ipv6Route.class));
    }

    @Test
    public void testPutRoutes() {
        ribSupport.putRoutes(tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final Ipv6Route route = (Ipv6Route) insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }

    @Test
    public void testEmptyRoute() {
        assertEquals(createEmptyTable(), ribSupport.emptyTable());
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Update update = ribSupport.buildUpdate(List.of(), createRoutes(ROUTES), ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().augmentation(AttributesUnreach.class)
                .getMpUnreachNlri().getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(AttributesReach.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Update update = ribSupport.buildUpdate(createRoutes(ROUTES), List.of(), ATTRIBUTES);
        assertEquals(REACH_NLRI, update.getAttributes().augmentation(AttributesReach.class)
                .getMpReachNlri().getAdvertizedRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(AttributesUnreach.class));
    }

    @Test
    public void testCacheableNlriObjects() {
        assertEquals(Set.of(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet
                .rev180329.Ipv6Prefix.class), ribSupport.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        assertEquals(Set.of(), ribSupport.cacheableAttributeObjects());
    }

    @Test
    public void testRoutePath() {
        final NodeIdentifierWithPredicates prefixNii = createRouteNIWP(ROUTES);
        assertEquals(getRoutePath().node(prefixNii), ribSupport.routePath(getTablePath(), prefixNii));
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(QName.create(Ipv6Routes.QNAME,
                Attributes.QNAME.getLocalName().intern())), ribSupport.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(Ipv6RoutesCase.class, ribSupport.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(Ipv6Routes.class, ribSupport.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(Ipv6Route.class, ribSupport.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new Ipv6RoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        Assert.assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes = new Ipv6RoutesCaseBuilder().setIpv6Routes(new Ipv6RoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        Assert.assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes routes = new Ipv6RoutesCaseBuilder().setIpv6Routes(ROUTES).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = ribSupport.changedRoutes(tree);
        Assert.assertFalse(result.isEmpty());
    }
}
