/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.util.concurrent.FluentFuture;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.protocol.bgp.rib.spi.policy.RouteTargetMembershipConsumer;

/**
 * Marker interface identifying a BGP peer.
 */
public interface Peer extends PeerTrackerInformation, RouteTargetMembershipConsumer {
    /**
     * Return peer's symbolic name.
     *
     * @return symbolic name.
     */
    @NonNull String getName();

    /**
     * Close Peers and performs asynchronously DS clean up.
     *
     * @return future
     */
    @NonNull FluentFuture<? extends Object> close();

    /**
     * Update peers ribout after path selection processing.
     *
     * @param entryDep    RouteEntryDependenciesContainer
     * @param staleRoutes routes to be removed.
     * @param newRoutes   routes to be advertized.
     */
    @NonNullByDefault
    void refreshRibOut(RouteEntryDependenciesContainer entryDep, List<StaleBestPathRoute> staleRoutes,
        List<AdvertizedRoute> newRoutes);

    /**
     * Stores under peers rib Out already present routes, before proceed to process any new route advertizement.
     *
     * @param entryDep RouteEntryDependenciesContainer
     * @param routes   routes to be advertized.
     */
    @NonNullByDefault
    void initializeRibOut(RouteEntryDependenciesContainer entryDep, List<ActualBestPathRoutes> routes);

    /**
     * Applies all policies through all present routes, and advertize/withdraws based on new results.
     * Scenario would be for example a removal of RT membership. And reprocess VPN routes.
     *
     * @param entryDep RouteEntryDependenciesContainer
     * @param routes   routes to be updated.
     */
    @NonNullByDefault
    void reEvaluateAdvertizement(RouteEntryDependenciesContainer entryDep, List<ActualBestPathRoutes> routes);
}
