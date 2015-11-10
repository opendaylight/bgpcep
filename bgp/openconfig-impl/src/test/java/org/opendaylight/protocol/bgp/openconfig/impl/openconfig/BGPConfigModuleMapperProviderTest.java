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
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPAppPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPRibInstanceConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class BGPConfigModuleMapperProviderTest {

    private BGPConfigModuleMapperProvider mapperProvider;

    private final BindingTransactionChain txChain = Mockito.mock(BindingTransactionChain.class);
    private final AsyncTransaction<?, ?> asyncTx = Mockito.mock(AsyncTransaction.class);
    private final MyWriteTransaction myTx = Mockito.mock(MyWriteTransaction.class);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws TransactionCommitFailedException {

        final BindingDOMDataBrokerAdapter dataBroker = Mockito.mock(BindingDOMDataBrokerAdapter.class);
        final BGPConfigStateStore stateHolders = Mockito.mock(BGPConfigStateStore.class);

        Mockito.doReturn(this.txChain).when(dataBroker).createTransactionChain(Mockito.any(TransactionChainListener.class));
        Mockito.doReturn(this.myTx).when(this.txChain).newWriteOnlyTransaction();
        Mockito.doNothing().when(this.txChain).close();
        Mockito.doNothing().when(this.myTx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class));
        Mockito.doNothing().when(this.myTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doReturn(new MyFuture()).when(this.myTx).submit();
        Mockito.doReturn(new MyBGPConfigHolder()).when(stateHolders).getBGPConfigHolder(Mockito.any(Class.class));

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

    private class MyWriteTransaction implements WriteTransaction {
        @Override
        public boolean cancel() {
            return false;
        }
        @Override
        public CheckedFuture<Void, TransactionCommitFailedException> submit() {
            return null;
        }
        @Override
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {
            return null;
        }
        @Override
        public Object getIdentifier() {
            return null;
        }
        @Override
        public <T extends DataObject> void put(final LogicalDatastoreType store, final InstanceIdentifier<T> path, final T data) {

        }
        @Override
        public <T extends DataObject> void put(final LogicalDatastoreType store, final InstanceIdentifier<T> path, final T data,
            final boolean createMissingParents) {
        }
        @Override
        public <T extends DataObject> void merge(final LogicalDatastoreType store, final InstanceIdentifier<T> path, final T data) {
        }
        @Override
        public <T extends DataObject> void merge(final LogicalDatastoreType store, final InstanceIdentifier<T> path, final T data,
            final boolean createMissingParents) {
        }
        @Override
        public void delete(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
        }
    }

    private class MyFuture implements CheckedFuture<Void, TransactionCommitFailedException> {
        @Override
        public void addListener(final Runnable listener, final Executor executor) {
        }
        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            return false;
        }
        @Override
        public boolean isCancelled() {
            return false;
        }
        @Override
        public boolean isDone() {
            return false;
        }
        @Override
        public Void get() throws InterruptedException, ExecutionException {
            return null;
        }
        @Override
        public Void get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
        @Override
        public Void checkedGet() throws TransactionCommitFailedException {
            return null;
        }
        @Override
        public Void checkedGet(final long timeout, final TimeUnit unit) throws TimeoutException, TransactionCommitFailedException {
            return null;
        }
    }

    private class MyBGPConfigHolder implements BGPConfigHolder<DataObject> {
        @Override
        public boolean remove(final ModuleKey moduleKey) {
            return false;
        }
        @Override
        public boolean addOrUpdate(final ModuleKey moduleKey, final Identifier key, final DataObject newValue) {
            return false;
        }
        @Override
        public ModuleKey getModuleKey(final Identifier key) {
            return null;
        }
        @Override
        public Identifier getKey(final ModuleKey moduleKey) {
            return null;
        }
    }
}
