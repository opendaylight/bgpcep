/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Condition Policy: Check if route matches defined condition
 */
public interface ConditionPolicy {
    /**
     * Check if route matches defined condition (Import Policy)
     *
     * @param routeKey YangInstanceIdentifier containing routeKey
     * @param fromPeerId peer Id of announcer Peer
     * @param fromPeerRole role of announcer Peer
     * @param attributes route attributes
     * @param condition augmentation containing condition definition
     * @return true if all defined condition matches
     */
    boolean matchImportCondition(@Nonnull PathArgument routeKey, @Nonnull PeerId fromPeerId,
        @Nonnull PeerRole fromPeerRole, @Nonnull ContainerNode attributes, @Nonnull Augmentation<Conditions> condition);

    /**
     * Check if route matches defined condition (Export Policy)
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
    boolean matchExportCondition(@Nonnull PathArgument routeKey, @Nonnull PeerId fromPeerId,
        @Nonnull PeerRole fromPeerRole, @Nonnull PeerId toPeer, @Nonnull PeerRole toPeerRole,
        @Nonnull ContainerNode attributes, @Nonnull Augmentation<Conditions> condition);
}
