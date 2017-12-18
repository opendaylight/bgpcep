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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.Ipv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;

public final class IPv6RIBSupportTest extends AbstractRIBSupportTest {
    private static final IPv6RIBSupport RIB_SUPPORT = IPv6RIBSupport.getInstance();
    private static final PathId PATH_ID = new PathId(1L);
    private static final Ipv6Prefix PREFIX = new Ipv6Prefix("2001:db8:1:2::/64");
    private static final Ipv6RouteKey ROUTE_KEY = new Ipv6RouteKey(PATH_ID, PREFIX);
    private static final Ipv6Prefixes IPV6_PREFIXES = new Ipv6PrefixesBuilder().setPathId(PATH_ID)
            .setPrefix(PREFIX).build();
    private static final DestinationIpv6Case REACH_NLRI = new DestinationIpv6CaseBuilder()
            .setDestinationIpv6(new DestinationIpv6Builder()
                    .setIpv6Prefixes(Lists.newArrayList(IPV6_PREFIXES)).build()).build();

    private static final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6Case UNREACH_NLRI =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update
                    .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6CaseBuilder()
                    .setDestinationIpv6(new DestinationIpv6Builder()
                            .setIpv6Prefixes(Collections.singletonList(IPV6_PREFIXES)).build()).build();

    private static final Ipv6Route ROUTE = new Ipv6RouteBuilder().setAttributes(ATTRIBUTES).setPathId(PATH_ID)
            .setPrefix(PREFIX).build();
    private static final Ipv6Routes ROUTES = new Ipv6RoutesBuilder()
            .setIpv6Route(Collections.singletonList(ROUTE)).build();
    private static final Ipv6Routes EMPTY_ROUTES = new Ipv6RoutesBuilder()
            .setIpv6Route(Collections.emptyList()).build();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setUpTestCustomizer(RIB_SUPPORT);
    }

    @Test
    public void testDeleteRoutes() {
        RIB_SUPPORT.deleteRoutes(this.tx, getTablePath(), createNlriWithDrawnRoute(UNREACH_NLRI));
        @SuppressWarnings("unchecked") final InstanceIdentifier<Ipv6Route> instanceIdentifier =
                (InstanceIdentifier<Ipv6Route>) this.deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(Ipv6Route.class));
    }

    @Test
    public void testPutRoutes() {
        RIB_SUPPORT.putRoutes(this.tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final Ipv6Route route = (Ipv6Route) this.insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }


    @Test
    public void testEmptyRoute() throws Exception {
        final Routes empty = new Ipv6RoutesCaseBuilder().setIpv6Routes(EMPTY_ROUTES).build();
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
        assertEquals(REACH_NLRI, update.getAttributes().getAugmentation(Attributes1.class)
                .getMpReachNlri().getAdvertizedRoutes().getDestinationType());
        assertNull(update.getAttributes().getAugmentation(Attributes2.class));
    }

    @Test
    public void testIsComplexRoute() {
        Assert.assertFalse(RIB_SUPPORT.isComplexRoute());
    }

    @Test
    public void testCacheableNlriObjects() {
        assertEquals(ImmutableSet.of(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet
                .rev171207.Ipv6Prefix.class), RIB_SUPPORT.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        assertEquals(ImmutableSet.of(), RIB_SUPPORT.cacheableAttributeObjects());
    }

    @Test
    public void testRouteIdAddPath() {
        final NodeIdentifierWithPredicates expected = createRouteNIWP(ROUTES);
        final NodeIdentifierWithPredicates prefixNii = new NodeIdentifierWithPredicates(RIB_SUPPORT.routeQName(),
                ImmutableMap.of(RIB_SUPPORT.routeKeyQName(), PREFIX.getValue()));
        assertEquals(expected, RIB_SUPPORT.getRouteIdAddPath(AbstractRIBSupportTest.PATH_ID, prefixNii));
    }

    @Test
    public void testRoutePath() {
        final NodeIdentifierWithPredicates prefixNii = createRouteNIWP(ROUTES);
        assertEquals(getRoutePath().node(prefixNii),
                RIB_SUPPORT.routePath(getTablePath().node(Routes.QNAME), prefixNii));
    }


    @Test
    public void testExtractPathId() {
        final NormalizedNode<?, ?> route = Iterables.getOnlyElement(createRoutes(ROUTES));
        assertEquals(PATH_ID.getValue(), RIB_SUPPORT.extractPathId(route));
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(QName.create(Ipv6Routes.QNAME,
                Attributes.QNAME.getLocalName().intern())), RIB_SUPPORT.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(Ipv6RoutesCase.class, RIB_SUPPORT.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(Ipv6Routes.class, RIB_SUPPORT.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(Ipv6Route.class, RIB_SUPPORT.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new Ipv6RoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        Assert.assertTrue(RIB_SUPPORT.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes = new Ipv6RoutesCaseBuilder().setIpv6Routes(new Ipv6RoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        Assert.assertTrue(RIB_SUPPORT.changedRoutes(tree).isEmpty());

        final Routes routes = new Ipv6RoutesCaseBuilder().setIpv6Routes(ROUTES).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = RIB_SUPPORT.changedRoutes(tree);
        Assert.assertFalse(result.isEmpty());
    }
}
