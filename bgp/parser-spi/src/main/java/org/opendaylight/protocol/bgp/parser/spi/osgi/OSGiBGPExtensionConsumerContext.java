/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.osgi;

import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.google.common.base.Preconditions;

class OSGiBGPExtensionConsumerContext implements BGPExtensionConsumerContext {
	protected final SimpleBGPExtensionProviderContext providerContext = new SimpleBGPExtensionProviderContext();
	protected final BundleContext bundleContext;

	OSGiBGPExtensionConsumerContext(final BundleContext context) {
		this.bundleContext = Preconditions.checkNotNull(context);
	}

	protected final <T> AutoCloseable register(final Class<T> clazz, final T object) {
		final ServiceRegistration<T> reg = bundleContext.registerService(clazz, object, null);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				reg.unregister();
			}
		};
	}

	public final BundleContext getBundleContext() {
		return bundleContext;
	}

	@Override
	public final AddressFamilyRegistry getAddressFamilyRegistry() {
		return providerContext.getAddressFamilyRegistry();
	}

	@Override
	public final AttributeRegistry getAttributeRegistry() {
		return providerContext.getAttributeRegistry();
	}

	@Override
	public final CapabilityRegistry getCapabilityRegistry() {
		return providerContext.getCapabilityRegistry();
	}

	@Override
	public final MessageRegistry getMessageRegistry() {
		return providerContext.getMessageRegistry();
	}

	@Override
	public final NlriRegistry getNlriRegistry() {
		return providerContext.getNlriRegistry();
	}

	@Override
	public final ParameterRegistry getParameterRegistry() {
		return providerContext.getParameterRegistry();
	}

	@Override
	public final SubsequentAddressFamilyRegistry getSubsequentAddressFamilyRegistry() {
		return providerContext.getSubsequentAddressFamilyRegistry();
	}
}
