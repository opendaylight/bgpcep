/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;

public final class PolicyRIBBaseParametersImpl implements RouteEntryBaseAttributes {
    private final Long localAs;
    private final Ipv4AddressNoZone originatorId;
    private final ClusterIdentifier clusterId;

    public PolicyRIBBaseParametersImpl(final long localAs, final Ipv4AddressNoZone originatorId,
            final ClusterIdentifier clusterId) {
        this.localAs = localAs;
        this.originatorId = requireNonNull(originatorId);
        this.clusterId = requireNonNull(clusterId);
    }

    @Override
    public long getLocalAs() {
        return this.localAs;
    }

    @Override
    public Ipv4AddressNoZone getOriginatorId() {
        return this.originatorId;
    }

    @Override
    public ClusterIdentifier getClusterId() {
        return this.clusterId;
    }
}
