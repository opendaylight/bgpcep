/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getNeighborInstanceIdentifier;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getNeighborInstanceName;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getRibInstanceName;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.spi.InstanceType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.ProtocolsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpDeployerImpl implements BgpDeployer, ClusteredDataTreeChangeListener<Bgp>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BgpDeployerImpl.class);
    private final InstanceIdentifier<NetworkInstance> networkInstanceIId;
    private final BlueprintContainer container;
    private final BundleContext bundleContext;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private final ListenerRegistration<BgpDeployerImpl> registration;
    @GuardedBy("this")
    private final Map<InstanceIdentifier<Bgp>, RibImpl> ribs = new HashMap<>();
    @GuardedBy("this")
    private final Map<InstanceIdentifier<Neighbor>, PeerBean> peers = new HashMap<>();
    private final DataBroker dataBroker;
    @GuardedBy("this")
    private boolean closed;

    public BgpDeployerImpl(final String networkInstanceName, final BlueprintContainer container,
        final BundleContext bundleContext, final DataBroker dataBroker,
        final BGPTableTypeRegistryConsumer mappingService) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.container = Preconditions.checkNotNull(container);
        this.bundleContext = Preconditions.checkNotNull(bundleContext);
        this.tableTypeRegistry = Preconditions.checkNotNull(mappingService);
        this.networkInstanceIId = InstanceIdentifier.create(NetworkInstances.class)
            .child(NetworkInstance.class, new NetworkInstanceKey(networkInstanceName));
        Futures.addCallback(initializeNetworkInstance(dataBroker, this.networkInstanceIId), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Network Instance {} initialized successfully.", networkInstanceName);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initialize Network Instance {}.", networkInstanceName, t);
            }
        });
        this.registration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
            this.networkInstanceIId.child(Protocols.class).child(Protocol.class).augmentation(Protocol1.class).child(Bgp.class)), this);
        LOG.info("BGP Deployer {} started.", networkInstanceName);
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
            LOG.trace("BGP configuration has changed: {}", rootNode);
            for (final DataObjectModification<? extends DataObject> dataObjectModification : rootNode.getModifiedChildren()) {
                if (dataObjectModification.getDataType().equals(Global.class)) {
                    onGlobalChanged((DataObjectModification<Global>) dataObjectModification, rootIdentifier);
                } else if (dataObjectModification.getDataType().equals(Neighbors.class)) {
                    onNeighborsChanged((DataObjectModification<Neighbors>) dataObjectModification, rootIdentifier);
                }
            }
        }
    }

    @Override
    public InstanceIdentifier<NetworkInstance> getInstanceIdentifier() {
        return this.networkInstanceIId;
    }

    @Override
    public synchronized void close() throws Exception {
        this.registration.close();
        this.peers.values().forEach(PeerBean::close);
        this.peers.clear();
        this.ribs.values().forEach(RibImpl::close);
        this.ribs.clear();
        this.closed = true;
    }

    private static CheckedFuture<Void, TransactionCommitFailedException> initializeNetworkInstance(
        final DataBroker dataBroker, final InstanceIdentifier<NetworkInstance> networkInstance) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, networkInstance,
            new NetworkInstanceBuilder().setName(networkInstance.firstKeyOf(NetworkInstance.class).getName())
                .setProtocols(new ProtocolsBuilder().build()).build());
        return wTx.submit();
    }

    private synchronized void onGlobalChanged(final DataObjectModification<Global> dataObjectModification,
        final InstanceIdentifier<Bgp> rootIdentifier) {
        switch (dataObjectModification.getModificationType()) {
        case DELETE:
            onGlobalRemoved(rootIdentifier);
            break;
        case SUBTREE_MODIFIED:
        case WRITE:
            onGlobalModified(rootIdentifier, dataObjectModification.getDataAfter(), null);
            break;
        default:
            break;
        }
    }

    @Override
    public synchronized void onGlobalModified(final InstanceIdentifier<Bgp> rootIdentifier, final Global global,
        final WriteConfiguration configurationWriter) {
        final RibImpl ribImpl = this.ribs.get(rootIdentifier);
        if(ribImpl == null ) {
            onGlobalCreated(rootIdentifier, global, configurationWriter);
        } else if (!ribImpl.isGlobalEqual(global)) {
            onGlobalUpdated(rootIdentifier, global, ribImpl, configurationWriter);
        }
    }

    private synchronized List<PeerBean> closeAllBindedPeers(final InstanceIdentifier<Bgp> rootIdentifier) {
        final List<PeerBean> filtered = new ArrayList<>();
        this.peers.entrySet().stream().filter(entry -> entry.getKey().firstIdentifierOf(Bgp.class)
            .contains(rootIdentifier)).forEach(entry -> {final PeerBean peer = entry.getValue();
            peer.close();
            filtered.add(peer);
        });
        return filtered;
    }

    private synchronized void onGlobalCreated(final InstanceIdentifier<Bgp> rootIdentifier, final Global global,
        final WriteConfiguration configurationWriter) {
        LOG.debug("Creating RIB instance with configuration: {}", global);
        final RibImpl ribImpl = (RibImpl) this.container.getComponentInstance(InstanceType.RIB.getBeanName());
        initiateRibInstance(rootIdentifier, global, ribImpl, configurationWriter);
        this.ribs.put(rootIdentifier, ribImpl);
        LOG.debug("RIB instance created: {}", ribImpl);
    }

    private synchronized void onGlobalUpdated(final InstanceIdentifier<Bgp> rootIdentifier, final Global global,
        final RibImpl ribImpl, final WriteConfiguration configurationWriter) {
        LOG.debug("Modifying RIB instance with configuration: {}", global);
        final List<PeerBean> closedPeers = closeAllBindedPeers(rootIdentifier);
        ribImpl.close();
        initiateRibInstance(rootIdentifier, global, ribImpl, configurationWriter);
        closedPeers.forEach(peer -> peer.restart(ribImpl, this.tableTypeRegistry));
        LOG.debug("RIB instance created: {}", ribImpl);
    }

    @Override
    public synchronized void onGlobalRemoved(final InstanceIdentifier<Bgp> rootIdentifier) {
        LOG.debug("Removing RIB instance: {}", rootIdentifier);
        final RibImpl ribImpl = this.ribs.remove(rootIdentifier);
        if (ribImpl != null) {
            LOG.debug("RIB instance removed {}", ribImpl);
            ribImpl.close();
        }
    }

    private synchronized void registerRibInstance(final RibImpl ribImpl, final String ribInstanceName) {
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(InstanceType.RIB.getBeanName(), ribInstanceName);
        final ServiceRegistration<?> serviceRegistration = this.bundleContext.registerService(InstanceType.RIB.getServices(), ribImpl, properties);
        ribImpl.setServiceRegistration(serviceRegistration);
    }

    private synchronized void initiateRibInstance(final InstanceIdentifier<Bgp> rootIdentifier, final Global global,
        final RibImpl ribImpl, final WriteConfiguration configurationWriter) {
        final String ribInstanceName = getRibInstanceName(rootIdentifier);
        ribImpl.start(global, ribInstanceName, this.tableTypeRegistry, configurationWriter);
        registerRibInstance(ribImpl, ribInstanceName);
    }

    private synchronized void onNeighborsChanged(final DataObjectModification<Neighbors> dataObjectModification,
        final InstanceIdentifier<Bgp> rootIdentifier) {
        for (final DataObjectModification<? extends DataObject> neighborModification : dataObjectModification.getModifiedChildren()) {
            switch (neighborModification.getModificationType()) {
            case DELETE:
                onNeighborRemoved(rootIdentifier, (Neighbor) neighborModification.getDataBefore());
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                onNeighborModified(rootIdentifier, (Neighbor) neighborModification.getDataAfter(), null);
                break;
            default:
                break;
            }
        }
    }

    @Override
    public synchronized void onNeighborModified(final InstanceIdentifier<Bgp> rootIdentifier, final Neighbor neighbor,
        final WriteConfiguration configurationWriter) {
        //restart peer instance with a new configuration
        final PeerBean bgpPeer = this.peers.get(getNeighborInstanceIdentifier(rootIdentifier, neighbor.getKey()));
        if (bgpPeer == null) {
            onNeighborCreated(rootIdentifier, neighbor, configurationWriter);
        } else if(!bgpPeer.containsEqualConfiguration(neighbor)){
            onNeighborUpdated(bgpPeer, rootIdentifier, neighbor, configurationWriter);
        }
    }

    private synchronized void onNeighborCreated(final InstanceIdentifier<Bgp> rootIdentifier, final Neighbor neighbor,
        final WriteConfiguration configurationWriter) {
        LOG.debug("Creating Peer instance with configuration: {}", neighbor);
        final PeerBean bgpPeer;
        if (OpenConfigMappingUtil.isApplicationPeer(neighbor)) {
            bgpPeer = (PeerBean) this.container.getComponentInstance(InstanceType.APP_PEER.getBeanName());
        } else {
            bgpPeer = (PeerBean) this.container.getComponentInstance(InstanceType.PEER.getBeanName());
        }
        final InstanceIdentifier<Neighbor> neighborInstanceIdentifier = getNeighborInstanceIdentifier(rootIdentifier, neighbor.getKey());
        initiatePeerInstance(rootIdentifier, neighborInstanceIdentifier, neighbor, bgpPeer, configurationWriter);
        this.peers.put(neighborInstanceIdentifier, bgpPeer);
        LOG.debug("Peer instance created {}", bgpPeer);
    }

    private synchronized void onNeighborUpdated(final PeerBean bgpPeer, final InstanceIdentifier<Bgp> rootIdentifier, final Neighbor neighbor,
            final WriteConfiguration configurationWriter) {
        LOG.debug("Updating Peer instance with configuration: {}", neighbor);
        bgpPeer.close();
        final InstanceIdentifier<Neighbor> neighborInstanceIdentifier = getNeighborInstanceIdentifier(rootIdentifier, neighbor.getKey());
        initiatePeerInstance(rootIdentifier, neighborInstanceIdentifier, neighbor, bgpPeer, configurationWriter);
        LOG.debug("Peer instance updated {}", bgpPeer);
    }

    @Override
    public synchronized void onNeighborRemoved(final InstanceIdentifier<Bgp> rootIdentifier, final Neighbor neighbor) {
        LOG.debug("Removing Peer instance: {}", rootIdentifier);
        final PeerBean bgpPeer = this.peers.remove(getNeighborInstanceIdentifier(rootIdentifier, neighbor.getKey()));
        if (bgpPeer != null) {
            bgpPeer.close();
            LOG.debug("Peer instance removed {}", bgpPeer);
        }
    }

    private synchronized void registerPeerInstance(final BgpPeer bgpPeer, final String peerInstanceName) {
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(InstanceType.PEER.getBeanName(), peerInstanceName);
        final ServiceRegistration<?> serviceRegistration = this.bundleContext.registerService(InstanceType.PEER.getServices(), bgpPeer, properties);
        bgpPeer.setServiceRegistration(serviceRegistration);
    }

    private synchronized void registerAppPeerInstance(final AppPeer appPeer, final String peerInstanceName) {
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(InstanceType.PEER.getBeanName(), peerInstanceName);
        final ServiceRegistration<?> serviceRegistration = this.bundleContext
            .registerService(InstanceType.APP_PEER.getServices(), appPeer, properties);
        appPeer.setServiceRegistration(serviceRegistration);
    }

    private synchronized void initiatePeerInstance(final InstanceIdentifier<Bgp> rootIdentifier, final InstanceIdentifier<Neighbor> neighborIdentifier, final Neighbor neighbor,
        final PeerBean bgpPeer, final WriteConfiguration configurationWriter) {
        final String peerInstanceName = getNeighborInstanceName(neighborIdentifier);
        final RibImpl rib = this.ribs.get(rootIdentifier);
        if (rib != null) {
            bgpPeer.start(rib, neighbor, this.tableTypeRegistry, configurationWriter);
            if (bgpPeer instanceof BgpPeer) {
                registerPeerInstance((BgpPeer) bgpPeer, peerInstanceName);
            } else if(bgpPeer instanceof AppPeer) {
                registerAppPeerInstance((AppPeer) bgpPeer, peerInstanceName);
            }
        }
    }

    @Override
    public BGPTableTypeRegistryConsumer getTableTypeRegistry() {
        return this.tableTypeRegistry;
    }

    @Override
    public <T extends DataObject> ListenableFuture<Void> writeConfiguration(final T data,
        final InstanceIdentifier<T> identifier) {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, identifier, data, true);
        return wTx.submit();
    }

    @Override
    public <T extends DataObject> ListenableFuture<Void> removeConfiguration(
        final InstanceIdentifier<T> identifier) {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, identifier);
        return wTx.submit();
    }
}
