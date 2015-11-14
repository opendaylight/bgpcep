/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.programming.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
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
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RpcBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RuntimeMappingModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.SchemaServiceImplSingletonModuleFactory;
import org.opendaylight.controller.config.yang.netty.timer.HashedWheelTimerModuleFactory;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.dom.broker.GlobalBundleScanningSchemaServiceImpl;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final List<ModuleFactory> moduleFactories = getModuleFactories();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, moduleFactories.toArray(new ModuleFactory[moduleFactories.size()])));

        final Filter mockedFilter = mock(Filter.class);

        Mockito.doReturn(new ServiceReference[] {}).when(mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

        final ServiceReference<?> classLoadingStrategySR = mock(ServiceReference.class, "ClassLoadingStrategy");
        final ServiceReference<?> emptyServiceReference = mock(ServiceReference.class, "Empty");

        Mockito.doReturn(mockedFilter).when(mockedContext).createFilter(Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());
        Mockito.doNothing().when(mockedContext).removeServiceListener(any(ServiceListener.class));

        Mockito.doNothing().when(mockedContext).addBundleListener(any(BundleListener.class));
        Mockito.doNothing().when(mockedContext).removeBundleListener(any(BundleListener.class));

        Mockito.doReturn(new Bundle[] {}).when(mockedContext).getBundles();

        Mockito.doReturn(new ServiceReference[] {}).when(mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

        Mockito.doReturn("Class loading stategy reference").when(classLoadingStrategySR).toString();
        Mockito.doReturn("Empty reference").when(emptyServiceReference).toString();

        Mockito.doReturn(emptyServiceReference).when(mockedContext).getServiceReference(any(Class.class));
        Mockito.doReturn(classLoadingStrategySR).when(mockedContext).getServiceReference(GeneratedClassLoadingStrategy.class);

        Mockito.doReturn(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy()).when(mockedContext).getService(classLoadingStrategySR);
        Mockito.doReturn(null).when(mockedContext).getService(emptyServiceReference);

        final GlobalBundleScanningSchemaServiceImpl schemaService = GlobalBundleScanningSchemaServiceImpl.createInstance(this.mockedContext);
        final YangContextParser parser = new YangParserImpl();
        final SchemaContext context = parser.parseSources(getFilesAsByteSources(getYangModelsPaths()));
        final URLSchemaContextResolver mockedContextResolver = Mockito.mock(URLSchemaContextResolver.class);
        Mockito.doReturn(Optional.of(context)).when(mockedContextResolver).getSchemaContext();

        final Field contextResolverField = schemaService.getClass().getDeclaredField("contextResolver");
        contextResolverField.setAccessible(true);

        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(contextResolverField, contextResolverField.getModifiers() & ~Modifier.FINAL);

        contextResolverField.set(schemaService, mockedContextResolver);
    }

    @After
    public void tearDownGlobalBundleScanningSchemaServiceImpl() throws Exception{
        GlobalBundleScanningSchemaServiceImpl.destroyInstance();
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
            return new BundleContextServiceRegistrationHandler() {
                @Override
                public void handleServiceRegistration(final Class<?> clazz, final Object serviceInstance, final Dictionary<String, ?> props) {
                    final SchemaContextListener listener = (SchemaContextListener) serviceInstance;
                    final YangContextParser parser = new YangParserImpl();
                    SchemaContext context;
                    try {
                        context = parser.parseSources(getFilesAsByteSources(getYangModelsPaths()));
                    } catch (final IOException | YangSyntaxErrorException e) {
                        throw new IllegalStateException("Failed to parse models", e);
                    }
                    listener.onGlobalContextUpdated(context);
                }
            };
        }
        return super.getBundleContextServiceRegistrationHandler(serviceType);
    }

    public List<String> getYangModelsPaths() {
        final List<String> paths = Lists.newArrayList("/META-INF/yang/ietf-inet-types-2013-07-15.yang", "/META-INF/yang/programming.yang");
        return paths;
    }

    public List<ModuleFactory> getModuleFactories() {
        return Lists.newArrayList(new InstructionSchedulerImplModuleFactory(), new HashedWheelTimerModuleFactory(),
                new NotificationBrokerImplModuleFactory(), new RpcBrokerImplModuleFactory(), new DomBrokerImplModuleFactory(),
                new RuntimeMappingModuleFactory(), new BindingBrokerImplModuleFactory(), new BindingAsyncDataBrokerImplModuleFactory(),
                new DomInmemoryDataBrokerModuleFactory(), new SchemaServiceImplSingletonModuleFactory(),
                new ForwardedCompatibleDataBrokerImplModuleFactory());
    }

    // TODO move back to AbstractConfigTest
    private static Collection<ByteSource> getFilesAsByteSources(final List<String> paths) {
        final Collection<ByteSource> resources = new ArrayList<>();
        final List<String> failedToFind = new ArrayList<>();
        for (final String path : paths) {
            final URL url = AbstractInstructionSchedulerTest.class.getResource(path);
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
