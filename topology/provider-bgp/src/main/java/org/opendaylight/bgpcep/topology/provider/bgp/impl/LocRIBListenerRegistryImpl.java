/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.topology.provider.bgp.impl;

import org.opendaylight.bgpcep.topology.provider.bgp.LocRIBListener;
import org.opendaylight.bgpcep.topology.provider.bgp.LocRIBListenerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModification;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.concepts.ListenerRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

final class LocRIBListenerRegistryImpl implements LocRIBListenerRegistry {
	private static final InstanceIdentifier locRIBPath = InstanceIdentifier.builder().node(LocRib.class).toInstance();
	private static final Logger logger = LoggerFactory.getLogger(LocRIBListenerRegistryImpl.class);
	private final DataProviderService dps;

	LocRIBListenerRegistryImpl(final DataProviderService dps) {
		this.dps = Preconditions.checkNotNull(dps);
	}

	@Override
	public ListenerRegistration<LocRIBListener> registerListener(final Class<? extends AddressFamily> afi,
			final Class<? extends SubsequentAddressFamily> safi,
			final LocRIBListener listener) {

		final InstanceIdentifier path = InstanceIdentifier.builder(locRIBPath).
				node(Tables.class, new TablesKey(afi, safi)).toInstance();
		final DataChangeListener dcl = new DataChangeListener() {
			@Override
			public void onDataChange(final DataChangeEvent event) {
				final DataModification trans = dps.beginTransaction();

				try {
					listener.onLocRIBChange(trans, event);
				} catch (Exception e) {
					logger.info("Data change {} was not completely propagated to listener {}", event, listener, e);
				}

				// FIXME: abort the transaction if it's not committing
			}
		};

		dps.registerChangeListener(path, dcl);

		return new ListenerRegistration<LocRIBListener>(listener) {
			@Override
			protected void removeRegistration() {
				dps.unregisterChangeListener(path, dcl);
			}
		};
	}
}
