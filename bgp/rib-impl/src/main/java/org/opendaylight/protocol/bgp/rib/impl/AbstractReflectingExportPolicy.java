/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
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
        this.originatorId = Preconditions.checkNotNull(originatorId);
        this.clusterId = Preconditions.checkNotNull(clusterId);
    }

    /**
     * Modify attributes so they are updated as per RFC4456 route reflection.
     *
     * @param attributes Input attributes, may not be null.
     * @return Modified (reflected) attributes.
     */
    @Nonnull protected final ContainerNode reflectedAttributes(@Nonnull final ContainerNode attributes) {
        return AttributeOperations.getInstance(attributes).reflectedAttributes(attributes, originatorId, clusterId);
    }

    /**
     * Modify attributes so they are updated as per RFC4456 route reflection.
     *
     * @param attributes Input attributes, may not be null.
     * @return Modified (reflected) attributes.
     */
    @Nonnull protected final ContainerNode reflectedFromInternalAttributes(@Nonnull final ContainerNode attributes) {
        return AttributeOperations.getInstance(attributes).reflectedAttributes(attributes);
    }
}
