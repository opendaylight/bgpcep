/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;

import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInFactory;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class RIBActivator implements RIBExtensionProviderActivator {
	private static final Logger LOG = LoggerFactory.getLogger(RIBActivator.class);
	private AutoCloseable v4reg, v6reg;

	@Override
	public void startRIBExtensionProvider(final RIBExtensionProviderContext context) {
		Preconditions.checkState(v4reg == null);
		Preconditions.checkState(v6reg == null);

		v4reg = context.registerAdjRIBsInFactory(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, new AdjRIBsInFactory() {
			@Override
			public AdjRIBsIn createAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
				return new Ipv4AdjRIBsIn(comparator, key);
			}
		});

		v6reg = context.registerAdjRIBsInFactory(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, new AdjRIBsInFactory() {
			@Override
			public AdjRIBsIn createAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
				return new Ipv6AdjRIBsIn(comparator, key);
			}
		});

	}

	@Override
	public void stopRIBExtensionProvider() {
		if (v4reg != null) {
			try {
				v4reg.close();
			} catch (Exception e) {
				LOG.warn("Failed to unregister IPv4 extension", e);
			} finally {
				v4reg = null;
			}
		}
		if (v6reg != null) {
			try {
				v6reg.close();
			} catch (Exception e) {
				LOG.warn("Failed to unregister IPv6 extension", e);
			} finally {
				v6reg = null;
			}
		}
	}
}
