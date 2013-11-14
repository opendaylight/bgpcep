package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;

@Ignore
public class BGPImplModuleTest extends AbstractConfigTest {

	private final String instanceName = "bgp-impl1";
	
	private BGPImplModuleFactory factory;
	
	private BGPSessionProposalImplModuleFactory sessionFacotry;
	
	private BGPDispatcherImplModuleFactory dispactherFactory;
	
	private NettyThreadgroupModuleFactory threadgropFactory;
	
	private BGPMessageFactoryImplModuleFactory messageFactory;
	
	@Before
	public void setUp() throws Exception {
		this.factory = new BGPImplModuleFactory();
		this.dispactherFactory = new BGPDispatcherImplModuleFactory();
		this.sessionFacotry = new BGPSessionProposalImplModuleFactory();
		this.threadgropFactory = new NettyThreadgroupModuleFactory();
		this.messageFactory = new BGPMessageFactoryImplModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.factory, this.dispactherFactory, this.sessionFacotry, this.messageFactory, this.threadgropFactory));
	}
	
	@Test
	public void testValidationExceptionPortNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), instanceName, "localhost", null, sessionFacotry.getImplementationName(), dispactherFactory.getImplementationName(), threadgropFactory.getImplementationName(), messageFactory.getImplementationName());
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("Port value is not set."));
		}
	}
	
	@Test
	public void testValidationExceptionPortOutOfRange()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), instanceName, "localhost", -1, sessionFacotry.getImplementationName(), dispactherFactory.getImplementationName(), threadgropFactory.getImplementationName(), messageFactory.getImplementationName());
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("is out of range (0-65535)."));
		}
	}
	
	@Test
	public void testValidationExceptionHostNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), instanceName, null, 1, sessionFacotry.getImplementationName(), dispactherFactory.getImplementationName(), threadgropFactory.getImplementationName(), messageFactory.getImplementationName());
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("Host value is not set."));
		}
	}
	
	@Test
	public void testCreateBean() throws Exception {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), instanceName, "localhost", 1, sessionFacotry.getImplementationName(), dispactherFactory.getImplementationName(), threadgropFactory.getImplementationName(), messageFactory.getImplementationName());
		transaction.validateConfig();
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 6, 0, 0);
	}
	
	@Test
	public void testReusingOldInstance() throws InstanceAlreadyExistsException,
			ConflictingVersionException, ValidationException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), instanceName, "localhost", 1, sessionFacotry.getImplementationName(), dispactherFactory.getImplementationName(), threadgropFactory.getImplementationName(), messageFactory.getImplementationName());
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 0, 6);
	}

	@Test
	public void testReconfigure() throws InstanceAlreadyExistsException,
			ConflictingVersionException, ValidationException,
			InstanceNotFoundException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), instanceName, "localhost", 1, sessionFacotry.getImplementationName(), dispactherFactory.getImplementationName(), threadgropFactory.getImplementationName(), messageFactory.getImplementationName());
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		BGPImplModuleMXBean mxBean = transaction
				.newMBeanProxy(transaction.lookupConfigBean(
						factory.getImplementationName(),
						instanceName), BGPImplModuleMXBean.class);
		mxBean.setPort(10);
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 1, 5);
	}
	
	public static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName, final String host, final Integer port, final String sessionModuleName, final String dispatcherModuleName, final String threadgroupModuleName, final String messageFactoryModuleName) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		BGPImplModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, BGPImplModuleMXBean.class);
		mxBean.setHost(host);
		mxBean.setPort(port);
		mxBean.setBgpProposal(BGPSessionProposalImplModuleTest.createInstance(transaction, sessionModuleName, "bgp-session1", 1, (short)30, "128.0.0.1"));
		mxBean.setBgpDispatcher(BGPDispatcherImplModuleTest.createInstance(transaction, dispatcherModuleName, "bgp-dispatcher1", threadgroupModuleName, messageFactoryModuleName));
		return nameCreated;
	}
	
}
