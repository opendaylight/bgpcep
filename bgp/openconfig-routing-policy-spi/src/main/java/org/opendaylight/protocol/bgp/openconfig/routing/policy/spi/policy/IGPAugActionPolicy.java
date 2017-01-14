/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * IGP Action Policy to be applied to Route Attributes
 */
public interface IGPAugActionPolicy {
    /**
     * Applies IGP action to Route Attributes container (Export Policy)
     *
     * @param localAs RIB AS
     * @param originatorId originator Id
     * @param clusterId Cluster Identifier
     * @param routeKey YangInstanceIdentifier containing routeKey
     * @param fromPeerId peer Id of announcer Peer
     * @param fromPeerRole role of announcer Peer
     * @param toPeer peer Id od announced Peer
     * @param toPeerRole peer role od announced Peer
     * @param attributes route attributes
     * @param action augmentation containing actions definition
     * @return modified Route attributes
     */
    ContainerNode apply(long localAs, @Nonnull Ipv4Address originatorId, @Nonnull ClusterIdentifier clusterId,
        @Nonnull PathArgument routeKey, @Nonnull PeerId fromPeerId, @Nonnull PeerRole fromPeerRole,
        @Nonnull PeerId toPeer, @Nonnull PeerRole toPeerRole, @Nonnull ContainerNode attributes,
        @Nonnull Augmentation<IgpActions> action);

    /**
     * Applies IGP action to Route Attributes container (Import Policy)
     *
     * @param localAs RIB AS
     * @param originatorId originator Id
     * @param clusterId Cluster Identifier
     * @param routeKey YangInstanceIdentifier containing routeKey
     * @param fromPeerId peer Id of announcer Peer
     * @param fromPeerRole role of announcer Peer
     * @param attributes route attributes
     * @param action augmentation containing actions definition
     * @return modified Route attributes
     */
    ContainerNode apply(long localAs, @Nonnull Ipv4Address originatorId, @Nonnull ClusterIdentifier clusterId,
        @Nonnull PathArgument routeKey, @Nonnull PeerId fromPeerId, @Nonnull PeerRole fromPeerRole,
        @Nonnull ContainerNode attributes, @Nonnull Augmentation<IgpActions> action);
}
