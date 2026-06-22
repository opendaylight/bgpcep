/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.ChoiceIn;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer.NodeResult;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;

public class AddPathStaleRouteKeyIdentityTest extends DefaultRibPoliciesMockTest {
    private static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.VALUE,
        UnicastSubsequentAddressFamily.VALUE);
    private static final String PREFIX = "1.1.1.0/24";
    // RouterA sorts before RouterB so it holds offset 0 in every key set.
    private static final RouterId ROUTER_A = RouterId.forAddress("127.1.1.1");
    private static final RouterId ROUTER_B = RouterId.forAddress("127.1.1.2");
    private static final Uint32 PATH_A = Uint32.ONE;
    private static final Uint32 PATH_B = Uint32.TWO;

    private RIBExtensionProviderContext ribContext;
    private MapEntryNode routeA;
    private MapEntryNode routeB;

    @Before
    public void setUpSupport() {
        ribContext = new SimpleRIBExtensionProviderContext();
        new RIBActivator().startRIBExtensionProvider(ribContext, mappingService.currentSerializer());
        // RouterA advertises the preferred path (higher local-pref), RouterB the less preferred one.
        routeA = buildRoute(PATH_A, 200);
        routeB = buildRoute(PATH_B, 100);
    }

    /**
     * Test that the best path which survives a selection is not reported stale.
     *
     * <p>{@code AddPathAbstractRouteEntry} matched a surviving best path by RouteKey reference instead of equality, so
     * a path whose RouteKey instance differed between two selections was wrongly deleted from the loc-rib. It is
     * generic because the route support and the route entries must use the same route type parameters.
     */
    @Test
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>>
            void survivingBestPathMustNotBeReportedStale() {
        final RIBSupport<C, S> support = ribContext.getRIBSupport(TABLES_KEY);

        // A separate entry adds both routers first, so the RouteKeyOffsets cache (shared by all route entries) stores
        // its RouteKey instances for the RouterA and RouterB pair. RouterB goes first so the entry under test still
        // creates its own RouteKey for RouterA alone.
        final RouteEntry<C, S> keepAlive = new AddPathBestNPathSelection(2).createRouteEntry();
        keepAlive.addRoute(ROUTER_B, PATH_B, routeB);
        keepAlive.addRoute(ROUTER_A, PATH_A, routeA);

        final RouteEntry<C, S> entry = new AddPathBestNPathSelection(2).createRouteEntry();
        // First selection has only RouterA, using the RouteKey instance this entry created.
        entry.addRoute(ROUTER_A, PATH_A, routeA);
        entry.selectBest(support, AS);
        // Add RouterB as a second path. RouterA stays the best path through the second selection.
        entry.addRoute(ROUTER_B, PATH_B, routeB);
        entry.selectBest(support, AS);
        final var stale = entry.removeStalePaths(support, PREFIX);

        // Keep a strong reference until the last addRoute method is called.
        assertNotNull(keepAlive);
        assertTrue("RouterA path is still best after the second selection and must not be reported stale",
            stale.isEmpty());
    }

    private MapEntryNode buildRoute(final Uint32 pathId, final long localPref) {
        final var attributes = new AttributesBuilder()
            .setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(localPref)).build())
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setMultiExitDisc(new MultiExitDiscBuilder().setMed(Uint32.ZERO).build())
            .setAsPath(new AsPathBuilder().setSegments(List.of()).build())
            .build();
        final var route = new Ipv4RouteBuilder()
            .setRouteKey(PREFIX)
            .setPathId(new PathId(pathId))
            .setPrefix(new Ipv4Prefix(PREFIX))
            .setAttributes(attributes)
            .build();
        final var routes = new Ipv4RoutesBuilder().setIpv4Route(Map.of(route.key(), route)).build();

        final var routesId = DataObjectIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("rib")))
            .child(LocRib.class)
            .child(Tables.class, TABLES_KEY)
            .child(Ipv4RoutesCase.class, Ipv4Routes.class)
            .build();
        final BindingNormalizedNodeSerializer serializer = mappingService.currentSerializer();
        final var routesNode = (ContainerNode) ((NodeResult) serializer.toNormalizedNode(routesId, routes)).node();
        final var routeMap = (MapNode) routesNode.getChildByArg(new NodeIdentifier(Ipv4Route.QNAME));
        return routeMap.body().iterator().next();
    }
}
