/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the mapping of PeerId -> Role. Subclasses get notified of changes and can do their
 * own thing.
 */
abstract class AbstractPeerRoleTracker implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPeerRoleTracker.class);

    /**
     * We are subscribed to our target leaf, but that is a wildcard:
     *     /bgp-rib/rib/peer/peer-role
     *
     * MD-SAL assumption: we are getting one {@link DataTreeCandidate} for each expanded
     *                    wildcard path, so are searching for a particular key.
     */
    private final class PeerRoleListener implements DOMDataTreeChangeListener {

        @Override
        public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
            LOG.debug("Data Changed for Peer role {}", changes);
            for (final DataTreeCandidate tc : changes) {
                // Obtain the peer's path
                final YangInstanceIdentifier peerPath = IdentifierUtils.peerPath(tc.getRootPath());

                // Check for removal
                final Optional<NormalizedNode<?, ?>> maybePeerRole = tc.getRootNode().getDataAfter();
                final PeerRole role;
                if (maybePeerRole.isPresent()) {
                    final LeafNode<?> peerRoleLeaf = (LeafNode<?>) maybePeerRole.get();
                    // We could go for a coded, but his is simpler and faster
                    role = PeerRole.valueOf(BindingMapping.getClassName((String) peerRoleLeaf.getValue()));
                } else {
                    role = null;
                }

                peerRoleChanged(peerPath, role);
            }
        }
    }

    private static final QName PEER_ROLE = QName.cachedReference(QName.create(Peer.QNAME, "peer-role"));
    private final ListenerRegistration<?> registration;

    protected AbstractPeerRoleTracker(final @Nonnull DOMDataTreeChangeService service, @Nonnull final YangInstanceIdentifier ribId) {
        // Slightly evil, but our users should be fine with this
        final YangInstanceIdentifier roleId = ribId.node(Peer.QNAME).node(Peer.QNAME).node(PEER_ROLE);
        this.registration = service.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, roleId), new PeerRoleListener());
    }

    @Override
    public void close() {
        this.registration.close();
    }

    /**
     * Invoked whenever a peer role changes.
     *
     * @param peerPath Peer's path
     * @param role Peer's new role, null indicates the peer has disappeared.
     */
    protected abstract void peerRoleChanged(@Nonnull YangInstanceIdentifier peerPath, @Nullable PeerRole role);
}
