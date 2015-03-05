/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

/**
 * Tracks import policy corresponding to a particular peer.
 */
final class ImportPolicyPeerTracker extends AbstractPeerRoleTracker {
    private final Map<PeerId, AbstractImportPolicy> policies = new ConcurrentHashMap<>();

    protected ImportPolicyPeerTracker(final DOMDataTreeChangeService service, final YangInstanceIdentifier ribId) {
        super(service, ribId);
    }

    @Override
    protected void peerRoleChanged(final YangInstanceIdentifier peerPath, final PeerRole role) {
        final PeerId peer = IdentifierUtils.peerId((NodeIdentifierWithPredicates) peerPath.getLastPathArgument());

        if (role != null) {
            // Lookup policy based on role
            final AbstractImportPolicy policy = AbstractImportPolicy.forRole(role);

            // Update lookup map
            policies.put(peer, policy);
        } else {
            policies.remove(peer);
        }
    }

    AbstractImportPolicy policyFor(final PeerId peerId) {
        return policies.get(peerId);
    }
}