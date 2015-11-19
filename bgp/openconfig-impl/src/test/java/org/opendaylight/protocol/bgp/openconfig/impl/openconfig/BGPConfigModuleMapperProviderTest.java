/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import static org.junit.Assert.assertTrue;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPAppPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPRibInstanceConfiguration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BGPConfigModuleMapperProviderTest {

    private BGPConfigModuleMapperProvider mapperProvider;

    private final BindingTransactionChain txChain = Mockito.mock(BindingTransactionChain.class);
    private final AsyncTransaction<?, ?> asyncTx = Mockito.mock(AsyncTransaction.class);
    private final WriteTransaction myTx = Mockito.mock(WriteTransaction.class);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {

        final BindingDOMDataBrokerAdapter dataBroker = Mockito.mock(BindingDOMDataBrokerAdapter.class);
        final BGPConfigStateStore stateHolders = Mockito.mock(BGPConfigStateStore.class);
        final CheckedFuture<?, ?> checkedFuture = Mockito.mock(CheckedFuture.class);
        final BGPConfigHolder<?> confHolder = Mockito.mock(BGPConfigHolder.class);

        Mockito.doReturn(this.txChain).when(dataBroker).createTransactionChain(Mockito.any(TransactionChainListener.class));
        Mockito.doReturn(this.myTx).when(this.txChain).newWriteOnlyTransaction();
        Mockito.doNothing().when(this.txChain).close();
        Mockito.doNothing().when(this.myTx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class));
        Mockito.doNothing().when(this.myTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doReturn(checkedFuture).when(this.myTx).submit();
        Mockito.doReturn(null).when(checkedFuture).checkedGet();
        Mockito.doReturn(confHolder).when(stateHolders).getBGPConfigHolder(Mockito.any(Class.class));

        this.mapperProvider = new BGPConfigModuleMapperProvider(dataBroker, stateHolders);
    }

    @Test
    public void testGetOpenConfigMapper() {
        assertTrue(this.mapperProvider.getOpenConfigMapper(BGPRibInstanceConfiguration.class) instanceof BGPGlobalProviderImpl);
        assertTrue(this.mapperProvider.getOpenConfigMapper(BGPPeerInstanceConfiguration.class) instanceof BGPNeighborProviderImpl);
        assertTrue(this.mapperProvider.getOpenConfigMapper(BGPAppPeerInstanceConfiguration.class) instanceof BGPAppNeighborProviderImpl);
    }

    @Test
    public void testOnTransactionChainFailed() {
        this.mapperProvider.onTransactionChainFailed(this.txChain, this.asyncTx, new Throwable());
        Mockito.verify(this.txChain).close();
    }

    @Test
    public void testClose() throws Exception {
        this.mapperProvider.close();
        Mockito.verify(this.myTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.verify(this.myTx, Mockito.times(2)).submit();
        Mockito.verify(this.txChain).close();
    }

}
