package org.opendaylight.controller.config.yang.bgp.rib.impl;

import javax.management.InstanceAlreadyExistsException;
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
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleMXBean;

@Ignore
public class BGPMessageFactoryImplModuleTest extends AbstractConfigTest {
	
	private final String instanceName = "bgp-message-fct";

	private BGPMessageFactoryImplModuleFactory factory;
	
	private SimpleBGPExtensionProviderContextModuleFactory bgpEPCFactory;
	
	@Before
	public void setUp() throws Exception {
		this.factory = new BGPMessageFactoryImplModuleFactory();
		this.bgpEPCFactory = new SimpleBGPExtensionProviderContextModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.factory, this.bgpEPCFactory));
	}
	
	@Test
	public void testCreateBean() throws Exception {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), instanceName, this.bgpEPCFactory.getImplementationName());
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
		createInstance(transaction, this.factory.getImplementationName(), instanceName, this.bgpEPCFactory.getImplementationName());
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 0, 1);
	}
	
	public static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName, final String extensionsModuleName) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		BGPMessageFactoryImplModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, BGPMessageFactoryImplModuleMXBean.class);
		//mxBean.setBgpExtensions(createBgpExtensionsInstance(transaction, extensionsModuleName, "bgp-extension"));
		return nameCreated;	
	}
	
	public static ObjectName createBgpExtensionsInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		transaction.newMBeanProxy(
				nameCreated, SimpleBGPExtensionProviderContextModuleMXBean.class);
		return nameCreated;			
	}
	
}
