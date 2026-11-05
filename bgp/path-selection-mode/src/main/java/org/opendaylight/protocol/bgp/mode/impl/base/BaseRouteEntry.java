/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
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

final class BaseRouteEntry implements RouteEntry {
    private static final class Stale extends StaleBestPathRoute {
        Stale(final NodeIdentifierWithPredicates nonAddPathRouteKeyIdentifier) {
            super(nonAddPathRouteKeyIdentifier);
        }

        @Override
        public List<NodeIdentifierWithPredicates> getStaleRouteKeyIdentifiers() {
            return Collections.singletonList(getNonAddPathRouteKeyIdentifier());
        }

        @Override
        public List<NodeIdentifierWithPredicates> getAddPathRouteKeyIdentifiers() {
            return Collections.emptyList();
        }

        @Override
        public boolean isNonAddPathBestPathNew() {
            return true;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(BaseRouteEntry.class);
    private static final MapEntryNode[] EMPTY_VALUES = new MapEntryNode[0];

    private RouterIdOffsets offsets = RouterIdOffsets.EMPTY;
    private MapEntryNode[] values = EMPTY_VALUES;
    private @Nullable BaseBestPath bestPath;
    // FIXNE: BGPCEP-879: this just a hand-off from selectBest() == true to removeStalePaths()
    private boolean removedBestPath;

    @Override
    public boolean putRoute(final RouterId routerId, final Uint32 remotePathId, final MapEntryNode route) {
        final int offset = offsets.offsetOf(routerId);
        if (offset < 0) {
            addRoute(routerId, route);
            return true;
        }
        setRoute(routerId, route, offset);
        return false;
    }

    @NonNullByDefault
    private void addRoute(final RouterId routerId, final MapEntryNode route) {
        final var newOffsets = offsets.with(routerId);
        final var offset = newOffsets.offsetOf(routerId);
        values = newOffsets.expand(offsets, values, offset);
        offsets = newOffsets;
        setRoute(routerId, route, offset);
    }

    @NonNullByDefault
    private void setRoute(final RouterId routerId, final MapEntryNode route, final int offset) {
        offsets.setValue(values, offset, route);
        LOG.trace("Added route {} from {}", route, routerId);
    }

    @Override
    public boolean removeRoute(final RouterId routerId, final Uint32 remotePathId) {
        final int offset = offsets.offsetOf(routerId);
        values = offsets.removeValue(values, offset, EMPTY_VALUES);
        offsets = offsets.without(routerId);
        return offsets.isEmpty();
    }

    @Override
    public boolean selectBest(final RIBSupport ribSupport, final long localAs) {
        final var size = offsets.size();
        return size != 0 ? selectBest(ribSupport, localAs, size) : selectNone();
    }

    @NonNullByDefault
    private boolean selectBest(final RIBSupport ribSupport, final long localAs, final int size) {
        /*
         * FIXME: optimize flaps by making sure we consider stability of currently-selected route.
         */
        final var selector = new BasePathSelector(localAs);

        // Select the best route.
        for (int i = 0; i < size; ++i) {
            final var routerId = offsets.getKey(i);
            final var attributes = ribSupport.extractAttributes(offsets.getValue(values, i));
            LOG.trace("Processing router id {} attributes {}", routerId, attributes);
            selector.processPath(routerId, attributes);
        }

        // Get the newly-selected best path and apply it to state
        return selectBest(selector.result());
    }

    private boolean selectBest(final @Nullable BaseBestPath newBestPath) {
        final var oldBestPath = bestPath;
        if (Objects.equals(oldBestPath, newBestPath)) {
            return false;
        }
        setBestPath(oldBestPath, newBestPath);
        return true;
    }

    private boolean selectNone() {
        // there are no candidates, so not bother with selection: there is no best path and if there was one, it is
        // now removed
        final var oldBestPath = bestPath;
        if (oldBestPath == null) {
            removedBestPath = false;
            return false;
        }
        removedBestPath = true;
        setBestPath(oldBestPath, null);
        return true;
    }

    private void setBestPath(final @Nullable BaseBestPath oldBestPath, final @Nullable BaseBestPath newBestPath) {
        LOG.trace("Previous best {}, current best {}", oldBestPath, newBestPath);
        bestPath = newBestPath;
    }

    @Override
    public StaleBestPathRoute removeStalePaths(final RIBSupport ribSupport, final String routeKey) {
        if (removedBestPath) {
            removedBestPath = false;
            return new Stale(ribSupport.createRouteListArgument(routeKey));
        }
        return null;
    }

    @Override
    public List<AdvertizedRoute> newBestPaths(final RIBSupport ribSupport, final String routeKey) {
        final var best = bestPath;
        if (best == null) {
            return List.of();
        }
        final var route = createRoute(ribSupport, routeKey, best);
        LOG.trace("Selected best route {}", route);
        return Collections.singletonList(new AdvertizedRoute(ribSupport, route, best.getAttributes(), best.getPeerId(),
            best.isDepreferenced()));
    }

    @Override
    public List<ActualBestPathRoutes> actualBestPaths(final RIBSupport ribSupport, final RouteEntryInfo entryInfo) {
        final var best = bestPath;
        return best == null ? List.of()
            : Collections.singletonList(new ActualBestPathRoutes(ribSupport,
                createRoute(ribSupport, entryInfo.getRouteKey(), best), best.getPeerId(), best.getAttributes(),
                best.isDepreferenced()));
    }

    @NonNullByDefault
    private MapEntryNode createRoute(final RIBSupport ribSupport, final String routeKey, final BaseBestPath best) {
        return ribSupport.createRoute(offsets.getValue(values, offsets.offsetOf(best.getRouterId())),
            ribSupport.createRouteListArgument(routeKey), best.getAttributes());
    }
}
