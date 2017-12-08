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
import java.util.List;
import java.util.Optional;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyConfiguration;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyProviderDependencies;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev171025.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.NetworkTopologyPcepService;
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
    private final ServerSessionManager manager;
    private final PCEPTopologyProviderDependencies dependenciesProvider;
    private final PCEPTopologyConfiguration configDependencies;
    private RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> network;
    private RoutedRpcRegistration<NetworkTopologyPcepService> element;
    private Channel channel;

    private PCEPTopologyProvider(
            final PCEPTopologyConfiguration configDependencies,
            final PCEPTopologyProviderDependencies dependenciesProvider,
            final ServerSessionManager manager) {
        super(configDependencies.getTopology());
        this.dependenciesProvider = requireNonNull(dependenciesProvider);
        this.configDependencies = configDependencies;
        this.manager = requireNonNull(manager);
    }

    public static PCEPTopologyProvider create(final PCEPTopologyProviderDependencies dependenciesProvider,
            final PCEPTopologyConfiguration configDependencies) {
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

        return new PCEPTopologyProvider(configDependencies, dependenciesProvider, manager);
    }

    public void instantiateServiceInstance() {
        final RpcProviderRegistry rpcRegistry = this.dependenciesProvider.getRpcProviderRegistry();

        this.element = requireNonNull(rpcRegistry
                .addRoutedRpcImplementation(NetworkTopologyPcepService.class, new TopologyRPCs(this.manager)));
        this.element.registerPath(NetworkTopologyContext.class, this.configDependencies.getTopology());

        this.network = requireNonNull(rpcRegistry
                .addRoutedRpcImplementation(NetworkTopologyPcepProgrammingService.class,
                        new TopologyProgramming(configDependencies.getSchedulerDependency(), this.manager)));
        this.network.registerPath(NetworkTopologyContext.class, this.configDependencies.getTopology());
        try {
            this.manager.instantiateServiceInstance().get();
            final ChannelFuture channelFuture = this.dependenciesProvider.getPCEPDispatcher()
                    .createServer(this.manager.getPCEPDispatcherDependencies());
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