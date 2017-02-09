/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCEPTopologyProvider extends DefaultTopologyReference implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProvider.class);
    private final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> network;
    private final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> element;
    private final ServerSessionManager manager;
    private final Channel channel;

    private PCEPTopologyProvider(final Channel channel, final InstanceIdentifier<Topology> topology, final ServerSessionManager manager,
            final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> element,
            final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> network) {
        super(topology);
        this.channel = Preconditions.checkNotNull(channel);
        this.manager = Preconditions.checkNotNull(manager);
        this.element = Preconditions.checkNotNull(element);
        this.network = Preconditions.checkNotNull(network);
    }

    public static PCEPTopologyProvider create(final PCEPDispatcher dispatcher, final InetSocketAddress address, final Optional<KeyMapping> keys,
            final InstructionScheduler scheduler, final DataBroker dataBroker, final RpcProviderRegistry rpcRegistry,
            final InstanceIdentifier<Topology> topology, final TopologySessionListenerFactory listenerFactory,
            final Optional<PCEPTopologyProviderRuntimeRegistrator> runtimeRootRegistrator, final int rpcTimeout) throws InterruptedException,
            ExecutionException, ReadFailedException, TransactionCommitFailedException {
        List<PCEPCapability> capabilities = dispatcher.getPCEPSessionNegotiatorFactory().getPCEPSessionProposalFactory().getCapabilities();
        boolean statefulCapability = false;
        for (final PCEPCapability capability : capabilities) {
            if (capability.isStateful()) {
                statefulCapability = true;
                break;
            }
        }
        if (!statefulCapability && listenerFactory != null) {
            throw new IllegalStateException("Stateful capability not defined, aborting PCEP Topology Provider instantiation");
        }

        final ServerSessionManager manager = new ServerSessionManager(dataBroker, topology, listenerFactory, rpcTimeout);
        if (runtimeRootRegistrator.isPresent()) {
            manager.setRuntimeRootRegistartion(runtimeRootRegistrator.get());
        }
        final ChannelFuture f = dispatcher.createServer(address, keys, manager, manager);
        f.get();

        final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> element = rpcRegistry.addRoutedRpcImplementation(
                NetworkTopologyPcepService.class, new TopologyRPCs(manager));
        element.registerPath(NetworkTopologyContext.class, topology);

        final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> network = rpcRegistry.addRoutedRpcImplementation(
                NetworkTopologyPcepProgrammingService.class, new TopologyProgramming(scheduler, manager));
        network.registerPath(NetworkTopologyContext.class, topology);

        return new PCEPTopologyProvider(f.channel(), topology, manager, element, network);
    }

    @Override
    public void close() throws InterruptedException {
        try {
            this.channel.close().sync();
            LOG.debug("Server channel {} closed", this.channel);
        } catch (InterruptedException e) {
            LOG.error("Failed to close channel {}", this.channel, e);
        }

        try {
            this.network.close();
        } catch (final Exception e) {
            LOG.error("Failed to unregister network-level RPCs", e);
        }
        try {
            this.element.close();
        } catch (final Exception e) {
            LOG.error("Failed to unregister element-level RPCs", e);
        }
        try {
            this.manager.close();
        } catch (final Exception e) {
            LOG.error("Failed to shutdown session manager", e);
        }
    }
}
