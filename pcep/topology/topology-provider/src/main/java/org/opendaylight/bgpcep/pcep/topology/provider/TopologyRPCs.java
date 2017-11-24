/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Future;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.AddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.EnsureLspOperationalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.EnsureLspOperationalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.RemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.TearDownSessionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.TriggerSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.TriggerSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.TriggerSyncOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.UpdateLspOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

final class TopologyRPCs implements NetworkTopologyPcepService {
    private final ServerSessionManager manager;

    TopologyRPCs(final ServerSessionManager manager) {
        this.manager = requireNonNull(manager);
    }

    @Override
    public Future<RpcResult<AddLspOutput>> addLsp(final AddLspInput input) {
        return Futures.transform(this.manager.addLsp(input),
                input1 -> SuccessfulRpcResult.create(new AddLspOutputBuilder(input1).build()),
                MoreExecutors.directExecutor());
    }

    @Override
    public Future<RpcResult<RemoveLspOutput>> removeLsp(final RemoveLspInput input) {
        return Futures.transform(this.manager.removeLsp(input),
                input1 -> SuccessfulRpcResult.create(new RemoveLspOutputBuilder(input1).build()),
                MoreExecutors.directExecutor());
    }

    @Override
    public Future<RpcResult<TriggerSyncOutput>> triggerSync(final TriggerSyncInput input) {
        return Futures.transform(this.manager.triggerSync(input),
                input1 -> SuccessfulRpcResult.create(new TriggerSyncOutputBuilder(input1).build()),
                MoreExecutors.directExecutor());
    }

    @Override
    public Future<RpcResult<UpdateLspOutput>> updateLsp(final UpdateLspInput input) {
        return Futures.transform(this.manager.updateLsp(input),
                input1 -> SuccessfulRpcResult.create(new UpdateLspOutputBuilder(input1).build()),
                MoreExecutors.directExecutor());
    }

    @Override
    public Future<RpcResult<EnsureLspOperationalOutput>> ensureLspOperational(final EnsureLspOperationalInput input) {
        return Futures.transform(this.manager.ensureLspOperational(input),
                input1 -> SuccessfulRpcResult.create(new EnsureLspOperationalOutputBuilder(input1).build()),
                MoreExecutors.directExecutor());
    }

    @Override
    public Future<RpcResult<Void>> tearDownSession(final TearDownSessionInput input) {
        return Futures.transform(this.manager.tearDownSession(input),
                input1 -> SuccessfulRpcResult.create(null), MoreExecutors.directExecutor());
    }
}
