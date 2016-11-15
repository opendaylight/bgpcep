/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import com.google.common.base.Preconditions;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.protocol.bgp.state.spi.BGPStateProvider;
import org.opendaylight.protocol.bgp.state.spi.state.BGPStateConsumer;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StateProviderImpl implements BGPStateProvider, TransactionChainListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(StateProviderImpl.class);
    private final BindingTransactionChain transactionChain;
    @GuardedBy("this")
    private final List<BGPStateConsumer> bgpStateConsumers = new ArrayList<>();
    private final ScheduledFuture<?> scheduleTask;

    public StateProviderImpl(@Nonnull final DataBroker dataBroker, final int timeout) {
        this.transactionChain = Preconditions.checkNotNull(dataBroker).createTransactionChain(this);
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                synchronized (StateProviderImpl.this) {
                    final WriteTransaction wTx = StateProviderImpl.this.transactionChain.newWriteOnlyTransaction();
                    StateProviderImpl.this.bgpStateConsumers.forEach(bgpStateConsumer ->
                        wTx.put(LogicalDatastoreType.OPERATIONAL, bgpStateConsumer.getInstanceIdentifier(),
                            bgpStateConsumer.getBgpState()));
                    wTx.submit();
                }
            }
        };
        this.scheduleTask = GlobalEventExecutor.INSTANCE.scheduleAtFixedRate(task, 0, TimeUnit.SECONDS.toMillis(timeout),
            TimeUnit.SECONDS);
    }

    @Override
    public synchronized void close() throws Exception {
        this.scheduleTask.cancel(true);
        this.bgpStateConsumers.clear();
    }

    @Nonnull
    @Override
    public synchronized AbstractRegistration registerBGPState(final BGPStateConsumer bgpStateConsumer) {
        final WriteTransaction wTx = this.transactionChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, bgpStateConsumer.getInstanceIdentifier(), bgpStateConsumer.getBgpState(), true);
        wTx.submit();
        this.bgpStateConsumers.add(bgpStateConsumer);
        final InstanceIdentifier<Bgp> iid = bgpStateConsumer.getInstanceIdentifier();
        LOG.info("Register BGP State for {}", iid);
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (StateProviderImpl.this) {
                    StateProviderImpl.this.bgpStateConsumers.remove(bgpStateConsumer);
                    LOG.info("Unregister BGP State for {}", iid);
                    final WriteTransaction wTx = StateProviderImpl.this.transactionChain.newWriteOnlyTransaction();
                    wTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                    wTx.submit();
                }
            }
        };
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Transaction chain failed {}.", transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successful.", chain);
    }
}
