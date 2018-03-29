/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.primitives.UnsignedInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.impl.state.rib.TotalPathsCounter;
import org.opendaylight.protocol.bgp.rib.impl.state.rib.TotalPrefixesCounter;
import org.opendaylight.protocol.bgp.rib.spi.AdditionalPathUtil;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathIdGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.AttributesBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class LocRibWriter implements AutoCloseable, TotalPrefixesCounter, TotalPathsCounter,
        ClusteredDataTreeChangeListener<Tables> {

    private static final Logger LOG = LoggerFactory.getLogger(LocRibWriter.class);

    private final Map<Identifier, RouteEntry> routeEntries = new HashMap<>();
    private final Long ourAs;
    private final RIBSupport ribSupport;
    private final DataBroker dataBroker;
    private final PathSelectionMode pathSelectionMode;
    private final LongAdder totalPathsCounter = new LongAdder();
    private final LongAdder totalPrefixesCounter = new LongAdder();
    private final RouteEntryDependenciesContainerImpl entryDep;
    private final BGPPeerTracker peerTracker;
    private final KeyedInstanceIdentifier<Rib, RibKey> ribIId;
    private final TablesKey tk;
    private final KeyedInstanceIdentifier<Tables, TablesKey> locRibTableIID;
    private BindingTransactionChain chain;
    @GuardedBy("this")
    private ListenerRegistration<LocRibWriter> reg;

    private LocRibWriter(final RIBSupport ribSupport,
            final BindingTransactionChain chain,
            final KeyedInstanceIdentifier<Rib, RibKey> ribIId,
            final Long ourAs,
            final DataBroker dataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            final BGPPeerTracker peerTracker,
            final TablesKey tablesKey,
            final PathSelectionMode pathSelectionMode) {
        this.chain = requireNonNull(chain);
        this.ribIId = requireNonNull(ribIId);
        this.tk = requireNonNull(tablesKey);
        this.locRibTableIID = ribIId.child(LocRib.class).child(Tables.class, this.tk);
        this.ourAs = requireNonNull(ourAs);
        this.dataBroker = requireNonNull(dataBroker);
        this.ribSupport = requireNonNull(ribSupport);
        this.peerTracker = peerTracker;
        this.pathSelectionMode = pathSelectionMode;

        this.entryDep = new RouteEntryDependenciesContainerImpl(this.ribSupport, ribPolicies,
                tablesKey, this.locRibTableIID);
        init();
    }

    public static LocRibWriter create(@Nonnull final RIBSupport ribSupport,
            @Nonnull final TablesKey tablesKey,
            @Nonnull final BindingTransactionChain chain,
            @Nonnull final KeyedInstanceIdentifier<Rib, RibKey> ribIId,
            @Nonnull final AsNumber ourAs,
            @Nonnull final DataBroker dataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            @Nonnull final BGPPeerTracker peerTracker,
            @Nonnull final PathSelectionMode pathSelectionStrategy) {
        return new LocRibWriter(ribSupport, chain, ribIId, ourAs.getValue(), dataBroker, ribPolicies,
                peerTracker, tablesKey, pathSelectionStrategy);
    }

    @SuppressWarnings("unchecked")
    private synchronized void init() {
        final WriteTransaction tx = this.chain.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL,
                this.locRibTableIID.builder().child(Attributes.class).build(),
                new AttributesBuilder().setUptodate(true).build());
        tx.submit();

        final InstanceIdentifier<Tables> tableId = this.ribIId.builder().child(Peer.class)
                .child(EffectiveRibIn.class).child(Tables.class, this.tk).build();
        this.reg = this.dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, tableId), this);
    }

    /**
     * Re-initialize this LocRibWriter with new transaction chain.
     *
     * @param newChain new transaction chain
     */
    synchronized void restart(@Nonnull final BindingTransactionChain newChain) {
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
        this.chain.close();
    }

    @Nonnull
    private RouteEntry createEntry(final Identifier routeId) {
        final RouteEntry ret = this.pathSelectionMode.createRouteEntry(this.ribSupport.isComplexRoute());
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
    public void onDataTreeChanged(final Collection<DataTreeModification<Tables>> changes) {
        LOG.trace("Received data change {} to LocRib {}", changes, this);

        final WriteTransaction tx = this.chain.newWriteOnlyTransaction();
        try {
            final Map<RouteUpdateKey, RouteEntry> toUpdate = update(tx, changes);

            if (!toUpdate.isEmpty()) {
                walkThrough(tx, toUpdate.entrySet());
            }
        } catch (final Exception e) {
            LOG.error("Failed to completely propagate updates {}, state is undefined", changes, e);
        } finally {
            tx.submit();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<RouteUpdateKey, RouteEntry> update(final WriteTransaction tx,
            final Collection<DataTreeModification<Tables>> changes) {
        final Map<RouteUpdateKey, RouteEntry> ret = new HashMap<>();
        for (final DataTreeModification<Tables> tc : changes) {
            final DataObjectModification<Tables> table = tc.getRootNode();
            final DataTreeIdentifier<Tables> rootPath = tc.getRootPath();
            final KeyedInstanceIdentifier<Peer, PeerKey> peerKIid = (KeyedInstanceIdentifier<Peer, PeerKey>)
                    rootPath.getRootIdentifier().firstIdentifierOf(Peer.class);
            final UnsignedInteger peerUuid = RouterIds.routerIdForPeerId(peerKIid.getKey().getPeerId());
            /*
            Initialize Peer with routes under loc rib
             */
            if (!this.routeEntries.isEmpty() && table.getDataBefore() == null) {
                final org.opendaylight.protocol.bgp.rib.spi.Peer peer
                        = this.peerTracker.getPeer(peerKIid.getKey().getPeerId());
                if (peer != null && peer.supportsTable(this.entryDep.getLocalTablesKey())) {
                    LOG.debug("Peer {} table has been created, inserting existent routes", peer.getPeerId());
                    this.routeEntries.forEach((key, value) -> value.initializeBestPaths(this.entryDep,
                            new RouteEntryInfoImpl(peer, key), tx));
                }
            }
            /*
            Process new routes from Peer
             */
            updateNodes(table, peerUuid, tx, ret);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private void updateNodes(
            final DataObjectModification<Tables> table,
            final UnsignedInteger peerUuid,
            final WriteTransaction tx,
            final Map<RouteUpdateKey, RouteEntry> routes
    ) {

        final DataObjectModification<Attributes> attUpdate = table.getModifiedChildContainer(Attributes.class);

        if (attUpdate != null && attUpdate.getDataAfter() != null) {
            final Attributes newAttValue = attUpdate.getDataAfter();
            LOG.trace("Uptodate found for {}", newAttValue);
            tx.put(LogicalDatastoreType.OPERATIONAL, this.locRibTableIID.child(Attributes.class), newAttValue);
        }

        final DataObjectModification routesChangesContainer =
                table.getModifiedChildContainer(this.ribSupport.routesContainerClass());
        if (routesChangesContainer == null) {
            return;
        }
        updateRoutesEntries(routesChangesContainer.getModifiedChildren(), peerUuid, routes);
    }

    @SuppressWarnings("unchecked")
    private void updateRoutesEntries(
            final Collection<DataObjectModification<? extends DataObject>> routeChanges,
            final UnsignedInteger routerId,
            final Map<RouteUpdateKey, RouteEntry> routes
    ) {
        for (final DataObjectModification<? extends DataObject> route : routeChanges) {
            final Identifier routeKey = ((InstanceIdentifier.IdentifiableItem) route.getIdentifier()).getKey();
            RouteEntry entry = this.routeEntries.get(routeKey);
            final Route newRoute = (Route) route.getDataAfter();
            final Route oldRoute = (Route) route.getDataBefore();
            if (newRoute != null) {
                if (entry == null) {
                    entry = createEntry(routeKey);
                }
                final long pathId = AdditionalPathUtil.extractPathId((PathIdGrouping) newRoute);
                entry.addRoute(routerId, pathId, newRoute);
                this.totalPathsCounter.increment();
            } else if (oldRoute != null && entry != null) {
                this.totalPathsCounter.decrement();
                final long pathId = AdditionalPathUtil.extractPathId((PathIdGrouping) oldRoute);
                if (entry.removeRoute(routerId, pathId)) {
                    this.routeEntries.remove(routeKey);
                    this.totalPrefixesCounter.decrement();
                    LOG.trace("Removed route from {}", routerId);
                }
            }
            final RouteUpdateKey routeUpdateKey = new RouteUpdateKey(routerId, routeKey);
            LOG.debug("Updated route {} entry {}", routeKey, entry);
            routes.put(routeUpdateKey, entry);
        }
    }

    private void walkThrough(final WriteTransaction tx,
            final Set<Map.Entry<RouteUpdateKey, RouteEntry>> toUpdate) {
        for (final Map.Entry<RouteUpdateKey, RouteEntry> e : toUpdate) {
            LOG.trace("Walking through {}", e);
            final RouteEntry entry = e.getValue();

            if (!entry.selectBest(this.ourAs)) {
                LOG.trace("Best path has not changed, continuing");
                continue;
            }
            entry.updateBestPaths(entryDep, e.getKey().getRouteId(), tx);
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

    public TablesKey getTableKey() {
        return this.tk;
    }
}
