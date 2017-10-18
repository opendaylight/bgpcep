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
import static org.opendaylight.controller.md.sal.binding.impl.BindingDOMMountPointServiceAdapter.LOG;
import static org.opendaylight.protocol.bgp.rib.impl.AdjRibInWriter.PEER_ID_QNAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ExportPolicyPeerTrackerImplTest {
    private static final TablesKey TABLE_KEY = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private static final Ipv4Address BGP_ID = new Ipv4Address("127.0.0.1");
    private static final PolicyDatabase PD = new PolicyDatabase(72L, BGP_ID, new ClusterIdentifier(BGP_ID));
    private static final PeerId PEER_ID1 = new PeerId("bgp://42.42.42.42");
    private static final YangInstanceIdentifier YII_PEER1 = YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Peer.QNAME)
        .nodeWithKey(Peer.QNAME, PEER_ID_QNAME, PEER_ID1.getValue()).build();
    private static final PeerId PEER_ID2 = new PeerId("bgp://42.42.42.43");
    private static final YangInstanceIdentifier YII_PEER2 = YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Peer.QNAME)
        .nodeWithKey(Peer.QNAME, PEER_ID_QNAME, PEER_ID2.getValue()).build();
    private static final PeerId PEER_ID3 = new PeerId("bgp://42.42.42.44");
    private static final YangInstanceIdentifier YII_PEER3 = YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Peer.QNAME)
        .nodeWithKey(Peer.QNAME, PEER_ID_QNAME, PEER_ID3.getValue()).build();
    private static final PeerId PEER_ID4 = new PeerId("bgp://42.42.42.45");
    private static final YangInstanceIdentifier YII_PEER4 = YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Peer.QNAME)
        .nodeWithKey(Peer.QNAME, PEER_ID_QNAME, PEER_ID4.getValue()).build();
    private static final PeerId PEER_ID5 = new PeerId("bgp://42.42.42.46");
    private static final List<AutoCloseable> TABLE_REGISTRATION = new ArrayList<>();

    @Test
    public void testExportPolicyPeerTrackerImpl() throws Exception {
        final ExportPolicyPeerTrackerImpl exportPpt = new ExportPolicyPeerTrackerImpl(PD, TABLE_KEY);

        TABLE_REGISTRATION.add(exportPpt.registerPeer(PEER_ID1, SendReceive.Both, YII_PEER1, PeerRole.RrClient, Optional.empty()));
        TABLE_REGISTRATION.add(exportPpt.registerPeer(PEER_ID2, SendReceive.Receive, YII_PEER2, PeerRole.Ibgp, Optional.of(SimpleRoutingPolicy.AnnounceNone)));
        TABLE_REGISTRATION.add(exportPpt.registerPeer(PEER_ID3, SendReceive.Send, YII_PEER3, PeerRole.Ebgp, Optional.of(SimpleRoutingPolicy.LearnNone)));
        TABLE_REGISTRATION.add(exportPpt.registerPeer(PEER_ID4, null, YII_PEER4, PeerRole.Ibgp, Optional.empty()));

        assertEquals(PeerRole.RrClient, exportPpt.getRole(YII_PEER1));
        assertEquals(PeerRole.Ibgp, exportPpt.getRole(YII_PEER2));
        assertEquals(PeerRole.Ebgp, exportPpt.getRole(YII_PEER3));
        assertEquals(PeerRole.Ibgp, exportPpt.getRole(YII_PEER4));

        assertTrue(exportPpt.getPeerGroup(PeerRole.RrClient).containsPeer(PEER_ID1));
        assertTrue(exportPpt.getPeerGroup(PeerRole.Ibgp).containsPeer(PEER_ID2));
        assertTrue(exportPpt.getPeerGroup(PeerRole.Ebgp).containsPeer(PEER_ID3));
        assertTrue(exportPpt.getPeerGroup(PeerRole.Ibgp).containsPeer(PEER_ID4));

        assertTrue(exportPpt.isTableSupported(PEER_ID1));
        assertFalse(exportPpt.isTableSupported(PEER_ID2));
        assertTrue(exportPpt.isTableSupported(PEER_ID3));
        assertTrue(exportPpt.isTableSupported(PEER_ID4));
        assertFalse(exportPpt.isTableSupported(PEER_ID5));

        assertTrue(exportPpt.isAddPathSupportedByPeer(PEER_ID1));
        assertTrue(exportPpt.isAddPathSupportedByPeer(PEER_ID2));
        assertFalse(exportPpt.isAddPathSupportedByPeer(PEER_ID3));
        assertFalse(exportPpt.isAddPathSupportedByPeer(PEER_ID4));
        assertFalse(exportPpt.isAddPathSupportedByPeer(PEER_ID5));

        TABLE_REGISTRATION.remove(0).close();
        assertNull(exportPpt.getRole(YII_PEER1));
        assertNull(exportPpt.getPeerGroup(PeerRole.RrClient));
        assertFalse(exportPpt.isTableSupported(PEER_ID1));
        assertFalse(exportPpt.isAddPathSupportedByPeer(PEER_ID1));


        TABLE_REGISTRATION.get(0).close();
        assertNull(exportPpt.getRole(YII_PEER2));
        assertFalse(exportPpt.getPeerGroup(PeerRole.Ibgp).containsPeer(PEER_ID2));
        assertFalse(exportPpt.isTableSupported(PEER_ID2));
        assertFalse(exportPpt.isAddPathSupportedByPeer(PEER_ID2));

        for (final AutoCloseable tableCloseable : TABLE_REGISTRATION) {
            try {
                tableCloseable.close();
            } catch (final Exception e) {
                LOG.warn("Failed to close registration", e);
            }
        }
        TABLE_REGISTRATION.clear();
    }

}