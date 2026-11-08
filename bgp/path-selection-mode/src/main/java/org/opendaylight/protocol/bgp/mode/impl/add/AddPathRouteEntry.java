/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.mode.impl.base.BaseBestPathState;
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
abstract class AddPathRouteEntry implements RouteEntry {
    private static final class Stale extends StaleBestPathRoute {
        @NonNullByDefault
        private final List<NodeIdentifierWithPredicates> addPathRouteKeyIdentifier;
        @NonNullByDefault
        private final List<NodeIdentifierWithPredicates> staleRouteKeyIdentifier;
        private final boolean isNonAddPathBestPathNew;

        @NonNullByDefault
        Stale(final RIBSupport ribSupport, final String routeKey, final List<Uint32> staleRoutesPathIds,
                final List<Uint32> withdrawalRoutePathIds, final boolean isNonAddPathBestPathNew) {
            super(ribSupport.createRouteListArgument(routeKey));
            this.isNonAddPathBestPathNew = isNonAddPathBestPathNew;
            staleRouteKeyIdentifier = createIdentifier(ribSupport, routeKey, staleRoutesPathIds);
            addPathRouteKeyIdentifier = createIdentifier(ribSupport, routeKey, withdrawalRoutePathIds);
        }

        @NonNullByDefault
        private static List<NodeIdentifierWithPredicates> createIdentifier(final RIBSupport ribSupport,
                final String routeKey, final List<Uint32> pathIds) {
            final var size = pathIds.size();
            return switch (size) {
                case 0 -> List.of();
                case 1 -> List.of(ribSupport.createRouteListArgument(pathIds.getFirst(), routeKey));
                case 2 -> List.of(
                    ribSupport.createRouteListArgument(pathIds.getFirst(), routeKey),
                    ribSupport.createRouteListArgument(pathIds.getLast(), routeKey));
                default -> {
                    final var tmp = new ArrayList<NodeIdentifierWithPredicates>(size);
                    for (var pathId : pathIds) {
                        tmp.add(ribSupport.createRouteListArgument(pathId, routeKey));
                    }
                    yield List.copyOf(tmp);
                }
            };
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

    private static final Logger LOG = LoggerFactory.getLogger(AddPathRouteEntry.class);
    private static final Uint32[] EMPTY_PATH_IDS = new Uint32[0];
    private static final MapEntryNode[] EMPTY_VALUES = new MapEntryNode[0];

    private RouteKeyOffsets offsets = RouteKeyOffsets.EMPTY;
    private MapEntryNode[] values = EMPTY_VALUES;
    private Uint32[] pathIds = EMPTY_PATH_IDS;
    @NonNullByDefault
    private List<AddPathBestPath> bestPath = List.of();
    @NonNullByDefault
    private List<Uint32> removedPathIds = List.of();
    private int lastPathId = 0;
    private boolean fastPathId = true;

    @NonNullByDefault
    private Uint32 nextPathId() {
        return fastPathId ? nextPathIdFast() : nextPathIdSlow();
    }

    @NonNullByDefault
    private Uint32 nextPathIdFast() {
        final var nextPathId = ++lastPathId;
        return nextPathId != 0 ? Uint32.fromIntBits(nextPathId) : nextPathIdFastOverflow();
    }

    @NonNullByDefault
    private Uint32 nextPathIdFastOverflow() {
        fastPathId = false;
        return nextPathIdSlow();
    }

    @NonNullByDefault
    private static Uint32 nextPathIdSlow() {
        // FIXME: overflow handling: we can no longer be certain that next allocated path id is unused
        throw new IllegalStateException("path id overflown");
    }

    @Override
    public final boolean putRoute(final RouterId routerId, final Uint32 remotePathId, final MapEntryNode route) {
        final var routeKey = new RouteKey(routerId, remotePathId);
        final int offset = offsets.offsetOf(routeKey);
        if (offset < 0) {
            addRoute(routerId, route, routeKey, nextPathId());
            return true;
        }
        setRoute(routerId, route, offset);
        return false;
    }

    @NonNullByDefault
    private void addRoute(final RouterId routerId, final MapEntryNode route, final RouteKey routeKey,
            final Uint32 pathId) {
        final var newOffsets = offsets.with(routeKey);
        final var offset = newOffsets.offsetOf(routeKey);
        final var newRoute = newOffsets.expand(offsets, values, offset);
        final var newPathsId = newOffsets.expand(offsets, pathIds, offset);
        values = newRoute;
        offsets = newOffsets;
        pathIds = newPathsId;
        offsets.setValue(pathIds, offset, pathId);
        setRoute(routerId, route, offset);
    }

    private void setRoute(final RouterId routerId, final MapEntryNode route, final int offset) {
        offsets.setValue(values, offset, route);
        LOG.trace("Added route {} from {}", route, routerId);
    }

    @Override
    public final boolean removeRoute(final RouterId routerId, final Uint32 remotePathId) {
        final var key = new RouteKey(routerId, remotePathId);
        final int offset = offsets.offsetOf(key);
        final var pathId = offsets.getValue(pathIds, offset);
        values = offsets.removeValue(values, offset, EMPTY_VALUES);
        pathIds = offsets.removeValue(pathIds, offset, EMPTY_PATH_IDS);
        offsets = offsets.without(key);
        if (removedPathIds.isEmpty()) {
            removedPathIds = new ArrayList<>();
        }
        removedPathIds.add(pathId);
        return offsets.isEmpty();
    }

    @Override
    public final List<ActualBestPathRoutes> actualBestPaths(final RIBSupport ribSupport,
            final RouteEntryInfo entryInfo) {
        final var paths = bestPath;
        final var size = paths.size();
        return switch (size) {
            case 0 -> List.of();
            case 1 -> List.of(actualBestPath(ribSupport, entryInfo, paths.getFirst()));
            case 2 -> List.of(
                actualBestPath(ribSupport, entryInfo, paths.getFirst()),
                actualBestPath(ribSupport, entryInfo, paths.getLast()));
            default -> {
                final var ret = new ArrayList<ActualBestPathRoutes>(size);
                for (var path : paths) {
                    ret.add(actualBestPath(ribSupport, entryInfo, path));
                }
                yield ret;
            }
        };
    }

    @NonNullByDefault
    private ActualBestPathRoutes actualBestPath(final RIBSupport ribSupport, final RouteEntryInfo entryInfo,
            final AddPathBestPath path) {
        return new ActualBestPathRoutes(ribSupport, createRoute(ribSupport, entryInfo.getRouteKey(), path),
            path.getPeerId(), path.getAttributes(), path.isDepreferenced());
    }

    @Override
    public final @Nullable Update selectBest(final RIBSupport ribSupport, final long localAs, final String routeKey) {
        final var size = offsets.size();
        return size == 0 ? select0(ribSupport, routeKey)
            : selectBest(ribSupport, routeKey, selectBest(ribSupport, localAs, size));
    }

    @NonNullByDefault
    abstract List<AddPathBestPath> selectBest(RIBSupport ribSupport, long localAs, int size);

    @NonNullByDefault
    private @Nullable Update selectBest(final RIBSupport ribSupport, final String routeKey,
            final List<AddPathBestPath> newBestPath) {
        return newBestPath.isEmpty() ? select0(ribSupport, routeKey)
            : selectN(ribSupport, routeKey, List.copyOf(newBestPath));
    }

    private @Nullable Update select0(final @NonNull RIBSupport ribSupport, final @NonNull String routeKey) {
        final var oldBestPath = bestPath;
        final var staleRoute = staleRoute(ribSupport, routeKey, oldBestPath, true);
        if (staleRoute == null) {
            return null;
        }

        setBestPath(List.of(), oldBestPath);
        return new Update(List.of(), staleRoute);
    }

    @NonNullByDefault
    private @Nullable Update selectN(final RIBSupport ribSupport, final String routeKey,
            final List<AddPathBestPath> newBestPath) {
        final var oldBestPath = bestPath;
        if (!oldBestPath.isEmpty()) {
            return selectN(ribSupport, routeKey, newBestPath, oldBestPath);
        }

        setBestPath(newBestPath, List.of());
        return new Update(
            advertizedRoutes(ribSupport, routeKey, newBestPath),
            staleRoute(ribSupport, routeKey, List.of(), true));
    }

    private @Nullable Update selectN(final @NonNull RIBSupport ribSupport, final @NonNull String routeKey,
            final @NonNull List<AddPathBestPath> newBestPath, final @NonNull List<AddPathBestPath> oldBestPath) {
        // FIXME: clean this up: we are computing a delta between two lists and there just has to be a more efficient
        //        way to do this
        final var advertizedPaths = new ArrayList<>(newBestPath);
        advertizedPaths.removeAll(oldBestPath);
        final var removedPaths = new ArrayList<>(oldBestPath);
        oldBestPath.forEach(oldBest -> newBestPath.stream()
            .filter(newBest -> newBest.getPathId() == oldBest.getPathId()
                    && newBest.getRouteKey().equals(oldBest.getRouteKey()))
            .findAny()
            .ifPresent(addPathBestPath -> removedPaths.remove(oldBest)));
        final var staleRoute = staleRoute(ribSupport, routeKey, removedPaths,
            !newBestPath.getFirst().equals(oldBestPath.getFirst()));
        if (advertizedPaths.isEmpty() && staleRoute == null) {
            return null;
        }

        setBestPath(newBestPath, removedPaths);
        return new Update(advertizedRoutes(ribSupport, routeKey, advertizedPaths), staleRoute);
    }

    @NonNullByDefault
    private List<AdvertizedRoute> advertizedRoutes(final RIBSupport ribSupport, final String routeKey,
            final List<AddPathBestPath> paths) {
        final var size = paths.size();
        if (size == 0) {
            return List.of();
        }

        final var isFirst = isFirst();
        final var ret = new ArrayList<AdvertizedRoute>(size);
        for (var path : paths) {
            ret.add(new AdvertizedRoute(ribSupport, isFirst.test(path), createRoute(ribSupport, routeKey, path),
                path.getAttributes(), path.getPeerId(), path.isDepreferenced()));
        }
        return ret;
    }

    @NonNullByDefault
    private Predicate<AddPathBestPath> isFirst() {
        final var paths = bestPath;
        if (paths.isEmpty()) {
            return path -> false;
        }
        final var first = paths.getFirst();
        return path -> first == path || first.equals(path);
    }

    // Note: picks up any remotedPathsId
    private @Nullable Stale staleRoute(final @NonNull RIBSupport ribSupport, final String routeKey,
            final @NonNull List<AddPathBestPath> removedPaths, final boolean isNonAddPathBestPathNew) {
        final var removedIds = removedPathIds;
        removedPathIds = List.of();

        return removedIds.isEmpty() && removedPaths.isEmpty() ? null
            : new Stale(ribSupport, routeKey, removedPaths.stream().map(AddPathBestPath::getPathIdLong).toList(),
                removedIds, isNonAddPathBestPathNew);
    }

    @NonNullByDefault
    private void setBestPath(final List<AddPathBestPath> newBestPath, final List<AddPathBestPath> bestPathRemoved) {
        bestPath = requireNonNull(newBestPath);
        LOG.trace("Actual Best {}, removed best {}", newBestPath, bestPathRemoved);
    }

    /**
     * Process a specific route offset into specified selector.
     *
     * @param selector selector to update
     * @param offset offset to process
     */
    @NonNullByDefault
    final void processOffset(final RIBSupport ribSupport, final AddPathSelector selector,
            final int offset) {
        final var key = offsets.getKey(offset);
        final var route = offsets.getValue(values, offset);
        final var pathId = offsets.getValue(pathIds, offset);
        LOG.trace("Processing router key {} route {}", key, route);
        selector.processPath(ribSupport.extractAttributes(route), key, offset, pathId);
    }

    @NonNullByDefault
    final AddPathBestPath bestPathAt(final RIBSupport ribSupport, final int offset) {
        final var route = verifyNotNull(offsets.getValue(values, offset));
        return new AddPathBestPath(new BaseBestPathState(ribSupport.extractAttributes(route)), offsets.getKey(offset),
            offsets.getValue(pathIds, offset), offset);
    }

    @NonNullByDefault
    private MapEntryNode createRoute(final RIBSupport ribSupport, final String routeKey, final AddPathBestPath path) {
        final var map = offsets;
        final var route = map.getValue(values, map.offsetOf(path.getRouteKey()));
        return ribSupport.createRoute(route, ribSupport.createRouteListArgument(path.getPathIdLong(), routeKey),
            path.getAttributes());
    }
}
