/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.topology.provider.bgp.impl;

import org.opendaylight.bgpcep.topology.provider.bgp.LocRIBListeners;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.google.common.base.Preconditions;

public class Activator extends AbstractBindingAwareProvider {
	private ServiceRegistration<LocRIBListeners.Subscribtion> ipv4, ipv6, linkstate;
	private BundleContext context;

	@Override
	public void onSessionInitiated(final ProviderContext session) {
		final LocRIBListenerSubscriptionTracker reg = new LocRIBListenerSubscriptionTracker(context, session.getSALService(DataProviderService.class));
		reg.open();

		ipv4 = LocRIBListeners.subscribe(context, Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
				new Ipv4ReachabilityTopologyBuilder(null));
		ipv6 = LocRIBListeners.subscribe(context, Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class,
				new Ipv6ReachabilityTopologyBuilder(null));
//		linkstate = LocRIBListeners.subscribe(context, LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
//				new LinkstateTopologyBuilder(null));
	}

	@Override
	protected void startImpl(final BundleContext context) {
		this.context = Preconditions.checkNotNull(context);
	}
}
