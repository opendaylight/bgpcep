/**
 * Generated file

 * Generated from: yang module name: bgp-rib-impl  yang module local name: bgp-message-factory-impl
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Wed Nov 06 13:02:31 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import org.opendaylight.protocol.bgp.parser.BGPMessageFactory;
import org.opendaylight.protocol.bgp.parser.impl.BGPMessageFactoryImpl;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
*
*/
public final class BGPMessageFactoryImplModule
		extends
		org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPMessageFactoryImplModule {

	public BGPMessageFactoryImplModule(
			org.opendaylight.controller.config.api.ModuleIdentifier name,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(name, dependencyResolver);
	}

	public BGPMessageFactoryImplModule(
			org.opendaylight.controller.config.api.ModuleIdentifier name,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			BGPMessageFactoryImplModule oldModule,
			java.lang.AutoCloseable oldInstance) {
		super(name, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void validate() {
		super.validate();
		// Add custom validation for module attributes here.
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		try {
			return new BGPMessageFactoryCloseable(new BGPMessageFactoryImpl(
					ServiceLoaderBGPExtensionProviderContext
							.createConsumerContext().getMessageRegistry()));
		} catch (Exception e) {
			throw new RuntimeException("Failed to create consumer context.", e);
		}
	}

	private static class BGPMessageFactoryCloseable implements
			BGPMessageFactory, AutoCloseable {
		private final BGPMessageFactoryImpl inner;

		public BGPMessageFactoryCloseable(
				BGPMessageFactoryImpl bgpMessageFactory) {
			this.inner = bgpMessageFactory;
		}

		@Override
		public void close() throws Exception {
			// NOOP
		}

		@Override
		public Notification parse(byte[] bytes) throws DeserializerException,
				DocumentedException {
			return inner.parse(bytes);
		}

		@Override
		public byte[] put(Notification bgpMessage) {
			return inner.put(bgpMessage);
		}
	}
}
