/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

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
    AbstractRegistration registerPeer(@Nonnull Peer peer);

    /**
     * Returns Peer
     *
     * @param peerId peer ID
     * @return Peer
     */
    @Nullable
    Peer getPeer(@Nonnull PeerId peerId);

    /**
     * Returns if peer supports table.
     *
     * @param peerIdOfNewPeer PeerId
     * @param tableKey        table
     * @return true if Additional Path is supported for defined table
     */
    boolean supportsTable(final PeerId peerIdOfNewPeer, @Nonnull TablesKey tableKey);

    /**
     * Returns map of PeerId per PeerRole.
     * Role with none peerId will be filtered.
     *
     * @return Returns map of PeerId group by PeerRole
     */
    @Nonnull
    Map<PeerRole, List<PeerId>> getRoles();

    /**
     * Returns if peer supports Additional Path for specific table.
     *
     * @param toPeer  peer ID
     * @param localTK table
     * @return true if Additional Path is supported for defined table
     */
    boolean supportsAddPathSupported(PeerId toPeer, TablesKey localTK);

    /**
     * Returns role of specific peer if present.
     *
     * @param peerId peer identifier
     * @return role
     */
    @Nullable
    PeerRole getRole(PeerId peerId);

    /**
     * Returns YangInstanceIdentifier pointing peer under specific rib.
     *
     * @return Peer YangInstanceIdentifier
     */
    @Nullable
    YangInstanceIdentifier getPeerRibInstanceIdentifier(PeerId peerId);
}
