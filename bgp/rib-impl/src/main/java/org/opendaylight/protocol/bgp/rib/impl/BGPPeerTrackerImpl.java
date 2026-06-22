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

    // Initialized to empty (never null) so getPeers()/getNonInternalPeers() are safe to call and iterate before
    // the first Peer registration.
    private @NonNull ImmutableList<Peer> peersList = ImmutableList.of();
    private @NonNull ImmutableList<Peer> peersFilteredList = ImmutableList.of();

    @Override
    public Registration registerPeer(final Peer peer) {
        final List<Consumer<Peer>> listeners;
        synchronized (this) {
            this.peers.put(peer.getPeerId(), peer);
            rebuildPeersList();
            listeners = ImmutableList.copyOf(this.peerAddedListeners);
        }
        // Notify outside the lock so a listener may call back into this tracker without risking a deadlock.
        for (final var listener : listeners) {
            notifyListener(listener, peer);
        }
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (BGPPeerTrackerImpl.this) {
                    BGPPeerTrackerImpl.this.peers.remove(peer.getPeerId());
                    BGPPeerTrackerImpl.this.rebuildPeersList();
                }
            }
        };
    }

    @Override
    public Registration registerPeerAddedListener(final Consumer<Peer> listener) {
        final List<Peer> existing;
        synchronized (this) {
            peerAddedListeners.add(listener);
            // Copy inside the lock so a peer registering at the same time is notified exactly once.
            existing = ImmutableList.copyOf(peers.values());
        }
        // Notify the listener about peers already registered before it was added.
        for (final var peer : existing) {
            notifyListener(listener, peer);
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
    public synchronized Peer getPeer(final PeerId peerId) {
        return this.peers.get(peerId);
    }

    @Override
    public @NonNull synchronized List<Peer> getPeers() {
        return this.peersList;
    }

    @Override
    public @NonNull synchronized List<Peer> getNonInternalPeers() {
        return this.peersFilteredList;
    }

    @Holding("this")
    private void rebuildPeersList() {
        peersList = ImmutableList.copyOf(peers.values());
        peersFilteredList = peers.values().stream()
            .filter(peer -> peer.getRole() != PeerRole.Internal)
            .collect(ImmutableList.toImmutableList());
    }

    /**
     * Deliver a peer to one listener, isolating a listener failure so it cannot abort the registration or stop
     * the other notifications.
     *
     * @param listener the listener to notify
     * @param peer the peer to deliver
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void notifyListener(final Consumer<Peer> listener, final Peer peer) {
        try {
            listener.accept(peer);
        } catch (final RuntimeException e) {
            LOG.error("Listener failed for peer {}, ignored", peer.getPeerId(), e);
        }
    }
}
