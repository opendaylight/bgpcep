/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.data.change.counter;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.DataChangeCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.Counter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.CounterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.CounterKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TopologyDataChangeCounter implements ClusteredDataTreeChangeListener<Topology>,
        TransactionChainListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataChangeCounter.class);

    private final DataBroker dataBroker;
    private final String counterId;
    private final InstanceIdentifier<Counter> counterInstanceId;
    private final LongAdder count = new LongAdder();
    private final ListenerRegistration<TopologyDataChangeCounter> registration;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private BindingTransactionChain transactionChain;

    TopologyDataChangeCounter(final DataBroker dataBroker, final String counterId, final String topologyName) {
        this.dataBroker = dataBroker;
        this.transactionChain = this.dataBroker.createTransactionChain(this);
        this.counterId = counterId;
        this.counterInstanceId = InstanceIdentifier.builder(DataChangeCounter.class)
                .child(Counter.class, new CounterKey(this.counterId)).build();
        putCount(this.count.longValue());
        final InstanceIdentifier<Topology> topoIId = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(topologyName))).build();
        this.registration = this.dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, topoIId), this);
        LOG.debug("Data change counter {} initiated", this.counterId);
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Topology>> changes) {
        final long inc = this.count.sum();
        LOG.debug("Data change #{} for counter {}", inc, this.counterId);
        putCount(inc);
    }

    @Override
    public synchronized void close() {
        this.registration.close();
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, this.counterInstanceId);
        try {
            wTx.submit().checkedGet();
        } catch (final TransactionCommitFailedException except) {
            LOG.warn("Error on remove data change counter {}", this.counterId, except);
        }
        this.transactionChain.close();
        LOG.debug("Data change counter {} removed", this.counterId);
    }

    private void putCount(final long totalCount) {
        final WriteTransaction wTx = this.transactionChain.newWriteOnlyTransaction();
        final Counter counter = new CounterBuilder().setId(this.counterId).setCount(totalCount).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, this.counterInstanceId, counter);
        wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Data change count update stored");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed to store Data change count");
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain<?, ?> chain,
            final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.warn("Transaction chain failure. Transaction: {}", transaction, cause);
        if (!closed.get()) {
            this.transactionChain.close();
            this.transactionChain = dataBroker.createTransactionChain(this);
        }
    }

    @Override
    public synchronized void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain successful. {}", chain);
    }
}
