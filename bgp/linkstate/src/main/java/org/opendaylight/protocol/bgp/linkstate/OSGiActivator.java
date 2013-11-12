/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.osgi.AbstractOSGiBGPExtensionProviderActivator;

public final class OSGiActivator extends AbstractOSGiBGPExtensionProviderActivator {
	private final BGPExtensionProviderActivator activator = new BGPActivator();

	@Override
	public void start(final BGPExtensionProviderContext context) throws Exception {
		activator.start(context);
	}

	@Override
	public void stop() throws Exception {
		activator.stop();
	}
}
