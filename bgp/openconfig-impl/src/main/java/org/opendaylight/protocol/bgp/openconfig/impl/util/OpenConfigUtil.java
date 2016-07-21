/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.util;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Shorts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.Config1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflector;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L2VPNEVPN;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV6UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.IPV4FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.IPV4L3VPNFLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.IPV6FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.IPV6L3VPNFLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.LINKSTATE;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public final class OpenConfigUtil {

    public static final String APPLICATION_PEER_GROUP_NAME = "application-peers";
    private static final BiMap<BgpTableType, Class<? extends AfiSafiType>> TABLETYPE_TO_AFISAFI;

    //TODO mapping should be provided by extensions themself
    static {
        final ImmutableBiMap.Builder<BgpTableType, Class<? extends AfiSafiType>> b = ImmutableBiMap.builder();
        b.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class), IPV4UNICAST.class);
        b.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class), IPV6UNICAST.class);
        b.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class), IPV4LABELLEDUNICAST.class);
        b.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class), IPV6LABELLEDUNICAST.class);
        b.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class), L3VPNIPV4UNICAST.class);
        b.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class), L3VPNIPV6UNICAST.class);
        b.put(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class), LINKSTATE.class);
        b.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class), IPV4FLOW.class);
        b.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class), IPV6FLOW.class);
        b.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, FlowspecL3vpnSubsequentAddressFamily.class), IPV4L3VPNFLOW.class);
        b.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, FlowspecL3vpnSubsequentAddressFamily.class), IPV6L3VPNFLOW.class);
        b.put(new BgpTableTypeImpl(L2vpnAddressFamily.class, EvpnSubsequentAddressFamily.class), L2VPNEVPN.class);
        TABLETYPE_TO_AFISAFI = b.build();
    }

    private OpenConfigUtil() {
        throw new UnsupportedOperationException();
    }

    public static Optional<AfiSafi> toAfiSafi(final BgpTableType tableType) {
        final Class<? extends AfiSafiType> afiSafi = TABLETYPE_TO_AFISAFI.get(tableType);
        if (afiSafi != null) {
            return Optional.of(new AfiSafiBuilder().setAfiSafiName(afiSafi).build());
        }
        return Optional.absent();
    }

    public static Optional<BgpTableType> toBgpTableType(final Class<? extends AfiSafiType> afiSafi) {
        return Optional.fromNullable(TABLETYPE_TO_AFISAFI.inverse().get(afiSafi));
    }

    public static List<AfiSafi> toAfiSafis(final List<BgpTableType> advertizedTables, final BiFunction<AfiSafi, BgpTableType, AfiSafi> function) {
        final List<AfiSafi> afiSafis = new ArrayList<>(advertizedTables.size());
        for (final BgpTableType tableType : advertizedTables) {
            final Optional<AfiSafi> afiSafiMaybe = toAfiSafi(new BgpTableTypeImpl(tableType.getAfi(), tableType.getSafi()));
            if (afiSafiMaybe.isPresent()) {
                final AfiSafi afiSafi = function.apply(afiSafiMaybe.get(), tableType);
                afiSafis.add(afiSafi);
            }
        }
        return afiSafis;
    }

    public static AfiSafi toGlobalAfiSafiAddPath(final AfiSafi afiSafi, final BgpTableType tableType,
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
        final AfiSafi1 addPath = new AfiSafi1Builder()
        .setReceive(false)
        .setSendMax(Shorts.checkedCast(maxPaths))
        .build();
        return new AfiSafiBuilder(afiSafi)
        .addAugmentation(AfiSafi1.class, addPath).build();
    }

    public static AfiSafi toNeighborAfiSafiAddPath(final AfiSafi afiSafi, final BgpTableType tableType, final List<AddressFamilies> capabilities) {
        final Optional<AddressFamilies> capability = Iterables.tryFind(capabilities, af -> af.getAfi().equals(tableType.getAfi()) && af.getSafi().equals(tableType.getSafi()));
        if (!capability.isPresent()) {
            return afiSafi;
        }
        return new AfiSafiBuilder(afiSafi)
        .addAugmentation(AfiSafi2.class, fromSendReceiveMode(capability.get().getSendReceive())).build();
    }

    private static AfiSafi2 fromSendReceiveMode(final SendReceive mode) {
        final AfiSafi2Builder builder = new AfiSafi2Builder();
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

    public static boolean isAppNeighbor(final Neighbor neighbor) {
        final Config1 config1 = neighbor.getConfig().getAugmentation(Config1.class);
        if (config1 != null) {
            return config1.getPeerGroup() != null && config1.getPeerGroup().equals(OpenConfigUtil.APPLICATION_PEER_GROUP_NAME);
        }
        return false;
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

    private static boolean isRrClient(final Neighbor neighbor) {
        final RouteReflector routeReflector = neighbor.getRouteReflector();
        if (routeReflector != null && routeReflector.getConfig() != null) {
            return routeReflector.getConfig().isRouteReflectorClient();
        }
        return false;
    }
}
