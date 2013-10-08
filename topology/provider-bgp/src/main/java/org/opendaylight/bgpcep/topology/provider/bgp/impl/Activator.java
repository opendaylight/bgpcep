/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.topology.provider.bgp.impl;

import org.opendaylight.bgpcep.topology.provider.bgp.LocRIBListenerRegistry;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.google.common.base.Preconditions;

public class Activator extends AbstractBindingAwareProvider {
	private ServiceRegistration<LocRIBListenerRegistry> registration;
	private BundleContext bundle;

	@Override
	public void onSessionInitialized(final ConsumerContext session) {
		// Not interesting
	}

	@Override
	public void onSessionInitiated(final ProviderContext session) {
		final DataProviderService dps = Preconditions.checkNotNull(session.getSALService(DataProviderService.class));
		registration = bundle.registerService(LocRIBListenerRegistry.class,	new LocRIBListenerRegistryImpl(dps), null);
	}

	@Override
	protected void startImpl(final BundleContext context) {
		this.bundle = Preconditions.checkNotNull(context);
	}
}
