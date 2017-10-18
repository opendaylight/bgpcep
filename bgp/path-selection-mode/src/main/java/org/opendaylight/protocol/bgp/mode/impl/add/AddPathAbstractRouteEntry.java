/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.mode.api.BestPath;
import org.opendaylight.protocol.bgp.mode.spi.AbstractRouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single route entry inside a route table. Maintains the attributes of
 * from all contributing peers. The information is stored in arrays with a
 * shared map of offsets for peers to allow lookups. This is needed to
 * maintain low memory overhead in face of large number of routes and peers,
 * where individual object overhead becomes the dominating factor.
 */
@NotThreadSafe
public abstract class AddPathAbstractRouteEntry extends AbstractRouteEntry {

    private static final Logger LOG = LoggerFactory.getLogger(AddPathAbstractRouteEntry.class);
    private List<AddPathBestPath> bestPath;
    private List<AddPathBestPath> bestPathRemoved;
    protected OffsetMap offsets = OffsetMap.EMPTY;
    protected ContainerNode[] values = new ContainerNode[0];
    protected Long[] pathsId = new Long[0];
    private long pathIdCounter = 0L;
    private boolean oldNonAddPathBestPathTheSame;
    private List<AddPathBestPath> newBestPathToBeAdvertised;
    private List<RemovedPath> removedPaths;

    private static final class RemovedPath {
        private final RouteKey key;
        private final Long pathId;

        RemovedPath(final RouteKey key, final Long pathId) {
            this.key = key;
            this.pathId = pathId;
        }

        Long getPathId() {
            return this.pathId;
        }

        UnsignedInteger getRouteId() {
            return this.key.getRouteId();
        }
    }

    private int addRoute(final RouteKey key, final ContainerNode attributes) {
        int offset = this.offsets.offsetOf(key);
        if (offset < 0) {
            final OffsetMap newOffsets = this.offsets.with(key);
            offset = newOffsets.offsetOf(key);
            final ContainerNode[] newAttributes = newOffsets.expand(this.offsets, this.values, offset);
            final Long[] newPathsId = newOffsets.expand(this.offsets, this.pathsId, offset);
            this.values = newAttributes;
            this.offsets = newOffsets;
            this.pathsId = newPathsId;
            this.offsets.setValue(this.pathsId, offset, ++this.pathIdCounter);
        }
        this.offsets.setValue(this.values, offset, attributes);
        LOG.trace("Added route from {} attributes {}", key.getRouteId(), attributes);
        return offset;
    }

    protected int addRoute(final RouteKey key, final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        LOG.trace("Find {} in {}", attributesIdentifier, data);
        final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(data, attributesIdentifier).orNull();
        return addRoute(key, advertisedAttrs);
    }

    /**
     * Remove route
     *
     * @param key RouteKey of removed route
     * @param offset Offset of removed route
     * @return true if it was the last route
     */
    protected final boolean removeRoute(final RouteKey key, final int offset) {
        final Long pathId = this.offsets.getValue(this.pathsId, offset);
        this.values = this.offsets.removeValue(this.values, offset);
        this.pathsId = this.offsets.removeValue(this.pathsId, offset);
        this.offsets = this.offsets.without(key);
        if(this.removedPaths == null) {
            this.removedPaths = new ArrayList<>();
        }
        this.removedPaths.add(new RemovedPath(key, pathId));
        return isEmpty();
    }

    @Override
    public void updateRoute(final TablesKey localTK, final ExportPolicyPeerTracker peerPT, final YangInstanceIdentifier locRibTarget,
        final RIBSupport ribSupport, final DOMDataWriteTransaction tx, final PathArgument routeIdPA) {
        if(this.bestPathRemoved != null) {
            this.bestPathRemoved.forEach(path -> {
                final PathArgument routeIdAddPath = ribSupport.getRouteIdAddPath(path.getPathId(), routeIdPA);
                final YangInstanceIdentifier pathAddPathTarget = ribSupport.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdAddPath);
                fillLocRib(pathAddPathTarget, null, tx);
            });
            this.bestPathRemoved = null;
        }
        if(this.removedPaths != null) {
            this.removedPaths.forEach(removedPath -> {
                final PathArgument routeIdAddPath = ribSupport.getRouteIdAddPath(removedPath.getPathId(), routeIdPA);
                fillAdjRibsOut(true, null, null, null, routeIdPA, routeIdAddPath, RouterIds.createPeerId(removedPath.getRouteId()),
                    peerPT, localTK, ribSupport, tx);
            });
            this.removedPaths = null;
        }

        if(this.newBestPathToBeAdvertised != null) {
            this.newBestPathToBeAdvertised.forEach(path -> addPathToDataStore(path, isFirstBestPath(this.bestPath.indexOf(path)), routeIdPA,
                locRibTarget, ribSupport, peerPT, localTK, tx));
            this.newBestPathToBeAdvertised = null;
        }
    }

    @Override
    public void writeRoute(final PeerId destPeer, final PathArgument routeId, final YangInstanceIdentifier rootPath, final PeerExportGroup peerGroup,
        final TablesKey localTK, final ExportPolicyPeerTracker peerPT, final RIBSupport ribSup, final DOMDataWriteTransaction tx) {
        final boolean destPeerSupAddPath = peerPT.isAddPathSupportedByPeer(destPeer);
        if(this.bestPath != null) {
            final PeerRole destPeerRole = getRoutePeerIdRole(peerPT, destPeer);
            this.bestPath.stream().filter(path -> filterRoutes(path.getPeerId(), destPeer, peerPT, localTK, destPeerRole) &&
                peersSupportsAddPathOrIsFirstBestPath(destPeerSupAddPath, isFirstBestPath(this.bestPath.indexOf(path))))
                .forEach(path -> writeRoutePath(destPeer, routeId, peerPT, peerGroup, destPeerSupAddPath, path, rootPath, localTK, ribSup, tx));
        }
    }

    private void writeRoutePath(final PeerId destPeer, final PathArgument routeId, final ExportPolicyPeerTracker peerPT,
        final PeerExportGroup peerGroup, final boolean destPeerSupAddPath,
        final BestPath path, final YangInstanceIdentifier rootPath, final TablesKey localTK, final RIBSupport ribSup, final DOMDataWriteTransaction tx) {
        final PathArgument routeIdAddPath = ribSup.getRouteIdAddPath(path.getPathId(), routeId);
        final ContainerNode effectiveAttributes = peerGroup.effectiveAttributes(getRoutePeerIdRole(peerPT,path.getPeerId()), path.getAttributes());
        if (destPeerSupAddPath) {
            writeRoute(destPeer, getAdjRibOutYII(ribSup, rootPath, routeIdAddPath, localTK), effectiveAttributes, createValue(routeIdAddPath, path), ribSup, tx);
        } else {
            writeRoute(destPeer, getAdjRibOutYII(ribSup, rootPath, routeId, localTK), effectiveAttributes, createValue(routeId, path), ribSup, tx);
        }
    }

    private void addPathToDataStore(final BestPath path, final boolean isFirstBestPath, final PathArgument routeIdPA, final YangInstanceIdentifier locRibTarget,
        final RIBSupport ribSup, final ExportPolicyPeerTracker peerPT, final TablesKey localTK, final DOMDataWriteTransaction tx) {
        final PathArgument routeIdAddPath = ribSup.getRouteIdAddPath(path.getPathId(), routeIdPA);
        final YangInstanceIdentifier pathAddPathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdAddPath);
        final MapEntryNode addPathValue = createValue(routeIdAddPath, path);
        final MapEntryNode value = createValue(routeIdPA, path);
        LOG.trace("Selected best value {}", addPathValue);
        fillLocRib(pathAddPathTarget, addPathValue, tx);
        fillAdjRibsOut(isFirstBestPath, path.getAttributes(), value, addPathValue, routeIdPA, routeIdAddPath, path.getPeerId(), peerPT, localTK,
            ribSup, tx);
    }

    private void fillAdjRibsOut(final boolean isFirstBestPath, final ContainerNode attributes, final NormalizedNode<?, ?> value, final MapEntryNode addPathValue,
        final PathArgument routeId, final PathArgument routeIdAddPath, final PeerId routePeerId, final ExportPolicyPeerTracker peerPT, final TablesKey
        localTK, final RIBSupport ribSup, final DOMDataWriteTransaction tx) {
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
                final ContainerNode effectiveAttributes = peerGroup.effectiveAttributes(getRoutePeerIdRole(peerPT, routePeerId), attributes);
                peerGroup.forEach((destPeer, rootPath) -> {
                    final boolean destPeerSupAddPath = peerPT.isAddPathSupportedByPeer(destPeer);
                    if (filterRoutes(routePeerId, destPeer, peerPT, localTK, role) &&
                        peersSupportsAddPathOrIsFirstBestPath(destPeerSupAddPath, isFirstBestPath)) {
                        if (destPeerSupAddPath) {
                            update(destPeer, getAdjRibOutYII(ribSup, rootPath, routeIdAddPath, localTK), effectiveAttributes,
                                addPathValue, ribSup, tx);
                        } else if(!this.oldNonAddPathBestPathTheSame){
                            update(destPeer, getAdjRibOutYII(ribSup, rootPath, routeId, localTK), effectiveAttributes, value, ribSup, tx);
                        }
                    }
                });
            }
        }
    }


    protected final OffsetMap getOffsets() {
        return this.offsets;
    }

    public final boolean isEmpty() {
        return this.offsets.isEmpty();
    }

    private void selectBest(final RouteKey key, final AddPathSelector selector) {
        final int offset = this.offsets.offsetOf(key);
        final ContainerNode attributes = this.offsets.getValue(this.values, offset);
        final Long pathId = this.offsets.getValue(this.pathsId, offset);
        LOG.trace("Processing router key {} attributes {}", key, attributes);
        selector.processPath(attributes, key, offset, pathId);
    }

    /**
     * Process best path selection
     *
     * @param localAs The local autonomous system number
     * @param keyList List of RouteKey
     * @return the best path inside offset map passed
     */
    protected AddPathBestPath selectBest(final long localAs, final List<RouteKey> keyList) {
        /*
         * FIXME: optimize flaps by making sure we consider stability of currently-selected route.
         */
        final AddPathSelector selector = new AddPathSelector(localAs);
        Lists.reverse(keyList).forEach(key -> selectBest(key, selector));
        LOG.trace("Best path selected {}", this.bestPath);
        return selector.result();
    }

    private static boolean isFirstBestPath(final int bestPathPosition) {
        return bestPathPosition == 0;
    }

    private static boolean peersSupportsAddPathOrIsFirstBestPath(final boolean peerSupportsAddPath,
            final boolean isFirstBestPath) {
        return !(!peerSupportsAddPath && !isFirstBestPath);
    }

    protected boolean isBestPathNew(final List<AddPathBestPath> newBestPathList) {
        this.oldNonAddPathBestPathTheSame = isNonAddPathBestPathTheSame(newBestPathList);
        filterRemovedPaths(newBestPathList);
        if (this.bestPathRemoved != null && !this.bestPathRemoved.isEmpty() || newBestPathList != null && !newBestPathList.equals(this.bestPath)) {
            this.newBestPathToBeAdvertised = new ArrayList<>(newBestPathList);
            if(this.bestPath != null) {
                this.newBestPathToBeAdvertised.removeAll(this.bestPath);
            }
            this.bestPath = newBestPathList;
            LOG.trace("Actual Best {}, removed best {}", this.bestPath, this.bestPathRemoved);
            return true;
        }
        return false;
    }

    private boolean isNonAddPathBestPathTheSame(final List<AddPathBestPath> newBestPathList) {
        return !(isEmptyOrNull(this.bestPath) || isEmptyOrNull(newBestPathList)) &&
            this.bestPath.get(0).equals(newBestPathList.get(0));
    }

    private static boolean isEmptyOrNull(final List<AddPathBestPath> pathList) {
        return pathList == null || pathList.isEmpty();
    }

    private void filterRemovedPaths(final List<AddPathBestPath> newBestPathList) {
        if(this.bestPath == null) {
            return;
        }
        this.bestPathRemoved = new ArrayList<>(this.bestPath);
        this.bestPath.forEach(oldBest -> {
            final Optional<AddPathBestPath> present = newBestPathList.stream()
                .filter(newBest -> newBest.getPathId() == oldBest.getPathId() && newBest.getRouteKey() == oldBest.getRouteKey()).findAny();
            present.ifPresent(addPathBestPath -> this.bestPathRemoved.remove(oldBest));
        });
    }
}
