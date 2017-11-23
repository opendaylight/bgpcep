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
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.mode.api.BestPath;
import org.opendaylight.protocol.bgp.mode.spi.AbstractRouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
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
    private OffsetMap offsets = OffsetMap.EMPTY;
    private ContainerNode[] values = EMPTY_ATTRIBUTES;
    private BaseBestPath bestPath;
    private BaseBestPath removedBestPath;

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
     * @param offset of removed route
     * @return true if its the last route
     */
    protected final boolean removeRoute(final UnsignedInteger routerId, final int offset) {
        this.values = this.offsets.removeValue(this.values, offset);
        this.offsets = this.offsets.without(routerId);
        return this.offsets.isEmpty();
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
        final BaseBestPath newBestPath = selector.result();
        final boolean modified = newBestPath == null || !newBestPath.equals(this.bestPath);
        if (modified) {
            if(this.offsets.isEmpty()) {
                this.removedBestPath = this.bestPath;
            }
            LOG.trace("Previous best {}, current best {}", this.bestPath, newBestPath);
            this.bestPath = newBestPath;
        }
        return modified;
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final Long remotePathId, final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        LOG.trace("Find {} in {}", attributesIdentifier, data);
        final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(data, attributesIdentifier).orElse(null);
        return addRoute(routerId, advertisedAttrs);
    }

    @Override
    public void updateRoute(final TablesKey localTK, final ExportPolicyPeerTracker peerPT, final YangInstanceIdentifier locRibTarget, final RIBSupport ribSup,
        final DOMDataWriteTransaction tx, final PathArgument routeIdPA) {
        if (this.removedBestPath != null) {
            removePathFromDataStore(this.removedBestPath, routeIdPA, locRibTarget, peerPT, localTK, ribSup, tx);
            this.removedBestPath = null;
        }
        if (this.bestPath != null) {
            addPathToDataStore(this.bestPath, routeIdPA, locRibTarget, ribSup, peerPT, localTK, tx);
        }
    }

    @Override
    public void writeRoute(final PeerId destPeer, final PathArgument routeId, final YangInstanceIdentifier rootPath, final PeerExportGroup peerGroup,
        final TablesKey localTK, final ExportPolicyPeerTracker peerPT, final RIBSupport ribSupport, final DOMDataWriteTransaction tx) {
        if (this.bestPath != null) {
            final BaseBestPath path = this.bestPath;
            final PeerRole destPeerRole = getRoutePeerIdRole(peerPT, destPeer);
            if (filterRoutes(path.getPeerId(), destPeer, peerPT, localTK, destPeerRole)) {
                final ContainerNode effAttrib = peerGroup.effectiveAttributes(getRoutePeerIdRole(peerPT,path.getPeerId()), path.getAttributes());
                writeRoute(destPeer, getAdjRibOutYII(ribSupport, rootPath, routeId, localTK), effAttrib,
                    createValue(routeId, path), ribSupport, tx);
            }
        }
    }

    private void removePathFromDataStore(final BestPath path, final PathArgument routeIdPA, final YangInstanceIdentifier locRibTarget,
        final ExportPolicyPeerTracker peerPT, final TablesKey localTK, final RIBSupport ribSup, final DOMDataWriteTransaction tx) {
        LOG.trace("Best Path removed {}", path);
        final PathArgument routeIdAddPath = ribSup.getRouteIdAddPath(path.getPathId(), routeIdPA);
        final YangInstanceIdentifier pathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdPA);
        YangInstanceIdentifier pathAddPathTarget = null;
        if (routeIdAddPath != null) {
            pathAddPathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdAddPath);
        }
        fillLocRib(pathAddPathTarget == null ? pathTarget : pathAddPathTarget, null, tx);
        fillAdjRibsOut(null, null, routeIdPA, path.getPeerId(), peerPT, localTK, ribSup, tx);
    }

    private void addPathToDataStore(final BestPath path, final PathArgument routeIdPA, final YangInstanceIdentifier locRibTarget,
        final RIBSupport ribSup, final ExportPolicyPeerTracker peerPT, final TablesKey localTK, final DOMDataWriteTransaction tx) {
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
        fillAdjRibsOut(path.getAttributes(), value, routeIdPA, path.getPeerId(), peerPT, localTK, ribSup, tx);
    }

    final OffsetMap getOffsets() {
        return this.offsets;
    }

    @VisibleForTesting
    private void fillAdjRibsOut(final ContainerNode attributes, final NormalizedNode<?, ?> value,
        final PathArgument routeId, final PeerId routePeerId, final ExportPolicyPeerTracker peerPT,
        final TablesKey localTK, final RIBSupport ribSup, final DOMDataWriteTransaction tx) {
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
                final ContainerNode effAttrib = peerGroup.effectiveAttributes(getRoutePeerIdRole(peerPT, routePeerId),
                    attributes);
                peerGroup.forEach((destPeer, rootPath) -> {
                    if (!filterRoutes(routePeerId, destPeer, peerPT, localTK, getRoutePeerIdRole(peerPT, destPeer))) {
                        return;
                    }
                    update(destPeer, getAdjRibOutYII(ribSup, rootPath, routeId, localTK), effAttrib, value, ribSup, tx);
                });
            }
        }
    }
}