/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.Reference;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.mode.spi.PathSelectionModes;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer.NodeResult;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;

// FIXME: Migrate to JUnit5 when MDSAL is migrated or rework the test
public class AddPathStaleRouteKeyIdentityTest extends DefaultRibPoliciesMockTest {
    private static final @NonNull TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.VALUE,
        UnicastSubsequentAddressFamily.VALUE);
    private static final @NonNull String PREFIX = "1.1.1.0/24";
    // RouterA sorts before RouterB so it holds offset 0 in every key set.
    private static final @NonNull RouterId ROUTER_A = RouterId.forAddress("127.1.1.1");
    private static final @NonNull RouterId ROUTER_B = RouterId.forAddress("127.1.1.2");
    private static final @NonNull Uint32 PATH_A = Uint32.ONE;
    private static final @NonNull Uint32 PATH_B = Uint32.TWO;

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
    public void survivingBestPathMustNotBeReportedStale() {
        final var support = ribContext.getRIBSupport(TABLES_KEY);
        assertNotNull(support);

        // A separate entry adds both routers first, so the RouteKeyOffsets cache (shared by all route entries) stores
        // its RouteKey instances for the RouterA and RouterB pair. RouterB goes first so the entry under test still
        // creates its own RouteKey for RouterA alone.
        final var npaths = PathSelectionModes.addNPaths(2);
        final var keepAlive = npaths.createRouteEntry();
        try {
            assertTrue(keepAlive.putRoute(ROUTER_B, PATH_B, routeB));
            assertTrue(keepAlive.putRoute(ROUTER_A, PATH_A, routeA));

            final var entry = npaths.createRouteEntry();
            // First selection has only RouterA, using the RouteKey instance this entry created.
            assertTrue(entry.putRoute(ROUTER_A, PATH_A, routeA));
            final var firstUpdate = entry.selectBest(support, AS, PREFIX);
            assertNotNull(firstUpdate);
            assertEquals(1, firstUpdate.adverized().size());
            assertNull(firstUpdate.stale());

            // Add RouterB as a second path. RouterA stays the best path through the second selection.
            assertTrue(entry.putRoute(ROUTER_B, PATH_B, routeB));

            final var secondUpdate = entry.selectBest(support, AS, PREFIX);
            assertNotNull(secondUpdate);
            assertEquals(1, firstUpdate.adverized().size());
            assertNull(secondUpdate.stale(),
                "RouterA path is still best after the second selection and must not be reported stale");
        } finally {
            // Keep a strong reference until the last putRoute method is called.
            Reference.reachabilityFence(keepAlive);
        }
    }

    @NonNullByDefault
    private MapEntryNode buildRoute(final Uint32 pathId, final long localPref) {
        final var serializer = assertInstanceOf(BindingNormalizedNodeSerializer.class,
            mappingService.currentSerializer());
        final var result = assertInstanceOf(NodeResult.class, serializer.toNormalizedNode(
            DataObjectIdentifier.builder(BgpRib.class)
                .child(Rib.class, new RibKey(new RibId("rib")))
                .child(LocRib.class)
                .child(Tables.class, TABLES_KEY)
                .child(Ipv4RoutesCase.class, Ipv4Routes.class)
                .build(),
            new Ipv4RoutesBuilder()
                .setIpv4Route(BindingMap.of(new Ipv4RouteBuilder()
                    .setRouteKey(PREFIX)
                    .setPathId(new PathId(pathId))
                    .setPrefix(new Ipv4Prefix(PREFIX))
                    .setAttributes(new AttributesBuilder()
                        .setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(localPref)).build())
                        .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
                        .setMultiExitDisc(new MultiExitDiscBuilder().setMed(Uint32.ZERO).build())
                        .setAsPath(new AsPathBuilder().setSegments(List.of()).build())
                        .build())
                    .build()))
                .build()));
        final var routes = assertInstanceOf(ContainerNode.class, result.node());
        final var ipv4 = assertInstanceOf(MapNode.class, routes.getChildByArg(new NodeIdentifier(Ipv4Route.QNAME)));
        assertEquals(1, ipv4.size());
        return ipv4.body().iterator().next();
    }
}
