package org.opendaylight.controller.config.yang.bgp.rib.impl;

import java.io.Closeable;
import java.math.BigDecimal;
import java.util.Dictionary;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.reconnectstrategy.TimedReconnectStrategyModuleTest;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingBrokerImplModuleMXBean;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.TimedReconnectStrategyModuleFactory;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

@Ignore
public class RIBImplModuleTest extends AbstractConfigTest {

	private final String instanceName = "bgp-rib-impl";

	private RIBImplModuleFactory factory;

	private BindingBrokerImplModuleFactory brokerFactory;

	private TimedReconnectStrategyModuleFactory reconnectFactory;

	private BGPImplModuleFactory bgpFactory;

	private BGPSessionProposalImplModuleFactory sessionFacotry;

	private BGPDispatcherImplModuleFactory dispactherFactory;

	private NettyThreadgroupModuleFactory threadgropFactory;

	private RIBExtensionsImplModuleFactory messageFactory;

	private GlobalEventExecutorModuleFactory executorFactory;

	@Before
	public void setUp() throws Exception {
		this.factory = new RIBImplModuleFactory();
		this.brokerFactory = new BindingBrokerImplModuleFactory();
		this.bgpFactory = new BGPImplModuleFactory();
		this.executorFactory = new GlobalEventExecutorModuleFactory();
		this.dispactherFactory = new BGPDispatcherImplModuleFactory();
		this.sessionFacotry = new BGPSessionProposalImplModuleFactory();
		this.threadgropFactory = new NettyThreadgroupModuleFactory();
		this.messageFactory = new RIBExtensionsImplModuleFactory();
		this.reconnectFactory = new TimedReconnectStrategyModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.factory,
				this.dispactherFactory, this.sessionFacotry, this.messageFactory,
				this.threadgropFactory, this.bgpFactory, this.reconnectFactory, this.brokerFactory, this.executorFactory));
		Mockito.doReturn(mockedServiceRegistration).when(mockedContext).registerService(
				Matchers.any(String.class), Mockito.any(Closeable.class),
				Mockito.any(Dictionary.class));
		Mockito.doReturn(mockedServiceRegistration).when(mockedContext).registerService(
				Matchers.any(Class.class), Mockito.any(Closeable.class),
				Mockito.any(Dictionary.class));
		Filter mockedFilter = Mockito.mock(Filter.class);
		Mockito.doReturn(mockedFilter).when(mockedContext).createFilter(Mockito.anyString());

		Mockito.doNothing().when(mockedContext).addServiceListener(Mockito.any(ServiceListener.class), Mockito.anyString());

		Mockito.doReturn(new ServiceReference[]{}).when(mockedContext).getServiceReferences(Matchers.anyString(), Matchers.anyString());

		ServiceReference<?> mockedserviceReference = Mockito.mock(ServiceReference.class);
		Mockito.doReturn(new String()).when(mockedserviceReference).toString();
		Mockito.doReturn(mockedserviceReference).when(mockedContext).getServiceReference(Matchers.any(Class.class));

		DataProviderService mockedService = Mockito.mock(DataProviderService.class);
		Mockito.doReturn(mockedService).when(mockedContext).getService(Matchers.any(ServiceReference.class));
	}

	@Test
	public void testCreateBean() throws Exception {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), instanceName, new ObjectName("org.opendaylight.controller:instanceName=binding-broker-singleton,moduleFactoryName=binding-broker-impl-singleton,type=Module"),
				this.reconnectFactory.getImplementationName(), this.executorFactory.getImplementationName(), this.bgpFactory.getImplementationName(), this.sessionFacotry.getImplementationName(),
				this.dispactherFactory.getImplementationName(), this.threadgropFactory.getImplementationName(), this.messageFactory.getImplementationName());
		transaction.validateConfig();
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 6, 0, 0);
	}

	public static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName, final ObjectName bindingDataModule, final String reconnectModueName, final String executorModuleName, final String bgpModuleName,
			final String sessionModuleName, final String dispatcherModuleName, final String threadgroupModuleName, final String messageFactoryModuleName) throws InstanceAlreadyExistsException, MalformedObjectNameException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		RIBImplModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, RIBImplModuleMXBean.class);
		ObjectName reconnectObjectName = TimedReconnectStrategyModuleTest.createInstance(transaction, reconnectModueName, "session-reconnect-strategy", 0, 2000L, new BigDecimal(1.0), 5000L, 2000L, null, executorModuleName);
		mxBean.setSessionReconnectStrategy(reconnectObjectName);
		mxBean.setDataProvider(bindingDataModule);
		mxBean.setTcpReconnectStrategy(reconnectObjectName);
		mxBean.setBgp(BGPImplModuleTest.createInstance(transaction, bgpModuleName, "bgp-impl1", "localhost", 1, sessionModuleName, dispatcherModuleName, threadgroupModuleName, messageFactoryModuleName));
		return nameCreated;
	}

	public static ObjectName createBindingBrokerInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		transaction.newMBeanProxy(
				nameCreated, BindingBrokerImplModuleMXBean.class);
		return nameCreated;
	}
}
