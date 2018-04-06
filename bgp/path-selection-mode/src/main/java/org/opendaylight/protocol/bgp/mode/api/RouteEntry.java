/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.api;

import com.google.common.primitives.UnsignedInteger;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;

/**
 * A single route entry inside a route table. Maintains the attributes of
 * from all contributing peers. The information is stored in arrays with a
 * shared map of offsets for peers to allow lookups. This is needed to
 * maintain low memory overhead in face of large number of routes and peers,
 * where individual object overhead becomes the dominating factor.
 */
public interface RouteEntry {
    /**
     * Remove route.
     *
     * @param routerId     router ID in unsigned integer format from an Ipv4Address
     * @param remotePathId remote path Id received
     * @return return true if it was the last route on entry
     */
    boolean removeRoute(@Nonnull UnsignedInteger routerId, long remotePathId);

    /**
     * Indicates whether best has changed.
     *
     * @param localAs The local autonomous system number
     * @return return true if it has changed
     */
    boolean selectBest(long localAs);

    /**
     * Add Route.
     *
     * @param routerId     router ID in unsigned integer format from an Ipv4Address
     * @param remotePathId remote path Id received
     * @param route        route Data change
     * @return returns the offset
     */
    int addRoute(@Nonnull UnsignedInteger routerId, long remotePathId, @Nonnull Route route);

    /**
     * Update LocRibOut and AdjRibsOut by removing stale best path and writing new best.
     *
     * @param entryDependencies entry Dependencies container
     * @param routeKey          route key
     * @param tx                DOM transaction
     */
    void updateBestPaths(
            @Nonnull RouteEntryDependenciesContainer entryDependencies,
            @Nonnull String routeKey,
            @Nonnull WriteTransaction tx);

    /**
     * Initialize LocRibOut and AdjRibsOut for new peers with already present best paths.
     *
     * @param entryDependencies Route Entry Dependencies wrapper
     * @param entryInfo         Route Entry Info wrapper
     * @param tx                transaction
     */
    void initializeBestPaths(
            @Nonnull RouteEntryDependenciesContainer entryDependencies,
            @Nonnull RouteEntryInfo entryInfo,
            @Nonnull WriteTransaction tx);
}