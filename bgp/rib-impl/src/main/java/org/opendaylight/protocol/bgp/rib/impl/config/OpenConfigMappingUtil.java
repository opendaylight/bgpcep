/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil.INSTANCE;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandlingSupport;
import org.opendaylight.protocol.bgp.parser.spi.pojo.RevisedErrorHandlingSupportImpl;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.BgpCommonAfiSafiList;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.BgpNeighborAddPathsConfig;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.BgpNeighborGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.BgpNeighborTransportConfig;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.GracefulRestart;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ErrorHandling;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflector;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.transport.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.GlobalAddPathsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.GlobalConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborAddPathsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborPeerGroupConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborTransportConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.PeerGroupTransportConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.TransportConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;

final class OpenConfigMappingUtil {
    static final String APPLICATION_PEER_GROUP_NAME = "application-peers";
    static final Optional<String> APPLICATION_PEER_GROUP_NAME_OPT = Optional.of(APPLICATION_PEER_GROUP_NAME);
    static final int HOLDTIMER = 90;
    private static final AfiSafi IPV4_AFISAFI = new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build();
    private static final List<AfiSafi> DEFAULT_AFISAFI = ImmutableList.of(IPV4_AFISAFI);
    private static final int CONNECT_RETRY = 30;
    private static final PortNumber PORT = new PortNumber(Uint16.valueOf(179).intern());

    private OpenConfigMappingUtil() {
        // Hidden on purpose
    }

    static String getRibInstanceName(final InstanceIdentifier<?> rootIdentifier) {
        return rootIdentifier.firstKeyOf(Protocol.class).getName();
    }

    static KeyMapping getNeighborKey(final Neighbor neighbor) {
        if (neighbor.getConfig() != null) {
            final String authPassword = neighbor.getConfig().getAuthPassword();
            if (authPassword != null) {
                return KeyMapping.getKeyMapping(INSTANCE.inetAddressFor(neighbor.getNeighborAddress()), authPassword);
            }
        }
        return null;
    }

    static InstanceIdentifier<Neighbor> getNeighborInstanceIdentifier(
            final InstanceIdentifier<Bgp> rootIdentifier,
            final NeighborKey neighborKey) {
        return rootIdentifier.child(Neighbors.class).child(Neighbor.class, neighborKey);
    }

    static String getNeighborInstanceName(final InstanceIdentifier<?> rootIdentifier) {
        return Ipv4Util.toStringIP(rootIdentifier.firstKeyOf(Neighbor.class).getNeighborAddress());
    }

    //make sure IPv4 Unicast (RFC 4271) when required
    static List<AfiSafi> getAfiSafiWithDefault(
            final BgpCommonAfiSafiList afiSAfis, final boolean setDeafultIPv4) {
        if (afiSAfis == null || afiSAfis.getAfiSafi() == null) {
            return setDeafultIPv4 ? DEFAULT_AFISAFI : Collections.emptyList();
        }
        final List<AfiSafi> afiSafi = afiSAfis.getAfiSafi();
        if (setDeafultIPv4) {
            final boolean anyMatch = afiSafi.stream()
                    .anyMatch(input -> input.getAfiSafiName().equals(IPV4UNICAST.class));
            if (!anyMatch) {
                final List<AfiSafi> newAfiSafi = new ArrayList<>(afiSafi.size() + 1);
                newAfiSafi.addAll(afiSafi);
                newAfiSafi.add(IPV4_AFISAFI);
                return newAfiSafi;
            }
        }
        return afiSafi;
    }

    static ClusterIdentifier getGlobalClusterIdentifier(final org.opendaylight.yang.gen.v1.http.openconfig.net
            .yang.bgp.rev151009.bgp.global.base.Config globalConfig) {
        final GlobalConfigAugmentation globalConfigAugmentation
                = globalConfig.augmentation(GlobalConfigAugmentation.class);
        if (globalConfigAugmentation != null && globalConfigAugmentation.getRouteReflectorClusterId() != null) {
            return new ClusterIdentifier(globalConfigAugmentation.getRouteReflectorClusterId().getIpv4Address());
        }
        return new ClusterIdentifier(globalConfig.getRouterId());
    }

    static @Nullable ClusterIdentifier getNeighborClusterIdentifier(
            final @Nullable RouteReflector routeReflector, final @Nullable PeerGroup peerGroup) {
        if (peerGroup != null) {
            final ClusterIdentifier clusteriId = extractClusterId(peerGroup.getRouteReflector());
            if (clusteriId != null) {
                return clusteriId;
            }
        }

        return extractClusterId(routeReflector);
    }

    private static ClusterIdentifier extractClusterId(final RouteReflector routeReflector) {
        if (routeReflector != null) {
            final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.route
                    .reflector.Config config = routeReflector.getConfig();
            if (config != null && config.getRouteReflectorClusterId() != null) {
                return new ClusterIdentifier(config.getRouteReflectorClusterId().getIpv4Address());
            }
        }
        return null;
    }

    static Map<BgpTableType, PathSelectionMode> toPathSelectionMode(final List<AfiSafi> afiSafis,
            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        final Map<BgpTableType, PathSelectionMode> pathSelectionModes = new HashMap<>();
        for (final AfiSafi afiSafi : afiSafis) {
            final BgpNeighborAddPathsConfig afiSafi2 = afiSafi.augmentation(GlobalAddPathsConfig.class);
            if (afiSafi2 != null) {
                final Optional<BgpTableType> bgpTableType = tableTypeRegistry.getTableType(afiSafi.getAfiSafiName());
                if (bgpTableType.isPresent()) {
                    final short sendMax = afiSafi2.getSendMax().toJava();
                    final PathSelectionMode selectionMode;
                    if (sendMax > 1) {
                        selectionMode = new AddPathBestNPathSelection(sendMax);
                    } else {
                        selectionMode = new AllPathSelection();
                    }
                    pathSelectionModes.put(bgpTableType.get(), selectionMode);
                }
            }
        }
        return pathSelectionModes;
    }

    static boolean isApplicationPeer(final Neighbor neighbor) {
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                .Config config = neighbor.getConfig();
        if (config != null) {
            final NeighborPeerGroupConfig config1 = config.augmentation(NeighborPeerGroupConfig.class);
            if (config1 != null) {
                final String peerGroup = config1.getPeerGroup();
                return peerGroup != null && peerGroup.equals(APPLICATION_PEER_GROUP_NAME);
            }
        }
        return false;
    }

    static List<AddressFamilies> toAddPathCapability(final List<AfiSafi> afiSafis,
            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        final List<AddressFamilies> addPathCapability = new ArrayList<>();
        for (final AfiSafi afiSafi : afiSafis) {
            final BgpNeighborAddPathsConfig afiSafi1 = afiSafi.augmentation(NeighborAddPathsConfig.class);
            final Optional<BgpTableType> bgpTableType = tableTypeRegistry.getTableType(afiSafi.getAfiSafiName());
            if (afiSafi1 != null && bgpTableType.isPresent()) {
                final AddressFamiliesBuilder builder = new AddressFamiliesBuilder(bgpTableType.get());
                builder.setSendReceive(toSendReceiveMode(afiSafi1));
                addPathCapability.add(builder.build());
            }
        }
        return addPathCapability;
    }

    private static SendReceive toSendReceiveMode(final BgpNeighborAddPathsConfig addPath) {
        if (addPath.isReceive() && addPath.getSendMax() != null) {
            return SendReceive.Both;
        }
        if (addPath.getSendMax() != null) {
            return SendReceive.Send;
        }
        return SendReceive.Receive;
    }

    private static boolean isRrClient(final BgpNeighborGroup neighbor) {
        final RouteReflector routeReflector = neighbor.getRouteReflector();
        if (routeReflector != null && routeReflector.getConfig() != null) {
            return routeReflector.getConfig().isRouteReflectorClient();
        }
        return false;
    }

    static List<BgpTableType> toTableTypes(final List<AfiSafi> afiSafis,
            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        return afiSafis.stream()
                .map(afiSafi -> tableTypeRegistry.getTableType(afiSafi.getAfiSafiName()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    static Set<TablesKey> toTableKey(final List<AfiSafi> afiSafis, final BGPTableTypeRegistryConsumer
            tableTypeRegistry) {
        return afiSafis.stream()
                .map(afiSafi -> tableTypeRegistry.getTableKey(afiSafi.getAfiSafiName()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    static boolean isActive(final Neighbor neighbor, final PeerGroup peerGroup) {
        Optional<Boolean> activeConnection = peerGroup == null ? Optional.empty() : isActive(peerGroup.getTransport());
        if (!activeConnection.isPresent()) {
            activeConnection = isActive(neighbor.getTransport());
        }
        return activeConnection.orElse(Boolean.TRUE);
    }

    private static Optional<Boolean> isActive(final Transport transport) {
        if (transport != null) {
            final Config config = transport.getConfig();
            if (config != null) {
                final Boolean passive = config.isPassiveMode();
                if (passive != null) {
                    return Optional.of(!passive);
                }
            }
        }
        return Optional.empty();
    }

    static PeerRole toPeerRole(final BgpNeighborGroup neighbor) {
        if (isRrClient(neighbor)) {
            return PeerRole.RrClient;
        }

        if (neighbor.getConfig() != null) {
            final PeerType peerType = neighbor.getConfig().getPeerType();
            if (peerType == PeerType.EXTERNAL) {
                return PeerRole.Ebgp;
            } else if (peerType == PeerType.INTERNAL) {
                return PeerRole.Ibgp;
            }
        }
        return null;
    }

    static @NonNull PeerRole toPeerRole(final Neighbor neighbor, final PeerGroup peerGroup) {
        PeerRole role = null;
        if (peerGroup != null) {
            role = toPeerRole(peerGroup);
        }

        if (role == null) {
            role = toPeerRole(neighbor);
        }

        if (role == null) {
            return PeerRole.Ibgp;
        }
        return role;
    }

    static int getHoldTimer(final Neighbor neighbor, final PeerGroup peerGroup) {
        Integer hold = null;
        if (peerGroup != null) {
            hold = getHoldTimer(peerGroup.getTimers());
        }

        if (hold == null) {
            hold = getHoldTimer(neighbor.getTimers());
        }

        if (hold == null) {
            return HOLDTIMER;
        }

        return hold;
    }

    private static @Nullable Integer getHoldTimer(final Timers timers) {
        if (timers == null) {
            return null;
        }
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers
                .Config config = timers.getConfig();
        if (config != null && config.getHoldTime() != null) {
            return config.getHoldTime().intValue();
        }
        return null;
    }

    static int getGracefulRestartTimer(final Neighbor neighbor, final PeerGroup peerGroup, final int holdTimer) {
        Uint16 timer = null;
        if (peerGroup != null) {
            timer = getGracefulRestartTimer(peerGroup.getGracefulRestart());
        }

        if (timer == null) {
            timer = getGracefulRestartTimer(neighbor.getGracefulRestart());
        }

        /*
         * RFC4724: "A suggested default for the Restart Time is a value less than or
         * equal to the HOLDTIME carried in the OPEN."
         */
        return timer == null ? holdTimer : timer.toJava();
    }

    private static @Nullable Uint16 getGracefulRestartTimer(final GracefulRestart gracefulRestart) {
        if (gracefulRestart != null) {
            final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.graceful
                    .restart.Config config = gracefulRestart.getConfig();
            if (config != null) {
                return config.getRestartTime();
            }
        }
        return null;
    }

    static @NonNull AsNumber getRemotePeerAs(final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009
            .bgp.neighbor.group.Config config, final PeerGroup peerGroup, final AsNumber localAs) {
        AsNumber neighborAs = null;
        if (peerGroup != null) {
            neighborAs = getRemotePeerAs(peerGroup.getConfig());
        }

        if (neighborAs == null) {
            neighborAs = getRemotePeerAs(config);
        }

        if (neighborAs == null) {
            return localAs;
        }
        return neighborAs;
    }

    private static AsNumber getRemotePeerAs(final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
            .rev151009.bgp.neighbor.group.@Nullable Config config) {
        return config == null ? null : config.getPeerAs();
    }

    static @NonNull AsNumber getLocalPeerAs(final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
            .rev151009.bgp.neighbor.group.@Nullable Config config, final @NonNull AsNumber globalAs) {
        if (config != null) {
            final AsNumber peerAs = config.getLocalAs();
            if (peerAs != null) {
                return peerAs;
            }
        }
        return globalAs;
    }

    static int getRetryTimer(final Neighbor neighbor, final PeerGroup peerGroup) {
        Integer retryTimer = null;
        if (peerGroup != null) {
            retryTimer = getRetryTimer(peerGroup.getTimers());
        }

        if (retryTimer == null) {
            retryTimer = getRetryTimer(neighbor.getTimers());
        }

        if (retryTimer == null) {
            return CONNECT_RETRY;
        }

        return retryTimer;
    }

    private static @Nullable Integer getRetryTimer(final Timers timers) {
        if (timers == null) {
            return null;
        }
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers
                .Config config = timers.getConfig();
        if (config != null && config.getConnectRetry() != null) {
            return config.getConnectRetry().intValue();
        }
        return null;
    }

    static @NonNull PortNumber getPort(final Neighbor neighbor, final PeerGroup peerGroup) {
        PortNumber port = null;
        if (peerGroup != null) {
            port = getPort(peerGroup.getTransport(), PeerGroupTransportConfig.class);
        }

        if (port == null) {
            port = getPort(neighbor.getTransport(), NeighborTransportConfig.class);
        }

        if (port == null) {
            return PORT;
        }

        return port;
    }

    private static <T extends TransportConfig & Augmentation<Config>> @Nullable PortNumber getPort(
            final @Nullable Transport transport, final Class<T> augment) {
        if (transport != null) {
            final Config config = transport.getConfig();
            if (config != null) {
                final T peerTc = config.augmentation(augment);
                if (peerTc != null) {
                    return peerTc.getRemotePort();
                }
            }
        }
        return null;
    }

    static @Nullable IpAddress getLocalAddress(@Nullable final Transport transport) {
        if (transport != null && transport.getConfig() != null) {
            final BgpNeighborTransportConfig.LocalAddress localAddress = transport.getConfig().getLocalAddress();
            if (localAddress != null) {
                return localAddress.getIpAddress();
            }
        }
        return null;
    }

    static @Nullable RevisedErrorHandlingSupport getRevisedErrorHandling(final PeerRole role,final PeerGroup peerGroup,
            final Neighbor neighbor) {
        Optional<Boolean> enabled = getRevisedErrorHandling(neighbor);
        if (!enabled.isPresent()) {
            enabled = getRevisedErrorHandling(peerGroup);
        }
        if (!enabled.orElse(Boolean.FALSE)) {
            return null;
        }
        switch (role) {
            case Ebgp:
                return RevisedErrorHandlingSupportImpl.forExternalPeer();
            case Ibgp:
            case Internal:
            case RrClient:
                return RevisedErrorHandlingSupportImpl.forInternalPeer();
            default:
                throw new IllegalStateException("Unhandled role " + role);
        }
    }

    private static Optional<Boolean> getRevisedErrorHandling(final BgpNeighborGroup group) {
        if (group == null) {
            return Optional.empty();
        }
        final ErrorHandling errorHandling = group.getErrorHandling();
        if (errorHandling == null) {
            return Optional.empty();
        }
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.error.handling
            .Config config = errorHandling.getConfig();
        return config == null ? Optional.empty() : Optional.of(config.isTreatAsWithdraw());
    }
}
