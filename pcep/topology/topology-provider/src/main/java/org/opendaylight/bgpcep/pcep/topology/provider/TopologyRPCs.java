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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.EnsureLspOperationalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.EnsureLspOperationalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TearDownSessionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TearDownSessionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TearDownSessionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TriggerSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TriggerSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TriggerSyncOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

final class TopologyRPCs implements NetworkTopologyPcepService {
    private final ServerSessionManager manager;

    TopologyRPCs(final ServerSessionManager manager) {
        this.manager = requireNonNull(manager);
    }

    @Override
    public ListenableFuture<RpcResult<AddLspOutput>> addLsp(final AddLspInput input) {
        return Futures.transform(this.manager.addLsp(input),
            input1 -> SuccessfulRpcResult.create(new AddLspOutputBuilder(input1).build()),
            MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<RemoveLspOutput>> removeLsp(final RemoveLspInput input) {
        return Futures.transform(this.manager.removeLsp(input),
            input1 -> SuccessfulRpcResult.create(new RemoveLspOutputBuilder(input1).build()),
            MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<TriggerSyncOutput>> triggerSync(final TriggerSyncInput input) {
        return Futures.transform(this.manager.triggerSync(input),
            input1 -> SuccessfulRpcResult.create(new TriggerSyncOutputBuilder(input1).build()),
            MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<UpdateLspOutput>> updateLsp(final UpdateLspInput input) {
        return Futures.transform(this.manager.updateLsp(input),
            input1 -> SuccessfulRpcResult.create(new UpdateLspOutputBuilder(input1).build()),
            MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<EnsureLspOperationalOutput>> ensureLspOperational(
            final EnsureLspOperationalInput input) {
        return Futures.transform(this.manager.ensureLspOperational(input),
            input1 -> SuccessfulRpcResult.create(new EnsureLspOperationalOutputBuilder(input1).build()),
            MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<TearDownSessionOutput>> tearDownSession(final TearDownSessionInput input) {
        return Futures.transform(this.manager.tearDownSession(input),
            input1 -> SuccessfulRpcResult.create(new TearDownSessionOutputBuilder().build()),
            MoreExecutors.directExecutor());
    }
}
