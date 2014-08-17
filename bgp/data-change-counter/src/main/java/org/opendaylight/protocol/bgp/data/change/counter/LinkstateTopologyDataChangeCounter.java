/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.data.change.counter;

import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev140815.DataChangeCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev140815.DataChangeCounterBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkstateTopologyDataChangeCounter implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(LinkstateTopologyDataChangeCounter.class);

    protected static final InstanceIdentifier<DataChangeCounter> IID = InstanceIdentifier
            .builder(DataChangeCounter.class).toInstance();

    private final DataBroker dataBroker;
    private AtomicLong count;

    public LinkstateTopologyDataChangeCounter(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.count = new AtomicLong(0);
        putCount(this.count.get());
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        putCount(this.count.incrementAndGet());
        LOG.debug("Linkstate topology data change #{}", this.count.get());
    }

    public void close() {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, IID);
        wTx.submit();
        LOG.debug("Data change counter removed");
    }

    private void putCount(final long count) {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IID, new DataChangeCounterBuilder().setCount(count)
                .build());
        wTx.submit();
    }

}
