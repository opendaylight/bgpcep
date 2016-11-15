/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerGroupState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroups;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentationBuilder;

/**
 * Util for create OpenConfig Peer group with corresponding openConfig state.
 */
public final class PeerGroupUtil {
    private PeerGroupUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Build Openconfig PeerGroups containing Peer group stats from a list of BGPPeerGroupState
     *
     * @param bgpStateConsumer providing BGPPeerGroupState
     * @return PeerGroups containing Peer group stats
     */
    public static PeerGroups buildPeerGroups(final BGPRIBState bgpStateConsumer) {
        final List<PeerGroup> peerGroupsList = bgpStateConsumer.getPeerGroupsState().stream()
            .map(PeerGroupUtil::buildPeerGroupState).filter(Objects::nonNull).collect(Collectors.toList());
        if (peerGroupsList.isEmpty()) {
            return null;
        }
        return new PeerGroupsBuilder().setPeerGroup(peerGroupsList).build();
    }

    /**
     * Build Openconfig PeerGroup containing Peer group stats from BGPPeerGroupState
     *
     * @param bgpPeerGroupState providing state of the group
     * @return PeerGroups containing Peer group stats
     */
    public static PeerGroup buildPeerGroupState(final BGPPeerGroupState bgpPeerGroupState) {
        final String groupId = bgpPeerGroupState.getGroupId();
        if (groupId == null) {
            return null;
        }
        final PeerGroupStateAugmentation groupState = new PeerGroupStateAugmentationBuilder()
            .setTotalPrefixes(bgpPeerGroupState.getTotalPrefixesCount())
            .setTotalPaths(bgpPeerGroupState.getTotalPathsCount())
            .build();

        return new PeerGroupBuilder()
            .setPeerGroupName(groupId)
            .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.StateBuilder()
                .addAugmentation(PeerGroupStateAugmentation.class, groupState).build()).build();
    }
}
