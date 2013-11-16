/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

public final class PCEPTunnelTopologyProvider implements AutoCloseable {
	final ListenerRegistration<DataChangeListener> reg;

	private PCEPTunnelTopologyProvider(final ListenerRegistration<DataChangeListener> reg) {
		this.reg = Preconditions.checkNotNull(reg);
	}

	public static PCEPTunnelTopologyProvider create(final DataProviderService dataProvider,
			final InstanceIdentifier<Topology> sourceTopology, final TopologyId targetTopology) {
		final InstanceIdentifier<Topology> dst =
				InstanceIdentifier.builder().node(NetworkTopology.class).
				child(Topology.class, new TopologyKey(targetTopology)).toInstance();
		final NodeChangedListener ncl = new NodeChangedListener(dataProvider, dst);

		final InstanceIdentifier<Node> src = InstanceIdentifier.builder(sourceTopology).child(Node.class).toInstance();
		final ListenerRegistration<DataChangeListener> reg = dataProvider.registerDataChangeListener(src, ncl);

		return new PCEPTunnelTopologyProvider(reg);
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}
}
