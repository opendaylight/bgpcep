/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBOUT_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ATTRIBUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.UPTODATE_NID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteOperations;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer.RegisterAppPeerListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RIBNormalizedNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer of Adjacency-RIB-In for a single peer. An instance of this object is attached to each {@link BGPPeer} and
 * {@link ApplicationPeer}. This class is NOT thread-safe.
 */
final class AdjRibInWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AdjRibInWriter.class);

    private static final NodeIdentifier PEER_TABLES = NodeIdentifier.create(SupportedTables.QNAME);
    private static final QName SEND_RECEIVE = QName.create(SupportedTables.QNAME, "send-receive").intern();

    private final @NonNull AdjRibInTables tables;
    private final @NonNull YangInstanceIdentifier ribPath;
    private final @NonNull PeerTransactionChain chain;
    private final @NonNull PeerRole role;

    @GuardedBy("this")
    private final HashMap<TablesKey, Collection<NodeIdentifierWithPredicates>> staleRoutesRegistry = new HashMap<>();
    @GuardedBy("this")
    private FluentFuture<? extends CommitInfo> submitted;

    private AdjRibInWriter(final YangInstanceIdentifier ribPath, final PeerTransactionChain chain, final PeerRole role,
            final AdjRibInTables tables) {
        this.ribPath = requireNonNull(ribPath);
        this.chain = requireNonNull(chain);
        this.tables = requireNonNull(tables);
        this.role = requireNonNull(role);
    }

    /**
     * Create a new writer using a transaction chain.
     *
     * @param role                peer's role
     * @param chain               transaction chain
     * @return A fresh writer instance
     */
    static AdjRibInWriter create(final @NonNull YangInstanceIdentifier ribId, final @NonNull PeerRole role,
            final @NonNull PeerTransactionChain chain) {
        return new AdjRibInWriter(ribId, chain, role, AdjRibInTables.EMPTY);
    }

    /**
     * Transform this writer to a new writer, which is in charge of specified tables.
     * Empty tables are created for new entries and old tables are deleted. Once this
     * method returns, the old instance must not be reasonably used.
     *
     * @param newPeerId         new peer BGP identifier
     * @param peerPath          path of the peer in the datastore
     * @param registry          RIB extension registry
     * @param tableTypes        New tables, must not be null
     * @param addPathTablesType supported add path tables
     * @return New writer
     */
    AdjRibInWriter createStarted(final PeerId newPeerId, final YangInstanceIdentifier peerPath,
            final RIBSupportContextRegistry registry, final Set<TablesKey> tableTypes,
            final Map<TablesKey, SendReceive> addPathTablesType,
            final @Nullable RegisterAppPeerListener registerAppPeerListener,
            final DOMDataTreeWriteOperations tx) {
        createEmptyPeerStructure(newPeerId, peerPath, tx);
        final var newTables = createNewTableInstances(peerPath, registry, tableTypes, addPathTablesType, tx);
        return new AdjRibInWriter(ribPath, chain, role, newTables);
    }

    /**
     * Create new table instances, potentially creating their empty entries.
     */
    private static @NonNull AdjRibInTables createNewTableInstances(
            final YangInstanceIdentifier newPeerPath, final RIBSupportContextRegistry registry,
            final Set<TablesKey> tableTypes, final Map<TablesKey, SendReceive> addPathTablesType,
            final DOMDataTreeWriteOperations tx) {
        final var tables = AdjRibInTables.of(newPeerPath, tableTypes, tableType -> {
            final var supportContext = registry.getRIBSupportContext(tableType);
            if (supportContext == null) {
                LOG.warn("No support for table type {}, skipping it", tableType);
            }
            return supportContext;
        });

        tables.forEach((tableType, table) -> {
            installAdjRibsOutTables(newPeerPath, table.ribSupport(), addPathTablesType.get(tableType), tx);
            installAdjRibInTables(newPeerPath, tableType, table, tx);
        });
        return tables;
    }

    private static void installAdjRibInTables(final YangInstanceIdentifier newPeerPath, final TablesKey tableKey,
            final AdjRibInTable table, final DOMDataTreeWriteOperations tx) {
        table.createEmptyTableStructure(tx);
        tx.merge(LogicalDatastoreType.OPERATIONAL, table.path().node(ATTRIBUTES_NID).node(UPTODATE_NID),
            RIBNormalizedNodes.ATTRIBUTES_UPTODATE_FALSE);
        LOG.debug("Created table instance {}", table.path());
    }

    private static void installAdjRibsOutTables(final YangInstanceIdentifier newPeerPath, final RIBSupportContext rs,
            final SendReceive sendReceive, final DOMDataTreeWriteOperations tx) {
        final var tableKey = rs.ribSupport().tablesKey();
        final var supTablesKey = NodeIdentifierWithPredicates.of(SupportedTables.QNAME, tableKey.asMap());
        final var tt = ImmutableNodes.newMapEntryBuilder().withNodeIdentifier(supTablesKey);
        for (var e : supTablesKey.entrySet()) {
            tt.withChild(ImmutableNodes.leafNode(e.getKey(), e.getValue()));
        }
        if (sendReceive != null) {
            tt.withChild(ImmutableNodes.leafNode(SEND_RECEIVE, sendReceive.getName()));
        }
        tx.put(LogicalDatastoreType.OPERATIONAL, newPeerPath.node(PEER_TABLES).node(supTablesKey), tt.build());
        rs.createEmptyTableStructure(tx, newPeerPath.node(ADJRIBOUT_NID).node(TABLES_NID).node(tableKey));
    }

    private void createEmptyPeerStructure(final PeerId newPeerId,
            final YangInstanceIdentifier peerPath, final DOMDataTreeWriteOperations tx) {
        final var peerKey = IdentifierUtils.domPeerId(newPeerId);
        tx.put(LogicalDatastoreType.OPERATIONAL, peerPath, peerSkeleton(peerKey, newPeerId.getValue()));
        LOG.debug("New peer {} structure installed.", peerPath);
    }

    @NonNullByDefault
    @VisibleForTesting
    MapEntryNode peerSkeleton(final NodeIdentifierWithPredicates peerKey, final String peerId) {
        return AbstractPeer.skeletonOf(peerKey, peerId, role);
    }

    void markTableUptodate(final @NonNull TablesKey tableTypes) {
        final var tx = chain.getDomChain().newWriteOnlyTransaction();
        final var table = tables.getTableOf(tableTypes);
        tx.merge(LogicalDatastoreType.OPERATIONAL, table.path().node(ATTRIBUTES_NID).node(UPTODATE_NID),
            RIBNormalizedNodes.ATTRIBUTES_UPTODATE_TRUE);
        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Write Attributes uptodate, succeed");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Write Attributes uptodate failed", throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    void updateRoutes(final MpReachNlri nlri, final org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes attributes) {
        final var key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final var table = tables.tableOf(key);
        if (table == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }

        final DOMDataTreeWriteTransaction tx = chain.getDomChain().newWriteOnlyTransaction();
        final Collection<NodeIdentifierWithPredicates> routeKeys = table.writeRoutes(tx, nlri, attributes);
        final Collection<NodeIdentifierWithPredicates> staleRoutes = staleRoutesRegistry.get(key);
        if (staleRoutes != null) {
            staleRoutes.removeAll(routeKeys);
        }
        LOG.trace("Write routes {}", nlri);
        final FluentFuture<? extends CommitInfo> future = tx.commit();
        submitted = future;
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Write routes {}, succeed", nlri);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Write routes failed", throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    void removeRoutes(final MpUnreachNlri nlri) {
        final var key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final var table = tables.tableOf(key);
        if (table == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }
        LOG.trace("Removing routes {}", nlri);
        final var tx = chain.getDomChain().newWriteOnlyTransaction();
        table.removeRoutes(tx, nlri);
        final var future = tx.commit();
        submitted = future;
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Removing routes {}, succeed", nlri);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Removing routes failed", throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    void releaseChain() {
        if (submitted != null) {
            try {
                submitted.get();
            } catch (final InterruptedException | ExecutionException throwable) {
                LOG.error("Write routes failed", throwable);
            }
        }
    }

    void storeStaleRoutes(final Set<TablesKey> gracefulTables) {
        final var latch = new CountDownLatch(gracefulTables.size());

        try (var tx = chain.getDomChain().newReadOnlyTransaction()) {
            for (var tablesKey : gracefulTables) {
                final var table = tables.tableOf(tablesKey);
                if (table == null) {
                    LOG.warn("Missing table for address family {}", tablesKey);
                    latch.countDown();
                    continue;
                }

                tx.read(LogicalDatastoreType.OPERATIONAL, table.routesPath()).addCallback(
                    new FutureCallback<Optional<NormalizedNode>>() {
                        @Override
                        public void onSuccess(final Optional<NormalizedNode> routesOptional) {
                            try {
                                if (routesOptional.isPresent()) {
                                    synchronized (staleRoutesRegistry) {
                                        final var routesNode = (MapNode) routesOptional.orElseThrow();
                                        final var routes = routesNode.body().stream()
                                                .map(MapEntryNode::name)
                                                .collect(Collectors.toList());
                                        if (!routes.isEmpty()) {
                                            staleRoutesRegistry.put(tablesKey, routes);
                                        }
                                    }
                                }
                            } finally {
                                latch.countDown();
                            }
                        }

                        @Override
                        public void onFailure(final Throwable throwable) {
                            LOG.warn("Failed to store stale routes for table {}", tablesKey, throwable);
                            latch.countDown();
                        }
                    }, MoreExecutors.directExecutor());
            }
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting to store stale routes with {} tasks of {} to finish", latch.getCount(),
                gracefulTables, e);
        }
    }

    void removeStaleRoutes(final TablesKey tableKey) {
        final var table = tables.tableOf(tableKey);
        if (table == null) {
            LOG.debug("No table for {}, not removing any stale routes", tableKey);
            return;
        }
        final var routeKeys = staleRoutesRegistry.get(tableKey);
        if (routeKeys == null || routeKeys.isEmpty()) {
            LOG.debug("No stale routes present in table {}", tableKey);
            return;
        }
        LOG.trace("Removing routes {}", routeKeys);
        final var tx = chain.getDomChain().newWriteOnlyTransaction();
        for (var routeKey : routeKeys) {
            tx.delete(LogicalDatastoreType.OPERATIONAL, table.routePath(routeKey));
        }

        final var future = tx.commit();
        submitted = future;
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Removing routes {}, succeed", routeKeys);
                synchronized (staleRoutesRegistry) {
                    staleRoutesRegistry.remove(tableKey);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Removing routes {}, failed", routeKeys, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    FluentFuture<? extends CommitInfo> clearTables(final Set<TablesKey> tablesToClear) {
        if (tablesToClear == null || tablesToClear.isEmpty()) {
            return CommitInfo.emptyFluentFuture();
        }

        final var wtx = chain.getDomChain().newWriteOnlyTransaction();
        for (var tableType : tablesToClear) {
            wtx.delete(LogicalDatastoreType.OPERATIONAL, tables.getTableOf(tableType).routesPath().getParent());
        }
        return wtx.commit();
    }
}
