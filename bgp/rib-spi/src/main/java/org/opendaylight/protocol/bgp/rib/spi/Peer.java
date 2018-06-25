/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.protocol.bgp.rib.spi.policy.RouteTargetMembershipConsumer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

/**
 * Marker interface identifying a BGP peer.
 */
public interface Peer extends PeerTrackerInformation, RouteTargetMembershipConsumer {
    /**
     * Return peer's symbolic name.
     *
     * @return symbolic name.
     */
    @Nonnull
    String getName();

    /**
     * Return the peer's BGP identifier as raw byte array.
     *
     * @return byte[] raw identifier
     */
    byte[] getRawIdentifier();

    /**
     * Close Peers and performs asynchronously DS clean up.
     *
     * @return future
     */
    @Nonnull
    ListenableFuture<?> close();

    /**
     * Update peers ribout after path selection processing.
     *
     * @param entryDep    RouteEntryDependenciesContainer
     * @param staleRoutes routes to be removed.
     * @param newRoutes   routes to be advertized.
     */
    <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> void refreshRibOut(
            @Nonnull RouteEntryDependenciesContainer entryDep,
            @Nonnull List<StaleBestPathRoute<C, S, R, I>> staleRoutes,
            @Nonnull List<AdvertizedRoute<C, S, R, I>> newRoutes);

    /**
     * Stores under peers rib Out already present routes, before proceed to process any new route advertizement.
     *
     * @param entryDep RouteEntryDependenciesContainer
     * @param routes   routes to be advertized.
     */
    <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> void initializeRibOut(
            @Nonnull RouteEntryDependenciesContainer entryDep,
            @Nonnull List<ActualBestPathRoutes<C, S, R, I>> routes);

    /**
     * Applies all policies through all present routes, and advertize/withdraws based on new results.
     * Scenario would be for example a removal of RT membership. And reprocess VPN routes.
     *
     * @param entryDep RouteEntryDependenciesContainer
     * @param routes   routes to be updated.
     */
    <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> void reEvaluateAdvertizement(@Nonnull RouteEntryDependenciesContainer entryDep,
            @Nonnull List<ActualBestPathRoutes<C, S, R, I>> routes);
}
