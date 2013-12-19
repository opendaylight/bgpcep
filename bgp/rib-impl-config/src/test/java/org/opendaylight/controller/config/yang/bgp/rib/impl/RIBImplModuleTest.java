/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
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
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
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
import org.opendaylight.controller.config.yang.reconnectstrategy.TimedReconnectStrategyModuleFactory;
import org.opendaylight.controller.config.yang.store.impl.YangParserWrapper;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.google.common.collect.Lists;

public class RIBImplModuleTest extends AbstractConfigTest {
	private static final String INSTANCE_NAME = "bgp-rib-impl";
	private static final String TRANSACTION_NAME = "testTransaction";

	private RIBImplModuleFactory factory;
	private DataBrokerImplModuleFactory dataBrokerFactory;
	private TimedReconnectStrategyModuleFactory reconnectFactory;
	private BGPImplModuleFactory bgpFactory;
	private BGPSessionProposalImplModuleFactory sessionFacotry;
	private BGPDispatcherImplModuleFactory dispactherFactory;
	private NettyThreadgroupModuleFactory threadgropFactory;
	private GlobalEventExecutorModuleFactory executorFactory;
	private SimpleBGPExtensionProviderContextModuleFactory extensionFactory;
	private RIBExtensionsImplModuleFactory ribExtensionsFactory;
	private DomBrokerImplModuleFactory domBrokerFactory;
	private RuntimeMappingModuleFactory runtimeMappingFactory;
	private HashMapDataStoreModuleFactory dataStroreFactory;

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

		this.factory = new RIBImplModuleFactory();
		this.dataBrokerFactory = new DataBrokerImplModuleFactory();
		this.bgpFactory = new BGPImplModuleFactory();
		this.executorFactory = new GlobalEventExecutorModuleFactory();
		this.dispactherFactory = new BGPDispatcherImplModuleFactory();
		this.sessionFacotry = new BGPSessionProposalImplModuleFactory();
		this.threadgropFactory = new NettyThreadgroupModuleFactory();
		this.reconnectFactory = new TimedReconnectStrategyModuleFactory();
		this.extensionFactory = new SimpleBGPExtensionProviderContextModuleFactory();
		this.ribExtensionsFactory = new RIBExtensionsImplModuleFactory();
		this.domBrokerFactory = new DomBrokerImplModuleFactory();
		this.runtimeMappingFactory = new RuntimeMappingModuleFactory();
		this.dataStroreFactory = new HashMapDataStoreModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, this.factory,
				this.dispactherFactory, this.sessionFacotry, this.threadgropFactory, this.bgpFactory,
				this.reconnectFactory, this.dataBrokerFactory, this.executorFactory, this.extensionFactory,
				this.ribExtensionsFactory, this.domBrokerFactory, this.runtimeMappingFactory,
				this.dataStroreFactory));

		Filter mockedFilter = mock(Filter.class);
		Mockito.doReturn(mockedFilter).when(mockedContext).createFilter(Mockito.anyString());

		Mockito.doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());

		Mockito.doNothing().when(mockedContext).addBundleListener(any(BundleListener.class));

		Mockito.doReturn(new Bundle[]{}).when(mockedContext).getBundles();

		Mockito.doReturn(new ServiceReference[]{}).when(mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

		ServiceReference<?> mockedserviceReference = mock(ServiceReference.class);
		Mockito.doReturn(new String()).when(mockedserviceReference).toString();
		Mockito.doReturn(mockedserviceReference).when(mockedContext).getServiceReference(any(Class.class));

		Registration<DataCommitHandler<InstanceIdentifier, CompositeNode>> registration = mock(Registration.class);
		Mockito.doReturn(registration).when(mockedDataProvider).registerCommitHandler(any(InstanceIdentifier.class),
				any(DataCommitHandler.class));
		Mockito.doReturn(mockedDataProvider).when(mockedContext).getService(any(ServiceReference.class));

		Mockito.doReturn(null).when(mockedDataProvider).readOperationalData(any(InstanceIdentifier.class));
		Mockito.doReturn(mockedTransaction).when(mockedDataProvider).beginTransaction();

		Mockito.doNothing().when(mockedTransaction).putOperationalData(any(InstanceIdentifier.class), any(CompositeNode.class));
		Mockito.doReturn(mockedFuture).when(mockedTransaction).commit();
		Mockito.doReturn(TRANSACTION_NAME).when(mockedTransaction).getIdentifier();

		Mockito.doReturn(mockedResult).when(mockedFuture).get();
		Mockito.doReturn(true).when(mockedResult).isSuccessful();
		Mockito.doReturn(Collections.emptySet()).when(mockedResult).getErrors();

		// FIXME This needs further mocking
	}

	@Override
	protected BundleContextServiceRegistrationHandler getBundleContextServiceRegistrationHandler(final Class<?> serviceType) {
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
	public void testCreateBean() throws Exception {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), INSTANCE_NAME, this.dataBrokerFactory.getImplementationName(),
				this.reconnectFactory.getImplementationName(), this.executorFactory.getImplementationName(), this.bgpFactory.getImplementationName(),
				this.sessionFacotry.getImplementationName(), this.dispactherFactory.getImplementationName(), this.threadgropFactory.getImplementationName(),
				this.extensionFactory.getImplementationName(), this.ribExtensionsFactory.getImplementationName(), this.domBrokerFactory.getImplementationName(),
				this.dataStroreFactory.getImplementationName());
		transaction.validateConfig();
		CommitStatus status = transaction.commit();
		Thread.sleep(2000);
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 16, 0, 0);
	}

	@After
	public void closeAllModules() throws Exception {
		super.destroyAllConfigBeans();
	}

	public static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName, final String bindingDataModuleName, final String reconnectModueName, final String executorModuleName, final String bgpModuleName,
			final String sessionModuleName, final String dispatcherModuleName, final String threadgroupModuleName, final String extensionModuleName,
			final String ribExtensionsModuleName, final String domBrokerModuleName, final String dataStroreModuleName)
					throws Exception {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		RIBImplModuleMXBean mxBean = transaction.newMXBeanProxy(
				nameCreated, RIBImplModuleMXBean.class);
		ObjectName reconnectObjectName = TimedReconnectStrategyModuleTest.createInstance(transaction, reconnectModueName, "session-reconnect-strategy", 100, 1000L, new BigDecimal(1.0), 5000L, 2000L, null, executorModuleName,
				"global-event-executor1");
		mxBean.setSessionReconnectStrategy(reconnectObjectName);
		mxBean.setDataProvider(createDataBrokerInstance(transaction, bindingDataModuleName, "data-broker-impl", domBrokerModuleName, dataStroreModuleName));
		ObjectName reconnectStrategyON = TimedReconnectStrategyModuleTest.createInstance(transaction, reconnectModueName, "tcp-reconnect-strategy", 100, 1000L, new BigDecimal(1.0), 5000L, 2000L, null, executorModuleName,
				"global-event-executor2");
		mxBean.setTcpReconnectStrategy(reconnectStrategyON);
		mxBean.setBgp(Lists.newArrayList(BGPImplModuleTest.createInstance(transaction, bgpModuleName, "bgp-impl1", "localhost", 1, sessionModuleName, dispatcherModuleName, threadgroupModuleName, ribExtensionsModuleName, extensionModuleName)));
		mxBean.setExtensions(createRibExtensionsInstance(transaction, ribExtensionsModuleName, "rib-extensions-privider1"));
		mxBean.setRibId(new RibId("test"));
		mxBean.setLocalAs(5000L);
		mxBean.setBgpId("192.168.1.1");
		return nameCreated;
	}

	public static ObjectName createDataBrokerInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName, final String domBrokerModuleName, final String dataStroreModuleName) throws
			InstanceAlreadyExistsException, InstanceNotFoundException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		DataBrokerImplModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, DataBrokerImplModuleMXBean.class);
		mxBean.setDomBroker(createDomBrokerInstance(transaction, domBrokerModuleName, "dom-broker1", dataStroreModuleName));
		mxBean.setMappingService(lookupMappingServiceInstance(transaction));
		return nameCreated;
	}

	public static ObjectName createDomBrokerInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName, final String dataStroreModuleName) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		DomBrokerImplModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, DomBrokerImplModuleMXBean.class);
		mxBean.setDataStore(createDataStoreInstance(transaction, dataStroreModuleName, "has-map-data-strore-instance"));
		return nameCreated;
	}

	public static ObjectName createDataStoreInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		transaction.newMBeanProxy(
				nameCreated, HashMapDataStoreModuleMXBean.class);
		return nameCreated;
	}

	public static ObjectName lookupMappingServiceInstance(final ConfigTransactionJMXClient transaction)
			throws InstanceAlreadyExistsException, InstanceNotFoundException {
		ObjectName nameCreated = transaction.lookupConfigBean("runtime-generated-mapping", "runtime-mapping-singleton");
		return nameCreated;
	}

	public static ObjectName createRibExtensionsInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		transaction.newMBeanProxy(
				nameCreated, RIBExtensionsImplModuleMXBean.class);
		return nameCreated;
	}

	public SchemaContext getMockedSchemaContext() {
		List<String> paths = Arrays.asList("/META-INF/yang/bgp-rib.yang", "/META-INF/yang/ietf-inet-types.yang",
				"/META-INF/yang/bgp-message.yang", "/META-INF/yang/bgp-multiprotocol.yang", "/META-INF/yang/bgp-types.yang");
		return YangParserWrapper.parseYangFiles(getFilesAsInputStreams(paths));
	}
}
