/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * IGP Condition Policy: Check if route matches defined condition
 */
public interface IGPConditionPolicy {
    /**
     * Check if route matches defined IGP condition (Export Policy)
     *
     * @param routeKey YangInstanceIdentifier containing routeKey
     * @param fromPeerId peer Id of announcer Peer
     * @param fromPeerRole role of announcer Peer
     * @param toPeer peer Id od announced Peer
     * @param toPeerRole peer role od announced Peer
     * @param attributes route attributes
     * @param condition augmentation containing condition definition
     * @return true if all defined condition matches
     */
    boolean match(@Nonnull PathArgument routeKey, @Nonnull PeerId fromPeerId, @Nonnull PeerRole fromPeerRole,
        @Nonnull PeerId toPeer, @Nonnull PeerRole toPeerRole, @Nonnull ContainerNode attributes,
        @Nonnull Augmentation<IGPConditionPolicy> condition);

    /**
     * Check if route matches defined IGP condition (Import Policy)
     *
     * @param routeKey YangInstanceIdentifier containing routeKey
     * @param fromPeerId peer Id of announcer Peer
     * @param fromPeerRole role of announcer Peer
     * @param attributes route attributes
     * @param condition augmentation containing condition definition
     * @return true if all defined condition matches
     */
    boolean match(@Nonnull PathArgument routeKey, @Nonnull PeerId fromPeerId, @Nonnull PeerRole fromPeerRole,
        @Nonnull ContainerNode attributes, @Nonnull Augmentation<IGPConditionPolicy> condition);
}
