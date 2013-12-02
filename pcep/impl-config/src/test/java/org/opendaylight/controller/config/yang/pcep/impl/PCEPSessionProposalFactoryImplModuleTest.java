package org.opendaylight.controller.config.yang.pcep.impl;

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

public class PCEPSessionProposalFactoryImplModuleTest extends
AbstractConfigTest {

	private final String instanceName = "pcep-proposal";

	private PCEPSessionProposalFactoryImplModuleFactory factory;

	@Before
	public void setUp() throws Exception {
		this.factory = new PCEPSessionProposalFactoryImplModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
				factory));
	}

	@Test
	public void testValidationExceptionDeadTimerValueNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(),
					instanceName, null, 100, true, true, true, true, 1000);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains(
					"DeadTimerValue value is not set"));
		}
	}

	@Test
	public void testValidationExceptionKeepAliveTimerNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(),
					instanceName, 400, null, true, true, true, true, 1000);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains(
					"KeepAliveTimerValue value is not set"));
		}
	}

	@Test
	public void testValidationExceptionStatefulNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(),
					instanceName, 400, 100, null, false, false, false, -1);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("Stateful value is not set"));
		}
	}

	@Test
	public void testValidationExceptionActiveNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(),
					instanceName, 400, 100, true, null, true, true, 1000);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("Active value is not set"));
		}
	}

	@Test
	public void testValidationExceptionVersionedNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(),
					instanceName, 400, 100, true, true, null, true, 1000);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("Versioned value is not set"));
		}
	}

	@Test
	public void testValidationExceptionInstantiatedNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(),
					instanceName, 400, 100, true, true, true, null, 1000);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("Instantiated value is not set"));
		}
	}

	@Test
	public void testValidationExceptionTimeoutNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(),
					instanceName, 400, 100, true, true, true, true, null);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("Timeout value is not set"));
		}
	}

	@Test
	public void testValidationExceptionKeepAliveTimerMinValue()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(),
					instanceName, 400, -10, true, true, true, true, 1000);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("minimum value is 1."));
		}
	}

	@Test
	public void testStatefulAfterCommitted()
			throws InstanceAlreadyExistsException, InstanceNotFoundException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(),
				instanceName, 400, 100, false, true, true, true, 1000);
		transaction.validateConfig();
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		PCEPSessionProposalFactoryImplModuleMXBean mxBean = transaction
				.newMBeanProxy(
						transaction
						.lookupConfigBean(
								AbstractPCEPSessionProposalFactoryImplModuleFactory.NAME,
								instanceName),
								PCEPSessionProposalFactoryImplModuleMXBean.class);
		assertTrue(mxBean.getStateful());
	}

	@Test
	public void testCreateBean() throws Exception {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(),
				instanceName, 0, 0, true, true, true, true, 1000);
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
		createInstance(transaction, this.factory.getImplementationName(),
				instanceName, 400, 100, true, true, true, true, 1000);
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
		createInstance(transaction, this.factory.getImplementationName(),
				instanceName, 400, 100, true, true, true, true, 1000);
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		PCEPSessionProposalFactoryImplModuleMXBean mxBean = transaction
				.newMBeanProxy(
						transaction
						.lookupConfigBean(
								AbstractPCEPSessionProposalFactoryImplModuleFactory.NAME,
								instanceName),
								PCEPSessionProposalFactoryImplModuleMXBean.class);
		mxBean.setTimeout(200);
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 1, 0);
	}

	public static ObjectName createInstance(
			final ConfigTransactionJMXClient transaction,
			final String moduleName, final String instanceName,
			final Integer deadTimer, final Integer keepAlive,
			final Boolean stateful, final Boolean active,
			final Boolean versioned, final Boolean instant,
			final Integer timeout) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(moduleName,
				instanceName);
		PCEPSessionProposalFactoryImplModuleMXBean mxBean = transaction
				.newMBeanProxy(nameCreated,
						PCEPSessionProposalFactoryImplModuleMXBean.class);
		mxBean.setActive(active);
		mxBean.setDeadTimerValue(deadTimer);
		mxBean.setInitiated(instant);
		mxBean.setKeepAliveTimerValue(keepAlive);
		mxBean.setStateful(stateful);
		mxBean.setTimeout(timeout);
		mxBean.setVersioned(versioned);
		return nameCreated;
	}

}
