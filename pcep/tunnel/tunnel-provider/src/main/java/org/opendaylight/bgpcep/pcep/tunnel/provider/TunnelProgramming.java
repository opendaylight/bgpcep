/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepCreateP2pTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepCreateP2pTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepCreateP2pTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepDestroyTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepDestroyTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepDestroyTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepUpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepUpdateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepUpdateTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.TopologyTunnelPcepProgrammingService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TunnelProgramming implements TopologyTunnelPcepProgrammingService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelProgramming.class);
    private final InstructionScheduler scheduler;
    private final TunnelProviderDependencies dependencies;

    TunnelProgramming(
            @Nonnull final InstructionScheduler scheduler,
            @Nonnull final TunnelProviderDependencies dependencies) {
        this.scheduler = requireNonNull(scheduler);
        this.dependencies = requireNonNull(dependencies);
    }

    @Override
    public ListenableFuture<RpcResult<PcepCreateP2pTunnelOutput>> pcepCreateP2pTunnel(
            final PcepCreateP2pTunnelInput p2pTunnelInput) {
        final PcepCreateP2pTunnelOutputBuilder b = new PcepCreateP2pTunnelOutputBuilder();
        b.setResult(AbstractInstructionExecutor.schedule(this.scheduler,
                new CreateTunnelInstructionExecutor(p2pTunnelInput,
                        TunnelProgramming.this.dependencies.getDataBroker(),
                        TunnelProgramming.this.dependencies.getNtps())));
        final RpcResult<PcepCreateP2pTunnelOutput> res = SuccessfulRpcResult.create(b.build());
        return Futures.immediateFuture(res);
    }

    @Override
    public ListenableFuture<RpcResult<PcepDestroyTunnelOutput>> pcepDestroyTunnel(
            final PcepDestroyTunnelInput destroyTunnelInput) {
        final PcepDestroyTunnelOutputBuilder b = new PcepDestroyTunnelOutputBuilder();
        b.setResult(AbstractInstructionExecutor.schedule(this.scheduler,
                new DestroyTunnelInstructionExecutor(destroyTunnelInput,
                        TunnelProgramming.this.dependencies.getDataBroker(),
                        TunnelProgramming.this.dependencies.getNtps())));
        final RpcResult<PcepDestroyTunnelOutput> res = SuccessfulRpcResult.create(b.build());
        return Futures.immediateFuture(res);
    }

    @Override
    public ListenableFuture<RpcResult<PcepUpdateTunnelOutput>> pcepUpdateTunnel(
            final PcepUpdateTunnelInput updateTunnelInput) {
        final PcepUpdateTunnelOutputBuilder b = new PcepUpdateTunnelOutputBuilder();
        b.setResult(AbstractInstructionExecutor.schedule(this.scheduler,
                new UpdateTunnelInstructionExecutor(updateTunnelInput,
                        TunnelProgramming.this.dependencies.getDataBroker(),
                        TunnelProgramming.this.dependencies.getNtps())));

        final RpcResult<PcepUpdateTunnelOutput> res = SuccessfulRpcResult.create(b.build());
        return Futures.immediateFuture(res);
    }

    @Override
    public void close() {
        LOG.debug("Shutting down instruction scheduler {}", this);
    }
}
