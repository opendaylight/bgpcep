/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.RD;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ETHERNET_AD_ROUTE_CASE;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ETHERNET_AD_ROUTE_CASE_KEY;
import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.nlri.EvpnNlriParser;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupportTest;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.bgp.rib.rib.loc.rib.tables.routes.EvpnRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.bgp.rib.rib.loc.rib.tables.routes.EvpnRoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.destination.EvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.destination.EvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.EvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.EvpnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.evpn.routes.EvpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.evpn.routes.EvpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.evpn.routes.EvpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

public final class EvpnRibSupportTest extends AbstractRIBSupportTest<EvpnRoutesCase, EvpnRoutes, EvpnRoute> {
    private static final EvpnRoute ROUTE;
    private static final EvpnRouteKey ROUTE_KEY;
    private static final EvpnDestination EVPN_DESTINATION = new EvpnDestinationBuilder()
            .setRouteDistinguisher(RD)
            .setEvpnChoice(ETHERNET_AD_ROUTE_CASE)
            .setPathId(NON_PATH_ID)
            .build();
    private static final DestinationEvpnCase REACH_NLRI = new DestinationEvpnCaseBuilder()
            .setDestinationEvpn(new DestinationEvpnBuilder()
                    .setEvpnDestination(List.of(EVPN_DESTINATION)).build()).build();
    private static final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCase UNREACH_NLRI =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes
                .mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCaseBuilder()
                .setDestinationEvpn(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.evpn.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.evpn._case.DestinationEvpnBuilder()
                    .setEvpnDestination(List.of(EVPN_DESTINATION))
                    .build())
                .build();

    private static final EvpnRoutes EVPN_ROUTES;

    static {
        final BGPActivator act = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        act.start(context);
        final ByteBuf buffer = Unpooled.buffer();
        EvpnNlriParser.serializeNlri(List.of(new EvpnDestinationBuilder()
                .setRouteDistinguisher(RD).setEvpnChoice(ETHERNET_AD_ROUTE_CASE_KEY).build()), buffer);
        ROUTE_KEY = new EvpnRouteKey(new PathId(Uint32.ZERO), ByteArray.encodeBase64(buffer));
        ROUTE = new EvpnRouteBuilder().setRouteKey(ROUTE_KEY.getRouteKey())
                .setPathId(ROUTE_KEY.getPathId())
                .setAttributes(ATTRIBUTES).setRouteDistinguisher(RD).setEvpnChoice(ETHERNET_AD_ROUTE_CASE).build();
        EVPN_ROUTES = new EvpnRoutesBuilder().setEvpnRoute(BindingMap.of(ROUTE)).build();
    }

    private EvpnRibSupport ribSupport;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ribSupport = new EvpnRibSupport(adapter.currentSerializer());
        setUpTestCustomizer(ribSupport);
    }

    @Test
    public void testDeleteRoutes() {
        ribSupport.deleteRoutes(tx, getTablePath(), createNlriWithDrawnRoute(UNREACH_NLRI));
        final InstanceIdentifier<EvpnRoute> instanceIdentifier = deletedRoutes.get(0);
        assertEquals(ROUTE_KEY, instanceIdentifier.firstKeyOf(EvpnRoute.class));
    }

    @Test
    public void testPutRoutes() {
        ribSupport.putRoutes(tx, getTablePath(), createNlriAdvertiseRoute(REACH_NLRI), createAttributes());
        final EvpnRoute route = (EvpnRoute) insertedRoutes.get(0).getValue();
        assertEquals(ROUTE, route);
    }


    @Test
    public void testEmptyRoute() {
        assertEquals(createEmptyTable(), ribSupport.emptyTable());
    }

    @Test
    public void testBuildMpUnreachNlriUpdate() {
        final Update update = ribSupport.buildUpdate(List.of(),
                createRoutes(EVPN_ROUTES), ATTRIBUTES);
        assertEquals(UNREACH_NLRI, update.getAttributes().augmentation(AttributesUnreach.class).getMpUnreachNlri()
                .getWithdrawnRoutes().getDestinationType());
        assertNull(update.getAttributes().augmentation(AttributesReach.class));
    }

    @Test
    public void testBuildMpReachNlriUpdate() {
        final Update update = ribSupport.buildUpdate(
                createRoutes(EVPN_ROUTES), List.of(), ATTRIBUTES);
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
        final NodeIdentifierWithPredicates prefixNii = createRouteNIWP(EVPN_ROUTES);
        assertEquals(getRoutePath().node(prefixNii),
                ribSupport.routePath(getTablePath(), prefixNii));
    }

    @Test
    public void testRouteAttributesIdentifier() {
        assertEquals(new NodeIdentifier(QName.create(EvpnRoutes.QNAME,
                Attributes.QNAME.getLocalName().intern())), ribSupport.routeAttributesIdentifier());
    }

    @Test
    public void testRoutesCaseClass() {
        assertEquals(EvpnRoutesCase.class, ribSupport.routesCaseClass());
    }

    @Test
    public void testRoutesContainerClass() {
        assertEquals(EvpnRoutes.class, ribSupport.routesContainerClass());
    }

    @Test
    public void testRoutesListClass() {
        assertEquals(EvpnRoute.class, ribSupport.routesListClass());
    }

    @Test
    public void testChangedRoutes() {
        final Routes emptyCase = new EvpnRoutesCaseBuilder().build();
        DataTreeCandidateNode tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(),
                createRoutes(emptyCase)).getRootNode();
        assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes emptyRoutes = new EvpnRoutesCaseBuilder().setEvpnRoutes(new EvpnRoutesBuilder().build()).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(emptyRoutes)).getRootNode();
        assertTrue(ribSupport.changedRoutes(tree).isEmpty());

        final Routes routes = new EvpnRoutesCaseBuilder().setEvpnRoutes(EVPN_ROUTES).build();
        tree = DataTreeCandidates.fromNormalizedNode(getRoutePath(), createRoutes(routes)).getRootNode();
        final Collection<DataTreeCandidateNode> result = ribSupport.changedRoutes(tree);
        assertFalse(result.isEmpty());
    }
}
