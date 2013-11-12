/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.topology.provider.bgp;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.Item;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public abstract class AbstractLocRIBListener<T extends Route> implements LocRIBListener {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractLocRIBListener.class);
	private final InstanceIdentifier<Topology> topology;
	private final Item<T> item;

	protected AbstractLocRIBListener(final InstanceIdentifier<Topology> topology, final Class<T> idClass) {
		this.topology = Preconditions.checkNotNull(topology);
		this.item = new Item<T>(Preconditions.checkNotNull(idClass));
	}

	public final InstanceIdentifier<Topology> getTopology() {
		return topology;
	}

	protected abstract void createObject(DataModification<InstanceIdentifier<?>, DataObject> trans, InstanceIdentifier<T> id, T value);
	protected abstract void removeObject(DataModification<InstanceIdentifier<?>, DataObject> trans, InstanceIdentifier<T> id, T value);

	private InstanceIdentifier<T> changedObject(final InstanceIdentifier<?> id) {
		final List<PathArgument> p = id.getPath();

		final int i = p.indexOf(item);
		Preconditions.checkArgument(i > -1, "Class %s not found in identifier %s", item.getType(), id);
		return new InstanceIdentifier<>(p.subList(0, i + 1), item.getType());
	}

	@Override
	public final void onLocRIBChange(final DataModification<InstanceIdentifier<?>, DataObject> trans,
			final DataChangeEvent<InstanceIdentifier<?>, DataObject> event) {

		final Set<InstanceIdentifier<T>> ids = new HashSet<>();
		for (final InstanceIdentifier<?> i : event.getRemovedOperationalData()) {
			ids.add(Preconditions.checkNotNull(changedObject(i)));
		}
		for (final InstanceIdentifier<?> i : event.getUpdatedOperationalData().keySet()) {
			ids.add(Preconditions.checkNotNull(changedObject(i)));
		}
		for (final InstanceIdentifier<?> i : event.getCreatedOperationalData().keySet()) {
			ids.add(Preconditions.checkNotNull(changedObject(i)));
		}

		final Map<InstanceIdentifier<?>, DataObject> o = event.getOriginalOperationalData();
		final Map<InstanceIdentifier<?>, DataObject> n = event.getUpdatedOperationalData();
		for (final InstanceIdentifier<T> i : ids) {
			final T oldValue = item.getType().cast(o.get(i));
			final T newValue = item.getType().cast(n.get(i));

			LOG.debug("Updating object {} value {} -> {}", i, oldValue, newValue);
			if (oldValue != null) {
				removeObject(trans, i, oldValue);
			}
			if (newValue != null) {
				createObject(trans, i, newValue);
			}
		}
	}
}
