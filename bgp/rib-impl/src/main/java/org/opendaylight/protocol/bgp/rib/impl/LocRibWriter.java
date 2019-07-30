/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNormalizedNodes.ATTRIBUTES_UPTODATE_TRUE;

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
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is NOT thread-safe
final class LocRibWriter<C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
    R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
    implements AutoCloseable, RibOutRefresh, TotalPrefixesCounter, TotalPathsCounter,
    ClusteredDOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(LocRibWriter.class);

    private final Map<String, RouteEntry<C, S, R, I>> routeEntries = new HashMap<>();
    private final long ourAs;
    private final RIBSupport<C, S, R, I> ribSupport;
    private final PathSelectionMode pathSelectionMode;
    private final LongAdder totalPathsCounter = new LongAdder();
    private final LongAdder totalPrefixesCounter = new LongAdder();
    private final RouteEntryDependenciesContainerImpl entryDep;
    private final BGPPeerTracker peerTracker;
    private final YangInstanceIdentifier yangRibId;
    private final TablesKey tk;
    private final YangInstanceIdentifier locRibTarget;
    private final DOMDataTreeChangeService service;

    private DOMTransactionChain chain;
    @GuardedBy("this")
    private ListenerRegistration<LocRibWriter> reg;

    // FIXME: make ribsupport provide afisafiType

    private LocRibWriter(
            final RIBSupport<C, S, R, I> ribSupport,
            final DOMTransactionChain chain,
            final YangInstanceIdentifier yangRibId,
            final Long ourAs,
            final DOMDataTreeChangeService service,
            final BGPRibRoutingPolicy ribPolicies,
            final BGPPeerTracker peerTracker,
            final Class<? extends AfiSafiType> afiSafiType,
            final PathSelectionMode pathSelectionMode) {
        this.chain = requireNonNull(chain);
        this.yangRibId = requireNonNull(yangRibId);
        this.tk = requireNonNull(ribSupport.getTablesKey());
        this.locRibTarget = YangInstanceIdentifier.create(yangRibId.node(LocRib.QNAME).node(Tables.QNAME)
            .node(RibSupportUtils.toYangTablesKey(ribSupport.getTablesKey())).getPathArguments());
        this.ourAs = ourAs;
        this.ribSupport = requireNonNull(ribSupport);
        this.service = requireNonNull(service);
        this.peerTracker = peerTracker;
        this.pathSelectionMode = pathSelectionMode;

        this.entryDep = new RouteEntryDependenciesContainerImpl(this.ribSupport, this.peerTracker, ribPolicies,
                afiSafiType, this.locRibTarget);
        init();
    }

    public static <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
        LocRibWriter<C, S, R, I> create(
            final @NonNull RIBSupport<C, S, R, I> ribSupport,
            final @NonNull DOMTransactionChain chain,
            final @NonNull Class<? extends AfiSafiType> afiSafiType,
            final @NonNull YangInstanceIdentifier yangRibId,
            final @NonNull AsNumber ourAs,
            final @NonNull DOMDataTreeChangeService service,
            final BGPRibRoutingPolicy ribPolicies,
            final @NonNull BGPPeerTracker peerTracker,
            final @NonNull PathSelectionMode pathSelectionStrategy) {
        return new LocRibWriter<>(ribSupport, chain, yangRibId, ourAs.getValue(), service,
                ribPolicies, peerTracker, afiSafiType, pathSelectionStrategy);
    }

    private synchronized void init() {
        final DOMDataTreeWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL, this.locRibTarget.node(Routes.QNAME), this.ribSupport.emptyRoutes());
        tx.merge(LogicalDatastoreType.OPERATIONAL, this.locRibTarget.node(Attributes.QNAME)
            .node(ATTRIBUTES_UPTODATE_TRUE.getNodeType()), ATTRIBUTES_UPTODATE_TRUE);
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

        final YangInstanceIdentifier tableId = this.yangRibId.node(PEER_NID).node(PEER_NID)
            .node(EFFRIBIN_NID).node(TABLES_NID)
            .node(RibSupportUtils.toYangTablesKey(this.ribSupport.getTablesKey()));
        final DOMDataTreeIdentifier wildcard = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, tableId);
        this.reg = this.service.registerDataTreeChangeListener(wildcard, this);
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

    private @NonNull RouteEntry<C, S, R, I> createEntry(final String routeId) {
        final RouteEntry<C, S, R, I> ret = this.pathSelectionMode.createRouteEntry();
        this.routeEntries.put(routeId, ret);
        this.totalPrefixesCounter.increment();
        LOG.trace("Created new entry for {}", routeId);
        return ret;
    }

    /**
     * We use two-stage processing here in hopes that we avoid duplicate
     * calculations when multiple peers have changed a particular entry.
     *
     * @param changes on supported table
     */
    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    public synchronized void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        if (this.chain == null) {
            LOG.trace("Chain closed, ignoring received data change {} to LocRib {}", changes, this);
            return;
        }
        LOG.trace("Received data change {} to LocRib {}", changes, this);
        final DOMDataTreeWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        try {
            final Map<RouteUpdateKey, RouteEntry<C, S, R, I>> toUpdate = update(tx, changes);

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


    private Map<RouteUpdateKey, RouteEntry<C, S, R, I>> update(
        final DOMDataTreeWriteOperations tx,
        final Collection<DataTreeCandidate> changes) {
        final Map<RouteUpdateKey, RouteEntry<C, S, R, I>> ret = new HashMap<>();

        changes.forEach(tc -> {
            final DataTreeCandidateNode table = tc.getRootNode();
            final YangInstanceIdentifier tablePath = tc.getRootPath();
            final RouterId peerId = RouterId.forPeerId(IdentifierUtils.peerKeyToPeerId(tablePath));
            initializeTableWithExistentRoutes(table, peerId);
            updateNodes(tablePath, table, peerId, tx, ret);
        });
        return ret;
    }

    /*
      Initialize Peer with routes under loc rib
    */
    private void initializeTableWithExistentRoutes(
        final DataTreeCandidateNode table, final RouterId peerId) {
        if (!this.routeEntries.isEmpty() && table.getDataBefore() == null) {
            final org.opendaylight.protocol.bgp.rib.spi.Peer toPeer = this.peerTracker.getPeer(peerId.getPeerId());
            if (toPeer != null && toPeer.supportsTable(this.ribSupport.getTablesKey())) {
                LOG.debug("Peer {} table has been created, inserting existent routes", toPeer.getPeerId());
                final List<ActualBestPathRoutes<C, S, R, I>> routesToStore = new ArrayList<>();
                for (final Entry<String, RouteEntry<C, S, R, I>> entry : this.routeEntries.entrySet()) {
                    final List<ActualBestPathRoutes<C, S, R, I>> filteredRoute = entry.getValue()
                        .actualBestPaths(this.ribSupport, new RouteEntryInfoImpl(toPeer, entry.getKey()));
                    routesToStore.addAll(filteredRoute);
                }
                toPeer.initializeRibOut(this.entryDep, routesToStore);
            }
        }
    }

    private void updateNodes(
        final YangInstanceIdentifier tablePath, final DataTreeCandidateNode table,
        final RouterId peerId,
        final DOMDataTreeWriteOperations tx,
        final Map<RouteUpdateKey, RouteEntry<C, S, R, I>> routes) {
        for (final DataTreeCandidateNode child : table.getChildNodes()) {
            LOG.debug("Modification type {}", child.getModificationType());
            if (Attributes.QNAME.equals(child.getIdentifier().getNodeType())) {
                if (child.getDataAfter().isPresent()) {
                    // putting uptodate attribute in
                    LOG.trace("Uptodate found for {}", child.getDataAfter());
                    tx.put(LogicalDatastoreType.OPERATIONAL, this.locRibTarget.node(child.getIdentifier()),
                        child.getDataAfter().get());
                }
                continue;
            }
            updateRoutesEntries(tablePath, child, peerId, routes);
        }
    }

    private void updateRoutesEntries(final YangInstanceIdentifier node, final DataTreeCandidateNode routesPath,
        final RouterId routerId,
        final Map<RouteUpdateKey, RouteEntry<C, S, R, I>> routes) {

        final Collection<DataTreeCandidateNode> modifiedRoutes = this.ribSupport.changedRoutes(routesPath);
        for (final DataTreeCandidateNode route : modifiedRoutes) {
            final NodeIdentifierWithPredicates routeKeyN = (NodeIdentifierWithPredicates) route.getIdentifier();
            final String routeKey = this.ribSupport.extractRouteKey(routeKeyN);
            final Long pathId = this.ribSupport.extractPathId(routeKeyN);
            RouteEntry entry = this.routeEntries.get(routeKey);

            final java.util.Optional<NormalizedNode<?, ?>> maybeData = route.getDataAfter();
            if (maybeData.isPresent()) {
                final YangInstanceIdentifier routePath = this.ribSupport.createRouteIdentifier(node, route.getIdentifier());
                final R newRoute = this.ribSupport.fromNormalizedNode(routePath , maybeData.get());
                entry = this.routeEntries.get(routeKey);
                if (entry == null) {
                    entry = createEntry(routeKey);
                }

                entry.addRoute(routerId, pathId, newRoute);
                this.totalPathsCounter.increment();
            } else if (entry != null) {
                if(entry.removeRoute(routerId, pathId)) {
                    this.routeEntries.remove(routeKey);
                    this.totalPrefixesCounter.decrement();
                    LOG.trace("Removed route from {}", routerId);
                }
                this.totalPathsCounter.decrement();
            }
            final RouteUpdateKey routeUpdateKey = new RouteUpdateKey(routerId, routeKey);
            LOG.debug("Updated route {} entry {}", routeKey, entry);
            routes.put(routeUpdateKey, entry);
        }
    }

    private void walkThrough(final DOMDataTreeWriteOperations tx,
            final Set<Entry<RouteUpdateKey, RouteEntry<C, S, R, I>>> toUpdate) {
        final List<StaleBestPathRoute> staleRoutes = new ArrayList<>();
        final List<AdvertizedRoute<C, S, R, I>> newRoutes = new ArrayList<>();
        for (final Entry<RouteUpdateKey, RouteEntry<C, S, R, I>> e : toUpdate) {
            LOG.trace("Walking through {}", e);
            final RouteEntry<C, S, R, I> entry = e.getValue();

            if (!entry.selectBest(this.ourAs)) {
                LOG.trace("Best path has not changed, continuing");
                continue;
            }

            entry.removeStalePaths(this.ribSupport, e.getKey().getRouteId()).ifPresent(staleRoutes::add);
            newRoutes.addAll(entry.newBestPaths(this.ribSupport, e.getKey().getRouteId()));
        }
        updateLocRib(newRoutes, staleRoutes, tx);
        this.peerTracker.getNonInternalPeers().parallelStream().forEach(
            toPeer -> toPeer.refreshRibOut(this.entryDep, staleRoutes, newRoutes));
    }

    private void updateLocRib(final List<AdvertizedRoute<C, S, R, I>> newRoutes,
            final List<StaleBestPathRoute> staleRoutes,
            final DOMDataTreeWriteOperations tx) {
        final YangInstanceIdentifier locRibTarget = this.entryDep.getLocRibTableTarget();

        for (final StaleBestPathRoute staleContainer : staleRoutes) {
            for (final NodeIdentifierWithPredicates routeId : staleContainer.getStaleRouteKeyIdentifiers()) {
                final YangInstanceIdentifier routeTarget = this.ribSupport.createRouteIdentifier(locRibTarget, routeId);
                LOG.debug("Delete route from LocRib {}", routeTarget);
                tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
            }
        }

        /**
         * (urn:opendaylight:params:xml:ns:yang:bgp-inet?revision=2018-03-29)ipv4-route[{
         * (urn:opendaylight:params:xml:ns:yang:bgp-inet?revision=2018-03-29)path-id=0,
         * (urn:opendaylight:params:xml:ns:yang:bgp-inet?revision=2018-03-29)route-key=1.1.1.1/32}]
         */
        for (final AdvertizedRoute<C,S,R,I> advRoute : newRoutes) {
            final R route = advRoute.getRoute();
            final I iid = advRoute.getAddPathRouteKeyIdentifier();
            //final InstanceIdentifier<R> locRibRouteTarget this.ribSupport.createRouteIdentifier(locRibTarget, iid);
            LOG.debug("Write route to LocRib {}", route);
          //FIXME  tx.put(LogicalDatastoreType.OPERATIONAL, locRibRouteTarget, route);
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
        if (toPeer != null && toPeer.supportsTable(this.ribSupport.getTablesKey())) {
            LOG.debug("Peer {} table has been created, inserting existent routes", toPeer.getPeerId());
            final List<ActualBestPathRoutes<C, S, R, I>> routesToStore = new ArrayList<>();
            for (final Entry<String, RouteEntry<C, S, R, I>> entry : this.routeEntries.entrySet()) {
                final List<ActualBestPathRoutes<C, S, R, I>> filteredRoute = entry.getValue()
                        .actualBestPaths(this.ribSupport, new RouteEntryInfoImpl(toPeer, entry.getKey()));
                routesToStore.addAll(filteredRoute);
            }
            toPeer.reEvaluateAdvertizement(this.entryDep, routesToStore);
        }
    }
}
