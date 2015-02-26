/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the BGP import policy. Listens on all Adj-RIB-In, inspects all inbound
 * routes in the context of the advertising peer's role and applies the inbound policy.
 *
 * Inbound policy is applied as follows:
 *
 * 1) if the peer is an eBGP peer, perform attribute replacement and filtering
 * 2) check if a route is admissible based on attributes attached to it, as well as the
 *    advertising peer's role
 * 3) output admissing routes with edited attributes into /bgp-rib/rib/peer/effective-rib-in/tables/routes
 *
 * Note that we maintain the peer roles using a DCL, even if we could look up our internal
 * structures. This is done so we maintain causality and loose coupling.
 */
@NotThreadSafe
final class ImportPolicyEnforcer {
    private static final Predicate<PathArgument> IS_PEER = new Predicate<PathArgument>() {
        @Override
        public boolean apply(final PathArgument input) {
            return input.getNodeType().equals(Peer.QNAME);
        }
    };
    private static final Predicate<PathArgument> IS_TABLES = new Predicate<PathArgument>() {
        @Override
        public boolean apply(final PathArgument input) {
            return input.getNodeType().equals(Tables.QNAME);
        }
    };
    private static final Logger LOG = LoggerFactory.getLogger(RouteListener.class);
    private static final QName PEER_ID = QName.create(Peer.QNAME, "peer-id");

    // FIXME: implement as id.firstIdentifierOf(IS_PEER), null indicating not found
    private static final NodeIdentifierWithPredicates firstKeyOf(final YangInstanceIdentifier id, final Predicate<PathArgument> match) {
        final PathArgument ret = Iterables.find(id.getPathArguments(), IS_PEER);
        Preconditions.checkArgument(ret instanceof NodeIdentifierWithPredicates, "Non-key peer identifier %s", ret);
        return (NodeIdentifierWithPredicates) ret;
    }

    private static final NodeIdentifierWithPredicates peerKey(final YangInstanceIdentifier id) {
        return firstKeyOf(id, IS_PEER);
    }

    private static final NodeIdentifierWithPredicates tableKey(final YangInstanceIdentifier id) {
        return firstKeyOf(id, IS_TABLES);
    }

    /**
     * Maintains the mapping of PeerId -> Role inside. We are subscribed to our target leaf,
     * but that is a wildcard:
     *     /bgp-rib/rib/peer/peer-role
     *
     * MD-SAL assumption: we are getting one {@link DataTreeCandidate} for each expanded
     *                    wildcard path, so are searching for a particular key.
     */
    private final class PeerRoleListener implements DOMDataTreeChangeListener {
        @Override
        public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
            synchronized (policies) {
                for (DataTreeCandidate tc : changes) {
                    // Obtain the peer's key
                    final NodeIdentifierWithPredicates peerKey = peerKey(tc.getRootPath());
                    // Lookup peer ID
                    final PeerId peerId = (PeerId) peerKey.getKeyValues().get(PEER_ID);

                    // Check for removal
                    final Optional<NormalizedNode<?, ?>> maybePeerRole = tc.getRootNode().getDataAfter();
                    if (maybePeerRole.isPresent()) {
                        final LeafNode<?> peerRoleLeaf = (LeafNode<?>) maybePeerRole.get();
                        // FIXME: need codec here
                        final PeerRole peerRole = (PeerRole) peerRoleLeaf.getValue();

                        // Lookup policy based on role
                        final AbstractImportPolicy policy = AbstractImportPolicy.forRole(peerRole);

                        // Update lookup map
                        policies.put(peerId, policy);
                    } else {
                        policies.remove(peerId);
                    }
                }
            }
        }
    }

   private final class RouteListener implements DOMDataTreeChangeListener {

       //        /bgp-rib/rib/peer/adj-rib-in/tables/routes
       @Override
       public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
           for (DataTreeCandidate tc : changes) {
               // Obtain the peer's key
               final NodeIdentifierWithPredicates peerKey = peerKey(tc.getRootPath());
               // Lookup peer ID
               final PeerId peerId = (PeerId) peerKey.getKeyValues().get(PEER_ID);

               // Lookup table key
               final NodeIdentifierWithPredicates tableKey = tableKey(tc.getRootPath());

               // FIXME: Lookup per-table attributes from RIBSupport
               final ContainerNode adverisedAttrs = null;
               final ContainerNode effectiveAttrs;

               synchronized (this) {
                   final AbstractImportPolicy policy = policies.get(peerId);
                   effectiveAttrs = policy.effectiveAttributes(adverisedAttrs);

               }

               LOG.debug("Route change {} effective attributes {}", tc.getRootPath(), effectiveAttrs);

               updateRoutes(peerId, tableKey, tc.getRootNode()., effectiveAttrs);
           }
       }
   }

   private final Map<PeerId, AbstractImportPolicy> policies = new HashMap<>();
   private final PeerRoleListener peerRoleListener = new PeerRoleListener();
   private final RouteListener routeListener = new RouteListener();
   private final DOMTransactionChain chain;

   private ImportPolicyEnforcer(final DOMTransactionChain chain) {
       this.chain = Preconditions.checkNotNull(chain);
   }

   private void updateRoutes(final PeerId peerId, final NodeIdentifierWithPredicates tableKey, final DataTreeCandidateNode routes, final ContainerNode effectiveAttrs) {
       final DOMDataWriteTransaction tx = chain.newWriteOnlyTransaction();

       // FIXME: RIBSupport needs to have a context here
       // FIXME: note that we need to detect table clears efficiently and propagate them

       tx.submit();
   }
}
