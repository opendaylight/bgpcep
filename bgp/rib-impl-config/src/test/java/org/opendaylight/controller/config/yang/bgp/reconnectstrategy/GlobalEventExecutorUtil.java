package org.opendaylight.controller.config.yang.bgp.reconnectstrategy;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleMXBean;

public class GlobalEventExecutorUtil extends AbstractConfigTest {

	private final String instanceName = "global-executor";

	private ObjectName createInstance(
			final ConfigTransactionJMXClient transaction,
			final String instanceName, GlobalEventExecutorModuleFactory factory)
			throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				factory.getImplementationName(), instanceName);
		transaction.newMBeanProxy(nameCreated,
				GlobalEventExecutorModuleMXBean.class);
		return nameCreated;
	}

	private ObjectName createBean(GlobalEventExecutorModuleFactory factory,
			ConfigRegistryJMXClient configRegistryClient)
			throws InstanceAlreadyExistsException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		ObjectName objectName = createInstance(transaction, instanceName,
				factory);
		transaction.validateConfig();
		transaction.commit();
		return objectName;
	}

	public static ObjectName createGlobalEventExecutorBean(
			GlobalEventExecutorModuleFactory factory,
			ConfigRegistryJMXClient configRegistryClient)
			throws InstanceAlreadyExistsException {
		GlobalEventExecutorUtil util = new GlobalEventExecutorUtil();
		return util.createBean(factory, configRegistryClient);
	}

}
