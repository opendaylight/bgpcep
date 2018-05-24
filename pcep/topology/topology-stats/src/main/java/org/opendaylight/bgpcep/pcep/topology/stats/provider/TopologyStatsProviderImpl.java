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
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.pcep.topology.spi.stats.TopologySessionStatsRegistry;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev171113.PcepTopologyNodeStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev171113.PcepTopologyNodeStatsAugBuilder;
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
    private final DataBroker dataBroker;
    private final int timeout;
    private BindingTransactionChain transactionChain;
    private ScheduledFuture<?> scheduleTask;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TopologyStatsProviderImpl(@Nonnull final DataBroker dataBroker, final int timeout) {
        this.dataBroker = requireNonNull(dataBroker);
        this.timeout = timeout;
    }

    public synchronized void init() {
        LOG.info("Initializing TopologyStatsProvider service.", this);
        this.transactionChain = this.dataBroker.createTransactionChain(this);
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                updatePcepStats();
            }
        };

        this.scheduleTask = this.scheduler.scheduleAtFixedRate(task, 0, this.timeout, SECONDS);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private synchronized void updatePcepStats() {
        final WriteTransaction tx = TopologyStatsProviderImpl.this.transactionChain.newWriteOnlyTransaction();

        try {
            for (final Map.Entry<KeyedInstanceIdentifier<Node, NodeKey>, PcepSessionState> entry
                    : this.statsMap.entrySet()) {
                final PcepTopologyNodeStatsAug nodeStatsAug = new PcepTopologyNodeStatsAugBuilder()
                        .setPcepSessionState(new PcepSessionStateBuilder(entry.getValue()).build()).build();
                final InstanceIdentifier<PcepTopologyNodeStatsAug> statId =
                        entry.getKey().augmentation(PcepTopologyNodeStatsAug.class);
                tx.put(LogicalDatastoreType.OPERATIONAL, statId, nodeStatsAug);
            }
            tx.commit().addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(CommitInfo result) {
                    LOG.debug("Successfully committed Topology stats update");
                }

                @Override
                public void onFailure(Throwable ex) {
                    LOG.error("Failed to commit Topology stats update", ex);
                }
            }, MoreExecutors.directExecutor());
        } catch (final Exception e) {
            LOG.warn("Failed to prepare Tx for BGP stats update", e);
            tx.cancel();
        }
    }

    @Override
    public synchronized void close() throws Exception {
        if (closed.compareAndSet(false, true)) {
            LOG.info("Closing TopologyStatsProvider service.", this);
            this.scheduleTask.cancel(true);
            final WriteTransaction wTx = this.transactionChain.newWriteOnlyTransaction();
            for (final KeyedInstanceIdentifier<Node, NodeKey> statId : this.statsMap.keySet()) {
                wTx.delete(LogicalDatastoreType.OPERATIONAL, statId);
            }
            wTx.commit().get();
            this.statsMap.clear();
            this.transactionChain.close();
            this.scheduler.shutdown();
        }
    }

    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain<?, ?> chain,
            final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Transaction chain {} failed for tx {}",
                chain, transaction != null ? transaction.getIdentifier() : null, cause);

        if (!closed.get()) {
            transactionChain.close();
            transactionChain = dataBroker.createTransactionChain(this);
        }
    }

    @Override
    public synchronized void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
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
        final WriteTransaction wTx = this.transactionChain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, nodeId);
        try {
            wTx.commit().get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.warn("Failed to remove Pcep Node stats {}.", nodeId.getKey().getNodeId());
        }
    }
}
