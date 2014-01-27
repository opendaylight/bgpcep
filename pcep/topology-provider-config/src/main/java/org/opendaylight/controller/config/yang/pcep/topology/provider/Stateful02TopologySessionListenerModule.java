/**
 * Generated file

 * Generated from: yang module name: config-pcep-topology-provider  yang module local name: pcep-topology-stateful02
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Mon Jan 27 11:08:05 CET 2014
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.pcep.topology.provider;

import org.opendaylight.bgpcep.pcep.topology.provider.Stateful02TopologySessionListenerFactory;

/**
 *
 */
public final class Stateful02TopologySessionListenerModule extends org.opendaylight.controller.config.yang.pcep.topology.provider.AbstractStateful02TopologySessionListenerModule
{

	public Stateful02TopologySessionListenerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public Stateful02TopologySessionListenerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			final Stateful02TopologySessionListenerModule oldModule, final java.lang.AutoCloseable oldInstance) {

		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	protected void customValidation(){
		// Add custom validation for module attributes here.
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		return new AutoCloseableStateful02TopologySessionListenerFactory();
	}

	private static final class AutoCloseableStateful02TopologySessionListenerFactory extends Stateful02TopologySessionListenerFactory implements AutoCloseable {
		@Override
		public void close() {
			// Nothing to do
		}
	}
}
