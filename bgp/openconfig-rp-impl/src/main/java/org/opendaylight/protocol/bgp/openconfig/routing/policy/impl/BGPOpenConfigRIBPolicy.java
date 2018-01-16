/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPOpenconfigRIBRoutingPolicyProvider;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.OpenconfigPolicyConsumer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

public final class BGPOpenConfigRIBPolicy implements BGPOpenconfigRIBRoutingPolicyProvider {
    private final OpenconfigPolicyConsumer policyProvider;
    private final StatementRegistryConsumer statementRegistryConsumer;

    public BGPOpenConfigRIBPolicy(final OpenconfigPolicyConsumer policyProvider,
            final StatementRegistryConsumer statementRegistryConsumer) {
        this.policyProvider = requireNonNull(policyProvider);
        this.statementRegistryConsumer = requireNonNull(statementRegistryConsumer);
    }

    @Override
    public BGPRibRoutingPolicy buildBGPRibPolicy(final long localAs, final Ipv4Address bgpId,
            final ClusterIdentifier clusterId, final Config policyConfig) {
        requireNonNull(policyConfig);
        return new BGPRibPolicyImpl(this.statementRegistryConsumer, this.policyProvider,
                localAs, bgpId, clusterId, policyConfig);
    }
}
