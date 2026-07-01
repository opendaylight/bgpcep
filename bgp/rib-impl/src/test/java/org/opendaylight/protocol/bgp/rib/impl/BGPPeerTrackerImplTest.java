/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yangtools.concepts.Registration;

@ExtendWith(MockitoExtension.class)
class BGPPeerTrackerImplTest {
    private static final PeerId PEER_A = new PeerId("bgp://127.0.0.1");
    private static final PeerId PEER_B = new PeerId("bgp://127.0.0.2");
    private static final PeerId PEER_INTERNAL = new PeerId("bgp://127.0.0.3");

    @Mock
    private Peer peerA;
    @Mock
    private Peer peerB;
    @Mock
    private Peer internalPeer;

    private BGPPeerTrackerImpl tracker;

    @BeforeEach
    void setUp() {
        tracker = new BGPPeerTrackerImpl();
    }

    /**
     * Tests that closing a peer registration removes the peer from both snapshots. {@code removeRegistration()} used
     * to drop the peer from the backing map without rebuilding the snapshots, so a deregistered peer kept being
     * reported by {@link BGPPeerTrackerImpl#getPeers()} and {@link BGPPeerTrackerImpl#getNonInternalPeers()}.
     */
    @Test
    void testClosedRegistrationRemovesPeer() {
        stubPeer(peerA, PEER_A, PeerRole.Ebgp);
        stubPeer(peerB, PEER_B, PeerRole.Ibgp);

        tracker.registerPeer(peerA);
        final Registration regB = tracker.registerPeer(peerB);
        // The tracker snapshots a HashMap, so the peer order is unspecified.
        assertEquals(2, tracker.getPeers().size());
        assertTrue(tracker.getPeers().containsAll(List.of(peerA, peerB)));
        assertEquals(2, tracker.getNonInternalPeers().size());
        assertTrue(tracker.getNonInternalPeers().containsAll(List.of(peerA, peerB)));

        regB.close();

        assertEquals(List.of(peerA), tracker.getPeers());
        assertEquals(List.of(peerA), tracker.getNonInternalPeers());
    }

    /**
     * Tests that an internal peer is reported by {@link BGPPeerTrackerImpl#getPeers()} but filtered out of
     * {@link BGPPeerTrackerImpl#getNonInternalPeers()}.
     */
    @Test
    void testInternalPeerExcluded() {
        stubPeer(peerA, PEER_A, PeerRole.Ebgp);
        stubPeer(internalPeer, PEER_INTERNAL, PeerRole.Internal);

        tracker.registerPeer(peerA);
        tracker.registerPeer(internalPeer);

        assertEquals(2, tracker.getPeers().size());
        assertTrue(tracker.getPeers().containsAll(List.of(peerA, internalPeer)));
        assertEquals(List.of(peerA), tracker.getNonInternalPeers());
    }

    /**
     * Tests that a tracker with no registered peer returns empty lists. The snapshots were only assigned on the first
     * registration, so both getters returned null until some peer registered.
     */
    @Test
    void testEmptyTrackerReturnsEmptyLists() {
        assertTrue(tracker.getPeers().isEmpty());
        assertTrue(tracker.getNonInternalPeers().isEmpty());
    }

    private static void stubPeer(final Peer peer, final PeerId peerId, final PeerRole role) {
        doReturn(peerId).when(peer).getPeerId();
        doReturn(role).when(peer).getRole();
    }
}
