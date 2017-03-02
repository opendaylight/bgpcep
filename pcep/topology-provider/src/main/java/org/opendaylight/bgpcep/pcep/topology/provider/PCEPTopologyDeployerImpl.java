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
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;
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
    private final BundleContext bundleContext;
    private final PCEPDispatcher pcepDispatcher;
    private final DataBroker dataBroker;
    private final TopologySessionListenerFactory sessionListenerFactory;
    private final RpcProviderRegistry rpcProviderRegistry;
    @GuardedBy("this")
    private final Map<TopologyId, PCEPTopologyProvider> pcepTopologyServices = new HashMap<>();
    @GuardedBy("this")
    private final Map<TopologyId, PCEPTopologyProviderRuntimeRegistrator> runtimeRegistrators = new HashMap<>();

    public PCEPTopologyDeployerImpl(final BundleContext bundleContext, final DataBroker dataBroker,
        final PCEPDispatcher pcepDispatcher, final RpcProviderRegistry rpcProviderRegistry,
        final TopologySessionListenerFactory sessionListenerFactory) {
        this.bundleContext = Preconditions.checkNotNull(bundleContext);
        this.pcepDispatcher = Preconditions.checkNotNull(pcepDispatcher);
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

        final com.google.common.base.Optional<KeyMapping> keys = contructKeys(clients);
        if (keys.isPresent() && !Epoll.isAvailable()) {
            LOG.error(NATIVE_TRANSPORT_NOT_AVAILABLE);
            return;
        }

        try {
            final PCEPTopologyProvider pcepTopoProvider = PCEPTopologyProvider.create(this.pcepDispatcher,
                inetSocketAddress, keys, schedulerDependency, this.dataBroker, this.rpcProviderRegistry, topology,
                this.sessionListenerFactory, Optional.fromNullable(this.runtimeRegistrators.get(topologyId)), rpcTimeout);
            this.pcepTopologyServices.put(topologyId, pcepTopoProvider);

            final Dictionary<String, String> properties = new Hashtable<>();
            properties.put(PCEP_TOPOLOGY_PROVIDER_BEAN, topologyId.getValue());
            final ServiceRegistration<?> serviceRegistration = this.bundleContext
                .registerService(DefaultTopologyReference.class.getName(), pcepTopoProvider, properties);
            pcepTopoProvider.setServiceRegistration(serviceRegistration);
        } catch (final Exception e) {
            LOG.debug("Failed to create PCEPTopologyProvider {}", topologyId.getValue());
        }

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
}
