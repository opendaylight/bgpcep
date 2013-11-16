/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

public final class PCEPTopologyProvider extends DefaultTopologyReference implements AutoCloseable {
	private final ServerSessionManager manager;
	private final TopologyProgramming network;
	private final TopologyRPCs element;
	private final Channel channel;

	private PCEPTopologyProvider(final Channel channel, final InstanceIdentifier<Topology> topology, final ServerSessionManager manager,
			final TopologyRPCs element, final TopologyProgramming network) {
		super(topology);
		this.channel = Preconditions.checkNotNull(channel);
		this.manager = Preconditions.checkNotNull(manager);
		this.element = Preconditions.checkNotNull(element);
		this.network = Preconditions.checkNotNull(network);
	}

	public static PCEPTopologyProvider create(final PCEPDispatcher dispatcher,
			final InetSocketAddress address,
			final InstructionScheduler scheduler,
			final DataProviderService dataService,
			final InstanceIdentifier<Topology> topology) throws InterruptedException, ExecutionException {

		final ServerSessionManager manager = new ServerSessionManager(dataService, topology);
		final TopologyRPCs element = new TopologyRPCs(manager);
		final TopologyProgramming network = new TopologyProgramming(scheduler, manager);
		ChannelFuture f = dispatcher.createServer(address, manager);
		f.get();
		return new PCEPTopologyProvider(f.channel(), topology, manager, element, network);
	}

	@Override
	public void close() throws Exception {
		channel.close();
		// FIXME: close other stuff
	}
}
