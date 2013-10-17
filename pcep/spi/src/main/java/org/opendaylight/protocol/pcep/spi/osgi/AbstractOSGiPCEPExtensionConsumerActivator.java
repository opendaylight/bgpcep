/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.osgi;

import org.opendaylight.protocol.pcep.spi.PCEPExtensionConsumerActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionConsumerContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.google.common.base.Preconditions;

public abstract class AbstractOSGiPCEPExtensionConsumerActivator implements BundleActivator, PCEPExtensionConsumerActivator {
	private PCEPExtensionConsumerContext consumerContext;

	@Override
	public final void start(final BundleContext context) throws Exception {
		Preconditions.checkState(consumerContext == null);
		final PCEPExtensionConsumerContext consumerContext = new OSGiPCEPExtensionConsumerContext(context);
		start(consumerContext);
		this.consumerContext = consumerContext;
	}

	@Override
	public final void stop(final BundleContext context) throws Exception {
		Preconditions.checkState(consumerContext != null);
		try {
			stop();
		} finally {
			consumerContext = null;
		}
	}
}
