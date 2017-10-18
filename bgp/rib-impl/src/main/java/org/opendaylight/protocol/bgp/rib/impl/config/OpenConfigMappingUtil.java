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
import com.google.common.primitives.Shorts;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.BgpCommonAfiSafiList;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.BgpNeighborAddPathsConfig;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflector;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflectorBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.transport.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.CommunityType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.RrClusterIdType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Config1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Config2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.GlobalConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.GlobalConfigAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborConfigAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class OpenConfigMappingUtil {

    static final String APPLICATION_PEER_GROUP_NAME = "application-peers";
    private static final AfiSafi IPV4_AFISAFI = new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build();
    private static final List<AfiSafi> DEFAULT_AFISAFI = ImmutableList.of(IPV4_AFISAFI);
    private static final int HOLDTIMER = 90;
    private static final int CONNECT_RETRY = 30;
    private static final PortNumber PORT = new PortNumber(179);
    private static final BigDecimal DEFAULT_KEEP_ALIVE = BigDecimal.valueOf(30);
    private static final BigDecimal DEFAULT_MINIMUM_ADV_INTERVAL = BigDecimal.valueOf(30);

    private OpenConfigMappingUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getRibInstanceName(final InstanceIdentifier<?> rootIdentifier) {
        return rootIdentifier.firstKeyOf(Protocol.class).getName();
    }

    public static int getHoldTimer(final Neighbor neighbor) {
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers.Config config =
            getTimersConfig(neighbor);
        if (config != null && config.getHoldTime() != null) {
            return config.getHoldTime().intValue();
        }
        return HOLDTIMER;
    }

    public static AsNumber getPeerAs(final Neighbor neighbor, final RIB rib) {
        if (neighbor.getConfig() != null) {
            final AsNumber peerAs = neighbor.getConfig().getPeerAs();
            if (peerAs != null) {
                return peerAs;
            }
        }
        return rib.getLocalAs();
    }

    public static boolean isActive(final Neighbor neighbor) {
        if (neighbor.getTransport() != null) {
            final Config config = neighbor.getTransport().getConfig();
            if (config != null && config.isPassiveMode() != null) {
                return !config.isPassiveMode();
            }
        }
        return true;
    }

    public static int getRetryTimer(final Neighbor neighbor) {
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers.Config config =
            getTimersConfig(neighbor);
        if (config != null && config.getConnectRetry() != null) {
            return config.getConnectRetry().intValue();
        }
        return CONNECT_RETRY;
    }

    public static KeyMapping getNeighborKey(final Neighbor neighbor) {
        if (neighbor.getConfig() != null) {
            final String authPassword = neighbor.getConfig().getAuthPassword();
            if (authPassword != null) {
                return KeyMapping.getKeyMapping(INSTANCE.inetAddressFor(neighbor.getNeighborAddress()), authPassword);
            }
        }
        return null;
    }

    public static InstanceIdentifier<Neighbor> getNeighborInstanceIdentifier(final InstanceIdentifier<Bgp> rootIdentifier,
        final NeighborKey neighborKey) {
        return rootIdentifier.child(Neighbors.class).child(Neighbor.class, neighborKey);
    }

    public static String getNeighborInstanceName(final InstanceIdentifier<?> rootIdentifier) {
        return Ipv4Util.toStringIP(rootIdentifier.firstKeyOf(Neighbor.class).getNeighborAddress());
    }

    public static PortNumber getPort(final Neighbor neighbor) {
        if (neighbor.getTransport() != null) {
            final Config config = neighbor.getTransport().getConfig();
            if (config != null && config.getAugmentation(Config1.class) != null) {
                final PortNumber remotePort = config.getAugmentation(Config1.class).getRemotePort();
                if (remotePort != null) {
                    return remotePort;
                }
            }
        }
        return PORT;
    }

    //make sure IPv4 Unicast (RFC 4271) when required
    public static List<AfiSafi> getAfiSafiWithDefault(final BgpCommonAfiSafiList afiSAfis, final boolean setDeafultIPv4) {
        if (afiSAfis == null || afiSAfis.getAfiSafi() == null) {
            return setDeafultIPv4 ? DEFAULT_AFISAFI : Collections.emptyList();
        }
        final List<AfiSafi> afiSafi = afiSAfis.getAfiSafi();
        if (setDeafultIPv4) {
            final boolean anyMatch = afiSafi.stream().anyMatch(input -> input.getAfiSafiName().equals(IPV4UNICAST.class));
            if (!anyMatch) {
                afiSafi.add(IPV4_AFISAFI);
            }
        }
        return afiSafi;
    }

    public static ClusterIdentifier getClusterIdentifier(final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.Config globalConfig) {
        final GlobalConfigAugmentation globalConfigAugmentation = globalConfig.getAugmentation(GlobalConfigAugmentation.class);
        if (globalConfigAugmentation != null && globalConfigAugmentation.getRouteReflectorClusterId() != null) {
            return new ClusterIdentifier(globalConfigAugmentation.getRouteReflectorClusterId().getIpv4Address());
        }
        return new ClusterIdentifier(globalConfig.getRouterId());
    }

    public static SimpleRoutingPolicy getSimpleRoutingPolicy(final Neighbor neighbor) {
        if (neighbor.getConfig() != null) {
            final NeighborConfigAugmentation augmentation = neighbor.getConfig().getAugmentation(NeighborConfigAugmentation.class);
            if (augmentation != null) {
                return augmentation.getSimpleRoutingPolicy();
            }
        }
        return null;
    }

    private static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers.Config getTimersConfig(final Neighbor neighbor) {
        final Timers timers = neighbor.getTimers();
        return timers != null ? timers.getConfig() : null;
    }

    public static Map<BgpTableType, PathSelectionMode> toPathSelectionMode(final List<AfiSafi> afiSafis, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        final Map<BgpTableType, PathSelectionMode> pathSelectionModes = new HashMap<>();
        for (final AfiSafi afiSafi : afiSafis) {
            final BgpNeighborAddPathsConfig afiSafi2 = afiSafi.getAugmentation(AfiSafi2.class);
            if (afiSafi2 != null) {
                final Optional<BgpTableType> bgpTableType = tableTypeRegistry.getTableType(afiSafi.getAfiSafiName());
                if (bgpTableType.isPresent()) {
                    final Short sendMax = afiSafi2.getSendMax();
                    final PathSelectionMode selectionMode;
                    if (sendMax > 1) {
                        selectionMode = new AddPathBestNPathSelection(sendMax.longValue());
                    } else {
                        selectionMode = new AllPathSelection();
                    }
                    pathSelectionModes.put(bgpTableType.get(), selectionMode);
                }
            }
        }
        return pathSelectionModes;
    }

    public static boolean isApplicationPeer(final Neighbor neighbor) {
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config config = neighbor.getConfig();
        if (config != null) {
            final Config2 config1 = config.getAugmentation(Config2.class);
            if (config1 != null) {
                final String peerGroup = config1.getPeerGroup();
                return peerGroup != null && peerGroup.equals(APPLICATION_PEER_GROUP_NAME);
            }
        }
        return false;
    }

    public static List<AddressFamilies> toAddPathCapability(final List<AfiSafi> afiSafis, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        final List<AddressFamilies> addPathCapability = new ArrayList<>();
        for (final AfiSafi afiSafi : afiSafis) {
            final BgpNeighborAddPathsConfig afiSafi1 = afiSafi.getAugmentation(AfiSafi1.class);
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

    public static Global fromRib(final BgpId bgpId, final ClusterIdentifier clusterIdentifier, final RibId ribId,
        final AsNumber localAs, final List<BgpTableType> localTables,
        final Map<TablesKey, PathSelectionMode> pathSelectionStrategies, final BGPTableTypeRegistryConsumer bgpTableTypeRegistryConsumer) {
        return toGlobalConfiguration(bgpId, clusterIdentifier, localAs, localTables, pathSelectionStrategies, bgpTableTypeRegistryConsumer);
    }

    private static Global toGlobalConfiguration(final BgpId bgpId, final ClusterIdentifier clusterIdentifier,
        final AsNumber localAs, final List<BgpTableType> localTables,
        final Map<TablesKey, PathSelectionMode> pathSelectionStrategies, final BGPTableTypeRegistryConsumer bgpTableTypeRegistryConsumer) {
        final ConfigBuilder configBuilder = new ConfigBuilder();
        configBuilder.setAs(localAs);
        configBuilder.setRouterId(bgpId);
        if (clusterIdentifier != null) {
            configBuilder.addAugmentation(GlobalConfigAugmentation.class,
                new GlobalConfigAugmentationBuilder().setRouteReflectorClusterId(new RrClusterIdType(clusterIdentifier)).build());
        }
        return new GlobalBuilder().setAfiSafis(new AfiSafisBuilder().setAfiSafi(toAfiSafis(localTables,
            (afiSafi, tableType) -> toGlobalAfiSafiAddPath(afiSafi, tableType, pathSelectionStrategies), bgpTableTypeRegistryConsumer)).build())
            .setConfig(configBuilder.build()).build();
    }

    public static Neighbor fromBgpPeer(final List<AddressFamilies> addPathCapabilities,
        final List<BgpTableType> advertisedTables, final Integer holdTimer, final IpAddress ipAddress,
        final Boolean isActive, final Rfc2385Key password, final PortNumber portNumber, final Integer retryTimer,
        final AsNumber remoteAs, final PeerRole peerRole, final SimpleRoutingPolicy simpleRoutingPolicy, final BGPTableTypeRegistryConsumer bgpTableTypeRegistryConsumer) {
        final NeighborBuilder neighborBuilder = new NeighborBuilder();
        neighborBuilder.setNeighborAddress(ipAddress);
        neighborBuilder.setKey(new NeighborKey(ipAddress));
        neighborBuilder.setAfiSafis(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder().setAfiSafi(toAfiSafis(advertisedTables,
            (afiSafi, tableType) -> toNeighborAfiSafiAddPath(afiSafi, tableType, addPathCapabilities), bgpTableTypeRegistryConsumer)).build());
        neighborBuilder.setTransport(new TransportBuilder().setConfig(
            new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.transport.ConfigBuilder()
                .setPassiveMode(!isActive)
                .setMtuDiscovery(Boolean.FALSE)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Config1.class,
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Config1Builder()
                        .setRemotePort(portNumber).build())
                .build()).build());
        neighborBuilder.setConfig(
            new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder()
                .setAuthPassword(password != null ? password.getValue() : null)
                .setPeerAs(remoteAs)
                .setPeerType(toPeerType(peerRole))
                .setSendCommunity(CommunityType.NONE)
                .setRouteFlapDamping(Boolean.FALSE)
                .addAugmentation(NeighborConfigAugmentation.class, setNeighborAugmentation(simpleRoutingPolicy))
                .build());
        neighborBuilder.setTimers(new TimersBuilder().setConfig(
            new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers.ConfigBuilder()
                .setHoldTime(BigDecimal.valueOf(holdTimer))
                .setConnectRetry(BigDecimal.valueOf(retryTimer))
                .setKeepaliveInterval(DEFAULT_KEEP_ALIVE)
                .setMinimumAdvertisementInterval(DEFAULT_MINIMUM_ADV_INTERVAL)
                .build()).build());
        neighborBuilder.setRouteReflector(new RouteReflectorBuilder().setConfig(
            new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.route.reflector.ConfigBuilder()
                .setRouteReflectorClient(peerRole == PeerRole.RrClient).build()).build());
        return neighborBuilder.build();
    }


    public static Neighbor fromApplicationPeer(final ApplicationRibId applicationRibId, final BgpId bgpId) {
        final NeighborBuilder neighborBuilder = new NeighborBuilder();
        neighborBuilder.setNeighborAddress(new IpAddress(new Ipv4Address(bgpId.getValue())));
        neighborBuilder.setKey(new NeighborKey(neighborBuilder.getNeighborAddress()));
        neighborBuilder.setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder()
            .setDescription(applicationRibId.getValue())
            .addAugmentation(Config2.class, new Config2Builder().setPeerGroup(APPLICATION_PEER_GROUP_NAME).build())
            .build());

        return neighborBuilder.build();
    }

    private static NeighborConfigAugmentation setNeighborAugmentation(final SimpleRoutingPolicy simpleRoutingPolicy) {
        if (simpleRoutingPolicy != null) {
            return new NeighborConfigAugmentationBuilder().setSimpleRoutingPolicy(simpleRoutingPolicy).build();
        }
        return null;
    }

    public static PeerRole toPeerRole(final Neighbor neighbor) {
        if (isRrClient(neighbor)) {
            return PeerRole.RrClient;
        }

        if (neighbor.getConfig() != null) {
            final PeerType peerType = neighbor.getConfig().getPeerType();
            if (peerType == PeerType.EXTERNAL) {
                return PeerRole.Ebgp;
            }
        }
        return PeerRole.Ibgp;
    }

    static PeerType toPeerType(final PeerRole peerRole) {
        switch (peerRole) {
        case Ibgp:
        case RrClient:
            return PeerType.INTERNAL;
        case Ebgp:
            return PeerType.EXTERNAL;
        case Internal:
            break;
        default:
            break;
        }
        return null;
    }

    private static boolean isRrClient(final Neighbor neighbor) {
        final RouteReflector routeReflector = neighbor.getRouteReflector();
        if (routeReflector != null && routeReflector.getConfig() != null) {
            return routeReflector.getConfig().isRouteReflectorClient();
        }
        return false;
    }

    static List<AfiSafi> toAfiSafis(final List<BgpTableType> advertizedTables, final BiFunction<AfiSafi, BgpTableType, AfiSafi> function,
        final BGPTableTypeRegistryConsumer bgpTableTypeRegistryConsumer) {
        final List<AfiSafi> afiSafis = new ArrayList<>(advertizedTables.size());
        for (final BgpTableType tableType : advertizedTables) {
            final Optional<AfiSafi> afiSafiMaybe = toAfiSafi(new BgpTableTypeImpl(tableType.getAfi(), tableType.getSafi()), bgpTableTypeRegistryConsumer);
            if (afiSafiMaybe.isPresent()) {
                final AfiSafi afiSafi = function.apply(afiSafiMaybe.get(), tableType);
                afiSafis.add(afiSafi);
            }
        }
        return afiSafis;
    }

    static AfiSafi toGlobalAfiSafiAddPath(final AfiSafi afiSafi, final BgpTableType tableType,
        final Map<TablesKey, PathSelectionMode> multiPathTables) {
        final PathSelectionMode pathSelection = multiPathTables.get(new TablesKey(tableType.getAfi(), tableType.getSafi()));
        if (pathSelection == null) {
            return afiSafi;
        }
        final long maxPaths;
        if (pathSelection instanceof AddPathBestNPathSelection) {
            maxPaths = ((AddPathBestNPathSelection) pathSelection).getNBestPaths();
        } else {
            maxPaths = 0L;
        }
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2 addPath = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2Builder()
            .setReceive(true)
            .setSendMax(Shorts.checkedCast(maxPaths))
            .build();
        return new AfiSafiBuilder(afiSafi)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2.class,
                addPath).build();
    }

    static AfiSafi toNeighborAfiSafiAddPath(final AfiSafi afiSafi, final BgpTableType tableType, final List<AddressFamilies> capabilities) {
        final Optional<AddressFamilies> capability = capabilities.stream()
            .filter(af -> af.getAfi().equals(tableType.getAfi()) && af.getSafi().equals(tableType.getSafi()))
            .findFirst();
        if (!capability.isPresent()) {
            return afiSafi;
        }
        return new AfiSafiBuilder(afiSafi)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1.class,
                fromSendReceiveMode(capability.get().getSendReceive())).build();
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1 fromSendReceiveMode(final SendReceive mode) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1Builder builder =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1Builder();
        switch (mode) {
        case Both:
            builder.setReceive(true).setSendMax((short) 0);
            break;
        case Receive:
            builder.setReceive(true);
            break;
        case Send:
            builder.setReceive(false).setSendMax((short) 0);
            break;
        default:
            break;
        }
        return builder.build();
    }

    public static Optional<AfiSafi> toAfiSafi(final BgpTableType tableType, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        final Optional<Class<? extends AfiSafiType>> afiSafi = tableTypeRegistry.getAfiSafiType(tableType);
        if (afiSafi.isPresent()) {
            return Optional.of(new AfiSafiBuilder().setAfiSafiName(afiSafi.get()).build());
        }
        return Optional.empty();
    }

    public static List<BgpTableType> toTableTypes(final List<AfiSafi> afiSafis, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        return afiSafis.stream()
            .map(afiSafi -> tableTypeRegistry.getTableType(afiSafi.getAfiSafiName()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    public static Set<TablesKey> toTableKey(final List<AfiSafi> afiSafis, final BGPTableTypeRegistryConsumer
        tableTypeRegistry) {
        return afiSafis.stream()
            .map(afiSafi -> tableTypeRegistry.getTableKey(afiSafi.getAfiSafiName()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }
}
