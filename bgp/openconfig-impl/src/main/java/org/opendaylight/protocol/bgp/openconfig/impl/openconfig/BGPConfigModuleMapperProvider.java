/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.APPLICATION_PEER_GROUP_NAME;
import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.BGP_IID;

import com.google.common.base.Preconditions;
import java.util.Collections;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.BgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.NeighborsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.PeerGroupsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.peer.group.PeerGroupKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPConfigModuleMapperProvider implements BGPOpenConfigProvider, TransactionChainListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BGPConfigModuleMapperProvider.class);

    private static final PeerGroup APP_PEER_GROUP = new PeerGroupBuilder().setPeerGroupName(APPLICATION_PEER_GROUP_NAME)
            .setKey(new PeerGroupKey(APPLICATION_PEER_GROUP_NAME)).build();

    private final BindingTransactionChain txChain;
    private final BGPConfigMapperRegistry configMapperRegistry = new BGPConfigMapperRegistry();

    public BGPConfigModuleMapperProvider(final DataBroker dataBroker, final BGPConfigStateStore configStateHolders) throws TransactionCommitFailedException {
        Preconditions.checkNotNull(configStateHolders);
        this.txChain = Preconditions.checkNotNull(dataBroker).createTransactionChain(this);
        final WriteTransaction wTx = this.txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, BGP_IID,
                new BgpBuilder().setNeighbors(new NeighborsBuilder().build()).setPeerGroups(
                        new PeerGroupsBuilder().setPeerGroup(Collections.singletonList(APP_PEER_GROUP)).build()).build());
        wTx.submit().checkedGet();
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
