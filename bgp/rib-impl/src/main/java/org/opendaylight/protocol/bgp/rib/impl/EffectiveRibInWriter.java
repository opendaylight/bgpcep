/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBIN_ATTRIBUTES_AID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ATTRIBUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.LLGR_STALE_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ROUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.UPTODATE_NID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.LongAdder;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunityUtil;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RibOutRefresh;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesInstalledCounters;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesReceivedCounters;
import org.opendaylight.protocol.bgp.rib.spi.RIBNormalizedNodes;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.ClientRouteTargetContrainCache;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.RouteTargetMembeshipUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.RouteTargetConstrainSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.route.target.constrain.routes.RouteTargetConstrainRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteTarget;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
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
 *
 * <p>
 * This class is NOT thread-safe.
 */
final class EffectiveRibInWriter implements PrefixesReceivedCounters, PrefixesInstalledCounters,
        AutoCloseable, ClusteredDOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(EffectiveRibInWriter.class);
    private static final TablesKey IVP4_VPN_TABLE_KEY =
        new TablesKey(Ipv4AddressFamily.VALUE, MplsLabeledVpnSubsequentAddressFamily.VALUE);
    private static final TablesKey IVP6_VPN_TABLE_KEY =
        new TablesKey(Ipv6AddressFamily.VALUE, MplsLabeledVpnSubsequentAddressFamily.VALUE);
    private static final ImmutableList<Communities> STALE_LLGR_COMMUNUTIES =
        ImmutableList.of(StaleCommunities.STALE_LLGR);
    private static final Attributes STALE_LLGR_ATTRIBUTES = new org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder()
            .setCommunities(STALE_LLGR_COMMUNUTIES)
            .build();
    private static final ChoiceNode EMPTY_ROUTES = Builders.choiceBuilder().withNodeIdentifier(ROUTES_NID).build();

    private final RIBSupportContextRegistry registry;
    private final YangInstanceIdentifier peerIId;
    private final YangInstanceIdentifier effRibTables;
    private final DOMDataTreeChangeService service;
    private final List<RouteTarget> rtMemberships;
    private final RibOutRefresh vpnTableRefresher;
    private final ClientRouteTargetContrainCache rtCache;
    private ListenerRegistration<?> reg;
    private DOMTransactionChain chain;
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
            final DOMTransactionChain chain,
            final YangInstanceIdentifier peerIId,
            final Set<TablesKey> tables,
            final BGPTableTypeRegistryConsumer tableTypeRegistry,
            final List<RouteTarget> rtMemberships,
            final ClientRouteTargetContrainCache rtCache) {
        registry = requireNonNull(rib.getRibSupportContext());
        this.chain = requireNonNull(chain);
        this.peerIId = requireNonNull(peerIId);
        effRibTables = this.peerIId.node(EFFRIBIN_NID);
        prefixesInstalled = buildPrefixesTables(tables);
        prefixesReceived = buildPrefixesTables(tables);
        ribPolicies = requireNonNull(rib.getRibPolicies());
        service = requireNonNull(rib.getService());
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        peerImportParameters = peer;
        this.rtMemberships = rtMemberships;
        this.rtCache = rtCache;
        vpnTableRefresher = rib;
    }

    public void init() {
        final DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL,
            peerIId.node(ADJRIBIN_NID).node(TABLES_NID));
        LOG.debug("Registered Effective RIB on {}", peerIId);
        reg = requireNonNull(service).registerDataTreeChangeListener(treeId, this);
    }

    private static Map<TablesKey, LongAdder> buildPrefixesTables(final Set<TablesKey> tables) {
        final ImmutableMap.Builder<TablesKey, LongAdder> b = ImmutableMap.builder();
        tables.forEach(table -> b.put(table, new LongAdder()));
        return b.build();
    }

    @Override
    public synchronized void onInitialData() {
        // FIXME: update as if root was deleted
    }

    @Override
    public synchronized void onDataTreeChanged(final List<DataTreeCandidate> changes) {
        if (chain == null) {
            LOG.trace("Chain closed. Ignoring Changes : {}", changes);
            return;
        }

        LOG.trace("Data changed called to effective RIB. Change : {}", changes);
        DOMDataTreeWriteTransaction tx = null;
        for (final DataTreeCandidate tc : changes) {
            final YangInstanceIdentifier rootPath = tc.getRootPath();
            final DataTreeCandidateNode root = tc.getRootNode();
            for (final DataTreeCandidateNode table : root.getChildNodes()) {
                if (tx == null) {
                    tx = chain.newWriteOnlyTransaction();
                }
                changeDataTree(tx, rootPath, root, table);
            }
        }

        if (tx != null) {
            final FluentFuture<? extends CommitInfo> future = tx.commit();
            submitted = future;
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

        //Refresh VPN Table if RT Memberships were updated
        if (rtMembershipsUpdated) {
            vpnTableRefresher.refreshTable(IVP4_VPN_TABLE_KEY, peerImportParameters.getFromPeerId());
            vpnTableRefresher.refreshTable(IVP6_VPN_TABLE_KEY, peerImportParameters.getFromPeerId());
            rtMembershipsUpdated = false;
        }
    }

    @Override
    public synchronized void close() {
        if (reg != null) {
            reg.close();
            reg = null;
        }
        if (submitted != null) {
            try {
                submitted.get();
            } catch (final InterruptedException | ExecutionException throwable) {
                LOG.error("Write routes failed", throwable);
            }
        }
        if (chain != null) {
            chain.close();
            chain = null;
        }
        prefixesReceived.values().forEach(LongAdder::reset);
        prefixesInstalled.values().forEach(LongAdder::reset);
    }

    @Override
    public long getPrefixedReceivedCount(final TablesKey tablesKey) {
        final LongAdder counter = prefixesReceived.get(tablesKey);
        if (counter == null) {
            return 0;
        }
        return counter.longValue();
    }

    @Override
    public Set<TablesKey> getTableKeys() {
        return ImmutableSet.copyOf(prefixesReceived.keySet());
    }

    @Override
    public boolean isSupported(final TablesKey tablesKey) {
        return prefixesReceived.containsKey(tablesKey);
    }

    @Override
    public long getPrefixedInstalledCount(final TablesKey tablesKey) {
        final LongAdder counter = prefixesInstalled.get(tablesKey);
        if (counter == null) {
            return 0;
        }
        return counter.longValue();
    }

    @Override
    public long getTotalPrefixesInstalled() {
        return prefixesInstalled.values().stream().mapToLong(LongAdder::longValue).sum();
    }

    @Holding("this")
    private void changeDataTree(final DOMDataTreeWriteTransaction tx, final YangInstanceIdentifier rootPath,
            final DataTreeCandidateNode root, final DataTreeCandidateNode table) {
        final PathArgument lastArg = table.getIdentifier();
        verify(lastArg instanceof NodeIdentifierWithPredicates, "Unexpected type %s in path %s", lastArg.getClass(),
            rootPath);
        final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) lastArg;
        final RIBSupportContext ribContext = registry.getRIBSupportContext(tableKey);
        if (ribContext == null) {
            LOG.warn("Table {} is not supported, ignoring event", tableKey);
            return;
        }

        final YangInstanceIdentifier effectiveTablePath = effectiveTablePath(tableKey);
        final ModificationType modificationType = root.getModificationType();
        LOG.debug("Effective table {} modification type {}", effectiveTablePath, modificationType);
        switch (modificationType) {
            case DISAPPEARED:
            case DELETE:
                deleteTable(tx, ribContext, effectiveTablePath, table);
                break;
            case APPEARED:
            case WRITE:
                writeTable(tx, ribContext, effectiveTablePath, table);
                break;
            case SUBTREE_MODIFIED:
                modifyTable(tx, ribContext, effectiveTablePath, table);
                break;
            case UNMODIFIED:
                LOG.info("Ignoring spurious notification on {} data {}", rootPath, table);
                break;
            default:
                LOG.warn("Ignoring unhandled root {}", table);
                break;
        }
    }

    private void deleteTable(final DOMDataTreeWriteTransaction tx, final RIBSupportContext ribContext,
            final YangInstanceIdentifier effectiveTablePath, final DataTreeCandidateNode table) {
        LOG.debug("Delete Effective Table {}", effectiveTablePath);
        onDeleteTable(ribContext.getRibSupport(), effectiveTablePath, table.getDataBefore());
        tx.delete(LogicalDatastoreType.OPERATIONAL, effectiveTablePath);
    }

    private void modifyTable(final DOMDataTreeWriteTransaction tx, final RIBSupportContext ribContext,
            final YangInstanceIdentifier effectiveTablePath, final DataTreeCandidateNode table) {
        LOG.debug("Modify Effective Table {}", effectiveTablePath);

        final boolean wasLongLivedStale = isLongLivedStaleTable(table.getDataBefore());
        final boolean longLivedStale = isLongLivedStaleTable(table.getDataAfter());
        if (wasLongLivedStale != longLivedStale) {
            LOG.debug("LLGR_STALE flag flipped {}, overwriting table {}", longLivedStale ? "ON" : "OFF",
                    effectiveTablePath);
            writeTable(tx, ribContext, effectiveTablePath, table);
            return;
        }

        table.getModifiedChild(ATTRIBUTES_NID).ifPresent(modifiedAttrs -> {
            final YangInstanceIdentifier effAttrsPath = effectiveTablePath.node(ATTRIBUTES_NID);
            final Optional<NormalizedNode> optAttrsAfter = modifiedAttrs.getDataAfter();
            if (optAttrsAfter.isPresent()) {
                tx.put(LogicalDatastoreType.OPERATIONAL, effAttrsPath, effectiveAttributes(
                    NormalizedNodes.findNode(optAttrsAfter.get(), UPTODATE_NID)));
            } else {
                tx.delete(LogicalDatastoreType.OPERATIONAL, effAttrsPath);
            }
        });

        table.getModifiedChild(ROUTES_NID).ifPresent(modifiedRoutes -> {
            final RIBSupport<?, ?> ribSupport = ribContext.getRibSupport();
            switch (modifiedRoutes.getModificationType()) {
                case APPEARED:
                case WRITE:
                    deleteRoutesBefore(tx, ribSupport, effectiveTablePath, modifiedRoutes);
                    // XXX: YANG Tools seems to have an issue stacking DELETE with child WRITE
                    tx.put(LogicalDatastoreType.OPERATIONAL, effectiveTablePath.node(ROUTES_NID), EMPTY_ROUTES);
                    writeRoutesAfter(tx, ribSupport, effectiveTablePath, modifiedRoutes.getDataAfter(), longLivedStale);
                    break;
                case DELETE:
                case DISAPPEARED:
                    deleteRoutesBefore(tx, ribSupport, effectiveTablePath, modifiedRoutes);
                    tx.delete(LogicalDatastoreType.OPERATIONAL, effectiveTablePath.node(ROUTES_NID));
                    break;
                case SUBTREE_MODIFIED:
                    for (DataTreeCandidateNode modifiedRoute : ribSupport.changedRoutes(modifiedRoutes)) {
                        processRoute(tx, ribSupport, effectiveTablePath, modifiedRoute, longLivedStale);
                    }
                    break;
                case UNMODIFIED:
                    // No-op
                    return;
                default:
                    LOG.warn("Ignoring modified routes {}", modifiedRoutes);
                    break;
            }
        });
    }

    private void writeTable(final DOMDataTreeWriteTransaction tx, final RIBSupportContext ribContext,
            final YangInstanceIdentifier effectiveTablePath, final DataTreeCandidateNode table) {
        LOG.debug("Write Effective Table {}", effectiveTablePath);
        onDeleteTable(ribContext.getRibSupport(), effectiveTablePath, table.getDataBefore());

        final Optional<NormalizedNode> maybeTableAfter = table.getDataAfter();
        if (maybeTableAfter.isPresent()) {
            final MapEntryNode tableAfter = extractMapEntry(maybeTableAfter);
            ribContext.createEmptyTableStructure(tx, effectiveTablePath);

            final Optional<DataContainerChild> maybeAttrsAfter = tableAfter.findChildByArg(ATTRIBUTES_NID);
            final boolean longLivedStale;
            if (maybeAttrsAfter.isPresent()) {
                final ContainerNode attrsAfter = extractContainer(maybeAttrsAfter);
                longLivedStale = isLongLivedStale(attrsAfter);
                tx.put(LogicalDatastoreType.OPERATIONAL, effectiveTablePath.node(ATTRIBUTES_NID),
                    effectiveAttributes(attrsAfter.findChildByArg(UPTODATE_NID)));
            } else {
                longLivedStale = false;
            }

            writeRoutesAfter(tx, ribContext.getRibSupport(), effectiveTablePath,
                NormalizedNodes.findNode(tableAfter, ROUTES_NID), longLivedStale);
        }
    }

    // Performs house-keeping when the contents of a table is deleted
    private void onDeleteTable(final RIBSupport<?, ?> ribSupport, final YangInstanceIdentifier effectiveTablePath,
            final Optional<NormalizedNode> tableBefore) {
        // Routes are special in that we need to process the to keep our counters accurate
        final Optional<NormalizedNode> maybeRoutesBefore = findRoutesMap(ribSupport,
                NormalizedNodes.findNode(tableBefore, ROUTES_NID));
        if (maybeRoutesBefore.isPresent()) {
            onRoutesDeleted(ribSupport, effectiveTablePath, extractMap(maybeRoutesBefore).body());
        }
    }

    private void deleteRoutesBefore(final DOMDataTreeWriteTransaction tx, final RIBSupport<?, ?> ribSupport,
            final YangInstanceIdentifier effectiveTablePath, final DataTreeCandidateNode modifiedRoutes) {
        final Optional<NormalizedNode> maybeRoutesBefore = NormalizedNodes.findNode(
            modifiedRoutes.getDataBefore(), ribSupport.relativeRoutesPath());
        if (maybeRoutesBefore.isPresent()) {
            onRoutesDeleted(ribSupport, effectiveTablePath, extractMap(maybeRoutesBefore).body());
        }
    }

    private void writeRoutesAfter(final DOMDataTreeWriteTransaction tx, final RIBSupport<?, ?> ribSupport,
            final YangInstanceIdentifier effectiveTablePath, final Optional<NormalizedNode> routesAfter,
            final boolean longLivedStale) {
        final Optional<NormalizedNode> maybeRoutesAfter = NormalizedNodes.findNode(routesAfter,
            ribSupport.relativeRoutesPath());
        if (maybeRoutesAfter.isPresent()) {
            final YangInstanceIdentifier routesPath = routeMapPath(ribSupport, effectiveTablePath);
            for (MapEntryNode routeAfter : extractMap(maybeRoutesAfter).body()) {
                writeRoute(tx, ribSupport, routesPath.node(routeAfter.getIdentifier()), Optional.empty(), routeAfter,
                    longLivedStale);
            }
        }
    }

    private void onRoutesDeleted(final RIBSupport<?, ?> ribSupport, final YangInstanceIdentifier effectiveTablePath,
            final Collection<MapEntryNode> deletedRoutes) {
        if (RouteTargetConstrainSubsequentAddressFamily.VALUE.equals(ribSupport.getSafi())) {
            final YangInstanceIdentifier routesPath = routeMapPath(ribSupport, effectiveTablePath);
            for (final MapEntryNode routeBefore : deletedRoutes) {
                deleteRouteTarget(ribSupport, routesPath.node(routeBefore.getIdentifier()), routeBefore);
            }
            rtMembershipsUpdated = true;
        }

        final TablesKey tablesKey = ribSupport.getTablesKey();
        CountersUtil.add(prefixesInstalled.get(tablesKey), tablesKey, -deletedRoutes.size());
    }

    private void processRoute(final DOMDataTreeWriteTransaction tx, final RIBSupport<?, ?> ribSupport,
            final YangInstanceIdentifier routesPath, final DataTreeCandidateNode route, final boolean longLivedStale) {
        LOG.debug("Process route {}", route.getIdentifier());
        final YangInstanceIdentifier routePath = ribSupport.routePath(routesPath, route.getIdentifier());
        switch (route.getModificationType()) {
            case DELETE:
            case DISAPPEARED:
                deleteRoute(tx, ribSupport, routePath, route.getDataBefore().orElse(null));
                break;
            case UNMODIFIED:
                // No-op
                break;
            case APPEARED:
            case SUBTREE_MODIFIED:
            case WRITE:
                writeRoute(tx, ribSupport, routePath, route.getDataBefore(), route.getDataAfter().get(),
                    longLivedStale);
                break;
            default:
                LOG.warn("Ignoring unhandled route {}", route);
                break;
        }
    }

    private void deleteRoute(final DOMDataTreeWriteTransaction tx, final RIBSupport<?, ?> ribSupport,
            final YangInstanceIdentifier routeIdPath, final NormalizedNode route) {
        handleRouteTarget(ModificationType.DELETE, ribSupport, routeIdPath, route);
        tx.delete(LogicalDatastoreType.OPERATIONAL, routeIdPath);
        LOG.debug("Route deleted. routeId={}", routeIdPath);
        final TablesKey tablesKey = ribSupport.getTablesKey();
        CountersUtil.decrement(prefixesInstalled.get(tablesKey), tablesKey);
    }

    private void writeRoute(final DOMDataTreeWriteTransaction tx, final RIBSupport<?, ?> ribSupport,
            final YangInstanceIdentifier routePath, final Optional<NormalizedNode> routeBefore,
            final NormalizedNode routeAfter, final boolean longLivedStale) {
        final TablesKey tablesKey = ribSupport.getTablesKey();
        CountersUtil.increment(prefixesReceived.get(tablesKey), tablesKey);
        // Lookup per-table attributes from RIBSupport
        final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(routeAfter,
            ribSupport.routeAttributesIdentifier()).orElse(null);
        final Attributes routeAttrs = ribSupport.attributeFromContainerNode(advertisedAttrs);
        final Optional<Attributes> optEffAtt;
        // In case we want to add LLGR_STALE we do not process route through policies since it may be
        // considered as received with LLGR_STALE from peer which is not true.
        if (longLivedStale) {
            // LLGR procedures are in effect. If the route is tagged with NO_LLGR, it needs to be removed.
            final List<Communities> effCommunities = routeAttrs.getCommunities();
            if (effCommunities != null && effCommunities.contains(CommunityUtil.NO_LLGR)) {
                deleteRoute(tx, ribSupport, routePath, routeBefore.orElse(null));
                return;
            }
            optEffAtt = Optional.of(wrapLongLivedStale(routeAttrs));
        } else {
            optEffAtt = ribPolicies.applyImportPolicies(peerImportParameters, routeAttrs,
                verifyNotNull(tableTypeRegistry.getAfiSafiType(ribSupport.getTablesKey())));
        }
        if (!optEffAtt.isPresent()) {
            deleteRoute(tx, ribSupport, routePath, routeBefore.orElse(null));
            return;
        }
        handleRouteTarget(ModificationType.WRITE, ribSupport, routePath, routeAfter);
        tx.put(LogicalDatastoreType.OPERATIONAL, routePath, routeAfter);
        CountersUtil.increment(prefixesInstalled.get(tablesKey), tablesKey);

        final Attributes attToStore = optEffAtt.get();
        if (!attToStore.equals(routeAttrs)) {
            final YangInstanceIdentifier attPath = routePath.node(ribSupport.routeAttributesIdentifier());
            final ContainerNode finalAttribute = ribSupport.attributeToContainerNode(attPath, attToStore);
            tx.put(LogicalDatastoreType.OPERATIONAL, attPath, finalAttribute);
        }
    }

    private void addRouteTarget(final RouteTargetConstrainRoute rtc) {
        final RouteTarget rtMembership = RouteTargetMembeshipUtil.getRT(rtc);
        if (PeerRole.Ebgp != peerImportParameters.getFromPeerRole()) {
            rtCache.cacheRoute(rtc);
        }
        rtMemberships.add(rtMembership);
    }

    private void deleteRouteTarget(final RIBSupport<?, ?> ribSupport, final YangInstanceIdentifier routeIdPath,
            final NormalizedNode route) {
        deleteRouteTarget((RouteTargetConstrainRoute) ribSupport.fromNormalizedNode(routeIdPath, route));
    }

    private void deleteRouteTarget(final RouteTargetConstrainRoute rtc) {
        final RouteTarget rtMembership = RouteTargetMembeshipUtil.getRT(rtc);
        if (PeerRole.Ebgp != peerImportParameters.getFromPeerRole()) {
            rtCache.uncacheRoute(rtc);
        }
        rtMemberships.remove(rtMembership);
    }

    private void handleRouteTarget(final ModificationType modificationType, final RIBSupport<?, ?> ribSupport,
            final YangInstanceIdentifier routeIdPath, final NormalizedNode route) {
        if (RouteTargetConstrainSubsequentAddressFamily.VALUE.equals(ribSupport.getSafi())) {
            final var rtc = (RouteTargetConstrainRoute) ribSupport.fromNormalizedNode(routeIdPath, route);
            if (ModificationType.DELETE == modificationType) {
                deleteRouteTarget(rtc);
            } else {
                addRouteTarget(rtc);
            }
            rtMembershipsUpdated = true;
        }
    }

    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
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

        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120
                .path.attributes.AttributesBuilder(attrs).setCommunities(newCommunities).build();
    }

    // XXX: this should be moved to YangInstanceIdentifier at some point
    private static YangInstanceIdentifier concat(final YangInstanceIdentifier parent, final List<PathArgument> args) {
        YangInstanceIdentifier ret = parent;
        for (PathArgument arg : args) {
            ret = ret.node(arg);
        }
        return ret;
    }

    private YangInstanceIdentifier effectiveTablePath(final NodeIdentifierWithPredicates tableKey) {
        return effRibTables.node(TABLES_NID).node(tableKey);
    }

    private static YangInstanceIdentifier routeMapPath(final RIBSupport<?, ?> ribSupport,
            final YangInstanceIdentifier tablePath) {
        return concat(tablePath.node(ROUTES_NID), ribSupport.relativeRoutesPath());
    }

    private static Optional<NormalizedNode> findRoutesMap(final RIBSupport<?, ?> ribSupport,
            final Optional<NormalizedNode> optRoutes) {
        return NormalizedNodes.findNode(optRoutes, ribSupport.relativeRoutesPath());
    }

    private static ContainerNode extractContainer(final Optional<? extends NormalizedNode> optNode) {
        final NormalizedNode node = optNode.get();
        verify(node instanceof ContainerNode, "Expected ContainerNode, got %s", node);
        return (ContainerNode) node;
    }

    private static MapNode extractMap(final Optional<? extends NormalizedNode> optNode) {
        final NormalizedNode node = optNode.get();
        verify(node instanceof MapNode, "Expected MapNode, got %s", node);
        return (MapNode) node;
    }

    private static MapEntryNode extractMapEntry(final Optional<? extends NormalizedNode> optNode) {
        final NormalizedNode node = optNode.get();
        verify(node instanceof MapEntryNode, "Expected MapEntryNode, got %s", node);
        return (MapEntryNode) node;
    }

    private static boolean isLongLivedStale(final ContainerNode attributes) {
        return NormalizedNodes.findNode(attributes, ADJRIBIN_ATTRIBUTES_AID, LLGR_STALE_NID).isPresent();
    }

    private static boolean isLongLivedStaleTable(final Optional<NormalizedNode> optTable) {
        final Optional<NormalizedNode> optAttributes = NormalizedNodes.findNode(optTable, ATTRIBUTES_NID);
        return optAttributes.isPresent() && isLongLivedStale(extractContainer(optAttributes));
    }

    private static ContainerNode effectiveAttributes(final Optional<? extends NormalizedNode> optUptodate) {
        return optUptodate.map(leaf -> {
            final Object value = leaf.body();
            verify(value instanceof Boolean, "Expected boolean uptodate, got %s", value);
            return (Boolean) value ? RIBNormalizedNodes.UPTODATE_ATTRIBUTES
                    : RIBNormalizedNodes.NOT_UPTODATE_ATTRIBUTES;
        }).orElse(RIBNormalizedNodes.NOT_UPTODATE_ATTRIBUTES);
    }
}
