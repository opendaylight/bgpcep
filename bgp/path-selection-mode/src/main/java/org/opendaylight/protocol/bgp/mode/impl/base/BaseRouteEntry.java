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
    private BaseBestPath bestPath = null;
    private BaseBestPath removedBestPath;

    @Override
    public boolean removeRoute(final RouterId routerId, final Uint32 remotePathId) {
        final int offset = offsets.offsetOf(routerId);
        values = offsets.removeValue(values, offset, EMPTY_VALUES);
        offsets = offsets.without(routerId);
        return offsets.isEmpty();
    }

    private MapEntryNode createRoute(final RIBSupport<?, ?> ribSup, final String routeKey) {
        final var route = offsets.getValue(values, offsets.offsetOf(bestPath.getRouterId()));
        return ribSup.createRoute(route, ribSup.createRouteListArgument(routeKey), bestPath.getAttributes());
    }

    @Override
    public boolean selectBest(final RIBSupport<?, ?> ribSupport, final long localAs) {
        /*
         * FIXME: optimize flaps by making sure we consider stability of currently-selected route.
         */
        final var selector = new BasePathSelector(localAs);

        // Select the best route.
        for (int i = 0; i < offsets.size(); ++i) {
            final var routerId = offsets.getKey(i);
            final var attributes = ribSupport.extractAttributes(offsets.getValue(values, i));
            LOG.trace("Processing router id {} attributes {}", routerId, attributes);
            selector.processPath(routerId, attributes);
        }

        // Get the newly-selected best path.
        final var newBestPath = selector.result();
        final boolean modified = newBestPath == null || !newBestPath.equals(bestPath);
        if (modified) {
            if (offsets.isEmpty()) {
                removedBestPath = bestPath;
            }
            LOG.trace("Previous best {}, current best {}", bestPath, newBestPath);
            bestPath = newBestPath;
        }
        return modified;
    }

    @Override
    public int addRoute(final RouterId routerId, final Uint32 remotePathId, final MapEntryNode route) {
        int offset = offsets.offsetOf(routerId);
        if (offset < 0) {
            final var newOffsets = offsets.with(routerId);
            offset = newOffsets.offsetOf(routerId);

            values = newOffsets.expand(offsets, values, offset);
            offsets = newOffsets;
        }

        offsets.setValue(values, offset, route);
        LOG.trace("Added route {} from {}", route, routerId);
        return offset;
    }

    @Override
    public StaleBestPathRoute removeStalePaths(final RIBSupport<?, ?> ribSupport, final String routeKey) {
        if (removedBestPath == null) {
            return null;
        }
        removedBestPath = null;
        return new Stale(ribSupport.createRouteListArgument(routeKey));
    }

    @Override
    public List<AdvertizedRoute> newBestPaths(final RIBSupport<?, ?> ribSupport, final String routeKey) {
        if (bestPath == null) {
            return Collections.emptyList();
        }
        final var route = createRoute(ribSupport, routeKey);
        final var adv = new AdvertizedRoute(ribSupport, route, bestPath.getAttributes(), bestPath.getPeerId(),
            bestPath.isDepreferenced());
        LOG.trace("Selected best route {}", route);
        return Collections.singletonList(adv);
    }

    @Override
    public List<ActualBestPathRoutes> actualBestPaths(final RIBSupport<?, ?> ribSupport,
            final RouteEntryInfo entryInfo) {
        if (bestPath == null) {
            return Collections.emptyList();
        }
        final var route = createRoute(ribSupport, entryInfo.getRouteKey());
        return Collections.singletonList(new ActualBestPathRoutes(ribSupport, route, bestPath.getPeerId(),
                bestPath.getAttributes(), bestPath.isDepreferenced()));
    }
}
