/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getNeighborInstanceIdentifier;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getNeighborInstanceName;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getRibInstanceName;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.InstanceType;
import org.opendaylight.protocol.bgp.rib.spi.util.ClusterSingletonServiceRegistrationHelper;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPClusterSingletonService implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BGPClusterSingletonService.class);

    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);
    private final InstanceIdentifier<Bgp> bgpIid;
    @GuardedBy("this")
    private final Map<InstanceIdentifier<Neighbor>, PeerBean> peers = new HashMap<>();
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private final BlueprintContainer container;
    private final BundleContext bundleContext;
    private final ServiceGroupIdentifier serviceGroupIdentifier;
    private final AtomicBoolean instantiated = new AtomicBoolean(false);
    private RibImpl ribImpl;

    BGPClusterSingletonService(
            @Nonnull final ClusterSingletonServiceProvider provider,
            @Nonnull final BGPTableTypeRegistryConsumer tableTypeRegistry,
            @Nonnull final BlueprintContainer container,
            @Nonnull final BundleContext bundleContext,
            @Nonnull final InstanceIdentifier<Bgp> bgpIid) {
        this.tableTypeRegistry = tableTypeRegistry;
        this.container = container;
        this.bundleContext = bundleContext;
        this.bgpIid = bgpIid;
        final String ribInstanceName = getRibInstanceName(bgpIid);
        this.serviceGroupIdentifier = ServiceGroupIdentifier.create(ribInstanceName + "-service-group");
        LOG.info("BGPClusterSingletonService {} registered", this.serviceGroupIdentifier.getValue());
        ClusterSingletonServiceRegistrationHelper
                .registerSingletonService(provider, this);
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        if (this.ribImpl != null) {
            this.ribImpl.instantiateServiceInstance();
            this.peers.values().forEach(PeerBean::instantiateServiceInstance);
        }
        this.instantiated.set(true);
        LOG.info("BGPClusterSingletonService {} instantiated", this.serviceGroupIdentifier.getValue());
    }

    @Override
    public synchronized ListenableFuture<Void> closeServiceInstance() {
        LOG.info("BGPClusterSingletonService {} close service instance", this.serviceGroupIdentifier.getValue());
        this.instantiated.set(false);

        final List<ListenableFuture<Void>> futurePeerCloseList = this.peers.values().stream()
                .map(PeerBean::closeServiceInstance).collect(Collectors.toList());
        final SettableFuture<Void> done = SettableFuture.create();
        Futures.addCallback(Futures.allAsList(futurePeerCloseList), new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(final List<Void> result) {
                done.setFuture(BGPClusterSingletonService.this.ribImpl.closeServiceInstance());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Failed removing peers {}", throwable);
            }
        }, MoreExecutors.directExecutor());
        return done;
    }

    @Nonnull
    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return this.serviceGroupIdentifier;
    }

    synchronized void onGlobalChanged(final DataObjectModification<Global> dataObjectModification) {
        switch (dataObjectModification.getModificationType()) {
            case DELETE:
                LOG.debug("Removing RIB instance: {}", this.bgpIid);
                if (this.ribImpl != null) {
                    LOG.debug("RIB instance removed {}", this.ribImpl);
                    closeAllBindedPeers();
                    closeRibService();
                }
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                final Global global = dataObjectModification.getDataAfter();
                if (this.ribImpl == null) {
                    onGlobalCreated(global);
                } else if (!this.ribImpl.isGlobalEqual(global)) {
                    onGlobalUpdated(global);
                }
                break;
            default:
                break;
        }
    }

    private synchronized void onGlobalCreated(final Global global) {
        LOG.debug("Creating RIB instance with configuration: {}", global);
        this.ribImpl = (RibImpl) this.container.getComponentInstance(InstanceType.RIB.getBeanName());
        initiateRibInstance(global, this.ribImpl);
        LOG.debug("RIB instance created: {}", this.ribImpl);
    }

    private synchronized void onGlobalUpdated(final Global global) {
        LOG.debug("Modifying RIB instance with configuration: {}", global);
        final List<PeerBean> closedPeers = closeAllBindedPeers();
        closeRibService();
        initiateRibInstance(global, this.ribImpl);
        for(final PeerBean peer :closedPeers) {
            peer.restart(this.ribImpl, this.tableTypeRegistry);
        }
        if (this.instantiated.get()) {
            closedPeers.forEach(PeerBean::instantiateServiceInstance);
        }
        LOG.debug("RIB instance created: {}", this.ribImpl);
    }

    private void closeRibService() {
        try {
            this.ribImpl.closeServiceInstance().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        } catch (final Exception e) {
            LOG.error("RIB instance failed to close service instance", e);
        }
        this.ribImpl.close();
    }

    private synchronized void initiateRibInstance(final Global global, final RibImpl ribImpl) {
        final String ribInstanceName = getRibInstanceName(this.bgpIid);
        ribImpl.start(global, ribInstanceName, this.tableTypeRegistry);
        registerRibInstance(ribImpl, ribInstanceName);
        if (this.instantiated.get()) {
            this.ribImpl.instantiateServiceInstance();
        }
    }

    private synchronized List<PeerBean> closeAllBindedPeers() {
        final List<PeerBean> filtered = new ArrayList<>();
        this.peers.forEach((key, peer) -> {
            try {
                peer.closeServiceInstance().get();
            } catch (final Exception e) {
                LOG.error("Peer instance failed to close service instance", e);
            }
            peer.close();
            filtered.add(peer);
        });
        return filtered;
    }

    private synchronized void registerRibInstance(final RibImpl ribImpl, final String ribInstanceName) {
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(InstanceType.RIB.getBeanName(), ribInstanceName);
        final ServiceRegistration<?> serviceRegistration = this.bundleContext.registerService(
                InstanceType.RIB.getServices(), ribImpl, properties);
        ribImpl.setServiceRegistration(serviceRegistration);
    }

    @Override
    public void close() throws Exception {
        LOG.info("BGPClusterSingletonService {} close", this.serviceGroupIdentifier.getValue());
        this.peers.values().iterator().forEachRemaining(PeerBean::close);
        this.ribImpl.close();
        this.peers.clear();
        this.ribImpl = null;
    }

    synchronized void onNeighborsChanged(final DataObjectModification<Neighbors> dataObjectModification) {
        for (final DataObjectModification<? extends DataObject> neighborModification :
                dataObjectModification.getModifiedChildren()) {
            switch (neighborModification.getModificationType()) {
                case DELETE:
                    onNeighborRemoved((Neighbor) neighborModification.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    onNeighborModified((Neighbor) neighborModification.getDataAfter());
                    break;
                default:
                    break;
            }
        }
    }

    private synchronized void onNeighborModified(final Neighbor neighbor) {
        //restart peer instance with a new configuration
        final PeerBean bgpPeer = this.peers.get(getNeighborInstanceIdentifier(this.bgpIid, neighbor.getKey()));
        if (bgpPeer == null) {
            onNeighborCreated(neighbor);
        } else if (!bgpPeer.containsEqualConfiguration(neighbor)) {
            onNeighborUpdated(bgpPeer, neighbor);
        }
    }


    private synchronized void onNeighborCreated(final Neighbor neighbor) {
        LOG.debug("Creating Peer instance with configuration: {}", neighbor);
        final PeerBean bgpPeer;
        if (OpenConfigMappingUtil.isApplicationPeer(neighbor)) {
            bgpPeer = (PeerBean) this.container.getComponentInstance(InstanceType.APP_PEER.getBeanName());
        } else {
            bgpPeer = (PeerBean) this.container.getComponentInstance(InstanceType.PEER.getBeanName());
        }
        final InstanceIdentifier<Neighbor> neighborInstanceIdentifier =
                getNeighborInstanceIdentifier(this.bgpIid, neighbor.getKey());
        initiatePeerInstance(neighborInstanceIdentifier, neighbor, bgpPeer);
        this.peers.put(neighborInstanceIdentifier, bgpPeer);
        LOG.debug("Peer instance created {}", bgpPeer);
    }

    private synchronized void onNeighborUpdated(final PeerBean bgpPeer, final Neighbor neighbor) {
        LOG.debug("Updating Peer instance with configuration: {}", neighbor);
        closePeer(bgpPeer);

        final InstanceIdentifier<Neighbor> neighborInstanceIdentifier = getNeighborInstanceIdentifier(this.bgpIid,
                neighbor.getKey());
        initiatePeerInstance(neighborInstanceIdentifier, neighbor, bgpPeer);
        LOG.debug("Peer instance updated {}", bgpPeer);
    }

    private void closePeer(final PeerBean bgpPeer) {
        if (bgpPeer != null) {
            try {
                bgpPeer.closeServiceInstance().get();
                bgpPeer.close();
                LOG.debug("Peer instance closed {}", bgpPeer);
            } catch (final Exception e) {
                LOG.error("Peer instance failed to close service instance", e);
            }
        }
    }

    private synchronized void onNeighborRemoved(final Neighbor neighbor) {
        LOG.debug("Removing Peer instance: {}", neighbor);
        final PeerBean bgpPeer = this.peers.remove(getNeighborInstanceIdentifier(this.bgpIid, neighbor.getKey()));
        closePeer(bgpPeer);
    }

    private synchronized void registerPeerInstance(final BgpPeer bgpPeer, final String peerInstanceName) {
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(InstanceType.PEER.getBeanName(), peerInstanceName);
        final ServiceRegistration<?> serviceRegistration = this.bundleContext
                .registerService(InstanceType.PEER.getServices(), bgpPeer, properties);
        bgpPeer.setServiceRegistration(serviceRegistration);
    }

    private synchronized void registerAppPeerInstance(final AppPeer appPeer, final String peerInstanceName) {
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(InstanceType.PEER.getBeanName(), peerInstanceName);
        final ServiceRegistration<?> serviceRegistration = this.bundleContext
                .registerService(InstanceType.APP_PEER.getServices(), appPeer, properties);
        appPeer.setServiceRegistration(serviceRegistration);
    }

    private synchronized void initiatePeerInstance(final InstanceIdentifier<Neighbor> neighborIdentifier,
            final Neighbor neighbor, final PeerBean bgpPeer) {
        final String peerInstanceName = getNeighborInstanceName(neighborIdentifier);
        if (this.ribImpl != null) {
            bgpPeer.start(this.ribImpl, neighbor, this.tableTypeRegistry);
            if (bgpPeer instanceof BgpPeer) {
                registerPeerInstance((BgpPeer) bgpPeer, peerInstanceName);
            } else if (bgpPeer instanceof AppPeer) {
                registerAppPeerInstance((AppPeer) bgpPeer, peerInstanceName);
            }
        }
        if (this.instantiated.get()) {
            bgpPeer.instantiateServiceInstance();
        }
    }
}
