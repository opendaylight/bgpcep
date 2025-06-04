/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Set;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitAddLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitAddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitAddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitAddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitEnsureLspOperational;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitEnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitEnsureLspOperationalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitEnsureLspOperationalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitRemoveLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitRemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitRemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitRemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitTriggerSync;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitTriggerSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitTriggerSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitTriggerSyncOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitUpdateLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitUpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitUpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitUpdateLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.EnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.OperationResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.RpcResult;

final class TopologyProgramming {
    private final InstructionScheduler scheduler;
    private final ServerSessionManager manager;

    TopologyProgramming(final InstructionScheduler scheduler, final ServerSessionManager manager) {
        this.scheduler = requireNonNull(scheduler);
        this.manager = requireNonNull(manager);
    }

    Registration register(final RpcProviderService rpcProviderService,
            final DataObjectIdentifier.WithKey<Topology, TopologyKey> path) {
        return rpcProviderService.registerRpcImplementations(List.of(
            (SubmitAddLsp) this::submitAddLsp,
            (SubmitRemoveLsp) this::submitRemoveLsp,
            (SubmitUpdateLsp) this::submitUpdateLsp,
            (SubmitEnsureLspOperational) this::submitEnsureLspOperational,
            (SubmitTriggerSync) this::submitTriggerSync), Set.of(path));
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<SubmitAddLspOutput>> submitAddLsp(final SubmitAddLspInput input) {
        Preconditions.checkArgument(input.getNode() != null);
        Preconditions.checkArgument(input.getName() != null);

        return Futures.immediateFuture(SuccessfulRpcResult.create(new SubmitAddLspOutputBuilder()
            .setResult(AbstractInstructionExecutor.schedule(scheduler, new AbstractInstructionExecutor(input) {
                @Override
                protected ListenableFuture<OperationResult> invokeOperation() {
                    return manager.addLsp(input);
                }
            }))
            .build()));
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<SubmitRemoveLspOutput>> submitRemoveLsp(final SubmitRemoveLspInput input) {
        Preconditions.checkArgument(input.getNode() != null);
        Preconditions.checkArgument(input.getName() != null);

        return Futures.immediateFuture(SuccessfulRpcResult.create(new SubmitRemoveLspOutputBuilder()
            .setResult(AbstractInstructionExecutor.schedule(scheduler, new AbstractInstructionExecutor(input) {
                @Override
                protected ListenableFuture<OperationResult> invokeOperation() {
                    return manager.removeLsp(input);
                }
            }))
            .build()));
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<SubmitUpdateLspOutput>> submitUpdateLsp(final SubmitUpdateLspInput input) {
        Preconditions.checkArgument(input.getNode() != null);
        Preconditions.checkArgument(input.getName() != null);

        return Futures.immediateFuture(SuccessfulRpcResult.create(new SubmitUpdateLspOutputBuilder()
            .setResult(AbstractInstructionExecutor.schedule(scheduler, new AbstractInstructionExecutor(input) {
                @Override
                protected ListenableFuture<OperationResult> invokeOperation() {
                    return manager.updateLsp(input);
                }
            }))
            .build()));
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<SubmitEnsureLspOperationalOutput>> submitEnsureLspOperational(
            final SubmitEnsureLspOperationalInput input) {
        Preconditions.checkArgument(input.getNode() != null);
        Preconditions.checkArgument(input.getName() != null);
        Preconditions.checkArgument(input.getArguments() != null);

        // FIXME: can we validate this early?
        // Preconditions.checkArgument(input.getArguments().getOperational() != null);

        return Futures.immediateFuture(SuccessfulRpcResult.create(new SubmitEnsureLspOperationalOutputBuilder()
            .setResult(AbstractInstructionExecutor.schedule(scheduler, new AbstractInstructionExecutor(input) {
                @Override
                protected ListenableFuture<OperationResult> invokeOperation() {
                    EnsureLspOperationalInputBuilder ensureLspOperationalInputBuilder =
                        new EnsureLspOperationalInputBuilder();
                    ensureLspOperationalInputBuilder.fieldsFrom(input);
                    return manager.ensureLspOperational(ensureLspOperationalInputBuilder.build());
                }
            }))
            .build()));
    }


    @VisibleForTesting
    ListenableFuture<RpcResult<SubmitTriggerSyncOutput>> submitTriggerSync(final SubmitTriggerSyncInput input) {
        Preconditions.checkArgument(input.getNode() != null);

        return Futures.immediateFuture(SuccessfulRpcResult.create(new SubmitTriggerSyncOutputBuilder()
            .setResult(AbstractInstructionExecutor.schedule(scheduler, new AbstractInstructionExecutor(input) {
                @Override
                protected ListenableFuture<OperationResult> invokeOperation() {
                    return manager.triggerSync(input);
                }
            }))
            .build()));
    }
}
