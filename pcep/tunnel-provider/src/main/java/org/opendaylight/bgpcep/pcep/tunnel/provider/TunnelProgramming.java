/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;


import org.opendaylight.bgpcep.pcep.topology.spi.AbstractTopologyProgrammingExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.FailureBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.failure.Failure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.TopologyTunnelPcepProgrammingService;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

final class TunnelProgramming implements TopologyTunnelPcepProgrammingService {
	private final NetworkTopologyPcepService pcepTopology;
	private final InstructionScheduler scheduler;

	TunnelProgramming(final InstructionScheduler scheduler, final NetworkTopologyPcepService pcepTopology) {
		this.scheduler = Preconditions.checkNotNull(scheduler);
		this.pcepTopology = Preconditions.checkNotNull(pcepTopology);
	}

	@Override
	public ListenableFuture<RpcResult<PcepCreateP2pTunnelOutput>> pcepCreateP2pTunnel(final PcepCreateP2pTunnelInput input) {
		Preconditions.checkNotNull(input.getLinkId());
		Preconditions.checkNotNull(input.getSourceTp());
		Preconditions.checkNotNull(input.getDestinationTp());

		final AddLspInputBuilder ab = new AddLspInputBuilder();
		ab.fieldsFrom(input);

		final InstructionExecutor e = new AbstractTopologyProgrammingExecutor() {
			@Override
			protected ListenableFuture<OperationResult> executeImpl() {
				final ListenableFuture<RpcResult<AddLspOutput>> s =
						(ListenableFuture<RpcResult<AddLspOutput>>) pcepTopology.addLsp(ab.build());

				return Futures.transform(s, new Function<RpcResult<AddLspOutput>, OperationResult>() {
					@Override
					public OperationResult apply(final RpcResult<AddLspOutput> input) {
						return input.getResult();
					}
				});
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final PcepCreateP2pTunnelOutputBuilder b = new PcepCreateP2pTunnelOutputBuilder();
		if (f != null) {
			b.setResult(new FailureBuilder().setFailure(f).build());
		}

		final RpcResult<PcepCreateP2pTunnelOutput> res = SuccessfulRpcResult.create(b.build());
		return Futures.immediateFuture(res);
	}

	@Override
	public ListenableFuture<RpcResult<PcepDestroyTunnelOutput>> pcepDestroyTunnel(final PcepDestroyTunnelInput input) {
		Preconditions.checkNotNull(input.getLinkId());

		final RemoveLspInputBuilder ab = new RemoveLspInputBuilder();
		ab.fieldsFrom(input);

		final InstructionExecutor e = new AbstractTopologyProgrammingExecutor() {
			@Override
			protected ListenableFuture<OperationResult> executeImpl() {
				final ListenableFuture<RpcResult<RemoveLspOutput>> s =
						(ListenableFuture<RpcResult<RemoveLspOutput>>) pcepTopology.removeLsp(ab.build());

				return Futures.transform(s, new Function<RpcResult<RemoveLspOutput>, OperationResult>() {
					@Override
					public OperationResult apply(final RpcResult<RemoveLspOutput> input) {
						return input.getResult();
					}
				});
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final PcepDestroyTunnelOutputBuilder b = new PcepDestroyTunnelOutputBuilder();
		if (f != null) {
			b.setResult(new FailureBuilder().setFailure(f).build());
		}

		final RpcResult<PcepDestroyTunnelOutput> res = SuccessfulRpcResult.create(b.build());
		return Futures.immediateFuture(res);
	}

	@Override
	public ListenableFuture<RpcResult<PcepUpdateTunnelOutput>> pcepUpdateTunnel(final PcepUpdateTunnelInput input) {
		Preconditions.checkNotNull(input.getLinkId());

		final UpdateLspInputBuilder ab = new UpdateLspInputBuilder();
		ab.fieldsFrom(input);

		final InstructionExecutor e = new AbstractTopologyProgrammingExecutor() {
			@Override
			protected ListenableFuture<OperationResult> executeImpl() {
				final ListenableFuture<RpcResult<UpdateLspOutput>> s =
						(ListenableFuture<RpcResult<UpdateLspOutput>>) pcepTopology.updateLsp(ab.build());

				return Futures.transform(s, new Function<RpcResult<UpdateLspOutput>, OperationResult>() {
					@Override
					public OperationResult apply(final RpcResult<UpdateLspOutput> input) {
						return input.getResult();
					}
				});
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final PcepUpdateTunnelOutputBuilder b = new PcepUpdateTunnelOutputBuilder();
		if (f != null) {
			b.setResult(new FailureBuilder().setFailure(f).build());
		}

		final RpcResult<PcepUpdateTunnelOutput> res = SuccessfulRpcResult.create(b.build());
		return Futures.immediateFuture(res);
	}
}
