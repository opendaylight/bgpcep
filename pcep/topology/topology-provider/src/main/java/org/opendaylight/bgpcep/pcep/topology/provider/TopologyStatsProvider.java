/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
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
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.NoOpObjectRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TopologyStatsProvider implements SessionStateRegistry, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyStatsProvider.class);
    private static final VarHandle NEXT_TIMEOUT;

    static {
        try {
            NEXT_TIMEOUT = MethodHandles.lookup().findVarHandle(TopologyStatsProvider.class, "nextTimeout",
                Timeout.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

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
    private final Map<KeyedInstanceIdentifier<Node, NodeKey>, Reg<?>> statsMap = new HashMap<>();
    private final ExecutorService executor;
    private final long updateIntervalNanos;
    private final DataBroker dataBroker;
    private final Timer timer;

    // Note: null indicates we have been shut down
    private volatile Timeout nextTimeout;
    @GuardedBy("this")
    private TransactionChain transactionChain;

    TopologyStatsProvider(final DataBroker dataBroker, final Timer timer, final int updateIntervalSeconds) {
        this.dataBroker = requireNonNull(dataBroker);
        this.timer = requireNonNull(timer);
        updateIntervalNanos = TimeUnit.SECONDS.toNanos(updateIntervalSeconds);
        checkArgument(updateIntervalNanos > 0, "Invalid update interval %s", updateIntervalNanos);

        executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("odl-pcep-stats-%d")
            .build());

        nextTimeout = timer.newTimeout(this::updateStats, updateIntervalNanos, TimeUnit.NANOSECONDS);
        LOG.info("TopologyStatsProvider updating every {} seconds", updateIntervalSeconds);
    }

    // FIXME: there should be no further tasks, hence this should not be needed
    // FIXME: if it ends up being needed, it needs to be asynchronous
    void shutdown() throws InterruptedException, ExecutionException {
        final var local = (Timeout) NEXT_TIMEOUT.getAndSet(null);
        if (local == null) {
            LOG.debug("TopologyStatsProvider already shut down");
            return;
        }
        if (!local.cancel()) {
            LOG.debug("Failed to cancel timeout");
        }
        lockedShutdown();
    }

    private synchronized void lockedShutdown() throws InterruptedException, ExecutionException {
        LOG.info("Closing TopologyStatsProvider service.");
        executor.shutdownNow().forEach(Runnable::run);

        // Try to get a transaction chain and indicate we are done
        final TransactionChain chain = accessChain();
        transactionChain = null;

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
        if (nextTimeout == null) {
            return null;
        }

        var local = transactionChain;
        if (local == null) {
            // Re-create the chain if we have not been shut down
            transactionChain = local = dataBroker.createMergingTransactionChain(this);
        }
        return local;
    }

    private void updateStats(final Timeout timeout) {
        if (timeout.equals(nextTimeout)) {
            executor.execute(this::updateStats);
        } else {
            LOG.debug("Ignoring unexpected timeout {}", timeout);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private synchronized void updateStats() {
        final TransactionChain chain = accessChain();
        if (chain == null) {
            // Already closed, do not bother
            LOG.debug("Skipping update on shut down");
            return;
        }

        final long now = System.nanoTime();
        final WriteTransaction tx = chain.newWriteOnlyTransaction();
        try {
            for (var entry : statsMap.entrySet()) {
                if (!statsPendingDelete.contains(entry.getKey())) {
                    final var reg = entry.getValue();
                    if (reg.notClosed()) {
                        tx.put(LogicalDatastoreType.OPERATIONAL,
                            entry.getKey().augmentation(PcepTopologyNodeStatsAug.class),
                            new PcepTopologyNodeStatsAugBuilder()
                                .setPcepSessionState(new PcepSessionStateBuilder(reg.getInstance()).build())
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to prepare Tx for PCEP stats update", e);
            tx.cancel();
            schedule(now);
            return;
        }

        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Successfully committed Topology stats update");
                schedule(now);
            }

            @Override
            public void onFailure(final Throwable ex) {
                LOG.error("Failed to commit Topology stats update", ex);
                // Wait a complete cycle
                schedule(System.nanoTime());
            }
        }, MoreExecutors.directExecutor());
    }

    private synchronized void schedule(final long lastNow) {
        if (nextTimeout != null) {
            lockedSchedule(lastNow);
        } else {
            LOG.debug("Skipping schedule on shutdown");
        }
    }

    @Holding("this")
    private void lockedSchedule(final long lastNow) {
        final long now = System.nanoTime();

        // TODO: can we do something smarter?
        long delay = lastNow + updateIntervalNanos;
        while (delay - now < 0) {
            delay += updateIntervalNanos;
        }
        nextTimeout = timer.newTimeout(this::updateStats, lastNow, TimeUnit.NANOSECONDS);
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
    public synchronized <T extends PcepSessionState> ObjectRegistration<T> bind(
            final KeyedInstanceIdentifier<Node, NodeKey> nodeId, final T sessionState) {
        if (nextTimeout == null) {
            LOG.debug("Ignoring bind of Pcep Node {}", nodeId);
            return NoOpObjectRegistration.of(sessionState);
        }

        final var ret = new Reg<>(sessionState, nodeId);
        // FIXME: a replace should never happen, and hence regs are just a Set (which can be concurrent and this method
        //        does not need synchronization
        statsMap.put(nodeId, ret);
        return ret;
    }

    private synchronized void removeRegistration(final @NonNull Reg<?> reg) {
        final var nodeId = reg.nodeId;

        if (!statsMap.remove(nodeId, reg)) {
            // Already replaced by a subsequent bind()
            LOG.debug("Ignoring overridden unbind of Pcep Node {}", nodeId);
            return;
        }

        final TransactionChain chain = accessChain();
        if (chain == null) {
            // Already closed, do not bother
            LOG.debug("Ignoring unbind of Pcep Node {}", nodeId);
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

    private final class Reg<T extends PcepSessionState> extends AbstractObjectRegistration<T> {
        private final @NonNull KeyedInstanceIdentifier<Node, NodeKey> nodeId;

        Reg(final @NonNull T instance, final KeyedInstanceIdentifier<Node, NodeKey> nodeId) {
            super(instance);
            this.nodeId = requireNonNull(nodeId);
        }

        @Override
        protected void removeRegistration() {
            TopologyStatsProvider.this.removeRegistration(this);
        }
    }
}
