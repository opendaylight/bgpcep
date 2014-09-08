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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.CheckedFuture;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
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
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingAsyncDataBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingAsyncDataBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RuntimeMappingModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.SchemaServiceImplSingletonModuleFactory;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.netty.timer.HashedWheelTimerModuleFactory;
import org.opendaylight.controller.config.yang.protocol.framework.TimedReconnectStrategyFactoryModuleFactory;
import org.opendaylight.controller.config.yang.protocol.framework.TimedReconnectStrategyModuleTest;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.dom.broker.GlobalBundleScanningSchemaServiceImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.opendaylight.yangtools.yang.parser.repo.URLSchemaContextResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public abstract class AbstractRIBImplModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "rib-impl";
    private static final String FACTORY_NAME = RIBImplModuleFactory.NAME;
    private static final String TRANSACTION_NAME = "testTransaction";

    protected static final RibId RIB_ID = new RibId("test");
    protected static final Ipv4Address BGP_ID = new Ipv4Address("192.168.1.1");

    private static final String SESSION_RS_INSTANCE_NAME = "session-reconnect-strategy-factory";
    private static final String TCP_RS_INSTANCE_NAME = "tcp-reconnect-strategy-factory";
    private static final String RIB_EXTENSIONS_INSTANCE_NAME = "rib-extensions-impl";
    private static final String DOM_BROKER_INSTANCE_NAME = "dom-broker-impl";
    private static final String BINDING_ASYNC_BROKER_INSTANCE_NAME = "binding-async-broker-instance";
    private static final String DOM_ASYNC_DATA_BROKER_INSTANCE = "dom-inmemory-data-broker";

    @Mock
    private ReadWriteTransaction mockedTransaction;

    @Mock
    private DataBroker mockedDataProvider;

    @Mock
    private CheckedFuture<Void, TransactionCommitFailedException> mockedFuture;

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

        final ServiceReference<?> emptyServiceReference = mock(ServiceReference.class, "Empty");
        final ServiceReference<?> classLoadingStrategySR = mock(ServiceReference.class, "ClassLoadingStrategy");
        final ServiceReference<?> dataProviderServiceReference = mock(ServiceReference.class, "Data Provider");

        Mockito.doReturn(mockedFilter).when(mockedContext).createFilter(Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());
        Mockito.doNothing().when(mockedContext).removeServiceListener(any(ServiceListener.class));

        Mockito.doNothing().when(mockedContext).addBundleListener(any(BundleListener.class));
        Mockito.doNothing().when(mockedContext).removeBundleListener(any(BundleListener.class));

        Mockito.doReturn(new Bundle[] {}).when(mockedContext).getBundles();

        Mockito.doReturn(new ServiceReference[] {}).when(mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

        Mockito.doReturn("Empty reference").when(emptyServiceReference).toString();
        Mockito.doReturn("Data Provider Service Reference").when(dataProviderServiceReference).toString();
        Mockito.doReturn("Class loading stategy reference").when(classLoadingStrategySR).toString();

        Mockito.doReturn(emptyServiceReference).when(mockedContext).getServiceReference(any(Class.class));
        Mockito.doReturn(dataProviderServiceReference).when(mockedContext).getServiceReference(DataBroker.class);
        Mockito.doReturn(classLoadingStrategySR).when(mockedContext).getServiceReference(GeneratedClassLoadingStrategy.class);

        Mockito.doReturn(mockedDataProvider).when(mockedContext).getService(dataProviderServiceReference);
        Mockito.doReturn(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy()).when(mockedContext).getService(classLoadingStrategySR);
        Mockito.doReturn(null).when(mockedContext).getService(emptyServiceReference);

        Mockito.doReturn(mockedTransaction).when(mockedDataProvider).newReadWriteTransaction();

        Mockito.doReturn(null).when(mockedTransaction).read(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
        Mockito.doNothing().when(mockedTransaction).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        Mockito.doNothing().when(mockedTransaction).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));

        Mockito.doReturn(mockedFuture).when(mockedTransaction).submit();
        Mockito.doReturn(TRANSACTION_NAME).when(mockedTransaction).getIdentifier();

        Mockito.doReturn(null).when(mockedFuture).get();

        GlobalBundleScanningSchemaServiceImpl schemaService = GlobalBundleScanningSchemaServiceImpl.createInstance(this.mockedContext);
        YangContextParser parser = new YangParserImpl();
        SchemaContext context = parser.parseSources(getFilesAsByteSources(getYangModelsPaths()));
        URLSchemaContextResolver mockedContextResolver = Mockito.mock(URLSchemaContextResolver.class);
        Mockito.doReturn(Optional.of(context)).when(mockedContextResolver).getSchemaContext();

        final Field contextResolverField = schemaService.getClass().getDeclaredField("contextResolver");
        contextResolverField.setAccessible(true);

        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(contextResolverField, contextResolverField.getModifiers() & ~Modifier.FINAL);

        contextResolverField.set(schemaService, mockedContextResolver);
    }

    protected List<ModuleFactory> getModuleFactories() {
        return Lists.newArrayList(new RIBImplModuleFactory(), new GlobalEventExecutorModuleFactory(),
                new BGPDispatcherImplModuleFactory(), new NettyThreadgroupModuleFactory(),
                new TimedReconnectStrategyFactoryModuleFactory(), new SimpleBGPExtensionProviderContextModuleFactory(),
                new RIBExtensionsImplModuleFactory(), new DomBrokerImplModuleFactory(), new RuntimeMappingModuleFactory(),
                new HashedWheelTimerModuleFactory(), new BindingAsyncDataBrokerImplModuleFactory(),
                new DomInmemoryDataBrokerModuleFactory(), new SchemaServiceImplSingletonModuleFactory());
    }

    @Override
    protected BundleContextServiceRegistrationHandler getBundleContextServiceRegistrationHandler(final Class<?> serviceType) {
        if (serviceType.equals(SchemaContextListener.class)) {
            return new BundleContextServiceRegistrationHandler() {
                @Override
                public void handleServiceRegistration(final Class<?> clazz, final Object serviceInstance, final Dictionary<String, ?> props) {
                    SchemaContextListener listener = (SchemaContextListener) serviceInstance;
                    YangContextParser parser = new YangParserImpl();
                    final SchemaContext context;
                    try {
                        context = parser.parseSources(getFilesAsByteSources(getYangModelsPaths()));
                    } catch (IOException | YangSyntaxErrorException e) {
                        throw new IllegalStateException("Failed to parse models", e);
                    }
                    listener.onGlobalContextUpdated(context);
                }
            };
        }

        return super.getBundleContextServiceRegistrationHandler(serviceType);
    }

    @After
    public void closeAllModules() throws Exception {
        super.destroyAllConfigBeans();
        GlobalBundleScanningSchemaServiceImpl.destroyInstance();

    }

    protected CommitStatus createInstance() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createRIBImplModuleInstance(transaction);
        return transaction.commit();
    }

    protected CommitStatus createRIBImplModuleInstance(final RibId ribId, final Long localAs, final Ipv4Address bgpId) throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createRIBImplModuleInstance(transaction, ribId, localAs, bgpId, createAsyncDataBrokerInstance(transaction));
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
        return createRIBImplModuleInstance(transaction, RIB_ID, 5000L, BGP_ID,
                createAsyncDataBrokerInstance(transaction));
    }

    public ObjectName createRIBImplModuleInstance(final ConfigTransactionJMXClient transaction, final ObjectName dataBroker)
            throws Exception {
        return createRIBImplModuleInstance(transaction, RIB_ID, 5000L, BGP_ID, dataBroker);
    }

    public ObjectName createAsyncDataBrokerInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException, InstanceNotFoundException {
        final ObjectName nameCreated = transaction.createModule(BindingAsyncDataBrokerImplModuleFactory.NAME, BINDING_ASYNC_BROKER_INSTANCE_NAME);
        final BindingAsyncDataBrokerImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BindingAsyncDataBrokerImplModuleMXBean.class);
        mxBean.setBindingMappingService(lookupMappingServiceInstance(transaction));
        mxBean.setDomAsyncBroker(lookupDomBrokerInstance(transaction));
        return nameCreated;
    }

    private static ObjectName createDomAsyncDataBroker(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(DomInmemoryDataBrokerModuleFactory.NAME, DOM_ASYNC_DATA_BROKER_INSTANCE);
        final DomInmemoryDataBrokerModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, DomInmemoryDataBrokerModuleMXBean.class);
        mxBean.setSchemaService(lookupSchemaServiceInstance(transaction));
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

    private static ObjectName lookupSchemaServiceInstance(final ConfigTransactionJMXClient transaction) {
        try {
            return transaction.lookupConfigBean(SchemaServiceImplSingletonModuleFactory.NAME, SchemaServiceImplSingletonModuleFactory.SINGLETON_NAME);
        } catch (InstanceNotFoundException e) {
            try {
                return transaction.createModule(SchemaServiceImplSingletonModuleFactory.NAME, SchemaServiceImplSingletonModuleFactory.SINGLETON_NAME);
            } catch (InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    public static ObjectName lookupDomBrokerInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        try {
            return transaction.lookupConfigBean(DomBrokerImplModuleFactory.NAME, DOM_BROKER_INSTANCE_NAME);
        } catch (InstanceNotFoundException e) {
            try {
                final ObjectName nameCreated = transaction.createModule(DomBrokerImplModuleFactory.NAME, DOM_BROKER_INSTANCE_NAME);
                final DomBrokerImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, DomBrokerImplModuleMXBean.class);
                mxBean.setAsyncDataBroker(createDomAsyncDataBroker(transaction));
                return nameCreated;
            } catch (InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    private ObjectName createRibExtensionsInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(RIBExtensionsImplModuleFactory.NAME, RIB_EXTENSIONS_INSTANCE_NAME);
        transaction.newMXBeanProxy(nameCreated, RIBExtensionsImplModuleMXBean.class);
        return nameCreated;
    }

    public List<String> getYangModelsPaths() {
        List<String> paths = Lists.newArrayList("/META-INF/yang/bgp-rib.yang", "/META-INF/yang/ietf-inet-types.yang",
                "/META-INF/yang/bgp-message.yang", "/META-INF/yang/bgp-multiprotocol.yang", "/META-INF/yang/bgp-types.yang");
        return paths;
    }

    // TODO move back to AbstractConfigTest
    private static Collection<ByteSource> getFilesAsByteSources(final List<String> paths) {
        final Collection<ByteSource> resources = new ArrayList<>();
        List<String> failedToFind = new ArrayList<>();
        for (String path : paths) {
            URL url = AbstractRIBImplModuleTest.class.getResource(path);
            if (url == null) {
                failedToFind.add(path);
            } else {
                resources.add(Resources.asByteSource(url));
            }
        }
        Assert.assertEquals("Some files were not found", Collections.<String> emptyList(), failedToFind);

        return resources;
    }
}
