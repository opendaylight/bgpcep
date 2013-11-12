package org.opendaylight.controller.config.yang.bgp.reconnectstrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

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
import org.opendaylight.controller.config.yang.reconnectstrategy.AbstractTimedReconnectStrategyModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.TimedReconnectStrategyModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.TimedReconnectStrategyModuleMXBean;

public class TimedReconnectStrategyModuleTest extends AbstractConfigTest {

	private final String instanceName = "timed";

	private TimedReconnectStrategyModuleFactory factory;

	private ObjectName executor;

	@Before
	public void setUp() throws Exception {
		this.factory = new TimedReconnectStrategyModuleFactory();
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
			createInstance(transaction, instanceName, 500, 100L, null, 500L,
					10L, 10000L, this.executor);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("SleepFactor value is not set."));
		}
	}

	@Test
	public void testValidationException2()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, instanceName, 500, 100L,
					new BigDecimal(0.5), 500L, 10L, 10000L, this.executor);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("value 0.5 is less than 1"));
		}
	}

	@Test
	public void testValidationException3()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, instanceName, null, 100L,
					new BigDecimal(1.0), 500L, 10L, 10000L, this.executor);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("ConnectTime value is not set."));
		}
	}

	@Test
	public void testValidationException4()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, instanceName, -1, 100L, new BigDecimal(
					1.0), 500L, 10L, 10000L, this.executor);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("value -1 is less than 0"));
		}
	}

	@Test
	public void testValidationException5()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, instanceName, 100, null,
					new BigDecimal(1.0), null, 10L, 10000L, this.executor);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("MinSleep value is not set."));
		}
	}

	@Test
	public void testValidationException6()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, instanceName, 100, 300L,
					new BigDecimal(1.0), 100L, 10L, 10000L, this.executor);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains(
					"value 300 is greter than MaxSleep 100"));
		}
	}

	@Test
	public void testValidationException7()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, instanceName, 100, 100L,
					new BigDecimal(1.0), 300L, 10L, 10000L, null);
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
		createInstance(transaction, instanceName, 500, 100L,
				new BigDecimal(1.0), 500L, 10L, 10000L, this.executor);
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
		createInstance(transaction, instanceName, 500, 100L,
				new BigDecimal(1.0), 500L, 10L, 10000L, this.executor);
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
		createInstance(transaction, instanceName, 500, 100L,
				new BigDecimal(1.0), new Long(500), new Long(10), new Long(
						10000), this.executor);
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		TimedReconnectStrategyModuleMXBean mxBean = transaction
				.newMBeanProxy(transaction.lookupConfigBean(
						AbstractTimedReconnectStrategyModuleFactory.NAME,
						instanceName), TimedReconnectStrategyModuleMXBean.class);
		assertEquals(mxBean.getMinSleep(), new Long(100));
		mxBean.setMinSleep(200L);
		assertEquals(mxBean.getMinSleep(), new Long(200));
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 1, 1);

	}

	private ObjectName createInstance(
			final ConfigTransactionJMXClient transaction,
			final String instanceName, final Integer connectTime,
			final Long minSleep, final BigDecimal sleepFactor,
			final Long maxSleep, final Long maxAttempts, final Long deadline,
			final ObjectName executor) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				factory.getImplementationName(), instanceName);
		TimedReconnectStrategyModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, TimedReconnectStrategyModuleMXBean.class);
		mxBean.setConnectTime(connectTime);
		mxBean.setDeadline(deadline);
		mxBean.setMaxAttempts(maxAttempts);
		mxBean.setMaxSleep(maxSleep);
		mxBean.setMinSleep(minSleep);
		mxBean.setSleepFactor(sleepFactor);
		mxBean.setExecutor(executor);
		return nameCreated;
	}

}
