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
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.mode.impl.BestPathStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
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
 * <p>
 * This class is NOT thread-safe.
 */
public abstract class AddPathAbstractRouteEntry<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>, R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
        implements RouteEntry<C, S, R, I> {
    private static final class Stale<C extends Routes & DataObject & ChoiceIn<Tables>,
            S extends ChildOf<? super C>, R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> extends StaleBestPathRoute<C, S, R, I> {
        private final List<NodeIdentifierWithPredicates> addPathRouteKeyIdentifier;
        private final List<NodeIdentifierWithPredicates> staleRouteKeyIdentifier;
        private final boolean isNonAddPathBestPathNew;

        Stale(final RIBSupport<C, S, R, I> ribSupport, final String routeKey, final List<Uint32> staleRoutesPathIds,
                final List<Uint32> withdrawalRoutePathIds, final boolean isNonAddPathBestPathNew) {
            super(ribSupport.createRouteListArgument(routeKey));
            this.isNonAddPathBestPathNew = isNonAddPathBestPathNew;

            this.staleRouteKeyIdentifier = staleRoutesPathIds.stream()
                .map(pathId -> ribSupport.createRouteListArgument(pathId, routeKey))
                .collect(Collectors.toUnmodifiableList());
            if (withdrawalRoutePathIds != null) {
                this.addPathRouteKeyIdentifier = withdrawalRoutePathIds.stream()
                    .map(pathId -> ribSupport.createRouteListArgument(pathId, routeKey))
                    .collect(Collectors.toUnmodifiableList());
            } else {
                this.addPathRouteKeyIdentifier = List.of();
            }
        }

        @Override
        public List<NodeIdentifierWithPredicates> getStaleRouteKeyIdentifiers() {
            return this.staleRouteKeyIdentifier;
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

    private MapEntryNode createRoute(final RIBSupport<C, S, R, I> ribSup, final String routeKey,
            final AddPathBestPath path) {
        final RouteKeyOffsets map = this.offsets;
        final MapEntryNode route = map.getValue(this.values, map.offsetOf(path.getRouteKey()));
        return ribSup.createRoute(route, ribSup.createRouteListArgument(path.getPathIdLong(), routeKey),
            path.getAttributes());
    }

    @Override
    public final int addRoute(final RouterId routerId, final Uint32 remotePathId, final MapEntryNode route) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        int offset = this.offsets.offsetOf(key);
        if (offset < 0) {
            final RouteKeyOffsets newOffsets = this.offsets.with(key);
            offset = newOffsets.offsetOf(key);
            final MapEntryNode[] newRoute = newOffsets.expand(this.offsets, this.values, offset);
            final Uint32[] newPathsId = newOffsets.expand(this.offsets, this.pathsId, offset);
            this.values = newRoute;
            this.offsets = newOffsets;
            this.pathsId = newPathsId;
            this.offsets.setValue(this.pathsId, offset, Uint32.valueOf(++this.pathIdCounter));
        }
        this.offsets.setValue(this.values, offset, route);
        LOG.trace("Added route {} from {}", route, routerId);
        return offset;
    }

    @Override
    public final boolean removeRoute(final RouterId routerId, final Uint32 remotePathId) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        final int offset = this.offsets.offsetOf(key);
        final Uint32 pathId = this.offsets.getValue(this.pathsId, offset);
        this.values = this.offsets.removeValue(this.values, offset, EMPTY_VALUES);
        this.pathsId = this.offsets.removeValue(this.pathsId, offset, EMPTY_PATHS_ID);
        this.offsets = this.offsets.without(key);
        if (this.removedPathsId == null) {
            this.removedPathsId = new ArrayList<>();
        }
        this.removedPathsId.add(pathId);
        return this.offsets.isEmpty();
    }

    @Override
    public final Optional<StaleBestPathRoute<C, S, R, I>> removeStalePaths(final RIBSupport<C, S, R, I> ribSupport,
            final String routeKey) {
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

        return stalePaths.isEmpty() && removedPaths.isEmpty() ? Optional.empty()
                : Optional.of(new Stale<>(ribSupport, routeKey, stalePaths, removedPaths, isNonAddPathBestPathNew));
    }

    @Override
    public final List<AdvertizedRoute<C, S, R, I>> newBestPaths(final RIBSupport<C, S, R, I> ribSupport,
            final String routeKey) {
        if (this.newBestPathToBeAdvertised == null || this.newBestPathToBeAdvertised.isEmpty()) {
            return List.of();
        }
        final List<AdvertizedRoute<C, S, R, I>> advertized = new ArrayList<>(newBestPathToBeAdvertised.size());
        final AddPathBestPath firstBestPath = this.bestPath.isEmpty() ? null : this.bestPath.get(0);
        for (final AddPathBestPath path : this.newBestPathToBeAdvertised) {
            final MapEntryNode routeAddPath = createRoute(ribSupport, routeKey, path);
            // FIXME: can we use identity check here?
            final boolean isFirstBestPath = firstBestPath != null && firstBestPath.equals(path);
            final AdvertizedRoute<C, S, R, I> adv = new AdvertizedRoute<>(ribSupport, isFirstBestPath,
                    routeAddPath, path.getAttributes(), path.getPeerId(), path.isDepreferenced());
            advertized.add(adv);
        }
        this.newBestPathToBeAdvertised = null;
        return advertized;
    }

    @Override
    public final List<ActualBestPathRoutes<C, S, R, I>> actualBestPaths(final RIBSupport<C, S, R, I> ribSupport,
            final RouteEntryInfo entryInfo) {
        if (this.bestPath == null || this.bestPath.isEmpty()) {
            return List.of();
        }
        final List<ActualBestPathRoutes<C, S, R, I>> preexistentRoutes = new ArrayList<>();
        for (final AddPathBestPath path : this.bestPath) {
            final MapEntryNode route = createRoute(ribSupport, entryInfo.getRouteKey(), path);
            final ActualBestPathRoutes<C, S, R, I> adv = new ActualBestPathRoutes<>(ribSupport, route, path.getPeerId(),
                    path.getAttributes(), path.isDepreferenced());
            preexistentRoutes.add(adv);
        }
        return preexistentRoutes;
    }

    @Override
    public final boolean selectBest(final RIBSupport<C, S, R, I> ribSupport, final long localAs) {
        final int size;
        return isBestPathNew((size = offsets.size()) == 0 ? ImmutableList.of() : selectBest(ribSupport, localAs, size));
    }

    protected abstract ImmutableList<AddPathBestPath> selectBest(RIBSupport<C, S, R, I> ribSupport, long localAs,
        int size);

    /**
     * Process a specific route offset into specified selector.
     *
     * @param selector selector to update
     * @param offset offset to process
     */
    protected final void processOffset(final RIBSupport<C, S, R, I> ribSupport, final AddPathSelector selector,
            final int offset) {
        final RouteKey key = offsets.getKey(offset);
        final MapEntryNode route = offsets.getValue(values, offset);
        final Uint32 pathId = offsets.getValue(pathsId, offset);
        LOG.trace("Processing router key {} route {}", key, route);
        selector.processPath(ribSupport.extractAttributes(route), key, offset, pathId);
    }

    protected final AddPathBestPath bestPathAt(final RIBSupport<C, S, R, I> ribSupport, final int offset) {
        final MapEntryNode route = verifyNotNull(offsets.getValue(values, offset));
        return new AddPathBestPath(new BestPathStateImpl(ribSupport.extractAttributes(route)), offsets.getKey(offset),
            offsets.getValue(pathsId, offset), offset);
    }

    private boolean isBestPathNew(final ImmutableList<AddPathBestPath> newBestPathList) {
        this.isNonAddPathBestPathNew = !isNonAddPathBestPathTheSame(newBestPathList);
        filterRemovedPaths(newBestPathList);
        if (this.bestPathRemoved != null && !this.bestPathRemoved.isEmpty()
                || newBestPathList != null
                && !newBestPathList.equals(this.bestPath)) {
            if (this.bestPath != null) {
                this.newBestPathToBeAdvertised = new ArrayList<>(newBestPathList);
                this.newBestPathToBeAdvertised.removeAll(this.bestPath);
            } else {
                this.newBestPathToBeAdvertised = newBestPathList;
            }
            this.bestPath = newBestPathList;
            LOG.trace("Actual Best {}, removed best {}", this.bestPath, this.bestPathRemoved);
            return true;
        }
        return false;
    }

    private boolean isNonAddPathBestPathTheSame(final List<AddPathBestPath> newBestPathList) {
        return !isEmptyOrNull(this.bestPath) && !isEmptyOrNull(newBestPathList)
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
