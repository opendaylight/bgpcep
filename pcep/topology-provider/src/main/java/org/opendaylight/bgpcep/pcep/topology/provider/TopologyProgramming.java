/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.Future;

import org.opendaylight.bgpcep.programming.spi.ExecutionResult;
import org.opendaylight.bgpcep.programming.spi.InstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.FailureBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.failure.Failure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrogrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitRemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitRemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitRemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.FailureType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Preconditions;

/**
 *
 */
final class TopologyProgramming implements NetworkTopologyPcepProgrogrammingService {
	private abstract class AbstractInstructionExecutor implements InstructionExecutor {

		protected abstract io.netty.util.concurrent.Future<OperationResult> executeImpl();

		@Override
		public final io.netty.util.concurrent.Future<ExecutionResult<Details>> execute() {
			final Promise<ExecutionResult<Details>> promise = exec.newPromise();

			executeImpl().addListener(new FutureListener<OperationResult>() {
				@Override
				public void operationComplete(final io.netty.util.concurrent.Future<OperationResult> future) {
					if (future.isSuccess()) {
						final OperationResult res = future.getNow();
						final FailureType fail = res.getFailure();

						final ExecutionResult<Details> result;
						if (fail != null) {
							switch (fail) {
							case Failed:
							case NoAck:
								result = new ExecutionResult<Details>(InstructionStatus.Failed, null);
								break;
							case Unsent:
								result = new ExecutionResult<Details>(InstructionStatus.Cancelled, null);
								break;
							}

							throw new IllegalStateException("Unhandled operation state " + fail);
						} else {
							result = new ExecutionResult<Details>(InstructionStatus.Successful, null);
						}

						promise.setSuccess(result);
					} else {
						promise.setFailure(future.cause());
					}
				}
			});

			return promise;
		}
	}

	private final InstructionScheduler scheduler;
	private final ServerSessionManager manager;
	private final EventExecutor exec;

	TopologyProgramming(final EventExecutor executor, final InstructionScheduler scheduler, final ServerSessionManager manager) {
		this.scheduler = Preconditions.checkNotNull(scheduler);
		this.manager = Preconditions.checkNotNull(manager);
		this.exec = Preconditions.checkNotNull(executor);
	}

	@Override
	public Future<RpcResult<SubmitAddLspOutput>> submitAddLsp(final SubmitAddLspInput input) {
		Preconditions.checkArgument(input.getNode() != null);
		Preconditions.checkArgument(input.getName() != null);

		final InstructionExecutor e = new AbstractInstructionExecutor() {
			@Override
			public io.netty.util.concurrent.Future<OperationResult> executeImpl() {
				return manager.realAddLsp(input);
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final SubmitAddLspOutputBuilder b = new SubmitAddLspOutputBuilder();
		if (f != null) {
			b.setResult(new FailureBuilder().setFailure(f).build());
		}

		final RpcResult<SubmitAddLspOutput> res = SuccessfulRpcResult.create(b.build());
		return exec.newSucceededFuture(res);
	}

	@Override
	public Future<RpcResult<SubmitRemoveLspOutput>> submitRemoveLsp(final SubmitRemoveLspInput input) {
		Preconditions.checkArgument(input.getNode() != null);
		Preconditions.checkArgument(input.getName() != null);

		final InstructionExecutor e = new AbstractInstructionExecutor() {
			@Override
			protected io.netty.util.concurrent.Future<OperationResult> executeImpl() {
				return manager.realRemoveLsp(input);
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final SubmitRemoveLspOutputBuilder b = new SubmitRemoveLspOutputBuilder();
		if (f != null) {
			b.setResult(new FailureBuilder().setFailure(f).build());
		}

		final RpcResult<SubmitRemoveLspOutput> res = SuccessfulRpcResult.create(b.build());
		return exec.newSucceededFuture(res);
	}

	@Override
	public Future<RpcResult<SubmitUpdateLspOutput>> submitUpdateLsp(final SubmitUpdateLspInput input) {
		Preconditions.checkArgument(input.getNode() != null);
		Preconditions.checkArgument(input.getName() != null);

		final InstructionExecutor e = new AbstractInstructionExecutor() {
			@Override
			protected io.netty.util.concurrent.Future<OperationResult> executeImpl() {
				return manager.realUpdateLsp(input);
			}
		};

		final Failure f = this.scheduler.submitInstruction(input, e);
		final SubmitUpdateLspOutputBuilder b = new SubmitUpdateLspOutputBuilder();
		if (f != null) {
			b.setResult(new FailureBuilder().setFailure(f).build());
		}

		final RpcResult<SubmitUpdateLspOutput> res = SuccessfulRpcResult.create(b.build());
		return exec.newSucceededFuture(res);
	}

}
