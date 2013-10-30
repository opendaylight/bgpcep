/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

final class TunnelTopologyExporter {
	private final DataProviderService dataProvider;
	private final InstanceIdentifier<Node> srcTree;

	TunnelTopologyExporter(final DataProviderService dataProvider, final InstanceIdentifier<Topology> sourceTopology) {
		this.dataProvider = Preconditions.checkNotNull(dataProvider);
		srcTree = Preconditions.checkNotNull(InstanceIdentifier.builder(sourceTopology).node(Node.class).toInstance());
	}

	ListenerRegistration<?> addTargetTopology(final InstanceIdentifier<Topology> tunnelTopology) {
		return dataProvider.registerDataChangeListener(srcTree, new NodeChangedListener(dataProvider, tunnelTopology));
	}
}
