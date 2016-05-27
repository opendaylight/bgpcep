/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.CheckedFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.SucceededFuture;
import java.io.IOException;
import java.net.InetSocketAddress;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleFactory;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingAsyncDataBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingAsyncDataBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.ForwardedCompatibleDataBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.ForwardedCompatibleDataBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.NotificationBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RuntimeMappingModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.SchemaServiceImplSingletonModuleFactory;
import org.opendaylight.controller.config.yang.netty.eventexecutor.AutoCloseableEventExecutor;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NioEventLoopGroupCloseable;
import org.opendaylight.controller.config.yang.netty.timer.HashedWheelTimerModuleFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodecFactory;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.YangTextSourceProvider;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
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
    protected static final BgpId BGP_ID = new BgpId("192.168.1.1");
    protected static final ClusterIdentifier CLUSTER_ID = new ClusterIdentifier("192.168.1.2");

    private static final AsNumber AS_NUMBER = new AsNumber(5000L);
    private static final String SESSION_RS_INSTANCE_NAME = "session-reconnect-strategy-factory";
    private static final String TCP_RS_INSTANCE_NAME = "tcp-reconnect-strategy-factory";
    private static final String RIB_EXTENSIONS_INSTANCE_NAME = "rib-extensions-impl";
    private static final String DOM_BROKER_INSTANCE_NAME = "dom-broker-impl";
    private static final String BINDING_ASYNC_BROKER_INSTANCE_NAME = "binding-async-broker-instance";
    private static final String DOM_ASYNC_DATA_BROKER_INSTANCE = "dom-inmemory-data-broker";
    private static final String BINDING_BROKER_INSTANCE_NAME = "binding-broker-impl";
    private static final String COMPATIBLE_DATA_BROKER_INSTANCE_NAME = "binding-data-compatible-broker-instance";
    private static final String NOTIFICATION_BROKER_INSTANCE_NAME = "notification-broker-impl";

    @Mock
    private ReadWriteTransaction mockedTransaction;

    @Mock
    private DataBroker mockedDataProvider;

    @Mock
    private CheckedFuture<Void, TransactionCommitFailedException> mockedFuture;

    @Mock
    private RpcResult<TransactionStatus> mockedResult;

    @Mock
    protected BGPDispatcher mockedBGPDispatcher;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final List<ModuleFactory> moduleFactories = getModuleFactories();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext, moduleFactories.toArray(new ModuleFactory[moduleFactories.size()])));

        doAnswer(new Answer<Filter>() {
            @Override
            public Filter answer(final InvocationOnMock invocation) {
                final String str = invocation.getArgumentAt(0, String.class);
                final Filter mockFilter = mock(Filter.class);
                doReturn(str).when(mockFilter).toString();
                return mockFilter;
            }
        }).when(mockedContext).createFilter(anyString());

        final ServiceReference<?> emptyServiceReference = mock(ServiceReference.class, "Empty");
        final ServiceReference<?> classLoadingStrategySR = mock(ServiceReference.class, "ClassLoadingStrategy");
        final ServiceReference<?> dataProviderServiceReference = mock(ServiceReference.class, "Data Provider");

        Mockito.doNothing().when(this.mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());
        Mockito.doNothing().when(this.mockedContext).removeServiceListener(any(ServiceListener.class));

        Mockito.doNothing().when(this.mockedContext).addBundleListener(any(BundleListener.class));
        Mockito.doNothing().when(this.mockedContext).removeBundleListener(any(BundleListener.class));

        Mockito.doReturn(new Bundle[] {}).when(this.mockedContext).getBundles();

        Mockito.doReturn(new ServiceReference[] {}).when(this.mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

        Mockito.doReturn("Empty reference").when(emptyServiceReference).toString();
        Mockito.doReturn("Data Provider Service Reference").when(dataProviderServiceReference).toString();
        Mockito.doReturn("Class loading stategy reference").when(classLoadingStrategySR).toString();

        Mockito.doReturn(emptyServiceReference).when(this.mockedContext).getServiceReference(any(Class.class));
        Mockito.doReturn(dataProviderServiceReference).when(this.mockedContext).getServiceReference(DataBroker.class);
        Mockito.doReturn(classLoadingStrategySR).when(this.mockedContext).getServiceReference(GeneratedClassLoadingStrategy.class);
        Mockito.doReturn(classLoadingStrategySR).when(this.mockedContext).getServiceReference(ClassLoadingStrategy.class);

        Mockito.doReturn(this.mockedDataProvider).when(this.mockedContext).getService(dataProviderServiceReference);
        Mockito.doReturn(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy()).when(this.mockedContext).getService(classLoadingStrategySR);
        Mockito.doReturn(null).when(this.mockedContext).getService(emptyServiceReference);

        Mockito.doReturn(this.mockedTransaction).when(this.mockedDataProvider).newReadWriteTransaction();

        Mockito.doReturn(null).when(this.mockedTransaction).read(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
        Mockito.doNothing().when(this.mockedTransaction).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        Mockito.doNothing().when(this.mockedTransaction).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));

        Mockito.doReturn(this.mockedFuture).when(this.mockedTransaction).submit();
        Mockito.doReturn(TRANSACTION_NAME).when(this.mockedTransaction).getIdentifier();

        Mockito.doReturn(null).when(this.mockedFuture).get();

        final SchemaContext context = parseYangStreams(getFilesAsByteSources(getYangModelsPaths()));
        final SchemaService mockedSchemaService = mock(SchemaService.class);
        doReturn(context).when(mockedSchemaService).getGlobalContext();
        doAnswer(new Answer<ListenerRegistration<SchemaContextListener>>() {
            @Override
            public ListenerRegistration<SchemaContextListener> answer(InvocationOnMock invocation) {
                invocation.getArgumentAt(0, SchemaContextListener.class).onGlobalContextUpdated(context);
                ListenerRegistration<SchemaContextListener> reg = mock(ListenerRegistration.class);
                doNothing().when(reg).close();
                return reg;
            }
        }).when(mockedSchemaService).registerSchemaContextListener(any(SchemaContextListener.class));

        setupMockService(SchemaService.class, mockedSchemaService);
        setupMockService(YangTextSourceProvider.class, mock(YangTextSourceProvider.class));

        BindingToNormalizedNodeCodecFactory.getOrCreateInstance(
                GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(), mockedSchemaService);

        BGPExtensionProviderContext mockContext = mock(BGPExtensionProviderContext.class);
        doReturn(mock(MessageRegistry.class)).when(mockContext).getMessageRegistry();
        setupMockService(BGPExtensionProviderContext.class, mockContext);

        setupMockService(EventLoopGroup.class, NioEventLoopGroupCloseable.newInstance(0));
        setupMockService(EventExecutor.class, AutoCloseableEventExecutor.CloseableEventExecutorMixin.globalEventExecutor());

        setupMockService(DOMNotificationService.class, mock(DOMNotificationService.class));
        setupMockService(DOMNotificationPublishService.class, mock(DOMNotificationPublishService.class));
        setupMockService(DOMRpcService.class, mock(DOMRpcService.class));
        setupMockService(DOMRpcProviderService.class, mock(DOMRpcProviderService.class));
        setupMockService(DOMMountPointService.class, mock(DOMMountPointService.class));

        setupMockService(BGPDispatcher.class, mockedBGPDispatcher);
        doReturn(new SucceededFuture<>(ImmediateEventExecutor.INSTANCE, null)).when(mockedBGPDispatcher).createReconnectingClient(
                any(InetSocketAddress.class), any(BGPPeerRegistry.class), anyInt(), any(Optional.class));

        setupMockService(RIBExtensionProviderContext.class, new SimpleRIBExtensionProviderContext());

        setupMockService(BGPPeerRegistry.class, StrictBGPPeerRegistry.GLOBAL);
    }

    protected void setupMockService(final Class<?> serviceInterface, final Object instance) throws Exception {
        final ServiceReference<?> mockServiceRef = mock(ServiceReference.class);
        doReturn(new ServiceReference[]{mockServiceRef}).when(mockedContext).
                getServiceReferences(anyString(), contains(serviceInterface.getName()));
        doReturn(new ServiceReference[]{mockServiceRef}).when(mockedContext).
                getServiceReferences(serviceInterface.getName(), null);
        doReturn(instance).when(mockedContext).getService(mockServiceRef);
    }

    private static SchemaContext parseYangStreams(final Collection<ByteSource> streams) {
        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR
                .newBuild();
        try {
            return reactor.buildEffective(streams);
        } catch (final ReactorException | IOException e) {
            throw new RuntimeException("Unable to build schema context from " + streams, e);
        }
    }

    protected List<ModuleFactory> getModuleFactories() {
        return Lists.newArrayList(new RIBImplModuleFactory(), new GlobalEventExecutorModuleFactory(),
                new BGPDispatcherImplModuleFactory(), new NettyThreadgroupModuleFactory(),
                new RIBExtensionsImplModuleFactory(), new DomBrokerImplModuleFactory(), new RuntimeMappingModuleFactory(),
                new HashedWheelTimerModuleFactory(), new BindingAsyncDataBrokerImplModuleFactory(),
                new DomInmemoryDataBrokerModuleFactory(), new SchemaServiceImplSingletonModuleFactory(),
                new NotificationBrokerImplModuleFactory(), new ForwardedCompatibleDataBrokerImplModuleFactory(),
                new BindingBrokerImplModuleFactory());
    }

    @Override
    protected BundleContextServiceRegistrationHandler getBundleContextServiceRegistrationHandler(final Class<?> serviceType) {
        if (serviceType.equals(SchemaContextListener.class)) {
            return new BundleContextServiceRegistrationHandler() {
                @Override
                public void handleServiceRegistration(final Class<?> clazz, final Object serviceInstance, final Dictionary<String, ?> props) {
                    final SchemaContextListener listener = (SchemaContextListener) serviceInstance;
                    final SchemaContext context = parseYangStreams(getFilesAsByteSources(getYangModelsPaths()));
                    listener.onGlobalContextUpdated(context);
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
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createRIBImplModuleInstance(transaction);
        return transaction.commit();
    }

    protected CommitStatus createRIBImplModuleInstance(final RibId ribId, final AsNumber localAs, final BgpId bgpId, final ClusterIdentifier clusterId) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createRIBImplModuleInstance(transaction, ribId, localAs, bgpId, clusterId, createAsyncDataBrokerInstance(transaction));
        return transaction.commit();
    }

    private ObjectName createRIBImplModuleInstance(final ConfigTransactionJMXClient transaction, final RibId ribId, final AsNumber localAs,
            final BgpId bgpId, final ClusterIdentifier clusterId, final ObjectName dataBroker) throws Exception {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final RIBImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, RIBImplModuleMXBean.class);
        mxBean.setDataProvider(dataBroker);
        mxBean.setDomDataProvider(lookupDomAsyncDataBroker(transaction));
        mxBean.setCodecTreeFactory(lookupMappingServiceInstance(transaction));
        mxBean.setBgpDispatcher(createBGPDispatcherImplInstance(transaction));
        mxBean.setExtensions(createRibExtensionsInstance(transaction));
        mxBean.setRibId(ribId);
        mxBean.setLocalAs(localAs);
        mxBean.setBgpRibId(bgpId);
        mxBean.setClusterId(clusterId);
        return nameCreated;
    }

    public static ObjectName createBGPDispatcherImplInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(BGPDispatcherImplModuleFactory.NAME, "bgp-message-fct");
        final BGPDispatcherImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BGPDispatcherImplModuleMXBean.class);
        return nameCreated;
    }

    protected ObjectName createRIBImplModuleInstance(final ConfigTransactionJMXClient transaction) throws Exception {
        return createRIBImplModuleInstance(transaction, RIB_ID, AS_NUMBER, BGP_ID, CLUSTER_ID,
                createAsyncDataBrokerInstance(transaction));
    }

    public ObjectName createRIBImplModuleInstance(final ConfigTransactionJMXClient transaction, final ObjectName dataBroker)
            throws Exception {
        return createRIBImplModuleInstance(transaction, RIB_ID, AS_NUMBER, BGP_ID, CLUSTER_ID, dataBroker);
    }

    public ObjectName createAsyncDataBrokerInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException, InstanceNotFoundException {
        final ObjectName nameCreated = transaction.createModule(BindingAsyncDataBrokerImplModuleFactory.NAME, BINDING_ASYNC_BROKER_INSTANCE_NAME);
        final BindingAsyncDataBrokerImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BindingAsyncDataBrokerImplModuleMXBean.class);
        mxBean.setBindingMappingService(lookupMappingServiceInstance(transaction));
        mxBean.setDomAsyncBroker(lookupDomAsyncDataBroker(transaction));
        mxBean.setSchemaService(lookupSchemaServiceInstance(transaction));
        return nameCreated;
    }

    public static ObjectName lookupDomAsyncDataBroker(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        try {
            return transaction.lookupConfigBean(DomInmemoryDataBrokerModuleFactory.NAME, DOM_BROKER_INSTANCE_NAME);
        } catch (final InstanceNotFoundException e) {
            try {
                final ObjectName nameCreated = transaction.createModule(DomInmemoryDataBrokerModuleFactory.NAME, DOM_BROKER_INSTANCE_NAME);
                final DomInmemoryDataBrokerModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, DomInmemoryDataBrokerModuleMXBean.class);
                mxBean.setSchemaService(lookupSchemaServiceInstance(transaction));
                return nameCreated;
            } catch (final InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    private static ObjectName lookupMappingServiceInstance(final ConfigTransactionJMXClient transaction) {
        try {
            return transaction.lookupConfigBean(RuntimeMappingModuleFactory.NAME, RuntimeMappingModuleFactory.SINGLETON_NAME);
        } catch (final InstanceNotFoundException e) {
            try {
                return transaction.createModule(RuntimeMappingModuleFactory.NAME, RuntimeMappingModuleFactory.SINGLETON_NAME);
            } catch (final InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    private static ObjectName lookupSchemaServiceInstance(final ConfigTransactionJMXClient transaction) {
        try {
            return transaction.lookupConfigBean(SchemaServiceImplSingletonModuleFactory.NAME, SchemaServiceImplSingletonModuleFactory.SINGLETON_NAME);
        } catch (final InstanceNotFoundException e) {
            try {
                return transaction.createModule(SchemaServiceImplSingletonModuleFactory.NAME, SchemaServiceImplSingletonModuleFactory.SINGLETON_NAME);
            } catch (final InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    private static ObjectName createRibExtensionsInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(RIBExtensionsImplModuleFactory.NAME, RIB_EXTENSIONS_INSTANCE_NAME);
        transaction.newMXBeanProxy(nameCreated, RIBExtensionsImplModuleMXBean.class);
        return nameCreated;
    }

    public List<String> getYangModelsPaths() {
        final List<String> paths = Lists.newArrayList("/META-INF/yang/bgp-rib.yang", "/META-INF/yang/ietf-inet-types@2013-07-15.yang",
                "/META-INF/yang/bgp-message.yang", "/META-INF/yang/bgp-multiprotocol.yang", "/META-INF/yang/bgp-types.yang",
                "/META-INF/yang/network-concepts.yang", "/META-INF/yang/ieee754.yang", "/META-INF/yang/yang-ext.yang");
        return paths;
    }

    // TODO move back to AbstractConfigTest
    private static Collection<ByteSource> getFilesAsByteSources(final List<String> paths) {
        final Collection<ByteSource> resources = new ArrayList<>();
        final List<String> failedToFind = new ArrayList<>();
        for (final String path : paths) {
            final URL url = AbstractRIBImplModuleTest.class.getResource(path);
            if (url == null) {
                failedToFind.add(path);
            } else {
                resources.add(Resources.asByteSource(url));
            }
        }
        Assert.assertEquals("Some files were not found", Collections.<String> emptyList(), failedToFind);

        return resources;
    }

    public ObjectName createBindingBrokerImpl(final ConfigTransactionJMXClient transaction, final ObjectName dataBrokerON,
        final ObjectName notificationBrokerON) throws Exception {
        final ObjectName objectName = transaction.createModule(BindingBrokerImplModuleFactory.NAME, BINDING_BROKER_INSTANCE_NAME);
        final BindingBrokerImplModuleMXBean mxBean = transaction.newMXBeanProxy(objectName, BindingBrokerImplModuleMXBean.class);
        mxBean.setDataBroker(dataBrokerON);
        mxBean.setNotificationService(notificationBrokerON);
        mxBean.setBindingMappingService(lookupMappingServiceInstance(transaction));
        mxBean.setDomAsyncBroker(lookupDomBrokerInstance(transaction));
        return objectName;
    }

    public static ObjectName lookupDomBrokerInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        try {
            return transaction.lookupConfigBean(DomBrokerImplModuleFactory.NAME, DOM_BROKER_INSTANCE_NAME);
        } catch (final InstanceNotFoundException e) {
            try {
                final ObjectName nameCreated = transaction.createModule(DomBrokerImplModuleFactory.NAME, DOM_BROKER_INSTANCE_NAME);
                final DomBrokerImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, DomBrokerImplModuleMXBean.class);
                mxBean.setAsyncDataBroker(lookupDomAsyncDataBroker(transaction));
                return nameCreated;
            } catch (final InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    public ObjectName createCompatibleDataBrokerInstance(final ConfigTransactionJMXClient transaction)
        throws InstanceAlreadyExistsException, InstanceNotFoundException {
        final ObjectName nameCreated = transaction.createModule(ForwardedCompatibleDataBrokerImplModuleFactory.NAME, COMPATIBLE_DATA_BROKER_INSTANCE_NAME);
        final ForwardedCompatibleDataBrokerImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, ForwardedCompatibleDataBrokerImplModuleMXBean.class);
        mxBean.setDataBroker(lookupDataBrokerInstance(transaction));
        return nameCreated;
    }

    private static ObjectName lookupDataBrokerInstance(final ConfigTransactionJMXClient transaction) {
        try {
            return transaction.lookupConfigBean(BindingAsyncDataBrokerImplModuleFactory.NAME, BINDING_ASYNC_BROKER_INSTANCE_NAME);
        } catch (final InstanceNotFoundException e) {
            try {
                return transaction.createModule(RuntimeMappingModuleFactory.NAME, RuntimeMappingModuleFactory.SINGLETON_NAME);
            } catch (final InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    public ObjectName createNotificationBrokerInstance(final ConfigTransactionJMXClient transaction) throws Exception {
        final ObjectName objectName = transaction.createModule(NotificationBrokerImplModuleFactory.NAME, NOTIFICATION_BROKER_INSTANCE_NAME);
        return objectName;
    }
}
