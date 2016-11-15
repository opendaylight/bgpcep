/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.spi.state;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;

/**
 * Representing State and counters relating to a BGP peer-group
 * - Grouping containing operational parameters relating to a BGP peer group
 * Total Paths - Total Prefixes counters, representing the paths / prefixes installed on Effective-rib-in
 */
public interface BGPPeerGroupState {
    /**
     * Register neighbor to group
     *
     * @param neighborState neighbor
     * @return registration
     */
    @Nonnull AbstractRegistration registerNeighbor(@Nonnull BGPNeighborState neighborState);

    /**
     * List of Neighbors containing Neighbors State
     *
     * @return neighbors
     */
    @Nonnull List<Neighbor> getNeighbors();

    /**
     * Peer Group containing Group State
     *
     * @return peer group
     */
    @Nullable PeerGroup getPeerGroup();

    /**
     * Peer Group is empty
     * @return return true if Peer Group is empty
     */
    boolean isEmpty();
}
