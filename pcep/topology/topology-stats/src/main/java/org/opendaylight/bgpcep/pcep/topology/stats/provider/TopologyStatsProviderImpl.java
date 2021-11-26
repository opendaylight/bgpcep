/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.stats.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.Nullable;
import org.kohsuke.MetaInfServices;
import org.opendaylight.bgpcep.pcep.topology.stats.TopologySessionStatsRegistry;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev181109.PcepTopologyNodeStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev181109.PcepTopologyNodeStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@MetaInfServices(value = TopologySessionStatsRegistry.class)
public final class TopologyStatsProviderImpl implements TopologySessionStatsRegistry, TransactionChainListener,
        AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyStatsProviderImpl.class);

    // This tracking looks weird. It essentially tracks when there is a pending delete transaction and skips updates --
    // which is the okay part. The problem is that if the remove operation fails for some reason, we do not retry
    // deletion. The other weird thing is that this is concurrent set because of removals only -- additions are always
    // protected by the lock.
    //
    // FIXME: This was introduced to remedy "instance-2" of BGPCEP-901. I think we should change statsMap so that it
    //        tracks also the intent besides PcepSessionState -- that way we can mark 'we want to remove this' and
    //        retry in face of failing transactions.
    private final Set<KeyedInstanceIdentifier<Node, NodeKey>> statsPendingDelete = ConcurrentHashMap.newKeySet();
    @GuardedBy("this")
    private final Map<KeyedInstanceIdentifier<Node, NodeKey>, PcepSessionState> statsMap = new HashMap<>();
    // Note: null indicates we have been shut down
    @GuardedBy("this")
    private DataBroker dataBroker;
    @GuardedBy("this")
    private TransactionChain transactionChain;
    @GuardedBy("this")
    private final ScheduledFuture<?> scheduleTask;

    @Inject
    public TopologyStatsProviderImpl(final DataBroker dataBroker, final int updateIntervalSeconds) {
        this(dataBroker, updateIntervalSeconds, Executors.newScheduledThreadPool(1));
    }

    public TopologyStatsProviderImpl(final DataBroker dataBroker, final int updateIntervalSeconds,
            final ScheduledExecutorService scheduler) {
        this.dataBroker = requireNonNull(dataBroker);
        LOG.info("Initializing TopologyStatsProvider service.");
        final TimerTask task = new TimerTask() {
            @Override
            @SuppressWarnings("checkstyle:IllegalCatch")
            public void run() {
                synchronized (TopologyStatsProviderImpl.this) {
                    updateStats();
                }
            }
        };
        scheduleTask = scheduler.scheduleAtFixedRate(task, 0, updateIntervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    @PreDestroy
    public void close() throws InterruptedException, ExecutionException {
        if (scheduleTask.cancel(true)) {
            LOG.info("Closing TopologyStatsProvider service.");
            shutdown();
        } else {
            LOG.debug("TopologyStatsProvider already shut down");
        }
    }

    private synchronized void shutdown() throws InterruptedException, ExecutionException {
        // Try to get a transaction chain and indicate we are done
        final TransactionChain chain = accessChain();
        transactionChain = null;
        dataBroker = null;

        if (chain == null) {
            // Belt & suspenders so we do not error out elsewhere
            LOG.warn("Cannot acquire transaction chain, skipping cleanup");
            return;
        }

        // Issue deletes for all registered stats
        final WriteTransaction wTx = chain.newWriteOnlyTransaction();
        for (final KeyedInstanceIdentifier<Node, NodeKey> statId : statsMap.keySet()) {
            wTx.delete(LogicalDatastoreType.OPERATIONAL, statId);
        }
        statsMap.clear();

        // Fire the transaction commit ...
        final FluentFuture<?> future = wTx.commit();
        // ... close the transaction chain ...
        chain.close();
        // ... and wait for transaction commit to complete
        LOG.debug("Awaiting finish of TopologyStatsProvider cleanup");
        future.get();
    }

    @Holding("this")
    private @Nullable TransactionChain accessChain() {
        if (transactionChain == null && dataBroker != null) {
            // Re-create the chain if we have not been shut down
            transactionChain = dataBroker.createMergingTransactionChain(this);
        }
        return transactionChain;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public synchronized void updateStats() {
        final TransactionChain chain = accessChain();
        if (chain == null) {
            // Already closed, do not bother
            return;
        }

        final WriteTransaction tx = chain.newWriteOnlyTransaction();
        try {
            for (Entry<KeyedInstanceIdentifier<Node, NodeKey>, PcepSessionState> entry : statsMap.entrySet()) {
                if (!statsPendingDelete.contains(entry.getKey())) {
                    tx.put(LogicalDatastoreType.OPERATIONAL,
                            entry.getKey().augmentation(PcepTopologyNodeStatsAug.class),
                            new PcepTopologyNodeStatsAugBuilder()
                                    .setPcepSessionState(new PcepSessionStateBuilder(entry.getValue()).build())
                                    .build());
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to prepare Tx for PCEP stats update", e);
            tx.cancel();
            return;
        }

        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Successfully committed Topology stats update");
            }

            @Override
            public void onFailure(final Throwable ex) {
                LOG.error("Failed to commit Topology stats update", ex);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain chain,
            final Transaction transaction, final Throwable cause) {
        LOG.error("Transaction chain {} failed for tx {}",
                chain, transaction != null ? transaction.getIdentifier() : null, cause);
        chain.close();

        // Do not access the transaction chain again, re-recreated it instead
        if (chain == transactionChain) {
            transactionChain = null;
        }
    }

    @Override
    public synchronized void onTransactionChainSuccessful(final TransactionChain chain) {
        LOG.debug("Transaction chain {} successful.", chain);
    }

    @Override
    public synchronized void bind(final KeyedInstanceIdentifier<Node, NodeKey> nodeId,
            final PcepSessionState sessionState) {
        if (dataBroker != null) {
            statsMap.put(nodeId, sessionState);
        } else {
            LOG.debug("Ignoring bind of Pcep Node {}", nodeId);
        }
    }

    @Override
    public synchronized void unbind(final KeyedInstanceIdentifier<Node, NodeKey> nodeId) {
        final TransactionChain chain = accessChain();
        if (chain == null) {
            // Already closed, do not bother
            LOG.debug("Ignoring unbind of Pcep Node {}", nodeId);
            return;
        }

        final PcepSessionState node = statsMap.remove(nodeId);
        if (node == null) {
            LOG.debug("Ignoring duplicate unbind of Pcep Node {}", nodeId);
            return;
        }

        statsPendingDelete.add(nodeId);
        final WriteTransaction wTx = chain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, nodeId);
        wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Successfully removed Pcep Node stats {}.", nodeId.getKey().getNodeId());
                statsPendingDelete.remove(nodeId);
            }

            @Override
            public void onFailure(final Throwable ex) {
                LOG.warn("Failed to remove Pcep Node stats {}.", nodeId.getKey().getNodeId(), ex);
                statsPendingDelete.remove(nodeId);
            }
        }, MoreExecutors.directExecutor());
    }
}
