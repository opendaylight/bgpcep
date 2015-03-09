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
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart._case.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.EffectiveRibIn;
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
    private static final NodeIdentifier TABLE_ROUTES = new NodeIdentifier(Routes.QNAME);

    /**
     * Maintains {@link TableRouteListener} instances.
     */
    private final class AdjInTracker implements AutoCloseable, DOMDataTreeChangeListener {
        private final RIBExtensionConsumerContext registry;
        private final YangInstanceIdentifier ribId;
        private final ListenerRegistration<?> reg;
        private final DOMTransactionChain chain;

        AdjInTracker(final DOMDataTreeChangeService service, final RIBExtensionConsumerContext registry, final DOMTransactionChain chain, final YangInstanceIdentifier ribId) {
            this.registry = Preconditions.checkNotNull(registry);
            this.chain = Preconditions.checkNotNull(chain);
            this.ribId = Preconditions.checkNotNull(ribId);

            final YangInstanceIdentifier tableId = ribId.node(Peer.QNAME).node(AdjRibIn.QNAME).node(Tables.QNAME);
            final DOMDataTreeIdentifier treeId = new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, tableId);
            this.reg = service.registerDataTreeChangeListener(treeId, this);
        }

        private void processRoute(final DOMDataWriteTransaction tx, final RIBSupport ribSupport, final AbstractImportPolicy policy, final YangInstanceIdentifier routesPath, final DataTreeCandidateNode route) {
            switch (route.getModificationType()) {
            case DELETE:
                // Delete has already been affected by the store in caller, so this is a no-op.
                break;
            case MERGE:
                LOG.info("Merge on {} reported, this should never have happened, ignoring", route);
                break;
            case UNMODIFIED:
                // No-op
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                // Lookup per-table attributes from RIBSupport
                final ContainerNode adverisedAttrs = (ContainerNode) NormalizedNodes.findNode(route.getDataAfter(), ribSupport.routeAttributes()).orNull();
                final ContainerNode effectiveAttrs;

                if (adverisedAttrs != null) {
                    effectiveAttrs = policy.effectiveAttributes(adverisedAttrs);

                    /*
                     * Speed hack: if we determine that the policy has passed the attributes
                     * back unmodified, the corresponding change has already been written in
                     * our caller. There is no need to perform any further processing.
                     *
                     * We also use direct object comparison to make the check very fast, as
                     * it may not be that common, in which case it does not make sense to pay
                     * the full equals price.
                     */
                    if (effectiveAttrs == adverisedAttrs) {
                        return;
                    }
                } else {
                    effectiveAttrs = null;
                }

                final YangInstanceIdentifier routeId = ribSupport.routePath(routesPath, route.getIdentifier());
                LOG.debug("Route {} effective attributes {} towards {}", route.getIdentifier(), effectiveAttrs, routeId);

                if (effectiveAttrs != null) {
                    tx.put(LogicalDatastoreType.OPERATIONAL, routeId.node(ribSupport.routeAttributes()), effectiveAttrs);
                } else {
                    LOG.warn("Route {} advertised empty attributes", route.getDataAfter());
                    tx.delete(LogicalDatastoreType.OPERATIONAL,  routeId);
                }
                break;
            default:
                LOG.warn("Ignoring unhandled route {}", route);
                break;
            }
        }

        private void processTableChildren(final DOMDataWriteTransaction tx, final RIBSupport ribSupport, final AbstractImportPolicy policy, final YangInstanceIdentifier tablePath, final Collection<DataTreeCandidateNode> children) {
            for (DataTreeCandidateNode child : children) {
                switch (child.getModificationType()) {
                case DELETE:
                    tx.delete(LogicalDatastoreType.OPERATIONAL, tablePath.node(child.getIdentifier()));
                    break;
                case MERGE:
                    LOG.info("Merge on {} reported, this should never have happened, ignoring", child);
                    break;
                case UNMODIFIED:
                    // No-op
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    tx.put(LogicalDatastoreType.OPERATIONAL, tablePath.node(child.getIdentifier()), child.getDataAfter().get());

                    // Routes are special, as they may end up being filtered. The previous put conveniently
                    // ensured that we have them in at target, so a subsequent delete will not fail :)
                    if (TABLE_ROUTES.equals(child.getIdentifier())) {
                        final YangInstanceIdentifier routesPath = tablePath.node(Routes.QNAME);
                        for (DataTreeCandidateNode route : ribSupport.changedRoutes(child)) {
                            processRoute(tx, ribSupport, policy, routesPath, route);
                        }
                    }
                    break;
                default:
                    LOG.warn("Ignoring unhandled child {}", child);
                    break;
                }
            }
        }

        private RIBSupport getRibSupport(final NodeIdentifierWithPredicates tableKey) {
            // FIXME: use codec to translate tableKey
            return registry.getRIBSupport(null);
        }

        private YangInstanceIdentifier effectiveTablePath(final NodeIdentifierWithPredicates peerKey, final NodeIdentifierWithPredicates tableKey) {
            return ribId.node(peerKey).node(EffectiveRibIn.QNAME).node(tableKey);
        }

        private void modifyTable(final DOMDataWriteTransaction tx, final NodeIdentifierWithPredicates peerKey, final NodeIdentifierWithPredicates tableKey, final DataTreeCandidateNode table) {
            final RIBSupport ribSupport = getRibSupport(tableKey);
            final YangInstanceIdentifier tablePath = effectiveTablePath(peerKey, tableKey);

            final AbstractImportPolicy policy = peerPolicyTracker.policyFor(IdentifierUtils.peerId(peerKey));
            processTableChildren(tx, ribSupport, policy, tablePath, table.getChildNodes());
        }

        private void writeTable(final DOMDataWriteTransaction tx, final NodeIdentifierWithPredicates peerKey, final NodeIdentifierWithPredicates tableKey, final DataTreeCandidateNode table) {
            final RIBSupport ribSupport = getRibSupport(tableKey);
            final YangInstanceIdentifier tablePath = effectiveTablePath(peerKey, tableKey);

            // Create an empty table
            TableContext.clearTable(tx, ribSupport, tablePath);

            final AbstractImportPolicy policy = peerPolicyTracker.policyFor(IdentifierUtils.peerId(peerKey));
            processTableChildren(tx, ribSupport, policy, tablePath, table.getChildNodes());
        }

        @Override
        public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
            final DOMDataWriteTransaction tx = chain.newWriteOnlyTransaction();

            for (DataTreeCandidate tc : changes) {
                final YangInstanceIdentifier rootPath = tc.getRootPath();

                // Obtain the peer's key
                final NodeIdentifierWithPredicates peerKey = IdentifierUtils.peerKey(rootPath);

                // Extract the table key, this should be safe based on the path where we subscribed,
                // but let's verify explicitly.
                final PathArgument lastArg = rootPath.getLastPathArgument();
                Verify.verify(lastArg instanceof NodeIdentifierWithPredicates, "Unexpected type %s in path %s", lastArg.getClass(), rootPath);
                final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) lastArg;

                final DataTreeCandidateNode root = tc.getRootNode();
                switch (root.getModificationType()) {
                case DELETE:
                    // delete the corresponding effective table
                    tx.delete(LogicalDatastoreType.OPERATIONAL, effectiveTablePath(peerKey, tableKey));
                    break;
                case MERGE:
                    // TODO: upstream API should never give us this, as it leaks how the delta was created.
                    LOG.info("Merge on {} reported, this should never have happened, but attempting to cope", rootPath);
                    modifyTable(tx, peerKey, tableKey, root);
                    break;
                case SUBTREE_MODIFIED:
                    modifyTable(tx, peerKey, tableKey, root);
                    break;
                case UNMODIFIED:
                    LOG.info("Ignoring spurious notification on {} data {}", rootPath, root);
                    break;
                case WRITE:
                    writeTable(tx, peerKey, tableKey, root);
                    break;
                default:
                    LOG.warn("Ignoring unhandled root {}", root);
                    break;
                }
            }

            tx.submit();
        }

        @Override
        public void close() {
            // FIXME: wipe all effective routes?
            reg.close();
        }
    }

    private final ImportPolicyPeerTracker peerPolicyTracker;
    private final AdjInTracker adjInTracker;

    private EffectiveRibInWriter(final DOMDataTreeChangeService service, final DOMTransactionChain chain, final YangInstanceIdentifier ribId) {
        this.peerPolicyTracker = new ImportPolicyPeerTracker(service, ribId);
        // FIXME: proper argument
        this.adjInTracker = new AdjInTracker(service, null, chain, ribId);
    }

    @Override
    public void close() {
        adjInTracker.close();
        peerPolicyTracker.close();
    }
}
