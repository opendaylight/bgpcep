/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.state.spi.BGPStateProvider;
import org.opendaylight.protocol.bgp.state.spi.state.BGPStateConsumer;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StateProviderImpl implements BGPStateProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(StateProviderImpl.class);
    private static final int TO_SECONDS = 1000;
    private final Timer timer = new Timer();
    private final DataBroker dataBroker;
    @GuardedBy("this")
    private final List<BGPStateConsumer> bgpStateConsumers = new ArrayList<>();

    public StateProviderImpl(@Nonnull final DataBroker dataBroker, @Nonnull final Integer timeout) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        Preconditions.checkNotNull(timeout);
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
                StateProviderImpl.this.bgpStateConsumers.forEach(bgpStateConsumer ->
                    wTx.put(LogicalDatastoreType.OPERATIONAL, bgpStateConsumer.getInstanceIdentifier(),
                        bgpStateConsumer.getBgpState()));
                wTx.submit();
            }
        };

        this.timer.scheduleAtFixedRate(task, 0, timeout * TO_SECONDS);
    }

    @Override
    public synchronized void close() throws Exception {
        this.timer.cancel();
        this.bgpStateConsumers.clear();
    }

    @Nonnull
    @Override
    public synchronized AbstractRegistration registerBGPState(final BGPStateConsumer bgpStateConsumer) {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, bgpStateConsumer.getInstanceIdentifier(), bgpStateConsumer.getBgpState(), true);
        wTx.submit();
        this.bgpStateConsumers.add(bgpStateConsumer);
        final InstanceIdentifier<Bgp> iid = bgpStateConsumer.getInstanceIdentifier();
        LOG.debug("Register BGP State for ", iid);
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (StateProviderImpl.this) {
                    StateProviderImpl.this.bgpStateConsumers.remove(bgpStateConsumer);
                    LOG.debug("Unregister BGP State for ", iid);
                    final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
                    wTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                    wTx.submit();
                }
            }
        };
    }
}
