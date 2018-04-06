/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.rib.impl.config.AbstractConfig.AS;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.NEIGHBOR_ADDRESS;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createAddPath;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createAfiSafi;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createConfig;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createNeighborExpected;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createTimers;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createTransport;

import com.google.common.primitives.Shorts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.NeighborsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.GlobalAddPathsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.GlobalAddPathsConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;

final class RIBTestsUtil {
    private static final Ipv4Address BGP_ID = new BgpId(new Ipv4Address("127.0.0.1"));
    private static final List<AfiSafi> AFISAFIS_IPV4 = new ArrayList<>();
    private static final List<AfiSafi> AFISAFIS_IPV6 = new ArrayList<>();
    private static final Long ALL_PATHS = 0L;

    static {
        AFISAFIS_IPV4.add(new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
                .addAugmentation(GlobalAddPathsConfig.class, new GlobalAddPathsConfigBuilder().setReceive(true)
                        .setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
    }

    static {
        AFISAFIS_IPV6.add(new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class)
                .addAugmentation(GlobalAddPathsConfig.class, new GlobalAddPathsConfigBuilder().setReceive(true)
                        .setSendMax(Shorts.checkedCast(ALL_PATHS)).build()).build());
    }

    private RIBTestsUtil() {
        throw new UnsupportedOperationException();
    }


    public static Global createGlobalIpv4() {
        return new GlobalBuilder()
                .setAfiSafis(new AfiSafisBuilder().setAfiSafi(AFISAFIS_IPV4).build())
                .setConfig(new ConfigBuilder().setAs(AS).setRouterId(BGP_ID).build())
                .setState(new StateBuilder().setAs(AS).build())
                .build();
    }

    public static Global createGlobalIpv6() {
        return new GlobalBuilder()
                .setAfiSafis(new AfiSafisBuilder().setAfiSafi(AFISAFIS_IPV6).build())
                .setConfig(new ConfigBuilder().setAs(AS).setRouterId(BGP_ID).build())
                .setState(new StateBuilder().setAs(AS).build())
                .build();
    }

    public static Neighbors createNeighbors() {
        return new NeighborsBuilder()
                .setNeighbor(Collections.singletonList(createNeighbor()))
                .build();
    }

    private static Neighbor createNeighbor() {
        return createNeighborExpected(NEIGHBOR_ADDRESS);
    }

    public static Neighbors createNeighborsNoRR() {
        return new NeighborsBuilder()
                .setNeighbor(Collections.singletonList(createNeighborNoRR()))
                .build();
    }

    private static Neighbor createNeighborNoRR() {
        return new NeighborBuilder()
                .setAfiSafis(createAfiSafi())
                .setConfig(createConfig())
                .setNeighborAddress(NEIGHBOR_ADDRESS)
                .setTimers(createTimers())
                .setTransport(createTransport())
                .setAddPaths(createAddPath())
                .build();
    }
}
