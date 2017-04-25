/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.data.change.counter;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

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

public class TopologyDataChangeCounter implements ClusteredDataTreeChangeListener<Topology>, TransactionChainListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataChangeCounter.class);

    private final DataBroker dataBroker;
    private final String counterId;
    private final InstanceIdentifier<Counter> counterInstanceId;
    private final BindingTransactionChain chain;
    private final AtomicLong count;
    private ListenerRegistration<TopologyDataChangeCounter> registration;
    private final InstanceIdentifier<Topology> topoIId;

    public TopologyDataChangeCounter(final DataBroker dataBroker, final String counterId, final String topologyName) {
        this.dataBroker = dataBroker;
        this.chain = this.dataBroker.createTransactionChain(this);
        this.counterId = counterId;
        this.counterInstanceId = InstanceIdentifier.builder(DataChangeCounter.class)
            .child(Counter.class, new CounterKey(this.counterId)).build();
        this.count = new AtomicLong(0);
        putCount(this.count.get());
        this.topoIId = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topologyName))).build();
    }

    public void register() {
        this.registration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, topoIId), this);
        LOG.debug("Data change counter {} initiated", this.counterId);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Topology>> changes) {
        putCount(this.count.incrementAndGet());
        LOG.debug("Data change #{} for counter {}", this.count.get(), this.counterId);
    }

    @Override
    public void close() {
        if (this.registration != null) {
            this.registration.close();
        }
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, this.counterInstanceId);
        try {
            wTx.submit().checkedGet();
        } catch (TransactionCommitFailedException except) {
            LOG.warn("Error on remove data change counter {}", this.counterId, except);
        }
        this.chain.close();
        LOG.debug("Data change counter {} removed", this.counterId);
    }

    private void putCount(final long count) {
        final WriteTransaction wTx = this.chain.newWriteOnlyTransaction();
        Counter counter = new CounterBuilder().setId(this.counterId).setCount(count).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, this.counterInstanceId, counter);
        wTx.submit();
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        chain.close();
        LOG.warn("Transaction chain failure. Transaction: {}", transaction, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain successful. {}", chain);
    }

}
