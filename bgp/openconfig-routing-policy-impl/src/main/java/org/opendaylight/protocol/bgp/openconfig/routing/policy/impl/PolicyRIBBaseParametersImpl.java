package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.PolicyRIBBaseParameters;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

public final class PolicyRIBBaseParametersImpl implements PolicyRIBBaseParameters {
    private final Long localAs;
    private final Ipv4Address originatorId;
    private final ClusterIdentifier clusterId;

    public PolicyRIBBaseParametersImpl(final Long localAs, final Ipv4Address originatorId,
        final ClusterIdentifier clusterId) {
        this.localAs = Preconditions.checkNotNull(localAs);
        this.originatorId = Preconditions.checkNotNull(originatorId);
        this.clusterId = Preconditions.checkNotNull(clusterId);
    }

    @Override
    public long getLocalAs() {
        return this.localAs;
    }

    @Override
    public Ipv4Address getOriginatorId() {
        return this.originatorId;
    }

    @Override
    public ClusterIdentifier getClusterId() {
        return this.clusterId;
    }
}
