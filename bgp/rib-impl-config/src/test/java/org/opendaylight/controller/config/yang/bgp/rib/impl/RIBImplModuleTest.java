/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.bgp.reconnectstrategy.TimedReconnectStrategyModuleTest;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleFactory;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.DataBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.DataBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RuntimeMappingModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.HashMapDataStoreModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.HashMapDataStoreModuleMXBean;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.netty.timer.HashedWheelTimerModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.TimedReconnectStrategyModuleFactory;
import org.opendaylight.controller.config.yang.store.impl.YangParserWrapper;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.google.common.collect.Lists;

public class RIBImplModuleTest extends AbstractConfigTest {
    private static final String INSTANCE_NAME = "rib-impl";
    private static final String FACTORY_NAME = RIBImplModuleFactory.NAME;
    private static final String TRANSACTION_NAME = "testTransaction";

    private static final String RIB_ID = "test";
    private static final String BGP_ID = "192.168.1.1";

    private static final String SESSION_RS_INSTANCE_NAME = "session-reconnect-strategy-impl";
    private static final String TCP_RS_INSTANCE_NAME = "tcp-reconnect-strategy";
    private static final String DATA_BROKER_INSTANCE_NAME = "data-broker-impl";
    private static final String DOM_BROKER_INSTANCE_NAME = "data-broker-impl";
    private static final String DATA_STORE_INSTANCE_NAME = "data-store-impl";
    private static final String RIB_EXTENSIONS_INSTANCE_NAME = "rib-extensions-impl";

    @Mock
    private DataModificationTransaction mockedTransaction;

    @Mock
    private DataProviderService mockedDataProvider;

    @Mock
    private Future<RpcResult<TransactionStatus>> mockedFuture;

    @Mock
    private RpcResult<TransactionStatus> mockedResult;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        List<ModuleFactory> moduleFactories = getModuleFactories();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, moduleFactories
                .toArray(new ModuleFactory[moduleFactories.size()])));

        Filter mockedFilter = mock(Filter.class);
        Mockito.doReturn(mockedFilter).when(mockedContext).createFilter(Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addBundleListener(any(BundleListener.class));

        Mockito.doReturn(new Bundle[] {}).when(mockedContext).getBundles();

        Mockito.doReturn(new ServiceReference[] {}).when(mockedContext)
                .getServiceReferences(Matchers.anyString(), Matchers.anyString());

        ServiceReference<?> emptyServiceReference = mock(ServiceReference.class, "Empty");

        ServiceReference<?> dataProviderServiceReference = mock(ServiceReference.class, "Data Provider");

        Mockito.doReturn(mockedFilter).when(mockedContext).createFilter(Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addBundleListener(any(BundleListener.class));

        Mockito.doReturn(new Bundle[] {}).when(mockedContext).getBundles();

        Mockito.doReturn(new ServiceReference[] {}).when(mockedContext)
                .getServiceReferences(Matchers.anyString(), Matchers.anyString());

        // mockedDataProvider = mock(DataProviderService.class);

        Mockito.doReturn("Empty reference").when(emptyServiceReference).toString();
        Mockito.doReturn("Data Provider Service Reference").when(dataProviderServiceReference).toString();
        //
        Mockito.doReturn(emptyServiceReference).when(mockedContext).getServiceReference(any(Class.class));
        Mockito.doReturn(dataProviderServiceReference).when(mockedContext)
                .getServiceReference(DataProviderService.class);

        Mockito.doReturn(mockedDataProvider).when(mockedContext).getService(dataProviderServiceReference);

        // Mockito.doReturn(null).when(mockedContext).getService(dataProviderServiceReference);
        Mockito.doReturn(null).when(mockedContext).getService(emptyServiceReference);

        Registration<DataCommitHandler<InstanceIdentifier, CompositeNode>> registration = mock(Registration.class);
        Mockito.doReturn(registration).when(mockedDataProvider)
                .registerCommitHandler(any(InstanceIdentifier.class), any(DataCommitHandler.class));
        Mockito.doReturn(registration).when(mockedDataProvider)
                .registerCommitHandler(any(InstanceIdentifier.class), any(DataCommitHandler.class));

        Mockito.doReturn(null).when(mockedDataProvider).readOperationalData(any(InstanceIdentifier.class));
        Mockito.doReturn(mockedTransaction).when(mockedDataProvider).beginTransaction();

        Mockito.doNothing().when(mockedTransaction)
                .putOperationalData(any(InstanceIdentifier.class), any(CompositeNode.class));
        Mockito.doNothing().when(mockedTransaction).removeOperationalData(any(InstanceIdentifier.class));

        Mockito.doReturn(mockedFuture).when(mockedTransaction).commit();
        Mockito.doReturn(TRANSACTION_NAME).when(mockedTransaction).getIdentifier();

        Mockito.doReturn(mockedResult).when(mockedFuture).get();
        Mockito.doReturn(true).when(mockedResult).isSuccessful();
        Mockito.doReturn(Collections.emptySet()).when(mockedResult).getErrors();
    }

    protected List<ModuleFactory> getModuleFactories() {
        return Lists.newArrayList(new RIBImplModuleFactory(), new DataBrokerImplModuleFactory(),
                new GlobalEventExecutorModuleFactory(), new BGPDispatcherImplModuleFactory(),
                new NettyThreadgroupModuleFactory(), new TimedReconnectStrategyModuleFactory(),
                new SimpleBGPExtensionProviderContextModuleFactory(), new RIBExtensionsImplModuleFactory(),
                new DomBrokerImplModuleFactory(), new RuntimeMappingModuleFactory(),
                new HashMapDataStoreModuleFactory(), new HashedWheelTimerModuleFactory());
    }

    @Override
    protected BundleContextServiceRegistrationHandler getBundleContextServiceRegistrationHandler(
            final Class<?> serviceType) {
        if (serviceType.equals(SchemaServiceListener.class)) {
            return new BundleContextServiceRegistrationHandler() {
                @Override
                public void handleServiceRegistration(final Object o) {
                    SchemaServiceListener listener = (SchemaServiceListener) o;
                    listener.onGlobalContextUpdated(getMockedSchemaContext());
                }
            };
        }

        return super.getBundleContextServiceRegistrationHandler(serviceType);
    }

    @Test
    public void testValidationExceptionRibIdNotSet() throws Exception {
        try {
            createInstance(null, 500L, new Ipv4Address(BGP_ID));
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("RibId is not set."));
        }
    }

    @Test
    public void testValidationExceptionLocalAsNotSet() throws Exception {
        try {
            createInstance(new RibId(RIB_ID), null, new Ipv4Address(BGP_ID));
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("LocalAs is not set."));
        }
    }

    @Test
    public void testValidationExceptionBgpIdNotSet() throws Exception {
        try {
            createInstance(new RibId(RIB_ID), 500L, null);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("BgpId is not set."));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        CommitStatus status = createInstance();
        Thread.sleep(2000);
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 14, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        Thread.sleep(2000);
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 14);
    }

    @Test
    public void testReconfigure() throws Exception {
        createInstance();
        Thread.sleep(2000);
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final RIBImplModuleMXBean mxBean = transaction.newMBeanProxy(
                transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME), RIBImplModuleMXBean.class);
        mxBean.setLocalAs(100L);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 13);
    }

    @After
    public void closeAllModules() throws Exception {
        super.destroyAllConfigBeans();
    }

    private CommitStatus createInstance() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction);
        return transaction.commit();
    }

    private CommitStatus createInstance(final RibId ribId, final Long localAs, final Ipv4Address bgpId)
            throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction, ribId, localAs, bgpId);
        return transaction.commit();
    }

    private static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final RibId ribId,
            final Long localAs, final Ipv4Address bgpId) throws Exception {
        ObjectName nameCreated = transaction.createModule(RIBImplModuleFactory.NAME, INSTANCE_NAME);
        RIBImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, RIBImplModuleMXBean.class);
        ObjectName reconnectObjectName = TimedReconnectStrategyModuleTest.createInstance(transaction,
                SESSION_RS_INSTANCE_NAME);
        mxBean.setSessionReconnectStrategy(reconnectObjectName);
        mxBean.setDataProvider(createDataBrokerInstance(transaction));
        ObjectName reconnectStrategyON = TimedReconnectStrategyModuleTest.createInstance(transaction,
                TCP_RS_INSTANCE_NAME);
        mxBean.setTcpReconnectStrategy(reconnectStrategyON);
        mxBean.setBgpDispatcher(BGPDispatcherImplModuleTest.createInstance(transaction));
        mxBean.setExtensions(createRibExtensionsInstance(transaction));
        mxBean.setRibId(ribId);
        mxBean.setLocalAs(localAs);
        mxBean.setBgpId(bgpId);
        return nameCreated;
    }

    public static ObjectName createInstance(final ConfigTransactionJMXClient transaction) throws Exception {
        return createInstance(transaction, new RibId(RIB_ID), 5000L, new Ipv4Address(BGP_ID));
    }

    private static ObjectName createDataBrokerInstance(final ConfigTransactionJMXClient transaction)
            throws InstanceAlreadyExistsException, InstanceNotFoundException {
        ObjectName nameCreated = transaction.createModule(DataBrokerImplModuleFactory.NAME, DATA_BROKER_INSTANCE_NAME);
        DataBrokerImplModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated, DataBrokerImplModuleMXBean.class);
        mxBean.setDomBroker(createDomBrokerInstance(transaction));
        mxBean.setMappingService(lookupMappingServiceInstance(transaction));
        return nameCreated;
    }

    private static ObjectName createDomBrokerInstance(final ConfigTransactionJMXClient transaction)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(DomBrokerImplModuleFactory.NAME, DOM_BROKER_INSTANCE_NAME);
        DomBrokerImplModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated, DomBrokerImplModuleMXBean.class);
        mxBean.setDataStore(createDataStoreInstance(transaction));
        return nameCreated;
    }

    private static ObjectName createDataStoreInstance(final ConfigTransactionJMXClient transaction)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(HashMapDataStoreModuleFactory.NAME, DATA_STORE_INSTANCE_NAME);
        transaction.newMBeanProxy(nameCreated, HashMapDataStoreModuleMXBean.class);
        return nameCreated;
    }

    private static ObjectName lookupMappingServiceInstance(final ConfigTransactionJMXClient transaction) {

        try {
            return transaction.lookupConfigBean(RuntimeMappingModuleFactory.NAME,
                    RuntimeMappingModuleFactory.SINGLETON_NAME);
        } catch (InstanceNotFoundException e) {
            try {
                return transaction.createModule(RuntimeMappingModuleFactory.NAME,
                        RuntimeMappingModuleFactory.SINGLETON_NAME);
            } catch (InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    private static ObjectName createRibExtensionsInstance(final ConfigTransactionJMXClient transaction)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(RIBExtensionsImplModuleFactory.NAME,
                RIB_EXTENSIONS_INSTANCE_NAME);
        transaction.newMBeanProxy(nameCreated, RIBExtensionsImplModuleMXBean.class);
        return nameCreated;
    }

    public SchemaContext getMockedSchemaContext() {
        List<String> paths = Arrays.asList("/META-INF/yang/bgp-rib.yang", "/META-INF/yang/ietf-inet-types.yang",
                "/META-INF/yang/bgp-message.yang", "/META-INF/yang/bgp-multiprotocol.yang",
                "/META-INF/yang/bgp-types.yang");
        return YangParserWrapper.parseYangFiles(getFilesAsInputStreams(paths));
    }
}
