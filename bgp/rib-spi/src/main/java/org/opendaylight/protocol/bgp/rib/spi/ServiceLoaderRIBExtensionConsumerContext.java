/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class ServiceLoaderRIBExtensionConsumerContext extends SimpleRIBExtensionProviderContext implements AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(ServiceLoaderRIBExtensionConsumerContext.class);
	private final ServiceLoader<RIBExtensionProviderActivator> loader;

	private ServiceLoaderRIBExtensionConsumerContext(final ServiceLoader<RIBExtensionProviderActivator> loader) {
		this.loader = Preconditions.checkNotNull(loader);

		for (RIBExtensionProviderActivator a : loader) {
			a.startRIBExtensionProvider(this);
		}
	}

	public static ServiceLoaderRIBExtensionConsumerContext createConsumerContext() throws Exception {
		final ServiceLoader<RIBExtensionProviderActivator> loader = ServiceLoader.load(RIBExtensionProviderActivator.class);
		final ServiceLoaderRIBExtensionConsumerContext ctx = new ServiceLoaderRIBExtensionConsumerContext(loader);

		return ctx;
	}

	@Override
	public void close() throws Exception {
		for (RIBExtensionProviderActivator a : loader) {
			try {
				a.stopRIBExtensionProvider();
			} catch (Exception e) {
				LOG.warn("Stopping activator {} failed", a, e);
			}
		}
	}
}
