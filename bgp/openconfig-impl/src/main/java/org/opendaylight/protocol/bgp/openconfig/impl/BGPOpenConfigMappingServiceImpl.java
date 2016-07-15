/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.APPLICATION_PEER_GROUP_NAME;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.BgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.NeighborsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroups;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroupsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

public final class BGPOpenConfigMappingServiceImpl implements BGPOpenConfigMappingService {

    private static final PeerGroup APP_PEER_GROUP = new PeerGroupBuilder().setPeerGroupName(APPLICATION_PEER_GROUP_NAME)
            .setKey(new PeerGroupKey(APPLICATION_PEER_GROUP_NAME)).build();
    private static final PeerGroups PEER_GROUPS = new PeerGroupsBuilder().setPeerGroup(Collections.singletonList(APP_PEER_GROUP)).build();

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
            final AfiSafi1 afiSafi1 = afiSafi.getAugmentation(AfiSafi1.class);
            final Optional<BgpTableType> bgpTableType = OpenConfigUtil.toBgpTableType(afiSafi.getAfiSafiName());
            if (afiSafi1 != null && bgpTableType.isPresent()) {
                final Short sendMax = afiSafi1.getSendMax();
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
            final AfiSafi2 afiSafi2 = afiSafi.getAugmentation(AfiSafi2.class);
            final Optional<BgpTableType> bgpTableType = OpenConfigUtil.toBgpTableType(afiSafi.getAfiSafiName());
            if (afiSafi2 != null && bgpTableType.isPresent()) {
                final AddressFamiliesBuilder builder = new AddressFamiliesBuilder(bgpTableType.get());
                builder.setSendReceive(toSendReceiveMode(afiSafi2));
                addPathCapability.add(builder.build());
            }
        }
        return addPathCapability;
    }

    private static SendReceive toSendReceiveMode(final AfiSafi2 addPath) {
        if (addPath.isReceive() && addPath.getSendMax() != null) {
            return SendReceive.Both;
        }
        if (addPath.getSendMax() != null) {
            return SendReceive.Send;
        }
        return SendReceive.Receive;
    }

    @Override
    public Protocol fromRib(final BgpId bgpId, final ClusterIdentifier clusterIdentifier, final RibId ribId,
            final AsNumber localAs, final List<BgpTableType> localTables,
            final Map<TablesKey, PathSelectionMode> pathSelectionStrategies) {
        final Bgp bgp = toGlobalConfiguration(bgpId, clusterIdentifier, localAs, localTables, pathSelectionStrategies);
        final ProtocolBuilder protocolBuilder = new ProtocolBuilder();
        protocolBuilder.setIdentifier(BGP.class);
        protocolBuilder.setName(ribId.getValue());
        protocolBuilder.setKey(new ProtocolKey(protocolBuilder.getIdentifier(), protocolBuilder.getName()));
        return protocolBuilder.addAugmentation(Protocol1.class, new Protocol1Builder().setBgp(bgp).build()).build();
    }

    private static Bgp toGlobalConfiguration(final BgpId bgpId, final ClusterIdentifier clusterIdentifier,
            final AsNumber localAs, final List<BgpTableType> localTables,
            final Map<TablesKey, PathSelectionMode> pathSelectionStrategies) {
        final BgpBuilder bgpBuilder = new BgpBuilder();
        bgpBuilder.setNeighbors(new NeighborsBuilder().build());
        bgpBuilder.setPeerGroups(PEER_GROUPS);
        final Global global = new GlobalBuilder().setAfiSafis(new AfiSafisBuilder().setAfiSafi(OpenConfigUtil.toAfiSafis(localTables,
                (afiSafi, tableType) -> OpenConfigUtil.toGlobalAfiSafiAddPath(afiSafi, tableType, pathSelectionStrategies))).build())
                .setConfig(new ConfigBuilder().setAs(localAs).setRouterId(bgpId).build()).build();
        bgpBuilder.setGlobal(global);
        return bgpBuilder.build();
    }

}
