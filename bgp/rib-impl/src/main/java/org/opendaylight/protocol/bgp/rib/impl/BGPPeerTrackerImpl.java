/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPPeerTrackerImpl implements BGPPeerTracker {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerTrackerImpl.class);

    @GuardedBy("this")
    private final Map<PeerId, Peer> peers = new HashMap<>();
    @GuardedBy("this")
    private final List<Consumer<Peer>> peerAddedListeners = new ArrayList<>();
    @GuardedBy("this")
    private ImmutableList<Peer> peersList = ImmutableList.of();
    @GuardedBy("this")
    private ImmutableList<Peer> peersFilteredList = ImmutableList.of();

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public @NonNull Registration registerPeer(final @NonNull Peer peer) {
        final List<Consumer<Peer>> listeners;
        synchronized (this) {
            peers.put(peer.getPeerId(), peer);
            rebuildSnapshots();
            listeners = ImmutableList.copyOf(peerAddedListeners);
        }
        // Notify outside the lock so a listener may call back into this tracker without risking a deadlock.
        for (final var listener : listeners) {
            try {
                listener.accept(peer);
            } catch (RuntimeException e) {
                // Caught so one failing listener cannot stop the others or abort this registration. Only
                // logged and ignored. Nothing else reacts to it, this peer stays registered either way.
                LOG.warn("Listener failed for peer {}, ignored", peer.getPeerId(), e);
            }
        }
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (BGPPeerTrackerImpl.this) {
                    peers.remove(peer.getPeerId());
                    rebuildSnapshots();
                }
            }
        };
    }

    @Holding("this")
    private void rebuildSnapshots() {
        peersList = ImmutableList.copyOf(peers.values());
        peersFilteredList = peers.values().stream()
                .filter(peer -> peer.getRole() != PeerRole.Internal)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public @NonNull Registration registerPeerAddedListener(final @NonNull Consumer<Peer> listener) {
        synchronized (this) {
            peerAddedListeners.add(listener);
        }
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (BGPPeerTrackerImpl.this) {
                    peerAddedListeners.remove(listener);
                }
            }
        };
    }

    @Override
    public synchronized @Nullable Peer getPeer(final @NonNull PeerId peerId) {
        return peers.get(peerId);
    }

    @Override
    public synchronized @NonNull List<Peer> getPeers() {
        return peersList;
    }

    @Override
    public synchronized @NonNull List<Peer> getNonInternalPeers() {
        return peersFilteredList;
    }
}
