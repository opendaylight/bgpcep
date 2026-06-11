/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.bootstrap.Bootstrap;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.GracefulRestartUtil;
import org.opendaylight.protocol.bgp.rib.impl.protocol.BGPProtocolSessionPromise;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.ChoiceIn;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

/**
 * Tests {@link LocRibWriter} fan-out pacing. Within one route change batch every peer is served independently.
 * Across batches the writer keeps at most one outstanding AdjRibsOut commit per peer. A peer is served again
 * only after its previous commit completes.
 */
public class Bgpcep1078Test extends AbstractRIBTestSetup {
    private static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.VALUE,
        UnicastSubsequentAddressFamily.VALUE);
    private static final QName PEER_ID_QNAME = QName.create(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml
        .ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer.QNAME, "peer-id").intern();

    private final List<Registration> registrations = new ArrayList<>();

    @Mock
    private Peer firstPeer;
    @Mock
    private Peer secondPeer;
    @Mock
    private Peer thirdPeer;
    @Mock
    private Peer fourthPeer;
    @Mock
    private BGPSession session;
    private BGPPeerTracker peerTracker;
    private List<Peer> fannedOutPeers;
    private LocRibWriter<?, ?> locRibWriter;
    private DataTreeCandidate routeChange;
    // Commit future a test holds open on purpose. Completed in teardown so a failed assertion cannot leave the
    // writer's thread blocked in a batch, which would also hang locRibWriter.close().
    private SettableFuture<CommitInfo> heldCommit;

    @Before
    public void setUpWriter() {
        peerTracker = getRib().getPeerTracker();
        registrations.add(peerTracker.registerPeer(initPeer(firstPeer, "bgp://10.0.0.1")));
        registrations.add(peerTracker.registerPeer(initPeer(secondPeer, "bgp://10.0.0.2")));
        registrations.add(peerTracker.registerPeer(initPeer(thirdPeer, "bgp://10.0.0.3")));
        fannedOutPeers = peerTracker.getNonInternalPeers();

        locRibWriter = createWriter(getRib().getRibExtensions().getRIBSupport(TABLES_KEY));
        routeChange = effectiveRibInRouteChange("1.1.1.0/24");
    }

    @After
    public void closeWriter() {
        try {
            if (heldCommit != null) {
                heldCommit.set(CommitInfo.empty());
            }
        } finally {
            registrations.forEach(Registration::close);
            locRibWriter.close();
        }
    }

    /*
     * Within one route change batch every peer is served immediately. An incomplete commit of one peer must not
     * delay the fan-out to the remaining peers.
     */
    @Test
    public void testFanOutDoesNotWaitWithinOneBatch() {
        heldCommit = SettableFuture.create();
        for (final var peer : fannedOutPeers) {
            doReturn(FluentFuture.from(heldCommit)).when(peer)
                .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        }

        locRibWriter.onDataTreeChanged(List.of(routeChange));

        // Every peer is served although no commit has completed.
        for (final var peer : fannedOutPeers) {
            verify(peer, timeout(10_000))
                .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        }
        // The writer finishes the batch and commits its LocRib transaction without waiting for the peers' commits.
        verify(getTransaction(), timeout(10_000).times(2)).commit();
    }

    /*
     * The writer keeps at most one outstanding commit per peer. A peer whose commit has not completed is not
     * served again until that commit completes.
     */
    @Test
    public void testNextBatchWaitsForSamePeerCommit() {
        heldCommit = SettableFuture.create();
        final var first = fannedOutPeers.get(0);
        doReturn(FluentFuture.from(heldCommit)).doReturn(CommitInfo.emptyFluentFuture()).when(first)
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        for (final var peer : fannedOutPeers.subList(1, fannedOutPeers.size())) {
            doReturn(CommitInfo.emptyFluentFuture()).when(peer)
                .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        }

        // The first batch completes without waiting, leaving the first peer's commit outstanding.
        locRibWriter.onDataTreeChanged(List.of(routeChange));
        for (final var peer : fannedOutPeers) {
            verify(peer, timeout(10_000))
                .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        }

        // The second batch stops at the first peer while its commit is outstanding, so nobody is served again.
        // The writer's thread processes batches in order, so the batch can be handed over right away.
        locRibWriter.onDataTreeChanged(List.of(effectiveRibInRouteChange("2.2.2.0/24")));
        for (final var peer : fannedOutPeers) {
            verify(peer, after(500).times(1))
                .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        }

        // Completing that commit releases the batch to all peers.
        heldCommit.set(CommitInfo.empty());
        for (final var peer : fannedOutPeers) {
            verify(peer, timeout(10_000).times(2))
                .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        }
        verify(getTransaction(), timeout(10_000).times(3)).commit();
    }

    /*
     * A peer that leaves is dropped from the pacing map. After it registers again, the writer must not wait on
     * the commit left open by the departed session. The tracker refreshes its peer lists on registration, so the
     * departure becomes visible to the fan-out together with the next registered peer.
     */
    @Test
    public void testDepartedPeerIsForgotten() {
        heldCommit = SettableFuture.create();
        final var first = fannedOutPeers.get(0);
        doReturn(FluentFuture.from(heldCommit)).when(first)
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        doReturn(CommitInfo.emptyFluentFuture()).when(first)
            .initializeRibOut(any(RouteEntryDependenciesContainer.class), anyList());
        for (final var peer : fannedOutPeers.subList(1, fannedOutPeers.size())) {
            doReturn(CommitInfo.emptyFluentFuture()).when(peer)
                .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        }
        doReturn(CommitInfo.emptyFluentFuture()).when(fourthPeer)
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        doReturn(CommitInfo.emptyFluentFuture()).when(fourthPeer)
            .initializeRibOut(any(RouteEntryDependenciesContainer.class), anyList());

        // The first batch leaves the first peer's commit open. Wait for its LocRib commit, the batch has to be
        // processed before the peers change.
        locRibWriter.onDataTreeChanged(List.of(routeChange));
        verify(getTransaction(), timeout(10_000).times(2)).commit();

        // The first peer leaves and another peer registers, refreshing the tracker's peer lists.
        registrations.get(0).close();
        registrations.add(peerTracker.registerPeer(initPeer(fourthPeer, "bgp://10.0.0.4")));

        // This batch runs without the departed peer and drops its map entry.
        locRibWriter.onDataTreeChanged(List.of(effectiveRibInRouteChange("2.2.2.0/24")));
        verify(getTransaction(), timeout(10_000).times(3)).commit();

        // The peer returns. Serving it again must not wait on its earlier commit which is still open.
        registrations.add(peerTracker.registerPeer(first));
        locRibWriter.onDataTreeChanged(List.of(effectiveRibInRouteChange("3.3.3.0/24")));
        verify(first, timeout(10_000).times(2))
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
    }

    /*
     * A peer registration that blocks on a busy LocRibWriter must occupy only the registration executor of the
     * RIB. The shared netty GlobalEventExecutor must stay free, because BGP session setup and reconnect promises
     * run their callbacks there, so parking it would stall connections of other peers.
     */
    @Test
    public void testBlockedRegistrationLeavesSharedThreadFree() throws InterruptedException {
        // Hold the first peer's commit open and let a second batch block inside the writer, holding its lock.
        heldCommit = SettableFuture.create();
        final var first = fannedOutPeers.get(0);
        doReturn(FluentFuture.from(heldCommit))
            .doReturn(CommitInfo.emptyFluentFuture()).when(first).refreshRibOut(
                any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        for (final var peer : fannedOutPeers.subList(1, fannedOutPeers.size())) {
            doReturn(CommitInfo.emptyFluentFuture()).when(peer)
                .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        }
        locRibWriter.onDataTreeChanged(List.of(routeChange));
        locRibWriter.onDataTreeChanged(List.of(effectiveRibInRouteChange("2.2.2.0/24")));
        awaitWriterParkedInCommit();

        // The commit future of the test harness is a mock. Hand the registration callback to the executor the
        // peer chose, the same way a completed commit would.
        final var commitFuture = getTransaction().commit();
        doAnswer(inv -> {
            final var callback = inv.getArgument(0, FutureCallback.class);
            inv.getArgument(1, Executor.class).execute(() -> callback.onSuccess(null));
            return null;
        }).when(commitFuture).addCallback(any(FutureCallback.class), any(Executor.class));

        // A real peer session comes up. Its registration runs into the busy writer and blocks.
        doReturn(new Ipv4Address("127.0.0.4")).when(session).getBgpId();
        doReturn(Set.of(new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE)))
            .when(session).getAdvertisedTableTypes();
        doReturn(List.of()).when(session).getAdvertisedAddPathTableTypes();
        doReturn(GracefulRestartUtil.EMPTY_GR_CAPABILITY).when(session).getAdvertisedGracefulRestartCapability();
        doReturn(GracefulRestartUtil.EMPTY_LLGR_CAPABILITY).when(session).getAdvertisedLlGracefulRestartCapability();
        final var bgpPeer = AbstractAddPathTest.configurePeer(tableRegistry, new Ipv4AddressNoZone("127.0.0.4"),
            getRib(), null, PeerRole.Ibgp, new StrictBGPPeerRegistry());
        // The tracker notifies the LocRibWriter listener first and this listener second, so the latch counts
        // down only after the blocked registration got through the writer's lock.
        final var registrationFinished = new CountDownLatch(1);
        final var listenerRegistration = peerTracker.registerPeerAddedListener(added -> {
            if (added == bgpPeer) {
                registrationFinished.countDown();
            }
        });
        try (listenerRegistration) {
            bgpPeer.onSessionUp(session);

            // Another peer establishes its connection meanwhile. BGPProtocolSessionPromise notifies its
            // listeners on the shared GlobalEventExecutor thread and session setup continues in them, so the
            // blocked registration must not sit on that thread.
            final var otherPeerConnection = new BGPProtocolSessionPromise<>(
                new InetSocketAddress("127.0.0.5", 1790), 0, new Bootstrap(),
                new StrictBGPPeerRegistry());
            final var otherPeerSessionStarted = new CountDownLatch(1);
            otherPeerConnection.addListener(future -> otherPeerSessionStarted.countDown());
            otherPeerConnection.setSuccess(session);
            assertTrue("blocked peer registration stalls session setup of other peers",
                otherPeerSessionStarted.await(10, TimeUnit.SECONDS));

            // The registration stays blocked until the writer releases its lock.
            assertFalse("peer registration did not block on the busy writer",
                registrationFinished.await(300, TimeUnit.MILLISECONDS));
            heldCommit.set(CommitInfo.empty());
            assertTrue("peer registration did not finish after the writer released its lock",
                registrationFinished.await(10, TimeUnit.SECONDS));
        } finally {
            bgpPeer.close();
        }
    }

    /*
     * A single route change is fanned out to every non-internal peer exactly once, in peer-tracker order.
     */
    @Test
    public void testAllPeersUpdatedSequentially() {
        fannedOutPeers.forEach(peer -> doReturn(CommitInfo.emptyFluentFuture()).when(peer)
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList()));

        locRibWriter.onDataTreeChanged(List.of(routeChange));
        // Wait for the batch's LocRib commit, the writer works on its own thread.
        verify(getTransaction(), timeout(10_000).times(2)).commit();
        final var order = inOrder(fannedOutPeers.toArray());
        for (final var peer : fannedOutPeers) {
            order.verify(peer).refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        }
    }

    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
            LocRibWriter<C, S> createWriter(final RIBSupport<C, S> support) {
        final var rib = getRib();
        return LocRibWriter.create(support, IPV4UNICAST.VALUE, rib.createPeerDOMChain(), rib.getYangRibId(),
            rib.getLocalAs(), rib.getService(), rib.getRibPolicies(), peerTracker,
            BasePathSelectionModeFactory.createBestPathSelectionStrategy());
    }

    /*
     * Build the data-tree change LocRibWriter listens on route advertised in a source peer's effective-rib-in.
     */
    private DataTreeCandidate effectiveRibInRouteChange(final String advertisedPrefix) {
        final var prefix = new Ipv4Prefix(advertisedPrefix);
        final var route = new Ipv4RouteBuilder()
            .setRouteKey(prefix.getValue())
            .setPathId(new PathId(Uint32.ZERO))
            .setPrefix(prefix)
            .setAttributes(new AttributesBuilder()
                .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
                .setAsPath(new AsPathBuilder().setSegments(List.of()).build())
                .setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(100)).build())
                .build())
            .build();
        final var routes = new Ipv4RoutesCaseBuilder()
            .setIpv4Routes(new Ipv4RoutesBuilder().setIpv4Route(Map.of(route.key(), route)).build())
            .build();
        final var tables = new TablesBuilder().withKey(TABLES_KEY).setRoutes(routes)
            .setAttributes(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329
                .rib.tables.AttributesBuilder().build())
            .build();
        final var tablesId = DataObjectIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("test")))
            .child(LocRib.class)
            .child(Tables.class, TABLES_KEY)
            .build();

        final var tableNode = mappingService.currentSerializer().toNormalizedDataObject(tablesId, tables).node();
        final var rootPath = getRib().getYangRibId()
            .node(PEER_NID)
            .node(NodeIdentifierWithPredicates.of(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                .rib.rev180329.bgp.rib.rib.Peer.QNAME, PEER_ID_QNAME, "bgp://10.0.0.9"))
            .node(EFFRIBIN_NID)
            .node(TABLES_NID)
            .node(tableNode.name());
        return DataTreeCandidates.fromNormalizedNode(rootPath, tableNode);
    }

    private static Peer initPeer(final Peer peer, final String peerId) {
        doReturn(new PeerId(peerId)).when(peer).getPeerId();
        doReturn(PeerRole.Ibgp).when(peer).getRole();
        doReturn(true).when(peer).supportsTable(TABLES_KEY);
        return peer;
    }

    /*
     * Waits until the writer's own thread sits in awaitCommit, holding the writer's lock.
     */
    private static void awaitWriterParkedInCommit() throws InterruptedException {
        final var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!writerParkedInCommit()) {
            assertTrue("timed out waiting for the writer thread to park in awaitCommit",
                System.nanoTime() < deadline);
            Thread.sleep(10);
        }
    }

    private static boolean writerParkedInCommit() {
        for (final var entry : Thread.getAllStackTraces().entrySet()) {
            if (entry.getKey().getName().startsWith("bgp-locrib-writer")) {
                for (final var frame : entry.getValue()) {
                    if ("awaitCommit".equals(frame.getMethodName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
