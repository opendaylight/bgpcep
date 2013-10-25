/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.topology.tunnel.provider.pcep;

import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.pcc.Lsps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

final class NodeChangedListener implements DataChangeListener {
	private static final Logger LOG = LoggerFactory.getLogger(NodeChangedListener.class);
	private final InstanceIdentifier<Topology> target;
	private final DataProviderService dataProvider;

	NodeChangedListener(final DataProviderService dataProvider, final InstanceIdentifier<Topology> target) {
		this.dataProvider = Preconditions.checkNotNull(dataProvider);
		this.target = Preconditions.checkNotNull(target);
	}

	private void remove(final DataModificationTransaction trans, final InstanceIdentifier<?> id) {
		if (Node.class.equals(id.getTargetType())) {
			// FIXME: implement this
		} else if (Lsps.class.equals(id.getTargetType())) {
			// FIXME: implement this
		} else {
			LOG.debug("Ignoring changed instance {}", id);
		}
	}

	private void create(final DataModificationTransaction trans, final InstanceIdentifier<?> id, final DataObject obj) {
		// FIXME: implement this
	}

	@Override
	public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
		final DataModificationTransaction trans = dataProvider.beginTransaction();

		for (final InstanceIdentifier<?> i : change.getRemovedOperationalData()) {
			remove(trans, i);
		}

		for (final Entry<InstanceIdentifier<?>, DataObject> e : change.getUpdatedOperationalData().entrySet()) {
			remove(trans, e.getKey());
			create(trans, e.getKey(), e.getValue());
		}

		for (final Entry<InstanceIdentifier<?>, DataObject> e : change.getCreatedOperationalData().entrySet()) {
			create(trans, e.getKey(), e.getValue());
		}

		final Future<RpcResult<TransactionStatus>> f = trans.commit();

		// FIXME: change to a subscribtion once that is possible
		try {
			f.get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("Failed to propagate a topology change, target topology became inconsistent", e);
		}
	}
}