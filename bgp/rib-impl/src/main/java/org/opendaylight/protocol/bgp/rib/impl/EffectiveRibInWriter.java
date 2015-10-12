/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
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
    public static final NodeIdentifier TABLE_ROUTES = new NodeIdentifier(Routes.QNAME);
    private static final NodeIdentifier ADJRIBIN_NID = new NodeIdentifier(AdjRibIn.QNAME);
    private static final NodeIdentifier TABLES_NID = new NodeIdentifier(Tables.QNAME);

    /**
     * Maintains {@link TableRouteListener} instances.
     */
    private final class AdjInTracker implements AutoCloseable, DOMDataTreeChangeListener {
        private final RIBSupportContextRegistry registry;
        private final YangInstanceIdentifier ribId;
        private final ListenerRegistration<?> reg;
        private final DOMTransactionChain chain;

        AdjInTracker(final DOMDataTreeChangeService service, final RIBSupportContextRegistry registry, final DOMTransactionChain chain, final YangInstanceIdentifier ribId) {
            this.registry = Preconditions.checkNotNull(registry);
            this.chain = Preconditions.checkNotNull(chain);
            this.ribId = Preconditions.checkNotNull(ribId);

            final YangInstanceIdentifier tableId = ribId.node(Peer.QNAME).node(Peer.QNAME);
            final DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, tableId);
            LOG.debug("Registered Effective RIB on {}", tableId);
            this.reg = service.registerDataTreeChangeListener(treeId, this);
        }

        private void processRoute(final DOMDataWriteTransaction tx, final RIBSupport ribSupport, final AbstractImportPolicy policy, final YangInstanceIdentifier routesPath, final DataTreeCandidateNode route) {
            LOG.debug("Process route {}", route);
            final YangInstanceIdentifier routeId = ribSupport.routePath(routesPath, route.getIdentifier());
            switch (route.getModificationType()) {
            case DELETE:
                tx.delete(LogicalDatastoreType.OPERATIONAL, routeId);
                break;
            case UNMODIFIED:
                // No-op
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                tx.put(LogicalDatastoreType.OPERATIONAL, routeId, route.getDataAfter().get());
                // Lookup per-table attributes from RIBSupport
                final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(route.getDataAfter(), ribSupport.routeAttributesIdentifier()).orNull();
                final ContainerNode effectiveAttrs;

                if (advertisedAttrs != null) {
                    effectiveAttrs = policy.effectiveAttributes(advertisedAttrs);
                } else {
                    effectiveAttrs = null;
                }

                LOG.debug("Route {} effective attributes {} towards {}", route.getIdentifier(), effectiveAttrs, routeId);

                if (effectiveAttrs != null) {
                    tx.put(LogicalDatastoreType.OPERATIONAL, routeId.node(ribSupport.routeAttributesIdentifier()), effectiveAttrs);
                } else {
                    LOG.warn("Route {} advertised empty attributes", routeId);
                    tx.delete(LogicalDatastoreType.OPERATIONAL,  routeId);
                }
                break;
            default:
                LOG.warn("Ignoring unhandled route {}", route);
                break;
            }
        }

        private void processTableChildren(final DOMDataWriteTransaction tx, final RIBSupport ribSupport, final NodeIdentifierWithPredicates peerKey, final YangInstanceIdentifier tablePath, final Collection<DataTreeCandidateNode> children) {
            final AbstractImportPolicy policy = EffectiveRibInWriter.this.peerPolicyTracker.policyFor(IdentifierUtils.peerId(peerKey));

            for (final DataTreeCandidateNode child : children) {
                LOG.debug("Process table {} type {}", child, child.getModificationType());
                final YangInstanceIdentifier childPath = tablePath.node(child.getIdentifier());
                switch (child.getModificationType()) {
                case DELETE:
                    tx.delete(LogicalDatastoreType.OPERATIONAL, tablePath.node(child.getIdentifier()));
                    break;
                case UNMODIFIED:
                    // No-op
                    break;
                case SUBTREE_MODIFIED:
                    if (TABLE_ROUTES.equals(child.getIdentifier())) {
                        for (final DataTreeCandidateNode route : ribSupport.changedRoutes(child)) {
                            processRoute(tx, ribSupport, policy, childPath, route);
                        }
                    } else {
                        tx.put(LogicalDatastoreType.OPERATIONAL, childPath, child.getDataAfter().get());
                    }
                    break;
                case WRITE:
                    tx.put(LogicalDatastoreType.OPERATIONAL, childPath, child.getDataAfter().get());
                    // Routes are special, as they may end up being filtered. The previous put conveniently
                    // ensured that we have them in at target, so a subsequent delete will not fail :)
                    if (TABLE_ROUTES.equals(child.getIdentifier())) {
                        for (final DataTreeCandidateNode route : ribSupport.changedRoutes(child)) {
                            processRoute(tx, ribSupport, policy, childPath, route);
                        }
                    }
                    break;
                default:
                    LOG.warn("Ignoring unhandled child {}", child);
                    break;
                }
            }
        }

        private RIBSupportContext getRibSupport(final NodeIdentifierWithPredicates tableKey) {
            return this.registry.getRIBSupportContext(tableKey);
        }

        private YangInstanceIdentifier effectiveTablePath(final NodeIdentifierWithPredicates peerKey, final NodeIdentifierWithPredicates tableKey) {
            return this.ribId.node(Peer.QNAME).node(peerKey).node(EffectiveRibIn.QNAME).node(Tables.QNAME).node(tableKey);
        }

        private void modifyTable(final DOMDataWriteTransaction tx, final NodeIdentifierWithPredicates peerKey, final NodeIdentifierWithPredicates tableKey, final DataTreeCandidateNode table) {
            final RIBSupportContext ribSupport = getRibSupport(tableKey);
            final YangInstanceIdentifier tablePath = effectiveTablePath(peerKey, tableKey);

            processTableChildren(tx, ribSupport.getRibSupport(), peerKey, tablePath, table.getChildNodes());
        }

        private void writeTable(final DOMDataWriteTransaction tx, final NodeIdentifierWithPredicates peerKey, final NodeIdentifierWithPredicates tableKey, final DataTreeCandidateNode table) {
            final RIBSupportContext ribSupport = getRibSupport(tableKey);
            final YangInstanceIdentifier tablePath = effectiveTablePath(peerKey, tableKey);

            // Create an empty table
            ribSupport.clearTable(tx,tablePath);

            processTableChildren(tx, ribSupport.getRibSupport(), peerKey, tablePath, table.getChildNodes());
        }

        @Override
        public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
            LOG.trace("Data changed called to effective RIB. Change : {}", changes);
            final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
            for (final DataTreeCandidate tc : changes) {
                final YangInstanceIdentifier rootPath = tc.getRootPath();

                // Obtain the peer's key
                final NodeIdentifierWithPredicates peerKey = IdentifierUtils.peerKey(rootPath);
                final DataTreeCandidateNode root = tc.getRootNode();

                // call out peer-role has changed
                final DataTreeCandidateNode roleChange =  root.getModifiedChild(AbstractPeerRoleTracker.PEER_ROLE_NID);
                if (roleChange != null) {
                    EffectiveRibInWriter.this.peerPolicyTracker.onDataTreeChanged(roleChange, IdentifierUtils.peerPath(rootPath));
                }

                // filter out any change outside AdjRibsIn
                final DataTreeCandidateNode ribIn =  root.getModifiedChild(ADJRIBIN_NID);
                if (ribIn == null) {
                    LOG.debug("Skipping change {}", tc.getRootNode());
                    continue;
                }
                final DataTreeCandidateNode tables = ribIn.getModifiedChild(TABLES_NID);
                if (tables == null) {
                    LOG.debug("Skipping change {}", tc.getRootNode());
                    continue;
                }
                for (final DataTreeCandidateNode table : tables.getChildNodes()) {
                    final PathArgument lastArg = table.getIdentifier();
                    Verify.verify(lastArg instanceof NodeIdentifierWithPredicates, "Unexpected type %s in path %s", lastArg.getClass(), rootPath);
                    final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) lastArg;

                    switch (root.getModificationType()) {
                    case DELETE:
                        // delete the corresponding effective table
                        tx.delete(LogicalDatastoreType.OPERATIONAL, effectiveTablePath(peerKey, tableKey));
                        break;
                    case SUBTREE_MODIFIED:
                        modifyTable(tx, peerKey, tableKey, table);
                        break;
                    case UNMODIFIED:
                        LOG.info("Ignoring spurious notification on {} data {}", rootPath, table);
                        break;
                    case WRITE:
                        writeTable(tx, peerKey, tableKey, table);
                        break;
                    default:
                        LOG.warn("Ignoring unhandled root {}", root);
                        break;
                    }
                }
            }
            tx.submit();
        }

        @Override
        public void close() {
            // FIXME: wipe all effective routes?
            this.reg.close();
        }
    }

    private final ImportPolicyPeerTracker peerPolicyTracker;
    private final AdjInTracker adjInTracker;

    static EffectiveRibInWriter create(@Nonnull final DOMDataTreeChangeService service, @Nonnull final DOMTransactionChain chain,
        @Nonnull final YangInstanceIdentifier ribId, @Nonnull final PolicyDatabase pd, @Nonnull final RIBSupportContextRegistry registry) {
        return new EffectiveRibInWriter(service, chain, ribId, pd, registry);
    }

    private EffectiveRibInWriter(final DOMDataTreeChangeService service, final DOMTransactionChain chain, final YangInstanceIdentifier ribId,
        final PolicyDatabase pd, final RIBSupportContextRegistry registry) {
        this.peerPolicyTracker = new ImportPolicyPeerTracker(pd);
        this.adjInTracker = new AdjInTracker(service, registry, chain, ribId);
    }

    @Override
    public void close() {
        this.adjInTracker.close();
    }
}
