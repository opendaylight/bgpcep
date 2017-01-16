/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;

/**
 * Tracks Peers
 *
 * + Peer Role
 * + Peer Yii
 * + Supported Tables
 * + Additional Path Tables supported
 */
public interface BGPPeerTracker {
    /**
     * Register Peer
     *
     * @param peer Peer
     * @return registration tickets
     */
    AbstractRegistration registerPeer(@Nonnull Peer peer);


    /**
     * Flag Peer has ready to be notified once Peer's structure has been created
     *
     * @param peerId peer ID
     */
    void registerPeerAsInitialized(PeerId peerId);

    /**
     * Returns Peer
     *
     * @param peerId peer ID
     * @return Peer
     */
    @Nullable
    Peer getPeer(@Nonnull PeerId peerId);

    /**
     * Returns map of PeerId per PeerRole.
     * Role with none peerId will be filtered
     *
     * @return Returns map of PeerId group by PeerRole
     */
    @Nonnull
    Map<PeerRole, List<PeerId>> getRoles();
}
