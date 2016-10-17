/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkReceivedMessages;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.waitFutureSuccess;

import com.google.common.collect.ImmutableMap;
import java.net.InetSocketAddress;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;

public class AddPathAllPathsTest extends AbstractAddPathTest {
    private RIBImpl ribImpl;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(TABLE_KEY, new AllPathSelection());

        this.ribImpl = new RIBImpl(this.clusterSingletonServiceProvider, new RibId("test-rib"), AS_NUMBER, new BgpId(RIB_ID), null,
            READ_ONLY_LIMIT, this.ribExtension, this.dispatcher, this.mappingService.getCodecFactory(), getDomBroker(), TABLES, pathTables,
            this.ribExtension.getClassLoadingStrategy(), null);

        this.ribImpl.instantiateServiceInstance();
        this.ribImpl.onGlobalContextUpdated(this.schemaContext);
        checkPeersPresentOnDataStore(0);

        waitFutureSuccess(this.dispatcher.createServer(StrictBGPPeerRegistry.GLOBAL, new InetSocketAddress(RIB_ID, PORT)).sync());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closePeerSessions(PEER_SESSIONS.entrySet());
       /* this.ribImpl.closeServiceInstance();
        this.ribImpl.close();
        checkRibRemoved();*/
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
        final BGPHandlerFactory hf = new BGPHandlerFactory(this.context.getMessageRegistry());
        final BgpParameters nonAddPathParams = createParameter(false);
        final BgpParameters addPathParams = createParameter(true);

        final BGPSessionImpl session1 = createPeerSession(PEER1, PeerRole.Ibgp, nonAddPathParams, this.ribImpl, hf, new SimpleSessionListener());
        final BGPSessionImpl session2 = createPeerSession(PEER2, PeerRole.Ibgp, nonAddPathParams, this.ribImpl, hf, new SimpleSessionListener());
        final BGPSessionImpl session3 = createPeerSession(PEER3, PeerRole.Ibgp, nonAddPathParams, this.ribImpl, hf, new SimpleSessionListener());
        final SimpleSessionListener listener4 = new SimpleSessionListener();
        final BGPSessionImpl session4 = createPeerSession(PEER4, PeerRole.RrClient, nonAddPathParams, this.ribImpl, hf, listener4);
        final SimpleSessionListener listener5 = new SimpleSessionListener();
        final BGPSessionImpl session5 = createPeerSession(PEER5, PeerRole.RrClient, addPathParams, this.ribImpl, hf, listener5);
        checkPeersPresentOnDataStore(5);
        markEndOfReadOnly(session4);
        checkRibOut(PEER4, 0);
        markEndOfReadOnly(session5);
        checkRibOut(PEER5, 0);
        //the best route
        sendRouteAndCheckIsOnLocRib(session1, PREFIX1, 100, 1);
        checkReceivedMessages(listener4, 1);
        checkReceivedMessages(listener5, 1);
        assertEquals(UPD_100, listener5.getListMsg().get(0));

        final SimpleSessionListener listener6 = new SimpleSessionListener();
        final BGPSessionImpl session6 = createPeerSession(PEER6, PeerRole.RrClient, nonAddPathParams, this.ribImpl, hf, listener6);
        checkPeersPresentOnDataStore(6);
        markEndOfReadOnly(session6);
        checkReceivedMessages(listener6, 1);
        assertEquals(UPD_NA_100, listener6.getListMsg().get(0));
        //closePeerSession(new IpAddress(PEER6), session6);

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
    }
}
