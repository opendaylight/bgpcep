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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
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
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunityUtil;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RibOutRefresh;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesInstalledCounters;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesReceivedCounters;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.ClientRouteTargetContrainCache;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.RouteTargetMembeshipUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.adj.rib.in.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteTarget;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the BGP import policy. Listens on peer's Adj-RIB-In, inspects all inbound
 * routes in the context of the advertising peer's role and applies the inbound policy.
 *
 * <p>
 * Inbound policy is applied as follows:
 *
 * <p>
 * 1) if the peer is an eBGP peer, perform attribute replacement and filtering
 * 2) check if a route is admissible based on attributes attached to it, as well as the
 * advertising peer's role
 * 3) output admitting routes with edited attributes into /bgp-rib/rib/peer/effective-rib-in/tables/routes
 */
@NotThreadSafe
final class EffectiveRibInWriter implements PrefixesReceivedCounters, PrefixesInstalledCounters,
        AutoCloseable, ClusteredDataTreeChangeListener<Tables> {

    private static final Logger LOG = LoggerFactory.getLogger(EffectiveRibInWriter.class);
    static final NodeIdentifier TABLE_ROUTES = new NodeIdentifier(Routes.QNAME);
    private static final TablesKey IVP4_VPN_TABLE_KEY = new TablesKey(Ipv4AddressFamily.class,
            MplsLabeledVpnSubsequentAddressFamily.class);
    private static final TablesKey IVP6_VPN_TABLE_KEY = new TablesKey(Ipv6AddressFamily.class,
            MplsLabeledVpnSubsequentAddressFamily.class);
    private static final ImmutableList<Communities> STALE_LLGR_COMMUNUTIES = ImmutableList.of(
        StaleCommunities.STALE_LLGR);
    private static final Attributes STALE_LLGR_ATTRIBUTES = new org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder()
            .setCommunities(STALE_LLGR_COMMUNUTIES)
            .build();

    private final RIBSupportContextRegistry registry;
    private final KeyedInstanceIdentifier<Peer, PeerKey> peerIId;
    private final InstanceIdentifier<EffectiveRibIn> effRibTables;
    private final DataBroker databroker;
    private final List<RouteTarget> rtMemberships;
    private final RibOutRefresh vpnTableRefresher;
    private final ClientRouteTargetContrainCache rtCache;
    private ListenerRegistration<?> reg;
    private BindingTransactionChain chain;
    private final Map<TablesKey, LongAdder> prefixesReceived;
    private final Map<TablesKey, LongAdder> prefixesInstalled;
    private final BGPRibRoutingPolicy ribPolicies;
    private final BGPRouteEntryImportParameters peerImportParameters;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    @GuardedBy("this")
    private FluentFuture<? extends CommitInfo> submitted;
    private boolean rtMembershipsUpdated;

    EffectiveRibInWriter(
            final BGPRouteEntryImportParameters peer,
            final RIB rib,
            final BindingTransactionChain chain,
            final KeyedInstanceIdentifier<Peer, PeerKey> peerIId,
            final Set<TablesKey> tables,
            final BGPTableTypeRegistryConsumer tableTypeRegistry,
            final List<RouteTarget> rtMemberships,
            final ClientRouteTargetContrainCache rtCache) {
        this.registry = requireNonNull(rib.getRibSupportContext());
        this.chain = requireNonNull(chain);
        this.peerIId = requireNonNull(peerIId);
        this.effRibTables = this.peerIId.child(EffectiveRibIn.class);
        this.prefixesInstalled = buildPrefixesTables(tables);
        this.prefixesReceived = buildPrefixesTables(tables);
        this.ribPolicies = requireNonNull(rib.getRibPolicies());
        this.databroker = requireNonNull(rib.getDataBroker());
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        this.peerImportParameters = peer;
        this.rtMemberships = rtMemberships;
        this.rtCache = rtCache;
        this.vpnTableRefresher = rib;
    }

    public void init() {
        final DataTreeIdentifier<Tables> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                this.peerIId.child(AdjRibIn.class).child(Tables.class));
        LOG.debug("Registered Effective RIB on {}", this.peerIId);
        this.reg = requireNonNull(this.databroker).registerDataTreeChangeListener(treeId, this);
    }

    private static Map<TablesKey, LongAdder> buildPrefixesTables(final Set<TablesKey> tables) {
        final ImmutableMap.Builder<TablesKey, LongAdder> b = ImmutableMap.builder();
        tables.forEach(table -> b.put(table, new LongAdder()));
        return b.build();
    }

    @Override
    public synchronized void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<Tables>> changes) {
        if (this.chain == null) {
            LOG.trace("Chain closed. Ignoring Changes : {}", changes);
            return;
        }

        LOG.trace("Data changed called to effective RIB. Change : {}", changes);
        if (!changes.isEmpty()) {
            processModifications(changes);
        }

        //Refresh VPN Table if RT Memberships were updated
        if (this.rtMembershipsUpdated) {
            this.vpnTableRefresher.refreshTable(IVP4_VPN_TABLE_KEY, this.peerImportParameters.getFromPeerId());
            this.vpnTableRefresher.refreshTable(IVP6_VPN_TABLE_KEY, this.peerImportParameters.getFromPeerId());
            this.rtMembershipsUpdated = false;
        }
    }

    @GuardedBy("this")
    @SuppressWarnings("unchecked")
    private void processModifications(final Collection<DataTreeModification<Tables>> changes) {
        final WriteTransaction tx = this.chain.newWriteOnlyTransaction();
        for (final DataTreeModification<Tables> tc : changes) {
            final DataObjectModification<Tables> table = tc.getRootNode();
            final DataObjectModification.ModificationType modificationType = table.getModificationType();
            switch (modificationType) {
                case DELETE:
                    final Tables removeTable = table.getDataBefore();
                    final TablesKey tableKey = removeTable.key();
                    final KeyedInstanceIdentifier<Tables, TablesKey> effectiveTablePath = this.effRibTables
                            .child(Tables.class, tableKey);
                    LOG.debug("Delete Effective Table {} modification type {}, ", effectiveTablePath, modificationType);
                    tx.delete(LogicalDatastoreType.OPERATIONAL, effectiveTablePath);
                    CountersUtil.decrement(this.prefixesInstalled.get(tableKey), tableKey);
                    break;
                case SUBTREE_MODIFIED:
                    final Tables before = table.getDataBefore();
                    final Tables after = table.getDataAfter();
                    final TablesKey tk = after.key();
                    LOG.debug("Process table {} type {}, dataAfter {}, dataBefore {}",
                            tk, modificationType, after, before);

                    final KeyedInstanceIdentifier<Tables, TablesKey> tablePath
                            = this.effRibTables.child(Tables.class, tk);
                    final RIBSupport ribSupport = this.registry.getRIBSupport(tk);
                    if (ribSupport == null) {
                        break;
                    }

                    final boolean longLivedStale = longLivedStaleTable(after.getAttributes());
                    final DataObjectModification<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                        .rib.rev180329.rib.tables.Attributes> adjRibAttrsChanged = table.getModifiedChildContainer(
                            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329
                                .rib.tables.Attributes.class);
                    final boolean changedLongLivedStale;
                    if (adjRibAttrsChanged != null) {
                        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329
                            .rib.tables.Attributes adjRibAttrs = adjRibAttrsChanged.getDataAfter();
                        tx.put(LogicalDatastoreType.OPERATIONAL,
                            tablePath.child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                                .rib.rev180329.rib.tables.Attributes.class), effRibAttrs(adjRibAttrs));
                        final boolean wasLongLivedStale = before != null && longLivedStaleTable(before.getAttributes());
                        changedLongLivedStale = longLivedStale != wasLongLivedStale;
                    } else {
                        changedLongLivedStale = false;
                    }

                    if (!changedLongLivedStale) {
                        final DataObjectModification routesChangesContainer = table.getModifiedChildContainer(
                            ribSupport.routesCaseClass(), ribSupport.routesContainerClass());
                        if (routesChangesContainer != null) {
                            updateRoutes(tx, tk, ribSupport, tablePath, routesChangesContainer.getModifiedChildren(),
                                longLivedStale);
                        }
                    } else {
                        // Long-lived Graceful Restart Stale flag has changed. Re-write the entire table.
                        writeTable(tx, table);
                    }
                    break;
                case WRITE:
                    writeTable(tx, table);
                    break;
                default:
                    LOG.warn("Ignoring unhandled root {}", table);
                    break;
            }
        }

        final FluentFuture<? extends CommitInfo> future = tx.commit();
        this.submitted = future;
        future.addCallback(new FutureCallback<CommitInfo>() {
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

    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>> void updateRoutes(
            final WriteTransaction tx, final TablesKey tableKey, final RIBSupport<C, S, R, I> ribSupport,
            final KeyedInstanceIdentifier<Tables, TablesKey> tablePath,
            final Collection<DataObjectModification<R>> routeChanges, final boolean longLivedStale) {

        Class<? extends AfiSafiType> afiSafiType = null;
        for (final DataObjectModification<R> routeChanged : routeChanges) {
            final PathArgument routeChangeId = routeChanged.getIdentifier();
            verify(routeChangeId instanceof IdentifiableItem, "Route change %s has invalid identifier %s",
                routeChanged, routeChangeId);
            @SuppressWarnings("unchecked")
            final I routeKey = ((IdentifiableItem<R, I>) routeChangeId).getKey();

            switch (routeChanged.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    if (afiSafiType == null) {
                        afiSafiType = tableTypeRegistry.getAfiSafiType(ribSupport.getTablesKey()).get();
                    }

                    writeRoute(tx, tableKey, afiSafiType, ribSupport, tablePath, routeKey,
                        routeChanged.getDataAfter(), longLivedStale);
                    break;
                case DELETE:
                    final InstanceIdentifier<R> routeIID = ribSupport.createRouteIdentifier(tablePath, routeKey);
                    deleteRoute(routeIID, routeChanged.getDataBefore(), tx);
                    break;
            }
        }
    }

    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>> void writeRoute(
            final WriteTransaction tx, final TablesKey tk, final Class<? extends AfiSafiType> afiSafiType,
            final RIBSupport<C, S, R, I> ribSupport, final KeyedInstanceIdentifier<Tables, TablesKey> tablePath,
            final I routeKey, final R route, final boolean longLivedStale) {
        final InstanceIdentifier<R> routeIID = ribSupport.createRouteIdentifier(tablePath, routeKey);
        CountersUtil.increment(this.prefixesReceived.get(tk), tk);

        final Attributes routeAttrs = route.getAttributes();
        final Attributes inputAttrs;
        if (longLivedStale) {
            // LLGR stale procedures are in effect for the peer. Make sure we add LLGR_STALE to the set of received
            // communities before processing import policies. Import policy can
            inputAttrs = wrapLongLivedStale(routeAttrs);
        } else {
            inputAttrs = routeAttrs;
        }

        final Optional<Attributes> optEffAttr = this.ribPolicies.applyImportPolicies(this.peerImportParameters,
            inputAttrs, afiSafiType);
        if (!optEffAttr.isPresent()) {
            // Input policy has decided to not allow the route in, make sure we remove it
            deleteRoute(routeIID, route, tx);
            return;
        }

        final Attributes effAttr = optEffAttr.get();
        if (longLivedStale) {
            // LLGR procedures are in effect. If the route is tagged with NO_LLGR, it needs to be removed.
            final List<Communities> effCommunities = routeAttrs.getCommunities();
            if (effCommunities != null) {
                if (effCommunities.contains(CommunityUtil.NO_LLGR)) {
                    deleteRoute(routeIID, route, tx);
                    return;
                }
            }
        }

        final Optional<RouteTarget> rtMembership = RouteTargetMembeshipUtil.getRT(route);
        if (rtMembership.isPresent()) {
            final RouteTarget rt = rtMembership.get();
            if (PeerRole.Ebgp != this.peerImportParameters.getFromPeerRole()) {
                this.rtCache.cacheRoute(route);
            }
            this.rtMemberships.add(rt);
            this.rtMembershipsUpdated = true;
        }
        CountersUtil.increment(this.prefixesInstalled.get(tk), tk);
        tx.put(LogicalDatastoreType.OPERATIONAL, routeIID, route);
        tx.put(LogicalDatastoreType.OPERATIONAL, routeIID.child(Attributes.class), effAttr);
    }

    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>> void writeRoutes(
                    final WriteTransaction tx, final TablesKey tk, final RIBSupport<C, S, R, I> ribSupport,
                    final KeyedInstanceIdentifier<Tables, TablesKey> tablePath, final Tables newTable,
                    final boolean longLivedStale) {
        final Routes routes = newTable.getRoutes();
        if (routes != null) {
            for (R route : ribSupport.extractAdjRibInRoutes(routes)) {
                writeRoute(tx, tk, tableTypeRegistry.getAfiSafiType(ribSupport.getTablesKey()).get(), ribSupport,
                    tablePath, route.key(), route, longLivedStale);
            }
        }
    }

    private static Attributes wrapLongLivedStale(final Attributes attrs) {
        if (attrs == null) {
            return STALE_LLGR_ATTRIBUTES;
        }

        final List<Communities> oldCommunities = attrs.getCommunities();
        final List<Communities> newCommunities;
        if (oldCommunities != null) {
            if (oldCommunities.contains(StaleCommunities.STALE_LLGR)) {
                return attrs;
            }
            newCommunities = StaleCommunities.create(oldCommunities);
        } else {
            newCommunities = STALE_LLGR_COMMUNUTIES;
        }

        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329
                .path.attributes.AttributesBuilder(attrs).setCommunities(newCommunities).build();
    }

    private <R extends Route> void deleteRoute(final InstanceIdentifier<R> routeIID, final R route,
            final WriteTransaction tx) {
        deleteRT(route);
        tx.delete(LogicalDatastoreType.OPERATIONAL, routeIID);
    }

    private  <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>> void writeTable(
                    final WriteTransaction tx, final DataObjectModification<Tables> table) {
        final Tables newTable = table.getDataAfter();
        if (newTable == null) {
            final Tables oldTable = table.getDataBefore();
            if (oldTable != null) {
                final TablesKey tableKey = oldTable.key();
                final KeyedInstanceIdentifier<Tables, TablesKey> tablePath = tablePath(tableKey);
                LOG.trace("Delete table at {}", tablePath);
                tx.delete(LogicalDatastoreType.OPERATIONAL, tablePath);

                final RIBSupport<C, S, R, I> ribSupport = this.registry.getRIBSupport(tableKey);
                if (ribSupport != null) {
                    final Routes oldRoutes = oldTable.getRoutes();
                    if (oldRoutes != null) {
                        for (R route : ribSupport.extractAdjRibInRoutes(oldRoutes)) {
                            deleteRT(route);
                        }
                    }
                }
            }
            return;
        }

        final TablesKey tableKey = newTable.key();
        final KeyedInstanceIdentifier<Tables, TablesKey> tablePath = tablePath(tableKey);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Attributes
            attrs = newTable.getAttributes();

        // Create an empty table
        LOG.trace("Create Empty table at {}", tablePath);
        tx.put(LogicalDatastoreType.OPERATIONAL, tablePath, new TablesBuilder()
            .withKey(tableKey).setAfi(tableKey.getAfi()).setSafi(tableKey.getSafi())
            .setAttributes(effRibAttrs(attrs)).build());

        final RIBSupport<C, S, R, I> ribSupport = this.registry.getRIBSupport(tableKey);
        if (ribSupport == null) {
            LOG.trace("No RIB support for {}", tableKey);
            return;
        }

        writeTableRoutes(tx, tableKey, ribSupport, tablePath, newTable, longLivedStaleTable(attrs));
    }

    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>> void writeTableRoutes(
                    final WriteTransaction tx, final TablesKey tableKey, final RIBSupport<C, S, R, I> ribSupport,
                    final KeyedInstanceIdentifier<Tables, TablesKey> tablePath, final Tables newTable,
                    final boolean longLivedStale) {
        final Routes routes = newTable.getRoutes();
        if (routes != null) {
            final Class<? extends AfiSafiType> afiSafiType = tableTypeRegistry.getAfiSafiType(ribSupport.getTablesKey())
                    .get();
            for (R route : ribSupport.extractAdjRibInRoutes(routes)) {
                writeRoute(tx, tableKey, afiSafiType, ribSupport, tablePath, route.key(), route, longLivedStale);
            }
        }
    }

    private KeyedInstanceIdentifier<Tables, TablesKey> tablePath(final TablesKey tableKey) {
        return this.effRibTables.child(Tables.class, tableKey);
    }

    @Override
    public synchronized void close() {
        if (this.reg != null) {
            this.reg.close();
            this.reg = null;
        }
        if (this.submitted != null) {
            try {
                this.submitted.get();
            } catch (final InterruptedException | ExecutionException throwable) {
                LOG.error("Write routes failed", throwable);
            }
        }
        if (this.chain != null) {
            this.chain.close();
            this.chain = null;
        }
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

    private void deleteRT(final Route route) {
        final Optional<RouteTarget> rtMembership = RouteTargetMembeshipUtil.getRT(route);
        if (rtMembership.isPresent()) {
            if (PeerRole.Ebgp != this.peerImportParameters.getFromPeerRole()) {
                this.rtCache.uncacheRoute(route);
            }
            this.rtMemberships.remove(rtMembership.get());
            this.rtMembershipsUpdated = true;
        }
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables
            .Attributes effRibAttrs(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib
                    .rev180329.rib.tables.Attributes adjRibAttrs) {
        // Make sure we clear the augmentation specific to adj-rib-in
        return new AttributesBuilder(adjRibAttrs).addAugmentation(Attributes1.class, null).build();
    }

    private static boolean longLivedStaleTable(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .bgp.rib.rev180329.rib.tables.Attributes adjRibAttrs) {
        if (adjRibAttrs == null) {
            return false;
        }
        final Attributes1 staleAugment = adjRibAttrs.augmentation(Attributes1.class);
        return staleAugment != null && staleAugment.isLlgrStale() != null;
    }
}
