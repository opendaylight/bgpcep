/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPGlobalStateImpl implements BGPGlobalState, BGPStateConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(BGPGlobalStateImpl.class);

    private final InstanceIdentifier<Bgp> bgpIId;
    private final BgpId routeId;
    private final AsNumber localAs;
    @GuardedBy("this")
    private final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> totalPaths = new HashMap<>();
    @GuardedBy("this")
    private final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> totalPrefixes = new HashMap<>();
    @GuardedBy("this")
    private final Map<String, BGPPeerGroupState> peerGroups = new HashMap<>();

    public BGPGlobalStateImpl(@Nonnull final Set<Class<? extends AfiSafiType>> announcedAfiSafis,
        @Nonnull final String ribId, @Nonnull final BgpId routeId, @Nonnull final AsNumber localAs,
        @Nonnull final InstanceIdentifier<Bgp> bgpIId) {
        Preconditions.checkNotNull(ribId);
        this.routeId = Preconditions.checkNotNull(routeId);
        this.localAs = Preconditions.checkNotNull(localAs);
        this.bgpIId = Preconditions.checkNotNull(bgpIId);
        Preconditions.checkNotNull(announcedAfiSafis).forEach(afiSafiType -> createAfiSafiCounter(ribId, afiSafiType));
    }

    private void createAfiSafiCounter(final String ribId, final Class<? extends AfiSafiType> afiSafiType) {
        this.totalPaths.put(afiSafiType, new UnsignedInt32Counter("Total Paths installed on Global " + ribId +
            " AFI-SAFI"));
        this.totalPrefixes.put(afiSafiType, new UnsignedInt32Counter("Total Prefixes installed on Global " + ribId +
            " AFI-SAFI"));
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
            LOG.info("Group State registered {}", neighborState.getNeighborAddress());
            group = new BGPPeerGroupStateImpl(groupId);
            this.peerGroups.put(groupId, group);
        }
        final AbstractRegistration registry = group.registerNeighbor(neighborState);

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (BGPGlobalStateImpl.this) {
                    registry.close();
                    if(BGPGlobalStateImpl.this.peerGroups.get(groupId).isEmpty()) {
                        LOG.info("Group State unregistered {}", groupId);
                        BGPGlobalStateImpl.this.peerGroups.remove(groupId);
                    }
                }
            }
        };
    }

    @Override
    public synchronized UnsignedInt32Counter getPathCounter(final Class<? extends AfiSafiType> afiSafiType) {
        return this.totalPrefixes.get(afiSafiType);
    }

    @Override
    public synchronized UnsignedInt32Counter getPrefixesCounter(final Class<? extends AfiSafiType> afiSafiType) {
        return this.totalPaths.get(afiSafiType);
    }

    private AfiSafis buildAfiSafis() {
        return new AfiSafisBuilder().setAfiSafi(buildAfisSafis()).build();
    }

    private List<AfiSafi> buildAfisSafis() {
        return this.totalPrefixes.keySet().stream().map(this::buildAfiSafi).collect(Collectors.toList());
    }

    private AfiSafi buildAfiSafi(final Class<? extends AfiSafiType> afiSafiType) {
        final State state = new StateBuilder()
            .addAugmentation(GlobalAfiSafiStateAugmentation.class, new GlobalAfiSafiStateAugmentationBuilder()
                .setTotalPaths(this.totalPaths.get(afiSafiType).getCount())
                .setTotalPrefixes(this.totalPrefixes.get(afiSafiType).getCount()).build())
            .build();
        return new AfiSafiBuilder().setAfiSafiName(afiSafiType).setState(state).build();
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

    private synchronized PeerGroups buildPeerGroups() {
        final List<PeerGroup> peerGroupsList = this.peerGroups.values().stream().map(BGPPeerGroupState::getPeerGroup)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (peerGroupsList.isEmpty()) {
            return null;
        }
        return new PeerGroupsBuilder().setPeerGroup(peerGroupsList).build();
    }

    private synchronized Neighbors buildNeighbors() {
        final List<Neighbor> neighbors = this.peerGroups.values().stream().map(BGPPeerGroupState::getNeighbors)
            .flatMap(List::stream).collect(Collectors.toList());
        if (neighbors.isEmpty()) {
            return null;
        }
        return new NeighborsBuilder().setNeighbor(neighbors).build();
    }
}
