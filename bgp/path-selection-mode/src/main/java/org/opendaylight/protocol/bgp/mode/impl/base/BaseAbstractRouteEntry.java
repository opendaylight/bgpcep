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
import java.util.Optional;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.mode.api.BestPath;
import org.opendaylight.protocol.bgp.mode.impl.OffsetMap;
import org.opendaylight.protocol.bgp.mode.spi.AbstractRouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.CacheDisconnectedPeers;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
abstract class BaseAbstractRouteEntry extends AbstractRouteEntry {

    private static final Logger LOG = LoggerFactory.getLogger(BaseAbstractRouteEntry.class);
    private static final ContainerNode[] EMPTY_ATTRIBUTES = new ContainerNode[0];
    private OffsetMap<UnsignedInteger> offsets = new OffsetMap<>(UnsignedInteger.class);
    private ContainerNode[] values = EMPTY_ATTRIBUTES;
    private Optional<BaseBestPath> bestPath = Optional.empty();
    private Optional<BaseBestPath> removedBestPath = Optional.empty();

    private int addRoute(final UnsignedInteger routerId, final ContainerNode attributes) {
        int offset = this.offsets.offsetOf(routerId);
        if (offset < 0) {
            final OffsetMap<UnsignedInteger> newOffsets = this.offsets.with(routerId);
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
     * @param offset of removed route
     * @return true if its the last route
     */
    protected final boolean removeRoute(final UnsignedInteger routerId, final int offset) {
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
            final UnsignedInteger routerId = this.offsets.getRouterKey(i);
            final ContainerNode attributes = this.offsets.getValue(this.values, i);
            LOG.trace("Processing router id {} attributes {}", routerId, attributes);
            selector.processPath(routerId, attributes);
        }

        // Get the newly-selected best path.
        final Optional<BaseBestPath> newBestPath = Optional.ofNullable(selector.result());
        final boolean modified = !newBestPath.equals(this.bestPath);
        if (modified) {
            this.removedBestPath = this.bestPath;
            LOG.trace("Previous best {}, current best {}", this.bestPath, newBestPath);
            this.bestPath = newBestPath;
        }
        return modified;
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final Long remotePathId, final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        LOG.trace("Find {} in {}", attributesIdentifier, data);
        final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(data, attributesIdentifier).orNull();
        return addRoute(routerId, advertisedAttrs);
    }

    @Override
    public void updateRoute(final TablesKey localTK, final ExportPolicyPeerTracker peerPT, final YangInstanceIdentifier locRibTarget, final RIBSupport ribSup,
        final CacheDisconnectedPeers discPeers, final DOMDataWriteTransaction tx, final PathArgument routeIdPA) {
        if (this.removedBestPath.isPresent()) {
            removePathFromDataStore(this.removedBestPath.get(), routeIdPA, locRibTarget, peerPT, localTK, ribSup, discPeers, tx);
            this.removedBestPath = Optional.empty();
        }
        if (this.bestPath.isPresent()) {
            addPathToDataStore(this.bestPath.get(), routeIdPA, locRibTarget, ribSup, peerPT, localTK, discPeers, tx);
        }
    }

    @Override
    public void writeRoute(final PeerId destPeer, final PathArgument routeId, final YangInstanceIdentifier rootPath, final PeerExportGroup peerGroup,
        final TablesKey localTK, final ExportPolicyPeerTracker peerPT, final RIBSupport ribSup, final CacheDisconnectedPeers discPeers,
        final DOMDataWriteTransaction tx) {
        if (this.bestPath.isPresent()) {
            final BaseBestPath path = this.bestPath.get();
            if (filterRoutes(path.getPeerId(), destPeer, peerPT, localTK, discPeers)) {
                final ContainerNode effAttrib = peerGroup.effectiveAttributes(path.getPeerId(), path.getAttributes());
                writeRoute(destPeer, getAdjRibOutYII(ribSup, rootPath, routeId, localTK), effAttrib, createValue(routeId, path), ribSup, tx);
            }
        }
    }

    private void removePathFromDataStore(final BestPath path, final PathArgument routeIdPA, final YangInstanceIdentifier locRibTarget,
        final ExportPolicyPeerTracker peerPT, final TablesKey localTK, final RIBSupport ribSup, final CacheDisconnectedPeers discPeers,
        final DOMDataWriteTransaction tx) {
        LOG.trace("Best Path removed {}", path);
        final PathArgument routeIdAddPath = ribSup.getRouteIdAddPath(path.getPathId(), routeIdPA);
        final YangInstanceIdentifier pathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdPA);
        YangInstanceIdentifier pathAddPathTarget = null;
        if (routeIdAddPath != null) {
            pathAddPathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdAddPath);
        }
        fillLocRib(pathAddPathTarget == null ? pathTarget : pathAddPathTarget, null, tx);
        fillAdjRibsOut(null, null, routeIdPA, path.getPeerId(), peerPT, localTK, ribSup, discPeers, tx);
    }

    private void addPathToDataStore(final BestPath path, final PathArgument routeIdPA, final YangInstanceIdentifier locRibTarget,
        final RIBSupport ribSup, final ExportPolicyPeerTracker peerPT, final TablesKey localTK, final CacheDisconnectedPeers discPeers,
        final DOMDataWriteTransaction tx) {
        final PathArgument routeIdAddPath = ribSup.getRouteIdAddPath(path.getPathId(), routeIdPA);
        final YangInstanceIdentifier pathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdPA);
        final NormalizedNode<?, ?> value = createValue(routeIdPA, path);
        NormalizedNode<?, ?> addPathValue = null;
        YangInstanceIdentifier pathAddPathTarget = null;
        if (routeIdAddPath == null) {
            LOG.trace("Selected best value {}", value);
        } else {
            pathAddPathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdAddPath);
            addPathValue = createValue(routeIdAddPath, path);
            LOG.trace("Selected best value {}", addPathValue);
        }
        fillLocRib(pathAddPathTarget == null ? pathTarget : pathAddPathTarget, addPathValue == null ? value : addPathValue, tx);
        fillAdjRibsOut(path.getAttributes(), value, routeIdPA, path.getPeerId(), peerPT, localTK, ribSup, discPeers, tx);
    }

    protected final OffsetMap<UnsignedInteger> getOffsets() {
        return this.offsets;
    }

    @VisibleForTesting
    private void fillAdjRibsOut(final ContainerNode attributes, final NormalizedNode<?, ?> value,
        final PathArgument routeId, final PeerId routePeerId, final ExportPolicyPeerTracker peerPT, final TablesKey localTK, final RIBSupport ribSup,
        final CacheDisconnectedPeers discPeers, final DOMDataWriteTransaction tx) {
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
            final PeerExportGroup peerGroup = peerPT.getPeerGroup(role);
            if (peerGroup != null) {
                final ContainerNode effAttrib = peerGroup.effectiveAttributes(routePeerId, attributes);
                peerGroup.getPeers().stream()
                    .filter(pid -> filterRoutes(routePeerId, pid.getKey(), peerPT, localTK, discPeers))
                    .forEach(pid -> update(pid.getKey(), getAdjRibOutYII(ribSup, pid.getValue().getYii(), routeId, localTK), effAttrib, value, ribSup, tx));
            }
        }
    }

    private void update(final PeerId destPeer, final YangInstanceIdentifier routeTarget, final ContainerNode effAttrib,
        final NormalizedNode<?, ?> value, final RIBSupport ribSup, final DOMDataWriteTransaction tx) {
        if (!writeRoute(destPeer, routeTarget, effAttrib, value, ribSup, tx)) {
            LOG.trace("Removing {} from transaction for peer {}", routeTarget, destPeer);
            tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
        }
    }
}