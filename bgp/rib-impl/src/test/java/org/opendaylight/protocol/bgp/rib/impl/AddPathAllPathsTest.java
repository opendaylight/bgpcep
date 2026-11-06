/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.util.CheckUtil.checkEquals;
import static org.opendaylight.protocol.util.CheckUtil.checkReceivedMessages;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.spi.PathSelectionModes;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

// FIXME: Migrate to JUnit5 when MDSAL is migrated or rework the test
public class AddPathAllPathsTest extends AbstractAddPathTest {
    private RIBImpl ribImpl;
    private Channel serverChannel;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        ribImpl = new RIBImpl(tableRegistry, new RibId("test-rib"), AS_NUMBER, BGP_ID,
                ribExtension, serverDispatcher, codecsRegistry,
            getDomBroker(), policies, TABLES_TYPE, Map.of(TABLES_KEY, PathSelectionModes.addAllPaths()));

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
     * All-Paths
     *                                            ___________________
     *                                           | ODL BGP 127.0.0.1 |
     * [peer://127.0.0.2; p1, lp100] --(iBGP)--> |                   | --(RR-client, non add-path) -->
     * [peer://127.0.0.3; p1, lp200] --(iBGP)--> |                   |     [Peer://127.0.0.5; (p1, lp100), (p1, lp1200)]
     * [peer://127.0.0.4; p1, lp50] --(iBGP)-->  |                   | --(RR-client, add-path) -->
     * [peer://127.0.0.2; p1, lp20] --(iBGP)-->  |___________________|     [Peer://127.0.0.6; (p1, path-id1, lp100),
     * p1 = 1.1.1.1/32                                                      (p1, path-id2, pl50), (p1, path-id3, pl200),
     *                                                                      (p1, path-id4, pl20)]
     */
    @Test
    public void testUseCase1() throws Exception {
        final var nonAddPathParams = createParameter(false);
        final var addPathParams = createParameter(true);

        final var peer1 = configurePeer(tableRegistry, PEER1, ribImpl, nonAddPathParams, PeerRole.Ibgp, serverRegistry);
        final var session1 = createPeerSession(PEER1, nonAddPathParams, new SimpleSessionListener());

        configurePeer(tableRegistry, PEER2, ribImpl, nonAddPathParams, PeerRole.Ibgp, serverRegistry);
        final var session2 = createPeerSession(PEER2, nonAddPathParams, new SimpleSessionListener());

        configurePeer(tableRegistry, PEER3, ribImpl, nonAddPathParams, PeerRole.Ibgp, serverRegistry);
        final var session3 = createPeerSession(PEER3, nonAddPathParams, new SimpleSessionListener());

        final var listener4 = new SimpleSessionListener();
        final var peer4 = configurePeer(tableRegistry, PEER4, ribImpl, nonAddPathParams, PeerRole.RrClient,
            serverRegistry);

        var peer4State = peer4.getPeerState();
        assertNull(peer4State.getGroupId());
        assertEquals(new IpAddressNoZone(PEER4), peer4State.getNeighborAddress());
        assertEquals(0L, peer4State.getTotalPathsCount());
        assertEquals(0L, peer4State.getTotalPrefixes());

        assertNull(peer4State.getBGPTimersState());
        assertNull(peer4State.getBGPTransportState());
        assertNull(peer4State.getBGPSessionState());
        assertEquals(0L, peer4State.getBGPErrorHandlingState().getErroneousUpdateReceivedCount());

        var gracefulRestart = peer4State.getBGPGracelfulRestart();
        assertFalse(gracefulRestart.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(gracefulRestart.isGracefulRestartReceived(TABLES_KEY));
        assertFalse(gracefulRestart.isLocalRestarting());
        assertFalse(gracefulRestart.isPeerRestarting());
        assertEquals(0L, gracefulRestart.getPeerRestartTime());

        var afiSafiState = peer4State.getBGPAfiSafiState();
        assertEquals(AFI_SAFIS_ADVERTIZED, afiSafiState.getAfiSafisAdvertized());
        assertEquals(Set.of(), afiSafiState.getAfiSafisReceived());
        assertEquals(0L, afiSafiState.getPrefixesSentCount(TABLES_KEY));
        assertEquals(0L, afiSafiState.getPrefixesReceivedCount(TABLES_KEY));
        assertEquals(0L, afiSafiState.getPrefixesInstalledCount(TABLES_KEY));

        assertFalse(afiSafiState.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(afiSafiState.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(afiSafiState.isLocalRestarting());
        assertFalse(afiSafiState.isPeerRestarting());
        assertFalse(afiSafiState.isAfiSafiSupported(TABLES_KEY));

        final var session4 = createPeerSession(PEER4, nonAddPathParams, listener4);

        final var listener5 = new SimpleSessionListener();
        configurePeer(tableRegistry, PEER5, ribImpl, addPathParams, PeerRole.RrClient, serverRegistry);
        final var session5 = createPeerSession(PEER5, addPathParams, listener5);
        checkPeersPresentOnDataStore(5);

        //the best route
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

        final var peer1State = peer1.getPeerState();
        assertNull(peer1State.getGroupId());
        assertEquals(new IpAddressNoZone(PEER1), peer1State.getNeighborAddress());
        assertEquals(1L, peer1State.getTotalPathsCount());
        assertEquals(1L, peer1State.getTotalPrefixes());

        final var timerStatePeer1 = peer1State.getBGPTimersState();
        assertEquals(HOLDTIMER, timerStatePeer1.getNegotiatedHoldTime());
        assertTrue(timerStatePeer1.getUpTime() > 0L);

        final var transportStatePeer1 = peer1State.getBGPTransportState();
        assertEquals(new PortNumber(PORT), transportStatePeer1.getLocalPort());
        assertEquals(new IpAddressNoZone(PEER1), transportStatePeer1.getRemoteAddress());

        assertEquals(State.UP, peer1State.getBGPSessionState().getSessionState());
        checkEquals(() -> assertEquals(1L, peer1State.getBGPPeerMessagesState().getUpdateMessagesReceivedCount()));
        checkEquals(() -> assertEquals(1L, peer1State.getBGPPeerMessagesState().getUpdateMessagesSentCount()));

        final var sessionStatePeer1 = peer1State.getBGPSessionState();
        assertFalse(sessionStatePeer1.isAddPathCapabilitySupported());
        assertFalse(sessionStatePeer1.isAsn32CapabilitySupported());
        assertFalse(sessionStatePeer1.isGracefulRestartCapabilitySupported());
        assertTrue(sessionStatePeer1.isMultiProtocolCapabilitySupported());
        assertFalse(sessionStatePeer1.isRouterRefreshCapabilitySupported());

        final var afiSafiStatePeer1 = peer1State.getBGPAfiSafiState();
        assertEquals(AFI_SAFIS_ADVERTIZED, afiSafiStatePeer1.getAfiSafisAdvertized());
        assertEquals(AFI_SAFIS_ADVERTIZED, afiSafiStatePeer1.getAfiSafisReceived());
        assertEquals(0L, afiSafiStatePeer1.getPrefixesSentCount(TABLES_KEY));
        assertEquals(1L, afiSafiStatePeer1.getPrefixesReceivedCount(TABLES_KEY));
        assertEquals(1L, afiSafiStatePeer1.getPrefixesInstalledCount(TABLES_KEY));

        assertFalse(afiSafiStatePeer1.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(afiSafiStatePeer1.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(afiSafiStatePeer1.isLocalRestarting());
        assertFalse(afiSafiStatePeer1.isPeerRestarting());
        assertTrue(afiSafiStatePeer1.isAfiSafiSupported(TABLES_KEY));

        final var ribState = ribImpl.getRIBState();
        assertEquals(1, ribState.getPathsCount().size());
        assertEquals(1L,  ribState.getTablesPrefixesCount().size());
        assertEquals(BGP_ID, ribState.getRouteId());
        assertEquals(AS_NUMBER, ribState.getAs());
        assertEquals(1L, ribState.getPathCount(TABLES_KEY));
        assertEquals(1L, ribState.getPrefixesCount(TABLES_KEY));
        assertEquals(1L, ribState.getTotalPathsCount());
        assertEquals(1L, ribState.getTotalPrefixesCount());

        final var listener6 = new SimpleSessionListener();
        final var peer6 = configurePeer(tableRegistry, PEER6, ribImpl, nonAddPathParams, PeerRole.RrClient,
            serverRegistry);
        final var session6 = createPeerSession(PEER6, nonAddPathParams, listener6);
        checkPeersPresentOnDataStore(6);
        checkReceivedMessages(listener6, 2);
        assertEquals(UPD_NA_100, listener6.getListMsg().get(1));
        causeBGPError(session6);
        checkEquals(() -> assertEquals(1L,
            peer6.getPeerState().getBGPPeerMessagesState().getNotificationMessagesSentCount()));

        checkPeersPresentOnDataStore(5);

        //the second best route
        sendRouteAndCheckIsOnLocRib(session2, PREFIX1, 50, 2);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 3);
        assertEquals(UPD_50, listener5.getListMsg().get(2));

        //new best route
        sendRouteAndCheckIsOnLocRib(session3, PREFIX1, 200, 3);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 4);
        assertEquals(UPD_200, listener5.getListMsg().get(3));

        peer4State = peer4.getPeerState();
        assertNull(peer4State.getGroupId());
        assertEquals(new IpAddressNoZone(PEER4), peer4State.getNeighborAddress());
        assertEquals(0L, peer4State.getTotalPathsCount());
        assertEquals(0L, peer4State.getTotalPrefixes());

        final var timerState = peer4State.getBGPTimersState();
        assertEquals(HOLDTIMER, timerState.getNegotiatedHoldTime());
        assertTrue(timerState.getUpTime() > 0L);

        final var transportState = peer4State.getBGPTransportState();
        assertEquals(new PortNumber(PORT), transportState.getLocalPort());
        assertEquals(new IpAddressNoZone(PEER4), transportState.getRemoteAddress());

        final var peerMessagesState = peer4State.getBGPPeerMessagesState();
        assertEquals(0L, peerMessagesState.getNotificationMessagesReceivedCount());
        assertEquals(0L, peerMessagesState.getNotificationMessagesSentCount());
        assertEquals(0L, peerMessagesState.getUpdateMessagesReceivedCount());
        assertEquals(3L, peerMessagesState.getUpdateMessagesSentCount());

        final var bgpSessionState = peer4State.getBGPSessionState();
        assertEquals(State.UP, bgpSessionState.getSessionState());
        assertFalse(bgpSessionState.isAddPathCapabilitySupported());
        assertFalse(bgpSessionState.isAsn32CapabilitySupported());
        assertFalse(bgpSessionState.isGracefulRestartCapabilitySupported());
        assertTrue(bgpSessionState.isMultiProtocolCapabilitySupported());
        assertFalse(bgpSessionState.isRouterRefreshCapabilitySupported());

        final var errorHandling = peer4State.getBGPErrorHandlingState();
        assertEquals(0L, errorHandling.getErroneousUpdateReceivedCount());

        gracefulRestart = peer4State.getBGPGracelfulRestart();
        assertFalse(gracefulRestart.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(gracefulRestart.isGracefulRestartReceived(TABLES_KEY));
        assertFalse(gracefulRestart.isLocalRestarting());
        assertFalse(gracefulRestart.isPeerRestarting());
        assertEquals(0L, gracefulRestart.getPeerRestartTime());

        afiSafiState = peer4State.getBGPAfiSafiState();
        assertEquals(AFI_SAFIS_ADVERTIZED, afiSafiState.getAfiSafisAdvertized());
        assertEquals(AFI_SAFIS_ADVERTIZED, afiSafiState.getAfiSafisReceived());
        assertEquals(2L, afiSafiState.getPrefixesSentCount(TABLES_KEY));
        assertEquals(0L, afiSafiState.getPrefixesReceivedCount(TABLES_KEY));
        assertEquals(0L, afiSafiState.getPrefixesInstalledCount(TABLES_KEY));

        assertFalse(afiSafiState.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(afiSafiState.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(afiSafiState.isLocalRestarting());
        assertFalse(afiSafiState.isPeerRestarting());
        assertTrue(afiSafiState.isAfiSafiSupported(TABLES_KEY));

        //the worst route
        sendRouteAndCheckIsOnLocRib(session1, PREFIX1, 20, 3);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 5);
        assertEquals(UPD_200.getAttributes().getLocalPref(),
                ((Update) listener4.getListMsg().get(2)).getAttributes().getLocalPref());
        assertEquals(UPD_20, listener5.getListMsg().get(4));

        //withdraw second best route, 1 advertisement(1 withdrawal) for add-path supported, none for non add path
        sendWithdrawalRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 2);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 6);

        //we advertise again to try new test
        sendRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 3);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 7);
        assertEquals(UPD_200, listener5.getListMsg().get(3));

        // withdraw second best route, 1 advertisement(1 withdrawal) for add-path supported, 1 for non add path
        // (withdrawal)
        sendWithdrawalRouteAndCheckIsOnLocRib(session3, PREFIX1, 200, 2);
        checkReceivedMessages(listener4, 4);
        checkReceivedMessages(listener5, 8);

        sendNotification(session1);
        checkEquals(() -> assertEquals(1L,
                peer1.getPeerState().getBGPPeerMessagesState().getNotificationMessagesReceivedCount()));
        session1.close();
        session2.close();
        session3.close();
        session4.close();
        session5.close();
    }
}
