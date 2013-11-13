package org.opendaylight.controller.config.yang.bgp.reconnectstrategy;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.AbstractNeverReconnectStrategyModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.NeverReconnectStrategyModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.NeverReconnectStrategyModuleMXBean;

public class NeverReconnectStrategyModuleTest extends AbstractConfigTest {

	private final String instanceName = "never";

	private NeverReconnectStrategyModuleFactory factory;

	private ObjectName executor;

	@Before
	public void setUp() throws Exception {
		this.factory = new NeverReconnectStrategyModuleFactory();
		GlobalEventExecutorModuleFactory executorFactory = new GlobalEventExecutorModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
				factory, executorFactory));
		this.executor = GlobalEventExecutorUtil.createGlobalEventExecutorBean(
				executorFactory, configRegistryClient);
	}

	@Test
	public void testValidationException1()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, instanceName, null, this.executor);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("value is not set."));
		}
	}

	@Test
	public void testValidationException2()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, instanceName, -1, this.executor);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("is less than 0"));
		}
	}

	@Test
	public void testValidationException3()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, instanceName, 100, null);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains(
					"expected dependency implementing interface"));
		}
	}

	@Test
	public void testCreateBean() throws Exception {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, instanceName, 500, this.executor);
		transaction.validateConfig();
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 1, 0, 1);
	}

	@Test
	public void testReusingOldInstance() throws InstanceAlreadyExistsException,
			ConflictingVersionException, ValidationException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, instanceName, 100, this.executor);
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 0, 2);
	}

	@Test
	public void testReconfigure() throws InstanceAlreadyExistsException,
			ConflictingVersionException, ValidationException,
			InstanceNotFoundException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, instanceName, 500, this.executor);
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		NeverReconnectStrategyModuleMXBean mxBean = transaction
				.newMBeanProxy(transaction.lookupConfigBean(
						AbstractNeverReconnectStrategyModuleFactory.NAME,
						instanceName), NeverReconnectStrategyModuleMXBean.class);
		mxBean.setTimeout(200);
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 1, 1);
	}

	private ObjectName createInstance(
			final ConfigTransactionJMXClient transaction,
			final String instanceName, final Integer timeout,
			final ObjectName executor) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				factory.getImplementationName(), instanceName);
		NeverReconnectStrategyModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, NeverReconnectStrategyModuleMXBean.class);
		mxBean.setTimeout(timeout);
		mxBean.setExecutor(executor);
		return nameCreated;
	}

}
