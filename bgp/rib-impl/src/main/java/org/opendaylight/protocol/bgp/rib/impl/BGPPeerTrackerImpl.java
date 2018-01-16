/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;

public final class BGPPeerTrackerImpl implements BGPPeerTracker {
    @GuardedBy("this")
    private final List<Peer> peers;
    @GuardedBy("this")
    private Map<PeerRole, List<PeerId>> rolePerPeerId;

    public BGPPeerTrackerImpl() {
        this.peers = new ArrayList<>();
    }

    @Override
    public synchronized AbstractRegistration registerPeer(final Peer peer) {
        this.peers.add(peer);
        this.rolePerPeerId = this.peers.stream().collect(Collectors.groupingBy(Peer::getRole,
                Collectors.mapping(Peer::getPeerId, Collectors.toList())));
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (BGPPeerTrackerImpl.this) {
                    BGPPeerTrackerImpl.this.peers.remove(peer);
                    BGPPeerTrackerImpl.this.rolePerPeerId = BGPPeerTrackerImpl.this.peers
                            .stream().collect(Collectors.groupingBy(Peer::getRole,
                                    Collectors.mapping(Peer::getPeerId, Collectors.toList())));
                }
            }
        };
    }

    @Override
    public void registerPeerAsInitialized(final PeerId peerId) {
        //FIXME
    }

    @Override
    public Peer getPeer(final PeerId peerId) {
        synchronized (this.peers) {
            return this.peers.stream().filter(peer -> peer.getPeerId().equals(peerId)).findFirst().orElse(null);
        }
    }

    @Override
    public boolean supportsTable(final PeerId peerIdOfNewPeer, @Nonnull final TablesKey tableKey) {
        final Peer peer = getPeer(peerIdOfNewPeer);
        if (peer == null) {
            return false;
        }
        return peer.supportsTable(tableKey);
    }

    @Override
    public synchronized Map<PeerRole, List<PeerId>> getRoles() {
        return this.rolePerPeerId;
    }

    @Override
    public boolean supportsAddPathSupported(final PeerId toPeer, final TablesKey localTK) {
        final Peer peer = getPeer(toPeer);
        if (peer == null) {
            return false;
        }

        return peer.supportsAddPathSupported(localTK);
    }
}