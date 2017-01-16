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
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

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
     * @param peerId Peer Id
     * @param localTables List of supported Tables
     * @param addPathTableMaps send receive add ath configuration of the peer
     * @param peerPath Yii of the peer
     * @param peerRole Role of the peer
     */
    AbstractRegistration registerPeer(@Nonnull PeerId peerId, final Set<TablesKey> localTables,
        @Nullable Map<TablesKey, SendReceive> addPathTableMaps, @Nonnull YangInstanceIdentifier peerPath,
        @Nonnull PeerRole peerRole);

    /**
     * Returns role of Peer
     *
     * @param peerId peer ID
     * @return Peer Role per specific peer
     */
    @Nullable
    PeerRole getRole(@Nonnull final PeerId peerId);

    /**
     * Returns map of PeerId per PeerRole.
     * Role with none peerId will be filtered
     *
     * @return Returns map of PeerId group by PeerRole
     */
    @Nonnull
    Map<PeerRole, List<PeerId>> getRoles();

    /**
     * check whether the peer supports specific table
     *
     * @param peerId of peer
     * @param tableKey tableKey
     * @return true if peer supports table
     */
    boolean isTableSupported(@Nonnull PeerId peerId, @Nonnull TablesKey tableKey);

    /**
     * check whether the peer supports additional path per specific table
     *
     * @param peerId of peer
     * @param tableKey tableKey
     * @return true if peer supports additional path per specific table
     */
    boolean isAddPathSupported(@Nonnull PeerId peerId, @Nonnull TablesKey tableKey);

    /**
     * Return Yii per specific Peer
     *
     * @param peerId peer Id
     * @return Yii for specific peerId
     */
    @Nonnull
    YangInstanceIdentifier getYii(@Nonnull PeerId peerId);
}
