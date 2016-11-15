/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl.global;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.state.impl.peerGroup.BGPPeerGroupStateImpl;
import org.opendaylight.protocol.bgp.state.spi.counters.UnsignedInt32Counter;
import org.opendaylight.protocol.bgp.state.spi.state.BGPGlobalState;
import org.opendaylight.protocol.bgp.state.spi.state.BGPNeighborState;
import org.opendaylight.protocol.bgp.state.spi.state.BGPPeerGroupState;
import org.opendaylight.protocol.bgp.state.spi.state.BGPStateConsumer;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafis;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.BgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.NeighborsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroups;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class BGPGlobalStateImpl implements BGPGlobalState, BGPStateConsumer {
    private final InstanceIdentifier<Bgp> bgpIId;
    private final BgpId routeId;
    private final AsNumber localAs;
    @GuardedBy("this")
    private final Map<AfiSafi, UnsignedInt32Counter> totalPaths = new HashMap<>();
    @GuardedBy("this")
    private final Map<AfiSafi, UnsignedInt32Counter> totalPrefixes = new HashMap<>();
    @GuardedBy("this")
    private final Map<String, BGPPeerGroupState> peerGroups = new HashMap<>();

    public BGPGlobalStateImpl(@Nonnull final List<AfiSafi> afiSafis, @Nonnull final String ribId,
        @Nonnull final BgpId routeId, @Nonnull final AsNumber localAs, @Nonnull final InstanceIdentifier<Bgp> bgpIId) {
        Preconditions.checkNotNull(ribId);
        this.routeId = Preconditions.checkNotNull(routeId);
        this.localAs = Preconditions.checkNotNull(localAs);
        this.bgpIId = Preconditions.checkNotNull(bgpIId);
        Preconditions.checkNotNull(afiSafis).forEach(afiSafi -> createAfiSafiCounter(ribId, afiSafi));
    }

    private void createAfiSafiCounter(final String ribId, final AfiSafi afiSafi) {
        this.totalPaths.put(afiSafi, new UnsignedInt32Counter("Total Paths installed on Global " + ribId + " " + "AFI-SAFI"));
        this.totalPrefixes.put(afiSafi, new UnsignedInt32Counter("Total Prefixes installed on Global " + ribId + " AFI-SAFI"));
    }


    @Override
    public InstanceIdentifier<Bgp> getInstanceIdentifier() {
        return this.bgpIId;
    }

    @Override
    public Bgp getBgpState() {
        return new BgpBuilder().setGlobal(buildGlobal()).setNeighbors(buildNeighbors())
            .setPeerGroups(buildPeerGroups()).build();
    }

    @Override
    public Global buildGlobal() {
        return new GlobalBuilder().setState(buildState()).setAfiSafis(buildAfiSafis()).build();
    }

    @Override
    public synchronized AbstractRegistration registerNeighbor(final BGPNeighborState neighborState,
        final String groupId) {
        BGPPeerGroupState group = this.peerGroups.get(groupId);
        if (group == null) {
            group = new BGPPeerGroupStateImpl(groupId);
            this.peerGroups.put(groupId, group);
        }
        return group.registerNeighbor(neighborState);
    }

    @Override
    public synchronized void increaseAfiSafiPrefixes(final AfiSafi afiSafi) {
        this.totalPrefixes.get(afiSafi).increaseCount();
    }

    @Override
    public synchronized void increaseAfiSafiPaths(final AfiSafi afiSafi) {
        this.totalPaths.get(afiSafi).increaseCount();
    }

    @Override
    public  synchronized void decreaseAfiSafiPrefixes(final AfiSafi afiSafi) {
        this.totalPrefixes.get(afiSafi).decreaseCount();
    }

    @Override
    public synchronized  void decreaseAfiSafiPaths(final AfiSafi afiSafi) {
        this.totalPaths.get(afiSafi).decreaseCount();
    }

    private AfiSafis buildAfiSafis() {
        return new AfiSafisBuilder().setAfiSafi(buildAfisSafis()).build();
    }

    private List<AfiSafi> buildAfisSafis() {
        return this.totalPrefixes.keySet().stream().map(this::buildAfiSafi).collect(Collectors.toList());
    }

    private AfiSafi buildAfiSafi(final AfiSafi afiSafi) {
        final State state = new StateBuilder()
            .addAugmentation(GlobalAfiSafiStateAugmentation.class, new GlobalAfiSafiStateAugmentationBuilder()
            .setTotalPaths(this.totalPaths.get(afiSafi).getCount())
            .setTotalPrefixes(this.totalPrefixes.get(afiSafi).getCount()).build())
            .build();
        return new AfiSafiBuilder().setAfiSafiName(afiSafi.getAfiSafiName()).setState(state).build();
    }

    private org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.State buildState() {
        final long totalPathsCount = this.totalPaths.values().stream().mapToLong(UnsignedInt32Counter::getCount).sum();
        final long totalPrefCount = this.totalPrefixes.values().stream().mapToLong(UnsignedInt32Counter::getCount).sum();
        return new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.StateBuilder()
            .setAs(this.localAs)
            .setRouterId(this.routeId)
            .setTotalPaths(totalPathsCount)
            .setTotalPrefixes(totalPrefCount)
            .build();
    }

    public synchronized PeerGroups buildPeerGroups() {
        final List<PeerGroup> peerGroupsList = this.peerGroups.values().stream().map(BGPPeerGroupState::getPeerGroup)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if(peerGroupsList.isEmpty()) {
            return null;
        }
        return new PeerGroupsBuilder().setPeerGroup(peerGroupsList).build();
    }

    public synchronized Neighbors buildNeighbors() {
        final List<Neighbor> neighbors = this.peerGroups.values().stream().map(BGPPeerGroupState::getNeighbors)
            .flatMap(List::stream).collect(Collectors.toList());
        if(neighbors.isEmpty()) {
            return null;
        }
        return new NeighborsBuilder().setNeighbor(neighbors).build();
    }
}
