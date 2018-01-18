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
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class BGPPeerTrackerImpl implements BGPPeerTracker {
    @GuardedBy("this")
    private final List<Peer> peers = new ArrayList<>();
    private Map<PeerRole, List<PeerId>> rolePerPeerId;

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
        return peer != null && peer.supportsTable(tableKey);
    }

    @Override
    public synchronized Map<PeerRole, List<PeerId>> getRoles() {
        return this.rolePerPeerId;
    }

    @Override
    public boolean supportsAddPathSupported(final PeerId toPeer, final TablesKey localTK) {
        final Peer peer = getPeer(toPeer);
        return peer != null && peer.supportsAddPathSupported(localTK);
    }

    @Nullable
    @Override
    public PeerRole getRole(final PeerId peerId) {
        synchronized (this.peers) {
            final Optional<Peer> peerOptional = this.peers.stream()
                    .filter(peer -> peer.getPeerId().equals(peerId)).findFirst();
            if (peerOptional.isPresent()) {
                return peerOptional.get().getRole();
            }
        }
        return null;
    }

    @Override
    public YangInstanceIdentifier getPeerRibInstanceIdentifier(final PeerId peerId) {
        final Peer peer = getPeer(peerId);
        return peer == null ? null : peer.getPeerRibInstanceIdentifier();
    }
}