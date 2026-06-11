/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ATTRIBUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.LOCRIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ROUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.UPTODATE_NID;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.LongAdder;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.DataTreeChangeExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteOperations;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RibOutRefresh;
import org.opendaylight.protocol.bgp.rib.impl.state.rib.TotalPathsCounter;
import org.opendaylight.protocol.bgp.rib.impl.state.rib.TotalPrefixesCounter;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBNormalizedNodes;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.ChoiceIn;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is NOT thread-safe
final class LocRibWriter<C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
        implements AutoCloseable, RibOutRefresh, TotalPrefixesCounter, TotalPathsCounter, DOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(LocRibWriter.class);

    private final Map<String, RouteEntry<C, S>> routeEntries = new HashMap<>();
    private final long ourAs;
    private final RIBSupport<C, S> ribSupport;
    private final DataTreeChangeExtension dataBroker;
    private final PathSelectionMode pathSelectionMode;
    private final LongAdder totalPathsCounter = new LongAdder();
    private final LongAdder totalPrefixesCounter = new LongAdder();
    private final RouteEntryDependenciesContainerImpl entryDep;
    private final BGPPeerTracker peerTracker;
    private final YangInstanceIdentifier ribIId;
    private final YangInstanceIdentifier locRibTableIID;
    @GuardedBy("this")
    private final Map<PeerId, FluentFuture<? extends CommitInfo>> pendingRibOut = new HashMap<>();

    private DOMTransactionChain chain;
    @GuardedBy("this")
    private Registration reg;
    @GuardedBy("this")
    private Registration peerRegistration;
    // Written only while holding the lock in init and close, read without the lock in onDataTreeChanged.
    private volatile ExecutorService executor;

    private LocRibWriter(final RIBSupport<C, S> ribSupport,
            final DOMTransactionChain chain,
            final YangInstanceIdentifier ribIId,
            final Uint32 ourAs,
            final DataTreeChangeExtension dataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            final BGPPeerTracker peerTracker,
            final AfiSafiType afiSafiType,
            final PathSelectionMode pathSelectionMode) {
        this.chain = requireNonNull(chain);
        this.ribIId = requireNonNull(ribIId);
        this.ribSupport = requireNonNull(ribSupport);

        locRibTableIID = ribIId.node(LOCRIB_NID).node(TABLES_NID).node(ribSupport.emptyTable().name()).toOptimized();

        this.ourAs = ourAs.toJava();
        this.dataBroker = requireNonNull(dataBroker);
        this.peerTracker = peerTracker;
        this.pathSelectionMode = pathSelectionMode;

        entryDep = new RouteEntryDependenciesContainerImpl(this.ribSupport, this.peerTracker, ribPolicies,
                afiSafiType, locRibTableIID);
    }

    public static <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
                LocRibWriter<C, S> create(
            final @NonNull RIBSupport<C, S> ribSupport,
            final @NonNull AfiSafiType afiSafiType,
            final @NonNull DOMTransactionChain chain,
            final @NonNull YangInstanceIdentifier ribIId,
            final @NonNull AsNumber ourAs,
            final @NonNull DataTreeChangeExtension dataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            final @NonNull BGPPeerTracker peerTracker,
            final @NonNull PathSelectionMode pathSelectionStrategy) {
        final LocRibWriter<C, S> ret = new LocRibWriter<>(ribSupport, chain, ribIId, ourAs.getValue(), dataBroker,
            ribPolicies, peerTracker, afiSafiType, pathSelectionStrategy);
        ret.init();
        return ret;
    }

    private synchronized void init() {
        // One thread, so batches are processed in the order they arrived. When 10000 batches are queued, the
        // caller waits for free space instead of a batch being thrown away. The thread is created on demand
        // and removed again when idle.
        executor = SpecialExecutors.newBlockingBoundedFastThreadPool(1, 10_000, "bgp-locrib-writer",
            LocRibWriter.class);
        final DOMDataTreeWriteTransaction tx = chain.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, locRibTableIID.node(ATTRIBUTES_NID).node(UPTODATE_NID),
                RIBNormalizedNodes.ATTRIBUTES_UPTODATE_TRUE);
        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed commit", trw);
            }
        }, MoreExecutors.directExecutor());

        reg = dataBroker.registerTreeChangeListener(DOMDataTreeIdentifier.of(
            LogicalDatastoreType.OPERATIONAL, ribIId.node(PEER_NID).node(PEER_NID).node(EFFRIBIN_NID).node(TABLES_NID)
                .node(locRibTableIID.getLastPathArgument())), this);
        peerRegistration = peerTracker.registerPeerAddedListener(this::onPeerAdded);
    }

    /**
     * Re-initialize this LocRibWriter with new transaction chain.
     *
     * @param newChain new transaction chain
     */
    synchronized void restart(final @NonNull DOMTransactionChain newChain) {
        requireNonNull(newChain);
        close();
        chain = newChain;
        init();
    }

    @Override
    public synchronized void close() {
        if (peerRegistration != null) {
            peerRegistration.close();
            peerRegistration = null;
        }
        if (reg != null) {
            reg.close();
            reg = null;
        }
        if (chain != null) {
            chain.close();
            chain = null;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    private @NonNull RouteEntry<C, S> createEntry(final String routeId) {
        final RouteEntry<C, S> ret = pathSelectionMode.createRouteEntry();
        routeEntries.put(routeId, ret);
        totalPrefixesCounter.increment();
        LOG.trace("Created new entry for {}", routeId);
        return ret;
    }

    @Override
    public synchronized void onInitialData() {
        // FIXME: we need to do something
    }

    @Override
    public void onDataTreeChanged(final List<DataTreeCandidate> changes) {
        // onDataTreeChanged is called on a thread shared by every data tree change listener in the controller.
        // processChanges can wait in awaitCommit for a long time, and that wait must not block the shared
        // thread, so the batch is handed over to this writer's own thread.
        final var exec = executor;
        if (exec == null || exec.isShutdown()) {
            LOG.trace("Writer closed, ignoring received data change {} to LocRib {}", changes, this);
            return;
        }
        exec.execute(() -> processChanges(changes));
    }

    /**
     * We use two-stage processing here in hopes that we avoid duplicate
     * calculations when multiple peers have changed a particular entry.
     *
     * @param changes on supported table
     */
    @SuppressWarnings("checkstyle:illegalCatch")
    private synchronized void processChanges(final List<DataTreeCandidate> changes) {
        if (chain == null) {
            LOG.trace("Chain closed, ignoring received data change {} to LocRib {}", changes, this);
            return;
        }
        LOG.trace("Received data change {} to LocRib {}", changes, this);
        final DOMDataTreeWriteTransaction tx = chain.newWriteOnlyTransaction();
        try {
            final Map<RouteUpdateKey, RouteEntry<C, S>> toUpdate = update(tx, changes);

            if (!toUpdate.isEmpty()) {
                walkThrough(tx, toUpdate.entrySet());
            }
        } catch (final Exception e) {
            LOG.error("Failed to completely propagate updates {}, state is undefined", changes, e);
        } finally {
            tx.commit().addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    LOG.trace("Successful commit");
                }

                @Override
                public void onFailure(final Throwable trw) {
                    LOG.error("Failed commit", trw);
                }
            }, MoreExecutors.directExecutor());
        }
    }

    private Map<RouteUpdateKey, RouteEntry<C, S>> update(final DOMDataTreeWriteOperations tx,
            final Collection<DataTreeCandidate> changes) {
        final Map<RouteUpdateKey, RouteEntry<C, S>> ret = new HashMap<>();
        for (final DataTreeCandidate tc : changes) {
            final DataTreeCandidateNode table = tc.getRootNode();
            final RouterId peerUuid = RouterId.forPeerId(IdentifierUtils.peerKeyToPeerId(tc.getRootPath()));
            updateNodes(table, peerUuid, tx, ret);
        }
        return ret;
    }

    /**
     * Advertises the routes already present in the loc-rib to a peer that has just registered.
     *
     * <p>A peer can register after its table-creation event has been processed, so its initial routes are sent here
     * rather than on that event, which would otherwise miss them.
     *
     * @param peer the newly added {@link Peer}
     */
    private synchronized void onPeerAdded(final @NonNull Peer peer) {
        // An application peer (PeerRole.Internal) has no BGP session. Nothing sends its AdjRibsOut anywhere and
        // later route updates never reach it, so skip the dump instead of writing data nothing will read.
        if (peer.getRole() == PeerRole.Internal || routeEntries.isEmpty()
                || !peer.supportsTable(entryDep.getLocalTablesKey())) {
            return;
        }
        LOG.debug("Peer {} registered, inserting existing loc-rib routes", peer.getPeerId());
        final var routesToStore = new ArrayList<ActualBestPathRoutes<C, S>>();
        for (final var entry : routeEntries.entrySet()) {
            final var filteredRoute = entry.getValue().actualBestPaths(ribSupport,
                new RouteEntryInfoImpl(peer, entry.getKey()));
            routesToStore.addAll(filteredRoute);
        }
        peer.initializeRibOut(entryDep, routesToStore);
    }

    private void updateNodes(final DataTreeCandidateNode table, final RouterId peerUuid,
            final DOMDataTreeWriteOperations tx, final Map<RouteUpdateKey, RouteEntry<C, S>> routes) {
        final var modifiedAttrs = table.modifiedChild(ATTRIBUTES_NID);
        if (modifiedAttrs != null) {
            final var newAttValue = modifiedAttrs.dataAfter();
            if (newAttValue != null) {
                LOG.trace("Uptodate found for {}", newAttValue);
                tx.put(LogicalDatastoreType.OPERATIONAL, locRibTableIID.node(ATTRIBUTES_NID), newAttValue);
            }
        }
        final var modifiedRoutes = table.modifiedChild(ROUTES_NID);
        if (modifiedRoutes != null) {
            updateRoutesEntries(ribSupport.changedRoutes(modifiedRoutes), peerUuid, routes);
        }
    }

    private void updateRoutesEntries(final Collection<DataTreeCandidateNode> collection,
            final RouterId routerId, final Map<RouteUpdateKey, RouteEntry<C, S>> routes) {
        for (final DataTreeCandidateNode route : collection) {
            final PathArgument routeArg = route.name();
            if (!(routeArg instanceof NodeIdentifierWithPredicates routeId)) {
                LOG.debug("Route {} already deleted", routeArg);
                return;
            }

            final String routeKey = ribSupport.extractRouteKey(routeId);
            final Uint32 pathId = ribSupport.extractPathId(routeId);

            RouteEntry<C, S> entry;
            switch (route.modificationType()) {
                case DELETE:
                    entry = routeEntries.get(routeKey);
                    if (entry != null) {
                        totalPathsCounter.decrement();
                        if (entry.removeRoute(routerId, pathId)) {
                            routeEntries.remove(routeKey);
                            totalPrefixesCounter.decrement();
                            LOG.trace("Removed route from {}", routerId);
                        }
                    }
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    entry = routeEntries.get(routeKey);
                    if (entry == null) {
                        entry = createEntry(routeKey);
                    }

                    final var routeAfter = route.getDataAfter();
                    verify(routeAfter instanceof MapEntryNode, "Unexpected route %s", routeAfter);
                    entry.addRoute(routerId, pathId, (MapEntryNode) routeAfter);
                    totalPathsCounter.increment();
                    break;
                default:
                    throw new IllegalStateException("Unhandled route modification " + route);
            }

            final RouteUpdateKey routeUpdateKey = new RouteUpdateKey(routerId, routeKey);
            LOG.debug("Updated route {} entry {}", routeKey, entry);
            routes.put(routeUpdateKey, entry);
        }
    }

    @Holding("this")
    private void walkThrough(final DOMDataTreeWriteOperations tx,
            final Set<Entry<RouteUpdateKey, RouteEntry<C, S>>> toUpdate) {
        final List<StaleBestPathRoute> staleRoutes = new ArrayList<>();
        final List<AdvertizedRoute<C, S>> newRoutes = new ArrayList<>();
        for (final Entry<RouteUpdateKey, RouteEntry<C, S>> e : toUpdate) {
            LOG.trace("Walking through {}", e);
            final RouteEntry<C, S> entry = e.getValue();

            if (!entry.selectBest(ribSupport, ourAs)) {
                LOG.trace("Best path has not changed, continuing");
                continue;
            }

            entry.removeStalePaths(ribSupport, e.getKey().getRouteId()).ifPresent(staleRoutes::add);
            newRoutes.addAll(entry.newBestPaths(ribSupport, e.getKey().getRouteId()));
        }
        updateLocRib(newRoutes, staleRoutes, tx);
        // Nothing to advertise or withdraw, so skip the per-peer fan-out and avoid opening empty transactions.
        if (staleRoutes.isEmpty() && newRoutes.isEmpty()) {
            return;
        }
        // Wait for the peer's previous commit before writing its next update. A finished commit does not block,
        // so this only slows the fan-out down while the datastore is falling behind.
        final var tables = entryDep.getLocalTablesKey();
        var written = 0;
        for (final var toPeer : peerTracker.getNonInternalPeers()) {
            if (toPeer.supportsTable(tables)) {
                final var peerId = toPeer.getPeerId();
                awaitCommit(peerId, pendingRibOut.get(peerId));
                pendingRibOut.put(peerId, toPeer.refreshRibOut(entryDep, staleRoutes, newRoutes));
                written++;
            }
        }
        // Each peer written above has exactly one entry, so a bigger map holds leftovers of peers that are gone
        // or lost this table. Drop them, since nothing will ever wait on their commits again.
        if (pendingRibOut.size() > written) {
            pendingRibOut.keySet().removeIf(peerId -> {
                final var peer = peerTracker.getPeer(peerId);
                return peer == null || !peer.supportsTable(tables);
            });
        }
    }

    private void updateLocRib(final List<AdvertizedRoute<C, S>> newRoutes, final List<StaleBestPathRoute> staleRoutes,
            final DOMDataTreeWriteOperations tx) {
        final YangInstanceIdentifier locRibTarget = entryDep.getLocRibTableTarget();

        for (final StaleBestPathRoute staleContainer : staleRoutes) {
            for (final NodeIdentifierWithPredicates routeId : staleContainer.getStaleRouteKeyIdentifiers()) {
                final YangInstanceIdentifier routeTarget = ribSupport.createRouteIdentifier(locRibTarget, routeId);
                LOG.debug("Delete route from LocRib {}", routeTarget);
                tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
            }
        }

        for (final AdvertizedRoute<C, S> advRoute : newRoutes) {
            final MapEntryNode route = advRoute.getRoute();
            final NodeIdentifierWithPredicates iid = advRoute.getAddPathRouteKeyIdentifier();
            final YangInstanceIdentifier locRibRouteTarget = ribSupport.createRouteIdentifier(locRibTarget, iid);
            LOG.debug("Write LocRib route {}", locRibRouteTarget);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Write route to LocRib {}", NormalizedNodes.toStringTree(route));
            }
            tx.put(LogicalDatastoreType.OPERATIONAL, locRibRouteTarget, route);
        }
    }

    @Override
    public long getPrefixesCount() {
        return totalPrefixesCounter.longValue();
    }

    @Override
    public long getPathsCount() {
        return totalPathsCounter.longValue();
    }

    TablesKey getTableKey() {
        return ribSupport.getTablesKey();
    }

    @Override
    public void refreshTable(final TablesKey tk, final PeerId peerId) {
        // Called on a thread shared by every data tree change listener in the controller, from
        // EffectiveRibInWriter. The refresh can wait for this writer's lock while a batch waits in awaitCommit,
        // so it runs on the writer's own thread instead.
        final var exec = executor;
        if (exec == null || exec.isShutdown()) {
            LOG.trace("Writer closed, ignoring table refresh for peer {}", peerId);
            return;
        }
        exec.execute(() -> refreshTableNow(peerId));
    }

    private synchronized void refreshTableNow(final PeerId peerId) {
        final var toPeer = peerTracker.getPeer(peerId);
        if (toPeer != null && toPeer.supportsTable(entryDep.getLocalTablesKey())) {
            LOG.debug("Peer {} table has been created, inserting existent routes", toPeer.getPeerId());
            final var routesToStore = new ArrayList<ActualBestPathRoutes<C, S>>();
            for (final var entry : routeEntries.entrySet()) {
                final var filteredRoute = entry.getValue().actualBestPaths(ribSupport,
                    new RouteEntryInfoImpl(toPeer, entry.getKey()));
                routesToStore.addAll(filteredRoute);
            }
            toPeer.reEvaluateAdvertizement(entryDep, routesToStore);
        }
    }

    private static void awaitCommit(final PeerId peerId, final FluentFuture<? extends CommitInfo> future) {
        if (future == null) {
            return;
        }
        try {
            // Reacting to interruption here would interrupt rest of the batch, as every subsequent get() would throw
            // immediately while the interrupt flag is set. The wait is bounded, since MDSAL completes the future on
            // success, failure or its own request timeout.
            Uninterruptibles.getUninterruptibly(future);
        } catch (ExecutionException e) {
            LOG.debug("Previous AdjRibsOut commit for peer {} failed, proceeding with its next update", peerId, e);
        }
    }
}
