/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import java.util.List;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public abstract class AbstractExtensionProviderActivator implements PCEPExtensionProviderActivator {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractExtensionProviderActivator.class);

	@GuardedBy("this")
	private List<AutoCloseable> registrations;

	@GuardedBy("this")
	protected abstract List<AutoCloseable> startImpl(PCEPExtensionProviderContext context) throws Exception;

	@Override
	public synchronized final void start(final PCEPExtensionProviderContext context) throws Exception {
		Preconditions.checkState(this.registrations == null);

		this.registrations = Preconditions.checkNotNull(startImpl(context));
	}

	@Override
	public synchronized final void stop() {
		Preconditions.checkState(this.registrations != null);

		for (final AutoCloseable r : this.registrations) {
			try {
				r.close();
			} catch (final Exception e) {
				LOG.warn("Failed to close registration", e);
			}
		}

		this.registrations = null;
	}
}
