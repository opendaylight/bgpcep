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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class AddPathAllPathsTest extends AbstractAddPathTest {
    /*
     * All-Paths
     *                                          ___________________
     *                                         | ODL BGP 127.0.0.1 |
     * [peer://127.0.0.2; p1, nh1] --(iBGP)--> |                   | --(RR-client, non add-path) --> [Peer://127.0.0.5; (p1, nh1)]
     * [peer://127.0.0.3; p1, nh2] --(iBGP)--> |                   |
     * [peer://127.0.0.4; p1, nh3] --(iBGP)--> |                   | --(RR-client, add-path) --> [Peer://127.0.0.6; (p1, path-id1, nh1), ,
     *                                         |___________________|                                         (p1, path-id2, nh2),(p1,  path-id3, nh3)]
     * p1 = 1.1.1.1/32
     * nh1 = 2.2.2.2
     * nh2 = 3.3.3.3
     * nh3 = 4.4.4.4
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

        sendRouteAndCheckIsOnDS(session1, PREFIX1, NH1, 1);
        checkRibOut(1);
        assertEquals(1, listener4.getListMsg().size());
        assertEquals(1, listener5.getListMsg().size());
        //TODO check last message

        sendRouteAndCheckIsOnDS(session2, PREFIX1, NH2, 2);
        checkRibOut(2);
        /**1 are from previous update and 2 from new update**/
        assertEquals(2, listener4.getListMsg().size());
        assertEquals(2, listener5.getListMsg().size());
        //TODO check last message

        sendRouteAndCheckIsOnDS(session3, PREFIX1, NH3, 3);
        checkRibOut(3);
        /**3 are from previous update and 3 from new update**/
        assertEquals(2, listener4.getListMsg().size());
        assertEquals(3, listener5.getListMsg().size());
        //TODO check last message
    }
}
