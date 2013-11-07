/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;

final class TopologyRPCs implements NetworkTopologyPcepService {
	private final ServerSessionManager manager;

	TopologyRPCs(final ServerSessionManager manager) {
		this.manager = Preconditions.checkNotNull(manager);
	}

	@Override
	public Future<RpcResult<AddLspOutput>> addLsp(final AddLspInput input) {
		return Futures.transform(manager.realAddLsp(input), new Function<OperationResult, RpcResult<AddLspOutput>>() {
			@Override
			public RpcResult<AddLspOutput> apply(final OperationResult input) {
				return SuccessfulRpcResult.create(new AddLspOutputBuilder(input).build());
			}
		});
	}

	@Override
	public Future<RpcResult<RemoveLspOutput>> removeLsp(final RemoveLspInput input) {
		return Futures.transform(manager.realRemoveLsp(input), new Function<OperationResult, RpcResult<RemoveLspOutput>>() {
			@Override
			public RpcResult<RemoveLspOutput> apply(final OperationResult input) {
				return SuccessfulRpcResult.create(new RemoveLspOutputBuilder(input).build());
			}
		});
	}

	@Override
	public Future<RpcResult<UpdateLspOutput>> updateLsp(final UpdateLspInput input) {
		return Futures.transform(manager.realUpdateLsp(input), new Function<OperationResult, RpcResult<UpdateLspOutput>>() {
			@Override
			public RpcResult<UpdateLspOutput> apply(final OperationResult input) {
				return SuccessfulRpcResult.create(new UpdateLspOutputBuilder(input).build());
			}
		});
	}
}
