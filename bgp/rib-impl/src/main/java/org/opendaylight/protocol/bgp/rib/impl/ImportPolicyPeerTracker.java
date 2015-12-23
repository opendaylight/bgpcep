/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.protocol.bgp.rib.impl.spi.ImportPolicy;
import org.opendaylight.protocol.bgp.rib.impl.spi.PolicyDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks import policy corresponding to a particular peer.
 */
final class ImportPolicyPeerTracker extends AbstractPeerRoleTracker {
    private static final Logger LOG = LoggerFactory.getLogger(ImportPolicyPeerTracker.class);

    private final Map<PeerId, ImportPolicy> policies = new ConcurrentHashMap<>();
    private final PolicyDatabase policyDatabase;

    protected ImportPolicyPeerTracker(final PolicyDatabase policyDatabase) {
        super();
        this.policyDatabase = Preconditions.checkNotNull(policyDatabase);
    }

    @Override
    protected void peerRoleChanged(final YangInstanceIdentifier peerPath, final PeerRole role) {
        final PeerId peer = IdentifierUtils.peerId((NodeIdentifierWithPredicates) peerPath.getLastPathArgument());

        if (role != null) {
            // Lookup policy based on role
            final ImportPolicy policy = this.policyDatabase.importPolicyForRole(role);

            // Update lookup map
            this.policies.put(peer, policy);
            LOG.debug("Updating policy {} for peer {}", policy, peer);
        } else {
            this.policies.remove(peer);
        }
    }

    ImportPolicy policyFor(final PeerId peerId) {
        LOG.trace("Peer ID : {}", peerId);
        return new CachingImportPolicy(this.policies.get(peerId));
    }
}