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
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
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
 * 3) output admitting routes with edited attributes into /bgp-rib/rib/peer/effective-rib-in/tables/routes
 *
 * Note that we maintain the peer roles using a DCL, even if we could look up our internal
 * structures. This is done so we maintain causality and loose coupling.
 */
@NotThreadSafe
final class EffectiveRibInWriter implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EffectiveRibInWriter.class);

    /**
     * Maintains the mapping of PeerId -> Role inside. We are subscribed to our target leaf,
     * but that is a wildcard:
     *     /bgp-rib/rib/peer/peer-role
     *
     * MD-SAL assumption: we are getting one {@link DataTreeCandidate} for each expanded
     *                    wildcard path, so are searching for a particular key.
     */


    /**
     * Maintains the individual routes for a particular table's routes under:
     *     /bgp-rib/rib/peer/adj-rib-in/tables/routes
     */
    private final class TableRouteListener implements DOMDataTreeChangeListener {
        private final NodeIdentifierWithPredicates tableKey;
        private final YangInstanceIdentifier target;
        private final RIBSupport ribSupport;
        private final PeerId peerId;

        TableRouteListener(final RIBSupport ribSupport, final NodeIdentifierWithPredicates peerKey, final NodeIdentifierWithPredicates tableKey) {
            this.ribSupport = Preconditions.checkNotNull(ribSupport);
            this.tableKey = Preconditions.checkNotNull(tableKey);

            // Lookup peer ID
            this.peerId = IdentifierUtils.peerId(peerKey);

            // FIXME: need target table ID
            target = null;
        }

        private void updateRoutes(final DOMDataWriteTransaction tx, final DataTreeCandidateNode routes, final ContainerNode effectiveAttrs) {
            final YangInstanceIdentifier routeId = target.node(routes.getIdentifier());

            if (effectiveAttrs != null) {
                tx.put(LogicalDatastoreType.OPERATIONAL, routeId, routes.getDataAfter().get());
                tx.put(LogicalDatastoreType.OPERATIONAL, routeId.node(ribSupport.routeAttributes()), effectiveAttrs);
            } else if (routes.getDataBefore().isPresent()) {
                tx.delete(LogicalDatastoreType.OPERATIONAL, routeId);
            }

        }

        @Override
        public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
            // FIXME: note that we need to detect table clears efficiently and propagate them

            final DOMDataWriteTransaction tx = chain.newWriteOnlyTransaction();

            for (DataTreeCandidate tc : changes) {
                // Lookup per-table attributes from RIBSupport
                final ContainerNode adverisedAttrs = (ContainerNode) NormalizedNodes.findNode(tc.getRootNode().getDataAfter(), ribSupport.routeAttributes()).orNull();
                final ContainerNode effectiveAttrs;

                if (adverisedAttrs != null && tc.getRootNode().getDataAfter().isPresent()) {
                    final AbstractImportPolicy policy = peerPolicyTracker.policyFor(peerId);
                    effectiveAttrs = policy.effectiveAttributes(adverisedAttrs);
                } else {
                    effectiveAttrs = null;
                }

                LOG.debug("Route change {} effective attributes {}", tc.getRootPath(), effectiveAttrs);

                updateRoutes(tx, tc.getRootNode(), effectiveAttrs);
            }

            tx.submit();
        }
    }

    /**
     * Maintains {@link TableRouteListener} instances.
     */
    private final class TableListener implements DOMDataTreeChangeListener {
        private final Map<YangInstanceIdentifier, ListenerRegistration<?>> routeListeners = new HashMap<>();
        private final RIBExtensionConsumerContext registry;
        private final DOMDataTreeChangeService service;

        TableListener(final DOMDataTreeChangeService service, final RIBExtensionConsumerContext registry) {
            this.registry = Preconditions.checkNotNull(registry);
            this.service = Preconditions.checkNotNull(service);
        }

        @Override
        public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {

            for (DataTreeCandidate tc : changes) {
                // Obtain the peer's key
                final NodeIdentifierWithPredicates peerKey = IdentifierUtils.peerKey(tc.getRootPath());

                // Lookup
                final NodeIdentifierWithPredicates tableKey = IdentifierUtils.tableKey(tc.getRootPath());

                switch (tc.getRootNode().getModificationType()) {
                case DELETE:
                    final ListenerRegistration<?> reg = routeListeners.remove(tc.getRootPath());
                    if (reg != null) {
                        reg.close();
                    }
                    break;
                case WRITE:
                    // FIXME: use codec to translate
                    final RIBSupport ribSupport = registry.getRIBSupport(null);
                    if (ribSupport != null) {
                        final TableRouteListener routeListener = new TableRouteListener(ribSupport, peerKey, tableKey);
                        final ListenerRegistration<?> r = service.registerDataTreeChangeListener(
                            new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL,  tc.getRootPath()), routeListener);

                        routeListeners.put(tc.getRootPath(), r);
                    } else {
                        LOG.warn("No RIB support for table {}, ignoring advertisements from peer %s", tableKey, peerKey);
                    }
                    break;
                default:
                    // No-op
                    break;
                }
            }
        }
    }

    private final PeerPolicyTracker peerPolicyTracker;
    private final DOMTransactionChain chain;

    private EffectiveRibInWriter(final DOMDataTreeChangeService service, final DOMTransactionChain chain, final YangInstanceIdentifier ribId) {
        this.chain = Preconditions.checkNotNull(chain);

        this.peerPolicyTracker = new PeerPolicyTracker(service, ribId);

        // FIXME: subscribe tableListener
    }

    @Override
    public void close() {
        peerPolicyTracker.close();
    }
}
