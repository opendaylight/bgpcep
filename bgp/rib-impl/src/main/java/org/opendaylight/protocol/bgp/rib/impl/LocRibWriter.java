/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ATTRIBUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.LOCRIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ROUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.UPTODATE_NID;

import com.google.common.base.VerifyException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.DataTreeChangeExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteOperations;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RibOutRefresh;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RIBNormalizedNodes;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTableState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is NOT thread-safe
final class LocRibWriter implements AutoCloseable, RibOutRefresh, DOMDataTreeChangeListener {
    // internal state
    private sealed interface RouteEntries extends BGPTableState permits HashMapRouteEntries {

        @NonNullByDefault
        RouteEntry putRoute(RouterId routerId, String routeKey, Uint32 pathId, MapEntryNode route);

        @NonNullByDefault
        @Nullable RouteEntry removeRoute(RouterId routerId, String routeKey, Uint32 pathId);

        @NonNull Set<Map.Entry<@NonNull String, RouteEntry>> asEntrySet();

        // FIXME: ugly overlay with Map, we should be able to do better
        boolean isEmpty();
    }

    // HashMap-based implementation: a subclass to remove the cost of encapsulation.
    private static final class HashMapRouteEntries extends HashMap<@NonNull String, RouteEntry>
            implements RouteEntries {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        // read without any synchronization guarantees
        private final @NonNull LongAdder installedPrefixes = new LongAdder();
        private final @NonNull LongAdder installedPaths = new LongAdder();
        private final @NonNull PathSelectionMode pathSelectionMode;

        HashMapRouteEntries(final PathSelectionMode pathSelectionMode) {
            this.pathSelectionMode = requireNonNull(pathSelectionMode);
        }

        @Override
        public long installedPaths() {
            return installedPaths.sum();
        }

        @Override
        public long installedPrefixes() {
            return installedPrefixes.sum();
        }

        @Override
        public RouteEntry putRoute(final RouterId routerId, final String routeKey, final Uint32 pathId,
                final MapEntryNode route) {
            final var routeEntry = computeIfAbsent(routeKey, this::newRouteEntry);
            if (routeEntry.putRoute(routerId, pathId, route)) {
                installedPaths.increment();
            }
            return routeEntry;
        }

        @NonNullByDefault
        private RouteEntry newRouteEntry(final String routeKey) {
            final var routeEntry = pathSelectionMode.createRouteEntry();
            installedPrefixes.increment();
            LOG.trace("Created new entry for {}", routeKey);
            return routeEntry;
        }

        @Override
        public @Nullable RouteEntry removeRoute(final RouterId routerId, final String routeKey, final Uint32 pathId) {
            final var routeEntry = get(routeKey);
            if (routeEntry != null) {
                installedPaths.decrement();
                if (routeEntry.removeRoute(routerId, pathId)) {
                    remove(routeKey);
                    installedPrefixes.decrement();
                    LOG.trace("Removed route from {}", routerId);
                }
            }
            return routeEntry;
        }

        @Override
        public Set<Map.Entry<@NonNull String, RouteEntry>> asEntrySet() {
            return entrySet();
        }

        @java.io.Serial
        @SuppressWarnings("static-method")
        private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
            throw nse();
        }

        @java.io.Serial
        @SuppressWarnings("static-method")
        private void readObjectNoData() throws ObjectStreamException {
            throw nse();
        }

        @java.io.Serial
        @SuppressWarnings("static-method")
        private void writeObject(final ObjectOutputStream stream) throws IOException {
            throw nse();
        }

        private static NotSerializableException nse() {
            return new NotSerializableException(HashMapRouteEntries.class.getName());
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocRibWriter.class);

    private final @NonNull RouteEntries routeEntries;
    private final long ourAs;
    private final @NonNull RIBSupport ribSupport;
    private final @NonNull DataTreeChangeExtension dataBroker;
    private final @NonNull RouteEntryDependenciesContainerImpl entryDep;
    private final BGPPeerTracker peerTracker;
    private final @NonNull YangInstanceIdentifier ribIId;
    private final @NonNull YangInstanceIdentifier locRibTableIID;

    private DOMTransactionChain chain;
    @GuardedBy("this")
    private Registration reg;

    private LocRibWriter(final RIBSupport ribSupport,
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
        routeEntries = new HashMapRouteEntries(pathSelectionMode);
        entryDep = new RouteEntryDependenciesContainerImpl(ribSupport, peerTracker, ribPolicies, afiSafiType,
            locRibTableIID);
    }

    public static @NonNull LocRibWriter create(
            final @NonNull RIBSupport ribSupport,
            final @NonNull AfiSafiType afiSafiType,
            final @NonNull DOMTransactionChain chain,
            final @NonNull YangInstanceIdentifier ribIId,
            final @NonNull AsNumber ourAs,
            final @NonNull DataTreeChangeExtension dataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            final @NonNull BGPPeerTracker peerTracker,
            final @NonNull PathSelectionMode pathSelectionStrategy) {
        final var ret = new LocRibWriter(ribSupport, chain, ribIId, ourAs.getValue(), dataBroker,
            ribPolicies, peerTracker, afiSafiType, pathSelectionStrategy);
        ret.init();
        return ret;
    }

    private synchronized void init() {
        final var tx = chain.newWriteOnlyTransaction();
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
    }

    @NonNull BGPTableState asTableState() {
        return routeEntries;
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
        if (reg != null) {
            reg.close();
            reg = null;
        }
        if (chain != null) {
            chain.close();
            chain = null;
        }
    }

    @Override
    public synchronized void onInitialData() {
        // FIXME: we need to do something
    }

    /**
     * We use two-stage processing here in hopes that we avoid duplicate
     * calculations when multiple peers have changed a particular entry.
     *
     * @param changes on supported table
     */
    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    public synchronized void onDataTreeChanged(final List<DataTreeCandidate> changes) {
        if (chain == null) {
            LOG.trace("Chain closed, ignoring received data change {} to LocRib {}", changes, this);
            return;
        }
        LOG.trace("Received data change {} to LocRib {}", changes, this);
        final var tx = chain.newWriteOnlyTransaction();
        try {
            final var toUpdate = update(tx, changes);
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

    private Map<RouteUpdateKey, RouteEntry> update(final DOMDataTreeWriteOperations tx,
            final List<DataTreeCandidate> changes) {
        final var ret = new HashMap<RouteUpdateKey, RouteEntry>();
        for (var tc : changes) {
            final var table = tc.getRootNode();
            final var peerUuid = RouterId.forPeerId(IdentifierUtils.peerKeyToPeerId(tc.getRootPath()));

            // Initialize Peer with routes under loc rib
            if (!routeEntries.isEmpty() && table.dataBefore() == null) {
                final var toPeer = peerTracker.getPeer(peerUuid.getPeerId());
                if (toPeer != null && toPeer.supportsTable(entryDep.getLocalTablesKey())) {
                    LOG.debug("Peer {} table has been created, inserting existent routes", toPeer.getPeerId());
                    final var routesToStore = new ArrayList<ActualBestPathRoutes>();
                    for (var entry : routeEntries.asEntrySet()) {
                        routesToStore.addAll(entry.getValue()
                            .actualBestPaths(ribSupport, new RouteEntryInfoImpl(toPeer, entry.getKey())));
                    }
                    toPeer.initializeRibOut(entryDep, routesToStore);
                }
            }
            // Process new routes from Peer
            updateNodes(table, peerUuid, tx, ret);
        }
        return ret;
    }

    private void updateNodes(final DataTreeCandidateNode table, final @NonNull RouterId peerUuid,
            final DOMDataTreeWriteOperations tx, final Map<RouteUpdateKey, RouteEntry> routes) {
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
            final @NonNull RouterId routerId, final Map<RouteUpdateKey, RouteEntry> routes) {
        for (var route : collection) {
            final var routeArg = route.name();
            if (!(routeArg instanceof NodeIdentifierWithPredicates routeId)) {
                LOG.debug("Route {} already deleted", routeArg);
                return;
            }

            // split out early for validation
            final var routeKey = ribSupport.extractRouteKey(routeId);
            final var pathId = ribSupport.extractPathId(routeId);
            final var routeEntry = switch (route.modificationType()) {
                case APPEARED, DISAPPEARED, UNMODIFIED ->
                    throw new IllegalStateException("Unhandled route modification " + route);
                case DELETE -> routeEntries.removeRoute(routerId, routeKey, pathId);
                case SUBTREE_MODIFIED, WRITE -> {
                    final var dataAfter = route.getDataAfter();
                    if (dataAfter instanceof MapEntryNode routeAfter) {
                        yield routeEntries.putRoute(routerId, routeKey, pathId, routeAfter);
                    }
                    throw new VerifyException("Unexpected route type " + dataAfter.contract().getCanonicalName());
                }
            };

            final var routeUpdateKey = new RouteUpdateKey(routerId, routeKey);
            LOG.debug("Updated route {} entry {}", routeKey, routeEntry);
            routes.put(routeUpdateKey, routeEntry);
        }
    }

    private void walkThrough(final DOMDataTreeWriteOperations tx,
            final Set<Map.Entry<RouteUpdateKey, RouteEntry>> toUpdate) {
        final var staleRoutes = new ArrayList<StaleBestPathRoute>();
        final var newRoutes = new ArrayList<AdvertizedRoute>();
        for (var e : toUpdate) {
            LOG.trace("Walking through {}", e);
            final var entry = e.getValue();
            if (!entry.selectBest(ribSupport, ourAs)) {
                LOG.trace("Best path has not changed, continuing");
                continue;
            }

            final var routeKey = e.getKey().routeKey();
            final var stateRoute = entry.removeStalePaths(ribSupport, routeKey);
            if (stateRoute != null) {
                staleRoutes.add(stateRoute);
            }
            newRoutes.addAll(entry.newBestPaths(ribSupport, routeKey));
        }
        updateLocRib(newRoutes, staleRoutes, tx);
        peerTracker.getNonInternalPeers().parallelStream()
            .filter(toPeer -> toPeer.supportsTable(entryDep.getLocalTablesKey()))
            .forEach(toPeer -> toPeer.refreshRibOut(entryDep, staleRoutes, newRoutes));
    }

    private void updateLocRib(final List<AdvertizedRoute> newRoutes, final List<StaleBestPathRoute> staleRoutes,
            final DOMDataTreeWriteOperations tx) {
        final var locRibTarget = entryDep.getLocRibTableTarget();

        for (var staleContainer : staleRoutes) {
            for (var routeId : staleContainer.getStaleRouteKeyIdentifiers()) {
                final var routeTarget = ribSupport.createRouteIdentifier(locRibTarget, routeId);
                LOG.debug("Delete route from LocRib {}", routeTarget);
                tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
            }
        }

        for (var advRoute : newRoutes) {
            final var route = advRoute.getRoute();
            final var iid = advRoute.getAddPathRouteKeyIdentifier();
            final var locRibRouteTarget = ribSupport.createRouteIdentifier(locRibTarget, iid);
            LOG.debug("Write LocRib route {}", locRibRouteTarget);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Write route to LocRib {}", NormalizedNodes.toStringTree(route));
            }
            tx.put(LogicalDatastoreType.OPERATIONAL, locRibRouteTarget, route);
        }
    }

    TablesKey getTableKey() {
        return ribSupport.getTablesKey();
    }

    @Override
    public synchronized void refreshTable(final TablesKey tk, final PeerId peerId) {
        final var toPeer = peerTracker.getPeer(peerId);
        if (toPeer != null && toPeer.supportsTable(entryDep.getLocalTablesKey())) {
            LOG.debug("Peer {} table has been created, inserting existent routes", toPeer.getPeerId());
            final var routesToStore = new ArrayList<ActualBestPathRoutes>();
            for (var entry : routeEntries.asEntrySet()) {
                routesToStore.addAll(entry.getValue()
                    .actualBestPaths(ribSupport, new RouteEntryInfoImpl(toPeer, entry.getKey())));
            }
            toPeer.reEvaluateAdvertizement(entryDep, routesToStore);
        }
    }
}
