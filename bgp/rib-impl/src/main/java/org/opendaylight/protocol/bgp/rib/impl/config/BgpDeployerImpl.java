/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroups;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.ProtocolsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NetworkInstanceProtocol;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpDeployerImpl implements ClusteredDataTreeChangeListener<Bgp>, PeerGroupConfigLoader,
        AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BgpDeployerImpl.class);
    private final InstanceIdentifier<NetworkInstance> networkInstanceIId;
    private final BlueprintContainer container;
    private final BundleContext bundleContext;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private final ClusterSingletonServiceProvider provider;
    private final LoadingCache<InstanceIdentifier<PeerGroup>, Optional<PeerGroup>> peerGroups = CacheBuilder.newBuilder()
            .build(new CacheLoader<InstanceIdentifier<PeerGroup>, Optional<PeerGroup>>() {
                @Override
                public Optional<PeerGroup> load(final InstanceIdentifier<PeerGroup> key)
                        throws ExecutionException, InterruptedException {
                    return loadPeerGroup(key);
                }
            });
    private ListenerRegistration<BgpDeployerImpl> registration;
    @GuardedBy("this")
    private final Map<InstanceIdentifier<Bgp>, BGPClusterSingletonService> bgpCss = new HashMap<>();
    private final DataBroker dataBroker;
    private final String networkInstanceName;
    @GuardedBy("this")
    private boolean closed;

    public BgpDeployerImpl(final String networkInstanceName, final ClusterSingletonServiceProvider provider,
            final BlueprintContainer container,
            final BundleContext bundleContext, final DataBroker dataBroker,
            final BGPTableTypeRegistryConsumer mappingService) {
        this.dataBroker = requireNonNull(dataBroker);
        this.provider = requireNonNull(provider);
        this.networkInstanceName = requireNonNull(networkInstanceName);
        this.container = requireNonNull(container);
        this.bundleContext = requireNonNull(bundleContext);
        this.tableTypeRegistry = requireNonNull(mappingService);
        this.networkInstanceIId = InstanceIdentifier.create(NetworkInstances.class)
                .child(NetworkInstance.class, new NetworkInstanceKey(networkInstanceName));
        initializeNetworkInstance(dataBroker, this.networkInstanceIId).addCallback(new FutureCallback<CommitInfo>() {
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

    public synchronized void init() {
        this.registration = this.dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        this.networkInstanceIId.child(Protocols.class).child(Protocol.class)
                                .augmentation(NetworkInstanceProtocol.class).child(Bgp.class)), this);
        LOG.info("BGP Deployer {} started.", this.networkInstanceName);
    }

    private Optional<PeerGroup> loadPeerGroup(final InstanceIdentifier<PeerGroup> peerGroupIid)
            throws ExecutionException, InterruptedException {
        final ReadOnlyTransaction tr = this.dataBroker.newReadOnlyTransaction();
        return tr.read(LogicalDatastoreType.CONFIGURATION, peerGroupIid).get().toJavaUtil();
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Bgp>> changes) {
        if (this.closed) {
            LOG.trace("BGP Deployer was already closed, skipping changes.");
            return;
        }

        for (final DataTreeModification<Bgp> dataTreeModification : changes) {
            final InstanceIdentifier<Bgp> rootIdentifier = dataTreeModification.getRootPath().getRootIdentifier();
            final DataObjectModification<Bgp> rootNode = dataTreeModification.getRootNode();
            final List<DataObjectModification<? extends DataObject>> deletedConfig
                    = rootNode.getModifiedChildren().stream()
                    .filter(mod -> mod.getModificationType() == DataObjectModification.ModificationType.DELETE)
                    .collect(Collectors.toList());
            final List<DataObjectModification<? extends DataObject>> changedConfig = rootNode.getModifiedChildren().stream()
                    .filter(mod -> mod.getModificationType() != DataObjectModification.ModificationType.DELETE)
                    .collect(Collectors.toList());
            handleDeletions(deletedConfig, rootIdentifier);
            handleModifications(changedConfig, rootIdentifier);
        }
    }

    private void handleModifications(final List<DataObjectModification<? extends DataObject>> changedConfig,
            final InstanceIdentifier<Bgp> rootIdentifier) {
        final List<DataObjectModification<? extends DataObject>> globalMod = changedConfig.stream()
                .filter(mod -> mod.getDataType().equals(Global.class))
                .collect(Collectors.toList());
        final List<DataObjectModification<? extends DataObject>> peerMod = changedConfig.stream()
                .filter(mod -> !mod.getDataType().equals(Global.class))
                .collect(Collectors.toList());
        if (!globalMod.isEmpty()) {
            handleGlobalChange(globalMod, rootIdentifier);
        }
        if (!peerMod.isEmpty()) {
            handlePeersChange(peerMod, rootIdentifier);
        }
    }

    private void handleDeletions(final List<DataObjectModification<? extends DataObject>> deletedConfig,
            final InstanceIdentifier<Bgp> rootIdentifier) {
        final List<DataObjectModification<? extends DataObject>> globalMod = deletedConfig.stream()
                .filter(mod -> mod.getDataType().equals(Global.class))
                .collect(Collectors.toList());
        final List<DataObjectModification<? extends DataObject>> peerMod = deletedConfig.stream()
                .filter(mod -> !mod.getDataType().equals(Global.class))
                .collect(Collectors.toList());
        if (!peerMod.isEmpty()) {
            handleGlobalChange(peerMod, rootIdentifier);
        }
        if (!globalMod.isEmpty()) {
            handlePeersChange(globalMod, rootIdentifier);
        }
    }

    private void handleGlobalChange(
            final List<DataObjectModification<? extends DataObject>> config,
            final InstanceIdentifier<Bgp> rootIdentifier) {
        for (final DataObjectModification<? extends DataObject> dataObjectModification : config) {
            onGlobalChanged((DataObjectModification<Global>) dataObjectModification, rootIdentifier);
        }
    }

    private void handlePeersChange(
            final List<DataObjectModification<? extends DataObject>> config,
            final InstanceIdentifier<Bgp> rootIdentifier) {
        for (final DataObjectModification<? extends DataObject> dataObjectModification : config) {
            if (dataObjectModification.getDataType().equals(Neighbors.class)) {
                onNeighborsChanged((DataObjectModification<Neighbors>) dataObjectModification, rootIdentifier);
            } else if (dataObjectModification.getDataType().equals(PeerGroups.class)) {
                rebootNeighbors((DataObjectModification<PeerGroups>) dataObjectModification);
            }
        }
    }

    private synchronized void rebootNeighbors(final DataObjectModification<PeerGroups> dataObjectModification) {
        PeerGroups peerGroups = dataObjectModification.getDataAfter();
        if (peerGroups == null) {
            peerGroups = dataObjectModification.getDataBefore();
        }
        if (peerGroups == null) {
            return;
        }
        for (final PeerGroup peerGroup: peerGroups.getPeerGroup()) {
            this.bgpCss.values().forEach(css->css.restartNeighbors(peerGroup.getPeerGroupName()));
        }
    }

    @Override
    public synchronized void close() {
        LOG.info("Closing BGP Deployer.");
        if (this.registration != null) {
            this.registration.close();
            this.registration = null;
        }
        this.closed = true;

        this.bgpCss.values().iterator().forEachRemaining(service -> {
            try {
                service.close();
            } catch (Exception e) {
                LOG.warn("Failed to close BGP Cluster Singleton Service.");
            }
        });

    }

    private static FluentFuture<? extends CommitInfo> initializeNetworkInstance(
            final DataBroker dataBroker, final InstanceIdentifier<NetworkInstance> networkInstance) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, networkInstance,
                new NetworkInstanceBuilder().setName(networkInstance.firstKeyOf(NetworkInstance.class).getName())
                        .setProtocols(new ProtocolsBuilder().build()).build());
        return wTx.commit();
    }

    @VisibleForTesting
    synchronized void onGlobalChanged(final DataObjectModification<Global> dataObjectModification,
            final InstanceIdentifier<Bgp> bgpInstanceIdentifier) {
        BGPClusterSingletonService old = this.bgpCss.get(bgpInstanceIdentifier);
        if (old == null) {
            old = new BGPClusterSingletonService(this, this.provider, this.tableTypeRegistry,
                    this.container, this.bundleContext, bgpInstanceIdentifier);
            this.bgpCss.put(bgpInstanceIdentifier, old);
        }
        old.onGlobalChanged(dataObjectModification);
    }

    @VisibleForTesting
    synchronized void onNeighborsChanged(final DataObjectModification<Neighbors> dataObjectModification,
            final InstanceIdentifier<Bgp> bgpInstanceIdentifier) {
        BGPClusterSingletonService old = this.bgpCss.get(bgpInstanceIdentifier);
        if (old == null) {
            old = new BGPClusterSingletonService(this, this.provider, this.tableTypeRegistry,
                    this.container, this.bundleContext, bgpInstanceIdentifier);
            this.bgpCss.put(bgpInstanceIdentifier, old);
        }
        old.onNeighborsChanged(dataObjectModification);
    }

    @VisibleForTesting
    BGPTableTypeRegistryConsumer getTableTypeRegistry() {
        return this.tableTypeRegistry;
    }

    @Override
    public PeerGroup getPeerGroup(final InstanceIdentifier<Bgp> bgpIid, final String peerGroupName) {
        final InstanceIdentifier<PeerGroup> peerGroupsIid =
        bgpIid.child(PeerGroups.class).child(PeerGroup.class, new PeerGroupKey(peerGroupName));
        return this.peerGroups.getUnchecked(peerGroupsIid).orElse(null);
    }
}
