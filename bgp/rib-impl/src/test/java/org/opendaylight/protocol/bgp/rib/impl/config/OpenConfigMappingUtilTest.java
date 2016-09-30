/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.bgp.rib.impl.config.AbstractConfig.AS;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.AFI_SAFI;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.AFI_SAFI_IPV4;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.DEFAULT_TIMERS;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.MD5_PASSWORD;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.NEIGHBOR_ADDRESS;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.PORT;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.SHORT;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createAfiSafi;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createNeighborExpected;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createTransport;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getSimpleRoutingPolicy;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil.INSTANCE;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.ImportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl.BGPRenderStats;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafis;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalConfigAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborConfigAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class OpenConfigMappingUtilTest {
    private static final Neighbor NEIGHBOR = createNeighborExpected(NEIGHBOR_ADDRESS);
    private static final String KEY = "bgp";
    private static final InstanceIdentifier<Bgp> BGP_II = InstanceIdentifier.create(NetworkInstances.class)
        .child(NetworkInstance.class, new NetworkInstanceKey("identifier-test")).child(Protocols.class)
        .child(Protocol.class, new ProtocolKey(BGP.class, KEY)).augmentation(Protocol1.class).child(Bgp.class);
    private static final NeighborKey NEIGHBOR_KEY = new NeighborKey(NEIGHBOR_ADDRESS);
    private static final Ipv4Address ROUTER_ID = new Ipv4Address("1.2.3.4");
    private static final Ipv4Address CLUSTER_ID = new Ipv4Address("4.3.2.1");

    private static class RibMock implements RIB {

        @Override
        public AsNumber getLocalAs() {
            return AS;
        }

        @Override
        public BgpId getBgpIdentifier() {
            return null;
        }

        @Nonnull
        @Override
        public Set<? extends BgpTableType> getLocalTables() {
            return null;
        }

        @Override
        public BGPDispatcher getDispatcher() {
            return null;
        }

        @Override
        public DOMTransactionChain createPeerChain(final TransactionChainListener listener) {
            return null;
        }

        @Override
        public RIBExtensionConsumerContext getRibExtensions() {
            return null;
        }

        @Override
        public RIBSupportContextRegistry getRibSupportContext() {
            return null;
        }

        @Override
        public YangInstanceIdentifier getYangRibId() {
            return null;
        }

        @Override
        public CodecsRegistry getCodecsRegistry() {
            return null;
        }

        @Override
        public Optional<BGPOpenConfigProvider> getOpenConfigProvider() {
            return null;
        }

        @Override
        public DOMDataTreeChangeService getService() {
            return null;
        }

        @Override
        public BGPRenderStats getRenderStats() {
            return null;
        }

        @Override
        public ImportPolicyPeerTracker getImportPolicyPeerTracker() {
            return null;
        }

        @Override
        public ExportPolicyPeerTracker getExportPolicyPeerTracker(final TablesKey tablesKey) {
            return null;
        }

        @Override
        public Set<TablesKey> getLocalTablesKeys() {
            return null;
        }

        @Override
        public ServiceGroupIdentifier getRibIServiceGroupIdentifier() {
            return null;
        }

        @Override
        public ClusterSingletonServiceRegistration registerClusterSingletonService(final ClusterSingletonService clusterSingletonService) {
            return null;
        }

        @Override
        public void close() throws Exception {

        }

        @Override
        public KeyedInstanceIdentifier<Rib, RibKey> getInstanceIdentifier() {
            return null;
        }
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
        assertEquals(AS, OpenConfigMappingUtil.getPeerAs(new NeighborBuilder().build(), new RibMock()));
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
}