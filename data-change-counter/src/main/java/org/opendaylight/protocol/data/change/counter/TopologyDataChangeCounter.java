/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.data.change.counter;

import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.DataChangeCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.Counter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.CounterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.CounterKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyDataChangeCounter implements DataChangeListener, TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataChangeCounter.class);

    private final DataBroker dataBroker;
    private final String counterId;
    private final InstanceIdentifier<Counter> counterInstanceId;
    private final BindingTransactionChain chain;
    private final AtomicLong count;

    public TopologyDataChangeCounter(final DataBroker dataBroker, final String counterId) {
        this.dataBroker = dataBroker;
        this.chain = this.dataBroker.createTransactionChain(this);
        this.counterId = counterId;
        this.counterInstanceId = InstanceIdentifier.builder(DataChangeCounter.class)
                .child(Counter.class, new CounterKey(this.counterId)).build();
        this.count = new AtomicLong(0);
        putCount(this.count.get());
        LOG.debug("Data change counter {} initiated", this.counterId);
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        putCount(this.count.incrementAndGet());
        LOG.debug("Data change #{} for counter {}", this.count.get(), this.counterId);
    }

    public void close() {
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
