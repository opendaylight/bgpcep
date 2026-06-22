/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.List;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Tracks Peers under RIB.
 */
public interface BGPPeerTracker {
    /**
     * Register Peer.
     *
     * @param peer Peer
     * @return registration tickets
     */
    Registration registerPeer(@NonNull Peer peer);

    /**
     * Returns Peer.
     *
     * @param peerId peer ID
     * @return Peer
     */
    @Nullable Peer getPeer(@NonNull PeerId peerId);

    /**
     * Returns map of PeerId per PeerRole.
     * Role with none peerId will be filtered.
     *
     * @return Returns map of Peer group by PeerRole
     */
    @NonNull List<Peer> getPeers();

    /**
     * Returns map of PeerId per PeerRole, filtering internal Peers.
     * Role with none peerId will be filtered.
     *
     * @return Returns map of Peer group by PeerRole
     */
    @NonNull List<Peer> getNonInternalPeers();

    /**
     * Register a listener notified when a peer is added to the tracker. Before this method returns, the listener is
     * also notified once for every already-registered peer. A peer registered concurrently with this method is
     * delivered exactly once.
     *
     * <p>Notifications are synchronous and execute with no tracker lock held. Existing peers are delivered on the
     * thread registering the listener, while subsequently-added peers are delivered on the thread registering the
     * peer. A {@link RuntimeException} thrown by the listener is ignored and does not abort either registration.
     *
     * @param listener invoked with each peer as it is registered
     * @return a {@link Registration} to remove the listener
     */
    @NonNull Registration registerPeerAddedListener(@NonNull Consumer<Peer> listener);
}
