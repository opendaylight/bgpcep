/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
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
 * Inbound policy is applied as follows:
 *
 * 1) if the peer is an eBGP peer, perform attribute replacement and filtering
 * 2) check if a route is admissible based on attributes attached to it, as well as the
 *    advertising peer's role
 * 3) output admitting routes with edited attributes into /bgp-rib/rib/peer/effective-rib-in/tables/routes
 *
 */
@NotThreadSafe
final class EffectiveRibInWriter implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(EffectiveRibInWriter.class);
    protected static final NodeIdentifier TABLE_ROUTES = new NodeIdentifier(Routes.QNAME);

    private final class AdjInTracker implements AutoCloseable, DOMDataTreeChangeListener {
        private final RIBSupportContextRegistry registry;
        private final YangInstanceIdentifier peerIId;
        private final YangInstanceIdentifier effRibTables;
        private final ListenerRegistration<?> reg;
        private final DOMTransactionChain chain;
        private final PerTableTypeRouteCounter effectiveRibInRouteCounters;
        private final PerTableTypeRouteCounter adjRibInRouteCounters;
        private final Map<TablesKey, Set<YangInstanceIdentifier>> effectiveRibInRouteMap = new ConcurrentHashMap<>();
        private final Map<TablesKey, Set<YangInstanceIdentifier>> adjRibInRouteMap = new ConcurrentHashMap<>();

        AdjInTracker(final DOMDataTreeChangeService service, final RIBSupportContextRegistry registry, final DOMTransactionChain chain, final YangInstanceIdentifier peerIId,
            @Nonnull final PerTableTypeRouteCounter effectiveRibInRouteCounters, @Nonnull final PerTableTypeRouteCounter adjRibInRouteCounters) {
            this.registry = Preconditions.checkNotNull(registry);
            this.chain = Preconditions.checkNotNull(chain);
            this.peerIId = Preconditions.checkNotNull(peerIId);
            this.effRibTables = this.peerIId.node(EffectiveRibIn.QNAME).node(Tables.QNAME);
            this.effectiveRibInRouteCounters = Preconditions.checkNotNull(effectiveRibInRouteCounters);
            this.adjRibInRouteCounters = Preconditions.checkNotNull(adjRibInRouteCounters);

            final DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, this.peerIId.node(AdjRibIn.QNAME).node(Tables.QNAME));
            LOG.debug("Registered Effective RIB on {}", this.peerIId);
            this.reg = service.registerDataTreeChangeListener(treeId, this);
        }

        /**
         * @deprecated Should always pass in route counter
         * @param service
         * @param registry
         * @param chain
         * @param peerIId
         */
        @Deprecated
        AdjInTracker(final DOMDataTreeChangeService service, final RIBSupportContextRegistry registry, final DOMTransactionChain chain, final YangInstanceIdentifier peerIId) {
            this(service, registry, chain, peerIId, new PerTableTypeRouteCounter("effective-rib-in"), new PerTableTypeRouteCounter("adj-rib-in"));
        }

        private void updateRoute(@Nonnull final PerTableTypeRouteCounter counter, @Nonnull final Map<TablesKey, Set<YangInstanceIdentifier>> routeMap,
            @Nonnull final TablesKey tablesKey, @Nonnull final YangInstanceIdentifier routeId) {
            routeMap.putIfAbsent(tablesKey, new HashSet<>());
            routeMap.get(tablesKey).add(routeId);

            updateRouteCounter(counter, routeMap,tablesKey);
        }

        private void deleteRoute(@Nonnull final PerTableTypeRouteCounter counter, @Nonnull final Map<TablesKey, Set<YangInstanceIdentifier>> routeMap,
            @Nonnull final TablesKey tablesKey, @Nonnull final YangInstanceIdentifier routeId) {
            if (routeMap.containsKey(tablesKey)) {
                routeMap.get(tablesKey).remove(routeId);
            }

            updateRouteCounter(counter, routeMap,tablesKey);
        }

        private void deleteRoute(@Nonnull final PerTableTypeRouteCounter counter, @Nonnull final Map<TablesKey, Set<YangInstanceIdentifier>> routeMap,
            @Nonnull final TablesKey tablesKey) {
            routeMap.remove(tablesKey);

            updateRouteCounter(counter, routeMap,tablesKey);
        }

        private void updateRouteCounter(@Nonnull final PerTableTypeRouteCounter counter, @Nonnull final Map<TablesKey, Set<YangInstanceIdentifier>> routeMap,
            @Nonnull final TablesKey tablesKey) {
            counter.getCounterOrSetDefault(tablesKey)
                .setCount(routeMap.getOrDefault(tablesKey, new HashSet<>()).size());
        }

        private void processRoute(final DOMDataWriteTransaction tx, final RIBSupport ribSupport, final AbstractImportPolicy policy, final YangInstanceIdentifier routesPath, final DataTreeCandidateNode route) {
            LOG.debug("Process route {}", route.getIdentifier());
            final YangInstanceIdentifier routeId = ribSupport.routePath(routesPath, route.getIdentifier());
            final TablesKey tablesKey = new TablesKey(ribSupport.getAfi(), ribSupport.getSafi());
            switch (route.getModificationType()) {
            case DELETE:
            case DISAPPEARED:
                tx.delete(LogicalDatastoreType.OPERATIONAL, routeId);
                LOG.debug("Route deleted. routeId={}", routeId);

                deleteRoute(adjRibInRouteCounters, adjRibInRouteMap, tablesKey, routeId);
                deleteRoute(effectiveRibInRouteCounters, effectiveRibInRouteMap, tablesKey, routeId);
                break;
            case UNMODIFIED:
                // No-op
                break;
            case APPEARED:
            case SUBTREE_MODIFIED:
            case WRITE:
                tx.put(LogicalDatastoreType.OPERATIONAL, routeId, route.getDataAfter().get());
                // count adj-rib-in route first
                updateRoute(adjRibInRouteCounters, adjRibInRouteMap, tablesKey, routeId);
                updateRoute(effectiveRibInRouteCounters, effectiveRibInRouteMap, tablesKey, routeId);
                // Lookup per-table attributes from RIBSupport
                final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(route.getDataAfter(), ribSupport.routeAttributesIdentifier()).orNull();
                final ContainerNode effectiveAttrs;

                if (advertisedAttrs != null) {
                    effectiveAttrs = policy.effectiveAttributes(advertisedAttrs);
                } else {
                    effectiveAttrs = null;
                }

                LOG.debug("Route {} effective attributes {} towards {}", route.getIdentifier(), effectiveAttrs, routeId);

                if (effectiveAttrs != null) {
                    tx.put(LogicalDatastoreType.OPERATIONAL, routeId.node(ribSupport.routeAttributesIdentifier()), effectiveAttrs);

                    updateRoute(effectiveRibInRouteCounters, effectiveRibInRouteMap, tablesKey, routeId);
                } else {
                    LOG.warn("Route {} advertised empty attributes", routeId);
                    tx.delete(LogicalDatastoreType.OPERATIONAL,  routeId);

                    deleteRoute(effectiveRibInRouteCounters, effectiveRibInRouteMap, tablesKey, routeId);
                }
                break;
            default:
                LOG.warn("Ignoring unhandled route {}", route);
                break;
            }
        }

        private void processTableChildren(final DOMDataWriteTransaction tx, final RIBSupport ribSupport, final YangInstanceIdentifier tablePath, final Collection<DataTreeCandidateNode> children) {
            for (final DataTreeCandidateNode child : children) {
                final PathArgument childIdentifier = child.getIdentifier();
                final Optional<NormalizedNode<?, ?>> childDataAfter = child.getDataAfter();
                final TablesKey tablesKey = new TablesKey(ribSupport.getAfi(), ribSupport.getSafi());
                LOG.debug("Process table {} type {}, dataAfter {}, dataBefore {}", childIdentifier, child
                    .getModificationType(), childDataAfter, child.getDataBefore());
                final YangInstanceIdentifier childPath = tablePath.node(childIdentifier);
                switch (child.getModificationType()) {
                case DELETE:
                case DISAPPEARED:
                    tx.delete(LogicalDatastoreType.OPERATIONAL, childPath);
                    LOG.debug("Route deleted. routeId={}", childPath);

                    deleteRoute(adjRibInRouteCounters, adjRibInRouteMap, tablesKey, childPath);
                    deleteRoute(effectiveRibInRouteCounters, effectiveRibInRouteMap, tablesKey, childPath);
                    break;
                case UNMODIFIED:
                    // No-op
                    break;
                case SUBTREE_MODIFIED:
                    processModifiedRouteTables(child, childIdentifier,tx, ribSupport, importPolicy, childPath, childDataAfter);
                    break;
                case APPEARED:
                case WRITE:
                    writeRouteTables(child, childIdentifier,tx, ribSupport, importPolicy, childPath, childDataAfter);

                    break;
                default:
                    LOG.warn("Ignoring unhandled child {}", child);
                    break;
                }
            }
        }

        private void processModifiedRouteTables(final DataTreeCandidateNode child, final PathArgument childIdentifier, final DOMDataWriteTransaction tx,
            final RIBSupport ribSupport, final AbstractImportPolicy policy, final YangInstanceIdentifier childPath, final Optional<NormalizedNode<?, ?>> childDataAfter) {
            if (TABLE_ROUTES.equals(childIdentifier)) {
                for (final DataTreeCandidateNode route : ribSupport.changedRoutes(child)) {
                    processRoute(tx, ribSupport, policy, childPath, route);
                }
            } else {
                tx.put(LogicalDatastoreType.OPERATIONAL, childPath, childDataAfter.get());
            }
        }

        private void writeRouteTables(final DataTreeCandidateNode child, final PathArgument childIdentifier, final DOMDataWriteTransaction tx, final RIBSupport ribSupport, final AbstractImportPolicy policy, final YangInstanceIdentifier childPath, final Optional<NormalizedNode<?, ?>> childDataAfter) {
            if (TABLE_ROUTES.equals(childIdentifier)) {
                final Collection<DataTreeCandidateNode> changedRoutes = ribSupport.changedRoutes(child);
                if (!changedRoutes.isEmpty()) {
                    tx.put(LogicalDatastoreType.OPERATIONAL, childPath, childDataAfter.get());
                    // Routes are special, as they may end up being filtered. The previous put conveniently
                    // ensured that we have them in at target, so a subsequent delete will not fail :)
                    for (final DataTreeCandidateNode route : changedRoutes) {
                        processRoute(tx, ribSupport, policy, childPath, route);
                    }
                }
            }
        }

        private RIBSupportContext getRibSupport(final NodeIdentifierWithPredicates tableKey) {
            return this.registry.getRIBSupportContext(tableKey);
        }

        private YangInstanceIdentifier effectiveTablePath(final NodeIdentifierWithPredicates tableKey) {
            return this.effRibTables.node(tableKey);
        }

        private void modifyTable(final DOMDataWriteTransaction tx, final NodeIdentifierWithPredicates tableKey, final DataTreeCandidateNode table) {
            final RIBSupportContext ribSupport = getRibSupport(tableKey);
            final YangInstanceIdentifier tablePath = effectiveTablePath(tableKey);

            processTableChildren(tx, ribSupport.getRibSupport(), tablePath, table.getChildNodes());
        }

        private void writeTable(final DOMDataWriteTransaction tx, final NodeIdentifierWithPredicates tableKey, final DataTreeCandidateNode table) {
            final RIBSupportContext ribSupport = getRibSupport(tableKey);
            final YangInstanceIdentifier tablePath = effectiveTablePath(tableKey);

            // Create an empty table
            LOG.trace("Create Empty table", tablePath);
            ribSupport.createEmptyTableStructure(tx, tablePath);

            processTableChildren(tx, ribSupport.getRibSupport(), tablePath, table.getChildNodes());
        }

        @Override
        public void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes) {
            LOG.trace("Data changed called to effective RIB. Change : {}", changes);

            // we have a lot of transactions created for 'nothing' because a lot of changes
            // are skipped, so ensure we only create one transaction when we really need it
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
            if (tx != null) {
                tx.submit();
            }
        }

        private void changeDataTree(final DOMDataWriteTransaction tx, final YangInstanceIdentifier rootPath,
            final DataTreeCandidateNode root, final DataTreeCandidateNode table) {
            final PathArgument lastArg = table.getIdentifier();
            Verify.verify(lastArg instanceof NodeIdentifierWithPredicates, "Unexpected type %s in path %s", lastArg.getClass(), rootPath);
            final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) lastArg;
            final RIBSupport ribSupport = getRibSupport(tableKey).getRibSupport();
            final ModificationType modificationType = root.getModificationType();
            switch (modificationType) {
            case DELETE:
            case DISAPPEARED:
                final YangInstanceIdentifier effectiveTablePath = effectiveTablePath(tableKey);
                LOG.debug("Delete Effective Table {} modification type {}, ", effectiveTablePath, modificationType);

                // delete the corresponding effective table
                tx.delete(LogicalDatastoreType.OPERATIONAL, effectiveTablePath);

                deleteRoute(adjRibInRouteCounters, adjRibInRouteMap, new TablesKey(ribSupport.getAfi(), ribSupport.getSafi()));
                deleteRoute(effectiveRibInRouteCounters, effectiveRibInRouteMap, new TablesKey(ribSupport.getAfi(), ribSupport.getSafi()));
                break;
            case SUBTREE_MODIFIED:
                modifyTable(tx, tableKey, table);
                break;
            case UNMODIFIED:
                LOG.info("Ignoring spurious notification on {} data {}", rootPath, table);
                break;
            case APPEARED:
            case WRITE:
                writeTable(tx, tableKey, table);
                break;
            default:
                LOG.warn("Ignoring unhandled root {}", root);
                break;
            }
        }

        @Override
        public void close() {
            this.reg.close();
        }
    }

    private final AdjInTracker adjInTracker;
    private final AbstractImportPolicy importPolicy;

    @Deprecated
    static EffectiveRibInWriter create(@Nonnull final DOMDataTreeChangeService service, @Nonnull final DOMTransactionChain chain,
        @Nonnull final YangInstanceIdentifier peerIId, @Nonnull final ImportPolicyPeerTracker importPolicyPeerTracker, @Nonnull final RIBSupportContextRegistry registry, final PeerRole peerRole) {
        return new EffectiveRibInWriter(service, chain, peerIId, importPolicyPeerTracker, registry, peerRole);
    }

    static EffectiveRibInWriter create(@Nonnull final DOMDataTreeChangeService service, @Nonnull final DOMTransactionChain chain,
        @Nonnull final YangInstanceIdentifier peerIId, @Nonnull final ImportPolicyPeerTracker importPolicyPeerTracker, @Nonnull final RIBSupportContextRegistry registry, final PeerRole peerRole,
        @Nonnull final PerTableTypeRouteCounter effectiveRouteCounters, @Nonnull final PerTableTypeRouteCounter adjRibInRouteCounters) {
        return new EffectiveRibInWriter(service, chain, peerIId, importPolicyPeerTracker, registry, peerRole, effectiveRouteCounters, adjRibInRouteCounters);
    }

    @Deprecated
    private EffectiveRibInWriter(final DOMDataTreeChangeService service, final DOMTransactionChain chain, final YangInstanceIdentifier peerIId,
        final ImportPolicyPeerTracker importPolicyPeerTracker, final RIBSupportContextRegistry registry, final PeerRole peerRole) {
        importPolicyPeerTracker.peerRoleChanged(peerIId, peerRole);
        this.importPolicy = importPolicyPeerTracker.policyFor(IdentifierUtils.peerId((NodeIdentifierWithPredicates) peerIId.getLastPathArgument()));
        this.adjInTracker = new AdjInTracker(service, registry, chain, peerIId);
    }

    private EffectiveRibInWriter(final DOMDataTreeChangeService service, final DOMTransactionChain chain, final YangInstanceIdentifier peerIId,
        final ImportPolicyPeerTracker importPolicyPeerTracker, final RIBSupportContextRegistry registry, final PeerRole peerRole,
        @Nonnull final PerTableTypeRouteCounter effectiveRouteCounters, @Nonnull final PerTableTypeRouteCounter adjRibInRouteCounters) {
        importPolicyPeerTracker.peerRoleChanged(peerIId, peerRole);
        this.importPolicy = importPolicyPeerTracker.policyFor(IdentifierUtils.peerId((NodeIdentifierWithPredicates) peerIId.getLastPathArgument()));
        this.adjInTracker = new AdjInTracker(service, registry, chain, peerIId, effectiveRouteCounters, adjRibInRouteCounters);
    }

    @Override
    public void close() {
        this.adjInTracker.close();
    }
}
