/**
 * Generated file

 * Generated from: yang module name: bgp-rib-impl  yang module local name: base-bgp-parser
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Mon Nov 18 10:59:18 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;

/**
 *
 */
public final class BaseBGPParserModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBaseBGPParserModule
{

	public BaseBGPParserModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public BaseBGPParserModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final BaseBGPParserModule oldModule, final java.lang.AutoCloseable oldInstance) {
		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void validate(){
		super.validate();
		// Add custom validation for module attributes here.
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		final BGPExtensionProviderActivator act = new BGPActivator();

		act.start(getBgpExtensionsDependency());
		return new AutoCloseable() {
			@Override
			public void close() {
				act.stop();
			}
		};
	}
}
