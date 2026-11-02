/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.api;

import java.util.List;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * A single route entry inside a route table. Maintains the attributes of from all contributing peers. The information
 * is stored in arrays with a shared map of offsets for peers to allow lookups. This is needed to maintain low memory
 * overhead in face of large number of routes and peers, where individual object overhead becomes the dominating factor.
 */
public interface RouteEntry {
    /**
     * Put a route.
     *
     * @param routerId     router ID in unsigned integer format from an Ipv4Address
     * @param remotePathId remote path Id received
     * @param route        route Data change
     * @return {@code true} if a new route was introduced, {@code false} if a route was replaced
     */
    @NonNullByDefault
    boolean putRoute(RouterId routerId, Uint32 remotePathId, MapEntryNode route);

    /**
     * Remove a route.
     *
     * @param routerId     router ID in unsigned integer format from an Ipv4Address
     * @param remotePathId remote path Id received
     * @return {@code true} if it was the last route on entry
     */
    @NonNullByDefault
    boolean removeRoute(RouterId routerId, Uint32 remotePathId);

    /**
     * Indicates whether best has changed.
     *
     * @param localAs The local autonomous system number
     * @return return true if it has changed
     */
    boolean selectBest(RIBSupport ribSupport, long localAs);

    /**
     * Returns collections of present selected best path.
     *
     * @param ribSupport RIB Support
     * @param entryInfo  Route Entry Info wrapper
     */
    @NonNull List<ActualBestPathRoutes> actualBestPaths(@NonNull RIBSupport ribSupport,
            @NonNull RouteEntryInfo entryInfo);

    /**
     * Returns list of stale best path.
     *
     * @param ribSupport RIB Support
     * @param routeKey   of stale route
     * @return list containing list of stale best path
     */
    // FIXME: @NonNullByDefault on interface and @Nullable return here
    @NonNull Optional<StaleBestPathRoute> removeStalePaths(@NonNull RIBSupport ribSupport, @NonNull String routeKey);

    /**
     * Returns collection of best path routes after processing update of stale and new advertisement of routes.
     *
     * @param ribSupport RIB Support
     * @param routeKey   route key
     */
    @NonNull List<AdvertizedRoute> newBestPaths(@NonNull RIBSupport ribSupport, @NonNull String routeKey);
}
