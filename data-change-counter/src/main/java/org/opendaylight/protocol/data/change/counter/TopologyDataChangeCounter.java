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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev140815.DataChangeCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev140815.DataChangeCounterBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyDataChangeCounter implements DataChangeListener, TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataChangeCounter.class);

    protected static final InstanceIdentifier<DataChangeCounter> IID = InstanceIdentifier
            .builder(DataChangeCounter.class).build();

    private final DataBroker dataBroker;
    private final BindingTransactionChain chain;
    private final AtomicLong count;

    public TopologyDataChangeCounter(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.chain = this.dataBroker.createTransactionChain(this);
        this.count = new AtomicLong(0);
        putCount(this.count.get());
        LOG.debug("Data change counter initiated");
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        putCount(this.count.incrementAndGet());
        LOG.debug("Data change #{}", this.count.get());
    }

    public void close() {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, IID);
        wTx.submit();
        this.chain.close();
        LOG.debug("Data change counter removed");
    }

    private void putCount(final long count) {
        final WriteTransaction wTx = this.chain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IID, new DataChangeCounterBuilder().setCount(count).build());
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
