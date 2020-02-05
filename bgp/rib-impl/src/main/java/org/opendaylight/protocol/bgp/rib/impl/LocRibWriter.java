/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

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
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RibOutRefresh;
import org.opendaylight.protocol.bgp.rib.impl.state.rib.TotalPathsCounter;
import org.opendaylight.protocol.bgp.rib.impl.state.rib.TotalPrefixesCounter;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is NOT thread-safe
final class LocRibWriter<C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
        implements AutoCloseable, RibOutRefresh, TotalPrefixesCounter, TotalPathsCounter,
        ClusteredDataTreeChangeListener<Tables> {

    private static final Logger LOG = LoggerFactory.getLogger(LocRibWriter.class);

    private final Map<String, RouteEntry<C, S, R, I>> routeEntries = new HashMap<>();
    private final long ourAs;
    private final RIBSupport<C, S, R, I> ribSupport;
    private final DataBroker dataBroker;
    private final PathSelectionMode pathSelectionMode;
    private final LongAdder totalPathsCounter = new LongAdder();
    private final LongAdder totalPrefixesCounter = new LongAdder();
    private final RouteEntryDependenciesContainerImpl entryDep;
    private final BGPPeerTracker peerTracker;
    private final KeyedInstanceIdentifier<Rib, RibKey> ribIId;
    private final KeyedInstanceIdentifier<Tables, TablesKey> locRibTableIID;

    private TransactionChain chain;
    @GuardedBy("this")
    private ListenerRegistration<?> reg;

    private LocRibWriter(final RIBSupport<C, S, R, I> ribSupport,
            final TransactionChain chain,
            final KeyedInstanceIdentifier<Rib, RibKey> ribIId,
            final Uint32 ourAs,
            final DataBroker dataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            final BGPPeerTracker peerTracker,
            final Class<? extends AfiSafiType> afiSafiType,
            final PathSelectionMode pathSelectionMode) {
        this.chain = requireNonNull(chain);
        this.ribIId = requireNonNull(ribIId);
        this.ribSupport = requireNonNull(ribSupport);
        this.locRibTableIID = ribIId.child(LocRib.class).child(Tables.class, ribSupport.getTablesKey());
        this.ourAs = ourAs.toJava();
        this.dataBroker = requireNonNull(dataBroker);
        this.peerTracker = peerTracker;
        this.pathSelectionMode = pathSelectionMode;

        this.entryDep = new RouteEntryDependenciesContainerImpl(this.ribSupport, this.peerTracker, ribPolicies,
                afiSafiType, this.locRibTableIID);
        init();
    }

    public static <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
                R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
                LocRibWriter<C, S, R, I> create(
            final @NonNull RIBSupport<C, S, R, I> ribSupport,
            final @NonNull Class<? extends AfiSafiType> afiSafiType,
            final @NonNull TransactionChain chain,
            final @NonNull KeyedInstanceIdentifier<Rib, RibKey> ribIId,
            final @NonNull AsNumber ourAs,
            final @NonNull DataBroker dataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            final @NonNull BGPPeerTracker peerTracker,
            final @NonNull PathSelectionMode pathSelectionStrategy) {
        return new LocRibWriter<>(ribSupport, chain, ribIId, ourAs.getValue(), dataBroker, ribPolicies,
                peerTracker, afiSafiType, pathSelectionStrategy);
    }

    private synchronized void init() {
        final WriteTransaction tx = this.chain.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL,
                this.locRibTableIID.builder().child(Attributes.class).build(),
                new AttributesBuilder().setUptodate(true).build());
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

        final InstanceIdentifier<Tables> tableId = this.ribIId.builder().child(Peer.class)
                .child(EffectiveRibIn.class).child(Tables.class, getTableKey()).build();
        this.reg = this.dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, tableId), this);
    }

    /**
     * Re-initialize this LocRibWriter with new transaction chain.
     *
     * @param newChain new transaction chain
     */
    synchronized void restart(final @NonNull TransactionChain newChain) {
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
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Tables>> changes) {
        if (this.chain == null) {
            LOG.trace("Chain closed, ignoring received data change {} to LocRib {}", changes, this);
            return;
        }
        LOG.trace("Received data change {} to LocRib {}", changes, this);
        final WriteTransaction tx = this.chain.newWriteOnlyTransaction();
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

    @SuppressWarnings("unchecked")
    private Map<RouteUpdateKey, RouteEntry<C, S, R, I>> update(final WriteTransaction tx,
            final Collection<DataTreeModification<Tables>> changes) {
        final Map<RouteUpdateKey, RouteEntry<C, S, R, I>> ret = new HashMap<>();
        for (final DataTreeModification<Tables> tc : changes) {
            final DataObjectModification<Tables> table = tc.getRootNode();
            final DataTreeIdentifier<Tables> rootPath = tc.getRootPath();
            final KeyedInstanceIdentifier<Peer, PeerKey> peerKIid = (KeyedInstanceIdentifier<Peer, PeerKey>)
                    rootPath.getRootIdentifier().firstIdentifierOf(Peer.class);
            final RouterId peerUuid = RouterId.forPeerId(peerKIid.getKey().getPeerId());
            /*
            Initialize Peer with routes under loc rib
             */
            if (!this.routeEntries.isEmpty() && table.getDataBefore() == null) {
                final org.opendaylight.protocol.bgp.rib.spi.Peer toPeer
                        = this.peerTracker.getPeer(peerUuid.getPeerId());
                if (toPeer != null && toPeer.supportsTable(this.entryDep.getLocalTablesKey())) {
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
            /*
            Process new routes from Peer
             */
            updateNodes(table, peerUuid, tx, ret);
        }
        return ret;
    }

    private void updateNodes(final DataObjectModification<Tables> table, final RouterId peerUuid,
            final WriteTransaction tx, final Map<RouteUpdateKey, RouteEntry<C, S, R, I>> routes) {
        final DataObjectModification<Attributes> attUpdate = table.getModifiedChildContainer(Attributes.class);
        if (attUpdate != null) {
            final Attributes newAttValue = attUpdate.getDataAfter();
            if (newAttValue != null) {
                LOG.trace("Uptodate found for {}", newAttValue);
                tx.put(LogicalDatastoreType.OPERATIONAL, this.locRibTableIID.child(Attributes.class), newAttValue);
            }
        }

        final DataObjectModification<S> routesChangesContainer
                = table.getModifiedChildContainer(ribSupport.routesCaseClass(), ribSupport.routesContainerClass());
        if (routesChangesContainer != null) {
            updateRoutesEntries(routesChangesContainer.getModifiedChildren(), peerUuid, routes);
        }
    }

    private void updateRoutesEntries(final Collection<? extends DataObjectModification<?>> collection,
            final RouterId routerId, final Map<RouteUpdateKey, RouteEntry<C, S, R, I>> routes) {
        for (final DataObjectModification<? extends DataObject> route : collection) {
            if (!(route.getIdentifier() instanceof InstanceIdentifier.IdentifiableItem)) {
                LOG.debug("Route {} already deleted", route.getIdentifier());
                return;
            }
            final I routeListKey = (I) ((InstanceIdentifier.IdentifiableItem) route.getIdentifier()).getKey();
            final String routeKey = ribSupport.extractRouteKey(routeListKey);
            final PathId pathId = ribSupport.extractPathId(routeListKey);

            RouteEntry<C, S, R, I> entry;
            switch (route.getModificationType()) {
                case DELETE:
                    entry = this.routeEntries.get(routeKey);
                    if (entry != null) {
                        this.totalPathsCounter.decrement();
                        if (entry.removeRoute(routerId, pathId.getValue())) {
                            this.routeEntries.remove(routeKey);
                            this.totalPrefixesCounter.decrement();
                            LOG.trace("Removed route from {}", routerId);
                        }
                    }
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    final R newRoute = (R) route.getDataAfter();
                    entry = this.routeEntries.get(routeKey);
                    if (entry == null) {
                        entry = createEntry(routeKey);
                    }

                    entry.addRoute(routerId, pathId.getValue(), newRoute);
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

    private void walkThrough(final WriteTransaction tx,
            final Set<Entry<RouteUpdateKey, RouteEntry<C, S, R, I>>> toUpdate) {
        final List<StaleBestPathRoute<C, S, R, I>> staleRoutes = new ArrayList<>();
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
            final List<StaleBestPathRoute<C, S, R, I>> staleRoutes,
            final WriteTransaction tx) {
        final KeyedInstanceIdentifier<Tables, TablesKey> locRibTarget = this.entryDep.getLocRibTableTarget();

        for (final StaleBestPathRoute<C, S, R, I> staleContainer : staleRoutes) {
            for (final I routeId : staleContainer.getStaleRouteKeyIdentifiers()) {
                final InstanceIdentifier<R> routeTarget = ribSupport.createRouteIdentifier(locRibTarget, routeId);
                LOG.debug("Delete route from LocRib {}", routeTarget);
                tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
            }
        }

        for (final AdvertizedRoute<C,S,R,I> advRoute : newRoutes) {
            final R route = advRoute.getRoute();
            final I iid = advRoute.getAddPathRouteKeyIdentifier();
            final InstanceIdentifier<R> locRibRouteTarget
                    = this.ribSupport.createRouteIdentifier(locRibTarget, iid);
            LOG.debug("Write route to LocRib {}", route);
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
