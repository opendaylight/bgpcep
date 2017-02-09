/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.programming.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.Timer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;
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
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
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
import org.opendaylight.controller.config.yang.netty.timer.HashedWheelTimerModuleFactory;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodecFactory;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.YangTextSourceProvider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.parser.repo.YangTextSchemaContextResolver;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public abstract class AbstractInstructionSchedulerTest extends AbstractConfigTest {

    private static final String FACTORY_NAME = InstructionSchedulerImplModuleFactory.NAME;
    private static final String INSTANCE_NAME = "instruction-scheduler-impl";

    private static final String BINDING_BROKER_INSTANCE_NAME = "binding-broker-impl";
    private static final String NOTIFICATION_BROKER_INSTANCE_NAME = "notification-broker-impl";
    private static final String COMPATIBLE_DATA_BROKER_INSTANCE_NAME = "binding-data-compatible-broker-instance";
    private static final String DOM_BROKER_INSTANCE_NAME = "dom-broker-impl";
    private static final String TIMER_INSTANCE_NAME = "timer-impl";
    private static final String BINDING_ASYNC_BROKER_INSTANCE_NAME = "binding-async-broker-instance";
    private static final String DOM_ASYNC_DATA_BROKER_INSTANCE = "dom-inmemory-data-broker";

    @Mock
    private RpcResult<TransactionStatus> mockedResult;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final List<ModuleFactory> moduleFactories = getModuleFactories();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext, moduleFactories.toArray(new ModuleFactory[moduleFactories.size()])));

        doAnswer(invocation -> {
            final String str = invocation.getArgumentAt(0, String.class);
            final Filter mockFilter = mock(Filter.class);
            doReturn(str).when(mockFilter).toString();
            return mockFilter;
        }).when(this.mockedContext).createFilter(anyString());

        Mockito.doReturn(new ServiceReference[] {}).when(this.mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

        final ServiceReference<?> classLoadingStrategySR = mock(ServiceReference.class, "ClassLoadingStrategy");
        final ServiceReference<?> emptyServiceReference = mock(ServiceReference.class, "Empty");

        Mockito.doNothing().when(this.mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());
        Mockito.doNothing().when(this.mockedContext).removeServiceListener(any(ServiceListener.class));

        Mockito.doNothing().when(this.mockedContext).addBundleListener(any(BundleListener.class));
        Mockito.doNothing().when(this.mockedContext).removeBundleListener(any(BundleListener.class));

        Mockito.doReturn(new Bundle[] {}).when(this.mockedContext).getBundles();

        Mockito.doReturn(new ServiceReference[] {}).when(this.mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

        Mockito.doReturn("Class loading stategy reference").when(classLoadingStrategySR).toString();
        Mockito.doReturn("Empty reference").when(emptyServiceReference).toString();

        Mockito.doReturn(emptyServiceReference).when(this.mockedContext).getServiceReference(any(Class.class));
        Mockito.doReturn(classLoadingStrategySR).when(this.mockedContext).getServiceReference(GeneratedClassLoadingStrategy.class);
        Mockito.doReturn(classLoadingStrategySR).when(this.mockedContext).getServiceReference(ClassLoadingStrategy.class);

        Mockito.doReturn(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy()).when(this.mockedContext).getService(classLoadingStrategySR);
        Mockito.doReturn(null).when(this.mockedContext).getService(emptyServiceReference);

        final SchemaContext context = parseYangStreams(getFilesAsStreams(getYangModelsPaths()));
        final SchemaService mockedSchemaService = mock(SchemaService.class);
        doReturn(context).when(mockedSchemaService).getGlobalContext();
        doAnswer(invocation -> {
            invocation.getArgumentAt(0, SchemaContextListener.class).onGlobalContextUpdated(context);
            ListenerRegistration<SchemaContextListener> reg = mock(ListenerRegistration.class);
            doNothing().when(reg).close();
            return reg;
        }).when(mockedSchemaService).registerSchemaContextListener(any(SchemaContextListener.class));

        setupMockService(SchemaService.class, mockedSchemaService);
        setupMockService(YangTextSourceProvider.class, mock(YangTextSourceProvider.class));

        BindingToNormalizedNodeCodec bindingCodec = BindingToNormalizedNodeCodecFactory.newInstance(
                GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
        BindingToNormalizedNodeCodecFactory.registerInstance(bindingCodec, mockedSchemaService);
        setupMockService(BindingToNormalizedNodeCodec.class, bindingCodec);

        setupMockService(Timer.class, mock(Timer.class));
        setupMockService(EventLoopGroup.class, new NioEventLoopGroup());
        setupMockService(DOMNotificationService.class, mock(DOMNotificationService.class));
        setupMockService(DOMNotificationPublishService.class, mock(DOMNotificationPublishService.class));
        setupMockService(DOMRpcService.class, mock(DOMRpcService.class));
        setupMockService(DOMMountPointService.class, mock(DOMMountPointService.class));

        final DOMRpcProviderService mockRpcProvider = mock(DOMRpcProviderService.class);
        doReturn(mock(DOMRpcImplementationRegistration.class)).when(mockRpcProvider).registerRpcImplementation(
                any(DOMRpcImplementation.class), any(Set.class));
        setupMockService(DOMRpcProviderService.class, mockRpcProvider);
    }

    protected void setupMockService(final Class<?> serviceInterface, final Object instance) throws Exception {
        final ServiceReference<?> mockServiceRef = mock(ServiceReference.class);
        doReturn(new ServiceReference[]{mockServiceRef}).when(this.mockedContext).
                getServiceReferences(anyString(), contains(serviceInterface.getName()));
        doReturn(new ServiceReference[]{mockServiceRef}).when(this.mockedContext).
                getServiceReferences(serviceInterface.getName(), null);
        doReturn(instance).when(this.mockedContext).getService(mockServiceRef);
    }

    @After
    public void tearDownGlobalBundleScanningSchemaServiceImpl() throws Exception{
    }

    public ObjectName createInstructionSchedulerModuleInstance(final ConfigTransactionJMXClient transaction, final ObjectName dataBrokerON,
            final ObjectName rpcRegistyON, final ObjectName notificationBrokerON) throws Exception {
        final ObjectName objectName = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final InstructionSchedulerImplModuleMXBean mxBean = transaction.newMXBeanProxy(objectName,
                InstructionSchedulerImplModuleMXBean.class);
        mxBean.setDataProvider(dataBrokerON);
        mxBean.setRpcRegistry(rpcRegistyON);
        mxBean.setNotificationService(notificationBrokerON);
        mxBean.setTimer(createTimerInstance(transaction));
        return objectName;
    }

    private ObjectName createTimerInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(HashedWheelTimerModuleFactory.NAME, TIMER_INSTANCE_NAME);
        return nameCreated;

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

    public ObjectName createNotificationBrokerInstance(final ConfigTransactionJMXClient transaction) throws Exception {
        final ObjectName objectName = transaction.createModule(NotificationBrokerImplModuleFactory.NAME, NOTIFICATION_BROKER_INSTANCE_NAME);
        return objectName;
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

    public ObjectName createAsyncDataBrokerInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException, InstanceNotFoundException {
        final ObjectName nameCreated = transaction.createModule(BindingAsyncDataBrokerImplModuleFactory.NAME, BINDING_ASYNC_BROKER_INSTANCE_NAME);
        final BindingAsyncDataBrokerImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BindingAsyncDataBrokerImplModuleMXBean.class);
        mxBean.setBindingMappingService(lookupMappingServiceInstance(transaction));
        mxBean.setDomAsyncBroker(lookupDomAsyncDataBroker(transaction));
        mxBean.setSchemaService(lookupSchemaServiceInstance(transaction));
        return nameCreated;
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

    private static ObjectName lookupDomAsyncDataBroker(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        try {
            return transaction.lookupConfigBean(DomInmemoryDataBrokerModuleFactory.NAME, DOM_ASYNC_DATA_BROKER_INSTANCE);
        } catch (final InstanceNotFoundException e) {
            try {
                final ObjectName nameCreated = transaction.createModule(DomInmemoryDataBrokerModuleFactory.NAME, DOM_ASYNC_DATA_BROKER_INSTANCE);
                final DomInmemoryDataBrokerModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, DomInmemoryDataBrokerModuleMXBean.class);
                mxBean.setSchemaService(lookupSchemaServiceInstance(transaction));
                return nameCreated;
            } catch (final InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    @Override
    protected BundleContextServiceRegistrationHandler getBundleContextServiceRegistrationHandler(final Class<?> serviceType) {
        if (serviceType.equals(SchemaContextListener.class)) {
            return (clazz, serviceInstance, props) -> {
                final SchemaContextListener listener = (SchemaContextListener) serviceInstance;
                final SchemaContext context = parseYangStreams(getFilesAsStreams(getYangModelsPaths()));
                listener.onGlobalContextUpdated(context);
                listener.onGlobalContextUpdated(context);
            };
        }
        return super.getBundleContextServiceRegistrationHandler(serviceType);
    }

    public List<String> getYangModelsPaths() {
        return Lists.newArrayList("/META-INF/yang/ietf-inet-types@2013-07-15.yang", "/META-INF/yang/programming.yang");
    }

    public List<ModuleFactory> getModuleFactories() {
        return Lists.newArrayList(new InstructionSchedulerImplModuleFactory(), new HashedWheelTimerModuleFactory(),
                new NotificationBrokerImplModuleFactory(), new DomBrokerImplModuleFactory(),
                new RuntimeMappingModuleFactory(), new BindingBrokerImplModuleFactory(), new BindingAsyncDataBrokerImplModuleFactory(),
                new DomInmemoryDataBrokerModuleFactory(), new SchemaServiceImplSingletonModuleFactory(),
                new ForwardedCompatibleDataBrokerImplModuleFactory());
    }

    // TODO move back to AbstractConfigTest
    private static List<InputStream> getFilesAsStreams(final List<String> paths) {
        final List<InputStream> resources = new ArrayList<>();
        final List<String> failedToFind = new ArrayList<>();
        for (final String path : paths) {
            final InputStream is = AbstractInstructionSchedulerTest.class.getResourceAsStream(path);
            if (is == null) {
                failedToFind.add(path);
            } else {
                resources.add(is);
            }
        }
        Assert.assertEquals("Some files were not found", Collections.emptyList(), failedToFind);

        return resources;
    }

    private static SchemaContext parseYangStreams(final List<InputStream> streams) {
        try {
            return YangParserTestUtils.parseYangStreams(streams);
        } catch (final ReactorException e) {
            throw new RuntimeException("Unable to build schema context from " + streams, e);
        }
    }

}
