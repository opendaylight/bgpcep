/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitAddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitAddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitAddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitEnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitEnsureLspOperationalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitEnsureLspOperationalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitRemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitRemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitRemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitTriggerSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitTriggerSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitTriggerSyncOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitUpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitUpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitUpdateLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.EnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.OperationResult;
import org.opendaylight.yangtools.yang.common.RpcResult;

final class TopologyProgramming implements NetworkTopologyPcepProgrammingService {
    private final InstructionScheduler scheduler;
    private final ServerSessionManager manager;

    TopologyProgramming(final InstructionScheduler scheduler, final ServerSessionManager manager) {
        this.scheduler = requireNonNull(scheduler);
        this.manager = requireNonNull(manager);
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
                EnsureLspOperationalInputBuilder ensureLspOperationalInputBuilder =
                        new EnsureLspOperationalInputBuilder();
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
