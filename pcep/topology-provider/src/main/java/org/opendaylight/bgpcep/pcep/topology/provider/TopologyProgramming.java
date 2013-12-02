/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import org.opendaylight.bgpcep.pcep.topology.spi.AbstractTopologyProgrammingExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.FailureCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.failure._case.Failure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitEnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitEnsureLspOperationalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitEnsureLspOperationalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitRemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitRemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitRemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 */
final class TopologyProgramming implements NetworkTopologyPcepProgrammingService {
	private final InstructionScheduler scheduler;
	private final ServerSessionManager manager;

	TopologyProgramming(final InstructionScheduler scheduler, final ServerSessionManager manager) {
		this.scheduler = Preconditions.checkNotNull(scheduler);
		this.manager = Preconditions.checkNotNull(manager);
	}

	@Override
	public ListenableFuture<RpcResult<SubmitAddLspOutput>> submitAddLsp(final SubmitAddLspInput input) {
		Preconditions.checkArgument(input.getNode() != null);
		Preconditions.checkArgument(input.getName() != null);

		final InstructionExecutor e = new AbstractTopologyProgrammingExecutor() {
			@Override
			public ListenableFuture<OperationResult> executeImpl() {
				return TopologyProgramming.this.manager.realAddLsp(input);
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final SubmitAddLspOutputBuilder b = new SubmitAddLspOutputBuilder();
		if (f != null) {
			b.setResult(new FailureCaseBuilder().setFailure(f).build());
		}

		final RpcResult<SubmitAddLspOutput> res = SuccessfulRpcResult.create(b.build());
		return Futures.immediateFuture(res);
	}

	@Override
	public ListenableFuture<RpcResult<SubmitRemoveLspOutput>> submitRemoveLsp(final SubmitRemoveLspInput input) {
		Preconditions.checkArgument(input.getNode() != null);
		Preconditions.checkArgument(input.getName() != null);

		final InstructionExecutor e = new AbstractTopologyProgrammingExecutor() {
			@Override
			protected ListenableFuture<OperationResult> executeImpl() {
				return TopologyProgramming.this.manager.realRemoveLsp(input);
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final SubmitRemoveLspOutputBuilder b = new SubmitRemoveLspOutputBuilder();
		if (f != null) {
			b.setResult(new FailureCaseBuilder().setFailure(f).build());
		}

		final RpcResult<SubmitRemoveLspOutput> res = SuccessfulRpcResult.create(b.build());
		return Futures.immediateFuture(res);
	}

	@Override
	public ListenableFuture<RpcResult<SubmitUpdateLspOutput>> submitUpdateLsp(final SubmitUpdateLspInput input) {
		Preconditions.checkArgument(input.getNode() != null);
		Preconditions.checkArgument(input.getName() != null);

		final InstructionExecutor e = new AbstractTopologyProgrammingExecutor() {
			@Override
			protected ListenableFuture<OperationResult> executeImpl() {
				return TopologyProgramming.this.manager.realUpdateLsp(input);
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final SubmitUpdateLspOutputBuilder b = new SubmitUpdateLspOutputBuilder();
		if (f != null) {
			b.setResult(new FailureCaseBuilder().setFailure(f).build());
		}

		final RpcResult<SubmitUpdateLspOutput> res = SuccessfulRpcResult.create(b.build());
		return Futures.immediateFuture(res);
	}

	@Override
	public ListenableFuture<RpcResult<SubmitEnsureLspOperationalOutput>> submitEnsureLspOperational(
			final SubmitEnsureLspOperationalInput input) {
		Preconditions.checkArgument(input.getNode() != null);
		Preconditions.checkArgument(input.getName() != null);
		Preconditions.checkArgument(input.getArguments() != null);
		Preconditions.checkArgument(input.getArguments().getOperational() != null);

		final InstructionExecutor e = new AbstractTopologyProgrammingExecutor() {
			@Override
			protected ListenableFuture<OperationResult> executeImpl() {
				return TopologyProgramming.this.manager.realEnsureLspOperational(new EnsureLspOperationalInputBuilder(input).build());
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final SubmitEnsureLspOperationalOutputBuilder b = new SubmitEnsureLspOperationalOutputBuilder();
		if (f != null) {
			b.setResult(new FailureCaseBuilder().setFailure(f).build());
		}

		final RpcResult<SubmitEnsureLspOperationalOutput> res = SuccessfulRpcResult.create(b.build());
		return Futures.immediateFuture(res);
	}
}
