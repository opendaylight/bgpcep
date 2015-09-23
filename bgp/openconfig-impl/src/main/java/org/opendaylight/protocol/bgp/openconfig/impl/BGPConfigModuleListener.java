/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.protocol.bgp.openconfig.impl.util.ToOpenConfigUtil;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPGlobalProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.BgpBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPConfigModuleListener implements BGPOpenConfigProvider, TransactionChainListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BGPConfigModuleListener.class);

    private final BGPGlobalProvider globalWriter;
    private final BindingTransactionChain txChain;

    public BGPConfigModuleListener(final DataBroker dataBroker, final BGPConfigStateHolders configStateHolders) {
        this.txChain = dataBroker.createTransactionChain(this);
        final WriteTransaction wTx = this.txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, ToOpenConfigUtil.BGP_IID, new BgpBuilder().build());
        wTx.submit();
        this.globalWriter = new BGPGlobalProviderImpl(this.txChain, configStateHolders.getGlobalConfigHolder());
    }

    @Override
    public BGPGlobalProvider getGlobalProvider() {
        return globalWriter;
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> transactionChain, final AsyncTransaction<?, ?> asyncTransaction,
            final Throwable throwable) {
        LOG.error("Transaction chain {} failed.", throwable, transactionChain);
        this.txChain.close();
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> transactionChain) {
        LOG.debug("Transaction chain {} successful.", transactionChain);
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }

}
