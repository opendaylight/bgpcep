/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiprotocolCapabilitiesUtil;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProviderRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafis;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.AddPathCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborPeerGroupConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpPeerBean extends PeerBean {
    private static final Logger LOG = LoggerFactory.getLogger(BgpPeerBean.class);

    private final RpcProviderService rpcRegistry;
    private final BGPStateProviderRegistry stateProviderRegistry;

    @GuardedBy("this")
    private Neighbor currentConfiguration;
    @GuardedBy("this")
    private BgpPeerSingletonService bgpPeerSingletonService;
    @GuardedBy("this")
    private Registration stateProviderRegistration;

    public BgpPeerBean(final RpcProviderService rpcRegistry, final BGPStateProviderRegistry stateProviderRegistry) {
        this.rpcRegistry = requireNonNull(rpcRegistry);
        this.stateProviderRegistry = requireNonNull(stateProviderRegistry);
    }

    private static List<OptionalCapabilities> getBgpCapabilities(final AfiSafis afiSafis, final RIB rib,
                                                          final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        final var caps = new ArrayList<OptionalCapabilities>();
        caps.add(new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder()
                .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(rib.getLocalAs()).build())
                .build())
            .build());
        caps.add(new OptionalCapabilitiesBuilder()
            .setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY)
            .build());
        caps.add(new OptionalCapabilitiesBuilder().setCParameters(MultiprotocolCapabilitiesUtil.RR_CAPABILITY).build());

        final var afiSafi = OpenConfigMappingUtil.getAfiSafiWithDefault(afiSafis, false).values();
        final var addPathCapability = OpenConfigMappingUtil.toAddPathCapability(afiSafi, tableTypeRegistry);
        if (!addPathCapability.isEmpty()) {
            caps.add(new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder()
                    .addAugmentation(new CParameters1Builder()
                        .setAddPathCapability(new AddPathCapabilityBuilder()
                            .setAddressFamilies(addPathCapability)
                            .build())
                        .build())
                    .build())
                .build());
        }

        final var tableTypes = OpenConfigMappingUtil.toTableTypes(afiSafi, tableTypeRegistry);
        for (var tableType : tableTypes) {
            if (!rib.getLocalTables().contains(tableType)) {
                LOG.info("RIB instance does not list {} in its local tables. Incoming data will be dropped.",
                    tableType);
            }

            caps.add(new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder()
                    .addAugmentation(new CParameters1Builder()
                        .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder(tableType).build())
                        .build())
                    .build())
                .build());
        }
        return caps;
    }

    @Override
    synchronized void start(final RIB rib, final Neighbor neighbor, final DataObjectIdentifier<Bgp> bgpIid,
            final PeerGroupConfigLoader peerGroupLoader, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        checkState(bgpPeerSingletonService == null, "Previous peer instance was not closed.");
        LOG.info("Starting BgPeer instance {}", neighbor.getNeighborAddress());
        bgpPeerSingletonService = new BgpPeerSingletonService(rib, neighbor, bgpIid, peerGroupLoader,
                tableTypeRegistry);
        currentConfiguration = neighbor;
        stateProviderRegistration = stateProviderRegistry.register(this);
    }

    @Override
    synchronized ListenableFuture<?> stop() {
        if (bgpPeerSingletonService == null) {
            LOG.info("BGP Peer {} already closed, skipping", currentConfiguration.getNeighborAddress());
            return Futures.immediateVoidFuture();
        }
        LOG.info("Closing BGP Peer {}", currentConfiguration.getNeighborAddress());
        if (stateProviderRegistration != null) {
            stateProviderRegistration.close();
            stateProviderRegistration = null;
        }

        final var future = bgpPeerSingletonService.closeServiceInstance();
        bgpPeerSingletonService = null;
        return future;
    }

    @Override
    synchronized void instantiateServiceInstance() {
        if (bgpPeerSingletonService != null) {
            bgpPeerSingletonService.instantiateServiceInstance();
        }
    }

    @Override
    synchronized ListenableFuture<?> closeServiceInstance() {
        return bgpPeerSingletonService != null ? bgpPeerSingletonService.closeServiceInstance()
            : Futures.immediateVoidFuture();
    }

    @Override
    synchronized boolean containsEqualConfiguration(final Neighbor neighbor) {
        if (currentConfiguration == null) {
            return false;
        }
        final var actAfiSafi = currentConfiguration.getAfiSafis();
        final var extAfiSafi = neighbor.getAfiSafis();
        final var actualSafi = actAfiSafi != null ? actAfiSafi.nonnullAfiSafi().values() : List.of();
        final var extSafi = extAfiSafi != null ? extAfiSafi.nonnullAfiSafi().values() : List.of();
        return actualSafi.containsAll(extSafi) && extSafi.containsAll(actualSafi)
            && Objects.equals(currentConfiguration.getConfig(), neighbor.getConfig())
            && Objects.equals(currentConfiguration.getNeighborAddress(), neighbor.getNeighborAddress())
            && Objects.equals(currentConfiguration.getAddPaths(), neighbor.getAddPaths())
            && Objects.equals(currentConfiguration.getApplyPolicy(), neighbor.getApplyPolicy())
            && Objects.equals(currentConfiguration.getAsPathOptions(), neighbor.getAsPathOptions())
            && Objects.equals(currentConfiguration.getEbgpMultihop(), neighbor.getEbgpMultihop())
            && Objects.equals(currentConfiguration.getGracefulRestart(), neighbor.getGracefulRestart())
            && Objects.equals(currentConfiguration.getErrorHandling(), neighbor.getErrorHandling())
            && Objects.equals(currentConfiguration.getLoggingOptions(), neighbor.getLoggingOptions())
            && Objects.equals(currentConfiguration.getRouteReflector(), neighbor.getRouteReflector())
            && Objects.equals(currentConfiguration.getState(), neighbor.getState())
            && Objects.equals(currentConfiguration.getTimers(), neighbor.getTimers())
            && Objects.equals(currentConfiguration.getTransport(), neighbor.getTransport());
    }

    @Override
    synchronized Neighbor getCurrentConfiguration() {
        return currentConfiguration;
    }

    @Override
    public synchronized BGPPeerState getPeerState() {
        return bgpPeerSingletonService == null ? null : bgpPeerSingletonService.getPeerState();
    }

    synchronized void removePeer(final BGPPeerRegistry bgpPeerRegistry) {
        if (currentConfiguration != null) {
            bgpPeerRegistry.removePeer(OpenConfigMappingUtil.convertIpAddress(
                currentConfiguration.getNeighborAddress()));
        }
    }

    private final class BgpPeerSingletonService implements BGPPeerStateProvider {
        private final boolean activeConnection;
        private final BGPDispatcher dispatcher;
        private final InetSocketAddress inetAddress;
        private final int retryTimer;
        private final KeyMapping keys;
        private final InetSocketAddress localAddress;
        private final BGPPeer bgpPeer;
        private final IpAddressNoZone neighborAddress;
        private final BGPSessionPreferences prefs;
        private Future<Void> connection;
        private boolean isServiceInstantiated;
        private final List<OptionalCapabilities> finalCapabilities;
        private final int gracefulRestartTimer;

        BgpPeerSingletonService(final RIB rib, final Neighbor neighbor, final DataObjectIdentifier<Bgp> bgpIid,
                final PeerGroupConfigLoader peerGroupLoader, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
            neighborAddress = OpenConfigMappingUtil.convertIpAddress(neighbor.getNeighborAddress());

            PeerGroup peerGroup = null;
            String peerGroupName = null;
            final var neighborConfig = neighbor.getConfig();
            if (neighborConfig != null) {
                final var pgConfig = neighborConfig.augmentation(NeighborPeerGroupConfig.class);
                if (pgConfig != null) {
                    peerGroupName = StringUtils.substringBetween(pgConfig.getPeerGroup(), "=\"", "\"");
                    peerGroup = peerGroupLoader.getPeerGroup(bgpIid, peerGroupName);
                }
            }
            final AfiSafis afisSafis;
            if (peerGroup != null && peerGroup.getAfiSafis() != null) {
                afisSafis = peerGroup.getAfiSafis();
            } else {
                afisSafis = requireNonNull(neighbor.getAfiSafis(), "Missing mandatory AFIs/SAFIs");
            }
            finalCapabilities = getBgpCapabilities(afisSafis, rib, tableTypeRegistry);

            final var afiSafisAdvertized = OpenConfigMappingUtil.toTableKey(afisSafis.getAfiSafi(), tableTypeRegistry);
            final var role = OpenConfigMappingUtil.toPeerRole(neighbor, peerGroup);

            final var clusterId = OpenConfigMappingUtil.getNeighborClusterIdentifier(neighbor.getRouteReflector(),
                peerGroup);
            final int hold = OpenConfigMappingUtil.getHoldTimer(neighbor, peerGroup);
            gracefulRestartTimer = OpenConfigMappingUtil.getGracefulRestartTimer(neighbor, peerGroup, hold);
            final var gracefulTables = GracefulRestartUtil.getGracefulTables(afisSafis.nonnullAfiSafi().values(),
                tableTypeRegistry);
            final var llGracefulTimers = GracefulRestartUtil.getLlGracefulTimers(afisSafis.nonnullAfiSafi().values(),
                tableTypeRegistry);
            final var keyMapping = OpenConfigMappingUtil.getNeighborKey(neighbor);
            final var neighborLocalAddress = OpenConfigMappingUtil.getLocalAddress(neighbor.getTransport());
            final var globalAs = rib.getLocalAs();
            final var neighborRemoteAs = OpenConfigMappingUtil.getRemotePeerAs(neighborConfig, peerGroup, globalAs);
            final AsNumber neighborLocalAs;
            if (role == PeerRole.Ebgp) {
                neighborLocalAs = OpenConfigMappingUtil.getLocalPeerAs(neighborConfig, globalAs);
            } else {
                neighborLocalAs = globalAs;
            }

            bgpPeer = new BGPPeer(tableTypeRegistry, neighborAddress, peerGroupName, rib, role, clusterId,
                    neighborLocalAs, rpcRegistry, afiSafisAdvertized, gracefulTables, llGracefulTimers,
                    OpenConfigMappingUtil.getTreatAsWithdraw(peerGroup, neighbor),
                    BgpPeerBean.this);
            prefs = new BGPSessionPreferences(neighborLocalAs, hold, rib.getBgpIdentifier(),
                    neighborRemoteAs, getInitialBgpParameters(gracefulTables, llGracefulTimers),
                    keyMapping == null ? Optional.empty()
                        : Optional.of(Iterables.getOnlyElement(keyMapping.asMap().values())));
            activeConnection = OpenConfigMappingUtil.isActive(neighbor, peerGroup);
            retryTimer = OpenConfigMappingUtil.getRetryTimer(neighbor, peerGroup);
            dispatcher = rib.getDispatcher();

            final var port = OpenConfigMappingUtil.getPort(neighbor, peerGroup);
            inetAddress = Ipv4Util.toInetSocketAddress(neighborAddress, port);
            if (neighborLocalAddress != null) {
                localAddress = Ipv4Util.toInetSocketAddress(neighborLocalAddress, port);
            } else {
                localAddress = null;
            }
            keys = keyMapping;
            LOG.info("New BGP Peer {}:{} AS {} instance for BGP id {} created with activeConnection: {}",
                    inetAddress, localAddress, neighborRemoteAs, prefs.getBgpId(), activeConnection);
        }

        private List<BgpParameters> getInitialBgpParameters(final Set<TablesKey> gracefulTables,
                                                            final Map<TablesKey, Uint24> llGracefulTimers) {
            return Collections.singletonList(
                    GracefulRestartUtil.getGracefulBgpParameters(finalCapabilities, gracefulTables, Set.of(),
                        gracefulRestartTimer, false, llGracefulTimers, unused -> false));
        }

        synchronized void instantiateServiceInstance() {
            if (isServiceInstantiated) {
                LOG.warn("Peer {} has already been instantiated", neighborAddress);
                return;
            }
            isServiceInstantiated = true;
            LOG.info("Peer instantiated {}", neighborAddress);
            bgpPeer.instantiateServiceInstance();
            dispatcher.getBGPPeerRegistry().addPeer(neighborAddress, bgpPeer, prefs);
            if (activeConnection) {
                connection = dispatcher.createReconnectingClient(inetAddress, localAddress, retryTimer, keys);
            }
        }

        synchronized ListenableFuture<?> closeServiceInstance() {
            if (!isServiceInstantiated) {
                LOG.info("Peer {} already closed", neighborAddress);
                return Futures.immediateVoidFuture();
            }
            LOG.info("Close Peer {}", neighborAddress);
            isServiceInstantiated = false;
            if (connection != null) {
                connection.cancel(true);
                connection = null;
            }
            final var future = bgpPeer.close();
            removePeer(dispatcher.getBGPPeerRegistry());
            return future;
        }

        @Override
        public BGPPeerState getPeerState() {
            return bgpPeer.getPeerState();
        }
    }

    public synchronized List<OptionalCapabilities> getBgpFixedCapabilities() {
        return bgpPeerSingletonService.finalCapabilities;
    }

    public synchronized int getGracefulRestartTimer() {
        return bgpPeerSingletonService.gracefulRestartTimer;
    }
}
