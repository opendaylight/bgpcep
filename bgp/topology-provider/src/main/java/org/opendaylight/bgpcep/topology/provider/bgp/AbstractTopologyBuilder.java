/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.topology.provider.bgp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.protocol.concepts.InstanceIdentifierUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;

public abstract class AbstractTopologyBuilder<T extends Route> implements AutoCloseable, LocRIBListener, TopologyReference {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractTopologyBuilder.class);
	private final InstanceIdentifier<Topology> topology;
	private final Class<T> idClass;

	protected AbstractTopologyBuilder(final TopologyId topologyId, final Class<T> idClass) {
		this.topology = InstanceIdentifier.builder().node(Topology.class, new TopologyKey(Preconditions.checkNotNull(topologyId))).toInstance();
		this.idClass = Preconditions.checkNotNull(idClass);
	}

	@Override
	public final InstanceIdentifier<Topology> getInstanceIdentifier() {
		return topology;
	}

	protected abstract void createObject(DataModification<InstanceIdentifier<?>, DataObject> trans, InstanceIdentifier<T> id, T value);
	protected abstract void removeObject(DataModification<InstanceIdentifier<?>, DataObject> trans, InstanceIdentifier<T> id, T value);

	private InstanceIdentifier<T> changedObject(final InstanceIdentifier<?> id) {
		return InstanceIdentifierUtil.firstIdentifierOf(id, idClass);
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
			final T oldValue = idClass.cast(o.get(i));
			final T newValue = idClass.cast(n.get(i));

			LOG.debug("Updating object {} value {} -> {}", i, oldValue, newValue);
			if (oldValue != null) {
				removeObject(trans, i, oldValue);
			}
			if (newValue != null) {
				createObject(trans, i, newValue);
			}
		}

		Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()),
				new FutureCallback<RpcResult<TransactionStatus>>() {
			@Override
			public void onSuccess(final RpcResult<TransactionStatus> result) {
				// Nothing to do
			}

			@Override
			public void onFailure(final Throwable t) {
				LOG.error("Failed to propagate change by listener {}", AbstractTopologyBuilder.this);
			}
		});
	}

	@Override
	public final void close() throws Exception {
		// FIXME: remove data once we can get to a transaction
	}
}
