/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.google.common.base.Verify;
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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunityUtil;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.RouteTargetConstrainSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.route.target.constrain.routes.RouteTargetConstrainRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteTarget;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
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
        AutoCloseable, ClusteredDOMDataTreeChangeListener {

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
        this.registry = requireNonNull(rib.getRibSupportContext());
        this.chain = requireNonNull(chain);
        this.peerIId = requireNonNull(peerIId);
        this.effRibTables = this.peerIId.node(EffectiveRibIn.QNAME);
        this.prefixesInstalled = buildPrefixesTables(tables);
        this.prefixesReceived = buildPrefixesTables(tables);
        this.ribPolicies = requireNonNull(rib.getRibPolicies());
        this.service = requireNonNull(rib.getService());
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        this.peerImportParameters = peer;
        this.rtMemberships = rtMemberships;
        this.rtCache = rtCache;
        this.vpnTableRefresher = rib;
    }

    public void init() {
        final DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL,
            this.peerIId.node(AdjRibIn.QNAME).node(Tables.QNAME));
        LOG.debug("Registered Effective RIB on {}", this.peerIId);
        this.reg = requireNonNull(this.service).registerDataTreeChangeListener(treeId, this);
    }

    private static Map<TablesKey, LongAdder> buildPrefixesTables(final Set<TablesKey> tables) {
        final ImmutableMap.Builder<TablesKey, LongAdder> b = ImmutableMap.builder();
        tables.forEach(table -> b.put(table, new LongAdder()));
        return b.build();
    }

    @Override
    public synchronized void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes) {
        if (this.chain == null) {
            LOG.trace("Chain closed. Ignoring Changes : {}", changes);
            return;
        }

        LOG.trace("Data changed called to effective RIB. Change : {}", changes);
        DOMDataWriteTransaction tx = null;
        for (final DataTreeCandidate tc : changes) {
            final YangInstanceIdentifier rootPath = tc.getRootPath();
            final DataTreeCandidateNode root = tc.getRootNode();
            for (final DataTreeCandidateNode table : root.getChildNodes()) {
                if (tx == null) {
                    tx = this.chain.newWriteOnlyTransaction();
                }
                changeDataTree(tx, rootPath, root, table);
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

        //Refresh VPN Table if RT Memberships were updated
        if (this.rtMembershipsUpdated) {
            this.vpnTableRefresher.refreshTable(IVP4_VPN_TABLE_KEY, this.peerImportParameters.getFromPeerId());
            this.vpnTableRefresher.refreshTable(IVP6_VPN_TABLE_KEY, this.peerImportParameters.getFromPeerId());
            this.rtMembershipsUpdated = false;
        }
    }

    @GuardedBy("this")
    private void changeDataTree(
        final DOMDataWriteTransaction tx,
        final YangInstanceIdentifier rootPath,
        final DataTreeCandidateNode root,
        final DataTreeCandidateNode table) {
        final PathArgument lastArg = table.getIdentifier();
        Verify.verify(lastArg instanceof NodeIdentifierWithPredicates,
            "Unexpected type %s in path %s", lastArg.getClass(), rootPath);
        final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) lastArg;
        final ModificationType modificationType = root.getModificationType();
        final RIBSupport ribSupport = this.registry.getRIBSupport(tableKey);
        final TablesKey tk = ribSupport.getTablesKey();
        final YangInstanceIdentifier effectiveTablePath = effectiveTablePath(tableKey);

        switch (modificationType) {
            case DISAPPEARED:
            case DELETE:
                processTableChildren(tx, ribSupport, effectiveTablePath, table.getChildNodes());
                LOG.debug("Delete Effective Table {} modification type {}, ", effectiveTablePath, modificationType);
                tx.delete(LogicalDatastoreType.OPERATIONAL, effectiveTablePath);
                break;
            case APPEARED:
            case WRITE:
                final RIBSupportContext ctx = this.registry.getRIBSupportContext(tk);
                LOG.trace("Create Empty table {}", tk);
                ctx.createEmptyTableStructure(tx, effectiveTablePath);
                processTableChildren(tx, ribSupport, effectiveTablePath, table.getChildNodes());
                break;
            case SUBTREE_MODIFIED:
                processTableChildren(tx, ribSupport, effectiveTablePath, table.getChildNodes());
                break;
            case UNMODIFIED:
                LOG.info("Ignoring spurious notification on {} data {}", rootPath, table);
                break;
            default:
                LOG.warn("Ignoring unhandled root {}", table);
                break;
        }
    }

    private void processTableChildren(
        final DOMDataWriteTransaction tx,
        final RIBSupport ribSupport,
        final YangInstanceIdentifier effectiveTablePath,
        final Collection<DataTreeCandidateNode> children) {
        for (final DataTreeCandidateNode child : children) {
            final PathArgument childIdentifier = child.getIdentifier();
            final Optional<NormalizedNode<?, ?>> childDataAfter = child.getDataAfter();
            LOG.debug("Process table {} type {}, dataAfter {}, dataBefore {}", childIdentifier, child
                .getModificationType(), childDataAfter, child.getDataBefore());
            final YangInstanceIdentifier routesPath = effectiveTablePath.node(childIdentifier);
            switch (child.getModificationType()) {
                case DELETE:
                case DISAPPEARED:
                    processDeleteRouteTables(child, childIdentifier, ribSupport, routesPath);
                    tx.delete(LogicalDatastoreType.OPERATIONAL, routesPath);
                    LOG.debug("Route deleted. routeId={}", routesPath);
                    break;
                case UNMODIFIED:
                    // No-op
                    break;
                case SUBTREE_MODIFIED:
                    processModifiedRouteTables(child, childIdentifier,tx, ribSupport, routesPath, childDataAfter);
                    break;
                case APPEARED:
                case WRITE:
                    writeRouteTables(child, childIdentifier, tx, ribSupport, routesPath, childDataAfter);
                    break;
                default:
                    LOG.warn("Ignoring unhandled child {}", child);
                    break;
            }
        }
    }

    private void processDeleteRouteTables(
        final DataTreeCandidateNode child,
        final PathArgument childIdentifier,
        final RIBSupport ribSupport,
        final YangInstanceIdentifier routesPath) {
        if (TABLE_ROUTES.equals(childIdentifier)) {
            final Collection<DataTreeCandidateNode> changedRoutes = ribSupport.changedRoutes(child);
            for (final DataTreeCandidateNode route : changedRoutes) {
                handleRouteTarget(ModificationType.DELETE, ribSupport, routesPath.getParent(),
                    route.getDataBefore().orElse(null));
                final TablesKey tablesKey = ribSupport.getTablesKey();
                CountersUtil.decrement(this.prefixesInstalled.get(tablesKey), tablesKey);
            }
        }
    }

    private void processModifiedRouteTables(
        final DataTreeCandidateNode child,
        final PathArgument childIdentifier,
        final DOMDataWriteTransaction tx,
        final RIBSupport ribSupport,
        final YangInstanceIdentifier routesPath,
        final Optional<NormalizedNode<?, ?>> childDataAfter) {
        if (TABLE_ROUTES.equals(childIdentifier)) {
            final Collection<DataTreeCandidateNode> changedRoutes = ribSupport.changedRoutes(child);
            for (final DataTreeCandidateNode route : changedRoutes) {
                processRoute(tx, ribSupport, routesPath.getParent(), route);
            }
        } else {
            tx.put(LogicalDatastoreType.OPERATIONAL, routesPath, childDataAfter.get());
        }
    }

    private void writeRouteTables(
        final DataTreeCandidateNode child,
        final PathArgument childIdentifier,
        final DOMDataWriteTransaction tx,
        final RIBSupport ribSupport,
        final YangInstanceIdentifier routesPath,
        final Optional<NormalizedNode<?, ?>> childDataAfter) {
        if (TABLE_ROUTES.equals(childIdentifier)) {
            final Collection<DataTreeCandidateNode> changedRoutes = ribSupport.changedRoutes(child);
            if (!changedRoutes.isEmpty()) {
                tx.put(LogicalDatastoreType.OPERATIONAL, routesPath, childDataAfter.get());
                // Routes are special, as they may end up being filtered. The previous put conveniently
                // ensured that we have them in at target, so a subsequent delete will not fail :)
                for (final DataTreeCandidateNode route : changedRoutes) {
                    processRoute(tx, ribSupport, routesPath.getParent(), route);
                }
            }
        }
    }

    private void processRoute(
        final DOMDataWriteTransaction tx,
        final RIBSupport ribSupport,
        final YangInstanceIdentifier routesPath,
        final DataTreeCandidateNode route) {
        LOG.debug("Process route {}", route.getIdentifier());
        final YangInstanceIdentifier routePath = ribSupport.routePath(routesPath, route.getIdentifier());
        final TablesKey tablesKey = ribSupport.getTablesKey();
        switch (route.getModificationType()) {
            case DELETE:
            case DISAPPEARED:
                deleteRoute(tx, ribSupport, routePath, route.getDataBefore().orElse(null), tablesKey);
                break;
            case UNMODIFIED:
                // No-op
                break;
            case APPEARED:
            case SUBTREE_MODIFIED:
            case WRITE:
                CountersUtil.increment(this.prefixesReceived.get(tablesKey), tablesKey);
                // Lookup per-table attributes from RIBSupport
                final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(route.getDataAfter(),
                    ribSupport.routeAttributesIdentifier()).orElse(null);
                final Attributes routeAttrs = ribSupport.attributeFromContainerNode(advertisedAttrs);
                final Optional<Attributes> optEffAtt;
                // In case we want to add LLGR_STALE we do not process route through policies since it may be
                // considered as received with LLGR_STALE from peer which is not true.
                final boolean longLivedStale = false;
                if (longLivedStale) {
                    // LLGR procedures are in effect. If the route is tagged with NO_LLGR, it needs to be removed.
                    final List<Communities> effCommunities = routeAttrs.getCommunities();
                    if (effCommunities != null && effCommunities.contains(CommunityUtil.NO_LLGR)) {
                        deleteRoute(tx, ribSupport, routePath, route.getDataBefore().orElse(null), tablesKey);
                        return;
                    }
                    optEffAtt = Optional.of(wrapLongLivedStale(routeAttrs));
                } else {
                    final Class<? extends AfiSafiType> afiSafiType
                        = tableTypeRegistry.getAfiSafiType(ribSupport.getTablesKey()).get();
                    optEffAtt = this.ribPolicies
                        .applyImportPolicies(this.peerImportParameters, routeAttrs, afiSafiType);
                }
                if (!optEffAtt.isPresent()) {
                    deleteRoute(tx, ribSupport, routePath, route.getDataBefore().orElse(null), tablesKey);
                    return;
                }
                final NormalizedNode<?, ?> routeChanged = route.getDataAfter().get();
                handleRouteTarget(ModificationType.WRITE, ribSupport, routePath, routeChanged);
                tx.put(LogicalDatastoreType.OPERATIONAL, routePath, routeChanged);
                CountersUtil.increment(this.prefixesInstalled.get(tablesKey), tablesKey);

                final YangInstanceIdentifier attPath = routePath.node(ribSupport.routeAttributesIdentifier());
                final ContainerNode finalAttribute = ribSupport.attributeToContainerNode(attPath, optEffAtt.get());
                tx.put(LogicalDatastoreType.OPERATIONAL, attPath, finalAttribute);
                break;
            default:
                LOG.warn("Ignoring unhandled route {}", route);
                break;
        }
    }

    private void deleteRoute(
        final DOMDataWriteTransaction tx,
        final RIBSupport ribSupport,
        final YangInstanceIdentifier routeIdPath,
        final NormalizedNode<?, ?> route,
        final TablesKey tablesKey) {
        handleRouteTarget(ModificationType.DELETE, ribSupport, routeIdPath, route);
        tx.delete(LogicalDatastoreType.OPERATIONAL, routeIdPath);
        LOG.debug("Route deleted. routeId={}", routeIdPath);
        CountersUtil.decrement(this.prefixesInstalled.get(tablesKey), tablesKey);
    }

    private void handleRouteTarget(
        final ModificationType modificationType,
        final RIBSupport ribSupport,
        final YangInstanceIdentifier routeIdPath,
        final NormalizedNode<?, ?> route) {
        if (ribSupport.getSafi() == RouteTargetConstrainSubsequentAddressFamily.class) {
            final RouteTargetConstrainRoute rtc =
                (RouteTargetConstrainRoute) ribSupport.fromNormalizedNode(routeIdPath, route);
            final RouteTarget rtMembership = RouteTargetMembeshipUtil.getRT(rtc);
            if (ModificationType.DELETE == modificationType) {
                if (PeerRole.Ebgp != this.peerImportParameters.getFromPeerRole()) {
                    this.rtCache.uncacheRoute(rtc);
                }
                this.rtMemberships.remove(rtMembership);
            } else {
                if (PeerRole.Ebgp != this.peerImportParameters.getFromPeerRole()) {
                    this.rtCache.cacheRoute(rtc);
                }
                this.rtMemberships.add(rtMembership);
            }
            this.rtMembershipsUpdated = true;
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

        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329
                .path.attributes.AttributesBuilder(attrs).setCommunities(newCommunities).build();
    }

    private YangInstanceIdentifier effectiveTablePath(final NodeIdentifierWithPredicates tableKey) {
        return this.effRibTables.node(Tables.QNAME).node(tableKey);
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
}
