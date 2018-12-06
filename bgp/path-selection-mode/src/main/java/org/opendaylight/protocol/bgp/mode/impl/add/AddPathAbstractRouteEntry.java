/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
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
public abstract class AddPathAbstractRouteEntry<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
        implements RouteEntry<C, S, R, I> {

    private static final Logger LOG = LoggerFactory.getLogger(AddPathAbstractRouteEntry.class);
    private static final Long[] EMPTY_PATHS_ID = new Long[0];
    private static final Route[] EMPTY_VALUES = new Route[0];

    protected OffsetMap offsets = OffsetMap.EMPTY;
    protected R[] values = (R[]) EMPTY_VALUES;
    protected Long[] pathsId = EMPTY_PATHS_ID;
    private List<AddPathBestPath> bestPath;
    private List<AddPathBestPath> bestPathRemoved;
    private long pathIdCounter = 0L;
    private boolean isNonAddPathBestPathNew;
    private List<AddPathBestPath> newBestPathToBeAdvertised;
    private List<Long> removedPathsId;

    private R createRoute(final RIBSupport<C, S, R, I> ribSup, final String routeKey,
            final long pathId, final AddPathBestPath path) {
        final OffsetMap map = getOffsets();
        final R route = map.getValue(this.values, map.offsetOf(path.getRouteKey()));
        return ribSup.createRoute(route, routeKey, pathId, path.getAttributes());
    }

    @Override
    public final int addRoute(final UnsignedInteger routerId, final long remotePathId, final R route) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        int offset = this.offsets.offsetOf(key);
        if (offset < 0) {
            final OffsetMap newOffsets = this.offsets.with(key);
            offset = newOffsets.offsetOf(key);
            final R[] newRoute = newOffsets.expand(this.offsets, this.values, offset);
            final Long[] newPathsId = newOffsets.expand(this.offsets, this.pathsId, offset);
            this.values = newRoute;
            this.offsets = newOffsets;
            this.pathsId = newPathsId;
            this.offsets.setValue(this.pathsId, offset, ++this.pathIdCounter);
        }
        this.offsets.setValue(this.values, offset, route);
        LOG.trace("Added route {} from {}", route, routerId);
        return offset;
    }

    @Override
    public final boolean removeRoute(final UnsignedInteger routerId, final long remotePathId) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        final int offset = getOffsets().offsetOf(key);
        final Long pathId = this.offsets.getValue(this.pathsId, offset);
        this.values = this.offsets.removeValue(this.values, offset, (R[]) EMPTY_VALUES);
        this.pathsId = this.offsets.removeValue(this.pathsId, offset, EMPTY_PATHS_ID);
        this.offsets = this.offsets.without(key);
        if (this.removedPathsId == null) {
            this.removedPathsId = new ArrayList<>();
        }
        this.removedPathsId.add(pathId);
        return isEmpty();
    }

    @Override
    public final Optional<StaleBestPathRoute<C, S, R, I>> removeStalePaths(final RIBSupport<C, S, R, I> ribSupport,
            final String routeKey) {
        if ((this.bestPathRemoved == null || this.bestPathRemoved.isEmpty()) && this.removedPathsId == null) {
            return Optional.empty();
        }
        List<Long> stalePaths = Collections.emptyList();
        if (this.bestPathRemoved != null && !this.bestPathRemoved.isEmpty()) {
            stalePaths = this.bestPathRemoved.stream().map(AddPathBestPath::getPathId).collect(Collectors.toList());
            this.bestPathRemoved = null;
        }
        final StaleBestPathRoute<C, S, R, I> stale = new StaleBestPathRoute<>(ribSupport, routeKey, stalePaths,
                this.removedPathsId, this.isNonAddPathBestPathNew);
        this.removedPathsId = null;
        return Optional.of(stale);
    }

    @Override
    public final List<AdvertizedRoute<C, S, R, I>> newBestPaths(final RIBSupport<C, S, R, I> ribSupport,
            final String routeKey) {
        if (this.newBestPathToBeAdvertised == null || this.newBestPathToBeAdvertised.isEmpty()) {
            return Collections.emptyList();
        }
        final List<AdvertizedRoute<C, S, R, I>> advertized = new ArrayList<>(newBestPathToBeAdvertised.size());
        final AddPathBestPath firstBestPath = this.bestPath.isEmpty() ? null : this.bestPath.get(0);
        for (final AddPathBestPath path : this.newBestPathToBeAdvertised) {
            final R routeAddPath = createRoute(ribSupport, routeKey, path.getPathId(), path);
            // FIXME: can we use identity check here?
            final boolean isFirstBestPath = firstBestPath != null && firstBestPath.equals(path);
            final AdvertizedRoute<C, S, R, I> adv = new AdvertizedRoute<>(ribSupport, isFirstBestPath,
                    routeAddPath, path.getAttributes(), path.getPeerId());
            advertized.add(adv);
        }
        this.newBestPathToBeAdvertised = null;
        return advertized;
    }

    @Override
    public final List<ActualBestPathRoutes<C, S, R, I>> actualBestPaths(final RIBSupport<C, S, R, I> ribSupport,
            final RouteEntryInfo entryInfo) {
        if (this.bestPath == null || this.bestPath.isEmpty()) {
            return Collections.emptyList();
        }
        final List<ActualBestPathRoutes<C, S, R, I>> preexistentRoutes = new ArrayList<>();
        for (final AddPathBestPath path : this.bestPath) {
            final R route = createRoute(ribSupport, entryInfo.getRouteKey(), path.getPathId(), path);
            final ActualBestPathRoutes<C, S, R, I> adv = new ActualBestPathRoutes<>(ribSupport, route, path.getPeerId(),
                    path.getAttributes());
            preexistentRoutes.add(adv);
        }
        return preexistentRoutes;
    }

    private OffsetMap getOffsets() {
        return this.offsets;
    }

    public final boolean isEmpty() {
        return this.offsets.isEmpty();
    }

    private void selectBest(final RouteKey key, final AddPathSelector selector) {
        final int offset = this.offsets.offsetOf(key);
        final R route = this.offsets.getValue(this.values, offset);
        final long pathId = this.offsets.getValue(this.pathsId, offset);
        LOG.trace("Processing router key {} route {}", key, route);
        selector.processPath(route.getAttributes(), key, offset, pathId);
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

    protected boolean isBestPathNew(final ImmutableList<AddPathBestPath> newBestPathList) {
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
