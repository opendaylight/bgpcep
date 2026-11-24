/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ATTRIBUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.UPTODATE_NID;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadOperations;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.spi.RIBNormalizedNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer of Adjacency-RIB-In for a single peer. An instance of this object is attached to each {@link BGPPeer} and
 * {@link ApplicationPeer}. This class is NOT thread-safe.
 */
final class AdjRibInWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AdjRibInWriter.class);

    private final @NonNull DOMTransactionChain chain;
    private final @NonNull AdjRibInTables tables;

    @GuardedBy("this")
    private final HashMap<TablesKey, Collection<NodeIdentifierWithPredicates>> staleRoutesRegistry = new HashMap<>();
    @GuardedBy("this")
    private FluentFuture<? extends CommitInfo> submitted;

    @NonNullByDefault
    AdjRibInWriter(final DOMTransactionChain chain, final AdjRibInTables tables) {
        this.chain = requireNonNull(chain);
        this.tables = requireNonNull(tables);
    }

    void markTableUptodate(final @NonNull TablesKey tableTypes) {
        final var table = tables.getTableOf(tableTypes);
        final var tx = chain.newWriteOnlyTransaction();
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

    void updateRoutes(final MpReachNlri nlri,
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes
                  .Attributes attributes) {
        final var key = new TablesKey(nlri.requireAfi(), nlri.requireSafi());
        final var table = tables.tableOf(key);
        if (table == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }

        final var tx = chain.newWriteOnlyTransaction();
        final var routeKeys = table.ribSupport().writeRoutes(tx, table.path(), nlri, attributes);
        final var staleRoutes = staleRoutesRegistry.get(key);
        if (staleRoutes != null) {
            staleRoutes.removeAll(routeKeys);
        }
        LOG.trace("Write routes {}", nlri);
        final var future = tx.commit();
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
        final var key = new TablesKey(nlri.requireAfi(), nlri.requireSafi());
        final var table = tables.tableOf(key);
        if (table == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }

        LOG.trace("Removing routes {}", nlri);
        final var tx = chain.newWriteOnlyTransaction();
        table.ribSupport().deleteRoutes(tx, table.path(), nlri);
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

    // FIXME: rename method and improve semantics around waiting/interruption
    void releaseChain() {
        if (submitted != null) {
            try {
                submitted.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Write routes failed", e);
            }
        }
    }

    private void storeStaleRoutes(final DOMDataTreeReadOperations tx, final Set<TablesKey> gracefulTables) {
        final var latch = new CountDownLatch(gracefulTables.size());

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

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
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
        final var tx = chain.newWriteOnlyTransaction();
        for (var routeKey : routeKeys) {
            tx.delete(LogicalDatastoreType.OPERATIONAL,
                table.ribSupport().ribSupport().routePath(table.path(), routeKey));
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

    @NonNull FluentFuture<? extends CommitInfo> clearTables(final Set<TablesKey> tablesToClear) {
        return tablesToClear == null || tablesToClear.isEmpty() ? CommitInfo.emptyFluentFuture()
            : clearTables(chain.newWriteOnlyTransaction(), tablesToClear);
    }

    private @NonNull FluentFuture<? extends CommitInfo> clearTables(final DOMDataTreeWriteTransaction tx,
            final Set<TablesKey> tablesToClear) {
        for (var tableType : tablesToClear) {
            tx.delete(LogicalDatastoreType.OPERATIONAL, tables.getTableOf(tableType).routesPath().getParent());
        }
        return tx.commit();
    }

    @NonNull FluentFuture<? extends CommitInfo> retainTables(final Set<TablesKey> allTables,
            final Set<TablesKey> retainedTables) {
        final DOMDataTreeWriteTransaction tx;
        if (!retainedTables.isEmpty()) {
            final var rw = chain.newReadWriteTransaction();
            storeStaleRoutes(rw, retainedTables);
            tx = rw;
        } else {
            tx = chain.newWriteOnlyTransaction();
        }

        return clearTables(tx, Sets.difference(allTables, retainedTables));
    }
}
