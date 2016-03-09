/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.policy;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the mapping of PeerId - Role. Subclasses get notified of changes and can do their
 * own thing.
 */
public abstract class AbstractPeerRoleTracker {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPeerRoleTracker.class);

    public void onDataTreeChanged(final DataTreeCandidateNode change, final YangInstanceIdentifier peerPath) {
        // Check for removal
        final Optional<NormalizedNode<?, ?>> maybePeerRole = change.getDataAfter();
        LOG.debug("Data Changed for Peer role {} path {}, dataBefore {}, dataAfter {}", change.getIdentifier(),
            peerPath , change.getDataBefore(), maybePeerRole);

        final PeerRole role;
        if (maybePeerRole.isPresent()) {
            final LeafNode<?> peerRoleLeaf = (LeafNode<?>) maybePeerRole.get();
            // We could go for a codec, but this is simpler and faster
            role = PeerRole.valueOf(BindingMapping.getClassName((String) peerRoleLeaf.getValue()));
        } else {
            role = null;
        }
        peerRoleChanged(peerPath, role);
    }

    public static final NodeIdentifier PEER_ROLE_NID = new NodeIdentifier(QName.create(Peer.QNAME, "peer-role").intern());
    public static final NodeIdentifier PEER_TABLES = new NodeIdentifier(SupportedTables.QNAME);

    protected AbstractPeerRoleTracker() {
    }

    protected abstract void peerRoleChanged(@Nonnull YangInstanceIdentifier peerPath, @Nullable PeerRole role);
}
