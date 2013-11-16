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
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleMXBean;
import org.opendaylight.controller.config.yang.pcep.spi.SimplePCEPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.pcep.spi.SimplePCEPExtensionProviderContextModuleMXBean;

public class PCEPDispatcherImplModuleTest extends AbstractConfigTest {

	private final String instanceName = "pcep-dispatcher";

	private PCEPDispatcherImplModuleFactory factory;

	private PCEPSessionProposalFactoryImplModuleFactory sessionFactory;

	private NettyThreadgroupModuleFactory threadgroupFactory;

	private SimplePCEPExtensionProviderContextModuleFactory extensionsFactory;

	@Before
	public void setUp() throws Exception {
		this.factory = new PCEPDispatcherImplModuleFactory();
		this.sessionFactory = new PCEPSessionProposalFactoryImplModuleFactory();
		this.threadgroupFactory = new NettyThreadgroupModuleFactory();
		this.extensionsFactory = new SimplePCEPExtensionProviderContextModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
				factory, sessionFactory, threadgroupFactory, extensionsFactory));
	}

	@Test
	public void testValidationExceptionMaxUnknownMessagesNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(),
					instanceName, null,
					this.sessionFactory.getImplementationName(),
					this.threadgroupFactory.getImplementationName(),
					this.extensionsFactory.getImplementationName());
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("MaxUnknownMessages value is not set"));
		}
	}

	@Test
	public void testValidationExceptionMaxUnknownMessagesMinValue()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(),
					instanceName, 0,
					this.sessionFactory.getImplementationName(),
					this.threadgroupFactory.getImplementationName(),
					this.extensionsFactory.getImplementationName());
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("must be greater than 0"));
		}
	}

	@Test
	public void testCreateBean() throws Exception {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(),
				instanceName, 5, this.sessionFactory.getImplementationName(),
				this.threadgroupFactory.getImplementationName(),
				this.extensionsFactory.getImplementationName());
		transaction.validateConfig();
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 5, 0, 0);
	}

	@Test
	public void testReusingOldInstance() throws InstanceAlreadyExistsException,
	ConflictingVersionException, ValidationException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(),
				instanceName, 5, this.sessionFactory.getImplementationName(),
				this.threadgroupFactory.getImplementationName(), this.extensionsFactory.getImplementationName());
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 0, 5);
	}

	@Test
	public void testReconfigure() throws InstanceAlreadyExistsException,
	ConflictingVersionException, ValidationException,
	InstanceNotFoundException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(),
				instanceName, 5, this.sessionFactory.getImplementationName(),
				this.threadgroupFactory.getImplementationName(),
				this.extensionsFactory.getImplementationName());
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		PCEPDispatcherImplModuleMXBean mxBean = transaction.newMBeanProxy(
				transaction.lookupConfigBean(
						this.factory.getImplementationName(), instanceName),
						PCEPDispatcherImplModuleMXBean.class);
		mxBean.setMaxUnknownMessages(10);
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 1, 4);
	}

	public static ObjectName createInstance(
			final ConfigTransactionJMXClient transaction,
			final String moduleName, final String instanceName,
			final Integer maxUnknownMessages,
			final String sessionFactoryImplName,
			final String threadGroupFactoryImplName,
			final String extensionsImplName)
					throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(moduleName,
				instanceName);
		PCEPDispatcherImplModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, PCEPDispatcherImplModuleMXBean.class);
		mxBean.setPcepSessionProposalFactory(PCEPSessionProposalFactoryImplModuleTest
				.createInstance(transaction, sessionFactoryImplName,
						"pcep-proposal", 0, 0, true, true, true, true, 1000));
		mxBean.setMaxUnknownMessages(maxUnknownMessages);
		mxBean.setBossGroup(createThreadGroupInstance(transaction,
				threadGroupFactoryImplName, "boss-group", 10));
		mxBean.setWorkerGroup(createThreadGroupInstance(transaction,
				threadGroupFactoryImplName, "worker-group", 10));
		mxBean.setExtensions(createExtensionsInstance(transaction,
				extensionsImplName, "extensions"));
		return nameCreated;
	}

	public static ObjectName createExtensionsInstance(
			final ConfigTransactionJMXClient transaction,
			final String moduleName, final String instanceName) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(moduleName,
				instanceName);
		SimplePCEPExtensionProviderContextModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, SimplePCEPExtensionProviderContextModuleMXBean.class);

		return nameCreated;
	}

	public static ObjectName createThreadGroupInstance(
			final ConfigTransactionJMXClient transaction,
			final String moduleName, final String instanceName,
			final Integer threadCount) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(moduleName,
				instanceName);
		NettyThreadgroupModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, NettyThreadgroupModuleMXBean.class);
		mxBean.setThreadCount(threadCount);
		return nameCreated;
	}

}
