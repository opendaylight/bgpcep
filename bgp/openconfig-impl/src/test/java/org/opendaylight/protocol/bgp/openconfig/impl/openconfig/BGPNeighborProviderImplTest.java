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
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflectorBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev150930.Ipv4Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev150930.Ipv6Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;

public class BGPNeighborProviderImplTest {

    private BGPNeighborProviderImpl neighborProvider;
    private static final String PASSWORD = "Ug1Yp4Ssw0Rd";
    private static final IpAddress IP = new IpAddress(new Ipv4Address("1.2.3.4"));
    private static final PortNumber PORT = new PortNumber(123);
    private static final short TIMER = (short) 10;
    private static final AsNumber AS = new AsNumber(10L);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final BindingTransactionChain txChain = Mockito.mock(BindingTransactionChain.class);
        final BGPConfigStateStore stateHolders = Mockito.mock(BGPConfigStateStore.class);
        final BGPConfigHolder<Neighbor> configHolder = Mockito.mock(BGPConfigHolder.class);
        Mockito.doReturn(configHolder).when(stateHolders).getBGPConfigHolder(Mockito.any(Class.class));
        this.neighborProvider = new BGPNeighborProviderImpl(txChain, stateHolders);
    }

    @Test
    public void testCreateModuleKey() {
        assertEquals(new ModuleKey("instanceName", BgpPeer.class), this.neighborProvider.createModuleKey("instanceName"));
    }

    @Test
    public void testGetInstanceConfigurationType() {
        assertEquals(BGPPeerInstanceConfiguration.class, this.neighborProvider.getInstanceConfigurationType());
    }

    @Test
    public void testApply() {
        final boolean active = false;
        final PeerRole role = PeerRole.RrClient;
        final Neighbor neighbor = this.neighborProvider.apply(createConfiguration(new InstanceConfigurationIdentifier("instanceName"),
            IP, PORT, TIMER, role, active,
            Lists.<BgpTableType>newArrayList(new BgpTableTypeImpl(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class)),
            AS, Optional.<Rfc2385Key>absent()));
        final Neighbor expectedNeighbor = createNeighbor(Lists.<AfiSafi>newArrayList(new AfiSafiBuilder().setAfiSafiName(Ipv6Flow.class).build()),
            IP, null, AS, PeerType.INTERNAL, role, TIMER, !active);
        assertEquals(expectedNeighbor, neighbor);
    }

    @Test
    public void testApply2() {
        final boolean active = false;
        final PeerRole role = PeerRole.Ebgp;
        final Neighbor neighbor = this.neighborProvider.apply(createConfiguration(new InstanceConfigurationIdentifier("instanceName"),
            IP, PORT, TIMER, role, active,
            Lists.<BgpTableType>newArrayList(new BgpTableTypeImpl(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class)),
            AS, Optional.of(new Rfc2385Key(PASSWORD)) ));
        final Neighbor expectedNeighbor = createNeighbor(Lists.<AfiSafi>newArrayList(new AfiSafiBuilder().setAfiSafiName(Ipv6Flow.class).build()),
            IP, PASSWORD, AS, PeerType.EXTERNAL, role, TIMER, !active);
        assertEquals(expectedNeighbor, neighbor);
    }

    @Test
    public void testApply3() {
        final boolean active = true;
        final PeerRole role = PeerRole.Internal;
        final Neighbor neighbor = this.neighborProvider.apply(createConfiguration(new InstanceConfigurationIdentifier("instanceName"),
            IP, PORT, TIMER, role, active,
            Lists.<BgpTableType>newArrayList(new BgpTableTypeImpl(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class)),
            AS, Optional.of(new Rfc2385Key(PASSWORD)) ));
        final Neighbor expectedNeighbor = createNeighbor(Lists.<AfiSafi>newArrayList(new AfiSafiBuilder().setAfiSafiName(Ipv4Flow.class).build()),
            IP, PASSWORD, AS, null, role, TIMER, !active);
        assertEquals(expectedNeighbor, neighbor);
    }

    private BGPPeerInstanceConfiguration createConfiguration(final InstanceConfigurationIdentifier confId, final IpAddress ip, final PortNumber port,
        final short holdTimer, final PeerRole role, final boolean active, final List<BgpTableType> advertized, final AsNumber as, final Optional<Rfc2385Key> passwd) {
        return new BGPPeerInstanceConfiguration(confId, ip, port, holdTimer, role, active, advertized, as, passwd);
    }

    private Neighbor createNeighbor(final List<AfiSafi> families, final IpAddress ip, final String passwd, final AsNumber as, final PeerType peerType, final PeerRole role, final short timer, final boolean passive) {
        return new NeighborBuilder()
        .setAfiSafis(new AfiSafisBuilder().setAfiSafi(families).build())
        .setNeighborAddress(ip)
        .setKey(new NeighborKey(ip))
        .setConfig(new ConfigBuilder().setAuthPassword(passwd).setPeerAs(as).setPeerType(peerType).build())
        .setRouteReflector(new RouteReflectorBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.route.reflector.ConfigBuilder().setRouteReflectorClient(role == PeerRole.RrClient).build()).build())
        .setTimers(new TimersBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers.ConfigBuilder().setHoldTime(BigDecimal.valueOf(timer)).build()).build())
        .setTransport(new TransportBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.transport.ConfigBuilder().setPassiveMode(passive).build()).build())
        .build();
    }
}
