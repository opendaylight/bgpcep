/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.protocol.bgp.rib.impl.spi.ImportPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invoked on routes which we get from our normal home AS peers.
 */
class FromInternalImportPolicy implements ImportPolicy {
    private static final Logger LOG = LoggerFactory.getLogger(FromInternalImportPolicy.class);
    private final ClusterIdentifier clusterIdentifier;
    private final Ipv4Address bgpIdentifier;

    FromInternalImportPolicy(final Ipv4Address bgpIdentifier, final ClusterIdentifier clusterIdentifier) {
        this.bgpIdentifier = Preconditions.checkNotNull(bgpIdentifier);
        this.clusterIdentifier = Preconditions.checkNotNull(clusterIdentifier);
    }

    @Override
    public ContainerNode effectiveAttributes(final ContainerNode attributes) {
        final AttributeOperations oper = AttributeOperations.getInstance(attributes);

        /*
         * This is an implementation of https://tools.ietf.org/html/rfc4456#section-8
         *
         * We first check the ORIGINATOR_ID, if present. If it matches our BGP identifier,
         * we filter the route.
         */
        final Object originatorId = oper.getOriginatorId(attributes);
        if (bgpIdentifier.getValue().equals(originatorId)) {
            LOG.debug("Filtering route with our ORIGINATOR_ID {}", bgpIdentifier);
            return null;
        }

        /*
         * Second we check CLUSTER_LIST, if present. If it contains our CLUSTER_ID, we issue
         * a warning and ignore the route.
         */
        final LeafSetNode<?> clusterList = oper.getClusterList(attributes);
        if (clusterList != null) {
            for (LeafSetEntryNode<?> node : clusterList.getValue()) {
                if (clusterIdentifier.getValue().equals(node.getValue())) {
                    LOG.info("Received a route with our CLUSTER_ID {} in CLUSTER_LIST {}, filtering it", clusterIdentifier.getValue(), clusterList);
                    return null;
                }
            }
        }

        return attributes;
    }
}
