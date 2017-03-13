/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitEnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitEnsureLspOperationalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitEnsureLspOperationalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitRemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitRemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitRemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitTriggerSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitTriggerSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitTriggerSyncOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 *
 */
final class TopologyProgramming implements NetworkTopologyPcepProgrammingService {
    private final InstructionScheduler scheduler;
    private final ServerSessionManager manager;

    TopologyProgramming(final InstructionScheduler scheduler, final ServerSessionManager manager) {
        this.scheduler = Preconditions.checkNotNull(scheduler);
        this.manager = Preconditions.checkNotNull(manager);
    }

    @Override
    public ListenableFuture<RpcResult<SubmitAddLspOutput>> submitAddLsp(final SubmitAddLspInput input) {
        Preconditions.checkArgument(input.getNode() != null);
        Preconditions.checkArgument(input.getName() != null);

        final SubmitAddLspOutputBuilder b = new SubmitAddLspOutputBuilder();
        b.setResult(AbstractInstructionExecutor.schedule(this.scheduler, new AbstractInstructionExecutor(input) {
            @Override
            protected ListenableFuture<OperationResult> invokeOperation() {
                return TopologyProgramming.this.manager.addLsp(input);
            }
        }));

        final RpcResult<SubmitAddLspOutput> res = SuccessfulRpcResult.create(b.build());
        return Futures.immediateFuture(res);
    }

    @Override
    public ListenableFuture<RpcResult<SubmitRemoveLspOutput>> submitRemoveLsp(final SubmitRemoveLspInput input) {
        Preconditions.checkArgument(input.getNode() != null);
        Preconditions.checkArgument(input.getName() != null);

        final SubmitRemoveLspOutputBuilder b = new SubmitRemoveLspOutputBuilder();
        b.setResult(AbstractInstructionExecutor.schedule(this.scheduler, new AbstractInstructionExecutor(input) {
            @Override
            protected ListenableFuture<OperationResult> invokeOperation() {
                return TopologyProgramming.this.manager.removeLsp(input);
            }
        }));

        final RpcResult<SubmitRemoveLspOutput> res = SuccessfulRpcResult.create(b.build());
        return Futures.immediateFuture(res);
    }

    @Override
    public ListenableFuture<RpcResult<SubmitUpdateLspOutput>> submitUpdateLsp(final SubmitUpdateLspInput input) {
        Preconditions.checkArgument(input.getNode() != null);
        Preconditions.checkArgument(input.getName() != null);

        final SubmitUpdateLspOutputBuilder b = new SubmitUpdateLspOutputBuilder();
        b.setResult(AbstractInstructionExecutor.schedule(this.scheduler, new AbstractInstructionExecutor(input) {
            @Override
            protected ListenableFuture<OperationResult> invokeOperation() {
                return TopologyProgramming.this.manager.updateLsp(input);
            }
        }));

        final RpcResult<SubmitUpdateLspOutput> res = SuccessfulRpcResult.create(b.build());
        return Futures.immediateFuture(res);
    }

    @Override
    public ListenableFuture<RpcResult<SubmitEnsureLspOperationalOutput>> submitEnsureLspOperational(
            final SubmitEnsureLspOperationalInput input) {
        Preconditions.checkArgument(input.getNode() != null);
        Preconditions.checkArgument(input.getName() != null);
        Preconditions.checkArgument(input.getArguments() != null);

        // FIXME: can we validate this early?
        // Preconditions.checkArgument(input.getArguments().getOperational() != null);

        final SubmitEnsureLspOperationalOutputBuilder b = new SubmitEnsureLspOperationalOutputBuilder();
        b.setResult(AbstractInstructionExecutor.schedule(this.scheduler, new AbstractInstructionExecutor(input) {
            @Override
            protected ListenableFuture<OperationResult> invokeOperation() {
                EnsureLspOperationalInputBuilder ensureLspOperationalInputBuilder = new EnsureLspOperationalInputBuilder();
                ensureLspOperationalInputBuilder.fieldsFrom(input);
                return TopologyProgramming.this.manager.ensureLspOperational(ensureLspOperationalInputBuilder.build());
            }
        }));

        final RpcResult<SubmitEnsureLspOperationalOutput> res = SuccessfulRpcResult.create(b.build());
        return Futures.immediateFuture(res);
    }


    @Override
    public ListenableFuture<RpcResult<SubmitTriggerSyncOutput>> submitTriggerSync(final SubmitTriggerSyncInput input) {
        Preconditions.checkArgument(input.getNode() != null);

        final SubmitTriggerSyncOutputBuilder b = new SubmitTriggerSyncOutputBuilder();
        b.setResult(AbstractInstructionExecutor.schedule(this.scheduler, new AbstractInstructionExecutor(input) {
            @Override
            protected ListenableFuture<OperationResult> invokeOperation() {
                return TopologyProgramming.this.manager.triggerSync(input);
            }
        }));

        final RpcResult<SubmitTriggerSyncOutput> res = SuccessfulRpcResult.create(b.build());
        return Futures.immediateFuture(res);
    }
}
