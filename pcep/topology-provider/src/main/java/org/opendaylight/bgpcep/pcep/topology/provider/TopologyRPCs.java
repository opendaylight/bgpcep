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

import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Preconditions;

final class TopologyRPCs implements NetworkTopologyPcepService {
	private final ServerSessionManager manager;
	private final EventExecutor exec;

	private abstract class ExecutionResultAdaptor<T> implements FutureListener<OperationResult> {
		protected final Promise<RpcResult<T>> promise = exec.newPromise();

		protected abstract RpcResult<T> convertResult(OperationResult result);

		@Override
		public final void operationComplete(final io.netty.util.concurrent.Future<OperationResult> future) {
			if (future.isSuccess()) {
				promise.setSuccess(convertResult(future.getNow()));
			} else {
				promise.setFailure(future.cause());
			}
		}
	}

	TopologyRPCs(final EventExecutor exec, final ServerSessionManager manager) {
		this.manager = Preconditions.checkNotNull(manager);
		this.exec = Preconditions.checkNotNull(exec);
	}

	@Override
	public Future<RpcResult<AddLspOutput>> addLsp(final AddLspInput input) {
		final ExecutionResultAdaptor<AddLspOutput> adaptor = new ExecutionResultAdaptor<AddLspOutput>() {
			@Override
			protected RpcResult<AddLspOutput> convertResult(final OperationResult result) {
				return SuccessfulRpcResult.create(new AddLspOutputBuilder(result).build());
			}
		};

		manager.realAddLsp(input).addListener(adaptor);
		return adaptor.promise;
	}

	@Override
	public Future<RpcResult<RemoveLspOutput>> removeLsp(final RemoveLspInput input) {
		final ExecutionResultAdaptor<RemoveLspOutput> adaptor = new ExecutionResultAdaptor<RemoveLspOutput>() {
			@Override
			protected RpcResult<RemoveLspOutput> convertResult(final OperationResult result) {
				return SuccessfulRpcResult.create(new RemoveLspOutputBuilder(result).build());
			}
		};

		manager.realRemoveLsp(input).addListener(adaptor);
		return adaptor.promise;
	}

	@Override
	public Future<RpcResult<UpdateLspOutput>> updateLsp(final UpdateLspInput input) {
		final ExecutionResultAdaptor<UpdateLspOutput> adaptor = new ExecutionResultAdaptor<UpdateLspOutput>() {
			@Override
			protected RpcResult<UpdateLspOutput> convertResult(final OperationResult result) {
				return SuccessfulRpcResult.create(new UpdateLspOutputBuilder(result).build());
			}
		};

		manager.realUpdateLsp(input).addListener(adaptor);
		return adaptor.promise;
	}
}
