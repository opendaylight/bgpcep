/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.Epoll;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.config.yang.pcep.topology.provider.Client;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPTopologyDeployerImpl implements PCEPTopologyDeployer, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyDeployerImpl.class);

    public static final String PCEP_TOPOLOGY_PROVIDER_BEAN = "pcep-topology-provider-bean";
    public static final String NATIVE_TRANSPORT_NOT_AVAILABLE = "Client is configured with password but native" +
        " transport is not available";
    private static final String STATEFUL_NOT_DEFINED = "Stateful capability not defined, aborting PCEP Topology " +
        "Deployer instantiation";
    private static final ServiceGroupIdentifier SGI = ServiceGroupIdentifier.create("pcep-topology-service-group");
    private final BundleContext bundleContext;
    private final PCEPDispatcher pcepDispatcher;
    private final DataBroker dataBroker;
    private final TopologySessionListenerFactory sessionListenerFactory;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final ClusterSingletonServiceProvider provider;
    @GuardedBy("this")
    private final Map<TopologyId, PCEPTopologyProvider> pcepTopologyServices = new HashMap<>();
    @GuardedBy("this")
    private final Map<TopologyId, PCEPTopologyProviderRuntimeRegistrator> runtimeRegistrators = new HashMap<>();

    public PCEPTopologyDeployerImpl(final BundleContext bundleContext, final DataBroker dataBroker,
        final PCEPDispatcher pcepDispatcher, final RpcProviderRegistry rpcProviderRegistry,
        final TopologySessionListenerFactory sessionListenerFactory, final ClusterSingletonServiceProvider provider) {
        this.bundleContext = Preconditions.checkNotNull(bundleContext);
        this.pcepDispatcher = Preconditions.checkNotNull(pcepDispatcher);
        this.provider = Preconditions.checkNotNull(provider);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.sessionListenerFactory = Preconditions.checkNotNull(sessionListenerFactory);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        final List<PCEPCapability> capabilities = this.pcepDispatcher.getPCEPSessionNegotiatorFactory()
            .getPCEPSessionProposalFactory().getCapabilities();
        final boolean statefulCapability = capabilities.stream().anyMatch(PCEPCapability::isStateful);
        if (!statefulCapability) {
            throw new IllegalStateException(STATEFUL_NOT_DEFINED);
        }
    }

    public static Optional<KeyMapping> contructKeys(final List<Client> clients) {
        KeyMapping ret = null;

        if (clients != null && !clients.isEmpty()) {
            ret = KeyMapping.getKeyMapping();
            for (final Client c : clients) {
                if (c.getAddress() == null) {
                    LOG.warn("Client {} does not have an address skipping it", c);
                    continue;
                }
                final Rfc2385Key rfc2385KeyPassword = c.getPassword();
                if (rfc2385KeyPassword != null && !rfc2385KeyPassword.getValue().isEmpty()) {
                    final String s = getAddressString(c.getAddress());
                    ret.put(InetAddresses.forString(s), rfc2385KeyPassword.getValue().getBytes(StandardCharsets.US_ASCII));
                }
            }
        }
        return Optional.fromNullable(ret);
    }

    private static String getAddressString(final IpAddress address) {
        Preconditions.checkArgument(address.getIpv4Address() != null || address.getIpv6Address() != null,
            "Address %s is invalid", address);
        if (address.getIpv4Address() != null) {
            return address.getIpv4Address().getValue();
        }
        return address.getIpv6Address().getValue();
    }

    @Override
    public synchronized void createTopologyProvider(final TopologyId topologyId, final InetAddress address,
        final int port, final short rpcTimeout, final List<Client> clients,
        final InstructionScheduler schedulerDependency) {
        if (this.pcepTopologyServices.containsKey(topologyId)) {
            LOG.warn("Topology Provider {} already exist. New instance won't be created", topologyId);
            return;
        }
        final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(topologyId)).build();
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(address, port);

        final Optional<KeyMapping> keys = contructKeys(clients);
        if (keys.isPresent() && !Epoll.isAvailable()) {
            LOG.error(NATIVE_TRANSPORT_NOT_AVAILABLE);
            return;
        }

        final PCEPTopologyProvider pcepTopoProvider = new PCEPTopologyProvider(topologyId, inetSocketAddress, keys,
            schedulerDependency, topology, rpcTimeout);
        this.pcepTopologyServices.put(topologyId, pcepTopoProvider);

        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(PCEP_TOPOLOGY_PROVIDER_BEAN, topologyId.getValue());
        final ServiceRegistration<?> serviceRegistration = this.bundleContext
            .registerService(DefaultTopologyReference.class.getName(), pcepTopoProvider, properties);
        pcepTopoProvider.setServiceRegistration(serviceRegistration);
    }

    @Override
    public synchronized void removeTopologyProvider(final TopologyId topologyID) {
        final PCEPTopologyProvider service = this.pcepTopologyServices.remove(topologyID);
        if (service != null) {
            service.close();
        }
    }

    @Override
    public synchronized void addRootRuntimeBeanRegistratorWrapper(final TopologyId topologyID,
        final PCEPTopologyProviderRuntimeRegistrator rootRuntimeBeanRegistratorWrapper) {
        if (rootRuntimeBeanRegistratorWrapper == null) {
            return;
        }
        this.runtimeRegistrators.put(topologyID, rootRuntimeBeanRegistratorWrapper);
    }

    @Override
    public synchronized void removeRootRuntimeBeanRegistratorWrapper(final TopologyId topologyID) {
        this.runtimeRegistrators.remove(topologyID);
    }

    @Override
    public synchronized void close() throws Exception {
        this.pcepTopologyServices.values().forEach(PCEPTopologyProvider::close);
    }

    private final class PCEPTopologyProvider extends DefaultTopologyReference
        implements ClusterSingletonService, AutoCloseable {
        private final InetSocketAddress address;
        private final Short rpcTimeout;
        private final Optional<KeyMapping> keys;
        private final InstructionScheduler schedulerDependency;
        private final ServerSessionManager manager;
        private ClusterSingletonServiceRegistration cssRegistration;
        private Channel serverChanel;
        private RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> network;
        private RoutedRpcRegistration<NetworkTopologyPcepService> element;
        private ServiceRegistration<?> serviceRegistration;

        PCEPTopologyProvider(final TopologyId topologyId, final InetSocketAddress inetSocketAddress,
            final Optional<KeyMapping> keyMappingOptional, final InstructionScheduler schedulerDependency,
            final InstanceIdentifier<Topology> topology, final short rpcTimeout) {
            super(topology);
            this.address = Preconditions.checkNotNull(inetSocketAddress);
            this.rpcTimeout = Preconditions.checkNotNull(rpcTimeout);
            this.keys = Preconditions.checkNotNull(keyMappingOptional);
            this.schedulerDependency = Preconditions.checkNotNull(schedulerDependency);
            this.manager = new ServerSessionManager(PCEPTopologyDeployerImpl.this.dataBroker,
                topology, PCEPTopologyDeployerImpl.this.sessionListenerFactory, this.rpcTimeout);

            synchronized (PCEPTopologyDeployerImpl.this.runtimeRegistrators) {
                final PCEPTopologyProviderRuntimeRegistrator registrator = PCEPTopologyDeployerImpl.this.
                    runtimeRegistrators.get(topologyId);
                if (registrator != null) {
                    this.manager.setRuntimeRootRegistrator(registrator);
                }
            }

            this.cssRegistration = PCEPTopologyDeployerImpl.this.provider
                .registerClusterSingletonService(this);
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
            this.manager.close();
            if (this.serviceRegistration != null) {
                this.serviceRegistration.unregister();
                this.serviceRegistration = null;
            }
        }

        @Override
        public synchronized void instantiateServiceInstance() {
            try {
                final InstanceIdentifier<Topology> topology = getInstanceIdentifier();
                this.manager.instantiateServiceInstance();
                final ChannelFuture server = PCEPTopologyDeployerImpl.this.pcepDispatcher
                    .createServer(this.address, this.keys, this.manager, this.manager);
                server.get();
                this.serverChanel = server.channel();
                this.element = PCEPTopologyDeployerImpl.this.rpcProviderRegistry
                    .addRoutedRpcImplementation(NetworkTopologyPcepService.class, new TopologyRPCs(this.manager));
                this.element.registerPath(NetworkTopologyContext.class, topology);

                this.network = PCEPTopologyDeployerImpl.this.rpcProviderRegistry.addRoutedRpcImplementation(
                    NetworkTopologyPcepProgrammingService.class,
                    new TopologyProgramming(this.schedulerDependency, this.manager));
                this.network.registerPath(NetworkTopologyContext.class, topology);
            } catch (final Exception e) {
                LOG.error("Failed to instantiate PCEP Topology provider", e);
            }

        }

        @Override
        public synchronized ListenableFuture<Void> closeServiceInstance() {
            this.serverChanel.close();
            LOG.debug("Server channel {} closed", this.serverChanel);

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
                this.manager.closeServiceInstance();
            } catch (final Exception e) {
                LOG.error("Failed to shutdown session manager", e);
            }
            return Futures.immediateFuture(null);
        }

        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return SGI;
        }

        synchronized void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
            this.serviceRegistration = serviceRegistration;
        }
    }
}
