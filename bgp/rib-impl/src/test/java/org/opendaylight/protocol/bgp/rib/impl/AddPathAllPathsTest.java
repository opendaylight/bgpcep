/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;

public class AddPathAllPathsTest extends AbstractAddPathTest {
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

        final List<BgpTableType> tables = ImmutableList.of(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        final TablesKey tk = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(tk, new AllPathSelection());


        final RIBImpl ribImpl = new RIBImpl(this.clusterSingletonServiceProvider, new RibId("test-rib"), AS_NUMBER, new BgpId(RIB_ID), null, this.ribExtension,
                this.dispatcher, this.mappingService.getCodecFactory(), getDomBroker(), tables, pathTables, this.ribExtension.getClassLoadingStrategy(), null);

        ribImpl.instantiateServiceInstance();
        ribImpl.onGlobalContextUpdated(this.schemaContext);

        this.dispatcher.createServer(StrictBGPPeerRegistry.GLOBAL, new InetSocketAddress(RIB_ID, PORT)).sync();
        Thread.sleep(1000);


        final BGPHandlerFactory hf = new BGPHandlerFactory(this.context.getMessageRegistry());
        final BgpParameters nonAddPathParams = createParameter(false);
        final BgpParameters addPathParams = createParameter(true);

        final Channel session1 = createPeerSession(PEER1, PeerRole.Ibgp, nonAddPathParams, ribImpl, hf, new SimpleSessionListener());
        final Channel session2 = createPeerSession(PEER2, PeerRole.Ibgp, nonAddPathParams, ribImpl, hf, new SimpleSessionListener());
        final Channel session3 = createPeerSession(PEER3, PeerRole.Ibgp, nonAddPathParams, ribImpl, hf, new SimpleSessionListener());
        final SimpleSessionListener listener4 = new SimpleSessionListener();
        final Channel session4 = createPeerSession(PEER4, PeerRole.RrClient, nonAddPathParams, ribImpl, hf, listener4);
        final SimpleSessionListener listener5 = new SimpleSessionListener();
        final Channel session5 = createPeerSession(PEER5, PeerRole.RrClient, addPathParams, ribImpl, hf, listener5);
        Thread.sleep(1000);
        checkPeersPresentOnDataStore(5);

        //the best route
        sendRouteAndCheckIsOnDS(session1, PREFIX1, 100, 1);
        assertEquals(1, listener4.getListMsg().size());
        assertEquals(1, listener5.getListMsg().size());
        assertEquals(UPD_100, listener5.getListMsg().get(0));

        //the second best route
        sendRouteAndCheckIsOnDS(session2, PREFIX1, 50, 2);
        assertEquals(1, listener4.getListMsg().size());
        assertEquals(2, listener5.getListMsg().size());
        assertEquals(UPD_50, listener5.getListMsg().get(1));

        //new best route
        sendRouteAndCheckIsOnDS(session3, PREFIX1, 200, 3);
        assertEquals(2, listener4.getListMsg().size());
        assertEquals(3, listener5.getListMsg().size());
        assertEquals(UPD_200, listener5.getListMsg().get(2));

        //the worst route
        sendRouteAndCheckIsOnDS(session1, PREFIX1, 20, 3);
        assertEquals(2, listener4.getListMsg().size());
        assertEquals(4, listener5.getListMsg().size());
        assertEquals(UPD_200.getAttributes().getLocalPref(), ((Update) listener4.getListMsg().get(1)).getAttributes().getLocalPref());
        assertEquals(UPD_20, listener5.getListMsg().get(3));

        session1.close();
        session2.close();
        session3.close();
        session4.close();
        session5.close();
    }
}
