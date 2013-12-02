/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;
import java.util.List;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInFactory;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

import com.google.common.collect.Lists;

public final class RIBActivator extends AbstractRIBExtensionProviderActivator {

	@Override
	protected List<AutoCloseable> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context) {
		return Lists.newArrayList(
				context.registerAdjRIBsInFactory(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, new AdjRIBsInFactory() {
					@Override
					public AdjRIBsIn createAdjRIBsIn(final DataModificationTransaction trans, final Comparator<PathAttributes> comparator, final TablesKey key) {
						return new Ipv4AdjRIBsIn(trans, comparator, key);
					}
				}),
				context.registerAdjRIBsInFactory(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, new AdjRIBsInFactory() {
					@Override
					public AdjRIBsIn createAdjRIBsIn(final DataModificationTransaction trans, final Comparator<PathAttributes> comparator, final TablesKey key) {
						return new Ipv6AdjRIBsIn(trans, comparator, key);
					}
				}));
	}
}
