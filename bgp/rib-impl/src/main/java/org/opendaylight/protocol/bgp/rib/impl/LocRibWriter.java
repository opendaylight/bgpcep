/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class LocRibWriter implements AutoCloseable, DOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(LocRibWriter.class);

    private final Map<PathArgument, RouteEntry> routeEntries = new HashMap<>();
    private final YangInstanceIdentifier locRibTarget;
    private final DOMTransactionChain chain;
    private final ExportPolicyPeerTracker peerPolicyTracker;
    private final NodeIdentifier attributesIdentifier;
    private final Long ourAs;
    private final RIBSupport ribSupport;

    LocRibWriter(final RIBSupport ribSupport, final DOMTransactionChain chain, final YangInstanceIdentifier target, final Long ourAs,
        final DOMDataTreeChangeService service, final PolicyDatabase pd, final TablesKey tablesKey) {
        this.chain = Preconditions.checkNotNull(chain);
        this.locRibTarget = Preconditions.checkNotNull(target).node(LocRib.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(tablesKey)).node(Routes.QNAME);
        this.ourAs = Preconditions.checkNotNull(ourAs);
        this.attributesIdentifier = ribSupport.routeAttributesIdentifier();
        this.peerPolicyTracker = new ExportPolicyPeerTracker(service, target, pd);
        this.ribSupport = ribSupport;

        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL, this.locRibTarget, ribSupport.emptyRoutes());
        tx.submit();

        final YangInstanceIdentifier tableId = target.node(Peer.QNAME).node(Peer.QNAME).node(EffectiveRibIn.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(tablesKey));
        service.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, tableId), this);
    }

    public static LocRibWriter create(@Nonnull final RIBSupport ribSupport, @Nonnull final TablesKey tablesKey, @Nonnull final DOMTransactionChain chain, @Nonnull final YangInstanceIdentifier target,
        @Nonnull final AsNumber ourAs, @Nonnull final DOMDataTreeChangeService service, @Nonnull final PolicyDatabase pd) {
        return new LocRibWriter(ribSupport, chain, target, ourAs.getValue(), service, pd, tablesKey);
    }

    @Override
    public void close() {
        this.peerPolicyTracker.close();
    }

    private static void printChildren(final DataTreeCandidateNode root) {
        LOG.debug("Candidate node {} type {} identifier {}", root, root.getModificationType(), root.getIdentifier());
        if (ModificationType.WRITE.equals(root.getModificationType())) {
            LOG.debug("Data after {}", root.getDataAfter());
        }
        if (root.getChildNodes().isEmpty()) {
            return;
        }
        for (final DataTreeCandidateNode child : root.getChildNodes()) {
            printChildren(child);
        }
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        LOG.trace("Received data change to LocRib {}", Arrays.toString(changes.toArray()));
        /*
         * We use two-stage processing here in hopes that we avoid duplicate
         * calculations when multiple peers have changed a particular entry.
         */
        final Map<RouteUpdateKey, RouteEntry> toUpdate = new HashMap<>();
        for (final DataTreeCandidate tc : changes) {
            final YangInstanceIdentifier path = tc.getRootPath();
            final NodeIdentifierWithPredicates peerKey = IdentifierUtils.peerKey(path);
            final PeerId peerId = IdentifierUtils.peerId(peerKey);
            final UnsignedInteger routerId = RouterIds.routerIdForPeerId(peerId);
            for (final DataTreeCandidateNode child : tc.getRootNode().getChildNodes()) {
                for (final DataTreeCandidateNode route : this.ribSupport.changedRoutes(child)) {
                    final PathArgument routeId = route.getIdentifier();
                    RouteEntry entry = this.routeEntries.get(routeId);
                    if (route.getDataAfter().isPresent()) {
                        if (entry == null) {
                            entry = new RouteEntry();
                            this.routeEntries.put(routeId, entry);
                            LOG.trace("Created new entry for {}", routeId);
                        }
                        LOG.trace("Find {} in {}", this.attributesIdentifier, route.getDataAfter());
                        final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(route.getDataAfter(), this.attributesIdentifier).orNull();
                        entry.addRoute(routerId, advertisedAttrs);
                        LOG.trace("Added route from {} attributes {}", routerId, advertisedAttrs);
                    } else if (entry != null && entry.removeRoute(routerId)) {
                        this.routeEntries.remove(routeId);
                        entry = null;
                        LOG.trace("Removed route from {}", routerId);
                    }
                    LOG.debug("Updated route {} entry {}", routeId, entry);
                    toUpdate.put(new RouteUpdateKey(peerId, routeId), entry);
                }
            }
        }

        // Now walk all updated entries
        for (final Entry<RouteUpdateKey, RouteEntry> e : toUpdate.entrySet()) {
            LOG.trace("Walking through {}", e);
            final RouteEntry entry = e.getValue();
            final NormalizedNode<?, ?> value;

            if (entry != null) {
                if (!entry.selectBest(this.ourAs)) {
                    // Best path has not changed, no need to do anything else. Proceed to next route.
                    LOG.trace("Continuing");
                    continue;
                }
                final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = Builders.mapEntryBuilder();
                b.withNodeIdentifier((NodeIdentifierWithPredicates) e.getKey().getRouteId());
                b.addChild(entry.attributes());
                value = b.build();
                LOG.trace("Selected best value {}", value);
            } else {
                value = null;
            }
            final YangInstanceIdentifier writePath = this.ribSupport.routePath(this.locRibTarget, e.getKey().getRouteId());
            if (value != null) {
                LOG.debug("Write route to LocRib {}", value);
                tx.put(LogicalDatastoreType.OPERATIONAL, writePath, value);
            } else {
                LOG.debug("Delete route from LocRib {}", entry);
                tx.delete(LogicalDatastoreType.OPERATIONAL, writePath);
            }

            /*
             * We need to keep track of routers and populate adj-ribs-out, too. If we do not, we need to
             * expose from which client a particular route was learned from in the local RIB, and have
             * the listener perform filtering.
             *
             * We walk the policy set in order to minimize the amount of work we do for multiple peers:
             * if we have two eBGP peers, for example, there is no reason why we should perform the translation
             * multiple times.
             */
            for (final PeerRole role : PeerRole.values()) {
                final PeerExportGroup peerGroup = this.peerPolicyTracker.getPeerGroup(role);
                if (peerGroup != null) {
                    final ContainerNode attributes = entry.attributes();
                    final PeerId peerId = e.getKey().getPeerId();
                    final ContainerNode effectiveAttributes = peerGroup.effectiveAttributes(peerId, attributes);
                    for (final Entry<PeerId, YangInstanceIdentifier> pid : peerGroup.getPeers()) {
                        // This points to adj-rib-out for a particular peer/table combination
                        final YangInstanceIdentifier routeTarget = pid.getValue().node(e.getKey().getRouteId());
                        if (effectiveAttributes != null && value != null && !peerId.equals(pid.getKey())) {
                            LOG.debug("Write route to AdjRibsOut {}", value);
                            tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget, value);
                            tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget.node(this.attributesIdentifier), effectiveAttributes);
                        } else {
                            LOG.trace("Removing {} from transaction", routeTarget);
                            tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
                        }
                    }
                }
            }
        }
        tx.submit();
    }
}
