/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.DataTreeChangeExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.GracefulRestartUtil;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer.NodeResult;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

/**
 * Reproduces the lost initial advertisement to a freshly connected peer. {@code LocRibWriter} dumps the existing
 * loc-rib routes to a new peer only when it processes that peer's effective-rib-in table creation and the peer is
 * already present in the peer tracker. When the table-creation event is processed before the peer registers, the
 * lookup returns null, the dump is skipped and never retried, so the peer never receives the routes that already
 * existed when it connected.
 */
public class Bgpcep1094Test extends AbstractRIBTestSetup {
    private static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.VALUE,
        UnicastSubsequentAddressFamily.VALUE);
    private static final String PREFIX = "1.1.1.0/24";
    private static final PeerId PEER_A = new PeerId("bgp://127.0.0.2");
    private static final PeerId PEER_B = new PeerId("bgp://127.0.0.3");
    private static final AsNumber AS_NUMBER = new AsNumber(Uint32.valueOf(AS));

    private final BGPPeerTracker peerTracker = new BGPPeerTrackerImpl();

    @Mock
    private Peer peerB;
    @Mock
    private DOMDataTreeWriteTransaction tx;
    @Mock
    private DOMTransactionChain chain;
    @Mock
    private Registration reg;
    @Mock
    private Registration peerRegistration;
    @Mock
    private BGPPeerTracker mockedPeerTracker;
    @Mock
    private DataTreeChangeExtension dataBroker;
    @Mock
    private BGPSession session;
    private RIBExtensionProviderContext ribContext;
    private YangInstanceIdentifier ribIId;
    private MapEntryNode tableWithRoute;

    @Before
    public void setUpWriter() {
        ribContext = new SimpleRIBExtensionProviderContext();
        new RIBActivator().startRIBExtensionProvider(ribContext, mappingService.currentSerializer());
        ribIId = getRib().getYangRibId();
        tableWithRoute = buildTableWithRoute();

        doReturn(PEER_B).when(peerB).getPeerId();
        doReturn(PeerRole.RrClient).when(peerB).getRole();
        doReturn(true).when(peerB).supportsTable(any(TablesKey.class));
        doNothing().when(peerB).initializeRibOut(any(RouteEntryDependenciesContainer.class), anyList());

        doNothing().when(tx).put(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class),
            any(NormalizedNode.class));
        doNothing().when(tx).merge(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class),
            any(NormalizedNode.class));
        doNothing().when(tx).delete(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class));
        doReturn(CommitInfo.emptyFluentFuture()).when(tx).commit();

        doReturn(tx).when(chain).newWriteOnlyTransaction();
        doNothing().when(chain).close();

        doNothing().when(reg).close();
        doNothing().when(peerRegistration).close();
        doReturn(reg).when(dataBroker).registerTreeChangeListener(any(DOMDataTreeIdentifier.class),
            any(DOMDataTreeChangeListener.class));
    }

    /**
     * Test that a peer which registers right after its table-creation event still receives the existing routes.
     */
    @Test
    public void testLatePeerReceivesRoutes() {
        final var ribSupport = ribContext.getRIBSupport(TABLES_KEY);
        try (var locRibWriter = LocRibWriter.create(ribSupport, IPV4UNICAST.VALUE, chain, ribIId,
                AS_NUMBER, dataBroker, policies, peerTracker,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy())) {
            // PeerA already advertised a route, so the loc-rib holds a best path.
            locRibWriter.onDataTreeChanged(List.of(effRibInEvent(PEER_A, tableWithRoute)));
            // PeerB connects. Its empty effective-rib-in table is processed before PeerB registers in the tracker.
            locRibWriter.onDataTreeChanged(List.of(effRibInEvent(PEER_B, ribSupport.emptyTable())));
            // PeerB registers slightly later. The registration is intentionally not closed, the tracker is per-test.
            peerTracker.registerPeer(peerB);

            // PeerB must still receive the route that existed when it connected. Capture the advertised routes and
            // assert the list is non-empty - an empty list would still match anyList() while exhibiting the defect.
            final var routes = ArgumentCaptor.forClass(List.class);
            verify(peerB).initializeRibOut(any(RouteEntryDependenciesContainer.class), routes.capture());
            assertFalse("new peer must receive the routes that existed when it connected",
                routes.getValue().isEmpty());
        }
    }

    /**
     * A listener registered after a peer must be notified about that existing peer.
     */
    @Test
    public void testExistingPeerReplayedToNewListener() {
        final var peerTrackerRegistration = peerTracker.registerPeer(peerB);
        final var replayedPeer = new AtomicReference<Peer>();
        final var listenerRegistration = peerTracker.registerPeerAddedListener(replayedPeer::set);
        try {
            assertSame("existing peer was not replayed to the new listener", peerB, replayedPeer.get());
        } finally {
            listenerRegistration.close();
            peerTrackerRegistration.close();
        }
    }

    /**
     * Internal peers have no BGP session, so existing routes must not be written to their Adj-RIB-Out.
     */
    @Test
    public void testInternalPeerDoesNotReceiveExistingRoutes() {
        doReturn(PeerRole.Internal).when(peerB).getRole();
        final var ribSupport = ribContext.getRIBSupport(TABLES_KEY);
        try (var locRibWriter = LocRibWriter.create(ribSupport, IPV4UNICAST.VALUE, chain, ribIId,
                AS_NUMBER, dataBroker, policies, peerTracker,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy())) {
            locRibWriter.onDataTreeChanged(List.of(effRibInEvent(PEER_A, tableWithRoute)));
            peerTracker.registerPeer(peerB);

            verify(peerB, never()).initializeRibOut(any(RouteEntryDependenciesContainer.class), anyList());
        }
    }

    /**
     * Closing a LocRibWriter must unregister its peer-added listener.
     */
    @Test
    public void testCloseUnregistersPeerListener() {
        doReturn(peerRegistration).when(mockedPeerTracker).registerPeerAddedListener(any());
        final var ribSupport = ribContext.getRIBSupport(TABLES_KEY);
        final var locRibWriter = LocRibWriter.create(ribSupport, IPV4UNICAST.VALUE, chain, ribIId,
            AS_NUMBER, dataBroker, policies, mockedPeerTracker,
            BasePathSelectionModeFactory.createBestPathSelectionStrategy());

        locRibWriter.close();

        verify(peerRegistration).close();
    }

    /**
     * Tests that registration waits for peer-structure initialization after Adj-RIB-Out initialization completes.
     *
     * <p>It also must not hold the {@link BGPPeer} lock. {@code registerPeer} synchronously calls
     * {@code LocRibWriter.onPeerAdded}, which takes the
     * {@code LocRibWriter} lock, while {@code LocRibWriter.onDataTreeChanged} takes the {@code LocRibWriter} lock
     * and then the {@code BGPPeer} lock. Holding the {@code BGPPeer} lock during registration reverses that
     * order and deadlocks.
     */
    @Test
    public void testRegistrationWaitsForPeerStructureInitialization() throws Exception {
        doNothing().when(session).close();
        assertRegistrationWaitsForBothInitializations(false);
    }

    /**
     * Tests that registration waits for Adj-RIB-Out initialization after peer-structure initialization completes.
     */
    @Test
    public void testRegistrationWaitsForRibOutInitialization() throws Exception {
        doNothing().when(session).close();
        assertRegistrationWaitsForBothInitializations(true);
    }

    private void assertRegistrationWaitsForBothInitializations(final boolean completePeerStructureFirst)
            throws InterruptedException {
        final var peerStructure = SettableFuture.<CommitInfo>create();
        final var ribOut = SettableFuture.<CommitInfo>create();
        // onSessionUp() first commits the peer structure through domChain, then Adj-RIB-Out through ribOutChain.
        doReturn(FluentFuture.from(peerStructure), FluentFuture.from(ribOut)).when(getTransaction()).commit();

        doReturn(new Ipv4Address("127.0.0.3")).when(session).getBgpId();
        doReturn(Set.of(new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE)))
            .when(session).getAdvertisedTableTypes();
        doReturn(List.of()).when(session).getAdvertisedAddPathTableTypes();
        doReturn(GracefulRestartUtil.EMPTY_GR_CAPABILITY).when(session).getAdvertisedGracefulRestartCapability();
        doReturn(GracefulRestartUtil.EMPTY_LLGR_CAPABILITY).when(session).getAdvertisedLlGracefulRestartCapability();
        final var bgpPeer = AbstractAddPathTest.configurePeer(tableRegistry, new Ipv4AddressNoZone("127.0.0.3"),
            getRib(), null, PeerRole.Ibgp, new StrictBGPPeerRegistry());

        // Stands in for LocRibWriter.onPeerAdded. Record whether the BGPPeer lock is held while it runs.
        final var registered = new CountDownLatch(1);
        final var notifications = new AtomicInteger();
        final var peerMonitorHeld = new AtomicBoolean();
        final var listener = getRib().getPeerTracker().registerPeerAddedListener(added -> {
            if (added == bgpPeer) {
                notifications.incrementAndGet();
                peerMonitorHeld.set(Thread.holdsLock(bgpPeer));
                registered.countDown();
            }
        });

        try (listener) {
            bgpPeer.onSessionUp(session);
            final var first = completePeerStructureFirst ? peerStructure : ribOut;
            final var second = completePeerStructureFirst ? ribOut : peerStructure;

            first.set(CommitInfo.empty());
            assertFalse("peer registered before both initialization commits completed",
                registered.await(100, TimeUnit.MILLISECONDS));

            second.set(CommitInfo.empty());
            assertTrue("peer was not registered after both initialization commits completed",
                registered.await(5, TimeUnit.SECONDS));
            assertEquals("peer must be registered exactly once", 1, notifications.get());
            assertFalse("BGPPeer lock held while notifying peer-added listeners - deadlocks against LocRibWriter",
                peerMonitorHeld.get());
        } finally {
            peerStructure.set(CommitInfo.empty());
            ribOut.set(CommitInfo.empty());
            bgpPeer.close();
        }
    }

    private DataTreeCandidate effRibInEvent(final PeerId peerId, final MapEntryNode tableNode) {
        final var tablePath = ribIId.node(PEER_NID)
            .node(IdentifierUtils.domPeerId(peerId))
            .node(EFFRIBIN_NID)
            .node(TABLES_NID)
            .node(tableNode.name());
        return DataTreeCandidates.fromNormalizedNode(tablePath, tableNode);
    }

    private MapEntryNode buildTableWithRoute() {
        final var attributes = new AttributesBuilder()
            .setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(100)).build())
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setMultiExitDisc(new MultiExitDiscBuilder().setMed(Uint32.ZERO).build())
            .setAsPath(new AsPathBuilder().setSegments(List.of()).build())
            .build();
        final var route = new Ipv4RouteBuilder()
            .setRouteKey(PREFIX)
            .setPathId(new PathId(Uint32.ONE))
            .setPrefix(new Ipv4Prefix(PREFIX))
            .setAttributes(attributes)
            .build();
        final var routes = new Ipv4RoutesCaseBuilder()
            .setIpv4Routes(new Ipv4RoutesBuilder().setIpv4Route(Map.of(route.key(), route)).build())
            .build();
        final var table = new TablesBuilder().withKey(TABLES_KEY).setRoutes(routes).build();

        final var tableId = DataObjectIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("rib")))
            .child(LocRib.class)
            .child(Tables.class, TABLES_KEY)
            .build();
        final var serializer = (BindingNormalizedNodeSerializer) mappingService.currentSerializer();
        return (MapEntryNode) ((NodeResult) serializer.toNormalizedNode(tableId, table)).node();
    }
}
