/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.util;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.APPLICATION_PEER_GROUP_NAME;
import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.toAfiSafi;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflectorBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class OpenConfigUtilTest {
    private static final Long ALL_PATHS = 0L;
    private static final Long N_PATHS = 2L;
    private static final PathSelectionMode ADD_PATH_BEST_N_PATH_SELECTION = new AddPathBestNPathSelection(N_PATHS);
    private static final PathSelectionMode ADD_PATH_BEST_ALL_PATH_SELECTION = new AllPathSelection();
    private static final BgpTableType BGP_TABLE_TYPE_IPV4 = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private static final BgpTableType BGP_TABLE_TYPE_IPV6 = new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private static final AfiSafi AFISAFI_IPV4 = new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build();
    private static final String TEST = "/modules/module[type='dom-concurrent-data-broker'][name='concurrent-data-broker']";
    private static final TablesKey K4 = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    @Test
    public void testToAfiSafi() {
        assertEquals(AFISAFI_IPV4, toAfiSafi(BGP_TABLE_TYPE_IPV4).get());
        assertEquals(Optional.absent(), toAfiSafi(new BgpTableTypeImpl(L2vpnAddressFamily.class, UnicastSubsequentAddressFamily.class)));
    }

    @Test
    public void isAppNeighbor() {
        assertFalse(OpenConfigUtil.isAppNeighbor(new NeighborBuilder().setConfig(new ConfigBuilder().build()).build()));
        final Neighbor neighbor = new NeighborBuilder().setConfig(new ConfigBuilder()
            .addAugmentation(Config2.class, new Config2Builder().setPeerGroup(APPLICATION_PEER_GROUP_NAME).build())
            .build()).build();
        assertTrue(OpenConfigUtil.isAppNeighbor(neighbor));
    }

    @Test
    public void testToBgpTableType() {
        final Optional<BgpTableType> bgpTableType = OpenConfigUtil.toBgpTableType(IPV4UNICAST.class);
        assertEquals(BGP_TABLE_TYPE_IPV4, bgpTableType.get());
    }

    @Test
    public void testToAfiSafis() {
        final List<AfiSafi> afiSafis = OpenConfigUtil.toAfiSafis(Lists.newArrayList(BGP_TABLE_TYPE_IPV4), (afisafi, tableType) -> afisafi);
        Assert.assertEquals(Collections.singletonList(AFISAFI_IPV4), afiSafis);
    }

    @Test
    public void toPeerRole() {
        Neighbor neighbor = new NeighborBuilder().setConfig(new ConfigBuilder().setPeerType(PeerType.EXTERNAL).build()).build();
        PeerRole peerRoleResult = OpenConfigUtil.toPeerRole(neighbor);
        Assert.assertEquals(PeerRole.Ebgp, peerRoleResult);

        neighbor = new NeighborBuilder().setConfig(new ConfigBuilder().setPeerType(PeerType.INTERNAL).build()).build();
        peerRoleResult = OpenConfigUtil.toPeerRole(neighbor);
        Assert.assertEquals(PeerRole.Ibgp, peerRoleResult);

        neighbor = new NeighborBuilder()
            .setRouteReflector(new RouteReflectorBuilder().setConfig(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp
                .neighbor.group.route.reflector.ConfigBuilder().setRouteReflectorClient(true).build()).build()).build();
        peerRoleResult = OpenConfigUtil.toPeerRole(neighbor);
        Assert.assertEquals(PeerRole.RrClient, peerRoleResult);
    }

    @Test
    public void toPeerType() {
        Assert.assertEquals(PeerType.EXTERNAL, OpenConfigUtil.toPeerType(PeerRole.Ebgp));
        Assert.assertEquals(PeerType.INTERNAL, OpenConfigUtil.toPeerType(PeerRole.Ibgp));
        Assert.assertNull(OpenConfigUtil.toPeerType(PeerRole.Internal));
        Assert.assertEquals(PeerType.INTERNAL, OpenConfigUtil.toPeerType(PeerRole.RrClient));
    }

    @Test
    public void toNeighborAfiSafiAddPath() {
        final AfiSafi afiSafi = OpenConfigUtil.toNeighborAfiSafiAddPath(AFISAFI_IPV4, BGP_TABLE_TYPE_IPV4, Collections.emptyList());
        Assert.assertEquals(AFISAFI_IPV4, afiSafi);

        final AfiSafi afisafiIpv6Both = new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder()
                    .setSendMax(Shorts.checkedCast(ALL_PATHS)).setReceive(true).build()).build();
        final AfiSafi afiSafi6 = OpenConfigUtil.toNeighborAfiSafiAddPath(afisafiIpv6Both, BGP_TABLE_TYPE_IPV6,
            Collections.singletonList(new AddressFamiliesBuilder(BGP_TABLE_TYPE_IPV6).setSendReceive(SendReceive.Both).build()));
        Assert.assertEquals(afisafiIpv6Both, afiSafi6);

        final AfiSafi afisafiIpv6ReceiveExpected = new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder()
                    .setReceive(true).build()).build();
        final AfiSafi afiSafi6ReceiveResult = OpenConfigUtil.toNeighborAfiSafiAddPath(afisafiIpv6ReceiveExpected, BGP_TABLE_TYPE_IPV6,
            Collections.singletonList(new AddressFamiliesBuilder(BGP_TABLE_TYPE_IPV6).setSendReceive(SendReceive.Receive).build()));
        Assert.assertEquals(afisafiIpv6ReceiveExpected, afiSafi6ReceiveResult);

        final AfiSafi afisafiIpv6SendExpected = new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder()
                    .setSendMax(Shorts.checkedCast(ALL_PATHS)).setReceive(false).build()).build();
        final AfiSafi afiSafi6SendResult = OpenConfigUtil.toNeighborAfiSafiAddPath(afisafiIpv6ReceiveExpected, BGP_TABLE_TYPE_IPV6,
            Collections.singletonList(new AddressFamiliesBuilder(BGP_TABLE_TYPE_IPV6).setSendReceive(SendReceive.Send).build()));
        Assert.assertEquals(afisafiIpv6SendExpected, afiSafi6SendResult);
    }

    @Test
    public void toGlobalAfiSafiAddPath() {
        final AfiSafi afiSafi = OpenConfigUtil.toGlobalAfiSafiAddPath(AFISAFI_IPV4, BGP_TABLE_TYPE_IPV4, Collections.emptyMap());
        Assert.assertEquals(AFISAFI_IPV4, afiSafi);

        final AfiSafi afiSafiResult = OpenConfigUtil.toGlobalAfiSafiAddPath(AFISAFI_IPV4, BGP_TABLE_TYPE_IPV4, Collections.singletonMap(K4,
            ADD_PATH_BEST_N_PATH_SELECTION));
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2 addPath =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2Builder()
                .setSendMax(Shorts.checkedCast(N_PATHS)).setReceive(true).build();
        final AfiSafi afisafiIpv4Psm2 = new AfiSafiBuilder(afiSafi)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2.class,
                addPath).build();
        Assert.assertEquals(afisafiIpv4Psm2, afiSafiResult);

        final AfiSafi afiSafiAllResult = OpenConfigUtil.toGlobalAfiSafiAddPath(AFISAFI_IPV4, BGP_TABLE_TYPE_IPV4,
            Collections.singletonMap(K4, ADD_PATH_BEST_ALL_PATH_SELECTION));
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2 addPathAll =
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2Builder()
                .setSendMax(Shorts.checkedCast(ALL_PATHS)).setReceive(true).build();
        final AfiSafi afiAll = new AfiSafiBuilder(afiSafi)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2.class,
                addPathAll).build();
        Assert.assertEquals(afiAll, afiSafiAllResult);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<OpenConfigUtil> c = OpenConfigUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}