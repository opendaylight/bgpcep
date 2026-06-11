/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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
 * Tests {@link LocRibWriter} fan-out. The writer must not open a peer's AdjRibsOut transaction until the commit
 * two peers back completes, holding at most two transactions in flight regardless of peer count.
 */
public class LocRibWriterPacingTest extends AbstractRIBTestSetup {
    private static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.VALUE,
        UnicastSubsequentAddressFamily.VALUE);
    private static final QName PEER_ID_QNAME = QName.create(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml
        .ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer.QNAME, "peer-id").intern();

    private BGPPeerTracker peerTracker;
    private List<Peer> fannedOutPeers;
    private LocRibWriter<?, ?> locRibWriter;
    private DataTreeCandidate routeChange;
    private Thread worker;

    private final List<Registration> registrations = new ArrayList<>();

    @Before
    public void setUpWriter() {
        peerTracker = getRib().getPeerTracker();
        registrations.add(peerTracker.registerPeer(mockPeer("bgp://10.0.0.1")));
        registrations.add(peerTracker.registerPeer(mockPeer("bgp://10.0.0.2")));
        registrations.add(peerTracker.registerPeer(mockPeer("bgp://10.0.0.3")));
        fannedOutPeers = peerTracker.getNonInternalPeers();

        locRibWriter = createWriter(getRib().getRibExtensions().getRIBSupport(TABLES_KEY));
        routeChange = effectiveRibInRouteChange();
    }

    @After
    public void closeWriter() {
        if (worker != null) {
            try {
                worker.join(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            registrations.forEach(Registration::close);
        } finally {
            locRibWriter.close();
        }
    }

    /*
     * The writer opens at most two AdjRibsOut transactions at once. The third peer is served only after the first
     * commit completes.
     */
    @Test
    public void testNextTransactionWaitsForPreviousCommit() {
        final var firstCommit = SettableFuture.create();
        final var first = fannedOutPeers.get(0);
        doReturn(FluentFuture.from(firstCommit)).when(first)
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        final var secondCommit = SettableFuture.create();
        final var second = fannedOutPeers.get(1);
        doReturn(FluentFuture.from(secondCommit)).when(second)
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        final var third = fannedOutPeers.get(2);
        doReturn(CommitInfo.emptyFluentFuture()).when(third)
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());

        // onDataTreeChanged blocks on the held commits, so run it off the test thread.
        worker = new Thread(() -> locRibWriter.onDataTreeChanged(List.of(routeChange)));
        worker.start();

        // The first two transactions open at once. The writer then waits for the first commit.
        verify(first, timeout(10_000)).refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        verify(second, timeout(10_000)).refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());

        // The third transaction must not open until the first commits.
        verify(third, after(500).never())
            .refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());

        // Complete the first commit. The writer opens the third transaction.
        firstCommit.set(CommitInfo.empty());
        verify(third, timeout(10_000)).refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());

        // Complete the last commit. The writer finishes and commits its LocRib transaction.
        secondCommit.set(CommitInfo.empty());
        verify(getTransaction(), timeout(10_000).times(2)).commit();
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
        assertEquals(3, fannedOutPeers.size());
    }

    private static Peer mockPeer(final String peerId) {
        final var peer = mock(Peer.class);
        doReturn(new PeerId(peerId)).when(peer).getPeerId();
        doReturn(PeerRole.Ibgp).when(peer).getRole();
        doReturn(true).when(peer).supportsTable(TABLES_KEY);
        return peer;
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
    private DataTreeCandidate effectiveRibInRouteChange() {
        final var prefix = new Ipv4Prefix("1.1.1.0/24");
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
}
