/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.mode.impl.BestPathStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single route entry inside a route table. Maintains the attributes from all contributing peers. The information is
 * stored in arrays with a shared map of offsets for peers to allow lookups. This is needed to maintain low memory
 * overhead in face of large number of routes and peers, where individual object overhead becomes the dominating factor.
 *
 * <p>This class is NOT thread-safe.
 */
public abstract class AddPathAbstractRouteEntry implements RouteEntry {
    private static final class Stale extends StaleBestPathRoute {
        private final List<NodeIdentifierWithPredicates> addPathRouteKeyIdentifier;
        private final List<NodeIdentifierWithPredicates> staleRouteKeyIdentifier;
        private final boolean isNonAddPathBestPathNew;

        Stale(final RIBSupport<?, ?> ribSupport, final String routeKey, final List<Uint32> staleRoutesPathIds,
                final List<Uint32> withdrawalRoutePathIds, final boolean isNonAddPathBestPathNew) {
            super(ribSupport.createRouteListArgument(routeKey));
            this.isNonAddPathBestPathNew = isNonAddPathBestPathNew;

            staleRouteKeyIdentifier = staleRoutesPathIds.stream()
                .map(pathId -> ribSupport.createRouteListArgument(pathId, routeKey))
                .collect(Collectors.toUnmodifiableList());
            if (withdrawalRoutePathIds != null) {
                addPathRouteKeyIdentifier = withdrawalRoutePathIds.stream()
                    .map(pathId -> ribSupport.createRouteListArgument(pathId, routeKey))
                    .collect(Collectors.toUnmodifiableList());
            } else {
                addPathRouteKeyIdentifier = List.of();
            }
        }

        @Override
        public List<NodeIdentifierWithPredicates> getStaleRouteKeyIdentifiers() {
            return staleRouteKeyIdentifier;
        }

        @Override
        public List<NodeIdentifierWithPredicates> getAddPathRouteKeyIdentifiers() {
            return addPathRouteKeyIdentifier;
        }

        @Override
        public boolean isNonAddPathBestPathNew() {
            return isNonAddPathBestPathNew;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AddPathAbstractRouteEntry.class);
    private static final Uint32[] EMPTY_PATHS_ID = new Uint32[0];
    private static final MapEntryNode[] EMPTY_VALUES = new MapEntryNode[0];

    private RouteKeyOffsets offsets = RouteKeyOffsets.EMPTY;
    private MapEntryNode[] values = EMPTY_VALUES;
    private Uint32[] pathsId = EMPTY_PATHS_ID;
    private List<AddPathBestPath> bestPath;
    private List<AddPathBestPath> bestPathRemoved;
    private List<AddPathBestPath> newBestPathToBeAdvertised;
    private List<Uint32> removedPathsId;

    private long pathIdCounter = 0L;
    private boolean isNonAddPathBestPathNew;

    private MapEntryNode createRoute(final RIBSupport<?, ?> ribSup, final String routeKey, final AddPathBestPath path) {
        final var map = offsets;
        final var route = map.getValue(values, map.offsetOf(path.getRouteKey()));
        return ribSup.createRoute(route, ribSup.createRouteListArgument(path.getPathIdLong(), routeKey),
            path.getAttributes());
    }

    @Override
    public final int addRoute(final RouterId routerId, final Uint32 remotePathId, final MapEntryNode route) {
        final var key = new RouteKey(routerId, remotePathId);
        int offset = offsets.offsetOf(key);
        if (offset < 0) {
            final var newOffsets = offsets.with(key);
            offset = newOffsets.offsetOf(key);
            final var newRoute = newOffsets.expand(offsets, values, offset);
            final var newPathsId = newOffsets.expand(offsets, pathsId, offset);
            values = newRoute;
            offsets = newOffsets;
            pathsId = newPathsId;
            offsets.setValue(pathsId, offset, Uint32.valueOf(++pathIdCounter));
        }
        offsets.setValue(values, offset, route);
        LOG.trace("Added route {} from {}", route, routerId);
        return offset;
    }

    @Override
    public final boolean removeRoute(final RouterId routerId, final Uint32 remotePathId) {
        final var key = new RouteKey(routerId, remotePathId);
        final int offset = offsets.offsetOf(key);
        final var pathId = offsets.getValue(pathsId, offset);
        values = offsets.removeValue(values, offset, EMPTY_VALUES);
        pathsId = offsets.removeValue(pathsId, offset, EMPTY_PATHS_ID);
        offsets = offsets.without(key);
        if (removedPathsId == null) {
            removedPathsId = new ArrayList<>();
        }
        removedPathsId.add(pathId);
        return offsets.isEmpty();
    }

    @Override
    public final StaleBestPathRoute removeStalePaths(final RIBSupport<?, ?> ribSupport, final String routeKey) {
        final List<Uint32> stalePaths;
        if (bestPathRemoved != null && !bestPathRemoved.isEmpty()) {
            stalePaths = bestPathRemoved.stream().map(AddPathBestPath::getPathIdLong)
                .collect(Collectors.toUnmodifiableList());
            bestPathRemoved = null;
        } else {
            stalePaths = List.of();
        }

        List<Uint32> removedPaths;
        if (removedPathsId != null) {
            removedPaths = removedPathsId;
            removedPathsId = null;
        } else {
            removedPaths = List.of();
        }

        return stalePaths.isEmpty() && removedPaths.isEmpty() ? null
            : new Stale(ribSupport, routeKey, stalePaths, removedPaths, isNonAddPathBestPathNew);
    }

    @Override
    public final List<AdvertizedRoute> newBestPaths(final RIBSupport<?, ?> ribSupport, final String routeKey) {
        if (newBestPathToBeAdvertised == null || newBestPathToBeAdvertised.isEmpty()) {
            return List.of();
        }
        final var advertized = new ArrayList<AdvertizedRoute>(newBestPathToBeAdvertised.size());
        final var firstBestPath = bestPath.isEmpty() ? null : bestPath.getFirst();
        for (var path : newBestPathToBeAdvertised) {
            final var routeAddPath = createRoute(ribSupport, routeKey, path);
            // FIXME: can we use identity check here?
            final boolean isFirstBestPath = firstBestPath != null && firstBestPath.equals(path);
            final var adv = new AdvertizedRoute(ribSupport, isFirstBestPath, routeAddPath, path.getAttributes(),
                path.getPeerId(), path.isDepreferenced());
            advertized.add(adv);
        }
        newBestPathToBeAdvertised = null;
        return advertized;
    }

    @Override
    public final List<ActualBestPathRoutes> actualBestPaths(final RIBSupport<?, ?> ribSupport,
            final RouteEntryInfo entryInfo) {
        if (bestPath == null || bestPath.isEmpty()) {
            return List.of();
        }
        final var preexistentRoutes = new ArrayList<ActualBestPathRoutes>();
        for (var path : bestPath) {
            final var route = createRoute(ribSupport, entryInfo.getRouteKey(), path);
            final var adv = new ActualBestPathRoutes(ribSupport, route, path.getPeerId(), path.getAttributes(),
                path.isDepreferenced());
            preexistentRoutes.add(adv);
        }
        return preexistentRoutes;
    }

    @Override
    public final boolean selectBest(final RIBSupport<?, ?> ribSupport, final long localAs) {
        final int size;
        return isBestPathNew((size = offsets.size()) == 0 ? ImmutableList.of() : selectBest(ribSupport, localAs, size));
    }

    protected abstract ImmutableList<AddPathBestPath> selectBest(RIBSupport<?, ?> ribSupport, long localAs, int size);

    /**
     * Process a specific route offset into specified selector.
     *
     * @param selector selector to update
     * @param offset offset to process
     */
    protected final void processOffset(final RIBSupport<?, ?> ribSupport, final AddPathSelector selector,
            final int offset) {
        final var key = offsets.getKey(offset);
        final var route = offsets.getValue(values, offset);
        final var pathId = offsets.getValue(pathsId, offset);
        LOG.trace("Processing router key {} route {}", key, route);
        selector.processPath(ribSupport.extractAttributes(route), key, offset, pathId);
    }

    protected final AddPathBestPath bestPathAt(final RIBSupport<?, ?> ribSupport, final int offset) {
        final var route = verifyNotNull(offsets.getValue(values, offset));
        return new AddPathBestPath(new BestPathStateImpl(ribSupport.extractAttributes(route)), offsets.getKey(offset),
            offsets.getValue(pathsId, offset), offset);
    }

    private boolean isBestPathNew(final ImmutableList<AddPathBestPath> newBestPathList) {
        isNonAddPathBestPathNew = !isNonAddPathBestPathTheSame(newBestPathList);
        filterRemovedPaths(newBestPathList);
        if (bestPathRemoved != null && !bestPathRemoved.isEmpty()
                || newBestPathList != null
                && !newBestPathList.equals(bestPath)) {
            if (bestPath != null) {
                newBestPathToBeAdvertised = new ArrayList<>(newBestPathList);
                newBestPathToBeAdvertised.removeAll(bestPath);
            } else {
                newBestPathToBeAdvertised = newBestPathList;
            }
            bestPath = newBestPathList;
            LOG.trace("Actual Best {}, removed best {}", bestPath, bestPathRemoved);
            return true;
        }
        return false;
    }

    private boolean isNonAddPathBestPathTheSame(final List<AddPathBestPath> newBestPathList) {
        return !isEmptyOrNull(bestPath) && !isEmptyOrNull(newBestPathList)
                && bestPath.get(0).equals(newBestPathList.get(0));
    }

    private static boolean isEmptyOrNull(final List<AddPathBestPath> pathList) {
        return pathList == null || pathList.isEmpty();
    }

    private void filterRemovedPaths(final List<AddPathBestPath> newBestPathList) {
        if (bestPath == null) {
            return;
        }
        bestPathRemoved = new ArrayList<>(bestPath);
        bestPath.forEach(oldBest -> {
            newBestPathList.stream()
                .filter(newBest -> newBest.getPathId() == oldBest.getPathId()
                        && newBest.getRouteKey().equals(oldBest.getRouteKey()))
                .findAny()
                .ifPresent(addPathBestPath -> bestPathRemoved.remove(oldBest));
        });
    }
}
