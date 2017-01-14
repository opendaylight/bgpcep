/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

/**
 * Registry of Statement to be consumed by Export and Import BGPRIBPolicy
 */
public interface StatementRegistryConsumer {
    /**
     * Apply statement to BGP Route Attributes (Export Policy)
     *
     * @param policyConsumer OpenconfigPolicy
     * @param localAs RIB AS
     * @param originatorId originator Id
     * @param clusterId Cluster Identifier
     * @param key Route Id
     * @param statement Statement containing Conditions/Actions
     * @param fromPeerId peer Id of announcer Peer
     * @param fromPeerRole role of announcer Peer
     * @param toPeer peer Id od announced Peer
     * @param toPeerRole peer role od announced Peer
     * @param attributes route attributes
     * @return modified Route attributes
     */
    RouteAttributeContainer applyExportStatement(OpenconfigPolicyConsumer policyConsumer, long localAs, Ipv4Address
        originatorId, ClusterIdentifier clusterId, PathArgument key, PeerId fromPeerId, PeerRole fromPeerRole,
        PeerId toPeer, PeerRole toPeerRole, RouteAttributeContainer attributes, Statement statement);

    /**
     * Apply statement to BGP Route Attributes (Import Policy)
     *
     * @param policyConsumer OpenconfigPolicy
     * @param localAs RIB AS
     * @param originatorId originator Id
     * @param clusterId Cluster Identifier
     * @param key Route Id
     * @param fromPeerId peer Id of announcer Peer
     * @param fromPeerRole role of announcer Peer
     * @param attributes route attributes
     * @param statement Statement containing Conditions/Actions
     * @return modified Route attributes
     */
    RouteAttributeContainer applyImportStatement(OpenconfigPolicyConsumer policyConsumer, long localAs, Ipv4Address originatorId,
        ClusterIdentifier clusterId, PathArgument key, PeerId fromPeerId, PeerRole fromPeerRole,
        RouteAttributeContainer attributes, Statement statement);
}
