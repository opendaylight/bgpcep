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
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.DuplicateInstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatusChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.ProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UncancellableInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UnknownInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UnknownPreconditionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.FailureBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.tinkerpop.blueprints.Compare;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.filter.PropertyFilterPipe;
import com.tinkerpop.pipes.util.structures.ArrayQueue;

final class ProgrammingServiceImpl implements ProgrammingService {
	private static final Logger LOG = LoggerFactory.getLogger(ProgrammingServiceImpl.class);
	private static final String INPUT_PROP = "input";
	private static final String STATUS_PROP = "state";
	private static final String TIMEOUT_PROP = "timeout";
	private static final String DEPENDS_ON_LABEL = "depends on";
	private static final BigInteger MILLION = BigInteger.valueOf(1000000);

	@GuardedBy("this")
	private final Queue<Vertex> readyQueue = new ArrayQueue<>();

	@GuardedBy("this")
	private final TransactionalGraph graph;

	private final NotificationProviderService notifs;
	private final ExecutorService executor;
	private final Timer timer;

	private class CancelInstruction implements Callable<RpcResult<CancelInstructionOutput>> {
		private final CancelInstructionInput input;

		public CancelInstruction(final CancelInstructionInput input) {
			this.input = Preconditions.checkNotNull(input);
		}

		@Override
		public RpcResult<CancelInstructionOutput> call() throws Exception {
			final InstructionId id = input.getId();
			final Vertex i = graph.getVertex(id);
			if (i == null) {
				LOG.debug("Instruction {} not present in the graph", id);

				final CancelInstructionOutput out = new CancelInstructionOutputBuilder().setFailure(UnknownInstruction.class).build();
				return SuccessfulRpcResult.create(out);
			}

			final InstructionStatus s = i.getProperty(STATUS_PROP);
			if (s == null) {
				LOG.warn("Instruction {} vertex {} has no status property", id, i);

				return SuccessfulRpcResult.create(
						new CancelInstructionOutputBuilder().setFailure(UncancellableInstruction.class).build());
			}

			switch (s) {
			case Cancelled:
			case Executing:
			case Failed:
			case Successful:
			case Unknown:
				LOG.debug("Instruction {} can no longer be cancelled due to status {}", id);
				return SuccessfulRpcResult.create(
						new CancelInstructionOutputBuilder().setFailure(UncancellableInstruction.class).build());
			case Queued:
			case Scheduled:
				break;
			}

			cancelInstruction(i);
			return SuccessfulRpcResult.create(new CancelInstructionOutputBuilder().build());
		}
	}

	private class SubmitInstruction implements Callable<RpcResult<SubmitInstructionOutput>> {
		private  final SubmitInstructionInput input;

		SubmitInstruction(final SubmitInstructionInput input) {
			this.input = Preconditions.checkNotNull(input);
		}

		@Override
		public RpcResult<SubmitInstructionOutput> call() throws Exception {
			final InstructionId id = input.getId();
			if (graph.getVertex(id) != null) {
				LOG.info("Instruction ID {} already present", id);
				return SuccessfulRpcResult.create(
						new SubmitInstructionOutputBuilder().setResult(
								new FailureBuilder().setFailure(DuplicateInstructionId.class).build()).build());
			}

			// Insert the instruction into the graph first
			final Vertex v = graph.addVertex(id);
			for (final InstructionId pid : input.getPreconditions()) {
				final Vertex pv = graph.getVertex(pid);
				if (pv == null) {
					LOG.info("Instruction {} depends on {}, which is not a known instruction", id, pid);
					graph.rollback();
					return SuccessfulRpcResult.create(
							new SubmitInstructionOutputBuilder().setResult(
									new FailureBuilder().setFailure(UnknownPreconditionId.class).build()).build());
				}

				graph.addEdge(null, v, pv, DEPENDS_ON_LABEL);
			}

			v.setProperty(INPUT_PROP, input);
			v.setProperty(STATUS_PROP, InstructionStatus.Queued);
			graph.commit();

			newInstruction(v, input);

			return SuccessfulRpcResult.create(new SubmitInstructionOutputBuilder().build());
		}
	}

	ProgrammingServiceImpl(final NotificationProviderService notifs, final ExecutorService executor,
			final Timer timer, final TransactionalGraph instructions) {
		this.notifs = Preconditions.checkNotNull(notifs);
		this.graph = Preconditions.checkNotNull(instructions);
		this.executor = Preconditions.checkNotNull(executor);
		this.timer = Preconditions.checkNotNull(timer);
	}

	@Override
	public Future<RpcResult<CancelInstructionOutput>> cancelInstruction(final CancelInstructionInput input) {
		return executor.submit(new CancelInstruction(input));
	}

	@Override
	public Future<RpcResult<SubmitInstructionOutput>> submitInstruction(final SubmitInstructionInput input) {
		return executor.submit(new SubmitInstruction(input));
	}

	@GuardedBy("this")
	private void transitionInstruction(final Vertex v, final InstructionStatus status) {
		// Set the status
		v.setProperty(STATUS_PROP, status);

		LOG.debug("Instruction {} transitioned to status {}", v.getId(), status);

		// Send out a notification
		notifs.publish(new InstructionStatusChangedBuilder().
				setId((InstructionId) v.getId()).setStatus(status).build());
	}

	@GuardedBy("this")
	private void cancelSingle(final Vertex v) {
		// Stop the timeout
		final Timeout to = v.getProperty(TIMEOUT_PROP);
		to.cancel();

		// Set the new status and send out notification
		transitionInstruction(v, InstructionStatus.Cancelled);
	}

	@GuardedBy("this")
	private void cancelDependants(final Vertex v) {
		@SuppressWarnings("unchecked")
		final GremlinPipeline<Vertex, Vertex> pipe = new GremlinPipeline<Vertex, InstructionId>().
		start(v).in(DEPENDS_ON_LABEL).or(
				new PropertyFilterPipe<Vertex, Vertex>(STATUS_PROP, Compare.EQUAL, InstructionStatus.Queued),
				new PropertyFilterPipe<Vertex, Vertex>(STATUS_PROP, Compare.EQUAL, InstructionStatus.Scheduled)).dedup();

		for (final Vertex cv : pipe) {
			cancelSingle(cv);
		}
	}

	private synchronized void cancelInstruction(final Vertex v) {
		readyQueue.remove(v);
		cancelSingle(v);
		cancelDependants(v);
		graph.commit();
	}

	private synchronized void newInstruction(final Vertex v, final SubmitInstructionInput i) {
		// First things first: check the deadline
		final BigInteger now = BigInteger.valueOf(System.currentTimeMillis()).multiply(MILLION);
		final BigInteger left = i.getDeadline().getValue().subtract(now);

		if (left.compareTo(BigInteger.ZERO) <= 0) {
			timeoutInstruction(v, i);
			return;
		}

		// Schedule a new timeout and attach it
		v.setProperty(TIMEOUT_PROP, timer.newTimeout(new TimerTask() {
			@Override
			public void run(final Timeout timeout) throws Exception {
				timeoutInstruction(v, i);
			}
		}, left.longValue(), TimeUnit.NANOSECONDS));

		tryScheduleInstruction(v);
		graph.commit();
	}

	private synchronized void timeoutInstruction(final Vertex v, final SubmitInstructionInput i) {
		final InstructionStatus s = v.getProperty(STATUS_PROP);

		switch (s) {
		case Cancelled:
		case Failed:
		case Successful:
			LOG.debug("Instruction {} has status {}, timeout is a no-op", i.getId(), s);
			break;
		case Unknown:
			LOG.warn("Instruction {} has status {} before timeout completed", i.getId(), s);
			break;
		case Executing:
			LOG.info("Instruction {} timed out while executing, transitioning into Unknown", i.getId());
			transitionInstruction(v, InstructionStatus.Unknown);
			cancelDependants(v);
			graph.commit();
			break;
		case Queued:
		case Scheduled:
			LOG.debug("Instruction {} timed out in status {}, cancelling it", i.getId(), s);
			cancelInstruction(v);
			break;
		}
	}

	@GuardedBy("this")
	private void tryScheduleInstruction(final Vertex v) {
		Preconditions.checkState(v.getProperty(STATUS_PROP).equals(InstructionStatus.Queued));

		/*
		 * Check all vertices we depend on. We start off as ready for
		 * scheduling. If we encounter a cancelled/failed/unknown
		 * dependency, we cancel this instruction (and cascade). If we
		 * encounter an executing/queued/scheduled dependency, we hold
		 * of scheduling this one.
		 */
		final GremlinPipeline<Vertex, InstructionStatus> pipe = new GremlinPipeline<Vertex, InstructionId>().
				start(v).out(DEPENDS_ON_LABEL).property(STATUS_PROP).cast(InstructionStatus.class);
		boolean ready = true;

		for (InstructionStatus s : pipe) {
			switch (s) {
			case Cancelled:
			case Failed:
			case Unknown:
				LOG.warn("Instruction {} was Queued, while a dependency was {}, cancelling it", v.getId(), s);
				// Don't call cancelInstruction() because it invokes graph.commit()!
				cancelSingle(v);
				cancelDependants(v);
				return;
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

		if (ready) {
			LOG.debug("Instruction {} is ready for execution", v.getId());
			transitionInstruction(v, InstructionStatus.Scheduled);
			readyQueue.add(v);
		}
	}
}
