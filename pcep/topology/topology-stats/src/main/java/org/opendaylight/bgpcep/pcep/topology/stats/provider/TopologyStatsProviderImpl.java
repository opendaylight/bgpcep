/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.stats.provider;

import static java.util.Objects.requireNonNull;

import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.pcep.stats.rev171113.PcepTopologyNodeStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.pcep.stats.rev171113.PcepTopologyNodeStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopologyStatsProviderImpl implements TransactionChainListener, TopologySessionStatsRegistry,
        AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyStatsProviderImpl.class);
    @GuardedBy("this")
    private final Map<KeyedInstanceIdentifier<Node, NodeKey>, PcepSessionState> statsMap = new HashMap<>();
    private final DataBroker dataBroker;
    private final int timeout;
    private BindingTransactionChain transactionChain;
    private ScheduledFuture<?> scheduleTask;

    public TopologyStatsProviderImpl(@Nonnull final DataBroker dataBroker, final int timeout) {
        this.dataBroker = requireNonNull(dataBroker);
        this.timeout = timeout;
    }

    public void init() {
        LOG.info("Initializing TopologyStatsProvider service.", this);
        this.transactionChain = this.dataBroker.createTransactionChain(this);
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                synchronized (TopologyStatsProviderImpl.this) {
                    final WriteTransaction tx = TopologyStatsProviderImpl
                            .this.transactionChain.newWriteOnlyTransaction();

                    TopologyStatsProviderImpl.this.statsMap
                            .forEach((key, value) -> updatePCEPStats(key, value, tx));
                    tx.submit();
                }
            }
        };

        this.scheduleTask = GlobalEventExecutor.INSTANCE.scheduleAtFixedRate(task, 0, this.timeout,
                TimeUnit.SECONDS);
    }

    private synchronized void updatePCEPStats(
            final KeyedInstanceIdentifier<Node, NodeKey> nodeIId,
            final PcepSessionState stats,
            final WriteTransaction tx) {

        final PcepTopologyNodeStatsAug nodeStatsAug = new PcepTopologyNodeStatsAugBuilder()
                .setPcepSessionState(new PcepSessionStateBuilder(stats).build()).build();
        final InstanceIdentifier<PcepTopologyNodeStatsAug> statId =
                nodeIId.augmentation(PcepTopologyNodeStatsAug.class);
        tx.put(LogicalDatastoreType.OPERATIONAL, statId, nodeStatsAug);
    }

    @Override
    public void close() throws Exception {
        LOG.info("Closing TopologyStatsProvider service.", this);
        this.scheduleTask.cancel(true);
        final WriteTransaction wTx = this.transactionChain.newWriteOnlyTransaction();
        this.statsMap.keySet().iterator().forEachRemaining(statId ->
                wTx.delete(LogicalDatastoreType.OPERATIONAL, statId));
        wTx.submit().get();
        this.statsMap.clear();
        this.transactionChain.close();
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        LOG.error("Transaction chain failed {}.", transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
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
    }
}
