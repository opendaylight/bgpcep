/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.concepts.AbstractRegistration;

/**
 * Representing State and counters relating to a BGP peer-group
 * - Grouping containing operational parameters relating to a BGP peer group
 * Total Paths - Total Prefixes counters, representing the paths / prefixes installed on Effective-rib-in
 */
public interface BGPPeerGroupState {
    /**
     * Register neighbor to Peer Group
     *
     * @param neighborState neighbor
     * @return registration
     */
    @Nonnull
    AbstractRegistration registerNeighbor(@Nonnull BGPPeerState neighborState);

    /**
     * List of Neighbors State per group
     *
     * @return neighbors
     */
    @Nonnull
    List<BGPPeerState> getNeighbors();

    /**
     * Peer Group is empty
     *
     * @return return true if Peer Group is empty
     */
    boolean isEmpty();

    /**
     * Total Prefixes count per group
     *
     * @return count
     */
    long getTotalPrefixesCount();

    /**
     * Total Paths count per group
     *
     * @return count
     */
    long getTotalPathsCount();

    /**
     * Group Id
     *
     * @return group Id
     */
    @Nullable
    String getGroupId();
}