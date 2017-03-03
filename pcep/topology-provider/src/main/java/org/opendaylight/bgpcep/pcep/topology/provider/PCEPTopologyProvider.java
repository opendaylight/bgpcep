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
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.util.List;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCEPTopologyProvider extends DefaultTopologyReference
    implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProvider.class);

    private static final String STATEFUL_NOT_DEFINED = "Stateful capability not defined, aborting PCEP Topology" +
        " Provider instantiation";
    private final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> network;
    private final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> element;
    private final ServerSessionManager manager;
    private final PCEPDispatcher dispatcher;
    private final InetSocketAddress address;
    private final Optional<KeyMapping> keys;
    private final InstructionScheduler scheduler;
    private ClusterSingletonServiceRegistration cssRegistration;
    private Channel channel;
    private ServiceRegistration<?> serviceRegistration;

    public static PCEPTopologyProvider create(final PCEPDispatcher dispatcher, final InetSocketAddress address,
        final Optional<KeyMapping> keys, final InstructionScheduler scheduler, final DataBroker dataBroker,
        final RpcProviderRegistry rpcRegistry, final InstanceIdentifier<Topology> topology,
        final TopologySessionListenerFactory listenerFactory,
        final Optional<PCEPTopologyProviderRuntimeRegistrator> runtimeRootRegistrator,
        final int rpcTimeout, final ClusterSingletonServiceProvider cssp) throws Exception {
        final List<PCEPCapability> capabilities = dispatcher.getPCEPSessionNegotiatorFactory()
            .getPCEPSessionProposalFactory().getCapabilities();
        boolean statefulCapability = false;
        for (final PCEPCapability capability : capabilities) {
            if (capability.isStateful()) {
                statefulCapability = true;
                break;
            }
        }
        if (!statefulCapability && listenerFactory != null) {
            throw new IllegalStateException(STATEFUL_NOT_DEFINED);
        }

        final ServerSessionManager manager = new ServerSessionManager(dataBroker, topology, listenerFactory, rpcTimeout);
        if(runtimeRootRegistrator.isPresent()){
            manager.setRuntimeRootRegistrator(runtimeRootRegistrator.get());
        }

        return new PCEPTopologyProvider(address, keys, dispatcher, topology, manager,  scheduler, rpcRegistry, cssp);
    }

    private PCEPTopologyProvider(final InetSocketAddress address, final Optional<KeyMapping> keys,
        final PCEPDispatcher dispatcher, final InstanceIdentifier<Topology> topology,
        final ServerSessionManager manager, final InstructionScheduler scheduler, final RpcProviderRegistry rpcRegistry,
        final ClusterSingletonServiceProvider cssp) {
        super(topology);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.address = Preconditions.checkNotNull(address);
        this.keys = Preconditions.checkNotNull(keys);
        this.manager = Preconditions.checkNotNull(manager);
        this.scheduler = Preconditions.checkNotNull(scheduler);

        this.element = Preconditions.checkNotNull(rpcRegistry
            .addRoutedRpcImplementation(NetworkTopologyPcepService.class, new TopologyRPCs(manager)));
        this.element.registerPath(NetworkTopologyContext.class, topology);

        this.network = Preconditions.checkNotNull(rpcRegistry
            .addRoutedRpcImplementation(NetworkTopologyPcepProgrammingService.class,
                new TopologyProgramming(scheduler, manager)));
        this.network.registerPath(NetworkTopologyContext.class, topology);

        LOG.info("PCEP Topology Provider service {} registered, RIB {}", getIdentifier().getValue());
        this.cssRegistration = cssp.registerClusterSingletonService(this);
    }

    @Override
    public void close() {
        if (this.cssRegistration != null) {
            try {
                this.cssRegistration.close();
            } catch (final Exception e) {
                LOG.debug("Failed to close PCEP Topology Provider service {}", getInstanceIdentifier(), e);
            }
            this.cssRegistration = null;
        }
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
            this.serviceRegistration = null;
        }
    }

    synchronized void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("RIB Singleton Service {} instantiated", getIdentifier().getValue());

        try {
            this.manager.instantiateServiceInstance();
            final ChannelFuture channelFuture = this.dispatcher
                .createServer(this.address, this.keys, this.manager, this.manager);
            channelFuture.get();
            this.channel = channelFuture.channel();
        } catch (final Exception e) {
            LOG.error("Failed to instantiate PCEP Topology provider", e);
        }

    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Close Topology Provider Singleton Service {}", getIdentifier().getValue());

        //FIXME return also channelClose once ListenableFuture implements wildcard
        this.channel.close().addListener((ChannelFutureListener) future ->
            Preconditions.checkArgument(future.isSuccess(), "Channel failed to close: %s", future.cause()));

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
        return this.manager.closeServiceInstance();
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return this.scheduler.getIdentifier();
    }
}
