/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.impl;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.APPLICATION_PEER_GROUP_NAME;

import com.google.common.primitives.Shorts;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflectorBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.CommunityType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.RrClusterIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalConfigAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.IPV6FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class BGPOpenConfigMappingServiceImplTest {
    private static final ClusterIdentifier CLUSTER_IDENTIFIER = new ClusterIdentifier("192.168.1.2");
    private static final Long ALL_PATHS = 0L;
    private static final Long N_PATHS = 2L;
    private static final PathSelectionMode ADD_PATH_BEST_N_PATH_SELECTION = new AddPathBestNPathSelection(N_PATHS);
    private static final PathSelectionMode ADD_PATH_BEST_ALL_PATH_SELECTION = new AllPathSelection();
    private static final BGPOpenConfigMappingServiceImpl OPENCONFIG = new BGPOpenConfigMappingServiceImpl();
    private static final AsNumber AS = new AsNumber(72L);
    private static final IpAddress IPADDRESS = new IpAddress(new Ipv4Address("127.0.0.1"));
    private static final BgpId BGP_ID = new BgpId(IPADDRESS.getIpv4Address());
    private static final RibId RIB_ID = new RibId("bgp");
    private static final List<AddressFamilies> FAMILIES;
    private static final List<BgpTableType> TABLE_TYPES;
    private static final List<AfiSafi> AFISAFIS = new ArrayList<>();
    private static final BigDecimal DEFAULT_TIMERS = BigDecimal.valueOf(30);
    private static final PortNumber PORT_NUMBER = new PortNumber(179);

    static {
        FAMILIES = new ArrayList<>();
        FAMILIES.add(new AddressFamiliesBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
            .setSendReceive(SendReceive.Both).build());
        FAMILIES.add(new AddressFamiliesBuilder().setAfi(Ipv6AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
            .setSendReceive(SendReceive.Send).build());
        FAMILIES.add(new AddressFamiliesBuilder().setAfi(Ipv6AddressFamily.class).setSafi(FlowspecSubsequentAddressFamily.class)
            .setSendReceive(SendReceive.Receive).build());
        TABLE_TYPES = new ArrayList<>();
        TABLE_TYPES.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        TABLE_TYPES.add(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class));
        AFISAFIS.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(AfiSafi2.class, new AfiSafi2Builder().setReceive(true).setSendMax(Shorts.checkedCast(N_PATHS)).build()).build());
        AFISAFIS.add(new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(AfiSafi2.class, new AfiSafi2Builder().setReceive(true).setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
    }

    @Test
    public void toTableTypes() {
        final List<AfiSafi> families = new ArrayList<>();
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build());
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class).build());
        final List<BgpTableType> result = OPENCONFIG.toTableTypes(families);
        assertEquals(TABLE_TYPES, result);
    }

    @Test
    public void toPathSelectionMode() {
        final List<AfiSafi> families = new ArrayList<>();
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(AfiSafi2.class, new AfiSafi2Builder().setSendMax(Shorts.checkedCast(N_PATHS)).build()).build());
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(AfiSafi2.class, new AfiSafi2Builder().setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
        final Map<BgpTableType, PathSelectionMode> result = OPENCONFIG.toPathSelectionMode(families);
        final Map<BgpTableType, PathSelectionMode> expected = new HashMap<>();
        expected.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class), ADD_PATH_BEST_N_PATH_SELECTION);
        expected.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class), ADD_PATH_BEST_ALL_PATH_SELECTION);
        assertEquals(expected.get(0), result.get(0));
        assertEquals(expected.get(1), result.get(1));
    }

    @Test
    public void isApplicationPeer() {
        assertFalse(OPENCONFIG.isApplicationPeer(new NeighborBuilder().setConfig(new ConfigBuilder().build()).build()));
        final Neighbor neighbor = new NeighborBuilder().setConfig(new ConfigBuilder()
            .addAugmentation(Config2.class, new Config2Builder().setPeerGroup(APPLICATION_PEER_GROUP_NAME).build())
            .build()).build();
        assertTrue(OPENCONFIG.isApplicationPeer(neighbor));
    }

    @Test
    public void toPeerRole() {
        Neighbor neighbor = new NeighborBuilder().setConfig(new ConfigBuilder().setPeerType(PeerType.EXTERNAL).build()).build();
        PeerRole peerRoleResult = OPENCONFIG.toPeerRole(neighbor);
        Assert.assertEquals(PeerRole.Ebgp, peerRoleResult);

        neighbor = new NeighborBuilder().setConfig(new ConfigBuilder().setPeerType(PeerType.INTERNAL).build()).build();
        peerRoleResult = OPENCONFIG.toPeerRole(neighbor);
        Assert.assertEquals(PeerRole.Ibgp, peerRoleResult);

        neighbor = new NeighborBuilder()
            .setRouteReflector(new RouteReflectorBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp
                .neighbor.group.route.reflector.ConfigBuilder().setRouteReflectorClient(true).build()).build()).build();
        peerRoleResult = OPENCONFIG.toPeerRole(neighbor);
        Assert.assertEquals(PeerRole.RrClient, peerRoleResult);
    }

    @Test
    public void toAddPathCapability() {
        final List<AfiSafi> families = new ArrayList<>();
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder()
                    .setReceive(true).setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder()
                    .setReceive(false).setSendMax(Shorts.checkedCast(N_PATHS)).build()).build());
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV6FLOW.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder()
                    .setReceive(false).build()).build());
        final List<AddressFamilies> result = OPENCONFIG.toAddPathCapability(families);
        assertEquals(FAMILIES, result);
    }

    @Test
    public void toSendReceiveMode() {
        final Map<TablesKey, PathSelectionMode> bgpTableKeyPsm = new HashMap<>();
        bgpTableKeyPsm.put(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class), ADD_PATH_BEST_N_PATH_SELECTION);
        bgpTableKeyPsm.put(new TablesKey(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class), ADD_PATH_BEST_ALL_PATH_SELECTION);

        final Global result = OPENCONFIG.fromRib(BGP_ID, CLUSTER_IDENTIFIER, RIB_ID, AS, TABLE_TYPES, bgpTableKeyPsm);
        final Global expected = new GlobalBuilder()
                .setAfiSafis(new AfiSafisBuilder().setAfiSafi(AFISAFIS).build())
                .setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base
                    .ConfigBuilder()
                    .setAs(AS)
                    .setRouterId(BGP_ID)
                    .addAugmentation(GlobalConfigAugmentation.class,
                            new GlobalConfigAugmentationBuilder().setRouteReflectorClusterId(new RrClusterIdType(CLUSTER_IDENTIFIER)).build())
                    .build()).build();
        assertEquals(expected, result);
    }

    @Test
    public void fromBgpPeer() {
        final Neighbor result = OPENCONFIG.fromBgpPeer(FAMILIES, TABLE_TYPES, 30, IPADDRESS, true, null, PORT_NUMBER, 30, AS, PeerRole.Ibgp, null);
        final List<AfiSafi> afisafis = new ArrayList<>();
        afisafis.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(AfiSafi1.class, new AfiSafi1Builder().setReceive(true).setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
        afisafis.add(new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(AfiSafi1.class, new AfiSafi1Builder().setReceive(false).setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
        final Neighbor expected = new NeighborBuilder()
            .setAfiSafis(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder().setAfiSafi(afisafis).build())
            .setConfig(new ConfigBuilder().setPeerAs(AS).setPeerType(PeerType.INTERNAL).setRouteFlapDamping(false).setSendCommunity(CommunityType.NONE).build())
            .setNeighborAddress(IPADDRESS)
            .setRouteReflector(new RouteReflectorBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp
                .neighbor.group.route.reflector.ConfigBuilder().setRouteReflectorClient(false).build()).build())
            .setTimers(new TimersBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                .timers.ConfigBuilder().setHoldTime(DEFAULT_TIMERS).setMinimumAdvertisementInterval(DEFAULT_TIMERS)
                .setKeepaliveInterval(DEFAULT_TIMERS).setConnectRetry(DEFAULT_TIMERS).build()).build())
            .setTransport(new TransportBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor
                .group.transport.ConfigBuilder().setMtuDiscovery(false).setPassiveMode(false).addAugmentation(Config1.class, new Config1Builder()
                .setRemotePort(new PortNumber(179)).build()).build()).build())
            .build();
        assertEquals(expected, result);
    }

    @Test
    public void fromApplicationPeer() {
        final ApplicationRibId app = new ApplicationRibId("app");
        final Neighbor result = OPENCONFIG.fromApplicationPeer(app, BGP_ID);
        final Neighbor expected = new NeighborBuilder().setConfig(new ConfigBuilder().setDescription(app.getValue())
            .addAugmentation(Config2.class, new Config2Builder().setPeerGroup(APPLICATION_PEER_GROUP_NAME).build()).build())
            .setNeighborAddress(IPADDRESS).build();
        assertEquals(expected, result);
    }
}