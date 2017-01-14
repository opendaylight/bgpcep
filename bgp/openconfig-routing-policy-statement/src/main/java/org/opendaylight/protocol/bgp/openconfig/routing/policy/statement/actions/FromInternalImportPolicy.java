/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invoked on routes which we get from our normal home AS peers from an Internal Peer.
 */
public final class FromInternalImportPolicy extends AbstractImportAction {
    private static final Logger LOG = LoggerFactory.getLogger(FromInternalImportPolicy.class);

    @Override
    public ContainerNode applyImportAction(final long localAs, final Ipv4Address originatorId,
        final ClusterIdentifier clusterId, final YangInstanceIdentifier.PathArgument routeKey, final PeerId fromPeerId,
        final PeerRole fromPeerRole, final ContainerNode attributes, final Augmentation<Actions> action) {
        final AttributeOperations operationAtt = AttributeOperations.getInstance(attributes);

        /*
         * This is an implementation of https://tools.ietf.org/html/rfc4456#section-8
         *
         * We first check the ORIGINATOR_ID, if present. If it matches our BGP identifier,
         * we filter the route.
         */
        final Object presentOriginatorId = operationAtt.getOriginatorId(attributes);
        if (originatorId.getValue().equals(presentOriginatorId)) {
            LOG.debug("Filtering route with our ORIGINATOR_ID {}", originatorId);
            return null;
        }

        /*
         * Second we check CLUSTER_LIST, if present. If it contains our CLUSTER_ID, we issue
         * a warning and ignore the route.
         */
        final LeafSetNode<?> clusterList = operationAtt.getClusterList(attributes);
        if (clusterList != null) {
            for (final LeafSetEntryNode<?> node : clusterList.getValue()) {
                if (clusterId.getValue().equals(node.getValue())) {
                    LOG.info("Received a route with our CLUSTER_ID {} in CLUSTER_LIST {}, filtering it", clusterId.getValue(), clusterList);
                    return null;
                }
            }
        }

        return attributes;
    }
}
