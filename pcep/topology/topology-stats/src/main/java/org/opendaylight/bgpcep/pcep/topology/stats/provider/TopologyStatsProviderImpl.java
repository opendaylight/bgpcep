/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.stats.provider;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.pcep.topology.spi.stats.TopologySessionStatsRegistry;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopologyStatsProviderImpl implements TransactionChainListener,
        TopologySessionStatsRegistry, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyStatsProviderImpl.class);
    @GuardedBy("this")
    private final Map<KeyedInstanceIdentifier<Node, NodeKey>, PcepSessionState> statsMap = new HashMap<>();
    private final Set<KeyedInstanceIdentifier<Node, NodeKey>> statsPendingDelete = ConcurrentHashMap.newKeySet();
    private final DataBroker dataBroker;
    private final int timeout;
    private TransactionChain transactionChain;
    private ScheduledFuture<?> scheduleTask;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TopologyStatsProviderImpl(final @NonNull DataBroker dataBroker, final int timeout) {
        this.dataBroker = requireNonNull(dataBroker);
        this.timeout = timeout;
    }

    public synchronized void init() {
        LOG.info("Initializing TopologyStatsProvider service.");
        this.transactionChain = this.dataBroker.createMergingTransactionChain(this);
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                updatePcepStats();
            }
        };

        this.scheduleTask = this.scheduler.scheduleAtFixedRate(task, 0, this.timeout, SECONDS);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void updatePcepStats() {
        synchronized (this.transactionChain) {
            WriteTransaction tx = null;

            try {
                tx = TopologyStatsProviderImpl.this.transactionChain.newWriteOnlyTransaction();
                for (final Map.Entry<KeyedInstanceIdentifier<Node, NodeKey>, PcepSessionState> entry
                        : this.statsMap.entrySet()) {
                    if (this.statsPendingDelete.contains(entry.getKey())) {
                        continue;
                    }
                    final PcepTopologyNodeStatsAug nodeStatsAug = new PcepTopologyNodeStatsAugBuilder()
                            .setPcepSessionState(new PcepSessionStateBuilder(entry.getValue()).build()).build();
                    final InstanceIdentifier<PcepTopologyNodeStatsAug> statId =
                            entry.getKey().augmentation(PcepTopologyNodeStatsAug.class);
                    tx.put(LogicalDatastoreType.OPERATIONAL, statId, nodeStatsAug);
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
            } catch (final Exception e) {
                if (tx != null) {
                    LOG.warn("Failed to prepare Tx {} for PCEP stats update", tx.getIdentifier(), e);
                    tx.cancel();
                    this.transactionChain.close();
                    recreateTxChain();
                }
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private synchronized void recreateTxChain() {
        try {
            if (!closed.get()) {
                transactionChain = dataBroker.createMergingTransactionChain(this);
            }
        } catch (final Exception e) {
            LOG.error("Failed to recreate transaction chain {}", transactionChain, e);
        }
    }

    @Override
    public synchronized void close() throws Exception {
        if (closed.compareAndSet(false, true)) {
            LOG.info("Closing TopologyStatsProvider service.");
            this.scheduleTask.cancel(true);
            this.transactionChain.close();
            final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
            for (final KeyedInstanceIdentifier<Node, NodeKey> statId : this.statsMap.keySet()) {
                wTx.delete(LogicalDatastoreType.OPERATIONAL, statId);
            }
            wTx.commit().get();
            this.statsMap.clear();
            this.scheduler.shutdown();
        }
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain chain,
            final Transaction transaction, final Throwable cause) {
        LOG.error("Transaction chain {} failed for tx {}",
                chain, transaction != null ? transaction.getIdentifier() : null, cause);
        chain.close();
        if (chain == this.transactionChain) {
            recreateTxChain();
        }
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain chain) {
        LOG.debug("Transaction chain {} successful.", chain);
    }

    @Override
    public void bind(final KeyedInstanceIdentifier<Node, NodeKey> nodeId,
            final PcepSessionState sessionState) {
        LOG.info("Bind: {}", nodeId.getKey().getNodeId());
        synchronized (this.transactionChain) {
            this.statsMap.put(nodeId, sessionState);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public void unbind(final KeyedInstanceIdentifier<Node, NodeKey> nodeId) {
        synchronized (this.transactionChain) {
            LOG.info("Unbind: {}", nodeId.getKey().getNodeId());
            this.statsMap.remove(nodeId);
            this.statsPendingDelete.add(nodeId);
            WriteTransaction tx = null;
            try {
                tx = this.transactionChain.newWriteOnlyTransaction();
                tx.delete(LogicalDatastoreType.OPERATIONAL, nodeId);
                tx.commit().addCallback(new FutureCallback<CommitInfo>() {
                    @Override
                    public void onSuccess(final CommitInfo result) {
                        LOG.info("Successfully removed Pcep Node stats {}.", nodeId.getKey().getNodeId());
                        TopologyStatsProviderImpl.this.statsPendingDelete.remove(nodeId);
                    }

                    @Override
                    public void onFailure(final Throwable ex) {
                        LOG.warn("Failed to remove Pcep Node stats {}.", nodeId.getKey().getNodeId(), ex);
                        TopologyStatsProviderImpl.this.statsPendingDelete.remove(nodeId);
                    }
                }, MoreExecutors.directExecutor());
            } catch (final Exception e) {
                if (tx != null) {
                    LOG.warn("Failed to prepare Tx {} for Pcep Node stats delete for node {}.", tx.getIdentifier(),
                            nodeId.getKey().getNodeId(), e);
                    tx.cancel();
                    this.transactionChain.close();
                    recreateTxChain();
                }
            }
        }
    }
}
