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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class AddPathNPathsTest extends AbstractAddPathTest {

    /*
     * N-Paths
     *                                          ___________________
     *                                         | ODL BGP 127.0.0.1 |
     * [peer://127.0.0.2; p1, nh1] --(iBGP)--> |                   | --(RR-client, non add-path) --> [Peer://127.0.0.5; (p1, nh1)]
     * [peer://127.0.0.3; p1, nh2] --(iBGP)--> |                   |
     * [peer://127.0.0.4; p1, nh3] --(iBGP)--> |                   | --(RR-client, add-path) --> [Peer://127.0.0.6; (p1, path-id1, nh1), (p1, path-id2, nh2)]
     *                                         |___________________|
     * p1 = 1.1.1.1/32
     * nh1 = 2.2.2.2
     * nh2 = 3.3.3.3
     * nh3 = 4.4.4.4
     */
    @Test
    public void testUseCase1() throws Exception {

        final List<BgpTableType> tables = ImmutableList.of(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        final TablesKey tk = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(tk, new AddPathBestNPathSelection(2L));

        final RIBImpl ribImpl = new RIBImpl(new RibId("test-rib"), AS_NUMBER, new BgpId(RIB_ID), null, this.ribExtension,
                this.dispatcher, this.mappingService.getCodecFactory(), getDomBroker(), tables, pathTables, this.ribExtension.getClassLoadingStrategy());


        ribImpl.onGlobalContextUpdated(this.schemaContext);

        this.dispatcher.createServer(StrictBGPPeerRegistry.GLOBAL, new InetSocketAddress(RIB_ID, PORT)).sync();
        Thread.sleep(1000);

        final BGPHandlerFactory hf = new BGPHandlerFactory(this.context.getMessageRegistry());
        final BgpParameters nonAddPathParams = createParameter(false);
        final BgpParameters addPathParams = createParameter(true);

        final Channel session1 = createPeerSession(PEER1, PeerRole.Ibgp, nonAddPathParams, ribImpl, hf);
        final Channel session2 = createPeerSession(PEER2, PeerRole.Ibgp, nonAddPathParams, ribImpl, hf);
        createPeerSession(PEER3, PeerRole.Ibgp, nonAddPathParams, ribImpl, hf);
        final Channel session4 = createPeerSession(PEER4, PeerRole.RrClient, nonAddPathParams, ribImpl, hf);
        final Channel session5 = createPeerSession(PEER5, PeerRole.RrClient, addPathParams, ribImpl, hf);

        checkPeersPresentOnDataStore(5);
        //not able to parse add-path NLRIs now, the session will fails
        //replace message handler and session handler with some "packet capture"
        final HexDumpCollector messageCollector = new HexDumpCollector();
        session5.pipeline().remove(BGPByteToMessageDecoder.class);
        session5.pipeline().replace(BGPSessionImpl.class, "message-collector", messageCollector);
        Thread.sleep(500);

        sendRouteAndCheckIsOnDS(session1, PREFIX1, NH1, 1);
        checkRibOut(1);
        assertEquals(1, messageCollector.getReceivedMessages().size());

        sendRouteAndCheckIsOnDS(session2, PREFIX1, NH2, 2);
        checkRibOut(2);
        /**1 are from previous update and 3 from new update**/
        assertEquals(3, messageCollector.getReceivedMessages().size());

        sendRouteAndCheckIsOnDS(session4, PREFIX1, NH3, 2);
        checkRibOut(2);
        /**3 are from previous update and non new Upd since new route is not better**/
        assertEquals(3, messageCollector.getReceivedMessages().size());

        checkMessageHexDump(messageCollector);
    }




    private void checkMessageHexDump(final HexDumpCollector messageCollector) {
        final Map<String, Long> col = messageCollector.getReceivedMessages().stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(2, (long) col.get(UPD_NH_1));
        assertEquals(1, (long) col.get(UPD_NH_2));
    }
}
