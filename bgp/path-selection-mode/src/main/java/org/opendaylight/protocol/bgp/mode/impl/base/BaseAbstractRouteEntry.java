/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedInteger;
import java.util.Map;
import java.util.Optional;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.mode.api.BestPath;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.CacheDisconnectedPeers;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.bgp.rib.spi.policy.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.policy.PeerExportGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
abstract class BaseAbstractRouteEntry implements RouteEntry {

    private static final NodeIdentifier ROUTES_IDENTIFIER = new NodeIdentifier(Routes.QNAME);
    private static final Logger LOG = LoggerFactory.getLogger(BaseAbstractRouteEntry.class);
    private static final ContainerNode[] EMPTY_ATTRIBUTES = new ContainerNode[0];
    private final ExportPolicyPeerTracker peerPolicyTracker;
    private final TablesKey localTablesKey;
    private final CacheDisconnectedPeers cacheDisconnectedPeers;
    private final NodeIdentifierWithPredicates tableKey;
    private final NodeIdentifier attributesIdentifier;
    private final RIBSupport ribSupport;
    private final YangInstanceIdentifier routesYangId;
    private final PathArgument routeId;

    private OffsetMap offsets = OffsetMap.EMPTY;
    private ContainerNode[] values = EMPTY_ATTRIBUTES;
    private Optional<BaseBestPath> bestPath = Optional.empty();
    private Optional<BaseBestPath> removedBestPath = Optional.empty();

    BaseAbstractRouteEntry(final PathArgument routeId, final YangInstanceIdentifier routesYangId, final RIBSupport ribSupport, final ExportPolicyPeerTracker exportPPT,
        final TablesKey localTK, final CacheDisconnectedPeers cacheDP) {
        this.routeId = routeId;
        this.routesYangId = routesYangId;
        this.peerPolicyTracker = exportPPT;
        this.tableKey = RibSupportUtils.toYangTablesKey(localTK);
        this.localTablesKey = localTK;
        this.cacheDisconnectedPeers = cacheDP;
        this.ribSupport = ribSupport;
        this.attributesIdentifier = this.ribSupport.routeAttributesIdentifier();
    }

    private int addRoute(final UnsignedInteger routerId, final ContainerNode attributes) {
        int offset = this.offsets.offsetOf(routerId);
        if (offset < 0) {
            final OffsetMap newOffsets = this.offsets.with(routerId);
            offset = newOffsets.offsetOf(routerId);

            this.values = newOffsets.expand(this.offsets, this.values, offset);
            this.offsets = newOffsets;
        }

        this.offsets.setValue(this.values, offset, attributes);
        LOG.trace("Added route from {} attributes {}", routerId, attributes);
        return offset;
    }

    /**
     * Remove route
     *
     * @param routerId router ID in unsigned integer
     * @param offset offset Offset of removed route
     * @return true if its the last route
     */
    final boolean removeRoute(final UnsignedInteger routerId, final int offset) {
        this.values = this.offsets.removeValue(this.values, offset);
        this.offsets = this.offsets.without(routerId);
        return this.offsets.isEmty();
    }

    @Override
    public final boolean selectBest(final long localAs) {
        /*
         * FIXME: optimize flaps by making sure we consider stability of currently-selected route.
         */
        final BasePathSelector selector = new BasePathSelector(localAs);

        // Select the best route.
        for (int i = 0; i < this.offsets.size(); ++i) {
            final UnsignedInteger routerId = this.offsets.getRouterId(i);
            final ContainerNode attributes = this.offsets.getValue(this.values, i);
            LOG.trace("Processing router id {} attributes {}", routerId, attributes);
            selector.processPath(routerId, attributes);
        }

        // Get the newly-selected best path.
        final Optional<BaseBestPath> newBestPath = Optional.ofNullable(selector.result());
        final boolean modified = !newBestPath.equals(this.bestPath);
        if (modified) {
            this.removedBestPath = this.bestPath;
        }

        LOG.trace("Previous best {}, current best {}, result {}", this.bestPath, newBestPath, modified);
        this.bestPath = newBestPath;
        return modified;
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        LOG.trace("Find {} in {}", attributesIdentifier, data);
        final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(data, attributesIdentifier).orNull();
        return addRoute(routerId, advertisedAttrs);
    }

    @Override
    public void updateRoute(final DOMDataWriteTransaction tx) {
        if (this.removedBestPath.isPresent()) {
            removePathFromDataStore(this.removedBestPath.get(), tx);
            this.removedBestPath = Optional.empty();
        }

        if (this.bestPath.isPresent()) {
            addPathToDataStore(this.bestPath.get(), tx);
        }
    }

    @Override
    public void writeRoute(final PeerId destinyPeer, final PathArgument routeId, final YangInstanceIdentifier rootPath,
        final PeerExportGroup peerGroup, final DOMDataWriteTransaction tx) {
        final BaseBestPath path = this.bestPath.get();
        final ContainerNode effectiveAttributes = peerGroup.effectiveAttributes(path.getPeerId(), path.getAttributes());
        writeRoute(destinyPeer, getAdjRibOutYII(rootPath, routeId), effectiveAttributes, createValue(routeId, path), tx);
    }

    private void removePathFromDataStore(final BestPath path, final DOMDataWriteTransaction tx) {
        LOG.trace("Best Path removed {}", path);
        final PathArgument routeIdAddPath = this.ribSupport.getRouteIdAddPath(path.getPathId(), this.routeId);
        final YangInstanceIdentifier pathTarget = this.ribSupport.routePath(this.routesYangId, this.routeId);
        YangInstanceIdentifier pathAddPathTarget = null;
        if (routeIdAddPath != null) {
            pathAddPathTarget = this.ribSupport.routePath(this.routesYangId, routeIdAddPath);
        }
        fillLocRib(pathAddPathTarget == null ? pathTarget : pathAddPathTarget, null, tx);
        fillAdjRibsOut(path.getPeerId(), null, null, tx);
    }

    private void addPathToDataStore(final BestPath path, final DOMDataWriteTransaction tx) {
        final PathArgument routeIdAddPath = this.ribSupport.getRouteIdAddPath(path.getPathId(), this.routeId);
        final YangInstanceIdentifier pathTarget = this.ribSupport.routePath(this.routesYangId, this.routeId);
        final NormalizedNode<?, ?> value = createValue(this.routeId, path);
        NormalizedNode<?, ?> addPathValue = null;
        YangInstanceIdentifier pathAddPathTarget = null;
        if (routeIdAddPath == null) {
            LOG.trace("Selected best value {}", value);
        } else {
            pathAddPathTarget = this.ribSupport.routePath(this.routesYangId, routeIdAddPath);
            addPathValue = createValue(routeIdAddPath, path);
            LOG.trace("Selected best value {}", addPathValue);
        }
        fillLocRib(pathAddPathTarget == null ? pathTarget : pathAddPathTarget, addPathValue == null ? value : addPathValue, tx);
        fillAdjRibsOut(path.getPeerId(), path.getAttributes(), value, tx);
    }

    private boolean writeRoute(final PeerId destinyPeer, final YangInstanceIdentifier routeTarget, final ContainerNode effectiveAttributes,
        final NormalizedNode<?, ?> value, final DOMDataWriteTransaction tx) {
        if (effectiveAttributes != null && value != null) {
            LOG.debug("Write route {} to peer AdjRibsOut {}", value, destinyPeer);
            tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget, value);
            tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget.node(this.attributesIdentifier), effectiveAttributes);
            return true;
        }
        return false;
    }

    final OffsetMap getOffsets() {
        return this.offsets;
    }

    private void fillLocRib(final YangInstanceIdentifier writePath, final NormalizedNode<?, ?> value, final DOMDataWriteTransaction tx) {
        if (value != null) {
            LOG.debug("Write route to LocRib {}", value);
            tx.put(LogicalDatastoreType.OPERATIONAL, writePath, value);
        } else {
            LOG.debug("Delete route from LocRib {}", writePath);
            tx.delete(LogicalDatastoreType.OPERATIONAL, writePath);
        }
    }

    @VisibleForTesting
    private void fillAdjRibsOut(final PeerId routePeerId, final ContainerNode attributes, final NormalizedNode<?, ?> value,
        final DOMDataWriteTransaction tx) {
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
            if (PeerRole.Internal.equals(role)) {
                continue;
            }
            final PeerExportGroup peerGroup = this.peerPolicyTracker.getPeerGroup(role);
            if (peerGroup != null) {
                final ContainerNode effectiveAttributes = peerGroup.effectiveAttributes(routePeerId, attributes);
                peerGroup.getPeers().stream()
                    .filter(pid -> filterRoutes(pid, routePeerId))
                    .forEach(pid -> update(pid.getKey(), getAdjRibOutYII(pid.getValue(), this.routeId), effectiveAttributes, value, tx));
            }
        }
    }

    private boolean filterRoutes(final Map.Entry<PeerId, YangInstanceIdentifier> pid, final PeerId destinyPeer) {
        final PeerId peerId = pid.getKey();
        return !peerId.equals(destinyPeer) && isTableSupported(peerId) && !this.cacheDisconnectedPeers.isPeerDisconnected(peerId);
    }

    private void update(final PeerId destinyPeer, final YangInstanceIdentifier routeTarget, final ContainerNode effectiveAttributes,
        final NormalizedNode<?, ?> value, final DOMDataWriteTransaction tx) {
        if (!writeRoute(destinyPeer, routeTarget, effectiveAttributes, value, tx)) {
            LOG.trace("Removing {} from transaction for peer {}", routeTarget, destinyPeer);
            tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
        }
    }

    private boolean isTableSupported(final PeerId peerId) {
        if (!this.peerPolicyTracker.isTableSupported(peerId)) {
            LOG.trace("Route rejected, peer {} does not support this table type {}", peerId, this.localTablesKey);
            return false;
        }
        return true;
    }


    private YangInstanceIdentifier getAdjRibOutYII(final YangInstanceIdentifier rootPath, final PathArgument routeId) {
        return this.ribSupport.routePath(rootPath.node(AdjRibOut.QNAME).node(Tables.QNAME).node(this.tableKey).node(ROUTES_IDENTIFIER), routeId);
    }
}