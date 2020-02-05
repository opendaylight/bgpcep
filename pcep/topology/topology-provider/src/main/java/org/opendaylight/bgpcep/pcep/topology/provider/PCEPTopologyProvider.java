/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyConfiguration;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyProviderDependencies;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.NetworkTopologyPcepService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

public final class PCEPTopologyProvider extends DefaultTopologyReference {
    private static final String STATEFUL_NOT_DEFINED = "Stateful capability not defined, aborting PCEP Topology"
            + " Provider instantiation";
    private final ServerSessionManager manager;
    private final PCEPTopologyProviderDependencies dependenciesProvider;
    private final PCEPTopologyConfiguration configDependencies;
    private final InstructionScheduler scheduler;
    private ObjectRegistration<NetworkTopologyPcepProgrammingService> network;
    private ObjectRegistration<NetworkTopologyPcepService> element;
    private Channel channel;

    private PCEPTopologyProvider(
            final PCEPTopologyConfiguration configDependencies,
            final PCEPTopologyProviderDependencies dependenciesProvider,
            final ServerSessionManager manager, final InstructionScheduler scheduler) {
        super(configDependencies.getTopology());
        this.dependenciesProvider = requireNonNull(dependenciesProvider);
        this.configDependencies = configDependencies;
        this.manager = requireNonNull(manager);
        this.scheduler = requireNonNull(scheduler);
    }

    public static PCEPTopologyProvider create(final PCEPTopologyProviderDependencies dependenciesProvider,
            final InstructionScheduler scheduler, final PCEPTopologyConfiguration configDependencies) {
        final List<PCEPCapability> capabilities = dependenciesProvider.getPCEPDispatcher()
                .getPCEPSessionNegotiatorFactory().getPCEPSessionProposalFactory().getCapabilities();
        final Optional<PCEPCapability> statefulCapability = capabilities
                .stream()
                .filter(PCEPCapability::isStateful)
                .findAny();

        final TopologySessionListenerFactory listenerFactory = dependenciesProvider.getTopologySessionListenerFactory();
        if (!statefulCapability.isPresent()) {
            throw new IllegalStateException(STATEFUL_NOT_DEFINED);
        }

        final ServerSessionManager manager = new ServerSessionManager(dependenciesProvider, listenerFactory,
                configDependencies);

        return new PCEPTopologyProvider(configDependencies, dependenciesProvider, manager, scheduler);
    }

    public void instantiateServiceInstance() throws ExecutionException, InterruptedException {
        final RpcProviderService rpcRegistry = this.dependenciesProvider.getRpcProviderRegistry();

        this.element = requireNonNull(rpcRegistry
            .registerRpcImplementation(NetworkTopologyPcepService.class, new TopologyRPCs(this.manager),
                Collections.singleton(this.configDependencies.getTopology())));

        this.network = requireNonNull(rpcRegistry
            .registerRpcImplementation(NetworkTopologyPcepProgrammingService.class,
                new TopologyProgramming(this.scheduler, this.manager),
                Collections.singleton(this.configDependencies.getTopology())));

        this.manager.instantiateServiceInstance();
        final ChannelFuture channelFuture = this.dependenciesProvider.getPCEPDispatcher()
                .createServer(this.manager.getPCEPDispatcherDependencies());
        channelFuture.get();
        this.channel = channelFuture.channel();
    }

    public FluentFuture<? extends CommitInfo> closeServiceInstance() {
        //FIXME return also channelClose once ListenableFuture implements wildcard
        this.channel.close().addListener((ChannelFutureListener) future ->
                checkArgument(future.isSuccess(), "Channel failed to close: %s", future.cause()));

        if (this.network != null) {
            this.network.close();
            this.network = null;
        }
        if (this.element != null) {
            this.element.close();
            this.element = null;
        }
        return this.manager.closeServiceInstance();
    }
}