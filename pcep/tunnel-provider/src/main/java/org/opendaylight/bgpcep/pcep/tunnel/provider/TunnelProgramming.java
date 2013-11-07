/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import java.util.concurrent.Future;

import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.TopologyTunnelPcepProgrammingService;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Preconditions;

final class TunnelProgramming implements TopologyTunnelPcepProgrammingService {
	private final InstructionScheduler scheduler;

	TunnelProgramming(final InstructionScheduler scheduler) {
		this.scheduler = Preconditions.checkNotNull(scheduler);
	}

	@Override
	public Future<RpcResult<PcepCreateTunnelOutput>> pcepCreateTunnel(final PcepCreateTunnelInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<PcepDestroyTunnelOutput>> pcepDestroyTunnel(final PcepDestroyTunnelInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<PcepUpdateTunnelOutput>> pcepUpdateTunnel(final PcepUpdateTunnelInput input) {
		// TODO Auto-generated method stub
		return null;
	}

}
