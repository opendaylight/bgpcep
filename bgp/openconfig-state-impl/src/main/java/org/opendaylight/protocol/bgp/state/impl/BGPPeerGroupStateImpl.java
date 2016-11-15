/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.state.spi.state.BGPNeighborState;
import org.opendaylight.protocol.bgp.state.spi.state.BGPPeerGroupState;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPPeerGroupStateImpl implements BGPPeerGroupState {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerGroupStateImpl.class);
    private final String groupId;
    @GuardedBy("this")
    private final List<BGPNeighborState> neighbors = new ArrayList<>();

    public BGPPeerGroupStateImpl(final String groupId) {
        this.groupId = groupId;
    }

    public synchronized AbstractRegistration registerNeighbor(final BGPNeighborState neighborState) {
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
    public synchronized List<Neighbor> getNeighbors() {
        return NeighborStateUtil.buildNeighbors(this.neighbors);
    }

    @Override
    public synchronized PeerGroup getPeerGroup() {
        if (this.groupId == null) {
            return null;
        }
        final PeerGroupStateAugmentation groupState = new PeerGroupStateAugmentationBuilder()
            .setTotalPrefixes(this.neighbors.stream().mapToLong(BGPNeighborState::getTotalPaths).sum())
            .setTotalPaths(this.neighbors.stream().mapToLong(BGPNeighborState::getTotalPrefixes).sum())
            .build();

        return new PeerGroupBuilder().setPeerGroupName(this.groupId).setState(new StateBuilder()
            .addAugmentation(PeerGroupStateAugmentation.class, groupState).build()).build();
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.neighbors.isEmpty();
    }
}
