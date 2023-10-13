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
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Set;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.EnsureLspOperational;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.EnsureLspOperationalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.EnsureLspOperationalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TearDownSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TearDownSessionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TearDownSessionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TearDownSessionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TriggerSync;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TriggerSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TriggerSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TriggerSyncOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.common.RpcResult;

final class TopologyRPCs {
    private final ServerSessionManager manager;

    TopologyRPCs(final ServerSessionManager manager) {
        this.manager = requireNonNull(manager);
    }

    Registration register(final RpcProviderService rpcProviderService,
            final KeyedInstanceIdentifier<Topology, TopologyKey> path) {
        return rpcProviderService.registerRpcImplementations(ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(AddLsp.class, this::addLsp)
            .put(RemoveLsp.class, this::removeLsp)
            .put(TriggerSync.class, this::triggerSync)
            .put(UpdateLsp.class, this::updateLsp)
            .put(EnsureLspOperational.class, this::ensureLspOperational)
            .put(TearDownSession.class, this::tearDownSession)
            .build(), Set.of(path));
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<AddLspOutput>> addLsp(final AddLspInput input) {
        return Futures.transform(manager.addLsp(input),
            output -> SuccessfulRpcResult.create(new AddLspOutputBuilder(output).build()),
            MoreExecutors.directExecutor());
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<RemoveLspOutput>> removeLsp(final RemoveLspInput input) {
        return Futures.transform(manager.removeLsp(input),
            output -> SuccessfulRpcResult.create(new RemoveLspOutputBuilder(output).build()),
            MoreExecutors.directExecutor());
    }

    private ListenableFuture<RpcResult<TriggerSyncOutput>> triggerSync(final TriggerSyncInput input) {
        return Futures.transform(manager.triggerSync(input),
            output -> SuccessfulRpcResult.create(new TriggerSyncOutputBuilder(output).build()),
            MoreExecutors.directExecutor());
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<UpdateLspOutput>> updateLsp(final UpdateLspInput input) {
        return Futures.transform(manager.updateLsp(input),
            output -> SuccessfulRpcResult.create(new UpdateLspOutputBuilder(output).build()),
            MoreExecutors.directExecutor());
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<EnsureLspOperationalOutput>> ensureLspOperational(
            final EnsureLspOperationalInput input) {
        return Futures.transform(manager.ensureLspOperational(input),
            output -> SuccessfulRpcResult.create(new EnsureLspOperationalOutputBuilder(output).build()),
            MoreExecutors.directExecutor());
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<TearDownSessionOutput>> tearDownSession(final TearDownSessionInput input) {
        return Futures.transform(manager.tearDownSession(input),
            output -> SuccessfulRpcResult.create(new TearDownSessionOutputBuilder().build()),
            MoreExecutors.directExecutor());
    }
}
