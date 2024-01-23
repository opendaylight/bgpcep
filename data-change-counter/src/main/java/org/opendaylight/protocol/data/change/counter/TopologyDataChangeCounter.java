/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.data.change.counter;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.DataChangeCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.Counter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.CounterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.CounterKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TopologyDataChangeCounter
        implements DataTreeChangeListener<Topology>, FutureCallback<Empty>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataChangeCounter.class);

    private final DataBroker dataBroker;
    private final String counterId;
    private final InstanceIdentifier<Counter> counterInstanceId;
    private final LongAdder count = new LongAdder();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Registration registration;

    private TransactionChain transactionChain;

    TopologyDataChangeCounter(final DataBroker dataBroker, final String counterId, final String topologyName) {
        this.dataBroker = requireNonNull(dataBroker);
        transactionChain = dataBroker.createMergingTransactionChain();
        this.counterId = counterId;
        counterInstanceId = InstanceIdentifier.builder(DataChangeCounter.class)
                .child(Counter.class, new CounterKey(this.counterId)).build();
        putCount(count.longValue());
        final InstanceIdentifier<Topology> topoIId = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(topologyName))).build();
        registration = dataBroker.registerTreeChangeListener(
            DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, topoIId), this);
        LOG.debug("Data change counter {} initiated", this.counterId);
        transactionChain.addCallback(this);
    }

    @Override
    public synchronized void onDataTreeChanged(final List<DataTreeModification<Topology>> changes) {
        count.increment();
        final long inc = count.sum();
        LOG.debug("Data change #{} for counter {}", inc, counterId);
        putCount(inc);
    }

    @Override
    public synchronized void close() {
        registration.close();
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, counterInstanceId);
        try {
            wTx.commit().get();
        } catch (final ExecutionException | InterruptedException except) {
            LOG.warn("Error on remove data change counter {}", counterId, except);
        }
        transactionChain.close();
        LOG.debug("Data change counter {} removed", counterId);
    }

    private void putCount(final long totalCount) {
        final WriteTransaction wTx = transactionChain.newWriteOnlyTransaction();
        final Counter counter = new CounterBuilder().setId(counterId).setCount(Uint32.valueOf(totalCount)).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, counterInstanceId, counter);
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
    public synchronized void onFailure(final Throwable cause) {
        LOG.warn("Transaction chain failure", cause);
        if (!closed.get()) {
            transactionChain.close();
            transactionChain = dataBroker.createMergingTransactionChain();
            transactionChain.addCallback(this);
        }
    }

    @Override
    public synchronized void onSuccess(final Empty result) {
        LOG.debug("Transaction chain successful");
    }
}
