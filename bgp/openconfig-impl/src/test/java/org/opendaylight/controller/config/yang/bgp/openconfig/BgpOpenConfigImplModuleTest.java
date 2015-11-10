/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.openconfig;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.CheckedFuture;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingAsyncDataBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingAsyncDataBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.ForwardedCompatibleDataBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.ForwardedCompatibleDataBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.NotificationBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.NotificationBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RuntimeMappingModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RuntimeMappingModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.SchemaServiceImplSingletonModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.SchemaServiceImplSingletonModuleMXBean;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.dom.broker.GlobalBundleScanningSchemaServiceImpl;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.opendaylight.yangtools.yang.parser.repo.URLSchemaContextResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class BgpOpenConfigImplModuleTest  extends AbstractConfigTest {
    private static final String INSTANCE_NAME = "bgp-openconf-impl-instance";
    private static final String FACTORY_NAME = BgpOpenConfigImplModuleFactory.NAME;
    private static final String BINDING_BROKER_INSTANCE_NAME = "binding-broker-inst";
    private static final String NOTIF_SERVICE_INSTANCE_NAME = "notif-service-inst";
    private static final String RUNTIME_MAPPING_INSTANCE_NAME = "runtime-mapping-singleton";
    private static final String DOM_ASYNC_BROKER_INSTANCE_NAME = "dom-async-broker-inst";
    private static final String FORWARDED_DATA_BROKER_INSTANCE_NAME = "forwarded-data-broker-inst";
    private static final String FORWARDED_SUB_DATA_BROKER_INSTANCE_NAME = "forwarded-sub-data-broker-inst";
    private static final String FORWARDED_SUB_ASYNC_BROKER_INSTANCE_NAME = "forwarded-sub-async-broker-inst";
    private static final String ASYNC_DATA_BROKER_INSTANCE_NAME = "async-data-broker-inst";
    private static final String SCHEMA_SERVICE_INSTANCE_NAME = "yang-schema-service";
    private static final String ROOT_ASYNC_BROKER_INSTANCE_NAME = "root-async-broker-inst";
    private static final String ROOT_SUB_ASYNC_BROKER_INSTANCE_NAME = "root-sub-async-broker-inst";

    private ObjectName runtimeMappingName;
    private ObjectName schemaService;

    private static final String TRANSACTION_NAME = "testTransaction";
    @Mock private ReadWriteTransaction mockedTransaction;
    @Mock private DataBroker mockedDataProvider;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> mockedFuture;
    @Mock private RpcResult<TransactionStatus> mockedResult;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext,
            new BgpOpenConfigImplModuleFactory(),
            new BindingBrokerImplModuleFactory(),
            new NotificationBrokerImplModuleFactory(),
            new RuntimeMappingModuleFactory(),
            new DomBrokerImplModuleFactory(),
            new DomInmemoryDataBrokerModuleFactory(),
            new BindingAsyncDataBrokerImplModuleFactory(),
            new SchemaServiceImplSingletonModuleFactory(),
            new ForwardedCompatibleDataBrokerImplModuleFactory()
        ));
        final Filter mockedFilter = mock(Filter.class);
        Mockito.doReturn(mockedFilter).when(this.mockedContext).createFilter(Mockito.anyString());

        final ServiceReference<?> emptyServiceReference = mock(ServiceReference.class, "Empty");
        final ServiceReference<?> classLoadingStrategySR = mock(ServiceReference.class, "ClassLoadingStrategy");
        final ServiceReference<?> dataProviderServiceReference = mock(ServiceReference.class, "Data Provider");
        final ServiceReference<?> schemaServiceReference = mock(ServiceReference.class, "schemaServiceReference");

        Mockito.doReturn(mockedFilter).when(this.mockedContext).createFilter(Mockito.anyString());

        Mockito.doNothing().when(this.mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());
        Mockito.doNothing().when(this.mockedContext).removeServiceListener(any(ServiceListener.class));

        Mockito.doNothing().when(this.mockedContext).addBundleListener(any(BundleListener.class));
        Mockito.doNothing().when(this.mockedContext).removeBundleListener(any(BundleListener.class));

        Mockito.doReturn(new Bundle[] {}).when(this.mockedContext).getBundles();

        Mockito.doReturn(new ServiceReference[] {}).when(this.mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

        Mockito.doReturn("Empty reference").when(emptyServiceReference).toString();
        Mockito.doReturn("Data Provider Service Reference").when(dataProviderServiceReference).toString();
        Mockito.doReturn("Class loading stategy reference").when(classLoadingStrategySR).toString();
        Mockito.doReturn("Schema Service reference").when(schemaServiceReference).toString();

        Mockito.doReturn(emptyServiceReference).when(this.mockedContext).getServiceReference(any(Class.class));
        Mockito.doReturn(dataProviderServiceReference).when(this.mockedContext).getServiceReference(DataBroker.class);
        Mockito.doReturn(classLoadingStrategySR).when(this.mockedContext).getServiceReference(GeneratedClassLoadingStrategy.class);
        Mockito.doReturn(schemaServiceReference).when(this.mockedContext).getServiceReference(SchemaService.class);

        Mockito.doReturn(this.mockedDataProvider).when(this.mockedContext).getService(dataProviderServiceReference);
        Mockito.doReturn(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy()).when(this.mockedContext).getService(classLoadingStrategySR);
        Mockito.doReturn(null).when(this.mockedContext).getService(emptyServiceReference);
        final GlobalBundleScanningSchemaServiceImpl schemaService = GlobalBundleScanningSchemaServiceImpl.createInstance(this.mockedContext);
        Mockito.doReturn(schemaService).when(this.mockedContext).getService(schemaServiceReference);

        Mockito.doReturn(this.mockedTransaction).when(this.mockedDataProvider).newReadWriteTransaction();

        Mockito.doReturn(null).when(this.mockedTransaction).read(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
        Mockito.doNothing().when(this.mockedTransaction).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(DataObject.class));
        Mockito.doNothing().when(this.mockedTransaction).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));

        Mockito.doReturn(this.mockedFuture).when(this.mockedTransaction).submit();
        Mockito.doReturn(TRANSACTION_NAME).when(this.mockedTransaction).getIdentifier();

        Mockito.doReturn(null).when(this.mockedFuture).get();

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

    private List<String> getYangModelsPaths() {
        final List<String> paths = Lists.newArrayList("/META-INF/yang/bgp-rib.yang", "/META-INF/yang/ietf-inet-types.yang",
                "/META-INF/yang/bgp-message.yang", "/META-INF/yang/bgp-multiprotocol.yang", "/META-INF/yang/bgp-types.yang",
                "/META-INF/yang/network-concepts.yang", "/META-INF/yang/ieee754.yang", "/META-INF/yang/yang-ext.yang",
                "/META-INF/yang/bmp-monitor.yang", "/META-INF/yang/bmp-message.yang", "/META-INF/yang/ietf-yang-types.yang");
        return paths;
    }

    private Collection<ByteSource> getFilesAsByteSources(final List<String> paths) {
        final Collection<ByteSource> resources = new ArrayList<>();
        final List<String> failedToFind = new ArrayList<>();
        for (final String path : paths) {
            final URL url = BgpOpenConfigImplModuleTest.class.getResource(path);
            if (url == null) {
                failedToFind.add(path);
            } else {
                resources.add(Resources.asByteSource(url));
            }
        }
        Assert.assertEquals("Some files were not found", Collections.<String> emptyList(), failedToFind);
        return resources;
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 9, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 9);
    }

    private CommitStatus createInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        initRuntimeMappingServiceName(transaction);
        initSchemaServiceName(transaction);
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final BgpOpenConfigImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BgpOpenConfigImplModuleMXBean.class);
        mxBean.setBindingBroker(createBindingBroker(transaction));
        return transaction.commit();
    }

    private ObjectName createBindingBroker(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(BindingBrokerImplModuleFactory.NAME, BINDING_BROKER_INSTANCE_NAME);
        final BindingBrokerImplModuleMXBean bean = transaction.newMXBeanProxy(nameCreated, BindingBrokerImplModuleMXBean.class);
        bean.setNotificationService(createNotificationService(transaction));
        bean.setBindingMappingService(this.runtimeMappingName);
        bean.setDataBroker(createForwaredDataBroker(transaction));
        bean.setDomAsyncBroker(createDomAsyncBroker(transaction));
        bean.setRootDataBroker(createRootDataBroker(transaction, ROOT_ASYNC_BROKER_INSTANCE_NAME, ROOT_SUB_ASYNC_BROKER_INSTANCE_NAME));
        return nameCreated;
    }

    private ObjectName createForwaredDataBroker(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
         final ObjectName nameCreated = transaction.createModule(ForwardedCompatibleDataBrokerImplModuleFactory.NAME, FORWARDED_DATA_BROKER_INSTANCE_NAME);
         final ForwardedCompatibleDataBrokerImplModuleMXBean bean = transaction.newMXBeanProxy(nameCreated, ForwardedCompatibleDataBrokerImplModuleMXBean.class);
         bean.setDataBroker(createRootDataBroker(transaction, FORWARDED_SUB_DATA_BROKER_INSTANCE_NAME, FORWARDED_SUB_ASYNC_BROKER_INSTANCE_NAME));
         return nameCreated;
    }

    private ObjectName createRootDataBroker(final ConfigTransactionJMXClient transaction, final String name, final String subName) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(BindingAsyncDataBrokerImplModuleFactory.NAME, name);
        final BindingAsyncDataBrokerImplModuleMXBean bean = transaction.newMXBeanProxy(nameCreated, BindingAsyncDataBrokerImplModuleMXBean.class);
        bean.setSchemaService(this.schemaService);
        bean.setBindingMappingService(this.runtimeMappingName);
        bean.setDomAsyncBroker(createDomDataBrokerService(transaction, subName));
        return nameCreated;
    }

    private void initSchemaServiceName(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        this.schemaService = transaction.createModule(SchemaServiceImplSingletonModuleFactory.NAME, SCHEMA_SERVICE_INSTANCE_NAME);
        transaction.newMXBeanProxy(this.schemaService, SchemaServiceImplSingletonModuleMXBean.class);
    }

    private ObjectName createDomAsyncBroker(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(DomBrokerImplModuleFactory.NAME, DOM_ASYNC_BROKER_INSTANCE_NAME);
        final DomBrokerImplModuleMXBean bean = transaction.newMXBeanProxy(nameCreated, DomBrokerImplModuleMXBean.class);
        bean.setAsyncDataBroker(createDomDataBrokerService(transaction, ASYNC_DATA_BROKER_INSTANCE_NAME));
//        bean.setNotificationQueueDepth(notificationQueueDepth); // TODO
//        bean.setNotificationQueuePark(notificationQueuePark);
//        bean.setNotificationQueueSpin(notificationQueueSpin);
//        bean.setRootSchemaService(rootSchemaService);
        return nameCreated;
    }

    private ObjectName createDomDataBrokerService(final ConfigTransactionJMXClient transaction, final String name) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(DomInmemoryDataBrokerModuleFactory.NAME, name);
        final DomInmemoryDataBrokerModuleMXBean bean = transaction.newMXBeanProxy(nameCreated, DomInmemoryDataBrokerModuleMXBean.class);
//        bean.setConfigDataStore(configDataStore); // TODO
//        bean.setMaxDataBrokerCommitQueueSize(maxDataBrokerCommitQueueSize);
//        bean.setMaxDataBrokerFutureCallbackPoolSize(maxDataBrokerFutureCallbackPoolSize);
//        bean.setOperationalDataStore(operationalDataStore);
//        bean.setSchemaService(nameCreated);
        return nameCreated;
    }

    private void initRuntimeMappingServiceName(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        this.runtimeMappingName  = transaction.createModule(RuntimeMappingModuleFactory.NAME, RUNTIME_MAPPING_INSTANCE_NAME);
        final RuntimeMappingModuleMXBean bean = transaction.newMXBeanProxy(this.runtimeMappingName, RuntimeMappingModuleMXBean.class);
//        bean.setWaitForSchema(waitForSchema); // TODO
    }

    private ObjectName createNotificationService(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(NotificationBrokerImplModuleFactory.NAME, NOTIF_SERVICE_INSTANCE_NAME);
        final NotificationBrokerImplModuleMXBean bean = transaction.newMXBeanProxy(nameCreated, NotificationBrokerImplModuleMXBean.class);
//        bean.setNotificationAdapter(notificationAdapter); //TODO
//        bean.setNotificationPublishAdapter(notificationPublishAdapter);
        return nameCreated;
    }
}
