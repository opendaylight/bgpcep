/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
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

    @Mock
    private Peer firstPeer;
    @Mock
    private Peer secondPeer;
    @Mock
    private Peer thirdPeer;
    @Mock
    private Peer fourthPeer;
    private BGPPeerTracker peerTracker;
    private List<Peer> fannedOutPeers;
    private LocRibWriter<?, ?> locRibWriter;
    private DataTreeCandidate routeChange;
    private Thread worker;
    // Commit future a test holds open on purpose. Completed in teardown so a failed assertion cannot leave the
    // worker thread blocked in the writer, which would also hang locRibWriter.close().
    private SettableFuture<CommitInfo> heldCommit;

    private final List<Registration> registrations = new ArrayList<>();

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
    public void closeWriter() throws InterruptedException {
        try {
            if (heldCommit != null) {
                heldCommit.set(CommitInfo.empty());
            }
            if (worker != null) {
                worker.join(10_000);
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

        worker = new Thread(() -> locRibWriter.onDataTreeChanged(List.of(routeChange)));
        worker.start();

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
            verify(peer).refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        }

        // The second batch stops at the first peer while its commit is outstanding, so nobody is served again.
        final var secondChange = effectiveRibInRouteChange("2.2.2.0/24");
        worker = new Thread(() -> locRibWriter.onDataTreeChanged(List.of(secondChange)));
        worker.start();
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

        // The first batch leaves the first peer's commit open.
        locRibWriter.onDataTreeChanged(List.of(routeChange));

        // The first peer leaves and another peer registers, refreshing the tracker's peer lists.
        registrations.get(0).close();
        registrations.add(peerTracker.registerPeer(initPeer(fourthPeer, "bgp://10.0.0.4")));

        // This batch runs without the departed peer and drops its map entry.
        locRibWriter.onDataTreeChanged(List.of(effectiveRibInRouteChange("2.2.2.0/24")));

        // The peer returns. Serving it again must not wait on its earlier commit which is still open.
        registrations.add(peerTracker.registerPeer(first));
        final var thirdChange = effectiveRibInRouteChange("3.3.3.0/24");
        worker = new Thread(() -> locRibWriter.onDataTreeChanged(List.of(thirdChange)));
        worker.start();
        verify(first, timeout(10_000).times(2))
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
    }

    /*
     * A single route change is fanned out to every non-internal peer exactly once, in peer-tracker order.
     */
    @Test
    public void testAllPeersUpdatedSequentially() {
        fannedOutPeers.forEach(peer -> doReturn(CommitInfo.emptyFluentFuture()).when(peer)
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList()));

        locRibWriter.onDataTreeChanged(List.of(routeChange));
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
}
