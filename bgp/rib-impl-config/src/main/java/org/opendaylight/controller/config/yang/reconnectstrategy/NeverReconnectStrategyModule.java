/**
* Generated file

* Generated from: yang module name: reconnect-strategy  yang module local name: never-reconnect-strategy
* Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
* Generated at: Tue Nov 12 13:27:18 CET 2013
*
* Do not modify this file unless it is present under src/main directory
*/
package org.opendaylight.controller.config.yang.reconnectstrategy;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.reconnectstrategy.util.ReconnectStrategyCloseable;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;

/**
*
*/
public final class NeverReconnectStrategyModule extends org.opendaylight.controller.config.yang.reconnectstrategy.AbstractNeverReconnectStrategyModule
{

    public NeverReconnectStrategyModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NeverReconnectStrategyModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, NeverReconnectStrategyModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate(){
		super.validate();
		JmxAttributeValidationException.checkNotNull(getTimeout(),
				"value is not set.", timeoutJmxAttribute);
		JmxAttributeValidationException.checkCondition(getTimeout() >= 0,
				"value " + getTimeout() + " is less than 0",
				timeoutJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
		ReconnectStrategy reconnectStrategy = new NeverReconnectStrategy(getExecutorDependency(), getTimeout());
		return new ReconnectStrategyCloseable(reconnectStrategy);
	}
}
