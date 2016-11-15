/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.spi.state;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;

/**
 * Representing State information relating to the global BGP router
 * -Grouping containing operational parameters relating to the global BGP instance.
 * Total Paths - Total Prefixes counters, representing the paths / prefixes installed on Loc-rib
 * -operational state and counters on a per-AFI-SAFI basis to the global BGP router
 * Total Paths - Total Prefixes counters, representing the paths / prefixes installed on Loc-rib per Afi Safi Type
 */
public interface BGPGlobalState {
    /**
     * Global containing the current state of counters
     *
     * @return Global
     */
    @Nonnull Global buildGlobal();

    /**
     * Register neighbor belonging to global BGP router
     *
     * @param neighborState neighbor
     * @param groupId group Id of the neighbor
     * @return registration
     */
    @Nonnull AbstractRegistration registerNeighbor(@Nonnull final BGPNeighborState neighborState, @Nullable final String groupId);

    /**
     * Increment prefixes counter per Afi Safi Type
     *
     * @param afiSafiType afi Safi
     */
    void incrementAfiSafiPrefixes(@Nonnull Class<? extends AfiSafiType> afiSafiType);

    /**
     * Increment paths counter per Afi Safi Type
     *
     * @param afiSafiType afi Safi
     */
    void incrementAfiSafiPaths(@Nonnull Class<? extends AfiSafiType> afiSafiType);

    /**
     * Decrement Prefixes counter per Afi Safi Type
     *
     * @param afiSafiType afi Safi
     */
    void decrementAfiSafiPrefixes(@Nonnull Class<? extends AfiSafiType> afiSafiType);

    /**
     * Decrement paths counter per Afi Safi Type
     *
     * @param afiSafiType afi Safi
     */
    void decrementAfiSafiPaths(@Nonnull Class<? extends AfiSafiType> afiSafiType);
}
