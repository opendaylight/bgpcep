/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.APPLICATION_PEER_GROUP_NAME;
import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.toPeerType;

import com.google.common.base.Optional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.BgpNeighborAddPathsConfig;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflectorBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.CommunityType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.RrClusterIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalConfigAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborConfigAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;

public final class BGPOpenConfigMappingServiceImpl implements BGPOpenConfigMappingService {

    private static final BigDecimal DEFAULT_KEEP_ALIVE = BigDecimal.valueOf(30);
    private static final BigDecimal DEFAULT_MINIMUM_ADV_INTERVAL = BigDecimal.valueOf(30);

    @Override
    public List<BgpTableType> toTableTypes(final List<AfiSafi> afiSafis) {
        return afiSafis.stream()
                .map(afiSafi -> OpenConfigUtil.toBgpTableType(afiSafi.getAfiSafiName()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public Map<BgpTableType, PathSelectionMode> toPathSelectionMode(final List<AfiSafi> afiSafis) {
        final Map<BgpTableType, PathSelectionMode> pathSelectionModes = new HashMap<>();
        for (final AfiSafi afiSafi : afiSafis) {
            final BgpNeighborAddPathsConfig afiSafi2 = afiSafi.getAugmentation(AfiSafi2.class);
            final Optional<BgpTableType> bgpTableType = OpenConfigUtil.toBgpTableType(afiSafi.getAfiSafiName());
            if (afiSafi2 != null && bgpTableType.isPresent()) {
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
        return pathSelectionModes;
    }

    @Override
    public boolean isApplicationPeer(final Neighbor neighbor) {
        return OpenConfigUtil.isAppNeighbor(neighbor);
    }

    @Override
    public PeerRole toPeerRole(final Neighbor neighbor) {
        return OpenConfigUtil.toPeerRole(neighbor);
    }

    @Override
    public List<AddressFamilies> toAddPathCapability(final List<AfiSafi> afiSafis) {
        final List<AddressFamilies> addPathCapability = new ArrayList<>();
        for (final AfiSafi afiSafi : afiSafis) {
            final BgpNeighborAddPathsConfig afiSafi1 = afiSafi.getAugmentation(AfiSafi1.class);
            final Optional<BgpTableType> bgpTableType = OpenConfigUtil.toBgpTableType(afiSafi.getAfiSafiName());
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

    @Override
    public Global fromRib(final BgpId bgpId, final ClusterIdentifier clusterIdentifier, final RibId ribId,
            final AsNumber localAs, final List<BgpTableType> localTables,
            final Map<TablesKey, PathSelectionMode> pathSelectionStrategies) {
        return toGlobalConfiguration(bgpId, clusterIdentifier, localAs, localTables, pathSelectionStrategies);
    }

    private static Global toGlobalConfiguration(final BgpId bgpId, final ClusterIdentifier clusterIdentifier,
            final AsNumber localAs, final List<BgpTableType> localTables,
            final Map<TablesKey, PathSelectionMode> pathSelectionStrategies) {
        final ConfigBuilder configBuilder = new ConfigBuilder();
        configBuilder.setAs(localAs);
        configBuilder.setRouterId(bgpId);
        if (clusterIdentifier != null) {
            configBuilder.addAugmentation(GlobalConfigAugmentation.class,
                    new GlobalConfigAugmentationBuilder().setRouteReflectorClusterId(new RrClusterIdType(clusterIdentifier)).build());
        }
        return new GlobalBuilder().setAfiSafis(new AfiSafisBuilder().setAfiSafi(OpenConfigUtil.toAfiSafis(localTables,
                (afiSafi, tableType) -> OpenConfigUtil.toGlobalAfiSafiAddPath(afiSafi, tableType, pathSelectionStrategies))).build())
                .setConfig(configBuilder.build()).build();
    }

    @Override
    public Neighbor fromBgpPeer(final List<AddressFamilies> addPathCapabilities,
            final List<BgpTableType> advertisedTables, final Integer holdTimer, final IpAddress ipAddress,
            final Boolean isActive, final Rfc2385Key password, final PortNumber portNumber, final Integer retryTimer,
            final AsNumber remoteAs, final PeerRole peerRole, final SimpleRoutingPolicy simpleRoutingPolicy) {
        final NeighborBuilder neighborBuilder = new NeighborBuilder();
        neighborBuilder.setNeighborAddress(ipAddress);
        neighborBuilder.setKey(new NeighborKey(ipAddress));
        neighborBuilder.setAfiSafis(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder().setAfiSafi(OpenConfigUtil.toAfiSafis(advertisedTables,
                (afiSafi, tableType) -> OpenConfigUtil.toNeighborAfiSafiAddPath(afiSafi, tableType, addPathCapabilities))).build());
        neighborBuilder.setTransport(new TransportBuilder().setConfig(
                new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.transport.ConfigBuilder()
                .setPassiveMode(!isActive)
                .setMtuDiscovery(Boolean.FALSE)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1.class,
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1Builder()
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

    @Override
    public Neighbor fromApplicationPeer(final ApplicationRibId applicationRibId, final BgpId bgpId) {
        final NeighborBuilder neighborBuilder = new NeighborBuilder();
        neighborBuilder.setNeighborAddress(new IpAddress(new Ipv4Address(bgpId.getValue())));
        neighborBuilder.setKey(new NeighborKey(neighborBuilder.getNeighborAddress()));
        neighborBuilder.setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder()
        .setDescription(applicationRibId.getValue())
        .addAugmentation(Config2.class, new Config2Builder().setPeerGroup(APPLICATION_PEER_GROUP_NAME).build())
        .build());

        return neighborBuilder.build();
    }

    private static final NeighborConfigAugmentation setNeighborAugmentation(final SimpleRoutingPolicy simpleRoutingPolicy) {
        if (simpleRoutingPolicy != null) {
            return new NeighborConfigAugmentationBuilder().setSimpleRoutingPolicy(simpleRoutingPolicy).build();
        }
        return null;
    }

}
