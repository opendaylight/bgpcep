/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.BGP_IID;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPConfigModuleMapperProvider implements BGPOpenConfigProvider, TransactionChainListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BGPConfigModuleMapperProvider.class);

    private final BindingTransactionChain txChain;
    private final BGPConfigMapperRegistry configMapperRegistry = new BGPConfigMapperRegistry();

    public BGPConfigModuleMapperProvider(final DataBroker dataBroker, final BGPConfigStateStore configStateHolders) {
        Preconditions.checkNotNull(configStateHolders);
        this.txChain = Preconditions.checkNotNull(dataBroker).createTransactionChain(this);
        configMapperRegistry.registerOpenConfigMapper(new BGPGlobalProviderImpl(txChain, configStateHolders));
        configMapperRegistry.registerOpenConfigMapper(new BGPNeighborProviderImpl(txChain, configStateHolders));
        configMapperRegistry.registerOpenConfigMapper(new BGPAppNeighborProviderImpl(txChain, configStateHolders));
    }

    @Override
    public <T extends InstanceConfiguration> BGPOpenconfigMapper<T> getOpenConfigMapper(final Class<T> clazz) {
        return configMapperRegistry.getOpenConfigMapper(clazz);
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> transactionChain, final AsyncTransaction<?, ?> asyncTransaction,
            final Throwable throwable) {
        LOG.error("Transaction chain {} failed.", transactionChain, throwable);
        txChain.close();
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> transactionChain) {
        LOG.debug("Transaction chain {} successful.", transactionChain);
    }

    @Override
    public void close() throws Exception {
        final WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, BGP_IID);
        wTx.submit().checkedGet();
        txChain.close();
    }

}
