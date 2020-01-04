/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.state;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroups;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.PeerGroupStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.PeerGroupStateAugmentationBuilder;

/**
 * Util for create OpenConfig Peer group with corresponding openConfig state.
 */
public final class PeerGroupUtil {
    private PeerGroupUtil() {
        // Hidden on purpose
    }

    /**
     * Build Openconfig PeerGroups containing Peer group stats from a list of BGPPeerGroupState.
     *
     * @param bgpStateConsumer providing BGPPeerGroupState
     * @return PeerGroups containing Peer group stats
     */

    public static @Nullable PeerGroups buildPeerGroups(final @NonNull List<BGPPeerState> bgpStateConsumer) {
        final Map<String, List<BGPPeerState>> peerGroups = bgpStateConsumer.stream()
                .filter(state -> state.getGroupId() != null)
                .collect(Collectors.groupingBy(BGPPeerState::getGroupId));
        if (peerGroups.isEmpty()) {
            return null;
        }

        final List<PeerGroup> peerGroupsList = peerGroups.entrySet().stream()
                .map(entry -> buildPeerGroupState(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return new PeerGroupsBuilder().setPeerGroup(peerGroupsList).build();
    }

    /**
     * Build Openconfig PeerGroup containing Peer group stats from BGPPeerGroupState.
     *
     * @param groupId Peer group Id
     * @param groups  providing state of the group
     * @return PeerGroups containing Peer group stats
     */
    public static @NonNull PeerGroup buildPeerGroupState(final @NonNull String groupId,
            final @NonNull List<BGPPeerState> groups) {
        final PeerGroupStateAugmentation groupState = new PeerGroupStateAugmentationBuilder()
                .setTotalPrefixes(groups.stream().mapToLong(BGPPeerState::getTotalPrefixes).sum())
                .setTotalPaths(groups.stream().mapToLong(BGPPeerState::getTotalPathsCount).sum())
                .build();

        return new PeerGroupBuilder()
                .setPeerGroupName(groupId)
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                        .StateBuilder().addAugmentation(PeerGroupStateAugmentation.class, groupState).build()).build();
    }
}
