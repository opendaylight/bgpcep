/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;

import com.google.common.base.Optional;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AddPaths;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AddPathsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafis;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflector;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflectorBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.add.paths.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.CommunityType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.RrClusterIdTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1Builder;

public class BgpPeerTest extends AbstractConfig {
    static final short SHORT = 0;
    static final IpAddress NEIGHBOR_ADDRESS = new IpAddress(new Ipv4Address("127.0.0.1"));
    static final String MD5_PASSWORD = "123";
    static final PortNumber PORT = new PortNumber(179);
    static final AfiSafi AFI_SAFI_IPV4 = new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(AfiSafi1.class, new AfiSafi1Builder().setReceive(true).setSendMax(SHORT).build()).build();
    static final List<AfiSafi> AFI_SAFI = Collections.singletonList(AFI_SAFI_IPV4);
    private static final BigDecimal DEFAULT_TIMERS = BigDecimal.valueOf(30);
    private BgpPeer bgpPeer;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.bgpPeer = new BgpPeer(Mockito.mock(RpcProviderRegistry.class));
    }

    @Test
    public void testBgpPeer() throws Exception {
        final Neighbor neighbor = new NeighborBuilder().setAfiSafis(createAfiSafi()).setConfig(createConfig())
                .setNeighborAddress(NEIGHBOR_ADDRESS).setRouteReflector(createRR()).setTimers(createTimers())
                .setTransport(createTransport()).setAddPaths(createAddPath()).build();

        this.bgpPeer.start(this.rib, neighbor, this.tableTypeRegistry, null);
        Mockito.verify(this.rib).createPeerChain(any());
        Mockito.verify(this.rib, times(2)).getLocalAs();
        Mockito.verify(this.rib).getLocalTables();

        this.bgpPeer.instantiateServiceInstance();
        Mockito.verify(this.bgpPeerRegistry).addPeer(any(), any(), any());
        Mockito.verify(this.dispatcher).createReconnectingClient(any(InetSocketAddress.class),
                anyInt(), any(Optional.class));

        try {
            this.bgpPeer.start(this.rib, neighbor, this.tableTypeRegistry,null);
            fail("Expected Exception");
        } catch (final IllegalStateException expected) {
            assertEquals("Previous peer instance was not closed.", expected.getMessage());
        }
        this.bgpPeer.closeServiceInstance();
        this.bgpPeer.close();
        Mockito.verify(this.future).cancel(true);

        this.bgpPeer.restart(this.rib, this.tableTypeRegistry);
        this.bgpPeer.instantiateServiceInstance();
        Mockito.verify(this.rib, times(2)).createPeerChain(any());
        Mockito.verify(this.rib, times(4)).getLocalAs();
        Mockito.verify(this.rib, times(2)).getLocalTables();

        final Neighbor neighborExpected = createNeighborExpected(NEIGHBOR_ADDRESS);
        assertTrue(this.bgpPeer.containsEqualConfiguration(neighborExpected));
        assertFalse(this.bgpPeer.containsEqualConfiguration(createNeighborExpected(
                new IpAddress(new Ipv4Address("127.0.0.2")))));
        Mockito.verify(this.bgpPeerRegistry).removePeer(any(IpAddress.class));

        this.bgpPeer.closeServiceInstance();
        this.bgpPeer.close();
        Mockito.verify(this.future, times(2)).cancel(true);

        final Neighbor neighborDiffConfig = new NeighborBuilder().setNeighborAddress(NEIGHBOR_ADDRESS)
                .setAfiSafis(createAfiSafi()).build();
        this.bgpPeer.start(this.rib, neighborDiffConfig, this.tableTypeRegistry,null);
        assertTrue(this.bgpPeer.containsEqualConfiguration(neighborDiffConfig));
        this.bgpPeer.close();
    }

    static Neighbor createNeighborExpected(final IpAddress neighborAddress) {
        return new NeighborBuilder()
                .setAfiSafis(createAfiSafi())
                .setConfig(createConfig())
                .setNeighborAddress(neighborAddress)
                .setRouteReflector(createRR())
                .setTimers(createTimers())
                .setTransport(createTransport())
                .setAddPaths(createAddPath())
                .build();
    }

    static Transport createTransport() {
        return new TransportBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.
                rev151009.bgp.neighbor.group.transport.ConfigBuilder().setMtuDiscovery(false)
                .setPassiveMode(false).addAugmentation(Config1.class, new Config1Builder()
                        .setRemotePort(PORT).build()).build()).build();
    }

    static Timers createTimers() {
        return new TimersBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009
                .bgp.neighbor.group.timers.ConfigBuilder()
                .setHoldTime(DEFAULT_TIMERS)
                .setMinimumAdvertisementInterval(DEFAULT_TIMERS)
                .setKeepaliveInterval(DEFAULT_TIMERS)
                .setConnectRetry(DEFAULT_TIMERS).build()).build();
    }

    static RouteReflector createRR() {
        return new RouteReflectorBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
                .rev151009.bgp.neighbor.group.route.reflector.ConfigBuilder()
                .setRouteReflectorClusterId(RrClusterIdTypeBuilder.getDefaultInstance("127.0.0.1"))
                .setRouteReflectorClient(false).build()).build();
    }

    static Config createConfig() {
        return new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.
                ConfigBuilder()
                .setPeerAs(AS)
                .setPeerType(PeerType.INTERNAL)
                .setAuthPassword(MD5_PASSWORD)
                .setRouteFlapDamping(false)
                .setSendCommunity(CommunityType.NONE).build();
    }

    static AfiSafis createAfiSafi() {
        return new AfiSafisBuilder().setAfiSafi(AFI_SAFI).build();
    }

    static AddPaths createAddPath() {
        return new AddPathsBuilder().setConfig(new ConfigBuilder().setReceive(true).setSendMax(SHORT).build()).build();
    }
}