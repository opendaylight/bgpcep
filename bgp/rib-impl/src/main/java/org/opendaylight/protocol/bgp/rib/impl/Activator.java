/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInFactory;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

// FIXME: rework in terms of RIBExtensionProviderActivator
public final class Activator extends AbstractBindingAwareProvider {
	@SuppressWarnings("unused")
	private RIBImpl rib;

	@Override
	public void onSessionInitiated(final ProviderContext session) {
		final RIBExtensionProviderContext reg = new AdjRIBsInFactoryRegistryImpl();

		reg.registerAdjRIBsInFactory(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, new AdjRIBsInFactory() {
			@Override
			public AdjRIBsIn createAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
				return new Ipv4AdjRIBsIn(comparator, key);
			}
		});
		reg.registerAdjRIBsInFactory(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, new AdjRIBsInFactory() {
			@Override
			public AdjRIBsIn createAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
				return new Ipv6AdjRIBsIn(comparator, key);
			}
		});


		// FIXME: publish the registry
		this.rib = new RIBImpl(reg, session.getSALService(DataProviderService.class));
	}
}
