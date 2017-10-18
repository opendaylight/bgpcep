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

import com.google.common.collect.ImmutableMap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class AddPathNPathsTest extends AbstractAddPathTest {
    private RIBImpl ribImpl;
    private Channel serverChannel;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final TablesKey tk = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(tk, new AddPathBestNPathSelection(2L));

        this.ribImpl = new RIBImpl(this.clusterSingletonServiceProvider, new RibId("test-rib"),
            AS_NUMBER, new BgpId(RIB_ID), null, this.ribExtension, this.serverDispatcher,
            this.mappingService.getCodecFactory(), getDomBroker(), TABLES_TYPE, pathTables,
            this.ribExtension.getClassLoadingStrategy(), null);

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
     * N-Paths
     *                                            ___________________
     *                                           | ODL BGP 127.0.0.1 |
     * [peer://127.0.0.2; p1, lp100] --(iBGP)--> |                   | --(RR-client, non add-path) --> [Peer://127.0.0.5; (p1, lp100), (p1, lp1200)]
     * [peer://127.0.0.3; p1, lp200] --(iBGP)--> |                   |
     * [peer://127.0.0.4; p1, lp50] --(iBGP)-->  |                   | --(RR-client, add-path) --> [Peer://127.0.0.6; (p1, path-id1, lp100), (p1, path-id2, pl50), (p1, path-id3, pl200)]
     * [peer://127.0.0.2; p1, lp20] --(iBGP)-->  |___________________|
     * p1 = 1.1.1.1/32
     */
    @Test
    public void testUseCase1() throws Exception {
        final BgpParameters nonAddPathParams = createParameter(false);
        final BgpParameters addPathParams = createParameter(true);

        configurePeer(PEER1, this.ribImpl, nonAddPathParams, PeerRole.Ibgp, this.serverRegistry);
        final BGPSessionImpl session1 = createPeerSession(PEER1, nonAddPathParams, new SimpleSessionListener());

        configurePeer(PEER2, this.ribImpl, nonAddPathParams, PeerRole.Ibgp, this.serverRegistry);
       final BGPSessionImpl session2 = createPeerSession(PEER2, nonAddPathParams, new SimpleSessionListener());

        configurePeer(PEER3, this.ribImpl, nonAddPathParams, PeerRole.Ibgp, this.serverRegistry);
        final BGPSessionImpl session3 = createPeerSession(PEER3, nonAddPathParams, new SimpleSessionListener());

        final SimpleSessionListener listener4 = new SimpleSessionListener();
        configurePeer(PEER4, this.ribImpl, nonAddPathParams, PeerRole.RrClient, this.serverRegistry);
        final BGPSessionImpl session4 = createPeerSession(PEER4, nonAddPathParams, listener4);

        final SimpleSessionListener listener5 = new SimpleSessionListener();
        configurePeer(PEER5, this.ribImpl, addPathParams, PeerRole.RrClient, this.serverRegistry);
        final BGPSessionImpl session5 = createPeerSession(PEER5, addPathParams, listener5);
        checkPeersPresentOnDataStore(5);

        //new best route so far
        sendRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 1);
        checkReceivedMessages(listener4, 1);
        checkReceivedMessages(listener5, 1);
        assertEquals(UPD_100, listener5.getListMsg().get(0));

        final SimpleSessionListener listener6 = new SimpleSessionListener();
        configurePeer(PEER6, this.ribImpl, nonAddPathParams, PeerRole.RrClient, this.serverRegistry);
        final BGPSessionImpl session6 = createPeerSession(PEER6, nonAddPathParams, listener6);
        checkPeersPresentOnDataStore(6);
        checkReceivedMessages(listener6, 1);
        assertEquals(UPD_NA_100, listener6.getListMsg().get(0));
        session6.close();
        checkPeersPresentOnDataStore(5);

        //the second best route
        sendRouteAndCheckIsOnLocRib(session2, PREFIX1, 50, 2);
        checkReceivedMessages(listener4, 1);
        checkReceivedMessages(listener5, 2);
        assertEquals(UPD_50, listener5.getListMsg().get(1));

        //new best route
        sendRouteAndCheckIsOnLocRib(session3, PREFIX1, 200, 2);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 3);
        assertEquals(UPD_200, listener5.getListMsg().get(2));

        //the worst prefix, no changes
        sendRouteAndCheckIsOnLocRib(session2, PREFIX1, 20, 2);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 3);

        //withdraw second best route, 2 advertisement (1 withdrawal) for add-path supported, none for non add path
        sendWithdrawalRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 2);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 5);

        //we advertise again to try new test
        sendRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 2);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 6);

        //withdraw second best route, 2 advertisement (1 withdrawal) for add-path supported, 1 withdrawal for non add path
        sendWithdrawalRouteAndCheckIsOnLocRib(session3, PREFIX1, 200, 2);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 8);

        session1.close();
        session2.close();
        session3.close();
        session4.close();
        session5.close();
    }
}
