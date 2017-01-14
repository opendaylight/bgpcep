/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRIBRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

public interface BGPOpenconfigRIBRoutingPolicyConsumer {
    /**
     * Builds BGPRIBRoutingPolicy from BGP Openconfig policy Configuration
     *
     *
     * @param localAs RIB AS
     * @param policyConfig BGP Openconfig policy Configuration
     * @return BGPRIBRoutingPolicy
     */
    BGPRIBRoutingPolicy buildBGPRIBPolicy(long localAs, @Nonnull Ipv4Address bgpId,
        @Nonnull ClusterIdentifier clusterId, @Nonnull Config policyConfig);
}
