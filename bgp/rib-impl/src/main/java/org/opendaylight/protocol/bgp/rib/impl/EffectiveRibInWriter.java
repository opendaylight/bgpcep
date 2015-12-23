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
import org.opendaylight.protocol.bgp.rib.impl.spi.ImportPolicy;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
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
    protected static final NodeIdentifier TABLE_ROUTES = new NodeIdentifier(Routes.QNAME);
    private static final NodeIdentifier ADJRIBIN_NID = new NodeIdentifier(AdjRibIn.QNAME);
    private static final NodeIdentifier TABLES_NID = new NodeIdentifier(Tables.QNAME);

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

        private void processRoute(final DOMDataWriteTransaction tx, final RIBSupport ribSupport, final ImportPolicy policy, final YangInstanceIdentifier routesPath, final DataTreeCandidateNode route) {
            LOG.debug("Process route {}", route.getIdentifier());
            final YangInstanceIdentifier routeId = ribSupport.routePath(routesPath, route.getIdentifier());
            switch (route.getModificationType()) {
            case DELETE:
            case DISAPPEARED:
                tx.delete(LogicalDatastoreType.OPERATIONAL, routeId);
                break;
            case UNMODIFIED:
                // No-op
                break;
            case APPEARED:
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
            final ImportPolicy policy = EffectiveRibInWriter.this.peerPolicyTracker.policyFor(IdentifierUtils.peerId(peerKey));

            for (final DataTreeCandidateNode child : children) {
                final PathArgument childIdentifier = child.getIdentifier();
                final Optional<NormalizedNode<?, ?>> childDataAfter = child.getDataAfter();
                LOG.debug("Process table {} type {}, dataAfter {}, dataBefore {}", childIdentifier, child
                    .getModificationType(), childDataAfter, child.getDataBefore());
                final YangInstanceIdentifier childPath = tablePath.node(childIdentifier);
                switch (child.getModificationType()) {
                case DELETE:
                case DISAPPEARED:
                    tx.delete(LogicalDatastoreType.OPERATIONAL, tablePath.node(childIdentifier));
                    break;
                case UNMODIFIED:
                    // No-op
                    break;
                case SUBTREE_MODIFIED:
                    processModifiedRouteTables(child, childIdentifier,tx, ribSupport, policy, childPath, childDataAfter);
                    break;
                case APPEARED:
                case WRITE:
                    writeRouteTables(child, childIdentifier,tx, ribSupport, policy, childPath, childDataAfter);

                    break;
                default:
                    LOG.warn("Ignoring unhandled child {}", child);
                    break;
                }
            }
        }

        private void processModifiedRouteTables(final DataTreeCandidateNode child, final PathArgument childIdentifier, final DOMDataWriteTransaction tx,
            final RIBSupport ribSupport, final ImportPolicy policy, final YangInstanceIdentifier childPath, final Optional<NormalizedNode<?, ?>> childDataAfter) {
            if (TABLE_ROUTES.equals(childIdentifier)) {
                for (final DataTreeCandidateNode route : ribSupport.changedRoutes(child)) {
                    processRoute(tx, ribSupport, policy, childPath, route);
                }
            } else {
                tx.put(LogicalDatastoreType.OPERATIONAL, childPath, childDataAfter.get());
            }
        }

        private void writeRouteTables(final DataTreeCandidateNode child, final PathArgument childIdentifier, final DOMDataWriteTransaction tx, final RIBSupport ribSupport, final ImportPolicy policy, final YangInstanceIdentifier childPath, final Optional<NormalizedNode<?, ?>> childDataAfter) {
            tx.put(LogicalDatastoreType.OPERATIONAL, childPath, childDataAfter.get());
            // Routes are special, as they may end up being filtered. The previous put conveniently
            // ensured that we have them in at target, so a subsequent delete will not fail :)
            if (TABLE_ROUTES.equals(childIdentifier)) {
                for (final DataTreeCandidateNode route : ribSupport.changedRoutes(child)) {
                    processRoute(tx, ribSupport, policy, childPath, route);
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
            LOG.trace("Create Empty table", tablePath);
            ribSupport.clearTable(tx, tablePath);

            processTableChildren(tx, ribSupport.getRibSupport(), peerKey, tablePath, table.getChildNodes());
        }

        @Override
        public void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes) {
            LOG.trace("Data changed called to effective RIB. Change : {}", changes);

            // we have a lot of transactions created for 'nothing' because a lot of changes
            // are skipped, so ensure we only create one transaction when we really need it
            DOMDataWriteTransaction tx = null;
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
                    LOG.debug("Skipping change {}", root.getIdentifier());
                    continue;
                }
                final DataTreeCandidateNode tables = ribIn.getModifiedChild(TABLES_NID);
                if (tables == null) {
                    LOG.debug("Skipping change {}", root.getIdentifier());
                    continue;
                }
                for (final DataTreeCandidateNode table : tables.getChildNodes()) {
                    if (tx == null) {
                        tx = this.chain.newWriteOnlyTransaction();
                    }
                    changeDataTree(tx, rootPath, root, peerKey, table);
                }
            }
            if (tx != null) {
                tx.submit();
            }
        }

        private void changeDataTree(final DOMDataWriteTransaction tx, final YangInstanceIdentifier rootPath,
            final DataTreeCandidateNode root, final NodeIdentifierWithPredicates peerKey, final DataTreeCandidateNode table) {
            final PathArgument lastArg = table.getIdentifier();
            Verify.verify(lastArg instanceof NodeIdentifierWithPredicates, "Unexpected type %s in path %s", lastArg.getClass(), rootPath);
            final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) lastArg;
            final ModificationType modificationType = root.getModificationType();
            switch (modificationType) {
            case DELETE:
            case DISAPPEARED:
                final YangInstanceIdentifier effectiveTablePath = effectiveTablePath(peerKey, tableKey);
                LOG.debug("Delete Effective Table {} modification type {}, ", effectiveTablePath, modificationType);
                // delete the corresponding effective table
                tx.delete(LogicalDatastoreType.OPERATIONAL, effectiveTablePath);
                break;
            case SUBTREE_MODIFIED:
                modifyTable(tx, peerKey, tableKey, table);
                break;
            case UNMODIFIED:
                LOG.info("Ignoring spurious notification on {} data {}", rootPath, table);
                break;
            case APPEARED:
            case WRITE:
                writeTable(tx, peerKey, tableKey, table);
                break;
            default:
                LOG.warn("Ignoring unhandled root {}", root);
                break;
            }
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
