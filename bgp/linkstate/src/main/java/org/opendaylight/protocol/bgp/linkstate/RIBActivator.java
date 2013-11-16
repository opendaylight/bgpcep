/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Comparator;

import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInFactory;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class RIBActivator implements RIBExtensionProviderActivator {
	private static final Logger LOG = LoggerFactory.getLogger(RIBActivator.class);
	private AutoCloseable reg;

	@Override
	public void startRIBExtensionProvider(final RIBExtensionProviderContext context) {
		Preconditions.checkState(reg == null);

		reg = context.registerAdjRIBsInFactory(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class, new AdjRIBsInFactory() {
			@Override
			public AdjRIBsIn createAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
				return new LinkstateAdjRIBsIn(comparator, key);
			}
		});
	}

	@Override
	public void stopRIBExtensionProvider() {
		if (reg != null) {
			try {
				reg.close();
			} catch (Exception e) {
				LOG.warn("Failed to unregister Linkstate extension", e);
			} finally {
				reg = null;
			}
		}
	}
}
