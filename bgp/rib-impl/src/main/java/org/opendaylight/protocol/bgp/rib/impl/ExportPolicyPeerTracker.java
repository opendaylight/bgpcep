/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Tracks peers for adj-rib-out writeout.
 */
final class ExportPolicyPeerTracker extends AbstractPeerRoleTracker {
    static final class PeerGroup {
        private final Collection<Entry<PeerId, YangInstanceIdentifier>> peers;
        private final Map<PeerId, PeerRole> peerRoles;
        private final AbstractExportPolicy policy;

        PeerGroup(final Collection<Entry<PeerId, YangInstanceIdentifier>> peers, final Map<PeerId, PeerRole> peerRoles, final AbstractExportPolicy policy) {
            this.peers = Preconditions.checkNotNull(peers);
            this.peerRoles = Preconditions.checkNotNull(peerRoles);
            this.policy = Preconditions.checkNotNull(policy);
        }

        ContainerNode effectiveAttributes(final PeerId sourcePeerId, final ContainerNode attributes) {
            return policy.effectiveAttributes(peerRoles.get(sourcePeerId), attributes);
        }

        Collection<Entry<PeerId, YangInstanceIdentifier>> getPeers() {
            return peers;
        }
    }

    private final Map<PeerRole, PeerGroup> groups = new EnumMap<>(PeerRole.class);
    private final Map<PeerId, PeerRole> peers = new HashMap<>();
    private final YangInstanceIdentifier ribId;

    protected ExportPolicyPeerTracker(final DOMDataTreeChangeService service, final YangInstanceIdentifier ribId) {
        super(service, ribId);
        this.ribId = ribId;
    }

    @Override
    protected synchronized void peerRoleChanged(final PeerId peer, final PeerRole role) {
        final PeerRole oldRole = peers.get(peer);
        if (role == oldRole) {
            // No-op
            return;
        }



        if (oldRole != null) {
            // FIXME: remove from old group
        }

        if (role != null) {
            // FIXME: add to new group
            peers.put(peer, role);
        } else {
            peers.remove(peer);
        }
    }

    // FIXME: remove the synchronized block as this is fast path
    synchronized PeerGroup getPeerGroup(final PeerRole role) {
        return groups.get(Preconditions.checkNotNull(role));
    }
}