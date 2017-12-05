/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Future;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerSyncOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

final class TopologyRPCs implements NetworkTopologyPcepService {
    private final ServerSessionManager manager;

    TopologyRPCs(final ServerSessionManager manager) {
        this.manager = Preconditions.checkNotNull(manager);
    }

    @Override
    public Future<RpcResult<AddLspOutput>> addLsp(final AddLspInput input) {
        return Futures.transform(this.manager.addLsp(input), (Function<OperationResult, RpcResult<AddLspOutput>>) input1 -> SuccessfulRpcResult.create(new AddLspOutputBuilder(input1).build()));
    }

    @Override
    public Future<RpcResult<RemoveLspOutput>> removeLsp(final RemoveLspInput input) {
        return Futures.transform(this.manager.removeLsp(input), (Function<OperationResult, RpcResult<RemoveLspOutput>>) input1 -> SuccessfulRpcResult.create(new RemoveLspOutputBuilder(input1).build()));
    }

    @Override
    public Future<RpcResult<TriggerSyncOutput>> triggerSync(final TriggerSyncInput input) {
        return Futures.transform(this.manager.triggerSync(input), (Function<OperationResult, RpcResult<TriggerSyncOutput>>) input1 -> SuccessfulRpcResult.create(new TriggerSyncOutputBuilder(input1).build()));
    }

    @Override
    public Future<RpcResult<UpdateLspOutput>> updateLsp(final UpdateLspInput input) {
        return Futures.transform(this.manager.updateLsp(input), (Function<OperationResult, RpcResult<UpdateLspOutput>>) input1 -> SuccessfulRpcResult.create(new UpdateLspOutputBuilder(input1).build()));
    }

    @Override
    public Future<RpcResult<EnsureLspOperationalOutput>> ensureLspOperational(final EnsureLspOperationalInput input) {
        return Futures.transform(this.manager.ensureLspOperational(input),
            (Function<OperationResult, RpcResult<EnsureLspOperationalOutput>>) input1 -> SuccessfulRpcResult.create(new EnsureLspOperationalOutputBuilder(input1).build()));
    }
}
