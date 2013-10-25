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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.bgpcep.programming.spi.ExecutionResult;
import org.opendaylight.bgpcep.programming.spi.InstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionExecutorRegistry;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.DeadOnArrival;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.DuplicateInstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatusChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.ProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UncancellableInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UnknownInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UnknownPreconditionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.DetailsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.failure.Failure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.failure.FailureBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

final class ProgrammingServiceImpl implements InstructionExecutorRegistry, ProgrammingService {
	private static final Logger LOG = LoggerFactory.getLogger(ProgrammingServiceImpl.class);
	private static final BigInteger MILLION = BigInteger.valueOf(1000000);

	private final Map<InstructionId, Instruction> insns = new HashMap<>();

	@GuardedBy("this")
	private final Deque<Instruction> readyQueue = new ArrayDeque<>();

	@GuardedBy("this")
	private final Deque<Instruction> deferredQueue = new ArrayDeque<>();

	@GuardedBy("this")
	private final Multimap<Class<? extends InstructionType>, InstructionExecutor> executors = ArrayListMultimap.create();

	private final NotificationProviderService notifs;
	private final ExecutorService executor;
	private final Timer timer;
	private java.util.concurrent.Future<Void> thread;
	private ExecutorService exec;

	ProgrammingServiceImpl(final NotificationProviderService notifs, final ExecutorService executor,
			final Timer timer, final InstructionExecutorRegistry registry) {
		this.notifs = Preconditions.checkNotNull(notifs);
		this.executor = Preconditions.checkNotNull(executor);
		this.timer = Preconditions.checkNotNull(timer);
	}

	@Override
	public java.util.concurrent.Future<RpcResult<CancelInstructionOutput>> cancelInstruction(final CancelInstructionInput input) {
		return executor.submit(new Callable<RpcResult<CancelInstructionOutput>>() {
			@Override
			public RpcResult<CancelInstructionOutput> call() {
				return realCancelInstruction(input);
			}
		});
	}

	@Override
	public java.util.concurrent.Future<RpcResult<SubmitInstructionOutput>> submitInstruction(final SubmitInstructionInput input) {
		return executor.submit(new Callable<RpcResult<SubmitInstructionOutput>>() {
			@Override
			public RpcResult<SubmitInstructionOutput> call() {
				return realSubmitInstruction(input);
			}
		});
	}

	@Override
	public synchronized Registration<InstructionExecutor> registerInstructionExecutor(final Class<? extends InstructionType> type, final InstructionExecutor executor) {
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(executor);

		executors.put(type, executor);

		/*
		 * Walk the deferred instructions back to front, check if they have
		 * the same type as the executor we have just registered. If they do,
		 * we move them to the head of readyQueue. This approach should retain
		 * submission order of the instructions.
		 */
		final Iterator<Instruction> it = deferredQueue.descendingIterator();
		while (it.hasNext()) {
			final Instruction i = it.next();
			if (type.equals(i.getInput().getType())) {
				it.remove();
				readyQueue.addFirst(i);
			}
		}

		notify();

		final Object lock = this;
		return new Registration<InstructionExecutor>() {
			@Override
			public void close() throws Exception {
				synchronized (lock) {
					executors.remove(type, executor);
				}
			}

			@Override
			public InstructionExecutor getInstance() {
				return executor;
			}
		};
	}

	private static final RpcResult<SubmitInstructionOutput> failedSubmit(final Failure f) {
		return SuccessfulRpcResult.create(
				new SubmitInstructionOutputBuilder().setResult(
						new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.FailureBuilder()
						.setFailure(f).build()).build());
	}

	private synchronized RpcResult<CancelInstructionOutput> realCancelInstruction(final CancelInstructionInput input)  {
		final Instruction i = insns.get(input.getId());
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
			LOG.debug("Instruction {} can no longer be cancelled due to status {}", input.getId());
			return SuccessfulRpcResult.create(
					new CancelInstructionOutputBuilder().setFailure(UncancellableInstruction.class).build());
		case Queued:
		case Scheduled:
			break;
		}

		cancelInstruction(i, null);
		return SuccessfulRpcResult.create(new CancelInstructionOutputBuilder().build());
	}

	private synchronized RpcResult<SubmitInstructionOutput> realSubmitInstruction(final SubmitInstructionInput input) {
		final InstructionId id = input.getId();
		if (insns.get(id) != null) {
			LOG.info("Instruction ID {} already present", id);
			return failedSubmit(new FailureBuilder().setType(DuplicateInstructionId.class).build());
		}

		// First things first: check the deadline
		final BigInteger now = BigInteger.valueOf(System.currentTimeMillis()).multiply(MILLION);
		final BigInteger left = input.getDeadline().getValue().subtract(now);

		if (left.compareTo(BigInteger.ZERO) <= 0) {
			LOG.debug("Instruction {} deadline has already passed by {}ns", id, left);
			return failedSubmit(new FailureBuilder().setType(DeadOnArrival.class).build());
		}

		// Resolve dependencies
		final List<Instruction> dependencies = new ArrayList<>();
		for (final InstructionId pid : input.getPreconditions()) {
			final Instruction i = insns.get(pid);
			if (i == null) {
				LOG.info("Instruction {} depends on {}, which is not a known instruction", id, pid);
				return failedSubmit(new FailureBuilder().setType(UnknownPreconditionId.class).build());
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
				unmet.add(d.getInput().getId());
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
			return failedSubmit(new FailureBuilder().setType(DeadOnArrival.class).setFailedPreconditions(unmet).build());
		}

		/*
		 * All pre-flight checks done are at this point, the following
		 * steps can only fail in catastrophic scenarios (OOM and the
		 * like).
		 */

		// Schedule a timeout for the instruction
		final Timeout t = timer.newTimeout(new TimerTask() {
			@Override
			public void run(final Timeout timeout) throws Exception {
				timeoutInstruction(input.getId());
			}
		}, left.longValue(), TimeUnit.NANOSECONDS);

		// Put it into the instruction list
		final Instruction i = new Instruction(input, dependencies, t);
		insns.put(id, i);

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
		executor.submit(new Runnable() {
			@Override
			public void run() {
				tryScheduleInstruction(i);
			}
		});

		return SuccessfulRpcResult.create(new SubmitInstructionOutputBuilder().build());
	}

	@GuardedBy("this")
	private void transitionInstruction(final Instruction v, final InstructionStatus status, final Details details) {
		// Set the status
		v.setStatus(status);

		LOG.debug("Instruction {} transitioned to status {}", v.getInput().getId(), status);

		// Send out a notification
		notifs.publish(new InstructionStatusChangedBuilder().
				setId(v.getInput().getId()).setStatus(status).setDetails(details).build());
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
		final Details details = new DetailsBuilder().setUnmetDependencies(ImmutableList.of(v.getInput().getId())).build();
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
		deferredQueue.remove(i);
		readyQueue.remove(i);
		cancelSingle(i, details);
		cancelDependants(i);
	}

	private synchronized void timeoutInstruction(final InstructionId id) {
		final Instruction i = insns.get(id);
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
					ids.add(d.getInput().getId());
				}
			}

			cancelInstruction(i, new DetailsBuilder().setUnmetDependencies(ids).build());
			break;
		case Scheduled:
			LOG.debug("Instruction {} timed out while Scheduled, cancelling it", i.getInput().getId());
			// FIXME: we should provide details why it timed out while scheduled
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
				unmet.add(d.getInput().getId());
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
			LOG.debug("Instruction {} was Queued, while some dependencies were resolved unsuccessfully, cancelling it", i.getInput().getId());
			cancelSingle(i, new DetailsBuilder().setUnmetDependencies(unmet).build());
			cancelDependants(i);
			return;
		}

		if (ready) {
			LOG.debug("Instruction {} is ready for execution", i.getInput().getId());
			transitionInstruction(i, InstructionStatus.Scheduled, null);

			readyQueue.add(i);
			notify();
		}
	}

	private synchronized void executionFailed(final Instruction i, final Throwable cause) {
		LOG.error("Instruction {} failed to execute", i.getInput().getId(), cause);
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
			while (!readyQueue.isEmpty()) {
				final Instruction i = readyQueue.poll();

				Preconditions.checkState(i.getStatus().equals(InstructionStatus.Scheduled));
				final SubmitInstructionInput input = i.getInput();

				/*
				 *  Walk all the registered executors for a particular type and
				 *  offer them the chance to execute the instruction. The first
				 *  one to accept it wins.
				 */
				Future<ExecutionResult<?>> f = null;
				final Collection<InstructionExecutor> el = executors.get(input.getType());

				for (final InstructionExecutor e : el) {
					f = e.offerInstruction(input.getArguments());
					if (f != null) {
						break;
					}
				}

				// We did not find an executor -- defer the instruction
				if (f == null) {
					deferredQueue.add(i);
					continue;
				}

				transitionInstruction(i, InstructionStatus.Executing, null);
				f.addListener(new FutureListener<ExecutionResult<?>>() {
					@Override
					public void operationComplete(final Future<ExecutionResult<?>> future) {
						if (future.isSuccess()) {
							executionSuccessful(i, future.getNow());
						} else {
							executionFailed(i, future.cause());
						}
					}
				});
			}

			wait();
		}
	}

	synchronized void start(final ThreadFactory threadFactory) {
		Preconditions.checkState(exec == null, "Programming service dispatch thread already started");

		exec = Executors.newSingleThreadExecutor(threadFactory);
		thread = exec.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					processQueues();
				} catch (Exception ex) {
					if (!(ex instanceof InterruptedException)) {
						LOG.error("Programming service dispatch thread died", ex);
					}
					throw ex;
				}
				return null;
			}
		});
		exec.shutdown();
	}

	synchronized void stop(final long timeout, final TimeUnit unit) throws InterruptedException {
		Preconditions.checkState(exec != null, "Programming service dispatch thread already stopped");

		thread.cancel(true);
		exec.awaitTermination(timeout, unit);
		exec = null;
	}
}
