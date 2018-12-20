/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    Registration registerPeer(@Nonnull Peer peer);

    /**
     * Returns Peer.
     *
     * @param peerId peer ID
     * @return Peer
     */
    @Nullable
    Peer getPeer(@Nonnull PeerId peerId);

    /**
     * Returns map of PeerId per PeerRole.
     * Role with none peerId will be filtered.
     *
     * @return Returns map of Peer group by PeerRole
     */
    @Nonnull
    List<Peer> getPeers();

    /**
     * Returns map of PeerId per PeerRole, filtering internal Peers.
     * Role with none peerId will be filtered.
     *
     * @return Returns map of Peer group by PeerRole
     */
    @Nonnull
    List<Peer> getNonInternalPeers();
}
