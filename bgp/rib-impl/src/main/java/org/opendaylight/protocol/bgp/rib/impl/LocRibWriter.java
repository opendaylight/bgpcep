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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is NOT thread-safe
final class LocRibWriter<C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
        implements AutoCloseable, RibOutRefresh, TotalPrefixesCounter, TotalPathsCounter,
        ClusteredDOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(LocRibWriter.class);

    private final Map<String, RouteEntry<C, S>> routeEntries = new HashMap<>();
    private final long ourAs;
    private final RIBSupport<C, S> ribSupport;
    private final DOMDataTreeChangeService dataBroker;
    private final PathSelectionMode pathSelectionMode;
    private final LongAdder totalPathsCounter = new LongAdder();
    private final LongAdder totalPrefixesCounter = new LongAdder();
    private final RouteEntryDependenciesContainerImpl entryDep;
    private final BGPPeerTracker peerTracker;
    private final YangInstanceIdentifier ribIId;
    private final YangInstanceIdentifier locRibTableIID;

    private DOMTransactionChain chain;
    @GuardedBy("this")
    private ListenerRegistration<?> reg;

    private LocRibWriter(final RIBSupport<C, S> ribSupport,
            final DOMTransactionChain chain,
            final YangInstanceIdentifier ribIId,
            final Uint32 ourAs,
            final DOMDataTreeChangeService dataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            final BGPPeerTracker peerTracker,
            final AfiSafiType afiSafiType,
            final PathSelectionMode pathSelectionMode) {
        this.chain = requireNonNull(chain);
        this.ribIId = requireNonNull(ribIId);
        this.ribSupport = requireNonNull(ribSupport);

        this.locRibTableIID = ribIId.node(LOCRIB_NID).node(TABLES_NID).node(ribSupport.emptyTable().getIdentifier())
            .toOptimized();
        this.ourAs = ourAs.toJava();
        this.dataBroker = requireNonNull(dataBroker);
        this.peerTracker = peerTracker;
        this.pathSelectionMode = pathSelectionMode;

        this.entryDep = new RouteEntryDependenciesContainerImpl(this.ribSupport, this.peerTracker, ribPolicies,
                afiSafiType, this.locRibTableIID);
    }

    public static <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
                LocRibWriter<C, S> create(
            final @NonNull RIBSupport<C, S> ribSupport,
            final @NonNull AfiSafiType afiSafiType,
            final @NonNull DOMTransactionChain chain,
            final @NonNull YangInstanceIdentifier ribIId,
            final @NonNull AsNumber ourAs,
            final @NonNull DOMDataTreeChangeService dataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            final @NonNull BGPPeerTracker peerTracker,
            final @NonNull PathSelectionMode pathSelectionStrategy) {
        final LocRibWriter<C, S> ret = new LocRibWriter<>(ribSupport, chain, ribIId, ourAs.getValue(), dataBroker,
            ribPolicies, peerTracker, afiSafiType, pathSelectionStrategy);
        ret.init();
        return ret;
    }

    private synchronized void init() {
        final DOMDataTreeWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, this.locRibTableIID.node(ATTRIBUTES_NID).node(UPTODATE_NID),
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

        this.reg = this.dataBroker.registerDataTreeChangeListener(new DOMDataTreeIdentifier(
            LogicalDatastoreType.OPERATIONAL, ribIId.node(PEER_NID).node(PEER_NID).node(EFFRIBIN_NID).node(TABLES_NID)
                .node(locRibTableIID.getLastPathArgument())), this);
    }

    /**
     * Re-initialize this LocRibWriter with new transaction chain.
     *
     * @param newChain new transaction chain
     */
    synchronized void restart(final @NonNull DOMTransactionChain newChain) {
        requireNonNull(newChain);
        close();
        this.chain = newChain;
        init();
    }

    @Override
    public synchronized void close() {
        if (this.reg != null) {
            this.reg.close();
            this.reg = null;
        }
        if (this.chain != null) {
            this.chain.close();
            this.chain = null;
        }
    }

    private @NonNull RouteEntry<C, S> createEntry(final String routeId) {
        final RouteEntry<C, S> ret = this.pathSelectionMode.createRouteEntry();
        this.routeEntries.put(routeId, ret);
        this.totalPrefixesCounter.increment();
        LOG.trace("Created new entry for {}", routeId);
        return ret;
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
        if (this.chain == null) {
            LOG.trace("Chain closed, ignoring received data change {} to LocRib {}", changes, this);
            return;
        }
        LOG.trace("Received data change {} to LocRib {}", changes, this);
        final DOMDataTreeWriteTransaction tx = this.chain.newWriteOnlyTransaction();
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

    @SuppressWarnings("unchecked")
    private Map<RouteUpdateKey, RouteEntry<C, S>> update(final DOMDataTreeWriteOperations tx,
            final Collection<DataTreeCandidate> changes) {
        final Map<RouteUpdateKey, RouteEntry<C, S>> ret = new HashMap<>();
        for (final DataTreeCandidate tc : changes) {
            final DataTreeCandidateNode table = tc.getRootNode();
            final RouterId peerUuid = RouterId.forPeerId(IdentifierUtils.peerKeyToPeerId(tc.getRootPath()));

            /*
            Initialize Peer with routes under loc rib
             */
            if (!this.routeEntries.isEmpty() && table.getDataBefore().isEmpty()) {
                final org.opendaylight.protocol.bgp.rib.spi.Peer toPeer
                        = this.peerTracker.getPeer(peerUuid.getPeerId());
                if (toPeer != null && toPeer.supportsTable(this.entryDep.getLocalTablesKey())) {
                    LOG.debug("Peer {} table has been created, inserting existent routes", toPeer.getPeerId());
                    final List<ActualBestPathRoutes<C, S>> routesToStore = new ArrayList<>();
                    for (final Entry<String, RouteEntry<C, S>> entry : this.routeEntries.entrySet()) {
                        final List<ActualBestPathRoutes<C, S>> filteredRoute = entry.getValue()
                                .actualBestPaths(this.ribSupport, new RouteEntryInfoImpl(toPeer, entry.getKey()));
                        routesToStore.addAll(filteredRoute);
                    }
                    toPeer.initializeRibOut(this.entryDep, routesToStore);
                }
            }
            /*
            Process new routes from Peer
             */
            updateNodes(table, peerUuid, tx, ret);
        }
        return ret;
    }

    private void updateNodes(final DataTreeCandidateNode table, final RouterId peerUuid,
            final DOMDataTreeWriteOperations tx, final Map<RouteUpdateKey, RouteEntry<C, S>> routes) {
        table.getModifiedChild(ATTRIBUTES_NID).flatMap(DataTreeCandidateNode::getDataAfter).ifPresent(newAttValue -> {
            LOG.trace("Uptodate found for {}", newAttValue);
            tx.put(LogicalDatastoreType.OPERATIONAL, this.locRibTableIID.node(ATTRIBUTES_NID), newAttValue);
        });

        table.getModifiedChild(ROUTES_NID).ifPresent(modifiedRoutes -> {
            updateRoutesEntries(ribSupport.changedRoutes(modifiedRoutes), peerUuid, routes);
        });
    }

    private void updateRoutesEntries(final Collection<DataTreeCandidateNode> collection,
            final RouterId routerId, final Map<RouteUpdateKey, RouteEntry<C, S>> routes) {
        for (final DataTreeCandidateNode route : collection) {
            final PathArgument routeArg = route.getIdentifier();
            if (!(routeArg instanceof NodeIdentifierWithPredicates routeId)) {
                LOG.debug("Route {} already deleted", routeArg);
                return;
            }

            final String routeKey = ribSupport.extractRouteKey(routeId);
            final Uint32 pathId = ribSupport.extractPathId(routeId);

            RouteEntry<C, S> entry;
            switch (route.getModificationType()) {
                case DELETE:
                    entry = this.routeEntries.get(routeKey);
                    if (entry != null) {
                        this.totalPathsCounter.decrement();
                        if (entry.removeRoute(routerId, pathId)) {
                            this.routeEntries.remove(routeKey);
                            this.totalPrefixesCounter.decrement();
                            LOG.trace("Removed route from {}", routerId);
                        }
                    }
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    entry = this.routeEntries.get(routeKey);
                    if (entry == null) {
                        entry = createEntry(routeKey);
                    }

                    final NormalizedNode routeAfter = route.getDataAfter().get();
                    verify(routeAfter instanceof MapEntryNode, "Unexpected route %s", routeAfter);
                    entry.addRoute(routerId, pathId, (MapEntryNode) routeAfter);
                    this.totalPathsCounter.increment();
                    break;
                default:
                    throw new IllegalStateException("Unhandled route modification " + route);
            }

            final RouteUpdateKey routeUpdateKey = new RouteUpdateKey(routerId, routeKey);
            LOG.debug("Updated route {} entry {}", routeKey, entry);
            routes.put(routeUpdateKey, entry);
        }
    }

    private void walkThrough(final DOMDataTreeWriteOperations tx,
            final Set<Entry<RouteUpdateKey, RouteEntry<C, S>>> toUpdate) {
        final List<StaleBestPathRoute> staleRoutes = new ArrayList<>();
        final List<AdvertizedRoute<C, S>> newRoutes = new ArrayList<>();
        for (final Entry<RouteUpdateKey, RouteEntry<C, S>> e : toUpdate) {
            LOG.trace("Walking through {}", e);
            final RouteEntry<C, S> entry = e.getValue();

            if (!entry.selectBest(this.ribSupport, this.ourAs)) {
                LOG.trace("Best path has not changed, continuing");
                continue;
            }

            entry.removeStalePaths(this.ribSupport, e.getKey().getRouteId()).ifPresent(staleRoutes::add);
            newRoutes.addAll(entry.newBestPaths(this.ribSupport, e.getKey().getRouteId()));
        }
        updateLocRib(newRoutes, staleRoutes, tx);
        this.peerTracker.getNonInternalPeers().parallelStream()
                .filter(toPeer -> toPeer.supportsTable(this.entryDep.getLocalTablesKey()))
                .forEach(toPeer -> toPeer.refreshRibOut(this.entryDep, staleRoutes, newRoutes));
    }

    private void updateLocRib(final List<AdvertizedRoute<C, S>> newRoutes, final List<StaleBestPathRoute> staleRoutes,
            final DOMDataTreeWriteOperations tx) {
        final YangInstanceIdentifier locRibTarget = this.entryDep.getLocRibTableTarget();

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
            final YangInstanceIdentifier locRibRouteTarget = this.ribSupport.createRouteIdentifier(locRibTarget, iid);
            LOG.debug("Write LocRib route {}", locRibRouteTarget);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Write route to LocRib {}", NormalizedNodes.toStringTree(route));
            }
            tx.put(LogicalDatastoreType.OPERATIONAL, locRibRouteTarget, route);
        }
    }

    @Override
    public long getPrefixesCount() {
        return this.totalPrefixesCounter.longValue();
    }

    @Override
    public long getPathsCount() {
        return this.totalPathsCounter.longValue();
    }

    TablesKey getTableKey() {
        return this.ribSupport.getTablesKey();
    }

    @Override
    public synchronized void refreshTable(final TablesKey tk, final PeerId peerId) {
        final org.opendaylight.protocol.bgp.rib.spi.Peer toPeer = this.peerTracker.getPeer(peerId);
        if (toPeer != null && toPeer.supportsTable(this.entryDep.getLocalTablesKey())) {
            LOG.debug("Peer {} table has been created, inserting existent routes", toPeer.getPeerId());
            final List<ActualBestPathRoutes<C, S>> routesToStore = new ArrayList<>();
            for (final Entry<String, RouteEntry<C, S>> entry : this.routeEntries.entrySet()) {
                final List<ActualBestPathRoutes<C, S>> filteredRoute = entry.getValue()
                        .actualBestPaths(this.ribSupport, new RouteEntryInfoImpl(toPeer, entry.getKey()));
                routesToStore.addAll(filteredRoute);
            }
            toPeer.reEvaluateAdvertizement(this.entryDep, routesToStore);
        }
    }
}
