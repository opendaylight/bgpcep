/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.tunnel.pcep;

import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;

import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

public final class TopologyExporter {
	private static final InstanceIdentifier<Nodes> inventory = new InstanceIdentifier<Nodes>(Nodes.class);
	private final PCEPDispatcher dispatcher;
	private final DataProviderService dataProvider;
	private final InstanceIdentifier<Topology> topology;

	public TopologyExporter(final PCEPDispatcher dispatcher,
			final DataProviderService dataService,
			final InstanceIdentifier<Topology> topology) {
		this.dispatcher = Preconditions.checkNotNull(dispatcher);
		this.dataProvider = Preconditions.checkNotNull(dataService);
		this.topology = Preconditions.checkNotNull(topology);
	}

	public ChannelFuture startServer(final InetSocketAddress address) {
		return dispatcher.createServer(address, new ServerSessionManager(dataProvider, inventory, topology));
	}
}
