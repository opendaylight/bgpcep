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
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.DataBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.DataBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.NotificationBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RpcBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RuntimeMappingModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.HashMapDataStoreModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.HashMapDataStoreModuleMXBean;
import org.opendaylight.controller.config.yang.netty.timer.HashedWheelTimerModuleFactory;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService.MountProvisionListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
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

public abstract class AbstractInstructionSchedulerTest extends AbstractConfigTest {

    private static final String FACTORY_NAME = InstructionSchedulerImplModuleFactory.NAME;
    private static final String INSTANCE_NAME = "instruction-scheduler-impl";

    private static final String TRANSACTION_NAME = "testTransaction";

    private static final String BINDING_BROKER_INSTANCE_NAME = "binding-broker-impl";
    private static final String NOTIFICATION_BROKER_INSTANCE_NAME = "notification-broker-impl";
    private static final String DATA_BROKER_INSTANCE_NAME = "data-broker-impl";
    private static final String DOM_BROKER_INSTANCE_NAME = "dom-broker-impl";
    private static final String DATA_STORE_INSTANCE_NAME = "data-store-impl";
    private static final String TIMER_INSTANCE_NAME = "timer-impl";

    @Mock
    private DataModificationTransaction mockedTransaction;

    @Mock
    private DataProviderService mockedDataProvider;

    @Mock
    private RpcProvisionRegistry mockedRpcProvision;

    @Mock
    private NotificationPublishService mockedNotificationPublish;

    @Mock
    private MountProvisionService mockedMountProvision;

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
        ServiceReference<?> rpcProvisionServiceReference = mock(ServiceReference.class, "Rpc Provision");
        ServiceReference<?> mountProvisionServiceReference = mock(ServiceReference.class, "Mount Provision");
        ServiceReference<?> notificationPublishServiceReference = mock(ServiceReference.class, "Notification Publish");

        Mockito.doReturn(mockedFilter).when(mockedContext).createFilter(Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());

        Mockito.doNothing().when(mockedContext).addBundleListener(any(BundleListener.class));

        Mockito.doReturn(new Bundle[] {}).when(mockedContext).getBundles();

        Mockito.doReturn(new ServiceReference[] {}).when(mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

        Mockito.doReturn("Empty reference").when(emptyServiceReference).toString();
        Mockito.doReturn("Data Provider Service Reference").when(dataProviderServiceReference).toString();
        Mockito.doReturn("Rpc Provision Service Reference").when(rpcProvisionServiceReference).toString();
        Mockito.doReturn("Mount Provision Service Reference").when(mountProvisionServiceReference).toString();
        Mockito.doReturn("Notification Publish Service Reference").when(notificationPublishServiceReference).toString();
        //
        Mockito.doReturn(emptyServiceReference).when(mockedContext).getServiceReference(any(Class.class));
        Mockito.doReturn(dataProviderServiceReference).when(mockedContext).getServiceReference(DataProviderService.class);
        Mockito.doReturn(rpcProvisionServiceReference).when(mockedContext).getServiceReference(RpcProvisionRegistry.class);
        Mockito.doReturn(notificationPublishServiceReference).when(mockedContext).getServiceReference(NotificationPublishService.class);
        Mockito.doReturn(mountProvisionServiceReference).when(mockedContext).getServiceReference(MountProvisionService.class);

        Mockito.doReturn(mockedDataProvider).when(mockedContext).getService(dataProviderServiceReference);
        Mockito.doReturn(mockedRpcProvision).when(mockedContext).getService(rpcProvisionServiceReference);
        Mockito.doReturn(mockedMountProvision).when(mockedContext).getService(mountProvisionServiceReference);
        Mockito.doReturn(mockedNotificationPublish).when(mockedContext).getService(notificationPublishServiceReference);

        Mockito.doReturn(null).when(mockedContext).getService(emptyServiceReference);

        Registration<DataCommitHandler<InstanceIdentifier, CompositeNode>> registration = mock(Registration.class);
        Mockito.doReturn(registration).when(mockedDataProvider).registerCommitHandler(any(InstanceIdentifier.class),
                any(DataCommitHandler.class));
        Mockito.doReturn(registration).when(mockedDataProvider).registerCommitHandler(any(InstanceIdentifier.class),
                any(DataCommitHandler.class));

        Mockito.doReturn(null).when(mockedMountProvision).registerProvisionListener(any(MountProvisionListener.class));

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

    public ObjectName createInstructionSchedulerModuleInstance(final ConfigTransactionJMXClient transaction, final ObjectName dataBrokerON,
            final ObjectName rpcRegistyON, final ObjectName notificationBrokerON) throws Exception {
        final ObjectName objectName = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final InstructionSchedulerImplModuleMXBean mxBean = transaction.newMBeanProxy(objectName,
                InstructionSchedulerImplModuleMXBean.class);
        mxBean.setDataProvider(dataBrokerON);
        mxBean.setRpcRegistry(rpcRegistyON);
        mxBean.setNotificationService(notificationBrokerON);
        mxBean.setTimer(createTimerInstance(transaction));
        return objectName;
    }

    private ObjectName createTimerInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(HashedWheelTimerModuleFactory.NAME, TIMER_INSTANCE_NAME);
        return nameCreated;

    }

    public ObjectName createBindingBrokerImpl(final ConfigTransactionJMXClient transaction, final ObjectName dataBrokerON,
            final ObjectName notificationBrokerON) throws Exception {
        final ObjectName objectName = transaction.createModule(BindingBrokerImplModuleFactory.NAME, BINDING_BROKER_INSTANCE_NAME);
        final BindingBrokerImplModuleMXBean mxBean = transaction.newMBeanProxy(objectName, BindingBrokerImplModuleMXBean.class);
        mxBean.setDataBroker(dataBrokerON);
        mxBean.setNotificationService(notificationBrokerON);
        return objectName;
    }

    public ObjectName createNotificationBrokerInstance(final ConfigTransactionJMXClient transaction) throws Exception {
        final ObjectName objectName = transaction.createModule(NotificationBrokerImplModuleFactory.NAME, NOTIFICATION_BROKER_INSTANCE_NAME);
        return objectName;
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

    public List<String> getYangModelsPaths() {
        List<String> paths = Lists.newArrayList("/META-INF/yang/ietf-inet-types.yang", "/META-INF/yang/programming.yang");
        return paths;
    }

    public List<ModuleFactory> getModuleFactories() {
        return Lists.newArrayList(new InstructionSchedulerImplModuleFactory(), new HashedWheelTimerModuleFactory(),
                new NotificationBrokerImplModuleFactory(), new RpcBrokerImplModuleFactory(), new DataBrokerImplModuleFactory(),
                new DomBrokerImplModuleFactory(), new HashMapDataStoreModuleFactory(), new RuntimeMappingModuleFactory(),
                new BindingBrokerImplModuleFactory());
    }

    // TODO move back to AbstractConfigTest
    private static Collection<InputStream> getFilesAsInputStreams(List<String> paths) {
        final Collection<InputStream> resources = new ArrayList<>();
        List<String> failedToFind = new ArrayList<>();
        for (String path : paths) {
            InputStream resourceAsStream = AbstractInstructionSchedulerTest.class.getResourceAsStream(path);
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
