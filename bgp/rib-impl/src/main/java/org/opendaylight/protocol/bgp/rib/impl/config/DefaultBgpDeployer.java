/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProviderRegistry;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroups;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.OpenconfigNetworkInstanceData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.ProtocolsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NetworkInstanceProtocol;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
// Non-final because of Mockito.spy()
public class DefaultBgpDeployer implements DataTreeChangeListener<Bgp>, PeerGroupConfigLoader, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultBgpDeployer.class);

    private final WithKey<NetworkInstance, NetworkInstanceKey> networkInstanceIId;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private final ClusterSingletonServiceProvider provider;
    private final RpcProviderService rpcRegistry;
    private final RIBExtensionConsumerContext ribExtensionConsumerContext;
    private final BGPDispatcher bgpDispatcher;
    private final BGPRibRoutingPolicyFactory routingPolicyFactory;
    private final BGPStateProviderRegistry stateProviderRegistry;
    private final CodecsRegistry codecsRegistry;
    private final DOMDataBroker domDataBroker;
    private final DataBroker dataBroker;

    @GuardedBy("this")
    private final Map<DataObjectIdentifier<Bgp>, BGPClusterSingletonService> bgpCss = new HashMap<>();
    private final LoadingCache<WithKey<PeerGroup, PeerGroupKey>, Optional<PeerGroup>> peerGroups =
        CacheBuilder.newBuilder().build(new CacheLoader<>() {
            @Override
            public Optional<PeerGroup> load(final WithKey<PeerGroup, PeerGroupKey> key)
                    throws ExecutionException, InterruptedException {
                final FluentFuture<Optional<PeerGroup>> future;
                try (var tx = dataBroker.newReadOnlyTransaction()) {
                    future = tx.read(LogicalDatastoreType.CONFIGURATION, key);
                }
                return future.get();
            }
        });
    private final String networkInstanceName;
    private Registration registration;
    @GuardedBy("this")
    private boolean closed;

    @Inject
    public DefaultBgpDeployer(final String networkInstanceName,
                              final ClusterSingletonServiceProvider provider,
                              final RpcProviderService rpcRegistry,
                              final RIBExtensionConsumerContext ribExtensionConsumerContext,
                              final BGPDispatcher bgpDispatcher,
                              final BGPRibRoutingPolicyFactory routingPolicyFactory,
                              final CodecsRegistry codecsRegistry,
                              final DOMDataBroker domDataBroker,
                              final DataBroker dataBroker,
                              final BGPTableTypeRegistryConsumer tableTypeRegistry,
                              final BGPStateProviderRegistry stateProviderRegistry) {
        this.dataBroker = requireNonNull(dataBroker);
        this.provider = requireNonNull(provider);
        this.networkInstanceName = requireNonNull(networkInstanceName);
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        this.stateProviderRegistry = requireNonNull(stateProviderRegistry);
        this.rpcRegistry = requireNonNull(rpcRegistry);
        this.ribExtensionConsumerContext = requireNonNull(ribExtensionConsumerContext);
        this.bgpDispatcher = requireNonNull(bgpDispatcher);
        this.routingPolicyFactory = requireNonNull(routingPolicyFactory);
        this.codecsRegistry = requireNonNull(codecsRegistry);
        this.domDataBroker = requireNonNull(domDataBroker);
        networkInstanceIId =
            DataObjectIdentifier.builderOfInherited(OpenconfigNetworkInstanceData.class, NetworkInstances.class)
                .child(NetworkInstance.class, new NetworkInstanceKey(networkInstanceName))
                .build();
        initializeNetworkInstance(dataBroker, networkInstanceIId).addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Network Instance {} initialized successfully.", networkInstanceName);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to initialize Network Instance {}.", networkInstanceName, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    @PostConstruct
    // Split out of constructor to support partial mocking
    public synchronized void init() {
        registration = dataBroker.registerTreeChangeListener(LogicalDatastoreType.CONFIGURATION,
            networkInstanceIId.toBuilder().toReferenceBuilder()
                .child(Protocols.class)
                .child(Protocol.class)
                .augmentation(NetworkInstanceProtocol.class)
                .child(Bgp.class)
                .build(), this);
        LOG.info("BGP Deployer {} started.", networkInstanceName);
    }

    @Override
    public synchronized void onDataTreeChanged(final List<DataTreeModification<Bgp>> changes) {
        if (closed) {
            LOG.trace("BGP Deployer was already closed, skipping changes.");
            return;
        }

        for (var dataTreeModification : changes) {
            final var rootIdentifier = dataTreeModification.path();
            final var rootNode = dataTreeModification.getRootNode();
            final List<DataObjectModification<?>> deletedConfig = rootNode.modifiedChildren()
                .stream()
                .filter(mod -> mod.modificationType() == DataObjectModification.ModificationType.DELETE)
                .collect(Collectors.toList());
            final List<DataObjectModification<?>> changedConfig = rootNode.modifiedChildren()
                .stream()
                .filter(mod -> mod.modificationType() != DataObjectModification.ModificationType.DELETE)
                .collect(Collectors.toList());
            handleDeletions(deletedConfig, rootIdentifier);
            handleModifications(changedConfig, rootIdentifier);
        }
    }

    private void handleModifications(final List<DataObjectModification<? extends DataObject>> changedConfig,
                                     final DataObjectIdentifier<Bgp> rootIdentifier) {
        final List<DataObjectModification<? extends DataObject>> globalMod = changedConfig.stream()
                .filter(mod -> mod.dataType().equals(Global.class))
                .collect(Collectors.toList());
        final List<DataObjectModification<? extends DataObject>> peerMod = changedConfig.stream()
                .filter(mod -> !mod.dataType().equals(Global.class))
                .collect(Collectors.toList());
        if (!globalMod.isEmpty()) {
            handleGlobalChange(globalMod, rootIdentifier);
        }
        if (!peerMod.isEmpty()) {
            handlePeersChange(peerMod, rootIdentifier);
        }
    }

    private void handleDeletions(final List<DataObjectModification<? extends DataObject>> deletedConfig,
                                 final DataObjectIdentifier<Bgp> rootIdentifier) {
        final List<DataObjectModification<? extends DataObject>> globalMod = deletedConfig.stream()
                .filter(mod -> mod.dataType().equals(Global.class))
                .collect(Collectors.toList());
        final List<DataObjectModification<? extends DataObject>> peerMod = deletedConfig.stream()
                .filter(mod -> !mod.dataType().equals(Global.class))
                .collect(Collectors.toList());
        if (!globalMod.isEmpty()) {
            handleGlobalChange(globalMod, rootIdentifier);
        }
        if (!peerMod.isEmpty()) {
            handlePeersChange(peerMod, rootIdentifier);
        }
    }

    private void handleGlobalChange(
            final List<DataObjectModification<? extends DataObject>> config,
            final DataObjectIdentifier<Bgp> rootIdentifier) {
        for (var dataObjectModification : config) {
            onGlobalChanged((DataObjectModification<Global>) dataObjectModification, rootIdentifier);
        }
    }

    private void handlePeersChange(
            final List<DataObjectModification<? extends DataObject>> config,
            final DataObjectIdentifier<Bgp> rootIdentifier) {
        for (var dataObjectModification : config) {
            if (dataObjectModification.dataType().equals(Neighbors.class)) {
                onNeighborsChanged((DataObjectModification<Neighbors>) dataObjectModification, rootIdentifier);
            } else if (dataObjectModification.dataType().equals(PeerGroups.class)) {
                rebootNeighbors((DataObjectModification<PeerGroups>) dataObjectModification);
            }
        }
    }

    private synchronized void rebootNeighbors(final DataObjectModification<PeerGroups> dataObjectModification) {
        var extPeerGroups = dataObjectModification.dataAfter();
        if (extPeerGroups == null) {
            extPeerGroups = dataObjectModification.dataBefore();
        }
        if (extPeerGroups == null) {
            return;
        }
        for (var peerGroup : extPeerGroups.nonnullPeerGroup().values()) {
            bgpCss.values().forEach(css -> css.restartPeerGroup(peerGroup.getPeerGroupName()));
        }
    }

    @Override
    @PreDestroy
    @SuppressWarnings("checkstyle:illegalCatch")
    public synchronized void close() {
        LOG.info("Closing BGP Deployer.");
        if (registration != null) {
            registration.close();
            registration = null;
        }
        closed = true;

        bgpCss.values().iterator().forEachRemaining(service -> {
            try {
                service.close();
            } catch (Exception e) {
                LOG.warn("Failed to close BGP Cluster Singleton Service.", e);
            }
        });
    }

    private static FluentFuture<? extends CommitInfo> initializeNetworkInstance(
            final DataBroker dataBroker, final WithKey<NetworkInstance, NetworkInstanceKey> networkInstance) {
        final var wTx = dataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, networkInstance, new NetworkInstanceBuilder()
            .withKey(networkInstance.key())
            .setProtocols(new ProtocolsBuilder().build())
            .build());
        return wTx.commit();
    }

    synchronized void onGlobalChanged(final DataObjectModification<Global> dataObjectModification,
                                      final DataObjectIdentifier<Bgp> bgpInstanceIdentifier) {
        getBgpClusterSingleton(bgpInstanceIdentifier).onGlobalChanged(dataObjectModification);
    }

    synchronized void onNeighborsChanged(final DataObjectModification<Neighbors> dataObjectModification,
                                         final DataObjectIdentifier<Bgp> bgpInstanceIdentifier) {
        getBgpClusterSingleton(bgpInstanceIdentifier).onNeighborsChanged(dataObjectModification);
    }

    @VisibleForTesting
    synchronized BGPClusterSingletonService getBgpClusterSingleton(
            final DataObjectIdentifier<Bgp> bgpInstanceIdentifier) {
        BGPClusterSingletonService old = bgpCss.get(bgpInstanceIdentifier);
        if (old == null) {
            old = new BGPClusterSingletonService(this, provider, tableTypeRegistry,
                    rpcRegistry, ribExtensionConsumerContext, bgpDispatcher, routingPolicyFactory,
                    codecsRegistry, stateProviderRegistry, domDataBroker, bgpInstanceIdentifier);
            bgpCss.put(bgpInstanceIdentifier, old);
        }
        return old;
    }

    @Override
    public PeerGroup getPeerGroup(final DataObjectIdentifier<Bgp> bgpIid, final String peerGroupName) {
        return peerGroups.getUnchecked(bgpIid.toBuilder()
            .child(PeerGroups.class)
            .child(PeerGroup.class, new PeerGroupKey(peerGroupName))
            .build())
            .orElse(null);
    }

}
