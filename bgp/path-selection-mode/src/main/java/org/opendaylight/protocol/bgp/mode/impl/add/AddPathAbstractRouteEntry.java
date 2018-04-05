/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID_VALUE;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.mode.impl.BGPRouteEntryExportParametersImpl;
import org.opendaylight.protocol.bgp.mode.spi.AbstractRouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
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
public abstract class AddPathAbstractRouteEntry extends AbstractRouteEntry<AddPathBestPath> {

    private static final Logger LOG = LoggerFactory.getLogger(AddPathAbstractRouteEntry.class);
    private List<AddPathBestPath> bestPath;
    private List<AddPathBestPath> bestPathRemoved;
    protected OffsetMap offsets = OffsetMap.EMPTY;
    protected Attributes[] values = new Attributes[0];
    protected Long[] pathsId = new Long[0];
    private long pathIdCounter = 0L;
    private boolean oldNonAddPathBestPathTheSame;
    private List<AddPathBestPath> newBestPathToBeAdvertised;
    private List<RemovedPath> removedPaths;

    public AddPathAbstractRouteEntry(final BGPPeerTracker peerTracker) {
        super(peerTracker);
    }

    private static final class RemovedPath {
        private final RouteKey key;
        private final long pathId;

        RemovedPath(final RouteKey key, final long pathId) {
            this.key = key;
            this.pathId = pathId;
        }

        long getPathId() {
            return this.pathId;
        }

        UnsignedInteger getRouteId() {
            return this.key.getRouteId();
        }
    }

    protected int addRoute(final RouteKey key, final Attributes attributes) {
        int offset = this.offsets.offsetOf(key);
        if (offset < 0) {
            final OffsetMap newOffsets = this.offsets.with(key);
            offset = newOffsets.offsetOf(key);
            final Attributes[] newAttributes = newOffsets.expand(this.offsets, this.values, offset);
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

    /**
     * Remove route.
     *
     * @param key RouteKey of removed route
     * @param offset Offset of removed route
     * @return true if it was the last route
     */
    protected final boolean removeRoute(final RouteKey key, final int offset) {
        final long pathId = this.offsets.getValue(this.pathsId, offset);
        this.values = this.offsets.removeValue(this.values, offset);
        this.pathsId = this.offsets.removeValue(this.pathsId, offset);
        this.offsets = this.offsets.without(key);
        if (this.removedPaths == null) {
            this.removedPaths = new ArrayList<>();
        }
        this.removedPaths.add(new RemovedPath(key, pathId));
        return isEmpty();
    }

    @Override
    public void updateBestPaths(
            final RouteEntryDependenciesContainer entryDependencies,
            final Object routeKey,
            final WriteTransaction tx) {

        final RIBSupport ribSupport = entryDependencies.getRibSupport();
        final KeyedInstanceIdentifier<Tables, TablesKey> locRibTarget = entryDependencies.getLocRibTableTarget();
        if (this.bestPathRemoved != null) {
            this.bestPathRemoved.forEach(path -> {
                final Identifier newRouteKey = ribSupport.createNewRouteKey(path.getPathId(), routeKey);
                final InstanceIdentifier routeTarget = ribSupport.createRouteIdentifier(locRibTarget, newRouteKey);
                LOG.debug("Delete route from LocRib {}", routeTarget);
                tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
            });
            this.bestPathRemoved = null;
        }
        if (this.removedPaths != null) {
            this.removedPaths.forEach(removedPath -> {
                final Identifier routeKeyAddPath
                        = ribSupport.createNewRouteKey(removedPath.getPathId(), routeKey);
                final Identifier routeKeyNonAddPath = ribSupport.createNewRouteKey(NON_PATH_ID_VALUE, routeKey);
                fillAdjRibsOut(true, null, null, null,
                        routeKeyNonAddPath, routeKeyAddPath,
                        RouterIds.createPeerId(removedPath.getRouteId()),
                        entryDependencies.getLocalTablesKey(), entryDependencies, tx);
            });
            this.removedPaths = null;
        }

        if (this.newBestPathToBeAdvertised != null) {
            this.newBestPathToBeAdvertised.forEach(path -> addPathToDataStore(entryDependencies, path,
                    isFirstBestPath(this.bestPath.indexOf(path)), routeKey, tx));
            this.newBestPathToBeAdvertised = null;
        }
    }

    @Override
    public void initializeBestPaths(final RouteEntryDependenciesContainer entryDependencies,
            final RouteEntryInfo entryInfo, final WriteTransaction tx) {
        if (this.bestPath != null) {
            final Peer toPeer = entryInfo.getToPeer();
            final TablesKey localTk = entryDependencies.getLocalTablesKey();
            final boolean destPeerSupAddPath = toPeer.supportsAddPathSupported(localTk);
            for (final AddPathBestPath path : this.bestPath) {
                if (!filterRoutes(path.getPeerId(), toPeer, localTk)) {
                    continue;
                }
                writeRoutePath(entryInfo, destPeerSupAddPath, path, localTk, entryDependencies, tx);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeRoutePath(final RouteEntryInfo entryInfo,
            final boolean destPeerSupAddPath, final AddPathBestPath path,
            final TablesKey localTK, final RouteEntryDependenciesContainer routeEntryDep, final WriteTransaction tx) {
        final Object routeKey = entryInfo.getRouteKey();
        final RIBSupport ribSupport = routeEntryDep.getRibSupport();
        final BGPRouteEntryExportParameters baseExp = new BGPRouteEntryExportParametersImpl(
                this.peerTracker.getPeer(path.getPeerId()), entryInfo.getToPeer());
        final Optional<Attributes> effAttrib = routeEntryDep.getRoutingPolicies()
                .applyExportPolicies(baseExp, path.getAttributes());

        Identifier newRouteKey = ribSupport.createNewRouteKey(destPeerSupAddPath
                ? path.getPathId() : NON_PATH_ID_VALUE, routeKey);
        final Peer toPeer = entryInfo.getToPeer();
        final Route route = createRoute(ribSupport, newRouteKey, destPeerSupAddPath
                ? path.getPathId() : NON_PATH_ID_VALUE, path);
        InstanceIdentifier ribOutIId = ribSupport.createRouteIdentifier(toPeer.getRibOutIId(localTK), newRouteKey);
        if (effAttrib.isPresent() && route != null) {
            LOG.debug("Write route {} to peer AdjRibsOut {}", route, toPeer.getPeerId());
            tx.put(LogicalDatastoreType.OPERATIONAL, ribOutIId, route);
            tx.put(LogicalDatastoreType.OPERATIONAL, ribOutIId.child(Attributes.class), effAttrib.get());
        }
    }

    private void addPathToDataStore(
            final RouteEntryDependenciesContainer entryDep,
            final AddPathBestPath path,
            final boolean isFirstBestPath,
            final Object routeKey,
            final WriteTransaction tx) {
        final RIBSupport ribSup = entryDep.getRibSupport();
        final Identifier routeKeyAddPath = ribSup.createNewRouteKey(path.getPathId(), routeKey);
        final Identifier routeKeyAddNonPath = ribSup.createNewRouteKey(NON_PATH_ID_VALUE, routeKey);
        final Route routeAddPath = createRoute(ribSup, routeKeyAddPath, path.getPathId(), path);
        final Route routeNonAddPath = createRoute(ribSup, routeKeyAddNonPath, NON_PATH_ID_VALUE, path);

        final KeyedInstanceIdentifier<Tables, TablesKey> locRibTarget = entryDep.getLocRibTableTarget();
        final InstanceIdentifier routeTarget = ribSup.createRouteIdentifier(locRibTarget, routeKeyAddPath);
        LOG.debug("Write route to LocRib {}", routeAddPath);
        tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget, routeAddPath);

        fillAdjRibsOut(isFirstBestPath, path.getAttributes(), routeNonAddPath, routeAddPath, routeKeyAddNonPath,
            routeKeyAddPath, path.getPeerId(), entryDep.getLocalTablesKey(), entryDep, tx);
    }

    @SuppressWarnings("unchecked")
    private void fillAdjRibsOut(
            final boolean isFirstBestPath,
            final Attributes attributes,
            final Route routeNonAddPath,
            final Route routeAddPath,
            final Identifier routeKeyAddNonPath,
            final Identifier routeKeyAddPath,
            final PeerId fromPeerId,
            final TablesKey localTK,
            final RouteEntryDependenciesContainer routeEntryDep,
            final WriteTransaction tx) {
        /*
         * We need to keep track of routers and populate adj-ribs-out, too. If we do not, we need to
         * expose from which client a particular route was learned from in the local RIB, and have
         * the listener perform filtering.
         *
         * We walk the policy set in order to minimize the amount of work we do for multiple peers:
         * if we have two eBGP peers, for example, there is no reason why we should perform the translation
         * multiple times.
         */
        final RIBSupport ribSupport = routeEntryDep.getRibSupport();
        for (final Peer toPeer : this.peerTracker.getPeers()) {
            if (!filterRoutes(fromPeerId, toPeer, localTK)) {
                continue;
            }
            final boolean destPeerSupAddPath = toPeer.supportsAddPathSupported(localTK);

            if (toPeer.getPeerId().getValue().equals("bgp://127.0.0.5")) {
                LOG.debug("Write route {} to peer AdjRibsOut {}", toPeer.getPeerId());
            }
            if (peersSupportsAddPathOrIsFirstBestPath(destPeerSupAddPath, isFirstBestPath)) {

                Optional<Attributes> effAttrib = Optional.empty();
                final Peer fromPeer = this.peerTracker.getPeer(fromPeerId);

                if (fromPeer != null && attributes != null) {
                    final BGPRouteEntryExportParameters baseExp
                            = new BGPRouteEntryExportParametersImpl(fromPeer, toPeer);
                    effAttrib = routeEntryDep.getRoutingPolicies()
                            .applyExportPolicies(baseExp, attributes);
                }
                Route newRoute = null;
                InstanceIdentifier ribOutRoute = null;
                if (destPeerSupAddPath) {
                    newRoute = routeAddPath;
                    ribOutRoute
                            = ribSupport.createRouteIdentifier(toPeer.getRibOutIId(localTK), routeKeyAddPath);
                } else if (!this.oldNonAddPathBestPathTheSame) {
                    ribOutRoute
                            = ribSupport.createRouteIdentifier(toPeer.getRibOutIId(localTK), routeKeyAddNonPath);
                    newRoute = routeNonAddPath;
                }

                if (effAttrib.isPresent() && newRoute != null) {
                    LOG.debug("Write route {} to peer AdjRibsOut {}", newRoute, toPeer.getPeerId());
                    tx.put(LogicalDatastoreType.OPERATIONAL, ribOutRoute, newRoute);
                    tx.put(LogicalDatastoreType.OPERATIONAL, ribOutRoute.child(Attributes.class), effAttrib.get());
                } else if (ribOutRoute != null) {
                    LOG.trace("Removing {} from transaction for peer {}", ribOutRoute, toPeer.getPeerId());
                    tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutRoute);
                }
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
        final Attributes attributes = this.offsets.getValue(this.values, offset);
        final long pathId = this.offsets.getValue(this.pathsId, offset);
        LOG.trace("Processing router key {} attributes {}", key, attributes);
        selector.processPath(attributes, key, offset, pathId);
    }

    /**
     * Process best path selection.
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
        if (this.bestPathRemoved != null && !this.bestPathRemoved.isEmpty()
                || newBestPathList != null
                && !newBestPathList.equals(this.bestPath)) {
            this.newBestPathToBeAdvertised = new ArrayList<>(newBestPathList);
            if (this.bestPath != null) {
                this.newBestPathToBeAdvertised.removeAll(this.bestPath);
            }
            this.bestPath = newBestPathList;
            LOG.trace("Actual Best {}, removed best {}", this.bestPath, this.bestPathRemoved);
            return true;
        }
        return false;
    }

    private boolean isNonAddPathBestPathTheSame(final List<AddPathBestPath> newBestPathList) {
        return !(isEmptyOrNull(this.bestPath) || isEmptyOrNull(newBestPathList))
                && this.bestPath.get(0).equals(newBestPathList.get(0));
    }

    private static boolean isEmptyOrNull(final List<AddPathBestPath> pathList) {
        return pathList == null || pathList.isEmpty();
    }

    private void filterRemovedPaths(final List<AddPathBestPath> newBestPathList) {
        if (this.bestPath == null) {
            return;
        }
        this.bestPathRemoved = new ArrayList<>(this.bestPath);
        this.bestPath.forEach(oldBest -> {
            final Optional<AddPathBestPath> present = newBestPathList.stream()
                    .filter(newBest -> newBest.getPathId() == oldBest.getPathId()
                            && newBest.getRouteKey() == oldBest.getRouteKey()).findAny();
            present.ifPresent(addPathBestPath -> this.bestPathRemoved.remove(oldBest));
        });
    }
}
