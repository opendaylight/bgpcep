package org.opendaylight.controller.config.yang.bmp.impl;

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
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleFactory;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleMXBean;
import org.opendaylight.controller.config.yang.bmp.spi.SimpleBmpExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.bmp.spi.SimpleBmpExtensionProviderContextModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RuntimeMappingModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.RuntimeMappingModuleMXBean;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerModuleMXBean;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleMXBean;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.dom.broker.GlobalBundleScanningSchemaServiceImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
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

public class BmpMonitorImplModuleTest extends AbstractConfigTest {

    private static final String FACTORY_NAME = BmpMonitorImplModuleFactory.NAME;
    private static final String INSTANCE_NAME = "bmp-monitor-impl-instance";
    private static final String RIB_EXT_INSTANCE_NAME = "rib-ext-instance";
    private static final String CODEC_INSTANCE_NAME = "runtime-mapping-singleton";
    private static final String DOM_INSTANCE_NAME = "dom-data-instance";
    private static final String DISP_INSTANCE_NAME = "disp-instance";
    private static final String WORKER_INSTANCE_NAME = "worker-group-instance";
    private static final String BOSS_INSTANCE_NAME = "boss-group-instance";
    private static final String BMP_EXTENSION_INSTANCE_NAME = "bmp-ext-instance";

    private static final String TRANSACTION_NAME = "testTransaction";
    @Mock private ReadWriteTransaction mockedTransaction;
    @Mock private DataBroker mockedDataProvider;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> mockedFuture;
    @Mock private RpcResult<TransactionStatus> mockedResult;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext,
            new BmpMonitorImplModuleFactory(),
            new RIBExtensionsImplModuleFactory(),
            new RuntimeMappingModuleFactory(),
            new DomInmemoryDataBrokerModuleFactory(),
            new BmpDispatcherImplModuleFactory(),
            new NettyThreadgroupModuleFactory(),
            new SimpleBmpExtensionProviderContextModuleFactory()));

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
                "/META-INF/yang/network-concepts.yang", "/META-INF/yang/ieee754.yang",
                "/META-INF/yang/bmp-monitor.yang", "/META-INF/yang/bmp-message.yang", "/META-INF/yang/ietf-yang-types.yang");
        return paths;
    }
    private Collection<ByteSource> getFilesAsByteSources(final List<String> paths) {
        final Collection<ByteSource> resources = new ArrayList<>();
        final List<String> failedToFind = new ArrayList<>();
        for (final String path : paths) {
            final URL url = BmpMonitorImplModuleTest.class.getResource(path);
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
        assertStatus(status, 7, 0, 0);
    }

    private CommitStatus createInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final BmpMonitorImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BmpMonitorImplModuleMXBean.class);
        mxBean.setExtensions(createExtensions(transaction));
        mxBean.setCodecTreeFactory(createRuntimeMapping(transaction));
        mxBean.setDomDataProvider(createDomData(transaction));
        mxBean.setBmpDispatcher(createDispatcher(transaction));
        mxBean.setBindingPort(new PortNumber(9999));
        return transaction.commit();
    }

    private static ObjectName createExtensions(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(RIBExtensionsImplModuleFactory.NAME, RIB_EXT_INSTANCE_NAME);
        transaction.newMXBeanProxy(nameCreated, RIBExtensionsImplModuleMXBean.class);
        return nameCreated;
    }

    private static ObjectName createRuntimeMapping(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(RuntimeMappingModuleFactory.NAME, CODEC_INSTANCE_NAME);
        transaction.newMXBeanProxy(nameCreated, RuntimeMappingModuleMXBean.class);
        return nameCreated;
    }

    private static ObjectName createDomData(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(DomInmemoryDataBrokerModuleFactory.NAME, DOM_INSTANCE_NAME);
        transaction.newMXBeanProxy(nameCreated, DomInmemoryDataBrokerModuleMXBean.class);
        return nameCreated;
    }

    private static ObjectName createDispatcher(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(BmpDispatcherImplModuleFactory.NAME, DISP_INSTANCE_NAME);
        final BmpDispatcherImplModuleMXBean bean = transaction.newMXBeanProxy(nameCreated, BmpDispatcherImplModuleMXBean.class);
        bean.setWorkerGroup(createThreadgroupInstance(transaction, WORKER_INSTANCE_NAME, 2));
        bean.setBossGroup(createThreadgroupInstance(transaction, BOSS_INSTANCE_NAME, 3));
        bean.setBmpExtensions(createBmpExtensionsInstance(transaction));
        return nameCreated;
    }

    private static ObjectName createThreadgroupInstance(final ConfigTransactionJMXClient transaction, final String instanceName,
        final Integer threadCount) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(NettyThreadgroupModuleFactory.NAME, instanceName);
        final NettyThreadgroupModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, NettyThreadgroupModuleMXBean.class);
        mxBean.setThreadCount(threadCount);
        return nameCreated;
    }

    private static ObjectName createBmpExtensionsInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(SimpleBmpExtensionProviderContextModuleFactory.NAME, BMP_EXTENSION_INSTANCE_NAME);
        transaction.newMXBeanProxy(nameCreated, SimpleBmpExtensionProviderContextModuleMXBean.class);
        return nameCreated;
    }
}
