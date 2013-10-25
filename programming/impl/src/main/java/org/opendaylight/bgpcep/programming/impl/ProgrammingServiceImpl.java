/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.DuplicateInstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.ProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UncancellableInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UnknownInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UnknownPreconditionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.FailureBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.tinkerpop.blueprints.Compare;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.filter.PropertyFilterPipe;

final class RpcUtil {
	private RpcUtil() {

	}

	public static <T> RpcResult<T> successfulRpcResult(final T value) {
		return new RpcResult<T>() {
			@Override
			public boolean isSuccessful() {
				return true;
			}

			@Override
			public T getResult() {
				return value;
			}

			@Override
			public Collection<RpcError> getErrors() {
				return Collections.emptyList();
			}
		};
	}
}

final class ProgrammingServiceImpl implements ProgrammingService {
	private static final Logger LOG = LoggerFactory.getLogger(ProgrammingServiceImpl.class);
	private static final String STATUS_PROP = "state";
	private static final String INPUT_PROP = "input";
	private static final String DEPENDS_ON_LABEL = "depends on";
	private final ExecutorService executor;
	private final TransactionalGraph graph;

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
				return RpcUtil.successfulRpcResult(out);
			}

			final InstructionStatus s = i.getProperty(STATUS_PROP);
			if (s == null) {
				LOG.warn("Instruction {} vertex {} has no status property", id, i);

				return RpcUtil.successfulRpcResult(
						new CancelInstructionOutputBuilder().setFailure(UncancellableInstruction.class).build());
			}

			switch (s) {
			case Cancelled:
			case Executing:
			case Failed:
			case Successful:
			case Unknown:
				LOG.debug("Instruction {} can no longer be cancelled due to status {}", id);
				return RpcUtil.successfulRpcResult(
						new CancelInstructionOutputBuilder().setFailure(UncancellableInstruction.class).build());
			case Queued:
			case Scheduled:
				break;
			}

			@SuppressWarnings("unchecked")
			final GremlinPipeline<Vertex, Vertex> pipe = new GremlinPipeline<Vertex, InstructionId>().
			start(i).in(DEPENDS_ON_LABEL).or(
					new PropertyFilterPipe<Vertex, Vertex>(STATUS_PROP, Compare.EQUAL, InstructionStatus.Queued),
					new PropertyFilterPipe<Vertex, Vertex>(STATUS_PROP, Compare.EQUAL, InstructionStatus.Scheduled)).dedup();

			for (Vertex v : pipe) {
				v.setProperty(STATUS_PROP, InstructionStatus.Cancelled);
				// FIXME: trigger notifications, etc.
			}

			graph.commit();
			return RpcUtil.successfulRpcResult(new CancelInstructionOutputBuilder().build());
		}
	}

	ProgrammingServiceImpl(final ExecutorService executor, final TransactionalGraph instructions) {
		this.graph = Preconditions.checkNotNull(instructions);
		this.executor = Preconditions.checkNotNull(executor);
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
				return RpcUtil.successfulRpcResult(
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
					return RpcUtil.successfulRpcResult(
							new SubmitInstructionOutputBuilder().setResult(
									new FailureBuilder().setFailure(UnknownPreconditionId.class).build()).build());
				}

				graph.addEdge(null, v, pv, DEPENDS_ON_LABEL);
			}

			v.setProperty(INPUT_PROP, input);
			v.setProperty(STATUS_PROP, InstructionStatus.Queued);
			graph.commit();

			// FIXME: now whack the execution engine

			return RpcUtil.successfulRpcResult(new SubmitInstructionOutputBuilder().build());
		}
	}

	@Override
	public Future<RpcResult<CancelInstructionOutput>> cancelInstruction(final CancelInstructionInput input) {
		return executor.submit(new CancelInstruction(input));
	}

	@Override
	public Future<RpcResult<SubmitInstructionOutput>> submitInstruction(final SubmitInstructionInput input) {
		return executor.submit(new SubmitInstruction(input));
	}
}
