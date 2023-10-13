/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepCreateP2pTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepCreateP2pTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepCreateP2pTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepCreateP2pTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepDestroyTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepDestroyTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepDestroyTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepDestroyTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepUpdateTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepUpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepUpdateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepUpdateTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TunnelProgramming implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelProgramming.class);

    private final InstructionScheduler scheduler;
    private final TunnelProviderDependencies dependencies;

    TunnelProgramming(final @NonNull InstructionScheduler scheduler,
            final @NonNull TunnelProviderDependencies dependencies) {
        this.scheduler = requireNonNull(scheduler);
        this.dependencies = requireNonNull(dependencies);
    }

    Registration register(final KeyedInstanceIdentifier<Topology, TopologyKey> topologyPath) {
        return dependencies.getRpcProviderRegistry().registerRpcImplementations(
            ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
                .put(PcepCreateP2pTunnel.class, this::pcepCreateP2pTunnel)
                .put(PcepDestroyTunnel.class, this::pcepDestroyTunnel)
                .put(PcepUpdateTunnel.class, this::pcepUpdateTunnel)
                .build(), Set.of(topologyPath));
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<PcepCreateP2pTunnelOutput>> pcepCreateP2pTunnel(
            final PcepCreateP2pTunnelInput p2pTunnelInput) {
        return Futures.immediateFuture(SuccessfulRpcResult.create(new PcepCreateP2pTunnelOutputBuilder()
            .setResult(AbstractInstructionExecutor.schedule(scheduler, new CreateTunnelInstructionExecutor(
                p2pTunnelInput, dependencies.getDataBroker(), dependencies.getRpcConsumerRegistry())))
            .build()));
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<PcepDestroyTunnelOutput>> pcepDestroyTunnel(
            final PcepDestroyTunnelInput destroyTunnelInput) {
        return Futures.immediateFuture(SuccessfulRpcResult.create(new PcepDestroyTunnelOutputBuilder()
            .setResult(AbstractInstructionExecutor.schedule(scheduler, new DestroyTunnelInstructionExecutor(
                destroyTunnelInput, dependencies.getDataBroker(), dependencies.getRpcConsumerRegistry())))
            .build()));
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<PcepUpdateTunnelOutput>> pcepUpdateTunnel(
            final PcepUpdateTunnelInput updateTunnelInput) {
        return Futures.immediateFuture(SuccessfulRpcResult.create(new PcepUpdateTunnelOutputBuilder()
            .setResult(AbstractInstructionExecutor.schedule(scheduler, new UpdateTunnelInstructionExecutor(
                updateTunnelInput, dependencies.getDataBroker(), dependencies.getRpcConsumerRegistry())))
            .build()));
    }

    @Override
    public void close() {
        LOG.debug("Shutting down instruction scheduler {}", this);
    }
}
