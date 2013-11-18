/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import java.util.concurrent.Executors;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.ProgrammingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class BundleActivator extends AbstractBindingAwareProvider {
	private static final Logger LOG = LoggerFactory.getLogger(BundleActivator.class);
	private RpcRegistration<ProgrammingService> reg;
	private final Timer timer = new HashedWheelTimer();

	@Override
	public void onSessionInitiated(final ProviderContext session) {
		final NotificationProviderService notifs = Preconditions.checkNotNull(session.getSALService(NotificationProviderService.class));

		reg = Preconditions.checkNotNull(session.addRpcImplementation(ProgrammingService.class,
				new ProgrammingServiceImpl(notifs, Executors.newSingleThreadExecutor(), timer)));
	}
}
