/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesInstalledCounters;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesReceivedCounters;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the BGP import policy. Listens on peer's Adj-RIB-In, inspects all inbound
 * routes in the context of the advertising peer's role and applies the inbound policy.
 * <p>
 * Inbound policy is applied as follows:
 * <p>
 * 1) if the peer is an eBGP peer, perform attribute replacement and filtering
 * 2) check if a route is admissible based on attributes attached to it, as well as the
 * advertising peer's role
 * 3) output admitting routes with edited attributes into /bgp-rib/rib/peer/effective-rib-in/tables/routes
 */
@NotThreadSafe
final class EffectiveRibInWriter implements PrefixesReceivedCounters, PrefixesInstalledCounters, AutoCloseable {
    static final NodeIdentifier TABLE_ROUTES = new NodeIdentifier(Routes.QNAME);
    private static final Logger LOG = LoggerFactory.getLogger(EffectiveRibInWriter.class);
    private final AdjInTracker adjInTracker;

    private EffectiveRibInWriter(final BGPRouteEntryImportParameters peer, final RIB rib,
            final BindingTransactionChain chain,
            final KeyedInstanceIdentifier<Peer, PeerKey> peerIId, final Set<TablesKey> tables) {
        this.adjInTracker = new AdjInTracker(peer, rib, chain, peerIId, tables);
    }

    static EffectiveRibInWriter create(final BGPRouteEntryImportParameters peer, @Nonnull final RIB rib,
            @Nonnull final BindingTransactionChain chain,
            @Nonnull final KeyedInstanceIdentifier<Peer, PeerKey> peerIId,
            @Nonnull final Set<TablesKey> tables) {
        return new EffectiveRibInWriter(peer, rib, chain, peerIId, tables);
    }

    @Override
    public synchronized void close() {
        this.adjInTracker.close();
    }

    @Override
    public long getPrefixedReceivedCount(final TablesKey tablesKey) {
        return this.adjInTracker.getPrefixedReceivedCount(tablesKey);
    }

    @Override
    public Set<TablesKey> getTableKeys() {
        return this.adjInTracker.getTableKeys();
    }

    @Override
    public boolean isSupported(final TablesKey tablesKey) {
        return this.adjInTracker.isSupported(tablesKey);
    }

    @Override
    public long getPrefixedInstalledCount(@Nonnull final TablesKey tablesKey) {
        return this.adjInTracker.getPrefixedInstalledCount(tablesKey);
    }

    @Override
    public long getTotalPrefixesInstalled() {
        return this.adjInTracker.getTotalPrefixesInstalled();
    }

    private static final class AdjInTracker implements PrefixesReceivedCounters, PrefixesInstalledCounters,
            AutoCloseable, ClusteredDataTreeChangeListener<Tables> {
        private final RIBSupportContextRegistry registry;
        private final KeyedInstanceIdentifier<Peer, PeerKey> peerIId;
        private final InstanceIdentifier<EffectiveRibIn> effRibTables;
        private final ListenerRegistration<?> reg;
        private final BindingTransactionChain chain;
        private final Map<TablesKey, LongAdder> prefixesReceived;
        private final Map<TablesKey, LongAdder> prefixesInstalled;
        private final BGPRibRoutingPolicy ribPolicies;
        private final BGPRouteEntryImportParameters peerImportParameters;

        @SuppressWarnings("unchecked")
        AdjInTracker(final BGPRouteEntryImportParameters peer, final RIB rib,
                final BindingTransactionChain chain,
                final KeyedInstanceIdentifier<Peer, PeerKey> peerIId,
                @Nonnull final Set<TablesKey> tables) {
            this.registry = requireNonNull(rib.getRibSupportContext());
            this.chain = requireNonNull(chain);
            this.peerIId = requireNonNull(peerIId);
            this.effRibTables = this.peerIId.child(EffectiveRibIn.class);
            this.prefixesInstalled = buildPrefixesTables(tables);
            this.prefixesReceived = buildPrefixesTables(tables);
            this.ribPolicies = requireNonNull(rib.getRibPolicies());
            this.peerImportParameters = peer;
            final DataTreeIdentifier treeId = new DataTreeIdentifier(LogicalDatastoreType.OPERATIONAL,
                    this.peerIId.child(AdjRibIn.class).child(Tables.class));
            LOG.debug("Registered Effective RIB on {}", this.peerIId);
            this.reg = requireNonNull(rib.getDataBroker()).registerDataTreeChangeListener(treeId, this);
        }

        private Map<TablesKey, LongAdder> buildPrefixesTables(final Set<TablesKey> tables) {
            final ImmutableMap.Builder<TablesKey, LongAdder> b = ImmutableMap.builder();
            tables.forEach(table -> b.put(table, new LongAdder()));
            return b.build();
        }

        @Override
        @SuppressWarnings("unchecked")
        public synchronized void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<Tables>> changes) {
            LOG.trace("Data changed called to effective RIB. Change : {}", changes);
            WriteTransaction tx = null;
            for (final DataTreeModification<Tables> tc : changes) {
                final DataObjectModification<Tables> table = tc.getRootNode();
                if (tx == null) {
                    tx = this.chain.newWriteOnlyTransaction();
                }
                final DataObjectModification.ModificationType modificationType = table.getModificationType();
                switch (modificationType) {
                    case DELETE:
                        final Tables removeTable = table.getDataBefore();
                        final TablesKey tableKey = removeTable.getKey();
                        final KeyedInstanceIdentifier<Tables, TablesKey> effectiveTablePath
                                = this.effRibTables.child(Tables.class, tableKey);
                        LOG.debug("Delete Effective Table {} modification type {}, "
                                , effectiveTablePath, modificationType);
                        tx.delete(LogicalDatastoreType.OPERATIONAL, effectiveTablePath);
                        CountersUtil.decrement(this.prefixesInstalled.get(tableKey), tableKey);
                        break;
                    case SUBTREE_MODIFIED:
                        final Tables before = table.getDataBefore();
                        final Tables after = table.getDataAfter();
                        final TablesKey tk = after.getKey();
                        LOG.debug("Process table {} type {}, dataAfter {}, dataBefore {}",
                                tk, modificationType, after, before);

                        final KeyedInstanceIdentifier<Tables, TablesKey> tablePath
                                = this.effRibTables.child(Tables.class, tk);
                        final RIBSupport ribSupport = this.registry.getRIBSupport(tk);
                        if (ribSupport == null) {
                            break;
                        }
                        tx.put(LogicalDatastoreType.OPERATIONAL,
                                tablePath.child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                                        .rib.rev171207.rib.tables.Attributes.class), after.getAttributes());

                        final DataObjectModification routesChangesContainer =
                                table.getModifiedChildContainer(ribSupport.routesContainerClass());

                        if (routesChangesContainer == null) {
                            return;
                        }
                        updateRoutes(tx, tk, ribSupport, tablePath, routesChangesContainer.getModifiedChildren());
                        break;
                    case WRITE:
                        writeTable(tx, table);
                        break;
                    default:
                        LOG.warn("Ignoring unhandled root {}", table);
                        break;
                }
            }
            if (tx != null) {
                tx.submit();
            }
        }

        @SuppressWarnings("unchecked")
        private void updateRoutes(
                final WriteTransaction tx,
                final TablesKey tableKey, final RIBSupport ribSupport,
                final KeyedInstanceIdentifier<Tables, TablesKey> tablePath,
                final Collection<DataObjectModification<? extends DataObject>> routeChanges) {
            for (final DataObjectModification<? extends DataObject> routeChanged : routeChanges) {
                final Identifier routeKey
                        = ((InstanceIdentifier.IdentifiableItem) routeChanged.getIdentifier()).getKey();
                switch (routeChanged.getModificationType()) {
                    case SUBTREE_MODIFIED:
                    case WRITE:
                        writeRoutes(tx, tableKey, ribSupport, tablePath, routeKey, (Route) routeChanged.getDataAfter());
                        break;
                    case DELETE:
                        final InstanceIdentifier routeIID = ribSupport.createRouteIdentifier(tablePath, routeKey);
                        tx.delete(LogicalDatastoreType.OPERATIONAL, routeIID);
                        break;
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void writeRoutes(final WriteTransaction tx, final TablesKey tk, final RIBSupport ribSupport,
                final KeyedInstanceIdentifier<Tables, TablesKey> tablePath, final Identifier routeKey,
                final Route route) {
            final InstanceIdentifier routeIID = ribSupport.createRouteIdentifier(tablePath, routeKey);
            CountersUtil.increment(this.prefixesReceived.get(tk), tk);
            final Optional<Attributes> effAtt = this.ribPolicies
                    .applyImportPolicies(this.peerImportParameters, route.getAttributes());
            if (effAtt.isPresent()) {
                CountersUtil.increment(this.prefixesInstalled.get(tk), tk);
                tx.put(LogicalDatastoreType.OPERATIONAL, routeIID, route, true);
                tx.put(LogicalDatastoreType.OPERATIONAL, routeIID.child(Attributes.class), effAtt.get());
            } else {
                tx.delete(LogicalDatastoreType.OPERATIONAL, routeIID);
            }
        }

        @SuppressWarnings("unchecked")
        private void writeTable(final WriteTransaction tx, final DataObjectModification<Tables> table) {
            final Tables newTable = table.getDataAfter();
            if (newTable == null) {
                return;
            }
            final TablesKey tableKey = newTable.getKey();
            final KeyedInstanceIdentifier<Tables, TablesKey> tablePath
                    = this.effRibTables.child(Tables.class, tableKey);

            // Create an empty table
            LOG.trace("Create Empty table", tablePath);
            tx.put(LogicalDatastoreType.OPERATIONAL, tablePath, new TablesBuilder()
                    .setAfi(tableKey.getAfi()).setSafi(tableKey.getSafi())
                    .setAttributes(newTable.getAttributes()).build());

            final RIBSupport ribSupport = this.registry.getRIBSupport(tableKey);
            final Routes routes = newTable.getRoutes();
            if (ribSupport == null || routes == null) {
                return;
            }

            final DataObjectModification routesChangesContainer =
                    table.getModifiedChildContainer(ribSupport.routesContainerClass());

            if (routesChangesContainer == null) {
                return;
            }
            updateRoutes(tx, tableKey, ribSupport, tablePath, routesChangesContainer.getModifiedChildren());
        }

        @Override
        public synchronized void close() {
            this.reg.close();
            this.prefixesReceived.values().forEach(LongAdder::reset);
            this.prefixesInstalled.values().forEach(LongAdder::reset);
        }

        @Override
        public long getPrefixedReceivedCount(final TablesKey tablesKey) {
            final LongAdder counter = this.prefixesReceived.get(tablesKey);
            if (counter == null) {
                return 0;
            }
            return counter.longValue();
        }

        @Override
        public Set<TablesKey> getTableKeys() {
            return ImmutableSet.copyOf(this.prefixesReceived.keySet());
        }

        @Override
        public boolean isSupported(final TablesKey tablesKey) {
            return this.prefixesReceived.containsKey(tablesKey);
        }

        @Override
        public long getPrefixedInstalledCount(final TablesKey tablesKey) {
            final LongAdder counter = this.prefixesInstalled.get(tablesKey);
            if (counter == null) {
                return 0;
            }
            return counter.longValue();
        }

        @Override
        public long getTotalPrefixesInstalled() {
            return this.prefixesInstalled.values().stream().mapToLong(LongAdder::longValue).sum();
        }
    }
}
