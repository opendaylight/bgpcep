/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.util.List;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyProviderDependenciesProvider;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
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
    private BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> network;
    private BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> element;
    private final ServerSessionManager manager;
    private final InetSocketAddress address;
    private final Optional<KeyMapping> keys;
    private final InstructionScheduler scheduler;
    private final PCEPTopologyProviderDependenciesProvider dependenciesProvider;
    private Channel channel;

    public static PCEPTopologyProvider create(final PCEPTopologyProviderDependenciesProvider dependenciesProvider,
        final InetSocketAddress address, final Optional<KeyMapping> keys, final InstructionScheduler scheduler,
        final TopologyId topologyId, final Optional<PCEPTopologyProviderRuntimeRegistrator> runtimeRootRegistrator,
        final int rpcTimeout) throws Exception {
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
            .child(Topology.class, new TopologyKey(topologyId)).build();
        final ServerSessionManager manager = new ServerSessionManager(dependenciesProvider.getDataBroker(), topology,
            listenerFactory, rpcTimeout);
        if(runtimeRootRegistrator.isPresent()){
            manager.setRuntimeRootRegistrator(runtimeRootRegistrator.get());
        }

        return new PCEPTopologyProvider(address, keys, dependenciesProvider, topology, manager,  scheduler);
    }

    private PCEPTopologyProvider(final InetSocketAddress address, final Optional<KeyMapping> keys,
        final PCEPTopologyProviderDependenciesProvider dependenciesProvider,
        final InstanceIdentifier<Topology> topology, final ServerSessionManager manager,
        final InstructionScheduler scheduler) {
        super(topology);
        this.dependenciesProvider = Preconditions.checkNotNull(dependenciesProvider);
        this.address = Preconditions.checkNotNull(address);
        this.topology = Preconditions.checkNotNull(topology);
        this.keys = Preconditions.checkNotNull(keys);
        this.manager = Preconditions.checkNotNull(manager);
        this.scheduler = Preconditions.checkNotNull(scheduler);
    }

    public void instantiateServiceInstance() {
        final RpcProviderRegistry rpcRegistry = this.dependenciesProvider.getRpcProviderRegistry();

        this.element = Preconditions.checkNotNull(rpcRegistry
            .addRoutedRpcImplementation(NetworkTopologyPcepService.class, new TopologyRPCs(this.manager)));
        this.element.registerPath(NetworkTopologyContext.class, this.topology);

        this.network = Preconditions.checkNotNull(rpcRegistry
            .addRoutedRpcImplementation(NetworkTopologyPcepProgrammingService.class,
                new TopologyProgramming(this.scheduler, this.manager)));
        this.network.registerPath(NetworkTopologyContext.class, this.topology);
        try {
            this.manager.instantiateServiceInstance().checkedGet();
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