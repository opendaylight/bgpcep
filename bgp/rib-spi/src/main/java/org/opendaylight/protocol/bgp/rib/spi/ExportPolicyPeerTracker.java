/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.SimpleRoutingPolicy;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Tracks peers for adj-rib-out writeout.
 */
public interface ExportPolicyPeerTracker {
    /**
     * Register Peer.
     *
     * @param peerId              Peer Id
     * @param sendReceive         send receive add ath configuration of the peer
     * @param peerPath            Yii of the peer
     * @param peerRole            Role of the peer
     * @param simpleRoutingPolicy optional
     */
    @Nonnull
    AbstractRegistration registerPeer(@Nonnull PeerId peerId, @Nullable SendReceive sendReceive,
            @Nonnull YangInstanceIdentifier peerPath, @Nonnull PeerRole peerRole,
            @Nonnull Optional<SimpleRoutingPolicy> simpleRoutingPolicy);

    /**
     * Returns PeerExportGroup per role.
     *
     * @param role of desired PeerExportGroup
     * @return PeerExportGroup
     */
    @Nullable
    PeerExportGroup getPeerGroup(@Nonnull PeerRole role);

    /**
     * Check whether the peer supports the table.
     *
     * @param peerId of peer
     * @return true if peer supports table
     */
    boolean isTableSupported(@Nonnull PeerId peerId);

    /**
     * Returns roles per PeerID.
     *
     * @param peerId of peer
     * @return Role of peer
     */
    @Nonnull
    PeerRole getRole(@Nonnull YangInstanceIdentifier peerId);

    /**
     * Check whether Peer supports Add Path.
     *
     * @param peerId of peer
     * @return true if add-path is supported
     */
    boolean isAddPathSupportedByPeer(@Nonnull PeerId peerId);

    /**
     * Flags peers once empty structure has been created, then changes under it can
     * be applied.
     *
     * @param peerId of peer
     */
    void registerPeerAsInitialized(@Nonnull PeerId peerId);

    /**
     * Check whether the peer supports the table.
     *
     * @param peerId of peer
     * @return true if peer supports table
     */
    boolean isTableStructureInitialized(@Nonnull PeerId peerId);
}
