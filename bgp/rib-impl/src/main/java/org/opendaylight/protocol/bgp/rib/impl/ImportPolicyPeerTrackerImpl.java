/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.protocol.bgp.rib.impl.spi.AbstractImportPolicy;
import org.opendaylight.protocol.bgp.rib.impl.spi.ImportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ImportPolicyPeerTrackerImpl implements ImportPolicyPeerTracker {
    private static final Logger LOG = LoggerFactory.getLogger(ImportPolicyPeerTrackerImpl.class);

    private final Map<PeerId, AbstractImportPolicy> policies = new ConcurrentHashMap<>();
    private final PolicyDatabase policyDatabase;

    protected ImportPolicyPeerTrackerImpl(final PolicyDatabase policyDatabase) {
        super();
        this.policyDatabase = requireNonNull(policyDatabase);
    }

    @Override
    public void peerRoleChanged(final YangInstanceIdentifier peerPath, final PeerRole role) {
        final PeerId peer = IdentifierUtils.peerId((NodeIdentifierWithPredicates) peerPath.getLastPathArgument());

        if (role != null) {
            // Lookup policy based on role
            final AbstractImportPolicy policy = this.policyDatabase.importPolicyForRole(role);

            // Update lookup map
            this.policies.put(peer, policy);
            LOG.debug("Updating policy {} for peer {}", policy, peer);
        } else {
            this.policies.remove(peer);
        }
    }

    @Override
    public AbstractImportPolicy policyFor(final PeerId peerId) {
        LOG.trace("Peer ID : {}", peerId);
        return this.policies.get(peerId);
    }
}