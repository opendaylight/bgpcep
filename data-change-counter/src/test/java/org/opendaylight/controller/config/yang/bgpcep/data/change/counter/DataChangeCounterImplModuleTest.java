/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgpcep.data.change.counter;

import java.util.Collections;
import java.util.Set;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev140815.DataChangeCounter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;

public class DataChangeCounterImplModuleTest extends AbstractConfigTest {

    private static final String FACTORY_NAME = DataChangeCounterImplModuleFactory.NAME;
    private static final String INSTANCE_NAME = DataChangeCounterImplModuleFactory.SINGLETON_NAME;
    private static final String DATA_BROKER_INSTANCE_NAME = "data-broker-instance";

    private static final String TOPOLOGY_NAME = "test";
    private static final String NEW_TOPOLOGY_NAME = "new-test";

    @Mock
    private CloseableDataBroker dataBorker;
    @Mock
    private BindingTransactionChain chain;
    @Mock
    private WriteTransaction wTx;
    @Mock
    private ListenerRegistration<DataChangeListener> registration;

    @Before
    public void setUp() throws Exception {
        Mockito.doNothing().when(this.registration).close();
        Mockito.doReturn(null).when(this.wTx).submit();
        Mockito.doNothing().when(this.wTx).put(Mockito.any(LogicalDatastoreType.class), Mockito.<InstanceIdentifier<DataChangeCounter>>any(), Mockito.any(DataChangeCounter.class));
        Mockito.doReturn(this.registration).when(this.dataBorker).registerDataChangeListener(Mockito.any(LogicalDatastoreType.class), Mockito.<InstanceIdentifier<?>>any(), Mockito.any(DataChangeListener.class), Mockito.any(DataBroker.DataChangeScope.class));
        Mockito.doNothing().when(this.wTx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.<InstanceIdentifier<?>>any());
        Mockito.doReturn(this.chain).when(this.dataBorker).createTransactionChain(Mockito.any(TransactionChainListener.class));
        Mockito.doReturn(this.wTx).when(this.chain).newWriteOnlyTransaction();
        Mockito.doReturn(this.wTx).when(this.dataBorker).newWriteOnlyTransaction();
        Mockito.doNothing().when(this.chain).close();
        Mockito.doNothing().when(this.dataBorker).close();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext, new DataChangeCounterImplModuleFactory(), new MockDataBrokerModuleFct()));
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance(TOPOLOGY_NAME);
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 2, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance(TOPOLOGY_NAME);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 2);
    }

    @Test
    public void testReconfigureBean() throws Exception {
        createInstance(TOPOLOGY_NAME);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final DataChangeCounterImplModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                DataChangeCounterImplModuleMXBean.class);
        mxBean.setTopologyName(NEW_TOPOLOGY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 1);

        final ConfigTransactionJMXClient transaction2 = this.configRegistryClient.createTransaction();
        final DataChangeCounterImplModuleMXBean mxBean2 = transaction2.newMXBeanProxy(transaction2.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                DataChangeCounterImplModuleMXBean.class);
        Assert.assertEquals(NEW_TOPOLOGY_NAME, mxBean2.getTopologyName());
    }

    private CommitStatus createInstance(final String topologyName) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final ObjectName on = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final ObjectName dbOn = transaction.createModule(MockDataBrokerModuleFct.INSTANCE_NAME, DATA_BROKER_INSTANCE_NAME);
        final DataChangeCounterImplModuleMXBean mxBean = transaction.newMXBeanProxy(on, DataChangeCounterImplModuleMXBean.class);
        mxBean.setTopologyName(topologyName);
        mxBean.setDataProvider(dbOn);
        return transaction.commit();
    }

    private final class MockDataBrokerModuleFct implements org.opendaylight.controller.config.spi.ModuleFactory {

        private static final String INSTANCE_NAME = "data-broker-fct";

        @Override
        public String getImplementationName() {
            return INSTANCE_NAME;
        }

        @Override
        public Module createModule(final String instanceName, final DependencyResolver dependencyResolver,
                final BundleContext bundleContext) {
            return new MockDataBrokerModule();
        }

        @Override
        public Module createModule(final String instanceName, final DependencyResolver dependencyResolver,
                final DynamicMBeanWithInstance old, final BundleContext bundleContext) throws Exception {
            return new MockDataBrokerModule();
        }

        @Override
        public boolean isModuleImplementingServiceInterface(final Class<? extends AbstractServiceInterface> serviceInterface) {
            return true;
        }

        @Override
        public Set<Class<? extends AbstractServiceInterface>> getImplementedServiceIntefaces() {
            final java.util.Set<Class<? extends org.opendaylight.controller.config.api.annotations.AbstractServiceInterface>> serviceIfcs2 = new java.util.HashSet<Class<? extends org.opendaylight.controller.config.api.annotations.AbstractServiceInterface>>();
            return java.util.Collections.unmodifiableSet(serviceIfcs2);
        }

        @Override
        public Set<? extends Module> getDefaultModules(final DependencyResolverFactory dependencyResolverFactory,
                final BundleContext bundleContext) {
            return Collections.emptySet();
        }

    }

    private final class MockDataBrokerModule implements org.opendaylight.controller.config.spi.Module,org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingAsyncDataBrokerImplModuleMXBean,org.opendaylight.controller.config.yang.md.sal.binding.DataBrokerServiceInterface {

        @Override
        public ModuleIdentifier getIdentifier() {
            return new ModuleIdentifier(MockDataBrokerModuleFct.INSTANCE_NAME, DATA_BROKER_INSTANCE_NAME);
        }

        @Override
        public ObjectName getBindingMappingService() {
            return null;
        }

        @Override
        public void setBindingMappingService(final ObjectName bindingMappingService) {
            return;
        }

        @Override
        public ObjectName getDomAsyncBroker() {
            return null;
        }

        @Override
        public void setDomAsyncBroker(final ObjectName domAsyncBroker) {
            return;
        }

        @Override
        public void validate() {
            return;
        }

        @Override
        public AutoCloseable getInstance() {
            return DataChangeCounterImplModuleTest.this.dataBorker;
        }

        @Override
        public ObjectName getSchemaService() {
            return null;
        }

        @Override
        public void setSchemaService(final ObjectName schemaService) {
            return;
        }

        @Override
        public boolean canReuse(final Module arg0) {
            return true;
        }

    }

    private interface CloseableDataBroker extends DataBroker, AutoCloseable {
    }

}
