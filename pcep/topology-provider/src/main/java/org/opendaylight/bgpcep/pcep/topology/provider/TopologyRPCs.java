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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerInitialSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerInitialSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerInitialSyncOutputBuilder;
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
        return Futures.transform(manager.addLsp(input), new Function<OperationResult, RpcResult<AddLspOutput>>() {
            @Override
            public RpcResult<AddLspOutput> apply(final OperationResult input) {
                return SuccessfulRpcResult.create(new AddLspOutputBuilder(input).build());
            }
        });
    }

    @Override
    public Future<RpcResult<RemoveLspOutput>> removeLsp(final RemoveLspInput input) {
        return Futures.transform(manager.removeLsp(input), new Function<OperationResult, RpcResult<RemoveLspOutput>>() {
            @Override
            public RpcResult<RemoveLspOutput> apply(final OperationResult input) {
                return SuccessfulRpcResult.create(new RemoveLspOutputBuilder(input).build());
            }
        });
    }

    @Override
    public Future<RpcResult<TriggerInitialSyncOutput>> triggerInitialSync(final TriggerInitialSyncInput input) {
        return Futures.transform(manager.triggerInitialSync(input), new Function<OperationResult, RpcResult<TriggerInitialSyncOutput>>() {
            @Override
            public RpcResult<TriggerInitialSyncOutput> apply(final OperationResult input) {
                return SuccessfulRpcResult.create(new TriggerInitialSyncOutputBuilder(input).build());
            }
        });
    }

    @Override
    public Future<RpcResult<UpdateLspOutput>> updateLsp(final UpdateLspInput input) {
        return Futures.transform(manager.updateLsp(input), new Function<OperationResult, RpcResult<UpdateLspOutput>>() {
            @Override
            public RpcResult<UpdateLspOutput> apply(final OperationResult input) {
                return SuccessfulRpcResult.create(new UpdateLspOutputBuilder(input).build());
            }
        });
    }

    @Override
    public Future<RpcResult<EnsureLspOperationalOutput>> ensureLspOperational(final EnsureLspOperationalInput input) {
        return Futures.transform(manager.ensureLspOperational(input),
                new Function<OperationResult, RpcResult<EnsureLspOperationalOutput>>() {
                    @Override
                    public RpcResult<EnsureLspOperationalOutput> apply(final OperationResult input) {
                        return SuccessfulRpcResult.create(new EnsureLspOperationalOutputBuilder(input).build());
                    }
                });
    }
}
