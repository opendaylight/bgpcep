package org.opendaylight.controller.config.yang.bgp.reconnectstrategy;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleMXBean;

public class GlobalEventExecutorUtil {

	public static ObjectName createInstance(
			final ConfigTransactionJMXClient transaction,
			final String moduleName,
			final String instanceName)
			throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		transaction.newMBeanProxy(nameCreated,
				GlobalEventExecutorModuleMXBean.class);
		return nameCreated;
	}

}
