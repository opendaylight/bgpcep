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
import org.opendaylight.controller.config.yang.reconnectstrategy.AbstractReconnectImmediatelyStrategyModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.ReconnectImmediatelyStrategyModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.ReconnectImmediatelyStrategyModuleMXBean;

public class ReconnectImmediatelyStrategyModuleTest extends AbstractConfigTest {

	private final String instanceName = "immediately";

	private ReconnectImmediatelyStrategyModuleFactory factory;

	@Before
	public void setUp() {
		this.factory = new ReconnectImmediatelyStrategyModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
				factory));
	}

	@Test
	public void testValidationException1()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, instanceName, null);
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
			createInstance(transaction, instanceName, -1);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("is less than 0"));
		}
	}

	@Test
	public void testCreateBean() throws Exception {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, instanceName, 500);
		transaction.validateConfig();
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 1, 0, 0);
	}

	@Test
	public void testReusingOldInstance() throws InstanceAlreadyExistsException,
			ConflictingVersionException, ValidationException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, instanceName, 100);
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 0, 1);
	}

	@Test
	public void testReconfigure() throws InstanceAlreadyExistsException,
			ConflictingVersionException, ValidationException,
			InstanceNotFoundException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, instanceName, 500);
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		ReconnectImmediatelyStrategyModuleMXBean mxBean = transaction
				.newMBeanProxy(transaction.lookupConfigBean(
						AbstractReconnectImmediatelyStrategyModuleFactory.NAME,
						instanceName), ReconnectImmediatelyStrategyModuleMXBean.class);
		mxBean.setTimeout(200);
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 1, 0);
	}

	private ObjectName createInstance(final ConfigTransactionJMXClient transaction,
			final String instanceName, final Integer timeout)
			throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				factory.getImplementationName(), instanceName);
		ReconnectImmediatelyStrategyModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, ReconnectImmediatelyStrategyModuleMXBean.class);
		mxBean.setTimeout(timeout);
		return nameCreated;
	}

}
