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

import com.google.common.collect.ImmutableMap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerMessagesState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;

public class AddPathAllPathsTest extends AbstractAddPathTest {
    private RIBImpl ribImpl;
    private Channel serverChannel;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(TABLES_KEY, new AllPathSelection());

        this.ribImpl = new RIBImpl(new RibId("test-rib"), AS_NUMBER, BGP_ID, null,
                this.ribExtension, this.serverDispatcher, this.mappingService.getCodecFactory(),
            getDomBroker(), TABLES_TYPE, pathTables, this.ribExtension.getClassLoadingStrategy());

        this.ribImpl.instantiateServiceInstance();
        this.ribImpl.onGlobalContextUpdated(this.schemaContext);
        final ChannelFuture channelFuture = this.serverDispatcher.createServer(new InetSocketAddress(RIB_ID, PORT));
        waitFutureSuccess(channelFuture);
        this.serverChannel = channelFuture.channel();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        waitFutureSuccess(this.serverChannel.close());
        super.tearDown();
    }
    /*
     * All-Paths
     *                                            ___________________
     *                                           | ODL BGP 127.0.0.1 |
     * [peer://127.0.0.2; p1, lp100] --(iBGP)--> |                   | --(RR-client, non add-path) --> [Peer://127.0.0.5; (p1, lp100), (p1, lp1200)]
     * [peer://127.0.0.3; p1, lp200] --(iBGP)--> |                   |
     * [peer://127.0.0.4; p1, lp50] --(iBGP)-->  |                   | --(RR-client, add-path) --> [Peer://127.0.0.6; (p1, path-id1, lp100), (p1, path-id2, pl50), (p1, path-id3, pl200), (p1, path-id4, pl20)]
     * [peer://127.0.0.2; p1, lp20] --(iBGP)-->  |___________________|
     * p1 = 1.1.1.1/32
     */
    @Test
    public void testUseCase1() throws Exception {
        final BgpParameters nonAddPathParams = createParameter(false);
        final BgpParameters addPathParams = createParameter(true);

        final BGPPeer peer1 = configurePeer(PEER1, this.ribImpl, nonAddPathParams, PeerRole.Ibgp, this.serverRegistry);
        final BGPSessionImpl session1 = createPeerSession(PEER1, nonAddPathParams, new SimpleSessionListener());

        configurePeer(PEER2, this.ribImpl, nonAddPathParams, PeerRole.Ibgp, this.serverRegistry);
        final BGPSessionImpl session2 = createPeerSession(PEER2, nonAddPathParams, new SimpleSessionListener());

        configurePeer(PEER3, this.ribImpl, nonAddPathParams, PeerRole.Ibgp, this.serverRegistry);
        final BGPSessionImpl session3 = createPeerSession(PEER3, nonAddPathParams, new SimpleSessionListener());

        final SimpleSessionListener listener4 = new SimpleSessionListener();
        final BGPPeer peer4 = configurePeer(PEER4, this.ribImpl, nonAddPathParams, PeerRole.RrClient, this.serverRegistry);

        BGPPeerState peer4State = peer4.getPeerState();
        assertNull(peer4State.getGroupId());
        assertEquals(new IpAddress(PEER4), peer4State.getNeighborAddress());
        assertEquals(0L, peer4State.getTotalPathsCount());
        assertEquals(0L, peer4State.getTotalPrefixes());

        assertNull(peer4State.getBGPTimersState());
        assertNull(peer4State.getBGPTransportState());
        assertNull(peer4State.getBGPSessionState());
        assertEquals(0L, peer4State.getBGPErrorHandlingState().getErroneousUpdateReceivedCount());

        BGPGracelfulRestartState gracefulRestart = peer4State.getBGPGracelfulRestart();
        assertFalse(gracefulRestart.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(gracefulRestart.isGracefulRestartReceived(TABLES_KEY));
        assertFalse(gracefulRestart.isLocalRestarting());
        assertFalse(gracefulRestart.isPeerRestarting());
        assertEquals(0L, gracefulRestart.getPeerRestartTime());

        BGPAfiSafiState afiSafiState = peer4State.getBGPAfiSafiState();
        assertEquals(AFI_SAFIS_ADVERTIZED, afiSafiState.getAfiSafisAdvertized());
        assertEquals(Collections.emptySet(), afiSafiState.getAfiSafisReceived());
        assertEquals(0L, afiSafiState.getPrefixesSentCount(TABLES_KEY));
        assertEquals(0L, afiSafiState.getPrefixesReceivedCount(TABLES_KEY));
        assertEquals(0L, afiSafiState.getPrefixesInstalledCount(TABLES_KEY));

        assertFalse(afiSafiState.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(afiSafiState.isGracefulRestartAdvertized(TABLES_KEY));
        assertFalse(afiSafiState.isLocalRestarting());
        assertFalse(afiSafiState.isPeerRestarting());
        assertFalse(afiSafiState.isAfiSafiSupported(TABLES_KEY));

        final BGPSessionImpl session4 = createPeerSession(PEER4, nonAddPathParams, listener4);

        final SimpleSessionListener listener5 = new SimpleSessionListener();
        configurePeer(PEER5, this.ribImpl, addPathParams, PeerRole.RrClient, this.serverRegistry);
        final BGPSessionImpl session5 = createPeerSession(PEER5, addPathParams, listener5);
        checkPeersPresentOnDataStore(5);

        //the best route
        sendRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 1);
        checkReceivedMessages(listener4, 1);
        checkReceivedMessages(listener5, 1);
        assertEquals(UPD_100, listener5.getListMsg().get(0));

        final BGPPeerState peer1State = peer1.getPeerState();
        assertNull(peer1State.getGroupId());
        assertEquals(new IpAddress(PEER1), peer1State.getNeighborAddress());
        assertEquals(1L, peer1State.getTotalPathsCount());
        assertEquals(1L, peer1State.getTotalPrefixes());

        final BGPTimersState timerStatePeer1 = peer1State.getBGPTimersState();
        assertEquals(HOLDTIMER, timerStatePeer1.getNegotiatedHoldTime());
        assertTrue(timerStatePeer1.getUpTime() > 0L);

        final BGPTransportState transportStatePeer1 = peer1State.getBGPTransportState();
        assertEquals(new PortNumber(PORT), transportStatePeer1.getLocalPort());
        assertEquals(new IpAddress(PEER1), transportStatePeer1.getRemoteAddress());

        assertEquals(State.UP, peer1State.getBGPSessionState().getSessionState());
        checkEquals(()-> assertEquals(1L, peer1State.getBGPPeerMessagesState().getUpdateMessagesReceivedCount()));
        checkEquals(()-> assertEquals(0L, peer1State.getBGPPeerMessagesState().getUpdateMessagesSentCount()));

        final BGPSessionState sessionStatePeer1 = peer1State.getBGPSessionState();
        assertFalse(sessionStatePeer1.isAddPathCapabilitySupported());
        assertFalse(sessionStatePeer1.isAsn32CapabilitySupported());
        assertFalse(sessionStatePeer1.isGracefulRestartCapabilitySupported());
        assertTrue(sessionStatePeer1.isMultiProtocolCapabilitySupported());
        assertFalse(sessionStatePeer1.isRouterRefreshCapabilitySupported());

        final BGPAfiSafiState afiSafiStatePeer1 = peer1State.getBGPAfiSafiState();
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

        final BGPRIBState ribState = this.ribImpl.getRIBState();
        assertEquals(1, ribState.getPathsCount().size());
        assertEquals(1L,  ribState.getPrefixesCount().size());
        assertEquals(BGP_ID, ribState.getRouteId());
        assertEquals(AS_NUMBER, ribState.getAs());
        assertEquals(1L, ribState.getPathCount(TABLES_KEY));
        assertEquals(1L, ribState.getPrefixesCount(TABLES_KEY));
        assertEquals(1L, ribState.getTotalPathsCount());
        assertEquals(1L, ribState.getTotalPrefixesCount());

        final SimpleSessionListener listener6 = new SimpleSessionListener();
        final BGPPeer peer6 = configurePeer(PEER6, this.ribImpl, nonAddPathParams, PeerRole.RrClient, this.serverRegistry);
        final BGPSessionImpl session6 = createPeerSession(PEER6, nonAddPathParams, listener6);
        checkPeersPresentOnDataStore(6);
        checkReceivedMessages(listener6, 1);
        assertEquals(UPD_NA_100, listener6.getListMsg().get(0));
        causeBGPError(session6);
        checkEquals(()-> assertEquals(1L, peer6.getPeerState().getBGPPeerMessagesState().getNotificationMessagesSentCount()));

        checkPeersPresentOnDataStore(5);

        //the second best route
        sendRouteAndCheckIsOnLocRib(session2, PREFIX1, 50, 2);
        checkReceivedMessages(listener4, 1);
        checkReceivedMessages(listener5, 2);
        assertEquals(UPD_50, listener5.getListMsg().get(1));

        //new best route
        sendRouteAndCheckIsOnLocRib(session3, PREFIX1, 200, 3);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 3);
        assertEquals(UPD_200, listener5.getListMsg().get(2));

        peer4State = peer4.getPeerState();
        assertNull(peer4State.getGroupId());
        assertEquals(new IpAddress(PEER4), peer4State.getNeighborAddress());
        assertEquals(0L, peer4State.getTotalPathsCount());
        assertEquals(0L, peer4State.getTotalPrefixes());

        final BGPTimersState timerState = peer4State.getBGPTimersState();
        assertEquals(HOLDTIMER, timerState.getNegotiatedHoldTime());
        assertTrue(timerState.getUpTime() > 0L);

        final BGPTransportState transportState = peer4State.getBGPTransportState();
        assertEquals(new PortNumber(PORT), transportState.getLocalPort());
        assertEquals(new IpAddress(PEER4), transportState.getRemoteAddress());

        final BGPPeerMessagesState peerMessagesState = peer4State.getBGPPeerMessagesState();
        assertEquals(0L, peerMessagesState.getNotificationMessagesReceivedCount());
        assertEquals(0L, peerMessagesState.getNotificationMessagesSentCount());
        assertEquals(0L, peerMessagesState.getUpdateMessagesReceivedCount());
        assertEquals(2L, peerMessagesState.getUpdateMessagesSentCount());

        final BGPSessionState bgpSessionState = peer4State.getBGPSessionState();
        assertEquals(State.UP, bgpSessionState.getSessionState());
        assertFalse(bgpSessionState.isAddPathCapabilitySupported());
        assertFalse(bgpSessionState.isAsn32CapabilitySupported());
        assertFalse(bgpSessionState.isGracefulRestartCapabilitySupported());
        assertTrue(bgpSessionState.isMultiProtocolCapabilitySupported());
        assertFalse(bgpSessionState.isRouterRefreshCapabilitySupported());

        final BGPErrorHandlingState errorHandling = peer4State.getBGPErrorHandlingState();
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
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 4);
        assertEquals(UPD_200.getAttributes().getLocalPref(), ((Update) listener4.getListMsg().get(1)).getAttributes().getLocalPref());
        assertEquals(UPD_20, listener5.getListMsg().get(3));

        //withdraw second best route, 1 advertisement(1 withdrawal) for add-path supported, none for non add path
        sendWithdrawalRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 2);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 5);

        //we advertise again to try new test
        sendRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 3);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 6);
        assertEquals(UPD_200, listener5.getListMsg().get(2));

        //withdraw second best route, 1 advertisement(1 withdrawal) for add-path supported, 1 for non add path (withdrawal)
        sendWithdrawalRouteAndCheckIsOnLocRib(session3, PREFIX1, 200, 2);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 7);

        sendNotification(session1);
        checkEquals(()-> assertEquals(1L, peer1.getPeerState().getBGPPeerMessagesState().getNotificationMessagesReceivedCount()));
        session1.close();
        session2.close();
        session3.close();
        session4.close();
        session5.close();
    }
}
