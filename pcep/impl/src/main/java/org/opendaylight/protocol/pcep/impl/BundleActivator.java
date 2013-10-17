/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.osgi.AbstractOSGiPCEPExtensionProviderActivator;

public final class BundleActivator extends AbstractOSGiPCEPExtensionProviderActivator {
	private final PCEPExtensionProviderActivator activator = new Activator();

	@Override
	public void start(final PCEPExtensionProviderContext context) throws Exception {
		activator.start(context);
	}

	@Override
	public void stop() throws Exception {
		activator.stop();
	}
}
