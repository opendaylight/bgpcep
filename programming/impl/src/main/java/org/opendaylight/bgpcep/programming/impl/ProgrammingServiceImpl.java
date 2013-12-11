/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.bgpcep.programming.NanotimeUtil;
import org.opendaylight.bgpcep.programming.spi.ExecutionResult;
import org.opendaylight.bgpcep.programming.spi.InstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.DeadOnArrival;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.DuplicateInstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatusChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.Nanotime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.ProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UncancellableInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UnknownInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UnknownPreconditionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.DetailsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.failure._case.Failure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.failure._case.FailureBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public final class ProgrammingServiceImpl implements InstructionScheduler, ProgrammingService, AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(ProgrammingServiceImpl.class);

	// Default stop timeout, in seconds
	private static final long CLOSE_TIMEOUT = 5;

	private final Map<InstructionId, Instruction> insns = new HashMap<>();

	@GuardedBy("this")
	private final Deque<Instruction> readyQueue = new ArrayDeque<>();

	private final NotificationProviderService notifs;
	private final ListeningExecutorService executor;
	private final Timer timer;
	private Future<Void> thread;
	private ExecutorService exec;

	public ProgrammingServiceImpl(final NotificationProviderService notifs, final ListeningExecutorService executor, final Timer timer) {
		this.notifs = Preconditions.checkNotNull(notifs);
		this.executor = Preconditions.checkNotNull(executor);
		this.timer = Preconditions.checkNotNull(timer);
	}

	@Override
	public ListenableFuture<RpcResult<CancelInstructionOutput>> cancelInstruction(final CancelInstructionInput input) {
		return this.executor.submit(new Callable<RpcResult<CancelInstructionOutput>>() {
			@Override
			public RpcResult<CancelInstructionOutput> call() {
				return realCancelInstruction(input);
			}
		});
	}

	@Override
	public ListenableFuture<RpcResult<CleanInstructionsOutput>> cleanInstructions(final CleanInstructionsInput input) {
		return this.executor.submit(new Callable<RpcResult<CleanInstructionsOutput>>() {
			@Override
			public RpcResult<CleanInstructionsOutput> call() throws Exception {
				return realCleanInstructions(input);
			}
		});
	}

	private synchronized RpcResult<CancelInstructionOutput> realCancelInstruction(final CancelInstructionInput input) {
		final Instruction i = this.insns.get(input.getId());
		if (i == null) {
			LOG.debug("Instruction {} not present in the graph", input.getId());

			final CancelInstructionOutput out = new CancelInstructionOutputBuilder().setFailure(UnknownInstruction.class).build();
			return SuccessfulRpcResult.create(out);
		}

		switch (i.getStatus()) {
		case Cancelled:
		case Executing:
		case Failed:
		case Successful:
		case Unknown:
			LOG.debug("Instruction {} can no longer be cancelled due to status {}", input.getId(), i.getStatus());
			return SuccessfulRpcResult.create(new CancelInstructionOutputBuilder().setFailure(UncancellableInstruction.class).build());
		case Queued:
		case Scheduled:
			break;
		}

		cancelInstruction(i, null);
		return SuccessfulRpcResult.create(new CancelInstructionOutputBuilder().build());
	}


	private synchronized RpcResult<CleanInstructionsOutput> realCleanInstructions(final CleanInstructionsInput input) {
		final List<InstructionId> failed = new ArrayList<>();

		for (final InstructionId id : input.getId()) {
			// Find the instruction
			final Instruction i = this.insns.get(input.getId());
			if (i == null) {
				LOG.debug("Instruction {} not present in the graph", input.getId());
				failed.add(id);
				continue;
			}

			// Check its status
			switch (i.getStatus()) {
			case Cancelled:
			case Failed:
			case Successful:
				break;
			case Executing:
			case Queued:
			case Scheduled:
			case Unknown:
				LOG.debug("Instruction {} cannot be cleaned because of it's in state {}", id, i.getStatus());
				failed.add(id);
				continue;
			}

			// The instruction is in a terminal state, we need to just unlink
			// it from its dependencies and dependants
			i.clean();

			insns.remove(i);
			LOG.debug("Instruction {} cleaned successfully", id);
		}

		final CleanInstructionsOutputBuilder ob = new CleanInstructionsOutputBuilder();
		if (!failed.isEmpty()) {
			ob.setUnflushed(failed);
		}

		return SuccessfulRpcResult.create(ob.build());
	}

	@Override
	public Failure submitInstruction(final SubmitInstructionInput input, final InstructionExecutor executor) {
		final InstructionId id = input.getId();
		if (this.insns.get(id) != null) {
			LOG.info("Instruction ID {} already present", id);
			return new FailureBuilder().setType(DuplicateInstructionId.class).build();
		}

		// First things first: check the deadline
		final Nanotime now = NanotimeUtil.currentTime();
		final BigInteger left = input.getDeadline().getValue().subtract(now.getValue());

		if (left.compareTo(BigInteger.ZERO) <= 0) {
			LOG.debug("Instruction {} deadline has already passed by {}ns", id, left);
			return new FailureBuilder().setType(DeadOnArrival.class).build();
		}

		// Resolve dependencies
		final List<Instruction> dependencies = new ArrayList<>();
		for (final InstructionId pid : input.getPreconditions()) {
			final Instruction i = this.insns.get(pid);
			if (i == null) {
				LOG.info("Instruction {} depends on {}, which is not a known instruction", id, pid);
				return new FailureBuilder().setType(UnknownPreconditionId.class).build();
			}

			dependencies.add(i);
		}

		// Check if all dependencies are non-failed
		final List<InstructionId> unmet = new ArrayList<>();
		for (final Instruction d : dependencies) {
			switch (d.getStatus()) {
			case Cancelled:
			case Failed:
			case Unknown:
				unmet.add(d.getId());
				break;
			case Executing:
			case Queued:
			case Scheduled:
			case Successful:
				break;
			}
		}

		/*
		 *  Some dependencies have failed, declare the request dead-on-arrival
		 *  and fail the operation.
		 */
		if (!unmet.isEmpty()) {
			return new FailureBuilder().setType(DeadOnArrival.class).setFailedPreconditions(unmet).build();
		}

		/*
		 * All pre-flight checks done are at this point, the following
		 * steps can only fail in catastrophic scenarios (OOM and the
		 * like).
		 */

		// Schedule a timeout for the instruction
		final Timeout t = this.timer.newTimeout(new TimerTask() {
			@Override
			public void run(final Timeout timeout) throws Exception {
				timeoutInstruction(input.getId());
			}
		}, left.longValue(), TimeUnit.NANOSECONDS);

		// Put it into the instruction list
		final Instruction i = new Instruction(input.getId(), executor, dependencies, t);
		this.insns.put(id, i);

		// Attach it into its dependencies
		for (final Instruction d : dependencies) {
			d.addDependant(i);
		}

		/*
		 * All done. The next part is checking whether the instruction can
		 * run, which we can figure out after sending out the acknowledgement.
		 * This task should be ingress-weighed, so we reinsert it into the
		 * same execution service.
		 */
		this.executor.submit(new Runnable() {
			@Override
			public void run() {
				tryScheduleInstruction(i);
			}
		});

		return null;
	}

	@GuardedBy("this")
	private void transitionInstruction(final Instruction v, final InstructionStatus status, final Details details) {
		// Set the status
		v.setStatus(status);

		LOG.debug("Instruction {} transitioned to status {}", v.getId(), status);

		// Send out a notification
		this.notifs.publish(new InstructionStatusChangedBuilder().setId(v.getId()).setStatus(status).setDetails(details).build());
	}

	@GuardedBy("this")
	private void cancelSingle(final Instruction i, final Details details) {
		// Stop the timeout
		i.cancel();

		// Set the new status and send out notification
		transitionInstruction(i, InstructionStatus.Cancelled, details);
	}

	@GuardedBy("this")
	private void cancelDependants(final Instruction v) {
		final Details details = new DetailsBuilder().setUnmetDependencies(ImmutableList.of(v.getId())).build();
		for (final Instruction d : v.getDependants()) {
			switch (d.getStatus()) {
			case Cancelled:
			case Executing:
			case Failed:
			case Successful:
			case Unknown:
				break;
			case Queued:
			case Scheduled:
				cancelSingle(d, details);
				cancelDependants(d);
				break;
			}
		}
	}

	private synchronized void cancelInstruction(final Instruction i, final Details details) {
		this.readyQueue.remove(i);
		cancelSingle(i, details);
		cancelDependants(i);
	}

	private synchronized void timeoutInstruction(final InstructionId id) {
		final Instruction i = this.insns.get(id);
		if (i == null) {
			LOG.warn("Instruction {} timed out, but not found in the queue", id);
			return;
		}

		switch (i.getStatus()) {
		case Cancelled:
		case Failed:
		case Successful:
			LOG.debug("Instruction {} has status {}, timeout is a no-op", id, i.getStatus());
			break;
		case Unknown:
			LOG.warn("Instruction {} has status {} before timeout completed", id, i.getStatus());
			break;
		case Executing:
			LOG.info("Instruction {} timed out while executing, transitioning into Unknown", id);
			transitionInstruction(i, InstructionStatus.Unknown, null);
			cancelDependants(i);
			break;
		case Queued:
			LOG.debug("Instruction {} timed out while Queued, cancelling it", id);

			final List<InstructionId> ids = new ArrayList<>();
			for (final Instruction d : i.getDependencies()) {
				if (d.getStatus() != InstructionStatus.Successful) {
					ids.add(d.getId());
				}
			}

			cancelInstruction(i, new DetailsBuilder().setUnmetDependencies(ids).build());
			break;
		case Scheduled:
			LOG.debug("Instruction {} timed out while Scheduled, cancelling it", i.getId());
			// FIXME: BUG-191: we should provide details why it timed out while scheduled
			cancelInstruction(i, null);
			break;
		}
	}

	@GuardedBy("this")
	private synchronized void tryScheduleInstruction(final Instruction i) {
		Preconditions.checkState(i.getStatus().equals(InstructionStatus.Queued));

		/*
		 * Check all vertices we depend on. We start off as ready for
		 * scheduling. If we encounter a cancelled/failed/unknown
		 * dependency, we cancel this instruction (and cascade). If we
		 * encounter an executing/queued/scheduled dependency, we hold
		 * of scheduling this one.
		 */
		boolean ready = true;

		final List<InstructionId> unmet = new ArrayList<>();
		for (final Instruction d : i.getDependencies()) {
			switch (d.getStatus()) {
			case Cancelled:
			case Failed:
			case Unknown:
				unmet.add(d.getId());
				break;
			case Executing:
			case Queued:
			case Scheduled:
				ready = false;
				break;
			case Successful:
				// No-op
				break;
			}
		}

		if (!unmet.isEmpty()) {
			LOG.debug("Instruction {} was Queued, while some dependencies were resolved unsuccessfully, cancelling it", i.getId());
			cancelSingle(i, new DetailsBuilder().setUnmetDependencies(unmet).build());
			cancelDependants(i);
			return;
		}

		if (ready) {
			LOG.debug("Instruction {} is ready for execution", i.getId());
			transitionInstruction(i, InstructionStatus.Scheduled, null);

			this.readyQueue.add(i);
			notify();
		}
	}

	private synchronized void executionFailed(final Instruction i, final Throwable cause) {
		LOG.error("Instruction {} failed to execute", i.getId(), cause);
		transitionInstruction(i, InstructionStatus.Failed, null);
		cancelDependants(i);
	}

	private synchronized void executionSuccessful(final Instruction i, final ExecutionResult<?> res) {
		i.cancel();

		transitionInstruction(i, res.getStatus(), res.getDetails());

		// Walk all dependants and try to schedule them
		for (final Instruction d : i.getDependants()) {
			tryScheduleInstruction(d);
		}
	}

	private synchronized void processQueues() throws InterruptedException {
		/*
		 * This method is only ever interrupted by InterruptedException
		 */
		while (true) {
			while (!this.readyQueue.isEmpty()) {
				final Instruction i = this.readyQueue.poll();

				Preconditions.checkState(i.getStatus().equals(InstructionStatus.Scheduled));

				transitionInstruction(i, InstructionStatus.Executing, null);
				Futures.addCallback(i.execute(), new FutureCallback<ExecutionResult<Details>>() {

					@Override
					public void onSuccess(final ExecutionResult<Details> result) {
						executionSuccessful(i, result);
					}

					@Override
					public void onFailure(final Throwable t) {
						executionFailed(i, t);
					}
				});
			}

			wait();
		}
	}

	synchronized void start(final ThreadFactory threadFactory) {
		Preconditions.checkState(this.exec == null, "Programming service dispatch thread already started");

		this.exec = Executors.newSingleThreadExecutor(threadFactory);
		this.thread = this.exec.submit(new Callable<Void>() {
			@Override
			public Void call() {
				try {
					processQueues();
				} catch (final InterruptedException ex) {
					LOG.error("Programming service dispatch thread died", ex);
				}
				return null;
			}
		});
		this.exec.shutdown();
	}

	synchronized void stop(final long timeout, final TimeUnit unit) throws InterruptedException {
		Preconditions.checkState(this.exec != null, "Programming service dispatch thread already stopped");

		this.thread.cancel(true);
		this.exec.awaitTermination(timeout, unit);
		this.exec = null;
	}

	@Override
	public void close() throws InterruptedException {
		stop(CLOSE_TIMEOUT, TimeUnit.SECONDS);
	}
}
