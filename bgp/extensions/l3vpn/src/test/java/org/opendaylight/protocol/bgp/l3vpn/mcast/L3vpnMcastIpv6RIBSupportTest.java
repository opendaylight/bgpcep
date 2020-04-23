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
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.bgp.rib.rib.loc.rib.tables.routes.L3vpnMcastRoutesIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.bgp.rib.rib.loc.rib.tables.routes.L3vpnMcastRoutesIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.L3vpnMcastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.L3vpnMcastRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.L3vpnMcastRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.ipv6.L3vpnMcastRoutesIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.ipv6.L3vpnMcastRoutesIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationL3vpnMcastIpv6AdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationL3vpnMcastIpv6AdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.l3vpn.mcast.ipv6.advertized._case.DestinationIpv6L3vpnMcastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationL3vpnMcastIpv6WithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationL3vpnMcastIpv6WithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;

public class L3vpnMcastIpv6RIBSupportTest extends AbstractRIBSupportTest<L3vpnMcastRoutesIpv6Case, L3vpnMcastRoutesIpv6,
        L3vpnMcastRoute, L3vpnMcastRouteKey> {
    private static final PathId PATH_ID = new PathId(Uint32.ZERO);
    private static final IpPrefix IPV6_PREFIX = new IpPrefix(new Ipv6Prefix("2001:db8:1:1::/64"));
    private static final RouteDistinguisher RD = new RouteDistinguisher(new RdIpv4("1.2.3.4:258"));
    private static final L3vpnMcastRouteKey ROUTE_KEY
            = new L3vpnMcastRouteKey(PATH_ID, "gAABAQIDBAECQCABDbgAAQABAAAAAAAAAA==");
    private static final L3vpnMcastRoute ROUTE = new L3vpnMcastRouteBuilder()
            .setRouteKey(ROUTE_KEY.getRouteKey())
            .setPathId(ROUTE_KEY.getPathId())
            .setAttributes(ATTRIBUTES)
            .setRouteDistinguisher(RD)
            .setPrefix(IPV6_PREFIX)
            .build();
    private static final L3vpnMcastRoutesIpv6 MCAST_L3VPN_ROUTES
            = new L3vpnMcastRoutesIpv6Builder().setL3vpnMcastRoute(Collections.singletonList(ROUTE)).build();

    private static final L3vpnMcastDestination MCAST_L3VPN_DESTINATION = new L3vpnMcastDestinationBuilder()
            .setRouteDistinguisher(RD)
            .setPrefix(IPV6_PREFIX)
            .setPathId(PATH_ID)
            .build();
    private static final DestinationL3vpnMcastIpv6AdvertizedCase REACH_NLRI
            = new DestinationL3vpnMcastIpv6AdvertizedCaseBuilder()
            .setDestinationIpv6L3vpnMcast(new DestinationIpv6L3vpnMcastBuilder()
                    .setL3vpnMcastDestination(Collections.singletonList(MCAST_L3VPN_DESTINATION)).build()).build();
    private static final DestinationL3vpnMcastIpv6WithdrawnCase UNREACH_NLRI
            = new DestinationL3vpnMcastIpv6WithdrawnCaseBuilder()
            .setDestinationIpv6L3vpnMcast(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .l3vpn.mcast.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.l3vpn.mcast.ipv6.withdrawn._case.DestinationIpv6L3vpnMcastBuilder()
                    .setL3vpnMcastDestination(Collections.singletonList(MCAST_L3VPN_DESTINATION)).build()).build();

    private L3VpnMcastIpv6RIBSupport ribSupport;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ribSupport = L3VpnMcastIpv6RIBSupport.getInstance(this.adapter.currentSerializer());
        setUpTestCustomizer(ribSupport);
    }

    @Test
    public void testDeleteRoutes() {
        final ContainerNode withdraw = createNlriWithDrawnRoute(UNREACH_NLRI);
        this.ribSupport.deleteRoutes(this.tx, getTablePath(), withdraw);
        final InstanceIdentifier<L3vpnMcastRoute> instanceIdentifier = this.deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(L3vpnMcastRoute.class));
    }

    @Test
    public void testPutRoutes() {
        this.ribSupport.putRoutes(this.tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final L3vpnMcastRoute route = (L3vpnMcastRoute) this.insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }


    @Test
    public void testEmptyRoute() {
        Assert.assertEquals(createEmptyTable(), this.ribSupport.emptyTable());
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Collection<MapEntryNode> routes = createRoutes(MCAST_L3VPN_ROUTES);
        final Update update = this.ribSupport.buildUpdate(Collections.emptyList(), routes, ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().augmentation(Attributes2.class).getMpUnreachNlri()
                .getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(Attributes1.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Collection<MapEntryNode> routes = createRoutes(MCAST_L3VPN_ROUTES);
        final Update update = this.ribSupport.buildUpdate(routes, Collections.emptyList(), ATTRIBUTES);
        assertEquals(REACH_NLRI, update.getAttributes().augmentation(Attributes1.class).getMpReachNlri()
                .getAdvertizedRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(Attributes2.class));
    }

    @Test
    public void testCacheableNlriObjects() {
        Assert.assertEquals(ImmutableSet.of(L3vpnMcastRoutesIpv6Case.class), this.ribSupport.cacheableNlriObjects());
    }

    @Test
    public void testCacheableAttributeObjects() {
        Assert.assertEquals(ImmutableSet.of(), this.ribSupport.cacheableAttributeObjects());
    }

    @Test
    public void testRouteIdAddPath() {
        Assert.assertEquals(ROUTE_KEY, this.ribSupport.createRouteListKey(ROUTE_KEY.getPathId(),
                ROUTE_KEY.getRouteKey()));
    }

    @Test
    public void testRoutePath() {
        final YangInstanceIdentifier.NodeIdentifierWithPredicates prefixNii = createRouteNIWP(MCAST_L3VPN_ROUTES);
        final YangInstanceIdentifier expected = getRoutePath().node(prefixNii);
        final YangInstanceIdentifier actual = this.ribSupport.routePath(getTablePath(), prefixNii);
        assertEquals(expected, actual);
    }

    @Test
    public void testRouteAttributesIdentifier() {
        Assert.assertEquals(new YangInstanceIdentifier.NodeIdentifier(
                        Attributes.QNAME.withModule(BindingReflections.getQNameModule(L3vpnMcastRoutesIpv6Case.class))),
                this.ribSupport.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        Assert.assertEquals(L3vpnMcastRoutesIpv6Case.class, this.ribSupport.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        Assert.assertEquals(L3vpnMcastRoutesIpv6.class, this.ribSupport.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        Assert.assertEquals(L3vpnMcastRoute.class, this.ribSupport.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new L3vpnMcastRoutesIpv6CaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        assertTrue(this.ribSupport.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes
                = new L3vpnMcastRoutesIpv6CaseBuilder()
                .setL3vpnMcastRoutesIpv6(new L3vpnMcastRoutesIpv6Builder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        assertTrue(this.ribSupport.changedRoutes(tree).isEmpty());

        final Routes routes = new L3vpnMcastRoutesIpv6CaseBuilder().setL3vpnMcastRoutesIpv6(MCAST_L3VPN_ROUTES).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = this.ribSupport.changedRoutes(tree);
        assertFalse(result.isEmpty());
    }
}
