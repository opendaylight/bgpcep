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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.opendaylight.bgpcep.pcep.topology.spi.stats.TopologySessionStatsRegistry;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteOperations;
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

public final class TopologyStatsProviderImpl extends TimerTask
        implements TransactionChainListener, TopologySessionStatsRegistry, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyStatsProviderImpl.class);

    private final Set<KeyedInstanceIdentifier<Node, NodeKey>> statsPendingDelete = ConcurrentHashMap.newKeySet();
    @GuardedBy("this")
    private final Map<KeyedInstanceIdentifier<Node, NodeKey>, PcepSessionState> statsMap = new HashMap<>();
    private final DataBroker dataBroker;
    private final Timer timer;

    @GuardedBy("this")
    private TransactionChain transactionChain;

    public TopologyStatsProviderImpl(final DataBroker dataBroker, final int updateIntervalSeconds) {
        this.dataBroker = requireNonNull(dataBroker);
        LOG.info("Initializing TopologyStatsProvider service.");
        transactionChain = this.dataBroker.createMergingTransactionChain(this);
        timer = new Timer("pcep-topology-stats-timer", true);
        timer.scheduleAtFixedRate(this, 0, TimeUnit.SECONDS.toMillis(updateIntervalSeconds));
    }

    @Override
    public synchronized void close() throws InterruptedException, ExecutionException {
        // Look at the transaction chain ...
        // Take ownership of the chain and null it out -- indicating we are finished to all async others...
        final TransactionChain chain = transactionChain;
        if (chain == null) {
            // ... it's null, we have already been called;
            return;
        }

        // ... it's non-null, hence we have not been shutdown yet.
        LOG.info("Closing TopologyStatsProvider service.");
        transactionChain = null;

        // Cancel the timer, by extension cancelling us as a timer task
        timer.cancel();

        // Issue deletes for all registered stats
        final WriteTransaction wTx = chain.newWriteOnlyTransaction();
        for (final KeyedInstanceIdentifier<Node, NodeKey> statId : statsMap.keySet()) {
            wTx.delete(LogicalDatastoreType.OPERATIONAL, statId);
        }
        statsMap.clear();

        // Fire the transaction commit ...
        final FluentFuture<? > future = wTx.commit();
        // ... close the transaction chain ...
        chain.close();
        // ... and wait for transaction commit to complete
        future.get();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public synchronized void run() {
        if (transactionChain == null) {
            // Already closed, do not bother
            return;
        }

        final WriteTransaction tx = transactionChain.newWriteOnlyTransaction();
        try {
            updateStatistics(tx);
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

    @Holding("this")
    private void updateStatistics(final WriteOperations tx) {
        for (Entry<KeyedInstanceIdentifier<Node, NodeKey>, PcepSessionState> entry : statsMap.entrySet()) {
            if (!statsPendingDelete.contains(entry.getKey())) {
                tx.put(LogicalDatastoreType.OPERATIONAL, entry.getKey().augmentation(PcepTopologyNodeStatsAug.class),
                    new PcepTopologyNodeStatsAugBuilder()
                        .setPcepSessionState(new PcepSessionStateBuilder(entry.getValue()).build())
                        .build());
            }
        }
    }


    // FIXME: audit this




    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain chain,
            final Transaction transaction, final Throwable cause) {
        LOG.error("Transaction chain {} failed for tx {}",
                chain, transaction != null ? transaction.getIdentifier() : null, cause);

        if (!closed.get()) {
            transactionChain.close();
            transactionChain = dataBroker.createMergingTransactionChain(this);
        }
    }

    @Override
    public synchronized void onTransactionChainSuccessful(final TransactionChain chain) {
        LOG.debug("Transaction chain {} successful.", chain);
    }

    @Override
    public synchronized void bind(final KeyedInstanceIdentifier<Node, NodeKey> nodeId,
            final PcepSessionState sessionState) {
        this.statsMap.put(nodeId, sessionState);
    }

    @Override
    public synchronized void unbind(final KeyedInstanceIdentifier<Node, NodeKey> nodeId) {
        this.statsMap.remove(nodeId);
        this.statsPendingDelete.add(nodeId);
        final WriteTransaction wTx = this.transactionChain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, nodeId);
        wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Successfully removed Pcep Node stats {}.", nodeId.getKey().getNodeId());
                TopologyStatsProviderImpl.this.statsPendingDelete.remove(nodeId);
            }

            @Override
            public void onFailure(final Throwable ex) {
                LOG.warn("Failed to remove Pcep Node stats {}.", nodeId.getKey().getNodeId(), ex);
                TopologyStatsProviderImpl.this.statsPendingDelete.remove(nodeId);
            }
        }, MoreExecutors.directExecutor());
    }
}
