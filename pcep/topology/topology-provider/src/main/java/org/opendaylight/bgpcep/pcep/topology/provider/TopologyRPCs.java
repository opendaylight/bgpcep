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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.Set;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.AddLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.AddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.EnsureLspOperational;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.EnsureLspOperationalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.EnsureLspOperationalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.RemoveLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.RemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.TearDownSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.TearDownSessionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.TearDownSessionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.TearDownSessionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.TriggerSync;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.TriggerSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.TriggerSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.TriggerSyncOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.UpdateLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.UpdateLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.RpcResult;

final class TopologyRPCs {
    private final ServerSessionManager manager;

    TopologyRPCs(final ServerSessionManager manager) {
        this.manager = requireNonNull(manager);
    }

    Registration register(final RpcProviderService rpcProviderService,
            final DataObjectIdentifier.WithKey<Topology, TopologyKey> path) {
        return rpcProviderService.registerRpcImplementations(List.of(
            (AddLsp) this::addLsp,
            (RemoveLsp) this::removeLsp,
            (TriggerSync) this::triggerSync,
            (UpdateLsp) this::updateLsp,
            (EnsureLspOperational) this::ensureLspOperational,
            (TearDownSession) this::tearDownSession), Set.of(path));
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
