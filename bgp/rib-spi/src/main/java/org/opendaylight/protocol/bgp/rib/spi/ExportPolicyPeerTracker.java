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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;

/**
 * Tracks peers for adj-rib-out writeout.
 */
@Deprecated
public interface ExportPolicyPeerTracker {
    /**
     * Update set with supported tables per peer
     * @param peerId which receveid the change
     * @param node data change
     */
    @Deprecated
    default void onTablesChanged(PeerId peerId, DataTreeCandidateNode node) {}

    /**
     * Register Peer
     * @param peerId Peer Id
     * @param sendReceive send receive add ath configuration of the peer
     * @param peerPath Yii of the peer
     * @param peerRole Role of the peer
     * @param simpleRoutingPolicy
     */
    AbstractRegistration registerPeer(@Nonnull PeerId peerId, @Nullable SendReceive sendReceive, @Nonnull YangInstanceIdentifier peerPath, @Nonnull PeerRole peerRole,
        @Nonnull Optional<SimpleRoutingPolicy> simpleRoutingPolicy);

    /**
     * returns PeerExportGroup per role
     * @param role of desired PeerExportGroup
     * @return PeerExportGroup
     */
    PeerExportGroup getPeerGroup(@Nonnull PeerRole role);

    /**
     * check whether the peer supports the table
     * @param peerId of peer
     * @return true if peer supports table
     */
    boolean isTableSupported(@Nonnull PeerId peerId);

    /**
     * @param peerId of peer
     * @return Role of peer
     */
    PeerRole getRole(@Nonnull YangInstanceIdentifier peerId);

    /**
     * Check whether Peer supports Add Path
     * @param peerId
     * @return true if add-path is supported
     */
    boolean isAddPathSupportedByPeer(@Nonnull PeerId peerId);

    /**
     * Invoked whenever a peer role changes.
     *
     * @param peerPath Peer's path
     * @param role Peer's new role, null indicates the peer has disappeared.
     */
    @Deprecated
    default void peerRoleChanged(@Nonnull YangInstanceIdentifier peerPath,  @Nullable PeerRole role) {
    }
}
