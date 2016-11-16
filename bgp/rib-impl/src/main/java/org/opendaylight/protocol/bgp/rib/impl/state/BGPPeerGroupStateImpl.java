/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.spi.state.*;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPPeerGroupStateImpl implements BGPPeerGroupState {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerGroupStateImpl.class);
    private final String groupId;
    @GuardedBy("this")
    private final List<BGPPeerState> neighbors = new ArrayList<>();

    public BGPPeerGroupStateImpl(final String groupId) {
        this.groupId = groupId;
    }

    public synchronized AbstractRegistration registerNeighbor(final org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState neighborState) {
        LOG.info("Neighbor State registered {}", neighborState.getNeighborAddress());
        this.neighbors.add(neighborState);
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (BGPPeerGroupStateImpl.this) {
                    LOG.info("Neighbor State unregistered {}", neighborState.getNeighborAddress());
                    BGPPeerGroupStateImpl.this.neighbors.remove(neighborState);
                }
            }
        };
    }

    @Override
    public synchronized List<BGPPeerState> getNeighbors() {
        return this.neighbors;
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.neighbors.isEmpty();
    }

    public long getTotalPrefixesCount() {
        return this.neighbors.stream().mapToLong(BGPPeerState::getTotalPrefixes).sum();
    }

    public long getTotalPathsCount() {
        return this.neighbors.stream().mapToLong(BGPPeerState::getTotalPrefixes).sum();
    }

    @Override
    public synchronized String getGroupId() {
        return this.groupId;
    }
}
