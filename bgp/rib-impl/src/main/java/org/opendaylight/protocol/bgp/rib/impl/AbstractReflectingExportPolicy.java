/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * An intermediate abstract class shared by both Client and Non-Client
 * export policies.
 */
abstract class AbstractReflectingExportPolicy extends AbstractExportPolicy {
    private final ClusterIdentifier clusterId;
    private final Ipv4Address originatorId;

    protected AbstractReflectingExportPolicy(final Ipv4Address originatorId, final ClusterIdentifier clusterId) {
        this.originatorId = requireNonNull(originatorId);
        this.clusterId = requireNonNull(clusterId);
    }

    /**
     * Modify attributes so they are updated as per RFC4456 route reflection.
     *
     * @param attributes Input attributes, may not be null.
     * @return Modified (reflected) attributes.
     */
    @Nonnull protected final ContainerNode reflectedAttributes(@Nonnull final ContainerNode attributes) {
        return AttributeOperations.getInstance(attributes).reflectedAttributes(attributes, this.originatorId, this.clusterId);
    }

    /**
     * Modify attributes so they are updated as per RFC4456 route reflection, but without add ORIGINATOR_ID
     * and CLUSTER_ID inside CLUSTER_LIST as required https://tools.ietf.org/html/rfc4456#section-8, BUG 4070.
     *
     * @param attributes Input attributes, may not be null.
     * @return Modified (reflected) attributes.
     */
    @Nonnull protected static ContainerNode reflectedFromInternalAttributes(@Nonnull final ContainerNode attributes) {
        return AttributeOperations.getInstance(attributes).reflectedAttributes(attributes);
    }
}
