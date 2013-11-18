/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.topology.provider.bgp.impl;

import org.opendaylight.bgpcep.topology.provider.bgp.LocRIBListener;
import org.opendaylight.bgpcep.topology.provider.bgp.LocRIBListeners;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

final class LocRIBListenerSubscriptionTracker extends ServiceTracker<LocRIBListeners.Subscribtion, ListenerRegistration<LocRIBListener>> {
	private static final InstanceIdentifier<LocRib> locRIBPath = InstanceIdentifier.builder().node(LocRib.class).toInstance();
	private static final Logger LOG = LoggerFactory.getLogger(LocRIBListenerSubscriptionTracker.class);
	private final DataProviderService dps;

	LocRIBListenerSubscriptionTracker(final BundleContext context, final DataProviderService dps) {
		super(context, LocRIBListeners.Subscribtion.class, null);
		this.dps = Preconditions.checkNotNull(dps);
	}

	@Override
	public ListenerRegistration<LocRIBListener> addingService(final ServiceReference<LocRIBListeners.Subscribtion> reference) {
		final LocRIBListeners.Subscribtion service = context.getService(reference);
		if (service == null) {
			LOG.trace("Service for reference {} disappeared", reference);
			return null;
		}

		final InstanceIdentifier<Tables> path = InstanceIdentifier.builder(locRIBPath).
				child(Tables.class, new TablesKey(service.getAfi(), service.getSafi())).toInstance();
		final LocRIBListener listener = service.getLocRIBListener();

		final DataChangeListener dcl = new DataChangeListener() {

			@Override
			public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
				final DataModificationTransaction trans = dps.beginTransaction();

				try {
					listener.onLocRIBChange(trans, change);
				} catch (Exception e) {
					LOG.info("Data change {} was not completely propagated to listener {}", change, listener, e);
				}

				// FIXME: abort the transaction if it's not committing
			}
		};

		final ListenerRegistration<DataChangeListener> reg = dps.registerDataChangeListener(path, dcl);

		return new ListenerRegistration<LocRIBListener>() {
			@Override
			public void close() throws Exception {
				reg.close();
			}

			@Override
			public LocRIBListener getInstance() {
				return listener;
			}
		};
	}

	@Override
	public void removedService(final ServiceReference<LocRIBListeners.Subscribtion> reference, final ListenerRegistration<LocRIBListener> service) {
		try {
			service.close();
		} catch (Exception e) {
			LOG.error("Failed to unregister service {}", e);
		}
		context.ungetService(reference);
	}
}
