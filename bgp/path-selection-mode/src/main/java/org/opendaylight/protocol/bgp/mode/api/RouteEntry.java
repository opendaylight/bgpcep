/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.api;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
@NonNullByDefault
public interface RouteEntry {
    /**
     * Put a route.
     *
     * @param routerId     router ID in unsigned integer format from an Ipv4Address
     * @param remotePathId remote path Id received
     * @param route        route Data change
     * @return {@code true} if a new route was introduced, {@code false} if a route was replaced
     */
    boolean putRoute(RouterId routerId, Uint32 remotePathId, MapEntryNode route);

    /**
     * Remove a route.
     *
     * @param routerId     router ID in unsigned integer format from an Ipv4Address
     * @param remotePathId remote path Id received
     * @return {@code true} if it was the last route on entry
     */
    boolean removeRoute(RouterId routerId, Uint32 remotePathId);

    /**
     * Indicates whether best has changed.
     *
     * @param localAs The local autonomous system number
     * @return return true if it has changed
     */
    // FIXME: BGPCEP-879: return a contract including removeStalePaths() and newBestPaths()
    boolean selectBest(RIBSupport ribSupport, long localAs);

    /**
     * {@return collections of present selected best path}
     *
     * @param ribSupport RIB Support
     * @param entryInfo  Route Entry Info wrapper
     */
    List<ActualBestPathRoutes> actualBestPaths(RIBSupport ribSupport, RouteEntryInfo entryInfo);

    /**
     * {@return list of stale best paths}
     *
     * @param ribSupport RIB Support
     * @param routeKey   of stale route
     */
    @Nullable StaleBestPathRoute removeStalePaths(RIBSupport ribSupport, String routeKey);

    /**
     * {@return collection of best path routes after processing update of stale and new advertisement of routes}
     *
     * @param ribSupport RIB Support
     * @param routeKey   route key
     */
    List<AdvertizedRoute> newBestPaths(RIBSupport ribSupport, String routeKey);
}
