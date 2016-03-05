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
import com.google.common.primitives.UnsignedInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.CacheDisconnectedPeers;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.bgp.rib.spi.RouteUpdateKey;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.policy.AbstractPeerRoleTracker;
import org.opendaylight.protocol.bgp.rib.spi.policy.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.policy.PeerExportGroup;
import org.opendaylight.protocol.bgp.rib.spi.policy.PolicyDatabase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class LocRibWriter implements AutoCloseable, DOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(LocRibWriter.class);

    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_TRUE = ImmutableNodes.leafNode(QName.create(Attributes.QNAME, "uptodate"), Boolean.TRUE);
    private static final NodeIdentifier ROUTES_IDENTIFIER = new NodeIdentifier(Routes.QNAME);
    private static final NodeIdentifier EFFRIBIN_NID = new NodeIdentifier(EffectiveRibIn.QNAME);
    private static final NodeIdentifier TABLES_NID = new NodeIdentifier(Tables.QNAME);

    private final Map<PathArgument, RouteEntry> routeEntries = new HashMap<>();
    private final YangInstanceIdentifier locRibTarget;
    private final DOMTransactionChain chain;
    private final ExportPolicyPeerTracker peerPolicyTracker;
    private final NodeIdentifier attributesIdentifier;
    private final Long ourAs;
    private final RIBSupport ribSupport;
    private final NodeIdentifierWithPredicates tableKey;
    private final TablesKey localTablesKey;
    private final ListenerRegistration<LocRibWriter> reg;
    private final CacheDisconnectedPeers cacheDisconnectedPeers;
    private final PathSelectionMode pathSelectionStrategy;

    private LocRibWriter(final RIBSupportContextRegistry registry, final DOMTransactionChain chain, final YangInstanceIdentifier target, final Long ourAs,
        final DOMDataTreeChangeService service, final PolicyDatabase pd, final TablesKey tablesKey, final CacheDisconnectedPeers cacheDisconnectedPeers,
        @Nonnull final PathSelectionMode pathSelectionStrategy) {
        this.chain = Preconditions.checkNotNull(chain);
        this.tableKey = RibSupportUtils.toYangTablesKey(tablesKey);
        this.localTablesKey = tablesKey;
        this.locRibTarget = YangInstanceIdentifier.create(target.node(LocRib.QNAME).node(Tables.QNAME).node(this.tableKey).getPathArguments());
        this.ourAs = Preconditions.checkNotNull(ourAs);
        this.ribSupport = registry.getRIBSupportContext(tablesKey).getRibSupport();
        this.attributesIdentifier = this.ribSupport.routeAttributesIdentifier();
        this.peerPolicyTracker = new ExportPolicyPeerTracker(pd);
        this.cacheDisconnectedPeers = cacheDisconnectedPeers;
        this.pathSelectionStrategy = pathSelectionStrategy;

        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL, this.locRibTarget.node(Routes.QNAME), this.ribSupport.emptyRoutes());
        tx.merge(LogicalDatastoreType.OPERATIONAL, this.locRibTarget.node(Attributes.QNAME).node(ATTRIBUTES_UPTODATE_TRUE.getNodeType()), ATTRIBUTES_UPTODATE_TRUE);
        tx.submit();

        final YangInstanceIdentifier tableId = target.node(Peer.QNAME).node(Peer.QNAME);

        this.reg = service.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, tableId), this);
    }

    public static LocRibWriter create(@Nonnull final RIBSupportContextRegistry registry, @Nonnull final TablesKey tablesKey, @Nonnull final DOMTransactionChain chain,
        @Nonnull final YangInstanceIdentifier target, @Nonnull final AsNumber ourAs, @Nonnull final DOMDataTreeChangeService service, @Nonnull final PolicyDatabase pd,
        final CacheDisconnectedPeers cacheDisconnectedPeers, @Nonnull final PathSelectionMode pathSelectionStrategy) {
        return new LocRibWriter(registry, chain, target, ourAs.getValue(), service, pd, tablesKey, cacheDisconnectedPeers, pathSelectionStrategy);
    }

    @Override
    public void close() {
        this.reg.close();
        // FIXME: wait for the chain to close? unfortunately RIBImpl is the listener, so that may require some work
        this.chain.close();
    }

    @Nonnull
    private RouteEntry createEntry(final PathArgument routeId) {
        final YangInstanceIdentifier writePath = this.ribSupport.routePath(this.locRibTarget.node(ROUTES_IDENTIFIER), routeId);
        final RouteEntry ret = this.pathSelectionStrategy.createRouteEntry(writePath, this.ribSupport, this.peerPolicyTracker,
            this.localTablesKey, this.cacheDisconnectedPeers);
        this.routeEntries.put(routeId, ret);
        LOG.trace("Created new entry for {}", routeId);
        return ret;
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        LOG.trace("Received data change {} to LocRib {}", changes, this);

        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        try {
            /*
             * We use two-stage processing here in hopes that we avoid duplicate
             * calculations when multiple peers have changed a particular entry.
             */
            final Map<RouteUpdateKey, RouteEntry> toUpdate = update(tx, changes);

            // Now walk all updated entries
            walkThrough(tx, toUpdate);
        } catch (final Exception e) {
            LOG.error("Failed to completely propagate updates {}, state is undefined", changes, e);
        } finally {
            tx.submit();
        }
    }

    private Map<RouteUpdateKey, RouteEntry> update(final DOMDataWriteTransaction tx, final Collection<DataTreeCandidate> changes) {
        final Map<RouteUpdateKey, RouteEntry> ret = new HashMap<>();

        for (final DataTreeCandidate tc : changes) {
            final YangInstanceIdentifier rootPath = tc.getRootPath();
            final DataTreeCandidateNode rootNode = tc.getRootNode();
            final NodeIdentifierWithPredicates peerKey = IdentifierUtils.peerKey(rootPath);
            final PeerId peerId = IdentifierUtils.peerId(peerKey);
            filterOutPeerRole(peerId, rootNode, rootPath);
            filterOutChangesToSupportedTables(peerId, rootNode);
            filterOutAnyChangeOutsideEffRibsIn(peerId, rootNode, ret, rootPath, tx);
        }

        return ret;
    }

    private void filterOutAnyChangeOutsideEffRibsIn(final PeerId peerId, final DataTreeCandidateNode rootNode,
        final Map<RouteUpdateKey, RouteEntry> ret, final YangInstanceIdentifier rootPath, final DOMDataWriteTransaction tx) {
        final DataTreeCandidateNode ribIn = rootNode.getModifiedChild(EFFRIBIN_NID);
        if (ribIn == null) {
            LOG.trace("Skipping change {}", rootNode.getIdentifier());
            return;
        }
        final DataTreeCandidateNode table = ribIn.getModifiedChild(TABLES_NID).getModifiedChild(this.tableKey);
        if (table == null) {
            LOG.trace("Skipping change {}", rootNode.getIdentifier());
            return;
        }
        initializeTableWithExistenRoutes(table, peerId, rootPath, tx);
        updateNodes(table, peerId, tx, ret);
    }

    private void filterOutChangesToSupportedTables(final PeerId peerIdOfNewPeer, final DataTreeCandidateNode rootNode) {
        final DataTreeCandidateNode tablesChange = rootNode.getModifiedChild(AbstractPeerRoleTracker.PEER_TABLES);
        if (tablesChange != null) {
            final NodeIdentifierWithPredicates supTablesKey = RibSupportUtils.toYangKey(SupportedTables.QNAME, this.localTablesKey);
            final DataTreeCandidateNode containsLocalKeyTable = tablesChange.getModifiedChild(supTablesKey);
            if (containsLocalKeyTable != null) {
                this.peerPolicyTracker.onTablesChanged(peerIdOfNewPeer, containsLocalKeyTable);
            }
        }
    }

    private void initializeTableWithExistenRoutes(final DataTreeCandidateNode table, final PeerId peerIdOfNewPeer, final YangInstanceIdentifier rootPath,
        final DOMDataWriteTransaction tx) {
        if (!table.getDataBefore().isPresent() && this.peerPolicyTracker.isTableSupported(peerIdOfNewPeer, this.localTablesKey)) {
            LOG.debug("Peer {} table has been created, inserting existent routes", peerIdOfNewPeer);
            final PeerRole newPeerRole = this.peerPolicyTracker.getRole(IdentifierUtils.peerPath(rootPath));
            final PeerExportGroup peerGroup = this.peerPolicyTracker.getPeerGroup(newPeerRole);
            this.routeEntries.entrySet().forEach(entry -> entry.getValue().writeRoute(peerIdOfNewPeer, entry.getKey(), rootPath, peerGroup, tx));
        }
    }

    private void filterOutPeerRole(final PeerId peerId, final DataTreeCandidateNode rootNode, final YangInstanceIdentifier rootPath) {
        final DataTreeCandidateNode roleChange = rootNode.getModifiedChild(AbstractPeerRoleTracker.PEER_ROLE_NID);
        if (roleChange != null) {
            if (!rootNode.getModificationType().equals(ModificationType.DELETE)) {
                this.cacheDisconnectedPeers.reconnected(peerId);
            }
            this.peerPolicyTracker.onDataTreeChanged(roleChange, IdentifierUtils.peerPath(rootPath));
        }
    }

    private void updateNodes(final DataTreeCandidateNode table, final PeerId peerId, final DOMDataWriteTransaction tx,
        final Map<RouteUpdateKey, RouteEntry> routes) {
        for (final DataTreeCandidateNode child : table.getChildNodes()) {
            LOG.debug("Modification type {}", child.getModificationType());
            if ((Attributes.QNAME).equals(child.getIdentifier().getNodeType())) {
                if (child.getDataAfter().isPresent()) {
                    // putting uptodate attribute in
                    LOG.trace("Uptodate found for {}", child.getDataAfter());
                    tx.put(LogicalDatastoreType.OPERATIONAL, this.locRibTarget.node(child.getIdentifier()), child.getDataAfter().get());
                }
                continue;
            }
            updateRoutesEntries(child, peerId, routes);
        }
    }

    private void updateRoutesEntries(final DataTreeCandidateNode child, final PeerId peerId, final Map<RouteUpdateKey, RouteEntry> routes) {
        final UnsignedInteger routerId = RouterIds.routerIdForPeerId(peerId);
        final Collection<DataTreeCandidateNode> modifiedRoutes = this.ribSupport.changedRoutes(child);
        for (final DataTreeCandidateNode route : modifiedRoutes) {
            final PathArgument routeId = route.getIdentifier();
            RouteEntry entry = this.routeEntries.get(routeId);
            final Optional<NormalizedNode<?, ?>> maybeData = route.getDataAfter();
            if (maybeData.isPresent()) {
                if (entry == null) {
                    entry = createEntry(routeId);
                }
                entry.addRoute(routerId, this.attributesIdentifier, maybeData.get());
            } else if (entry != null && entry.removeRoute(routerId)) {
                this.routeEntries.remove(routeId);
                LOG.trace("Removed route from {}", routerId);
            }
            final RouteUpdateKey routeUpdateKey = new RouteUpdateKey(peerId, routeId);
            LOG.debug("Updated route {} entry {}", routeId, entry);
            routes.put(routeUpdateKey, entry);
        }
    }

    private void walkThrough(final DOMDataWriteTransaction tx, final Map<RouteUpdateKey, RouteEntry> toUpdate) {
        for (final Map.Entry<RouteUpdateKey, RouteEntry> e : toUpdate.entrySet()) {
            LOG.trace("Walking through {}", e);
            final RouteEntry entry = e.getValue();

            if (!entry.selectBest(this.ourAs)) {
                LOG.trace("Best path has not changed, continuing");
                continue;
            }
            entry.updateRoute(tx, e.getKey());
        }
    }
}
