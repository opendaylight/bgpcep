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

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.util.List;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyConfigDependencies;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyProviderDependenciesProvider;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCEPTopologyProvider extends DefaultTopologyReference {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProvider.class);

    private static final String STATEFUL_NOT_DEFINED = "Stateful capability not defined, aborting PCEP Topology" +
            " Provider instantiation";
    private final InstanceIdentifier<Topology> topology;
    private final ServerSessionManager manager;
    private final InetSocketAddress address;
    private final KeyMapping keys;
    private final InstructionScheduler scheduler;
    private final PCEPTopologyProviderDependenciesProvider dependenciesProvider;
    private RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> network;
    private RoutedRpcRegistration<NetworkTopologyPcepService> element;
    private Channel channel;

    public static PCEPTopologyProvider create(final PCEPTopologyProviderDependenciesProvider dependenciesProvider,
            final PCEPTopologyConfigDependencies configDependencies) {
        final List<PCEPCapability> capabilities = dependenciesProvider.getPCEPDispatcher()
                .getPCEPSessionNegotiatorFactory().getPCEPSessionProposalFactory().getCapabilities();
        boolean statefulCapability = false;
        for (final PCEPCapability capability : capabilities) {
            if (capability.isStateful()) {
                statefulCapability = true;
                break;
            }
        }

        final TopologySessionListenerFactory listenerFactory = dependenciesProvider.getTopologySessionListenerFactory();
        if (!statefulCapability && listenerFactory != null) {
            throw new IllegalStateException(STATEFUL_NOT_DEFINED);
        }

        final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(configDependencies.getTopologyId())).build();
        final ServerSessionManager manager = new ServerSessionManager(
                dependenciesProvider.getDataBroker(),
                topology,
                listenerFactory,
                dependenciesProvider.getStateRegistry(),
                configDependencies.getRpcTimeout());

        return new PCEPTopologyProvider(configDependencies.getAddress(), configDependencies.getKeys(),
                dependenciesProvider, topology, manager, configDependencies.getSchedulerDependency());
    }

    private PCEPTopologyProvider(final InetSocketAddress address, final KeyMapping keys,
            final PCEPTopologyProviderDependenciesProvider dependenciesProvider,
            final InstanceIdentifier<Topology> topology, final ServerSessionManager manager,
            final InstructionScheduler scheduler) {
        super(topology);
        this.dependenciesProvider = requireNonNull(dependenciesProvider);
        this.address = address;
        this.topology = requireNonNull(topology);
        this.keys = keys;
        this.manager = requireNonNull(manager);
        this.scheduler = scheduler;
    }

    public void instantiateServiceInstance() {
        final RpcProviderRegistry rpcRegistry = this.dependenciesProvider.getRpcProviderRegistry();

        this.element = requireNonNull(rpcRegistry
                .addRoutedRpcImplementation(NetworkTopologyPcepService.class, new TopologyRPCs(this.manager)));
        this.element.registerPath(NetworkTopologyContext.class, this.topology);

        this.network = requireNonNull(rpcRegistry
                .addRoutedRpcImplementation(NetworkTopologyPcepProgrammingService.class,
                        new TopologyProgramming(this.scheduler, this.manager)));
        this.network.registerPath(NetworkTopologyContext.class, this.topology);
        try {
            this.manager.instantiateServiceInstance().get();
            final ChannelFuture channelFuture = this.dependenciesProvider.getPCEPDispatcher()
                    .createServer(this.address, this.keys, this.manager, this.manager);
            channelFuture.get();
            this.channel = channelFuture.channel();
        } catch (final Exception e) {
            LOG.error("Failed to instantiate PCEP Topology provider", e);
        }

    }

    public ListenableFuture<Void> closeServiceInstance() {
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