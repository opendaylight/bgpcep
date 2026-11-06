/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.util.CheckUtil.checkReceivedMessages;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.spi.PathSelectionModes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

// FIXME: Migrate to JUnit5 when MDSAL is migrated or rework the test
public class AddPathNPathsTest extends AbstractAddPathTest {
    private RIBImpl ribImpl;
    private Channel serverChannel;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        ribImpl = new RIBImpl(tableRegistry, new RibId("test-rib"), AS_NUMBER, new BgpId(RIB_ID), ribExtension,
            serverDispatcher, codecsRegistry, getDomBroker(), policies, TABLES_TYPE,
            Map.of(new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE),
                PathSelectionModes.addNPaths(2)));

        ribImpl.instantiateServiceInstance();
        final var channelFuture = serverDispatcher.createServer(new InetSocketAddress(RIB_ID, PORT.toJava()));
        waitFutureSuccess(channelFuture);
        serverChannel = channelFuture.channel();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        waitFutureSuccess(serverChannel.close());
        super.tearDown();
    }

    /*
     * N-Paths
     *                                            ___________________
     *                                           | ODL BGP 127.0.0.1 |
     * [peer://127.0.0.2; p1, lp100] --(iBGP)--> |                   | --(RR-client, non add-path) -->
     * [peer://127.0.0.3; p1, lp200] --(iBGP)--> |                   |     [Peer://127.0.0.5; (p1, lp100), (p1, lp1200)]
     * [peer://127.0.0.4; p1, lp50] --(iBGP)-->  |                   | --(RR-client, add-path) -->
     * [peer://127.0.0.2; p1, lp20] --(iBGP)-->  |___________________|     [Peer://127.0.0.6; (p1, path-id1, lp100),
     * p1 = 1.1.1.1/32                                                      (p1, path-id2, pl50), (p1, path-id3, pl200)]
     */
    @Test
    public void testUseCase1() throws Exception {
        final var nonAddPathParams = createParameter(false);
        final var addPathParams = createParameter(true);

        configurePeer(tableRegistry, PEER1, ribImpl, nonAddPathParams, PeerRole.Ibgp, serverRegistry);
        final var session1 = createPeerSession(PEER1, nonAddPathParams, new SimpleSessionListener());

        configurePeer(tableRegistry, PEER2, ribImpl, nonAddPathParams, PeerRole.Ibgp, serverRegistry);
        final var session2 = createPeerSession(PEER2, nonAddPathParams, new SimpleSessionListener());

        configurePeer(tableRegistry, PEER3, ribImpl, nonAddPathParams, PeerRole.Ibgp, serverRegistry);
        final var session3 = createPeerSession(PEER3, nonAddPathParams, new SimpleSessionListener());

        final var listener4 = new SimpleSessionListener();
        configurePeer(tableRegistry, PEER4, ribImpl, nonAddPathParams, PeerRole.RrClient, serverRegistry);
        final var session4 = createPeerSession(PEER4, nonAddPathParams, listener4);

        final var listener5 = new SimpleSessionListener();
        configurePeer(tableRegistry, PEER5, ribImpl, addPathParams, PeerRole.RrClient, serverRegistry);
        final var session5 = createPeerSession(PEER5, addPathParams, listener5);
        checkPeersPresentOnDataStore(5);

        //new best route so far
        sendRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 1);

        final var eot = new UpdateBuilder().build();
        assertEquals(eot, awaitReceived(listener4, 1).getFirst());
        assertEquals(new UpdateBuilder()
            .setAttributes(new AttributesBuilder()
                .setAsPath(new AsPathBuilder().build())
                .setClusterId(new ClusterIdBuilder().setCluster(List.of(CLUSTER_ID)).build())
                .setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(100)).build())
                .setMultiExitDisc(new MultiExitDiscBuilder().setMed(Uint32.ZERO).build())
                .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
                .setOriginatorId(new OriginatorIdBuilder().setOriginator(CLUSTER_ID).build())
                .addAugmentation(new AttributesReachBuilder()
                    .setMpReachNlri(new MpReachNlriBuilder()
                        .setAfi(Ipv4AddressFamily.VALUE)
                        .setSafi(UnicastSubsequentAddressFamily.VALUE)
                        .setCNextHop(new Ipv4NextHopCaseBuilder()
                            .setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(NH1).build())
                            .build())
                        .setAdvertizedRoutes(new AdvertizedRoutesBuilder()
                            .setDestinationType(new DestinationIpv4CaseBuilder()
                                .setDestinationIpv4(new DestinationIpv4Builder()
                                    .setIpv4Prefixes(List.of(new Ipv4PrefixesBuilder().setPrefix(PREFIX1).build()))
                                    .build())
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build(), awaitReceived(listener4, 2).getLast());

        assertEquals(eot, awaitReceived(listener5, 1).getFirst());
        assertEquals(UPD_100, awaitReceived(listener5, 2).getLast());

        final var listener6 = new SimpleSessionListener();
        configurePeer(tableRegistry, PEER6, ribImpl, nonAddPathParams, PeerRole.RrClient, serverRegistry);
        final var session6 = createPeerSession(PEER6, nonAddPathParams, listener6);
        checkPeersPresentOnDataStore(6);
        checkReceivedMessages(listener6, 2);
        assertEquals(UPD_NA_100, listener6.getListMsg().get(1));
        session6.close();
        checkPeersPresentOnDataStore(5);

        //the second best route
        sendRouteAndCheckIsOnLocRib(session2, PREFIX1, 50, 2);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 3);
        assertEquals(UPD_50, listener5.getListMsg().get(2));

        //new best route
        sendRouteAndCheckIsOnLocRib(session3, PREFIX1, 200, 2);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 4);
        assertEquals(UPD_200, listener5.getListMsg().get(3));

        //the worst prefix, no changes
        sendRouteAndCheckIsOnLocRib(session2, PREFIX1, 20, 2);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 4);

        //withdraw second best route, 2 advertisement (1 withdrawal) for add-path supported, none for non add path
        sendWithdrawalRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 2);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 6);

        //we advertise again to try new test
        sendRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 2);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 7);

        //withdraw second best route, 2 advertisement (1 withdrawal) for add-path supported,
        // 1 withdrawal for non add path
        sendWithdrawalRouteAndCheckIsOnLocRib(session3, PREFIX1, 200, 2);
        checkReceivedMessages(listener4, 4);
        checkReceivedMessages(listener5, 9);

        session1.close();
        session2.close();
        session3.close();
        session4.close();
        session5.close();
    }
}
