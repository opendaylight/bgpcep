/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.osgi;

import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.google.common.base.Preconditions;

public abstract class AbstractOSGiBGPExtensionProviderActivator implements BundleActivator, BGPExtensionProviderActivator {
	private BGPExtensionProviderContext providerContext;

	@Override
	public final void start(final BundleContext context) throws Exception {
		Preconditions.checkState(providerContext == null);
		final BGPExtensionProviderContext providerContext = new OSGiBGPExtensionProviderContext(context);
		start(providerContext);
		this.providerContext = providerContext;
	}

	@Override
	public final void stop(final BundleContext context) throws Exception {
		Preconditions.checkState(providerContext != null);
		try {
			stop();
		} finally {
			providerContext = null;
		}
	}
}
