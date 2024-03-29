/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.BGPRIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.LOCRIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBQNames.PEER_ID_QNAME;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class IdentifierUtilsTest {
    private static final QName TABLES_KEY_QNAME = QName.create(Tables.QNAME, "tables-key").intern();
    private static final TablesKey TK = new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
    private static final PeerId PEER_ID = new PeerId("127.0.0.1");
    private static final NodeIdentifierWithPredicates NIWP_PEER = NodeIdentifierWithPredicates.of(Peer.QNAME,
            PEER_ID_QNAME, PEER_ID.getValue());
    private static final NodeIdentifierWithPredicates NIWP_TABLE = NodeIdentifierWithPredicates.of(Tables.QNAME,
            TABLES_KEY_QNAME, TK);
    private static final YangInstanceIdentifier YII_PEER;
    private static final YangInstanceIdentifier YII_TABLE;

    static {
        YII_PEER = YangInstanceIdentifier.builder().node(BGPRIB_NID).node(PEER_NID)
                .nodeWithKey(Peer.QNAME, PEER_ID_QNAME, PEER_ID.getValue()).build();
        YII_TABLE = YangInstanceIdentifier.builder().node(LOCRIB_NID).node(Tables.QNAME)
                .nodeWithKey(Tables.QNAME, TABLES_KEY_QNAME, TK).build();
    }

    @Test
    public void testPeerPath() throws Exception {
        final YangInstanceIdentifier result = IdentifierUtils.peerPath(YII_PEER);
        assertEquals(YII_PEER, result);
    }

    @Test
    public void testPeerKey() throws Exception {
        final NodeIdentifierWithPredicates result = IdentifierUtils.peerKey(YII_PEER);
        assertEquals(NIWP_PEER, result);
    }

    @Test
    public void testPeerId() throws Exception {
        final PeerId result = IdentifierUtils.peerId(NIWP_PEER);
        assertEquals(PEER_ID, result);
    }

    @Test
    public void testPeerKeyToPeerId() throws Exception {
        final PeerId result = IdentifierUtils.peerKeyToPeerId(YII_PEER);
        assertEquals(PEER_ID, result);
    }

    @Test
    public void testTableKey() throws Exception {
        final NodeIdentifierWithPredicates result = IdentifierUtils.tableKey(YII_TABLE);
        assertEquals(NIWP_TABLE, result);
    }

    @Test
    public void testDomPeerId() throws Exception {
        final NodeIdentifierWithPredicates result = IdentifierUtils.domPeerId(PEER_ID);
        assertEquals(NIWP_PEER, result);
    }

}