/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.doReturn;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yangtools.concepts.Registration;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class BGPPeerTrackerImplTest {
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

    @Before
    public void setUp() {
        tracker = new BGPPeerTrackerImpl();
    }

    @Test
    public void testClosedRegistrationRemovesPeer() {
        stubPeer(peerA, PEER_A, PeerRole.Ebgp);
        stubPeer(peerB, PEER_B, PeerRole.Ibgp);

        tracker.registerPeer(peerA);
        final Registration regB = tracker.registerPeer(peerB);
        assertThat(tracker.getPeers(), containsInAnyOrder(peerA, peerB));
        assertThat(tracker.getNonInternalPeers(), containsInAnyOrder(peerA, peerB));

        regB.close();

        assertThat(tracker.getPeers(), contains(peerA));
        assertThat(tracker.getNonInternalPeers(), contains(peerA));
    }

    @Test
    public void testInternalPeerExcluded() {
        stubPeer(peerA, PEER_A, PeerRole.Ebgp);
        stubPeer(internalPeer, PEER_INTERNAL, PeerRole.Internal);

        tracker.registerPeer(peerA);
        tracker.registerPeer(internalPeer);

        assertThat(tracker.getPeers(), containsInAnyOrder(peerA, internalPeer));
        assertThat(tracker.getNonInternalPeers(), contains(peerA));
    }

    @Test
    public void testEmptyTrackerReturnsEmptyLists() {
        assertThat(tracker.getPeers(), empty());
        assertThat(tracker.getNonInternalPeers(), empty());
    }

    private static void stubPeer(final Peer peer, final PeerId peerId, final PeerRole role) {
        doReturn(peerId).when(peer).getPeerId();
        doReturn(role).when(peer).getRole();
    }
}
