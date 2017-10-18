/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.AFI_SAFI;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.AFI_SAFI_IPV4;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.MD5_PASSWORD;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.NEIGHBOR_ADDRESS;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.PORT;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.SHORT;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createAfiSafi;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createNeighborExpected;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createTransport;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getSimpleRoutingPolicy;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil.INSTANCE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafis;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflectorBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.CommunityType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.RrClusterIdType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Config1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Config1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Config2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.GlobalConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.GlobalConfigAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborConfigAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Protocol1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OpenConfigMappingUtilTest {
    private static final Neighbor NEIGHBOR = createNeighborExpected(NEIGHBOR_ADDRESS);
    private static final String KEY = "bgp";
    private static final InstanceIdentifier<Bgp> BGP_II = InstanceIdentifier.create(NetworkInstances.class)
        .child(NetworkInstance.class, new NetworkInstanceKey("identifier-test")).child(Protocols.class)
        .child(Protocol.class, new ProtocolKey(BGP.class, KEY)).augmentation(Protocol1.class).child(Bgp.class);
    private static final NeighborKey NEIGHBOR_KEY = new NeighborKey(NEIGHBOR_ADDRESS);
    private static final Ipv4Address ROUTER_ID = new Ipv4Address("1.2.3.4");
    private static final Ipv4Address CLUSTER_ID = new Ipv4Address("4.3.2.1");

    private static final Long ALL_PATHS = 0L;
    private static final Long N_PATHS = 2L;
    private static final PathSelectionMode ADD_PATH_BEST_N_PATH_SELECTION = new AddPathBestNPathSelection(N_PATHS);
    private static final PathSelectionMode ADD_PATH_BEST_ALL_PATH_SELECTION = new AllPathSelection();
    private static final BgpTableType BGP_TABLE_TYPE_IPV4 = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private static final BgpTableType BGP_TABLE_TYPE_IPV6 = new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private static final AfiSafi AFISAFI_IPV4 = new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build();
    private static final TablesKey K4 = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private static final ClusterIdentifier CLUSTER_IDENTIFIER = new ClusterIdentifier("192.168.1.2");
    private static final AsNumber AS = new AsNumber(72L);
    private static final BgpId BGP_ID = new BgpId(NEIGHBOR_ADDRESS.getIpv4Address());
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
        FAMILIES.add(new AddressFamiliesBuilder().setAfi(Ipv6AddressFamily.class).setSafi(MplsLabeledVpnSubsequentAddressFamily.class)
            .setSendReceive(SendReceive.Receive).build());
        TABLE_TYPES = new ArrayList<>();
        TABLE_TYPES.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        TABLE_TYPES.add(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class));
        AFISAFIS.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(AfiSafi2.class, new AfiSafi2Builder().setReceive(true).setSendMax(Shorts.checkedCast(N_PATHS)).build()).build());
        AFISAFIS.add(new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(AfiSafi2.class, new AfiSafi2Builder().setReceive(true).setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
    }

    @Mock
    private BGPTableTypeRegistryConsumer tableTypeRegistry;

    @Mock
    private RIB rib;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(java.util.Optional.of(BGP_TABLE_TYPE_IPV4))
            .when(this.tableTypeRegistry).getTableType(IPV4UNICAST.class);
        Mockito.doReturn(java.util.Optional.of(BGP_TABLE_TYPE_IPV6))
            .when(this.tableTypeRegistry).getTableType(IPV6UNICAST.class);
        Mockito.doReturn(java.util.Optional.of(new BgpTableTypeImpl(Ipv6AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class)))
            .when(this.tableTypeRegistry).getTableType(IPV6LABELLEDUNICAST.class);
        Mockito.doReturn(java.util.Optional.of(IPV4UNICAST.class))
            .when(this.tableTypeRegistry).getAfiSafiType(BGP_TABLE_TYPE_IPV4);
        Mockito.doReturn(java.util.Optional.of(IPV6UNICAST.class))
            .when(this.tableTypeRegistry).getAfiSafiType(BGP_TABLE_TYPE_IPV6);
        Mockito.doReturn(AS).when(this.rib).getLocalAs();
    }

    @Test
    public void testGetRibInstanceName() throws Exception {
        assertEquals(KEY, OpenConfigMappingUtil.getRibInstanceName(BGP_II));
    }

    @Test
    public void testGetHoldTimer() throws Exception {
        assertEquals(DEFAULT_TIMERS.toBigInteger().intValue(), OpenConfigMappingUtil.getHoldTimer(NEIGHBOR));
    }

    @Test
    public void testGetPeerAs() throws Exception {
        assertEquals(AS, OpenConfigMappingUtil.getPeerAs(NEIGHBOR, null));
        assertEquals(AS, OpenConfigMappingUtil.getPeerAs(new NeighborBuilder().build(), this.rib));
    }

    @Test
    public void testIsActive() throws Exception {
        assertTrue(OpenConfigMappingUtil.isActive(new NeighborBuilder().build()));
        assertTrue(OpenConfigMappingUtil.isActive(new NeighborBuilder().setTransport(new TransportBuilder().build()).build()));
        assertTrue(OpenConfigMappingUtil.isActive(new NeighborBuilder().setTransport(createTransport()).build()));
    }

    @Test
    public void testGetRetryTimer() throws Exception {
        assertEquals(DEFAULT_TIMERS.toBigInteger().intValue(), OpenConfigMappingUtil.getRetryTimer(NEIGHBOR));
        assertEquals(DEFAULT_TIMERS.toBigInteger().intValue(), OpenConfigMappingUtil.getRetryTimer(new NeighborBuilder().build()));
    }

    @Test
    public void testGetNeighborKey() throws Exception {
        assertArrayEquals(MD5_PASSWORD.getBytes(StandardCharsets.US_ASCII),
            OpenConfigMappingUtil.getNeighborKey(NEIGHBOR).get(INSTANCE.inetAddressFor(NEIGHBOR_ADDRESS)));
        assertNull(OpenConfigMappingUtil.getNeighborKey(new NeighborBuilder().build()));
        assertNull(OpenConfigMappingUtil.getNeighborKey(new NeighborBuilder().setConfig(new ConfigBuilder().build()).build()));
    }

    @Test
    public void testGetNeighborInstanceIdentifier() throws Exception {
        assertEquals(BGP_II.child(Neighbors.class).child(Neighbor.class, NEIGHBOR_KEY),
            OpenConfigMappingUtil.getNeighborInstanceIdentifier(BGP_II, NEIGHBOR_KEY));

    }

    @Test
    public void testGetNeighborInstanceName() throws Exception {
        assertEquals(NEIGHBOR_ADDRESS.getIpv4Address().getValue(),
            OpenConfigMappingUtil.getNeighborInstanceName(BGP_II.child(Neighbors.class).child(Neighbor.class, NEIGHBOR_KEY)));
    }

    @Test
    public void testGetPort() throws Exception {
        assertEquals(PORT, OpenConfigMappingUtil.getPort(NEIGHBOR));
        assertEquals(PORT, OpenConfigMappingUtil.getPort(new NeighborBuilder().setTransport(new TransportBuilder().build()).build()));
        assertEquals(PORT, OpenConfigMappingUtil.getPort(new NeighborBuilder().setTransport(
            new TransportBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.transport.
                ConfigBuilder().build()).build()).build()));
        assertEquals(PORT, OpenConfigMappingUtil.getPort(new NeighborBuilder().setTransport(
            new TransportBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.transport.
                ConfigBuilder().addAugmentation(Config1.class, new Config1Builder().build()).build()).build()).build()));
    }

    @Test
    public void testGetAfiSafiWithDefault() throws Exception {
        final ImmutableList<AfiSafi> defaultValue = ImmutableList.of(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build());
        assertEquals(defaultValue, OpenConfigMappingUtil.getAfiSafiWithDefault(null, true));
        final AfiSafis afiSafi = new AfiSafisBuilder().build();
        assertEquals(defaultValue, OpenConfigMappingUtil.getAfiSafiWithDefault(afiSafi, true));

        final AfiSafi afiSafiIpv6 = new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class).addAugmentation(AfiSafi1.class,
            new AfiSafi1Builder().setReceive(true).setSendMax(SHORT).build()).build();
        final List<AfiSafi> afiSafiIpv6List = new ArrayList<>();
        afiSafiIpv6List.add(afiSafiIpv6);

        final List<AfiSafi> expected = new ArrayList<>(afiSafiIpv6List);
        expected.add(AFI_SAFI_IPV4);
        assertEquals(afiSafiIpv6, OpenConfigMappingUtil.getAfiSafiWithDefault(new AfiSafisBuilder().setAfiSafi(afiSafiIpv6List).build(), true).get(0));
        assertEquals(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build(),
            OpenConfigMappingUtil.getAfiSafiWithDefault(new AfiSafisBuilder().setAfiSafi(afiSafiIpv6List).build(), true).get(1));
        assertEquals(AFI_SAFI, OpenConfigMappingUtil.getAfiSafiWithDefault(createAfiSafi(), true));

        assertTrue(OpenConfigMappingUtil.getAfiSafiWithDefault(null, false).isEmpty());
        assertTrue(OpenConfigMappingUtil.getAfiSafiWithDefault(afiSafi, false).isEmpty());
        assertEquals(afiSafiIpv6, OpenConfigMappingUtil.getAfiSafiWithDefault(new AfiSafisBuilder().setAfiSafi(afiSafiIpv6List).build(), false).get(0));
        assertEquals(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build(),
            OpenConfigMappingUtil.getAfiSafiWithDefault(new AfiSafisBuilder().setAfiSafi(afiSafiIpv6List).build(), false).get(1));
        assertEquals(AFI_SAFI, OpenConfigMappingUtil.getAfiSafiWithDefault(createAfiSafi(), false));
    }

    @Test
    public void testGetClusterIdentifier() {
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder configBuilder = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder();
        configBuilder.setRouterId(ROUTER_ID);
        assertEquals(ROUTER_ID.getValue(), OpenConfigMappingUtil.getClusterIdentifier(configBuilder.build()).getValue());

        configBuilder.addAugmentation(GlobalConfigAugmentation.class,
                new GlobalConfigAugmentationBuilder().setRouteReflectorClusterId(new RrClusterIdType(CLUSTER_ID)).build()).build();
        assertEquals(CLUSTER_ID.getValue(), OpenConfigMappingUtil.getClusterIdentifier(configBuilder.build()).getValue());
    }

    @Test
    public void testGetSimpleRoutingPolicy() {
        final NeighborBuilder neighborBuilder = new NeighborBuilder();
        assertNull(getSimpleRoutingPolicy(neighborBuilder.build()));
        neighborBuilder.setConfig(new ConfigBuilder()
                .addAugmentation(NeighborConfigAugmentation.class,
                        new NeighborConfigAugmentationBuilder().setSimpleRoutingPolicy(SimpleRoutingPolicy.LearnNone).build()).build());
        assertEquals(SimpleRoutingPolicy.LearnNone, getSimpleRoutingPolicy(neighborBuilder.build()));
    }

    @Test
    public void isAppNeighbor() {
        assertFalse(OpenConfigMappingUtil.isApplicationPeer(new NeighborBuilder().setConfig(new ConfigBuilder().build()).build()));
        final Neighbor neighbor = new NeighborBuilder().setConfig(new ConfigBuilder()
            .addAugmentation(Config2.class, new Config2Builder().setPeerGroup(OpenConfigMappingUtil.APPLICATION_PEER_GROUP_NAME).build())
            .build()).build();
        assertTrue(OpenConfigMappingUtil.isApplicationPeer(neighbor));
    }

    @Test
    public void testToAfiSafis() {
        final List<AfiSafi> afiSafis = OpenConfigMappingUtil.toAfiSafis(Lists.newArrayList(BGP_TABLE_TYPE_IPV4), (afisafi, tableType) -> afisafi,
                this.tableTypeRegistry);
        Assert.assertEquals(Collections.singletonList(AFISAFI_IPV4), afiSafis);
    }

    @Test
    public void toPeerType() {
        Assert.assertEquals(PeerType.EXTERNAL, OpenConfigMappingUtil.toPeerType(PeerRole.Ebgp));
        Assert.assertEquals(PeerType.INTERNAL, OpenConfigMappingUtil.toPeerType(PeerRole.Ibgp));
        Assert.assertNull(OpenConfigMappingUtil.toPeerType(PeerRole.Internal));
        Assert.assertEquals(PeerType.INTERNAL, OpenConfigMappingUtil.toPeerType(PeerRole.RrClient));
    }

    @Test
    public void toNeighborAfiSafiAddPath() {
        final AfiSafi afiSafi = OpenConfigMappingUtil.toNeighborAfiSafiAddPath(AFISAFI_IPV4, BGP_TABLE_TYPE_IPV4, Collections.emptyList());
        Assert.assertEquals(AFISAFI_IPV4, afiSafi);

        final AfiSafi afisafiIpv6Both = new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1Builder()
                    .setSendMax(Shorts.checkedCast(ALL_PATHS)).setReceive(true).build()).build();
        final AfiSafi afiSafi6 = OpenConfigMappingUtil.toNeighborAfiSafiAddPath(afisafiIpv6Both, BGP_TABLE_TYPE_IPV6,
            Collections.singletonList(new AddressFamiliesBuilder(BGP_TABLE_TYPE_IPV6).setSendReceive(SendReceive.Both).build()));
        Assert.assertEquals(afisafiIpv6Both, afiSafi6);

        final AfiSafi afisafiIpv6ReceiveExpected = new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1Builder()
                    .setReceive(true).build()).build();
        final AfiSafi afiSafi6ReceiveResult = OpenConfigMappingUtil.toNeighborAfiSafiAddPath(afisafiIpv6ReceiveExpected, BGP_TABLE_TYPE_IPV6,
            Collections.singletonList(new AddressFamiliesBuilder(BGP_TABLE_TYPE_IPV6).setSendReceive(SendReceive.Receive).build()));
        Assert.assertEquals(afisafiIpv6ReceiveExpected, afiSafi6ReceiveResult);

        final AfiSafi afisafiIpv6SendExpected = new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1Builder()
                    .setSendMax(Shorts.checkedCast(ALL_PATHS)).setReceive(false).build()).build();
        final AfiSafi afiSafi6SendResult = OpenConfigMappingUtil.toNeighborAfiSafiAddPath(afisafiIpv6ReceiveExpected, BGP_TABLE_TYPE_IPV6,
            Collections.singletonList(new AddressFamiliesBuilder(BGP_TABLE_TYPE_IPV6).setSendReceive(SendReceive.Send).build()));
        Assert.assertEquals(afisafiIpv6SendExpected, afiSafi6SendResult);
    }

    @Test
    public void toGlobalAfiSafiAddPath() {
        final AfiSafi afiSafi = OpenConfigMappingUtil.toGlobalAfiSafiAddPath(AFISAFI_IPV4, BGP_TABLE_TYPE_IPV4, Collections.emptyMap());
        Assert.assertEquals(AFISAFI_IPV4, afiSafi);

        final AfiSafi afiSafiResult = OpenConfigMappingUtil.toGlobalAfiSafiAddPath(AFISAFI_IPV4, BGP_TABLE_TYPE_IPV4, Collections.singletonMap(K4,
            ADD_PATH_BEST_N_PATH_SELECTION));
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2 addPath =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2Builder()
                .setSendMax(Shorts.checkedCast(N_PATHS)).setReceive(true).build();
        final AfiSafi afisafiIpv4Psm2 = new AfiSafiBuilder(afiSafi)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2.class,
                addPath).build();
        Assert.assertEquals(afisafiIpv4Psm2, afiSafiResult);

        final AfiSafi afiSafiAllResult = OpenConfigMappingUtil.toGlobalAfiSafiAddPath(AFISAFI_IPV4, BGP_TABLE_TYPE_IPV4,
            Collections.singletonMap(K4, ADD_PATH_BEST_ALL_PATH_SELECTION));
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2 addPathAll =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2Builder()
                .setSendMax(Shorts.checkedCast(ALL_PATHS)).setReceive(true).build();
        final AfiSafi afiAll = new AfiSafiBuilder(afiSafi)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi2.class,
                addPathAll).build();
        Assert.assertEquals(afiAll, afiSafiAllResult);
    }

    @Test
    public void toPathSelectionMode() {
        final List<AfiSafi> families = new ArrayList<>();
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(AfiSafi2.class, new AfiSafi2Builder().setSendMax(Shorts.checkedCast(N_PATHS)).build()).build());
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(AfiSafi2.class, new AfiSafi2Builder().setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
        final Map<BgpTableType, PathSelectionMode> result = OpenConfigMappingUtil.toPathSelectionMode(families, this.tableTypeRegistry);
        final Map<BgpTableType, PathSelectionMode> expected = new HashMap<>();
        expected.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class), ADD_PATH_BEST_N_PATH_SELECTION);
        expected.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class), ADD_PATH_BEST_ALL_PATH_SELECTION);
        assertEquals(expected.get(0), result.get(0));
        assertEquals(expected.get(1), result.get(1));
    }

    @Test
    public void toPeerRole() {
        Neighbor neighbor = new NeighborBuilder().setConfig(new ConfigBuilder().setPeerType(PeerType.EXTERNAL).build()).build();
        PeerRole peerRoleResult = OpenConfigMappingUtil.toPeerRole(neighbor);
        Assert.assertEquals(PeerRole.Ebgp, peerRoleResult);

        neighbor = new NeighborBuilder().setConfig(new ConfigBuilder().setPeerType(PeerType.INTERNAL).build()).build();
        peerRoleResult = OpenConfigMappingUtil.toPeerRole(neighbor);
        Assert.assertEquals(PeerRole.Ibgp, peerRoleResult);

        neighbor = new NeighborBuilder()
            .setRouteReflector(new RouteReflectorBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp
                .neighbor.group.route.reflector.ConfigBuilder().setRouteReflectorClient(true).build()).build()).build();
        peerRoleResult = OpenConfigMappingUtil.toPeerRole(neighbor);
        Assert.assertEquals(PeerRole.RrClient, peerRoleResult);
    }

    @Test
    public void toAddPathCapability() {
        final List<AfiSafi> families = new ArrayList<>();
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1Builder()
                    .setReceive(true).setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1Builder()
                    .setReceive(false).setSendMax(Shorts.checkedCast(N_PATHS)).build()).build());
        families.add(new AfiSafiBuilder().setAfiSafiName(IPV6LABELLEDUNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.AfiSafi1Builder()
                    .setReceive(false).build()).build());
        final List<AddressFamilies> result = OpenConfigMappingUtil.toAddPathCapability(families, this.tableTypeRegistry);
        assertEquals(FAMILIES, result);
    }

    @Test
    public void toSendReceiveMode() {
        final Map<TablesKey, PathSelectionMode> bgpTableKeyPsm = new HashMap<>();
        bgpTableKeyPsm.put(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class), ADD_PATH_BEST_N_PATH_SELECTION);
        bgpTableKeyPsm.put(new TablesKey(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class), ADD_PATH_BEST_ALL_PATH_SELECTION);

        final Global result = OpenConfigMappingUtil.fromRib(BGP_ID, CLUSTER_IDENTIFIER, RIB_ID, AS, TABLE_TYPES, bgpTableKeyPsm, this.tableTypeRegistry);
        final Global expected = new GlobalBuilder()
                .setAfiSafis(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder().setAfiSafi(AFISAFIS).build())
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
        final Neighbor result = OpenConfigMappingUtil.fromBgpPeer(FAMILIES, TABLE_TYPES, 30, NEIGHBOR_ADDRESS, true, null, PORT_NUMBER, 30, AS, PeerRole.Ibgp, null, this.tableTypeRegistry);
        final List<AfiSafi> afisafis = new ArrayList<>();
        afisafis.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(AfiSafi1.class, new AfiSafi1Builder().setReceive(true).setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
        afisafis.add(new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(AfiSafi1.class, new AfiSafi1Builder().setReceive(false).setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
        final Neighbor expected = new NeighborBuilder()
            .setAfiSafis(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder().setAfiSafi(afisafis).build())
            .setConfig(new ConfigBuilder().setPeerAs(AS).setPeerType(PeerType.INTERNAL).setRouteFlapDamping(false).setSendCommunity(CommunityType.NONE).build())
            .setNeighborAddress(NEIGHBOR_ADDRESS)
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
        final Neighbor result = OpenConfigMappingUtil.fromApplicationPeer(app, BGP_ID);
        final Neighbor expected = new NeighborBuilder().setConfig(new ConfigBuilder().setDescription(app.getValue())
            .addAugmentation(Config2.class, new Config2Builder().setPeerGroup(OpenConfigMappingUtil.APPLICATION_PEER_GROUP_NAME).build()).build())
            .setNeighborAddress(NEIGHBOR_ADDRESS).build();
        assertEquals(expected, result);
    }
}