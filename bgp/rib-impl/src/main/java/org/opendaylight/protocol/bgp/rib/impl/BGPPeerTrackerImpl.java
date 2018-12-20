/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;

public final class BGPPeerTrackerImpl implements BGPPeerTracker {
    @GuardedBy("this")
    private final Map<PeerId, Peer> peers = new HashMap<>();
    private ImmutableList<Peer> peersList;
    private ImmutableList<Peer> peersFilteredList;

    @Override
    public synchronized Registration registerPeer(final Peer peer) {
        this.peers.put(peer.getPeerId(), peer);
        this.peersList = ImmutableList.copyOf(this.peers.values());
        this.peersFilteredList = this.peers.values().stream()
                .filter(p1 -> p1.getRole() != PeerRole.Internal)
                .collect(ImmutableList.toImmutableList());
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (BGPPeerTrackerImpl.this) {
                    BGPPeerTrackerImpl.this.peers.remove(peer.getPeerId());
                }
            }
        };
    }

    @Override
    public synchronized Peer getPeer(final PeerId peerId) {
        return this.peers.get(peerId);
    }

    @Override
    public synchronized List<Peer> getPeers() {
        return this.peersList;
    }

    @Override
    public synchronized List<Peer> getNonInternalPeers() {
        return this.peersFilteredList;
    }
}