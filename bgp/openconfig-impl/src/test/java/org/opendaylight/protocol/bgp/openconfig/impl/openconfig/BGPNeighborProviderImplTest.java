/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev150515.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev150515.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.RouteReflectorBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.PeerType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev150930.Ipv6Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;

public class BGPNeighborProviderImplTest {

    private BGPNeighborProviderImpl neighborProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final BindingTransactionChain txChain = Mockito.mock(BindingTransactionChain.class);
        final BGPConfigStateStore stateHolders = Mockito.mock(BGPConfigStateStore.class);
        final BGPConfigHolder<Neighbor> configHolder = Mockito.mock(BGPConfigHolder.class);
        Mockito.doReturn(configHolder).when(stateHolders).getBGPConfigHolder(Mockito.any(Class.class));
        neighborProvider = new BGPNeighborProviderImpl(txChain, stateHolders);
    }

    @Test
    public void testCreateModuleKey() {
        assertEquals(new ModuleKey("instanceName", BgpPeer.class), neighborProvider.createModuleKey("instanceName"));
    }

    @Test
    public void testGetInstanceConfigurationType() {
        assertEquals(BGPPeerInstanceConfiguration.class, neighborProvider.getInstanceConfigurationType());
    }

    @Test
    public void testApply() {
        final Neighbor neighbor = neighborProvider.apply(new BGPPeerInstanceConfiguration(new InstanceConfigurationIdentifier("instanceName"),
                new IpAddress(new Ipv4Address("1.2.3.4")), new PortNumber(123), (short) 10, PeerRole.RrClient, false,
                Lists.<BgpTableType>newArrayList(new BgpTableTypeImpl(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class)),
                new AsNumber(10L), Optional.<Rfc2385Key>absent()));
        final Neighbor expectedNeighbor = new NeighborBuilder()
            .setAfiSafis(new AfiSafisBuilder().setAfiSafi(Lists.<AfiSafi>newArrayList(new AfiSafiBuilder().setAfiSafiName(Ipv6Flow.class).build())).build())
            .setNeighborAddress(new IpAddress(new Ipv4Address("1.2.3.4")))
            .setKey(new NeighborKey(new IpAddress(new Ipv4Address("1.2.3.4"))))
            .setConfig(new ConfigBuilder().setPeerAs(new AsNumber(10L)).setPeerType(PeerType.INTERNAL).build())
            .setRouteReflector(new RouteReflectorBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.route.reflector.ConfigBuilder().setRouteReflectorClient(true).build()).build())
            .setTimers(new TimersBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.timers.ConfigBuilder().setHoldTime(BigDecimal.valueOf(10)).build()).build())
            .setTransport(new TransportBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.transport.ConfigBuilder().setPassiveMode(true).build()).build())
            .build();
        assertEquals(expectedNeighbor, neighbor);
    }

}
