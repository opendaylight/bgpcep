/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Predicate;
import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer which is responsible for exporting best paths into the datastore.
 */
final class LocalRibConsumer extends ConsumingRouter implements TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(LocalRibConsumer.class);
    private final BindingTransactionChain chain;

    protected LocalRibConsumer(final DataBroker broker, final Predicate<PathAttributes> policy) {
        super(policy);
        this.chain = broker.createTransactionChain(this);
    }

    @Override
    UnsignedInteger getRouterId() {
        /*
         * 0 value is not a valid router ID. We can safely use it to carve out
         * a place in the RibTableEntry.
         */
        return UnsignedInteger.ZERO;
    }

    @Override
    <T> void routeUpdated(final RibTable<T> table, final T nlri, final RibTableEntry<T> bestPath) {
        /*
         * Guaranteed to be called from the route selector thread only. We hijack the thread
         * to update the datastore.
         */
        KeyedInstanceIdentifier<Tables, TablesKey> tableId;

        final WriteTransaction tx = chain.newWriteOnlyTransaction();

        // FIXME: write/delete as appropriate

        tx.submit();
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> c, final AsyncTransaction<?, ?> tx, final Throwable cause) {
        LOG.error("Backing transaction chain {} failed on transaction {}. Cannot recover.", c, tx, cause);
        // FIXME: disable on failure
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> tx) {
        LOG.info("Processing completed");
    }
}
