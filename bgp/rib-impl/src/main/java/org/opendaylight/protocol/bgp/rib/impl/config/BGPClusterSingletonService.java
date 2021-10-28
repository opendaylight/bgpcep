/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.APPLICATION_PEER_GROUP_NAME;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.APPLICATION_PEER_GROUP_NAME_OPT;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getNeighborInstanceIdentifier;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getRibInstanceName;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProviderRegistry;
import org.opendaylight.protocol.bgp.rib.spi.util.ClusterSingletonServiceRegistrationHelper;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborPeerGroupConfig;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public class BGPClusterSingletonService implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BGPClusterSingletonService.class);

    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);
    private final InstanceIdentifier<Bgp> bgpIid;
    @GuardedBy("this")
    private final Map<InstanceIdentifier<Neighbor>, PeerBean> peers = new HashMap<>();
    @GuardedBy("this")
    private final Map<String, List<PeerBean>> peersGroups = new HashMap<>();
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private final ServiceGroupIdentifier serviceGroupIdentifier;
    private final AtomicBoolean instantiated = new AtomicBoolean(false);
    private final PeerGroupConfigLoader peerGroupLoader;
    private final RpcProviderService rpcRegistry;
    private final RIBExtensionConsumerContext ribExtensionContext;
    private final BGPDispatcher bgpDispatcher;
    private final BGPRibRoutingPolicyFactory routingPolicyFactory;
    private final CodecsRegistry codecsRegistry;
    private final BGPStateProviderRegistry stateProviderRegistry;
    private final DOMDataBroker domDataBroker;
    @GuardedBy("this")
    private RibImpl ribImpl;


    BGPClusterSingletonService(
            final @NonNull PeerGroupConfigLoader peerGroupLoader,
            final @NonNull ClusterSingletonServiceProvider provider,
            final @NonNull BGPTableTypeRegistryConsumer tableTypeRegistry,
            final @NonNull RpcProviderService rpcRegistry,
            final @NonNull RIBExtensionConsumerContext ribExtensionContext,
            final @NonNull BGPDispatcher bgpDispatcher,
            final @NonNull BGPRibRoutingPolicyFactory routingPolicyFactory,
            final @NonNull CodecsRegistry codecsRegistry,
            final @NonNull BGPStateProviderRegistry stateProviderRegistry,
            final @NonNull DOMDataBroker domDataBroker,
            final @NonNull InstanceIdentifier<Bgp> bgpIid) {
        this.peerGroupLoader = peerGroupLoader;
        this.tableTypeRegistry = tableTypeRegistry;
        this.rpcRegistry = rpcRegistry;
        this.ribExtensionContext = ribExtensionContext;
        this.bgpDispatcher = bgpDispatcher;
        this.routingPolicyFactory = routingPolicyFactory;
        this.codecsRegistry = codecsRegistry;
        this.stateProviderRegistry = stateProviderRegistry;
        this.domDataBroker = domDataBroker;
        this.bgpIid = bgpIid;
        final String ribInstanceName = getRibInstanceName(bgpIid);
        this.serviceGroupIdentifier = ServiceGroupIdentifier.create(ribInstanceName + "-service-group");
        ClusterSingletonServiceRegistrationHelper
                .registerSingletonService(provider, this);
        LOG.info("BGPClusterSingletonService {} registered", this.serviceGroupIdentifier.getName());
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        if (this.ribImpl != null) {
            this.ribImpl.instantiateServiceInstance();
            this.peers.values().forEach(PeerBean::instantiateServiceInstance);
        }
        this.instantiated.set(true);
        LOG.info("BGPClusterSingletonService {} instantiated", this.serviceGroupIdentifier.getName());
    }

    @Override
    public synchronized ListenableFuture<? extends CommitInfo> closeServiceInstance() {
        LOG.info("BGPClusterSingletonService {} close service instance", this.serviceGroupIdentifier.getName());
        this.instantiated.set(false);

        final List<ListenableFuture<? extends CommitInfo>> futurePeerCloseList = this.peers.values().stream()
                .map(PeerBean::closeServiceInstance).collect(Collectors.toList());
        final SettableFuture<? extends CommitInfo> done = SettableFuture.create();

        final ListenableFuture<List<CommitInfo>> futureResult = Futures.allAsList(futurePeerCloseList);
        Futures.addCallback(futureResult, new FutureCallback<List<? extends CommitInfo>>() {
            @Override
            public void onSuccess(final List<? extends CommitInfo> result) {
                synchronized (BGPClusterSingletonService.this) {
                    if (BGPClusterSingletonService.this.ribImpl != null) {
                        done.setFuture(Futures.transform(BGPClusterSingletonService.this.ribImpl.closeServiceInstance(),
                            input -> null, MoreExecutors.directExecutor()));
                    } else {
                        done.setFuture(Futures.transform(CommitInfo.emptyFluentFuture(),
                            input -> null, MoreExecutors.directExecutor()));
                    }
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Failed removing peers", throwable);
            }
        }, MoreExecutors.directExecutor());
        return done;
    }

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
                    this.ribImpl = null;
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
        this.ribImpl = new RibImpl(this.ribExtensionContext, this.bgpDispatcher, this.routingPolicyFactory,
                this.codecsRegistry, this.stateProviderRegistry, this.domDataBroker);
        initiateRibInstance(global);
        LOG.debug("RIB instance created: {}", this.ribImpl);
    }

    private synchronized void onGlobalUpdated(final Global global) {
        LOG.debug("Modifying RIB instance with configuration: {}", global);
        final List<PeerBean> closedPeers = closeAllBindedPeers();
        closeRibService();
        initiateRibInstance(global);
        for (final PeerBean peer : closedPeers) {
            peer.restart(this.ribImpl, this.bgpIid, this.peerGroupLoader, this.tableTypeRegistry);
        }
        if (this.instantiated.get()) {
            closedPeers.forEach(PeerBean::instantiateServiceInstance);
        }
        LOG.debug("RIB instance created: {}", this.ribImpl);
    }

    @Holding("this")
    @VisibleForTesting
    @SuppressWarnings("checkstyle:illegalCatch")
    void closeRibService() {
        try {
            this.ribImpl.closeServiceInstance().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        } catch (final Exception e) {
            LOG.error("RIB instance failed to close service instance", e);
        }
        this.ribImpl.close();
    }

    @Holding("this")
    @VisibleForTesting
    void initiateRibInstance(final Global global) {
        final String ribInstanceName = getRibInstanceName(this.bgpIid);
        ribImpl.start(global, ribInstanceName, this.tableTypeRegistry);
        if (this.instantiated.get()) {
            this.ribImpl.instantiateServiceInstance();
        }
    }

    @SuppressWarnings("checkstyle:illegalCatch")
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

    @Override
    public synchronized void close() {
        LOG.info("BGPClusterSingletonService {} close", this.serviceGroupIdentifier.getName());
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
        final PeerBean bgpPeer = this.peers.get(getNeighborInstanceIdentifier(this.bgpIid, neighbor.key()));
        if (bgpPeer == null) {
            onNeighborCreated(neighbor);
        } else if (!bgpPeer.containsEqualConfiguration(neighbor)) {
            onNeighborUpdated(bgpPeer, neighbor);
        }
    }

    @VisibleForTesting
    synchronized void onNeighborCreated(final Neighbor neighbor) {
        LOG.debug("Creating Peer instance with configuration: {}", neighbor);
        final PeerBean bgpPeer;
        if (OpenConfigMappingUtil.isApplicationPeer(neighbor)) {
            bgpPeer = new AppPeer(this.stateProviderRegistry);
        } else {
            bgpPeer = new BgpPeer(this.rpcRegistry, this.stateProviderRegistry);
        }
        final InstanceIdentifier<Neighbor> neighborInstanceIdentifier =
                getNeighborInstanceIdentifier(this.bgpIid, neighbor.key());
        initiatePeerInstance(neighbor, bgpPeer);
        this.peers.put(neighborInstanceIdentifier, bgpPeer);

        final Optional<String> peerGroupName = getPeerGroupName(neighbor.getConfig());
        peerGroupName.ifPresent(s -> this.peersGroups.computeIfAbsent(s, k -> new ArrayList<>()).add(bgpPeer));
        LOG.debug("Peer instance created {}", neighbor.key().getNeighborAddress());
    }

    private static Optional<String> getPeerGroupName(final Config config) {
        if (config == null) {
            return Optional.empty();
        }
        final NeighborPeerGroupConfig aug = config.augmentation(NeighborPeerGroupConfig.class);
        if (aug == null || aug.getPeerGroup() == null) {
            return Optional.empty();
        }
        final String peerGroupName = aug.getPeerGroup();
        if (peerGroupName.equals(APPLICATION_PEER_GROUP_NAME)) {
            return APPLICATION_PEER_GROUP_NAME_OPT;
        }
        return Optional.of(StringUtils.substringBetween(peerGroupName, "=\"", "\""));
    }

    @VisibleForTesting
    synchronized void onNeighborUpdated(final PeerBean bgpPeer, final Neighbor neighbor) {
        LOG.info("Updating Peer instance with configuration: {}", neighbor);
        closePeer(bgpPeer);
        initiatePeerInstance(neighbor, bgpPeer);
        LOG.info("Peer instance updated {}", bgpPeer);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private static void closePeer(final PeerBean bgpPeer) {
        if (bgpPeer != null) {
            try {
                bgpPeer.closeServiceInstance().get();
                bgpPeer.close();
                LOG.info("Peer instance closed {}", bgpPeer);
            } catch (final Exception e) {
                LOG.error("Peer instance failed to close service instance", e);
            }
        }
    }

    @VisibleForTesting
    public synchronized void onNeighborRemoved(final Neighbor neighbor) {
        LOG.debug("Removing Peer instance: {}", neighbor);
        final PeerBean bgpPeer = this.peers.remove(getNeighborInstanceIdentifier(this.bgpIid, neighbor.key()));

        final Optional<String> groupName = getPeerGroupName(neighbor.getConfig());
        groupName.ifPresent(s -> this.peersGroups.computeIfPresent(s, (k, groupPeers) -> {
            groupPeers.remove(bgpPeer);
            return groupPeers.isEmpty() ? null : groupPeers;
        }));
        closePeer(bgpPeer);
    }

    @VisibleForTesting
    synchronized void initiatePeerInstance(final Neighbor neighbor, final PeerBean bgpPeer) {
        if (this.ribImpl != null) {
            bgpPeer.start(this.ribImpl, neighbor, this.bgpIid, this.peerGroupLoader, this.tableTypeRegistry);
        }
        if (this.instantiated.get()) {
            bgpPeer.instantiateServiceInstance();
        }
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    synchronized void restartNeighbors(final String peerGroupName) {
        final List<PeerBean> peerGroup = this.peersGroups.get(peerGroupName);
        if (peerGroup == null) {
            return;
        }
        for (final PeerBean peer : peerGroup) {
            try {
                peer.closeServiceInstance().get();
            } catch (final Exception e) {
                LOG.error("Peer instance failed to close service instance", e);
            }
            peer.restart(this.ribImpl, this.bgpIid, this.peerGroupLoader, this.tableTypeRegistry);
            peer.instantiateServiceInstance();
        }
    }
}
