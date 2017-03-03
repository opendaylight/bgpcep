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
import com.google.common.collect.Iterables;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.epoll.Epoll;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.pcep.topology.provider.config.rev170301.OdlPcepTopologyProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.pcep.topology.provider.config.rev170301.OdlPcepTopologyProviderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.pcep.topology.provider.config.rev170301.odl.pcep.topology.provider.OdlPcepTopologyProviderConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.pcep.topology.provider.config.rev170301.odl.pcep.topology.provider.OdlPcepTopologyProviderConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.pcep.topology.provider.config.rev170301.odl.pcep.topology.provider.OdlPcepTopologyProviderConfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.pcep.topology.provider.config.rev170301.odl.pcep.topology.provider.odl.pcep.topology.provider.config.Client;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPTopologyDeployerImpl implements PCEPTopologyDeployer,
    ClusteredDataTreeChangeListener<OdlPcepTopologyProvider>, AutoCloseable {
    public static final String PCEP_TOPOLOGY_PROVIDER_BEAN = "pcep-topology-provider-bean";
    public static final String NATIVE_TRANSPORT_NOT_AVAILABLE = "Client is configured with password but native" +
        " transport is not available";
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyDeployerImpl.class);
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
    private final InstanceIdentifier<OdlPcepTopologyProvider> iid;
    private final ListenerRegistration<PCEPTopologyDeployerImpl> listenerRegistration;
    private final ClusterSingletonServiceProvider cssp;

    public PCEPTopologyDeployerImpl(final BundleContext bundleContext, final DataBroker dataBroker,
        final PCEPDispatcher pcepDispatcher, final RpcProviderRegistry rpcProviderRegistry,
        final TopologySessionListenerFactory sessionListenerFactory, final ClusterSingletonServiceProvider cssp) {
        this.bundleContext = Preconditions.checkNotNull(bundleContext);
        this.pcepDispatcher = Preconditions.checkNotNull(pcepDispatcher);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.sessionListenerFactory = Preconditions.checkNotNull(sessionListenerFactory);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        this.cssp = Preconditions.checkNotNull(cssp);
        final List<PCEPCapability> capabilities = this.pcepDispatcher.getPCEPSessionNegotiatorFactory()
            .getPCEPSessionProposalFactory().getCapabilities();
        final boolean statefulCapability = capabilities.stream().anyMatch(PCEPCapability::isStateful);
        if (!statefulCapability) {
            throw new IllegalStateException(STATEFUL_NOT_DEFINED);
        }
        this.iid = InstanceIdentifier.create(OdlPcepTopologyProvider.class);

        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, this.iid, new OdlPcepTopologyProviderBuilder().build());
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Instruction Instance {} initialized successfully.", PCEPTopologyDeployerImpl.this.iid);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initialize Instruction Instance {}.", PCEPTopologyDeployerImpl.this.iid, t);
            }
        });

        this.listenerRegistration = this.dataBroker.registerDataTreeChangeListener(
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, this.iid), this);
    }

    private static Optional<KeyMapping> contructKeys(final List<Client> clients) {
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

    public static String getAddressString(final IpAddress address) {
        Preconditions.checkArgument(address.getIpv4Address() != null || address.getIpv6Address() != null,
            "Address %s is invalid", address);
        if (address.getIpv4Address() != null) {
            return address.getIpv4Address().getValue();
        }
        return address.getIpv6Address().getValue();
    }

    private synchronized void createTopologyProvider(final TopologyId topologyId, final InetAddress address,
        final int port, final short rpcTimeout, final Optional<KeyMapping> keys,
        final InstructionScheduler schedulerDependency) {
        final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(topologyId)).build();
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(address, port);


        try {
            final PCEPTopologyProvider pcepTopoProvider = PCEPTopologyProvider.create(this.pcepDispatcher,
                inetSocketAddress, keys, schedulerDependency, this.dataBroker, this.rpcProviderRegistry, topology,
                this.sessionListenerFactory, Optional.fromNullable(this.runtimeRegistrators.get(topologyId)),
                rpcTimeout, this.cssp);
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

    private synchronized void removeTopologyProvider(final TopologyId topologyID) {
        final PCEPTopologyProvider service = this.pcepTopologyServices.remove(topologyID);
        if (service != null) {
            LOG.trace("Removing and closing Topology provider {}.", topologyID.getValue());
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
    public synchronized ListenableFuture<Void> writeConfiguration(@Nonnull final TopologyId topologyID, final String instructionID,
        final InetAddress address, final PortNumber portNumber, final short rpcTimeout,
        final List<Client> client) {
        final OdlPcepTopologyProviderConfig instruction = new OdlPcepTopologyProviderConfigBuilder()
            .setTopologyId(topologyID)
            .setListenAddress(IetfInetUtil.INSTANCE.ipAddressFor(address))
            .setListenPort(portNumber)
            .setRpcTimeout(rpcTimeout)
            .setSchedulerId(instructionID)
            .setClient(client)
            .build();
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, this.iid.child(OdlPcepTopologyProviderConfig.class,
            new OdlPcepTopologyProviderConfigKey(topologyID)), instruction, true);
        final CheckedFuture<Void, TransactionCommitFailedException> future = wTx.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Instruction Instance {} initialized successfully.", topologyID);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initialize Instruction Instance {}.", topologyID, t);
            }
        });
        return future;
    }

    @Override
    public synchronized ListenableFuture<Void> removeConfiguration(@Nonnull final TopologyId topologyID) {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, this.iid.child(OdlPcepTopologyProviderConfig.class,
            new OdlPcepTopologyProviderConfigKey(topologyID)));
        final CheckedFuture<Void, TransactionCommitFailedException> future = wTx.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Instruction Instance {} removed successfully.", topologyID);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to remove Instruction Instance {}.", topologyID, t);
            }
        });
        return future;
    }

    @Override
    public synchronized void close() throws Exception {
        this.listenerRegistration.close();
        this.pcepTopologyServices.values().forEach(PCEPTopologyProvider::close);
    }

    @Override
    public synchronized void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<OdlPcepTopologyProvider>> changes) {
        final DataTreeModification<OdlPcepTopologyProvider> dataTreeModification = Iterables.getOnlyElement(changes);
        final DataObjectModification<OdlPcepTopologyProvider> rootNode = dataTreeModification.getRootNode();
        rootNode.getModifiedChildren()
            .forEach(config -> handleModification((DataObjectModification<OdlPcepTopologyProviderConfig>) config));
    }

    private void handleModification(final DataObjectModification<OdlPcepTopologyProviderConfig> config) {
        final ModificationType modificationType = config.getModificationType();
        LOG.trace("Pcep Topology Provider configuration has changed: {}, type modification {}", config, modificationType);
        switch (modificationType) {
            case DELETE:
                removeTopologyProvider(config.getDataBefore().getTopologyId());
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                updateTopologyProvider(config.getDataAfter());
                break;
            default:
                break;
        }
    }

    private void updateTopologyProvider(final OdlPcepTopologyProviderConfig configuration) {
        final String schedulerID = configuration.getSchedulerId();

        final WaitingServiceTracker<InstructionScheduler> instructionSchedulerTracker = WaitingServiceTracker
            .create(InstructionScheduler.class,
                this.bundleContext, "(" + InstructionScheduler.class.getName() + "=" + schedulerID + ")");
        final InstructionScheduler instructionScheduler = instructionSchedulerTracker
            .waitForService(WaitingServiceTracker.FIVE_MINUTES);

        final Optional<KeyMapping> keys = contructKeys(configuration.getClient());
        if (keys.isPresent() && !Epoll.isAvailable()) {
            LOG.error(NATIVE_TRANSPORT_NOT_AVAILABLE);
            return;
        }

        final Integer port = configuration.getListenPort().getValue();
        final IpAddress ipAddress = configuration.getListenAddress();
        final TopologyId topologyId = configuration.getTopologyId();
        final InetAddress inetAddr = IetfInetUtil.INSTANCE.inetAddressFor(ipAddress);

        if(this.pcepTopologyServices.containsKey(topologyId)) {
            LOG.trace("Removing and closing Topology provider {}.", topologyId.getValue());
            final PCEPTopologyProvider service = this.pcepTopologyServices.remove(topologyId);
            service.close();
        }
        createTopologyProvider(topologyId, inetAddr, port, configuration.getRpcTimeout(),
            keys, instructionScheduler);
    }
}
