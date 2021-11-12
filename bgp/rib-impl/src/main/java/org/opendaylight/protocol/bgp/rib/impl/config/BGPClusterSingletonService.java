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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProviderRegistry;
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
    @GuardedBy("this")
    private ClusterSingletonServiceRegistration cssRegistration;


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
        this.serviceGroupIdentifier = ServiceGroupIdentifier.create(getRibInstanceName(bgpIid) + "-service-group");
        cssRegistration = provider.registerClusterSingletonService(this);
        LOG.info("BGPClusterSingletonService {} registered", this.serviceGroupIdentifier.getName());
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        if (ribImpl != null) {
            ribImpl.instantiateServiceInstance();
            peers.values().forEach(PeerBean::instantiateServiceInstance);
        }
        instantiated.set(true);
        LOG.info("BGPClusterSingletonService {} instantiated", serviceGroupIdentifier.getName());
    }

    @Override
    public synchronized ListenableFuture<? extends CommitInfo> closeServiceInstance() {
        LOG.info("BGPClusterSingletonService {} close service instance", serviceGroupIdentifier.getName());
        instantiated.set(false);

        final List<ListenableFuture<? extends CommitInfo>> futurePeerCloseList = peers.values().stream()
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
        return serviceGroupIdentifier;
    }

    synchronized void onGlobalChanged(final DataObjectModification<Global> dataObjectModification) {
        switch (dataObjectModification.getModificationType()) {
            case DELETE:
                LOG.debug("Removing RIB instance: {}", bgpIid);
                if (ribImpl != null) {
                    LOG.debug("RIB instance removed {}", ribImpl);
                    closeBoundPeers();
                    closeRibInstance();
                    ribImpl = null;
                }
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                final Global global = dataObjectModification.getDataAfter();
                if (ribImpl == null) {
                    onGlobalCreated(global);
                } else if (!ribImpl.isGlobalEqual(Objects.requireNonNull(global))) {
                    onGlobalUpdated(global);
                }
                break;
            default:
                break;
        }
    }

    private synchronized void onGlobalCreated(final Global global) {
        LOG.debug("Creating RIB instance with configuration: {}", global);
        ribImpl = new RibImpl(ribExtensionContext, bgpDispatcher, routingPolicyFactory,
                codecsRegistry, stateProviderRegistry, domDataBroker);
        initiateRibInstance(global);
        LOG.debug("RIB instance created: {}", ribImpl);
    }

    private synchronized void onGlobalUpdated(final Global global) {
        LOG.info("Global config {} updated, new configuration {}", global.getConfig().getRouterId(), global);
        closeRibInstance();
        initiateRibInstance(global);
        restartAllPeers();
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:illegalCatch")
    synchronized void closeRibInstance() {
        try {
            ribImpl.close();
        } catch (final Exception e) {
            LOG.error("RIB instance {} failed to close", ribImpl.getBgpIdentifier(), e);
        }
    }

    @VisibleForTesting
    synchronized void initiateRibInstance(final Global global) {
        final String ribInstanceName = getRibInstanceName(bgpIid);
        ribImpl.start(global, ribInstanceName, tableTypeRegistry);
        if (instantiated.get()) {
            ribImpl.instantiateServiceInstance();
        }
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private synchronized List<PeerBean> closeBoundPeers() {
        final List<PeerBean> filtered = new ArrayList<>();
        peers.forEach((key, peer) -> {
            try {
                peer.close();
                filtered.add(peer);
            } catch (final Exception e) {
                LOG.error("Peer instance failed to close service instance", e);
            }
        });
        return filtered;
    }

    @Override
    public synchronized void close() {
        LOG.info("Closing BGPClusterSingletonService {}", serviceGroupIdentifier.getName());
        cssRegistration.close();
        closeBoundPeers();
        peers.clear();
        closeRibInstance();
        ribImpl = null;
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
        final PeerBean bgpPeer = peers.get(getNeighborInstanceIdentifier(bgpIid, neighbor.key()));
        if (bgpPeer == null) {
            onNeighborCreated(neighbor);
        } else if (!bgpPeer.containsEqualConfiguration(neighbor)) {
            onNeighborUpdated(bgpPeer, neighbor);
        }
    }

    @VisibleForTesting
    synchronized void onNeighborCreated(final Neighbor neighbor) {
        LOG.info("Creating Peer instance {} with configuration: {}", neighbor.getNeighborAddress(), neighbor);
        final PeerBean bgpPeer;
        if (OpenConfigMappingUtil.isApplicationPeer(neighbor)) {
            bgpPeer = new AppPeer(stateProviderRegistry);
        } else {
            bgpPeer = new BgpPeer(rpcRegistry, stateProviderRegistry);
        }
        final InstanceIdentifier<Neighbor> neighborInstanceIdentifier =
                getNeighborInstanceIdentifier(bgpIid, neighbor.key());
        initiatePeerInstance(neighbor, bgpPeer);
        peers.put(neighborInstanceIdentifier, bgpPeer);

        final Optional<String> peerGroupName = getPeerGroupName(neighbor.getConfig());
        peerGroupName.ifPresent(s -> peersGroups.computeIfAbsent(s, k -> new ArrayList<>()).add(bgpPeer));
        LOG.info("Peer instance created {}", neighbor.getNeighborAddress());
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
        LOG.info("Updating Peer {} with new configuration: {}",
                neighbor.getNeighborAddress(), neighbor);
        closePeer(bgpPeer);
        initiatePeerInstance(neighbor, bgpPeer);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private static boolean closePeer(final PeerBean bgpPeer) {
        if (bgpPeer != null) {
            try {
                bgpPeer.close();
                LOG.info("Peer instance {} closed", bgpPeer.getCurrentConfiguration().getNeighborAddress());
                return true;
            } catch (final Exception e) {
                LOG.error("Peer instance failed to close service instance", e);
            }
        }
        return false;
    }

    @VisibleForTesting
    public synchronized void onNeighborRemoved(final Neighbor neighbor) {
        LOG.info("Removing Peer instance: {}", neighbor.getNeighborAddress());
        final PeerBean bgpPeer = peers.remove(getNeighborInstanceIdentifier(bgpIid, neighbor.key()));

        final Optional<String> groupName = getPeerGroupName(neighbor.getConfig());
        groupName.ifPresent(s -> peersGroups.computeIfPresent(s, (k, groupPeers) -> {
            groupPeers.remove(bgpPeer);
            return groupPeers.isEmpty() ? null : groupPeers;
        }));
        closePeer(bgpPeer);
    }

    @VisibleForTesting
    synchronized void initiatePeerInstance(final Neighbor neighbor, final PeerBean bgpPeer) {
        if (ribImpl != null) {
            bgpPeer.start(ribImpl, neighbor, bgpIid, peerGroupLoader, tableTypeRegistry);
        }
        if (instantiated.get()) {
            bgpPeer.instantiateServiceInstance();
        }
    }

    private synchronized void restartPeers(final @Nullable Collection<PeerBean> toRestart) {
        if (toRestart == null) {
            return;
        }
        toRestart.stream().filter(BGPClusterSingletonService::closePeer)
                .forEach(peer -> initiatePeerInstance(peer.getCurrentConfiguration(), peer));
    }

    private synchronized void restartAllPeers() {
        restartPeers(peers.values());
    }

    synchronized void restartPeerGroup(final String peerGroupName) {
        restartPeers(peersGroups.get(peerGroupName));
    }
}
