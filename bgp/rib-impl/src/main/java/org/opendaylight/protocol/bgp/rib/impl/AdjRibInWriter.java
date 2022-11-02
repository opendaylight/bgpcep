/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBOUT_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ATTRIBUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.UPTODATE_NID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer.RegisterAppPeerListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.PeerRoleUtil;
import org.opendaylight.protocol.bgp.rib.spi.RIBNormalizedNodes;
import org.opendaylight.protocol.bgp.rib.spi.RIBQNames;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer of Adjacency-RIB-In for a single peer. An instance of this object is attached to each {@link BGPPeer} and
 * {@link ApplicationPeer}. This class is NOT thread-safe.
 */
final class AdjRibInWriter {

    private static final Logger LOG = LoggerFactory.getLogger(AdjRibInWriter.class);

    private static final QName PEER_ROLE_QNAME = QName.create(Peer.QNAME, "peer-role").intern();
    private static final NodeIdentifier PEER_ID = NodeIdentifier.create(RIBQNames.PEER_ID_QNAME);
    private static final NodeIdentifier PEER_ROLE = NodeIdentifier.create(PEER_ROLE_QNAME);
    private static final NodeIdentifier PEER_TABLES = NodeIdentifier.create(SupportedTables.QNAME);
    private static final QName SEND_RECEIVE = QName.create(SupportedTables.QNAME, "send-receive").intern();

    // FIXME: is there a utility method to construct this?
    private static final MapNode EMPTY_TABLES = ImmutableNodes.mapNodeBuilder(TABLES_NID).build();
    private static final ContainerNode EMPTY_ADJRIBIN = Builders.containerBuilder()
            .withNodeIdentifier(ADJRIBIN_NID).addChild(EMPTY_TABLES).build();
    private static final ContainerNode EMPTY_EFFRIBIN = Builders.containerBuilder()
            .withNodeIdentifier(EFFRIBIN_NID).addChild(EMPTY_TABLES).build();
    private static final ContainerNode EMPTY_ADJRIBOUT = Builders.containerBuilder()
            .withNodeIdentifier(ADJRIBOUT_NID).addChild(EMPTY_TABLES).build();

    private final Map<TablesKey, TableContext> tables;
    private final YangInstanceIdentifier ribPath;
    private final PeerTransactionChain chain;
    private final PeerRole role;
    @GuardedBy("this")
    private final Map<TablesKey, Collection<NodeIdentifierWithPredicates>> staleRoutesRegistry = new HashMap<>();
    @GuardedBy("this")
    private FluentFuture<? extends CommitInfo> submitted;

    private AdjRibInWriter(final YangInstanceIdentifier ribPath, final PeerTransactionChain chain, final PeerRole role,
            final Map<TablesKey, TableContext> tables) {
        this.ribPath = requireNonNull(ribPath);
        this.chain = requireNonNull(chain);
        this.tables = requireNonNull(tables);
        this.role = requireNonNull(role);
    }

    /**
     * Create a new writer using a transaction chain.
     *
     * @param role                peer's role
     * @param chain               transaction chain  @return A fresh writer instance
     */
    static AdjRibInWriter create(final @NonNull YangInstanceIdentifier ribId, final @NonNull PeerRole role,
            final @NonNull PeerTransactionChain chain) {
        return new AdjRibInWriter(ribId, chain, role, Collections.emptyMap());
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
    AdjRibInWriter transform(final PeerId newPeerId, final YangInstanceIdentifier peerPath,
            final RIBSupportContextRegistry registry,
            final Set<TablesKey> tableTypes, final Map<TablesKey, SendReceive> addPathTablesType) {
        return transform(newPeerId, peerPath, registry, tableTypes, addPathTablesType, null);
    }

    AdjRibInWriter transform(final PeerId newPeerId, final YangInstanceIdentifier peerPath,
            final RIBSupportContextRegistry registry, final Set<TablesKey> tableTypes,
            final Map<TablesKey, SendReceive> addPathTablesType,
            final @Nullable RegisterAppPeerListener registerAppPeerListener) {
        final DOMDataTreeWriteTransaction tx = chain.getDomChain().newWriteOnlyTransaction();

        createEmptyPeerStructure(newPeerId, peerPath, tx);
        final ImmutableMap<TablesKey, TableContext> tb = createNewTableInstances(peerPath, registry, tableTypes,
                addPathTablesType, tx);

        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                if (registerAppPeerListener != null) {
                    LOG.trace("Application Peer Listener registered");
                    registerAppPeerListener.register();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                if (registerAppPeerListener != null) {
                    LOG.error("Failed to create Empty Structure, Application Peer Listener won't be registered",
                            throwable);
                } else {
                    LOG.error("Failed to create Empty Structure", throwable);
                }
            }
        }, MoreExecutors.directExecutor());
        return new AdjRibInWriter(ribPath, chain, role, tb);
    }

    /**
     * Create new table instances, potentially creating their empty entries.
     */
    private static ImmutableMap<TablesKey, TableContext> createNewTableInstances(
            final YangInstanceIdentifier newPeerPath, final RIBSupportContextRegistry registry,
            final Set<TablesKey> tableTypes, final Map<TablesKey, SendReceive> addPathTablesType,
            final DOMDataTreeWriteTransaction tx) {

        final Builder<TablesKey, TableContext> tb = ImmutableMap.builder();
        for (final TablesKey tableKey : tableTypes) {
            final RIBSupportContext rs = registry.getRIBSupportContext(tableKey);
            // TODO: Use returned value once Instance Identifier builder allows for it.
            final NodeIdentifierWithPredicates instanceIdentifierKey = RibSupportUtils.toYangTablesKey(tableKey);
            if (rs == null) {
                LOG.warn("No support for table type {}, skipping it", tableKey);
                continue;
            }
            installAdjRibsOutTables(newPeerPath, rs, instanceIdentifierKey, tableKey,
                    addPathTablesType.get(tableKey), tx);
            installAdjRibInTables(newPeerPath, tableKey, rs, instanceIdentifierKey, tx, tb);
        }
        return tb.build();
    }

    private static void installAdjRibInTables(final YangInstanceIdentifier newPeerPath, final TablesKey tableKey,
            final RIBSupportContext rs, final NodeIdentifierWithPredicates instanceIdentifierKey,
            final DOMDataTreeWriteTransaction tx, final Builder<TablesKey, TableContext> tb) {
        // We will use table keys very often, make sure they are optimized
        final InstanceIdentifierBuilder idb = YangInstanceIdentifier.builder(newPeerPath
                .node(EMPTY_ADJRIBIN.getIdentifier()).node(TABLES_NID)).node(instanceIdentifierKey);

        final TableContext ctx = new TableContext(rs, idb.build());
        ctx.createEmptyTableStructure(tx);

        tx.merge(LogicalDatastoreType.OPERATIONAL, ctx.getTableId().node(ATTRIBUTES_NID).node(UPTODATE_NID),
            RIBNormalizedNodes.ATTRIBUTES_UPTODATE_FALSE);
        LOG.debug("Created table instance {}", ctx.getTableId());
        tb.put(tableKey, ctx);
    }

    private static void installAdjRibsOutTables(final YangInstanceIdentifier newPeerPath, final RIBSupportContext rs,
            final NodeIdentifierWithPredicates instanceIdentifierKey, final TablesKey tableKey,
            final SendReceive sendReceive, final DOMDataTreeWriteTransaction tx) {
        final NodeIdentifierWithPredicates supTablesKey = RibSupportUtils.toYangKey(SupportedTables.QNAME, tableKey);
        final var tt = Builders.mapEntryBuilder().withNodeIdentifier(supTablesKey);
        for (final Entry<QName, Object> e : supTablesKey.entrySet()) {
            tt.withChild(ImmutableNodes.leafNode(e.getKey(), e.getValue()));
        }
        if (sendReceive != null) {
            tt.withChild(ImmutableNodes.leafNode(SEND_RECEIVE, sendReceive.toString().toLowerCase(Locale.ENGLISH)));
        }
        tx.put(LogicalDatastoreType.OPERATIONAL, newPeerPath.node(PEER_TABLES).node(supTablesKey), tt.build());
        rs.createEmptyTableStructure(tx, newPeerPath.node(EMPTY_ADJRIBOUT.getIdentifier())
                .node(TABLES_NID).node(instanceIdentifierKey));
    }

    private void createEmptyPeerStructure(final PeerId newPeerId,
            final YangInstanceIdentifier peerPath, final DOMDataTreeWriteTransaction tx) {
        final NodeIdentifierWithPredicates peerKey = IdentifierUtils.domPeerId(newPeerId);

        tx.put(LogicalDatastoreType.OPERATIONAL, peerPath, peerSkeleton(peerKey, newPeerId.getValue()));
        LOG.debug("New peer {} structure installed.", peerPath);
    }

    @VisibleForTesting
    MapEntryNode peerSkeleton(final NodeIdentifierWithPredicates peerKey, final String peerId) {
        return Builders.mapEntryBuilder()
            .withNodeIdentifier(peerKey)
            .withChild(ImmutableNodes.leafNode(PEER_ID, peerId))
            .withChild(ImmutableNodes.leafNode(PEER_ROLE, PeerRoleUtil.roleForString(role)))
            .withChild(Builders.mapBuilder().withNodeIdentifier(PEER_TABLES).build())
            .withChild(EMPTY_ADJRIBIN)
            .withChild(EMPTY_EFFRIBIN)
            .withChild(EMPTY_ADJRIBOUT)
            .build();
    }

    void markTableUptodate(final TablesKey tableTypes) {
        final DOMDataTreeWriteTransaction tx = chain.getDomChain().newWriteOnlyTransaction();
        final TableContext ctx = tables.get(tableTypes);
        tx.merge(LogicalDatastoreType.OPERATIONAL, ctx.getTableId().node(ATTRIBUTES_NID).node(UPTODATE_NID),
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
        final TablesKey key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final TableContext ctx = tables.get(key);
        if (ctx == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }

        final DOMDataTreeWriteTransaction tx = chain.getDomChain().newWriteOnlyTransaction();
        final Collection<NodeIdentifierWithPredicates> routeKeys = ctx.writeRoutes(tx, nlri, attributes);
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
        final TablesKey key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final TableContext ctx = tables.get(key);
        if (ctx == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }
        LOG.trace("Removing routes {}", nlri);
        final DOMDataTreeWriteTransaction tx = chain.getDomChain().newWriteOnlyTransaction();
        ctx.removeRoutes(tx, nlri);
        final FluentFuture<? extends CommitInfo> future = tx.commit();
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
        final CountDownLatch latch = new CountDownLatch(gracefulTables.size());

        try (DOMDataTreeReadTransaction tx = chain.getDomChain().newReadOnlyTransaction()) {
            for (TablesKey tablesKey : gracefulTables) {
                final TableContext ctx = tables.get(tablesKey);
                if (ctx == null) {
                    LOG.warn("Missing table for address family {}", tablesKey);
                    latch.countDown();
                    continue;
                }

                tx.read(LogicalDatastoreType.OPERATIONAL, ctx.routesPath()).addCallback(
                    new FutureCallback<Optional<NormalizedNode>>() {
                        @Override
                        public void onSuccess(final Optional<NormalizedNode> routesOptional) {
                            try {
                                if (routesOptional.isPresent()) {
                                    synchronized (staleRoutesRegistry) {
                                        final MapNode routesNode = (MapNode) routesOptional.get();
                                        final List<NodeIdentifierWithPredicates> routes = routesNode.body().stream()
                                                .map(MapEntryNode::getIdentifier)
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
        final TableContext ctx = tables.get(tableKey);
        if (ctx == null) {
            LOG.debug("No table for {}, not removing any stale routes", tableKey);
            return;
        }
        final Collection<NodeIdentifierWithPredicates> routeKeys = staleRoutesRegistry.get(tableKey);
        if (routeKeys == null || routeKeys.isEmpty()) {
            LOG.debug("No stale routes present in table {}", tableKey);
            return;
        }
        LOG.trace("Removing routes {}", routeKeys);
        final DOMDataTreeWriteTransaction tx = chain.getDomChain().newWriteOnlyTransaction();
        routeKeys.forEach(routeKey -> {
            tx.delete(LogicalDatastoreType.OPERATIONAL, ctx.routePath(routeKey));
        });
        final FluentFuture<? extends CommitInfo> future = tx.commit();
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

        final DOMDataTreeWriteTransaction wtx = chain.getDomChain().newWriteOnlyTransaction();
        tablesToClear.forEach(tableKey -> {
            final TableContext ctx = tables.get(tableKey);
            wtx.delete(LogicalDatastoreType.OPERATIONAL, ctx.routesPath().getParent());
        });
        return wtx.commit();
    }
}
