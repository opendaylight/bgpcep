/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yangtools.concepts.Mutable;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * A single route entry, identified by a {@code route key}, inside a route table, supported by a {@link RIBSupport}.
 * Maintains the attributes of from all contributing peers. The information is stored in arrays with a shared map
 * of offsets for peers to allow lookups. This is needed to maintain low memory overhead in face of large number
 * of routes and peers, where individual object overhead becomes the dominating factor.
 *
 * <p>{@link RouteEntry} objects are inherently mutable with no inter-thread synchronization guarantees. The intended
 * contract is that a controller can either:
 * <ul>
 *   <li>query current best paths via {@link #actualBestPaths(RIBSupport, String)}</li>
 *   <li>update current best paths by invoking a series of {@link #putRoute(RouterId, Uint32, MapEntryNode)} and
 *       {@link #removeRoute(RouterId, Uint32)} operations followed by a call to
 *       {@link #selectBest(RIBSupport, long, String)}</li>
 * </ul>
 */
@NonNullByDefault
public interface RouteEntry extends Mutable {
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
     * Run best path selection and indicate its impact on propagation of this entry.
     *
     * @param ribSupport RIB Support
     * @param localAs The local autonomous system number
     * @param routeKey route key
     * @return an {@link BestPathUpdate} if selection results in a change in how this entry should be adverized
     */
    @CheckReturnValue
    @Nullable BestPathUpdate selectBest(RIBSupport ribSupport, long localAs, String routeKey);

    /**
     * {@return collections of present selected best path}
     *
     * @param ribSupport RIB Support
     * @param routeKey route key
     */
    @CheckReturnValue
    List<ActualBestPathRoutes> actualBestPaths(RIBSupport ribSupport, String routeKey);
}
