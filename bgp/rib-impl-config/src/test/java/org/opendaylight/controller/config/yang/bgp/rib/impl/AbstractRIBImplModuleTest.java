/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleFactory;
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
import org.opendaylight.controller.config.yang.protocol.framework.TimedReconnectStrategyFactoryModuleFactory;
import org.opendaylight.controller.config.yang.protocol.framework.TimedReconnectStrategyModuleTest;
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
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.opendaylight.yangtools.yang.model.parser.api.YangModelParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public abstract class AbstractRIBImplModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "rib-impl";
    private static final String FACTORY_NAME = RIBImplModuleFactory.NAME;
    private static final String TRANSACTION_NAME = "testTransaction";

    private static final String RIB_ID = "test";
    private static final String BGP_ID = "192.168.1.1";

    private static final String SESSION_RS_INSTANCE_NAME = "session-reconnect-strategy-factory";
    private static final String TCP_RS_INSTANCE_NAME = "tcp-reconnect-strategy-factory";
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
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, moduleFactories.toArray(new ModuleFactory[moduleFactories.size()])));

        Filter mockedFilter = mock(Filter.class);
        Mockito.doReturn(mockedFilter).when(mockedContext).createFilter(Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addBundleListener(any(BundleListener.class));

        Mockito.doReturn(new Bundle[] {}).when(mockedContext).getBundles();

        Mockito.doReturn(new ServiceReference[] {}).when(mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

        ServiceReference<?> emptyServiceReference = mock(ServiceReference.class, "Empty");

        ServiceReference<?> dataProviderServiceReference = mock(ServiceReference.class, "Data Provider");

        Mockito.doReturn(mockedFilter).when(mockedContext).createFilter(Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addBundleListener(any(BundleListener.class));

        Mockito.doReturn(new Bundle[] {}).when(mockedContext).getBundles();

        Mockito.doReturn(new ServiceReference[] {}).when(mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

        // mockedDataProvider = mock(DataProviderService.class);

        Mockito.doReturn("Empty reference").when(emptyServiceReference).toString();
        Mockito.doReturn("Data Provider Service Reference").when(dataProviderServiceReference).toString();
        //
        Mockito.doReturn(emptyServiceReference).when(mockedContext).getServiceReference(any(Class.class));
        Mockito.doReturn(dataProviderServiceReference).when(mockedContext).getServiceReference(DataProviderService.class);

        Mockito.doReturn(mockedDataProvider).when(mockedContext).getService(dataProviderServiceReference);

        // Mockito.doReturn(null).when(mockedContext).getService(dataProviderServiceReference);
        Mockito.doReturn(null).when(mockedContext).getService(emptyServiceReference);

        Registration<DataCommitHandler<InstanceIdentifier, CompositeNode>> registration = mock(Registration.class);
        Mockito.doReturn(registration).when(mockedDataProvider).registerCommitHandler(any(InstanceIdentifier.class),
                any(DataCommitHandler.class));
        Mockito.doReturn(registration).when(mockedDataProvider).registerCommitHandler(any(InstanceIdentifier.class),
                any(DataCommitHandler.class));

        Mockito.doReturn(null).when(mockedDataProvider).readOperationalData(any(InstanceIdentifier.class));
        Mockito.doReturn(mockedTransaction).when(mockedDataProvider).beginTransaction();

        Mockito.doNothing().when(mockedTransaction).putOperationalData(any(InstanceIdentifier.class), any(CompositeNode.class));
        Mockito.doNothing().when(mockedTransaction).removeOperationalData(any(InstanceIdentifier.class));

        Mockito.doReturn(mockedFuture).when(mockedTransaction).commit();
        Mockito.doReturn(TRANSACTION_NAME).when(mockedTransaction).getIdentifier();

        Mockito.doReturn(mockedResult).when(mockedFuture).get();
        Mockito.doReturn(true).when(mockedResult).isSuccessful();
        Mockito.doReturn(Collections.emptySet()).when(mockedResult).getErrors();

        Mockito.doReturn(null).when(mockedDataProvider).readConfigurationData(any(InstanceIdentifier.class));
    }

    protected List<ModuleFactory> getModuleFactories() {
        return Lists.newArrayList(new RIBImplModuleFactory(), new DataBrokerImplModuleFactory(), new GlobalEventExecutorModuleFactory(),
                new BGPDispatcherImplModuleFactory(), new NettyThreadgroupModuleFactory(),
                new TimedReconnectStrategyFactoryModuleFactory(), new SimpleBGPExtensionProviderContextModuleFactory(),
                new RIBExtensionsImplModuleFactory(), new DomBrokerImplModuleFactory(), new RuntimeMappingModuleFactory(),
                new HashMapDataStoreModuleFactory(), new HashedWheelTimerModuleFactory());
    }

    @Override
    protected BundleContextServiceRegistrationHandler getBundleContextServiceRegistrationHandler(final Class<?> serviceType) {
        if (serviceType.equals(SchemaServiceListener.class)) {
            return new BundleContextServiceRegistrationHandler() {
                @Override
                public void handleServiceRegistration(Class<?> clazz, Object serviceInstance, Dictionary<String, ?> props) {
                    SchemaServiceListener listener = (SchemaServiceListener) serviceInstance;
                    YangModelParser parser = new YangParserImpl();
                    Map<InputStream, Module> inputStreamModuleMap = parser.parseYangModelsFromStreamsMapped(new ArrayList<>(getFilesAsInputStreams(getYangModelsPaths())));
                    listener.onGlobalContextUpdated(parser.resolveSchemaContext(Sets.newHashSet(inputStreamModuleMap.values())));
                }
            };
        }

        return super.getBundleContextServiceRegistrationHandler(serviceType);
    }

    @After
    public void closeAllModules() throws Exception {
        super.destroyAllConfigBeans();
    }

    protected CommitStatus createInstance() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createRIBImplModuleInstance(transaction);
        return transaction.commit();
    }

    protected CommitStatus createRIBImplModuleInstance(final RibId ribId, final Long localAs, final Ipv4Address bgpId) throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createRIBImplModuleInstance(transaction, ribId, localAs, bgpId, createDataBrokerInstance(transaction));
        return transaction.commit();
    }

    private ObjectName createRIBImplModuleInstance(final ConfigTransactionJMXClient transaction, final RibId ribId, final Long localAs,
            final Ipv4Address bgpId, final ObjectName dataBroker) throws Exception {
        ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        RIBImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, RIBImplModuleMXBean.class);
        ObjectName reconnectObjectName = TimedReconnectStrategyModuleTest.createInstance(transaction, SESSION_RS_INSTANCE_NAME);
        mxBean.setSessionReconnectStrategy(reconnectObjectName);
        mxBean.setDataProvider(dataBroker);
        ObjectName reconnectStrategyON = TimedReconnectStrategyModuleTest.createInstance(transaction, TCP_RS_INSTANCE_NAME);
        mxBean.setTcpReconnectStrategy(reconnectStrategyON);
        mxBean.setBgpDispatcher(BGPDispatcherImplModuleTest.createInstance(transaction));
        mxBean.setExtensions(createRibExtensionsInstance(transaction));
        mxBean.setRibId(ribId);
        mxBean.setLocalAs(localAs);
        mxBean.setBgpId(bgpId);
        return nameCreated;
    }

    protected ObjectName createRIBImplModuleInstance(final ConfigTransactionJMXClient transaction) throws Exception {
        return createRIBImplModuleInstance(transaction, new RibId(RIB_ID), 5000L, new Ipv4Address(BGP_ID),
                createDataBrokerInstance(transaction));
    }

    public ObjectName createRIBImplModuleInstance(final ConfigTransactionJMXClient transaction, final ObjectName dataBroker)
            throws Exception {
        return createRIBImplModuleInstance(transaction, new RibId(RIB_ID), 5000L, new Ipv4Address(BGP_ID), dataBroker);
    }

    public ObjectName createDataBrokerInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException,
            InstanceNotFoundException {
        ObjectName nameCreated = transaction.createModule(DataBrokerImplModuleFactory.NAME, DATA_BROKER_INSTANCE_NAME);
        DataBrokerImplModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated, DataBrokerImplModuleMXBean.class);
        mxBean.setDomBroker(createDomBrokerInstance(transaction));
        mxBean.setMappingService(lookupMappingServiceInstance(transaction));
        return nameCreated;
    }

    private ObjectName createDomBrokerInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(DomBrokerImplModuleFactory.NAME, DOM_BROKER_INSTANCE_NAME);
        DomBrokerImplModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated, DomBrokerImplModuleMXBean.class);
        mxBean.setDataStore(createDataStoreInstance(transaction));
        return nameCreated;
    }

    private ObjectName createDataStoreInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(HashMapDataStoreModuleFactory.NAME, DATA_STORE_INSTANCE_NAME);
        transaction.newMBeanProxy(nameCreated, HashMapDataStoreModuleMXBean.class);
        return nameCreated;
    }

    private static ObjectName lookupMappingServiceInstance(final ConfigTransactionJMXClient transaction) {

        try {
            return transaction.lookupConfigBean(RuntimeMappingModuleFactory.NAME, RuntimeMappingModuleFactory.SINGLETON_NAME);
        } catch (InstanceNotFoundException e) {
            try {
                return transaction.createModule(RuntimeMappingModuleFactory.NAME, RuntimeMappingModuleFactory.SINGLETON_NAME);
            } catch (InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    private ObjectName createRibExtensionsInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(RIBExtensionsImplModuleFactory.NAME, RIB_EXTENSIONS_INSTANCE_NAME);
        transaction.newMBeanProxy(nameCreated, RIBExtensionsImplModuleMXBean.class);
        return nameCreated;
    }

    public List<String> getYangModelsPaths() {
        List<String> paths = Lists.newArrayList("/META-INF/yang/bgp-rib.yang", "/META-INF/yang/ietf-inet-types.yang",
                "/META-INF/yang/bgp-message.yang", "/META-INF/yang/bgp-multiprotocol.yang", "/META-INF/yang/bgp-types.yang");
        return paths;
    }

    // TODO move back to AbstractConfigTest
    private static Collection<InputStream> getFilesAsInputStreams(List<String> paths) {
        final Collection<InputStream> resources = new ArrayList<>();
        List<String> failedToFind = new ArrayList<>();
        for (String path : paths) {
            InputStream resourceAsStream = AbstractRIBImplModuleTest.class.getResourceAsStream(path);
            if (resourceAsStream == null) {
                failedToFind.add(path);
            } else {
                resources.add(resourceAsStream);
            }
        }
        Assert.assertEquals("Some files were not found", Collections.<String> emptyList(), failedToFind);

        return resources;
    }
}
