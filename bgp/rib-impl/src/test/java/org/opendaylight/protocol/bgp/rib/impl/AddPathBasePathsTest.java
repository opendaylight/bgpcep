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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

public class AddPathBasePathsTest extends AbstractAddPathTest {
    private RIBImpl ribImpl;
    private Channel serverChannel;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final TablesKey tk = new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(tk,
            BasePathSelectionModeFactory.createBestPathSelectionStrategy());

        ribImpl = new RIBImpl(tableRegistry, new RibId("test-rib"), AS_NUMBER, new BgpId(RIB_ID), ribExtension,
                serverDispatcher, codecsRegistry, getDomBroker(), policies, TABLES_TYPE, pathTables);
        ribImpl.instantiateServiceInstance();
        final ChannelFuture channelFuture = serverDispatcher.createServer(
            new InetSocketAddress(RIB_ID, PORT.toJava()));
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
     * Base-Paths
     *                                            ___________________
     *                                           | ODL BGP 127.0.0.1 |
     * [peer://127.0.0.2; p1, lp100] --(iBGP)--> |                   | --(RR-client, non add-path) -->
     * [peer://127.0.0.3; p1, lp200] --(iBGP)--> |                   |    [Peer://127.0.0.5; (p1, lp100), (p1, lp1200)]
     * [peer://127.0.0.4; p1, lp50] --(iBGP)-->  |                   | --(eBgp, non add-path) -->
     * [peer://127.0.0.2; p1, lp20] --(iBGP)-->  |___________________|    [Peer://127.0.0.6; (p1, path-id1, lp100),
     * p1 = 1.1.1.1/32                                                      (p1, path-id2, pl50), (p1, path-id3, pl200)]
     */
    @Test
    public void testUseCase1() throws Exception {
        final BgpParameters nonAddPathParams = createParameter(false);

        configurePeer(tableRegistry, PEER1, ribImpl, nonAddPathParams, PeerRole.Ibgp, serverRegistry);
        final BGPSessionImpl session1 = createPeerSession(PEER1, nonAddPathParams, new SimpleSessionListener());

        configurePeer(tableRegistry, PEER2, ribImpl, nonAddPathParams, PeerRole.Ibgp, serverRegistry);
        final BGPSessionImpl session2 = createPeerSession(PEER2, nonAddPathParams, new SimpleSessionListener());

        configurePeer(tableRegistry, PEER3, ribImpl, nonAddPathParams, PeerRole.Ibgp, serverRegistry);
        final BGPSessionImpl session3 = createPeerSession(PEER3,nonAddPathParams, new SimpleSessionListener());

        final SimpleSessionListener listener4 = new SimpleSessionListener();
        configurePeer(tableRegistry, PEER4, ribImpl, nonAddPathParams, PeerRole.RrClient,
                serverRegistry);
        final BGPSessionImpl session4 = createPeerSession(PEER4, nonAddPathParams, listener4);

        final SimpleSessionListener listener5 = new SimpleSessionListener();
        configurePeer(tableRegistry, PEER5, ribImpl, nonAddPathParams, PeerRole.Ebgp, serverRegistry);
        final BGPSessionImpl session5 = createPeerSession(PEER5, nonAddPathParams, listener5);
        checkPeersPresentOnDataStore(5);

        //new best route so far
        sendRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 1);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 2);
        assertEquals(UPD_NA_100, listener4.getListMsg().get(1));
        assertEquals(UPD_NA_100_EBGP, listener5.getListMsg().get(1));

        //the second best route
        sendRouteAndCheckIsOnLocRib(session2, PREFIX1, 100, 1);
        checkReceivedMessages(listener4, 2);
        checkReceivedMessages(listener5, 2);

        //new best route
        sendRouteAndCheckIsOnLocRib(session3, PREFIX1, 200, 1);
        checkReceivedMessages(listener4, 3);
        checkReceivedMessages(listener5, 3);
        assertEquals(UPD_NA_200, listener4.getListMsg().get(2));
        assertEquals(UPD_NA_200_EBGP, listener5.getListMsg().get(2));

        final SimpleSessionListener listener6 = new SimpleSessionListener();
        configurePeer(tableRegistry, PEER6, ribImpl, nonAddPathParams, PeerRole.RrClient,
                serverRegistry);
        final BGPSessionImpl session6 = createPeerSession(PEER6, nonAddPathParams, listener6);

        checkPeersPresentOnDataStore(6);
        checkReceivedMessages(listener6, 2);
        assertEquals(UPD_NA_200, listener6.getListMsg().get(1));
        session6.close();
        checkPeersPresentOnDataStore(5);

        //best route updated to be the worse one
        sendRouteAndCheckIsOnLocRib(session3, PREFIX1, 20, 1);
        checkReceivedMessages(listener4, 4);
        checkReceivedMessages(listener5, 4);
        assertEquals(UPD_NA_100, listener4.getListMsg().get(3));
        assertEquals(UPD_NA_100_EBGP, listener5.getListMsg().get(3));

        //Remove second best, no advertisement should be done
        sendWithdrawalRouteAndCheckIsOnLocRib(session2, PREFIX1, 50, 1);
        checkReceivedMessages(listener4, 4);
        checkReceivedMessages(listener5, 4);

        //Remove best, 1 advertisement
        sendWithdrawalRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 1);
        checkReceivedMessages(listener4, 5);
        checkReceivedMessages(listener5, 5);

        //Remove best, 1 withdrawal
        sendWithdrawalRouteAndCheckIsOnLocRib(session3, PREFIX1, 20, 0);
        checkReceivedMessages(listener4, 6);
        checkReceivedMessages(listener5, 6);

        session1.close();
        session2.close();
        session3.close();
        session4.close();
        session5.close();
    }
}
