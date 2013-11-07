/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;

import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

public final class PCEPTopologyProvider {
	private final PCEPDispatcher dispatcher;
	private final TopologyProgramming topology;
	private final ServerSessionManager manager;
	private final TopologyRPCs element;

	public PCEPTopologyProvider(final PCEPDispatcher dispatcher,
			final InstructionScheduler scheduler,
			final DataProviderService dataService,
			final InstanceIdentifier<Topology> topology) {
		this.dispatcher = Preconditions.checkNotNull(dispatcher);


		this.manager = new ServerSessionManager(dataService, topology);
		this.element = new TopologyRPCs(manager);
		this.topology = new TopologyProgramming(scheduler, manager);
	}

	public ChannelFuture startServer(final InetSocketAddress address) {
		return dispatcher.createServer(address, manager);
	}
}
